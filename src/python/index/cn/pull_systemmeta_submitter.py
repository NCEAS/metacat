# This script pulls new records from the systemmetadata table periodically and submit the
# information as the index tasks to the RabbitMQ service.
# Needed libraries:
# pip3 install aiohttp
# pip3 install psycopg2-binary
# pip3 install amqpstorm
# You may run this script on the background by this command:
# nohup python3 pull_systemmeta_submitter.py &

import asyncio
import aiohttp
import concurrent.futures
import configparser
import fcntl
import psycopg2
import json
import logging
import logging.config
import os
import queue
import requests
import time
import threading
import xml.etree.ElementTree as ET


from amqpstorm import Connection, AMQPError, AMQPConnectionError, AMQPChannelError
from concurrent.futures import wait, ALL_COMPLETED
from datetime import datetime
from logging.handlers import RotatingFileHandler
from pathlib import Path
from psycopg2 import pool
from queue import Empty
from urllib.parse import urljoin

# Get the settings from config.ini
config = configparser.ConfigParser()
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
config.read(os.path.join(BASE_DIR, "config.ini"))

RABBITMQ_USERNAME = config["rabbitmq"]["username"]
RABBITMQ_PASSWORD = config["rabbitmq"]["password"]
RABBITMQ_URL = config["rabbitmq"]["hostname"]
RABBITMQ_PORT_NUMBER = int(config["rabbitmq"]["port"])

DB_USERNAME = config["database"]["username"]
DB_PASSWORD = config["database"]["password"]

CN_URL = config["services"]["cn_url"]
SOLR_URL = config["services"]["solr_url"]

ENABLE_INDEXER = config["services"].getboolean("enable_indexer")

# --- Configuration ---
PULL_INTERVAL = 50  # second
MAX_ROWS = 4000
EVERY_SUBMIT_WAIT_TIME_SEC = 0.01
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
DOCID_MAX_RETRIES = 20
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"
resourcemap_format_list = ["http://www.openarchives.org/ore/terms", "http://www.w3.org/TR/rdf-syntax-grammar"]
pg_pool = None
DEFAULT_DATE = "2000-01-01 00:00:00.000"
FORMATS_URL = urljoin(CN_URL + "/", "formats")
NODE_URL = urljoin(CN_URL + "/", "node")
MN_STATE_FILE = ".mn_latest_modified_map.json"
MN_NOTIFICATION_STATE_FILE = ".notification_mn_latest_modification_map.json"
LOG_LEVEL = logging.DEBUG
LOGGER_NAME = "pull_systemmeta_submitter"
LOG_FILE = f"log/{LOGGER_NAME}.log"
shutdown_event = threading.Event()
logger = logging.getLogger(LOGGER_NAME)

# Settings for not showing the log from some libraries
def _silence_third_party_logs():
    """
    Reduce verbosity of noisy third-party libraries.
    Safe to call multiple times.
    """
    logging.getLogger("amqpstorm").setLevel(logging.INFO)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("aiohttp").setLevel(logging.WARNING)

# Set up the configuration of logging
def setup_logging(
    log_file=LOG_FILE,
    level=LOG_LEVEL,
    max_bytes=100 * 1024 * 1024,
    backups=1000,
):
    log_dir = os.path.dirname(log_file)
    if log_dir:
        os.makedirs(log_dir, exist_ok=True)
    logger.setLevel(level)
    logger.handlers.clear()
    logger.propagate = False
    handler = RotatingFileHandler(
        log_file,
        maxBytes=max_bytes,
        backupCount=backups,
    )
    formatter = logging.Formatter(
        "%(asctime)s,%(msecs)03d %(name)s %(levelname)s "
        "[%(process)d:%(threadName)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    handler.setFormatter(formatter)
    logger.addHandler(handler)
    _silence_third_party_logs()
    logger.debug("Logging initialized")

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
        self.initialize_pool()

    def initialize_pool(self):
        # Create connection OUTSIDE lock
        conn = self._create_connection()
        with self._lock:
            if not self.is_healthy():  # double-check
                self._close_all_locked()
                self._connection = conn
                self._healthy = True
                # rebuild pool
                self._reset_pool()
            else:
                # Another thread already fixed it → discard extra connection
                try:
                    new_conn.close()
                except Exception:
                    pass

    # NEVER hold lock while performing network I/O (connection creation)
    # Only lock when mutating shared state. So please don't put this method into a lock
    def _create_connection(self):
        return Connection(
            self.host,
            self.username,
            self.password,
            port=self.port,
            heartbeat = 120,
            timeout = 10
        )

    def _create_new_channel(self):
        if not self._connection or not self._connection.is_open:
            raise Exception("Connection is not open")
        channel = self._connection.channel()
        self.ensure_topology(channel)
        return channel

    def is_healthy(self):
        return self._healthy and self._connection and self._connection.is_open

    def mark_unhealthy(self):
        with self._lock:
            self._healthy = False

    def ensure_topology(self, channel):
        channel.exchange.declare(EXCHANGE_NAME, durable=True)
        channel.queue.declare(QUEUE_NAME, durable=True, arguments={"x-max-priority": 10})
        channel.queue.bind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY)


    # Acquire channel (core)
    def acquire_channel(self, max_retries=3):
        attempt = 0
        while attempt <= max_retries:
            if attempt > 0:
                logger.info(f"[CHANNEL POOL] acquire retry #{attempt}")
            # --- Step 1: ensure healthy connection ---
            if not self.is_healthy():
                # Create connection OUTSIDE lock
                try:
                    new_conn = self._create_connection()
                except Exception as e:
                    if attempt >= max_retries:
                        raise AMQPConnectionError(f"Failed to connect RabbitMQ after retries: {e}")
                    time.sleep(0.5 * (2 ** attempt))
                    attempt += 1
                    continue
                # Swap inside lock to create channels
                with self._lock:
                    # Double check inside the the lock to see if another thread fixed the issue
                    if not self.is_healthy():
                        self._close_all_locked()
                        self._connection = new_conn
                        self._healthy = True
                        # rebuild pool
                        self._reset_pool()
                    else:
                        # Another thread fixed it
                        try:
                            new_conn.close()
                        except Exception:
                            pass
            # --- Step 2: acquire channel ---
            try:
                ch = self._channels.get_nowait()
            except queue.Empty:
                # pool exhausted → create extra channel
                try:
                    return self._create_new_channel()
                except Exception:
                    self.mark_unhealthy()
                    attempt += 1
                    continue
            # --- Step 3: validate channel ---
            if not ch or not ch.is_open:
                try:
                    return self._create_new_channel()
                except Exception:
                    self.mark_unhealthy()
                    attempt += 1
                    continue
            if not self._connection or not self._connection.is_open:
                self.mark_unhealthy()
                attempt += 1
                continue
            return ch
        raise AMQPChannelError("Failed to acquire healthy RabbitMQ channel after retries")


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

    def _close_all_locked(self):
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
            self._close_all_locked()
            self._healthy = False

    # This method is not thread safe and the caller should have a lock for it
    def _reset_pool(self):
        # Drain
        while True:
            try:
                ch = self._channels.get_nowait()
            except Empty:
                break
            try:
                if ch and ch.is_open:
                    ch.close()
            except Exception:
                pass
        # Rebuild
        for _ in range(self.pool_size):
            try:
                ch = self._create_new_channel()
                self._channels.put(ch)
            except Exception as e:
                logger.warning(f"Failed to create channel: {e}")


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
# two minutes
NODE_TTL = 5 * 60
# -------------------

"""
    Gets the member node id list from the systemmetadata table in CN. This is the truth of the node
    id list
"""
def get_node_ids_from_systemmetadata():
    global _last_node_cache, _last_node_fetch
    now = time.time()
    # Use cache if fresh
    if _last_node_cache is not None and (now - _last_node_fetch) < NODE_TTL:
        logger.debug("Using cached node list")
        return _last_node_cache
    # Get them from db
    mn_nodes = []
    conn = pg_pool.getconn()
    try:
        with conn.cursor() as cur:
            cur.execute("""SELECT DISTINCT authoritive_member_node FROM systemmetadata;""")
            for (node,) in cur.fetchall():
                # excludes None, '' and whitespace-only
                if node and node.strip():
                    mn_nodes.append(node)
        logger.debug(f"Get {len(mn_nodes)} 'mn' nodes from db since cache expired")
        _last_node_cache = mn_nodes
        _last_node_fetch = now
        return mn_nodes
    finally:
        pg_pool.putconn(conn)

"""
    Gets the map of the node_id and the indexed latest date_modified from solr. It can be empty
    if the solr server is empty.
"""
def get_mn_latest_map_from_solr():
    mn_map = asyncio.run(get_latest_date_by_mn_solr5_async())
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
                logger.warning(f"Failed to fetch {mn}: {e}")
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
            logger.debug(f"Fetched {len(mns)} MNs from offset {start}")

            # fetch max(dateModified) for this batch in parallel
            tasks = [fetch_latest_date(session, mn) for mn in mns]
            batch_results = await asyncio.gather(*tasks)
            result.update({mn: date for mn, date in batch_results if date is not None})

            if len(buckets) < batch_size * 2:
                break
            start += batch_size

    return result

"""
Get a full map of the node_id and latest_modification_date by checking the stored file, Solr
server and the full node_id list.
"""
def get_full_mn_latest_map():
    changed = False
    node_ids = get_node_ids_from_systemmetadata()
    # Loaded the map from the stored file first
    map = load_mn_latest_map_from_file()
    if not map:
        # Try to get the map from solr if it can't be load from the file
        map = get_mn_latest_map_from_solr()
        changed = True
        logger.info("Can't load the map from the stored file. So load the map from the solr server")
    # Check if all node_id in the systemmetadata table is in the map. If not, add it.
    # This can handle a fresh start as well.
    for node_id in node_ids:
            if node_id not in map:
                logger.debug(f"Adding missing node: {node_id} to the map of node_id and latest_modification_date")
                map[node_id] = DEFAULT_DATE
                changed = True
    if changed:
        save_mn_latest_map(map)
    return map

"""
Get a  map of the node_id and latest_modification_date from the stored file. An empty map will be
returned if the file doesn't exist, is empty, corrupted, or not well-formatted.
"""
def load_mn_latest_map_from_file():
    data = {}
    if os.path.exists(MN_STATE_FILE):
        try:
            with open(MN_STATE_FILE, "r") as f:
                data = json.load(f)
            logger.debug(f"Loaded MN state from {MN_STATE_FILE}")
        except Exception as e:
            logger.warning(f"[WARN] Failed to load MN state file, starting fresh: {e}")
    return data

"""
Save the given map into a file
"""
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

    logger.debug(non_data_format_ids)
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
   Find the docid in identifier table for the given guid. It will try multiple times if it cannot
    find it. None will be return if the docid cannot be found after multiple retries.
"""
def lookup_docid_with_retry(conn, guid):
    for attempt in range(1, DOCID_MAX_RETRIES + 1):
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT docid || '.' || rev
                FROM identifier
                WHERE guid = %s
                """,
                (guid,)
            )
            res = cur.fetchone()
            if res and res[0]:
                return res[0]
        if attempt == DOCID_MAX_RETRIES:
            logger.warning(
                f"Final retry ({attempt}/{DOCID_MAX_RETRIES}): doc_id still missing for {guid}"
            )
        time.sleep(DOCID_WAIT_SEC)

    return None

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
            logger.debug(f"[{thread_name}] Processing PID: {guid} with type: {index_type}, docid: {doc_id}, priority: {priority}")
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
                logger.debug(f"Published guid {guid} into RabbitMQ")
            finally:
                if channel:
                    try:
                        channel_pool.release_channel(channel)
                    except Exception:
                        pass
        else:
            logger.warning(f"[{thread_name}] No GUID found in the query")
    except (AMQPConnectionError, AMQPChannelError, AMQPError) as amqp_err:
        logger.error(f"[ERROR] [{thread_name}] AMQPStorm error while processing PID {guid}: {amqp_err}")
        channel_pool.mark_unhealthy()
        raise
    except Exception as e:
        logger.error(f"[ERROR] [{thread_name}] Unexpected error while processing PID {guid}: {e}")
        raise
    return None

"""
   Periodically to pull new modified records from the systemmetadata table and submit the index
   tasks for them
"""
def poll_and_submit_index(non_data_formats):
    if not ENABLE_INDEXER:
            logger.debug("The index submission is disabled.")
            return
    global pg_pool
    global channel_pool
    worker_timeout_sec = MAX_ROWS/MAX_WORKERS * 0.25
    logger.debug(f"The timeout for workers to completed jobs for a batch is {worker_timeout_sec}")

    non_data_formats = set(non_data_formats)

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=MAX_WORKERS,
        thread_name_prefix="PullProcessor"
    ) as executor:

        while True:
            cycle_start = time.perf_counter()
            try:
                logger.info("Start a new pulling cycle.")
                # Check if the connection to RabbitMQ is healthy
                if not channel_pool.is_healthy():
                    # Reconnect RabbitMQ by initializing the pool if the connection is not healthy
                    try:
                        channel_pool.initialize_pool()
                    except (AMQPConnectionError, AMQPChannelError, AMQPError) as amqp_err:
                        logger.error(f"Cannot connect RabbitMQ since {amqp_err}. The submitter will try the pull process later")
                        shutdown_event.wait(PULL_INTERVAL)
                        continue
                # Get latest map of node_ids and timestamps from the file or Solr
                mn_latest_map = get_full_mn_latest_map()
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
                        length = len(rows)

                        if not rows:
                            logger.info("No new records. Sleeping.")
                            shutdown_event.wait(PULL_INTERVAL)
                            continue

                        # Process rows
                        batch_max_time = {}
                        for guid, object_format, doc_id, modified_time, amn in rows:
                            try:
                                logger.debug(f"Start to process {guid}:")
                                # docId retry logic
                                if object_format in non_data_formats and not doc_id:
                                    doc_id = lookup_docid_with_retry(conn, guid)
                                # After multiple retries, we have to skip it if the docid still
                                # cannot be found
                                if object_format in non_data_formats and not doc_id:
                                    logger.info(f"Skipping guid {guid}: doc_id not found after {DOCID_MAX_RETRIES} retries")
                                    # Event though the guid is skipped, we still need to set its
                                    # date as the last modified_time
                                    batch_max_time[amn] = max(batch_max_time.get(amn, modified_time),
                                        modified_time
                                    )
                                    continue

                                # Submit task to thread pool
                                shutdown_event.wait(EVERY_SUBMIT_WAIT_TIME_SEC)
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
                            except Exception as error:
                                logger.error(f"[ERROR] Process {guid}: {error}")
                finally:
                    if conn:
                        pg_pool.putconn(conn)

                # Wait for all workers
                disconnectionHappened = None
                if futures:
                    try:
                        done, not_done = wait(futures, timeout=worker_timeout_sec)
                        for f in done:
                            exc = f.exception()
                            if exc:
                                if isinstance(exc, (AMQPConnectionError, AMQPChannelError, AMQPError)):
                                    logger.info("In the previous pull, at least one RabbitMQ disconnection happened.")
                                    disconnectionHappened = True
                                    break
                    except KeyboardInterrupt:
                        logger.warning(f"Interrupted while waiting for workers.")
                        shutdown_event.set()
                        for f in futures:
                            f.cancel()
                        raise
                if disconnectionHappened:
                    logger.info("In the previous pull, one RabbitMQ disconnection happened. The entire pull will be discard and try again.")
                    continue
                for amn, ts in batch_max_time.items():
                    mn_latest_map[amn] = ts.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
                save_mn_latest_map(mn_latest_map)
                logger.info("Cycle completed.")

            except KeyboardInterrupt:
                logger.warning(f"Polling interrupted. Exiting.")
                shutdown_event.set()
                break

            except Exception as poll_error:
                logger.error(f"[ERROR] Polling failed: {poll_error}")

            # --- Sleep regardless success or failure to maintain poll interval ---
            elapsed = time.perf_counter() - cycle_start
            sleep_time = max(0, PULL_INTERVAL - elapsed)
            if sleep_time > 0:
                shutdown_event.wait(sleep_time)

        channel_pool.close()
        if pg_pool:
            pg_pool.closeall()

"""
   Periodically to pull new modified records from the solr server and submit the RabbitMQ messages
   to the notification service
"""
def poll_and_submit_notification():


if __name__ == "__main__":
    setup_logging()
    logger.info("pull_systemmeta_submitter started")
    non_data_formats = load_non_data_format_ids()
    pg_pool = pool.ThreadedConnectionPool(
            minconn = 1,
            maxconn = DB_CONNECTION_POOL_SIZE,
            **DB_CONFIG
    )
    while True:
        try:
            channel_pool = AMQPStormChannelPool(
                    RABBITMQ_URL, RABBITMQ_PORT_NUMBER,
                    RABBITMQ_USERNAME, RABBITMQ_PASSWORD,
                    MAX_WORKERS
            )
            break
        except AMQPError as amqp_err:
                logger.error(f"[ERROR] AMQPStorm error while initializing the RabbitMQ channel pool: {amqp_err}. It will retry again.")
                # Sleep 10 seconds
                time.sleep(10)
    poll_and_submit_index(non_data_formats)