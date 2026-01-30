# This script populates a empty solr server from records of a CN. It pulls records from the
# systemmetadata table periodically and submit the information as the index tasks to the RabbitMQ
# service.
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

# --- Configuration ---
# The script will index all objects which have earlier modified_time than the cut_off_time
CUT_OFF_TIME = "2026-01-30 00:00:00.000"
# Replace with your RabbitMQ and database credentials
RABBITMQ_USERNAME = "guest"
RABBITMQ_PASSWORD = "guest"
DB_USERNAME = "metacat"
DB_PASSWORD = "metacat"
CN_URL = "https://cn.dataone.org/cn/v2"
RABBITMQ_URL = "localhost"
RABBITMQ_PORT_NUMBER = 5672
POLL_INTERVAL = 20  # second
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
DEFAULT_START_DATE = "2000-01-01 00:00:00.000"
FORMATS_URL = urljoin(CN_URL + "/", "formats")
NODE_URL = urljoin(CN_URL + "/", "node")


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

def load_last_timestamp():
    if os.path.exists(LAST_TIMESTAMP_FILE):
        try:
            with open(LAST_TIMESTAMP_FILE, "r") as f:
                content = f.read().strip()
                return datetime.fromisoformat(content)
        except Exception as e:
            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [WARN] Could not read or parse timestamp file: {e}")
    # fallback
    return datetime.fromisoformat(DEFAULT_START_DATE)

def save_last_timestamp(ts: datetime):
    try:
        with open(LAST_TIMESTAMP_FILE, "w") as f:
            f.write(ts.strftime('%Y-%m-%d %H:%M:%S.%f'))
    except Exception as e:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [ERROR] Failed to write last_timestamp file: {e}")

def poll_and_submit(non_data_formats):
    global pg_pool
    worker_timeout_sec = MAX_ROWS/MAX_WORKERS * 0.1
    cut_off_time_datetime = datetime.fromisoformat(CUT_OFF_TIME)
    last_timestamp = load_last_timestamp()
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] The last_timestamp from the previous process is {last_timestamp}")
    channel_pool = AMQPStormChannelPool(
        RABBITMQ_URL, RABBITMQ_PORT_NUMBER, RABBITMQ_USERNAME, RABBITMQ_PASSWORD, MAX_WORKERS
    )

    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='PullProcessor') as executor:
        while True:
            cycle_start = time.perf_counter()
            try:
                print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Start to pull new records from the systemmetadata table.")
                futures = []
                max_timestamp_in_batch = last_timestamp
                conn = None
                try:
                    conn = pg_pool.getconn()
                    with conn.cursor() as cur:
                        cur.execute(f"""
                            SELECT sm.guid, sm.object_format, i.docid || '.' || i.rev AS doc_id,
                            sm.date_modified
                            FROM systemmetadata sm
                            LEFT JOIN identifier i ON sm.guid = i.guid
                            WHERE sm.date_modified > %s
                            ORDER BY sm.date_modified ASC
                            LIMIT {MAX_ROWS}
                        """, (last_timestamp,))
                        rows = cur.fetchall()

                        if not rows:
                            print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] No more records. Existing...")
                            return

                        for guid, object_format, doc_id, modified_time in rows:
                            # Retry if format is non-DATA and doc_id is missing
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

                            if (modified_time > cut_off_time_datetime):
                                print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] All objects before the cut cut off time {CUT_OFF_TIME} have been indexed. Existing...")
                                return
                            # Skip if still missing after retries
                            if object_format in non_data_formats and not doc_id:
                                print(f"Skipping guid {guid}: doc_id not found after {DOCID_MAX_RETRIES} retries")
                                continue

                            futures.append(executor.submit(process_pid_wrapper, channel_pool,
                            guid, object_format, doc_id))
                            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] The modified_time from database is {modified_time}")
                            max_timestamp_in_batch = max(max_timestamp_in_batch, modified_time)
                            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] The max time in batch is {max_timestamp_in_batch}")
                finally:
                    if conn:
                        pg_pool.putconn(conn)
                if futures:
                    done, not_done = wait(futures, timeout=worker_timeout_sec)
                    if not_done:
                        print(f"[WARN] {len(not_done)} worker(s) hung — cancelling")
                    last_timestamp = max_timestamp_in_batch
                    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Save the last_timestamp {last_timestamp} to file")
                    save_last_timestamp(last_timestamp)
                    print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Batch completed.")
            except KeyboardInterrupt:
                print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Polling interrupted. Exiting.")
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