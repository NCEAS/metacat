# This script pulls new records from the systemmetadata table periodically and submit the
# information as the index tasks to the RabbitMQ service.
# Needed libraries:
# pip3 install psycopg2-binary
# pip3 install amqpstorm
# You may run this script on the background by this command:
# nohup python3 pull_systemmeta.py > pull_systemmeta.log 2>&1 &

import asyncio
import aiohttp
import psycopg2
import json
import os
import threading
import concurrent.futures
import queue
import time
import requests
import xml.etree.ElementTree as ET
from psycopg2 import pool
from amqpstorm import Connection, AMQPError, AMQPConnectionError, AMQPChannelError
from datetime import datetime
from concurrent.futures import wait, ALL_COMPLETED
from urllib.parse import urljoin
import fcntl

# --- Configuration ---
# Replace with your RabbitMQ and database credentials
RABBITMQ_USERNAME = "guest"
RABBITMQ_PASSWORD = "guest"
DB_USERNAME = "metacat"
DB_PASSWORD = "metacat"
CN_URL = "https://cn.dataone.org/cn/v2"
RABBITMQ_URL = "localhost"
RABBITMQ_PORT_NUMBER = 5672
SOLR_URL = "http://localhost:8983/solr/metacat-index/select"
POLL_INTERVAL = 10  # second
MAX_ROWS = 4000
# Number of worker threads to submit index tasks to RabbitMQ
# The pool_size of the rabbitmq channel pool is using it as well.
# The number must be less than those settings:
# the max number of channels connection to rabbitmq (2047) and the number of the processor core number.
MAX_WORKERS = 5
DB_DATABASE_NAME = "metacat"
DB_HOST_NAME = "localhost"
DB_PORT_NUMBER = 5432
LAST_TIMESTAMP_FILE = "last_timestamp"
DB_CONNECTION_POOL_SIZE = 3
DOCUMENTS_DIR = "/var/metacat/documents"
DATA_DIR = "/var/metacat/data"
CHECK_FILE_WAIT_MILLISECONDS = 50
CHECK_FILE_MAX_ATTEMPTS = 200
DOCID_WAIT_SEC = 0.1   # 100 milliseconds
DOCID_MAX_RETRIES = 5
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"
resourcemap_format_list = ["http://www.openarchives.org/ore/terms", "http://www.w3.org/TR/rdf-syntax-grammar"]
pg_pool = None
DEFAULT_DATE = "2000-01-01 00:00:00.000"
FORMATS_URL = urljoin(CN_URL + "/", "formats")
NODE_URL = urljoin(CN_URL + "/", "node")
MN_STATE_FILE = "mn_latest_modified_map.json"


# A class represents a RabbitMQ channel pool
class AMQPStormChannelPool:
    def __init__(self, host, port, username, password, pool_size=5):
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.pool_size = pool_size
        self._lock = threading.Lock()
        self._connection = None
        self._channels = queue.Queue(maxsize=pool_size)
        self._healthy = False
        self._initialize_pool()

    def _initialize_pool(self):
        # NEVER hold lock while connecting
        self._close_all()
        try:
            conn = Connection(
                self.host,
                self.username,
                self.password,
                port=self.port,
                heartbeat=30,
                timeout=10
            )
        except Exception as e:
            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] [CHANNEL POOL] Failed to connect to RabbitMQ: {e}")
            self._healthy = False
            raise
        with self._lock:
            self._connection = conn
            self._healthy = True
            for _ in range(self.pool_size):
                self._channels.put(self._create_new_channel())

    def _create_new_channel(self):
        if not self._connection or not self._connection.is_open:
            raise Exception("Connection is not open")
        channel = self._connection.channel()
        self.ensure_topology(channel)
        return channel

    def _is_healthy(self):
        return self._healthy and self._connection and self._connection.is_open

    def mark_unhealthy(self):
        with self._lock:
            self._healthy = False

    def ensure_topology(self, channel):
        channel.exchange.declare(EXCHANGE_NAME, durable=True)
        channel.queue.declare(QUEUE_NAME, durable=True, arguments={"x-max-priority": 10})
        channel.queue.bind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY)


    def acquire_channel(self):
        if not self._is_healthy():
            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] [CHANNEL POOL] Connection unhealthy. Reinitializing.")
            self._initialize_pool()
        try:
            channel = self._channels.get_nowait()
        except queue.Empty:
            return self._create_new_channel()
        if not channel or not channel.is_open:
            return self._create_new_channel()
        return channel

    def release_channel(self, channel):
        if not channel or not channel.is_open:
            return
        try:
            self._channels.put_nowait(channel)
        except queue.Full:
            try:
                channel.close()
            except Exception:
                pass

    def _close_all(self):
        while not self._channels.empty():
            try:
                ch = self._channels.get_nowait()
                if ch and ch.is_open:
                    ch.close()
            except Exception:
                pass
        if self._connection and self._connection.is_open:
            try:
                self._connection.close()
            except Exception:
                pass

    def close(self):
        with self._lock:
            self._close_all()


# Database connection parameters
DB_CONFIG = {
    'dbname': DB_DATABASE_NAME,
    'user': DB_USERNAME,
    'password': DB_PASSWORD,
    'host': DB_HOST_NAME,
    'port': DB_PORT_NUMBER
}

class DocumentNotFoundError(Exception):
    """Raised when the document paths are not found after max attempts."""
    pass

# --- memory cache for the node list---
_last_node_cache = None
_last_node_fetch = 0
NODE_TTL = 5 * 60  # two minutes
# -------------------

"""
    Gets the member node list from CN or the cached result
"""
def get_node_identifiers_memory_cached():
    global _last_node_cache, _last_node_fetch
    now = time.time()

    # ---- use cache if fresh ----
    if _last_node_cache is not None and (now - _last_node_fetch) < NODE_TTL:
        print("Using cached node list")
        return _last_node_cache

    print("Refreshing node list from CN...")

    r = requests.get(NODE_URL, timeout=60)
    r.raise_for_status()

    root = ET.fromstring(r.text)

    mn_nodes = []

    # ----- KEY TRICK: ignore namespaces entirely -----
    for node in root.findall(".//{*}node"):

        node_type = node.get("type")
        ident_el = node.find("{*}identifier")

        if (
            node_type
            and node_type.lower() == "mn"
            and ident_el is not None
            and ident_el.text
        ):
            mn_nodes.append(ident_el.text.strip())

    print(f"Found {len(mn_nodes)} 'mn' nodes")

    _last_node_cache = mn_nodes
    _last_node_fetch = now

    return mn_nodes

"""
    Gets the full map of the node_id and the indexed latest date_modified. If the solr server
    already has some records, use the latest one. If the running member nodes from CN don't
    have any solr docs, use a default value as the latest date_modified.
"""
def get_full_latest_map():
    mn_map = asyncio.run(get_latest_date_by_mn_solr5_async())
    nodes = get_node_identifiers_memory_cached()

    for node_id in nodes:
        if node_id not in mn_map:
            print(f"Adding missing node: {node_id}")
            mn_map[node_id] = DEFAULT_DATE

    return mn_map

"""
    Use an asynchronized way to get the latest modification date for a given mn
"""
async def fetch_latest_date(session, mn):
    params = {
        "q": f'authoritativeMN:"{mn}"',
        "rows": 0,
        "wt": "json",
        "stats": "true",
        "stats.field": "dateModified"
    }
    for attempt in range(3):
        try:
            async with session.get(SOLR_URL, params=params, timeout=60) as resp:
                resp.raise_for_status()
                data = await resp.json()
                max_date = data.get("stats", {}).get("stats_fields", {}).get("dateModified", {}).get("max")
                if max_date:
                    clean = max_date.replace("T", " ").replace("Z", "").split("+")[0]
                    return mn, clean
                return mn, None
        except Exception as e:
            if attempt < 2:
                await asyncio.sleep(2)
            else:
                print(f"Failed to fetch {mn}: {e}")
                return mn, None

"""
    Use an asynchronized way to get the latest modification date for all mns
"""
async def get_latest_date_by_mn_solr5_async(batch_size=100):
    result = {}
    start = 0

    async with aiohttp.ClientSession() as session:  # <- Session is open for all batches
        while True:
            params = {
                "q": "id:*",
                "rows": 0,
                "wt": "json",
                "facet": "true",
                "facet.field": "authoritativeMN",
                "facet.limit": batch_size,
                "facet.offset": start
            }

            for attempt in range(3):
                try:
                    async with session.get(SOLR_URL, params=params, timeout=60) as resp:
                        resp.raise_for_status()
                        data = await resp.json()
                        break
                except Exception as e:
                    if attempt < 2:
                        await asyncio.sleep(2)
                    else:
                        raise

            buckets = data.get("facet_counts", {}).get("facet_fields", {}).get("authoritativeMN", [])
            if not buckets:
                break

            mns = [buckets[i] for i in range(0, len(buckets), 2)]
            print(f"Fetched {len(mns)} MNs from offset {start}")

            # fetch max(dateModified) for this batch in parallel
            tasks = [fetch_latest_date(session, mn) for mn in mns]
            batch_results = await asyncio.gather(*tasks)
            result.update({mn: date for mn, date in batch_results if date is not None})

            if len(buckets) < batch_size * 2:
                break
            start += batch_size

    return result

def load_mn_latest_map():
    if os.path.exists(MN_STATE_FILE):
        with open(MN_STATE_FILE, "r") as f:
            data = json.load(f)
            print(f"Loaded MN state from {MN_STATE_FILE}")
            return data
    return None

def save_mn_latest_map(mn_map):
    tmp = MN_STATE_FILE + ".tmp"
    with open(tmp, "w") as f:
        fcntl.flock(f, fcntl.LOCK_EX)
        json.dump(mn_map, f, indent=2, sort_keys=True)
        f.flush()
        os.fsync(f.fileno())
    os.replace(tmp, MN_STATE_FILE)

"""
    Fetch DataONE format XML and return a list of formatId values
    whose type is not 'DATA'.
"""
def load_non_data_format_ids():
    non_data_format_ids = []

    resp = requests.get(FORMATS_URL, timeout=30)
    resp.raise_for_status()

    root = ET.fromstring(resp.text)

    # Iterate all objectFormat elements regardless of namespace
    for fmt in root.iter():
        if fmt.tag.endswith("objectFormat"):
            # Find formatId and formatType ignoring namespace
            fmt_id = None
            fmt_type = None
            for child in fmt:
                if child.tag.endswith("formatId"):
                    fmt_id = child.text.strip() if child.text else ""
                elif child.tag.endswith("formatType"):
                    fmt_type = child.text.strip() if child.text else ""

            if fmt_id and fmt_type and fmt_type.upper() != "DATA":
                non_data_format_ids.append(fmt_id)

    print(non_data_format_ids)
    return non_data_format_ids

"""
    Use the wait-try mechanism to make sure that the given docid exists in the file system
"""
def wait_for_docid(docid: str):
    """
    Wait until a file for the given docid exists under either
    DOCUMENTS_DIR or DATA_DIR.

    Args:
        docid (str): The document ID.

    Raises:
        DocumentNotFoundError: If neither path exists after CHECK_FILE_MAX_ATTEMPTS.
    """
    if not docid:  # docid doesn't exist (empty or None)
        return  # Do nothing

    doc_path = os.path.join(DOCUMENTS_DIR, docid)
    data_path = os.path.join(DATA_DIR, docid)

    for attempt in range(1, CHECK_FILE_MAX_ATTEMPTS + 1):
        if os.path.exists(doc_path) or os.path.exists(data_path):
            return  # Found the file, success
        if attempt < CHECK_FILE_MAX_ATTEMPTS:
            time.sleep(CHECK_FILE_WAIT_MILLISECONDS / 1000.0)

    # After max attempts, still not found
    raise DocumentNotFoundError(
        f"Document '{docid}' not found in {doc_path} or {data_path} "
        f"after {CHECK_FILE_MAX_ATTEMPTS} attempts."
    )

"""
    Processes a single PID:
       1 Construct the rabbitmq message
       2 Publish the message to the rabbitmq service
"""
def process_pid_wrapper(channel_pool, guid, object_format, doc_id):
    thread_name = threading.current_thread().name
    try:
        index_type = 'create'
        priority = 4
        if object_format and object_format in resourcemap_format_list:
            priority = 3
        if guid:
            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [{thread_name}] Processing PID: {guid} with type: {index_type}, docid: {doc_id}, priority: {priority}")
            headers = {'index_type': index_type, 'id': guid, 'doc_id': doc_id}
            message = ''
            channel = None
            wait_for_docid(doc_id)
            try:
                channel = channel_pool.acquire_channel()
                channel.basic.publish(
                    body=message,
                    routing_key=ROUTING_KEY,
                    exchange=EXCHANGE_NAME,
                    properties={'headers': headers, 'priority': priority}
                )
                print(f"Published guid {guid} into RabbitMQ")
            except (AMQPConnectionError, AMQPChannelError, OSError) as e:
                    print(f"[ERROR] RabbitMQ publish failed for {guid}: {e}")
                    channel_pool.mark_unhealthy()
                    raise   # VERY IMPORTANT
            finally:
                if channel:
                    try:
                        channel_pool.release_channel(channel)
                    except Exception:
                        pass
        else:
            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [{thread_name}] No GUID found in the query")
    except AMQPError as amqp_err:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [ERROR] [{thread_name}] AMQPStorm error while processing PID {guid}: {amqp_err}")
    except Exception as e:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [ERROR] [{thread_name}] Unexpected error while processing PID {guid}: {e}")
    return None

"""
   Periodically to pull new modified records from the systemmetadata table and submit the index
   tasks for them
"""
def poll_and_submit(non_data_formats):
    global pg_pool
    worker_timeout_sec = MAX_ROWS/MAX_WORKERS * 0.25
    print(f"The timeout for workers to completed jobs for a batch is {worker_timeout_sec}")
    channel_pool = AMQPStormChannelPool(
        RABBITMQ_URL, RABBITMQ_PORT_NUMBER,
        RABBITMQ_USERNAME, RABBITMQ_PASSWORD,
        MAX_WORKERS
    )

    non_data_formats = set(non_data_formats)

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=MAX_WORKERS,
        thread_name_prefix="PullProcessor"
    ) as executor:

        while True:
            cycle_start = time.perf_counter()
            try:
                print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Start new polling cycle.")

                # Get latest timestamps the file or Solr
                mn_latest_map = load_mn_latest_map()
                if mn_latest_map is None:
                    print("No MN state file found — bootstrapping from Solr")
                    mn_latest_map = get_full_latest_map()
                    save_mn_latest_map(mn_latest_map)
                else:
                    print("Using persisted MN state")

                print("Latest timestamps by node:")
                for k, v in mn_latest_map.items():
                    print(f"   {k} -> {v}")

                # Build JSON payload for all nodes
                payload = json.dumps([
                    {"amn": k, "last_time": v}  # use string directly
                    for k, v in mn_latest_map.items()
                ])

                # Single Postgres query
                futures = []
                conn = None
                try:
                    conn = pg_pool.getconn()
                    with conn.cursor() as cur:
                        cur.execute(f"""
                            SELECT
                                sm.guid,
                                sm.object_format,
                                i.docid || '.' || i.rev AS doc_id,
                                sm.date_modified,
                                sm.authoritive_member_node
                            FROM systemmetadata sm
                            LEFT JOIN identifier i
                                ON sm.guid = i.guid
                            JOIN (
                                SELECT *
                                FROM json_to_recordset(%s::json)
                                AS t(amn text, last_time timestamptz)
                            ) AS latest
                              ON sm.authoritive_member_node = latest.amn
                             AND sm.date_modified > latest.last_time
                            ORDER BY sm.date_modified ASC
                            LIMIT {MAX_ROWS};
                        """, (payload,))

                        rows = cur.fetchall()

                        if not rows:
                            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] No new records. Sleeping.")
                            time.sleep(POLL_INTERVAL)
                            continue

                        # Process rows
                        batch_max_time = {}
                        for guid, object_format, doc_id, modified_time, amn in rows:
                            print(f"Start to process {guid} which was read  from the dbname tables.")
                            # docId retry logic
                            if object_format in non_data_formats and not doc_id:
                                for attempt in range(1, DOCID_MAX_RETRIES + 1):
                                    time.sleep(DOCID_WAIT_SEC)
                                    cur.execute("""
                                        SELECT docid || '.' || rev
                                        FROM identifier
                                        WHERE guid = %s
                                    """, (guid,))
                                    res = cur.fetchone()
                                    if res and res[0]:
                                        doc_id = res[0]
                                        break
                                    else:
                                        print(f"Retry {attempt}/{DOCID_MAX_RETRIES}: doc_id still missing for guid {guid}")

                            if object_format in non_data_formats and not doc_id:
                                print(f"Skipping guid {guid}: doc_id not found after {DOCID_MAX_RETRIES} retries")
                                continue

                            # Submit task to thread pool
                            futures.append(
                                executor.submit(
                                    process_pid_wrapper,
                                    channel_pool,
                                    guid,
                                    object_format,
                                    doc_id
                                )
                            )
                            batch_max_time[amn] = max(
                                batch_max_time.get(amn, modified_time),
                                modified_time
                            )

                finally:
                    if conn:
                        pg_pool.putconn(conn)

                # Wait for all workers
                if futures:
                    done, not_done = wait(futures, timeout=worker_timeout_sec)
                    if not_done:
                        print(f"[WARN] {len(not_done)} worker(s) hung — state NOT advanced")
                    else:
                        for amn, ts in batch_max_time.items():
                            mn_latest_map[amn] = ts.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
                        save_mn_latest_map(mn_latest_map)
                    print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Batch completed.")

            except KeyboardInterrupt:
                print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Polling interrupted. Exiting.")
                break

            except Exception as poll_error:
                print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] [ERROR] Polling failed: {poll_error}")

            # --- Sleep regardless success or failure to maintain poll interval ---
            elapsed = time.perf_counter() - cycle_start
            sleep_time = max(0, POLL_INTERVAL - elapsed)
            if sleep_time > 0:
                time.sleep(sleep_time)

        channel_pool.close()
        if pg_pool:
            pg_pool.closeall()

if __name__ == "__main__":
    non_data_formats = load_non_data_format_ids()
    pg_pool = pool.ThreadedConnectionPool(
            minconn = 1,
            maxconn = DB_CONNECTION_POOL_SIZE,
            **DB_CONFIG
    )
    poll_and_submit(non_data_formats)