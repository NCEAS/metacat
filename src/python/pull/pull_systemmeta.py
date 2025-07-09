# This script is a listener of a trigger on the Metacat's systemmetadata table.
# It parses the payload from the trigger and sends a index task to RabbitMQ.
# Needed libraries:
# pip3 install psycopg2-binary
# pip3 install amqpstorm

import psycopg2
import select
import threading
import concurrent.futures
import queue
import time
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
# Number of worker threads to listen the database events
# The pool_size of the rabbitmq channel pool and database connection pool are using it as well.
# The number must be less than those settings: the max number of the database connection (100-200),
# the max number of channels connection to rabbitmq (2047) and the number of the processor core number.
MAX_WORKERS = 5
RABBITMQ_URL = "localhost"
RABBITMQ_PORT_NUMBER = 5672
DB_DATABASE_NAME = "metacat"
DB_HOST_NAME = "localhost"
DB_PORT_NUMBER = 5432
POLL_INTERVAL = 60  # seconds
MAX_ROWS = 2000
LAST_TIMESTAMP_FILE = "last_timestamp"
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"
resourcemap_format_list = ["http://www.openarchives.org/ore/terms", "http://www.w3.org/TR/rdf-syntax-grammar"]
pg_pool = None

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
                print("[CHANNEL POOL] Connection lost. Reinitializing.")
                self._initialize_pool()
        try:
            for _ in range(self.pool_size):
                channel = self._channels.get(timeout=5)
                if channel and channel.is_open:
                    return channel
                else:
                    print("[CHANNEL POOL] Found closed channel. Creating a new one.")
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

"""
    Processes a single PID:
       1 Construct the rabbitmq message
       2 Publish the message to the rabbitmq service
"""
def process_pid_wrapper(channel_pool, guid, object_format, docid):
    thread_name = threading.current_thread().name
    try:
        index_type = 'create'
        priority = 4
        if object_format and object_format in resourcemap_format_list:
            priority = 3
        if guid:
            print(f"[{thread_name}] Processing PID: {guid} with type: {index_type}, docid: {doc_id}, priority: {priority}")
            headers = {'index_type': index_type, 'id': guid, 'doc_id': doc_id}
            message = ''
            channel = None
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
            print(f"[{thread_name}] No GUID found in the query")
    except AMQPError as amqp_err:
        print(f"[ERROR] [{thread_name}] AMQPStorm error while processing PID {guid}: {amqp_err}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] Unexpected error while processing PID {guid}: {e}")
    return None

def load_last_timestamp():
    if os.path.exists(LAST_TIMESTAMP_FILE):
        try:
            with open(LAST_TIMESTAMP_FILE, "r") as f:
                content = f.read().strip()
                return datetime.fromisoformat(content)
        except Exception as e:
            print(f"[WARN] Could not read or parse timestamp file: {e}")
    return datetime.now()  # fallback

def save_last_timestamp(ts: datetime):
    try:
        with open(LAST_TIMESTAMP_FILE, "w") as f:
            f.write(ts.isoformat())
    except Exception as e:
        print(f"[ERROR] Failed to write last_timestamp file: {e}")

def poll_and_submit():
    global pg_pool
    last_timestamp = load_last_timestamp()
    channel_pool = AMQPStormChannelPool(
        RABBITMQ_URL, RABBITMQ_PORT_NUMBER, RABBITMQ_USERNAME, RABBITMQ_PASSWORD, MAX_WORKERS
    )

    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='TriggerProcessor') as executor:
        try:
            while True:
                cycle_start = time.time()
                futures = []
                max_timestamp_in_batch = last_timestamp

                try:
                    conn = pg_pool.getconn()
                    with conn.cursor() as cur:
                        cur.execute(f"""
                            SELECT sm.guid, sm.object_format, i.docid || '.' || i.rev AS docid, sm
                            .date_sys_metadata_modified
                            FROM systemmetadata sm
                            JOIN identifier i ON sm.guid = i.guid
                            WHERE sm.date_sys_metadata_modified > %s
                            ORDER BY sm.date_sys_metadata_modified ASC
                            LIMIT {MAX_ROWS}
                        """, (last_timestamp,))
                        rows = cur.fetchall()

                        for guid, object_format, docid, modified_time in rows:
                            futures.append(executor.submit(process_pid_wrapper, channel_pool, guid, object_format, docid))
                            max_timestamp_in_batch = max(max_timestamp_in_batch, modified_time)

                except Exception as poll_error:
                    print(f"[ERROR] Polling failed: {poll_error}")
                finally:
                    if conn:
                        pg_pool.putconn(conn)

                if futures:
                    wait(futures, return_when=ALL_COMPLETED)
                    last_timestamp = max_timestamp_in_batch
                    save_last_timestamp(last_timestamp)

                elapsed = time.time() - cycle_start
                sleep_time = max(0, POLL_INTERVAL - elapsed)
                if sleep_time > 0:
                    time.sleep(sleep_time)

        except KeyboardInterrupt:
            print("Polling interrupted. Exiting.")
        finally:
            channel_pool.close()
            if pg_pool:
                pg_pool.closeall()

if __name__ == "__main__":
    pg_pool = pool.ThreadedConnectionPool(
            minconn = 1,
            maxconn = MAX_WORKERS + 2,  # extra room
            **DB_CONFIG
    )
    poll_and_submit()