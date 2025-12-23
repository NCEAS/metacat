# This script pulls new records from the systemmetadata table periodically and submit the
# information as the index tasks to the RabbitMQ service.
# Needed libraries:
# pip3 install psycopg2-binary
# pip3 install amqpstorm
# You may run this script on the background by this command:
# nohup python3 pull_systemmeta.py > pull_systemmeta.log 2>&1 &

import psycopg2
import os
import threading
import concurrent.futures
import queue
import time
import requests
import xml.etree.ElementTree as ET
from psycopg2 import pool
from amqpstorm import Connection
from amqpstorm.exception import AMQPError
from datetime import datetime
from concurrent.futures import wait, ALL_COMPLETED

# --- Configuration ---
# Replace with your RabbitMQ and database credentials
RABBITMQ_USERNAME = "guest"
RABBITMQ_PASSWORD = "guest"
DB_USERNAME = "tao"
DB_PASSWORD = "your_db_password"
# Number of worker threads to submit index tasks to RabbitMQ
# The pool_size of the rabbitmq channel pool is using it as well.
# The number must be less than those settings:
# the max number of channels connection to rabbitmq (2047) and the number of the processor core number.
MAX_WORKERS = 5
RABBITMQ_URL = "localhost"
RABBITMQ_PORT_NUMBER = 5672
DB_DATABASE_NAME = "metacat"
DB_HOST_NAME = "localhost"
DB_PORT_NUMBER = 5432
POLL_INTERVAL = 1  # second
MAX_ROWS = 4000
LAST_TIMESTAMP_FILE = "last_timestamp"
DB_CONNECTION_POOL_SIZE = 3
DOCUMENTS_DIR = "/var/metacat/documents"
DATA_DIR = "/var/metacat/data"
CHECK_FILE_WAIT_MILLISECONDS = 50
CHECK_FILE_MAX_ATTEMPTS = 200
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"
resourcemap_format_list = ["http://www.openarchives.org/ore/terms", "http://www.w3.org/TR/rdf-syntax-grammar"]
pg_pool = None
FORMATS_URL = "https://cn.dataone.org/cn/v2/formats"

"""
    Fetch DataONE format XML and return a list of formatId values
    whose type is not 'DATA'.
"""
def load_non_data_format_ids():
    non_data_format_ids = []

    resp = requests.get(FORMATS_URL, timeout=30)
    resp.raise_for_status()

    root = ET.fromstring(resp.text)

    # Detect namespace if present
    ns = {}
    if root.tag.startswith("{"):
        ns_uri = root.tag.split("}")[0].strip("{")
        ns = {"d": ns_uri}

    # Find all format entries
    formats = root.findall(".//d:format", ns) if ns else root.findall(".//format")

    for fmt in formats:
        if ns:
            fmt_id = fmt.findtext("d:formatId", default="", namespaces=ns)
            fmt_type = fmt.findtext("d:type", default="", namespaces=ns)
        else:
            fmt_id = fmt.findtext("formatId", default="")
            fmt_type = fmt.findtext("type", default="")

        if fmt_id and fmt_type.upper() != "DATA":
            non_data_format_ids.append(fmt_id)

    return non_data_format_ids

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
        self._initialize_pool()

    def _initialize_pool(self):
        with self._lock:
            self._close_all()
            self._connection = Connection(self.host, self.username, self.password, port=self.port)
            for _ in range(self.pool_size):
                self._put_new_channel_into_queue()

    def _is_healthy(self):
        return self._connection and self._connection.is_open

    def acquire_channel(self):
        with self._lock:
            if not self._is_healthy():
                print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [CHANNEL POOL] Connection lost. Reinitializing.")
                self._initialize_pool()
        try:
            for _ in range(self.pool_size):
                channel = self._channels.get(timeout=5)
                if channel and channel.is_open:
                    return channel
                else:
                    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [CHANNEL POOL] Found "
                     "closed channel. Creating a new one.")
                    with self._lock:
                        self._put_new_channel_into_queue()
            raise Exception("No healthy AMQPStorm channels available in the pool.")
        except queue.Empty:
            raise Exception("No available AMQPStorm channels in the pool.")


    def release_channel(self, channel):
        if channel and channel.is_open:
            try:
                self._channels.put(channel, timeout=5)
            except queue.Full:
                pass  # Drop if full

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

    # Generate a new RabbitMQ channel and put it into the queue
    def _put_new_channel_into_queue(self):
        new_channel = self._connection.channel()
        new_channel.queue.declare(QUEUE_NAME, durable=True, arguments={"x-max-priority": 10})
        new_channel.queue.bind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY)
        self._channels.put(new_channel)

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
            finally:
                if channel:
                    channel_pool.release_channel(channel)
        else:
            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [{thread_name}] No GUID "
            "found in the query")
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
    return datetime.now()  # fallback

def save_last_timestamp(ts: datetime):
    try:
        with open(LAST_TIMESTAMP_FILE, "w") as f:
            f.write(ts.strftime('%Y-%m-%d %H:%M:%S.%f'))
    except Exception as e:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [ERROR] Failed to write last_timestamp file: {e}")

def poll_and_submit():
    global pg_pool
    last_timestamp = load_last_timestamp()
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] The last_timestamp from the previous process is {last_timestamp}")
    channel_pool = AMQPStormChannelPool(
        RABBITMQ_URL, RABBITMQ_PORT_NUMBER, RABBITMQ_USERNAME, RABBITMQ_PASSWORD, MAX_WORKERS
    )

    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='PullProcessor') as executor:
        try:
            while True:
                print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Start to pull new records from the systemmetadata table.")
                cycle_start = time.time()
                futures = []
                max_timestamp_in_batch = last_timestamp

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

                        for guid, object_format, doc_id, modified_time in rows:
                            futures.append(executor.submit(process_pid_wrapper, channel_pool,
                            guid, object_format, doc_id))
                            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] The modified_time from database is {modified_time}")
                            max_timestamp_in_batch = max(max_timestamp_in_batch, modified_time)
                            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] The max time in batch is {max_timestamp_in_batch}")

                except Exception as poll_error:
                    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] [ERROR] Polling failed: {poll_error}")
                finally:
                    if conn:
                        pg_pool.putconn(conn)

                if futures:
                    wait(futures, return_when=ALL_COMPLETED)
                    last_timestamp = max_timestamp_in_batch
                    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Save the last_timestamp {last_timestamp} to file")
                    save_last_timestamp(last_timestamp)

                elapsed = time.time() - cycle_start
                sleep_time = max(0, POLL_INTERVAL - elapsed)
                if sleep_time > 0:
                    time.sleep(sleep_time)

        except KeyboardInterrupt:
            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Polling interrupted. Exiting.")
        finally:
            channel_pool.close()
            if pg_pool:
                pg_pool.closeall()

if __name__ == "__main__":
    pg_pool = pool.ThreadedConnectionPool(
            minconn = 1,
            maxconn = DB_CONNECTION_POOL_SIZE,
            **DB_CONFIG
    )
    poll_and_submit()