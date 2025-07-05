# This script is a listener of a trigger on the systemmetadata table.
# It prints out the payload from the trigger and send the information to RabbitMQ.
# You need to install psycopg2 first: pip install psycopg2-binary
import psycopg2
import select
import json
import threading
import pika
import concurrent.futures
import queue
import pika.exceptions

# --- Configuration ---
# Replace with your RabbitMQ credentials
RABBITMQ_USERNAME = "guest"
RABBITMQ_PASSWORD = "guest"
DB_USERNAME = "tao"
DB_PASSWORD = "your_db_password"
# Size of RabbitMQ channel pool
POOL_SIZE = 5
# Number of worker threads
MAX_WORKERS = 5
# RabbitMQ URL
RABBITMQ_URL = "localhost"
# RabbitMQ port number
RABBITMQ_PORT_NUMBER = 5672
DB_DATABASE_NAME = "metacat"
DB_HOST_NAME = "localhost"
DB_PORT_NUMBER = 5432
# Request timeout in seconds
REQUEST_TIMEOUT = 30
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"
resourcemap_format_list = ["http://www.openarchives.org/ore/terms", "http://www.w3.org/TR/rdf-syntax-grammar"]

class ThreadSafeChannelPool:
    def __init__(self, username, password, pool_size=5):
        self.username = username
        self.password = password
        self.pool_size = pool_size
        self._lock = threading.Lock()
        self._connection = None
        self._channels = queue.Queue(maxsize=pool_size)
        self._create_connection_and_channels()

    def _create_connection(self):
        credentials = pika.PlainCredentials(self.username, self.password)
        parameters = pika.ConnectionParameters(
            host=RABBITMQ_URL,
            port=RABBITMQ_PORT_NUMBER,
            credentials=credentials
        )
        return pika.BlockingConnection(parameters)

    def _create_channel(self):
        channel = self._connection.channel()
        channel.queue_declare(queue=QUEUE_NAME, durable=True, arguments={'x-max-priority': 10})
        channel.queue_bind(exchange=EXCHANGE_NAME, queue=QUEUE_NAME, routing_key=ROUTING_KEY)
        return channel

    def _create_connection_and_channels(self):
        with self._lock:
            self._close_all()
            self._connection = self._create_connection()
            for _ in range(self.pool_size):
                self._channels.put(self._create_channel())

    def _is_healthy(self):
        return self._connection and self._connection.is_open

    def acquire_channel(self):
        with self._lock:
            if not self._is_healthy():
                print("[CHANNEL POOL] Connection lost. Reconnecting and recreating channel pool.")
                self._create_connection_and_channels()
        try:
            return self._channels.get(timeout=5)
        except queue.Empty:
            raise Exception("No available RabbitMQ channels in the pool.")

    def release_channel(self, channel):
        with self._lock:
            if channel and channel.is_open:
                try:
                    self._channels.put(channel, timeout=5)
                except queue.Full:
                    pass  # Drop it if pool is full

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

"""
    1. Parse the payload from the trigger
    2. Processes a single PID:
       2.1 Construct the rabbitmq message
       2.2 Publish the message to the rabbitmq service
    """
def process_pid_wrapper(channel_pool, notify):
    thread_name = threading.current_thread().name
    try:
        index_type = 'create'
        priority = 4
        # 1. Parse the payload from the trigger
        payload = json.loads(notify.payload)
        guid = payload.get("pid")
        action = payload.get("action")
        if action and action.lower() == 'delete':
            index_type = 'delete'
        doc_id = payload.get("docid")
        object_format = payload.get("record", {}).get("object_format")
        if object_format and object_format in resourcemap_format_list:
            priority = 3

        if guid:
            # 2.1 Construct the rabbitmq message
            print(f"[{thread_name}] Processing PID: {guid} with the type of index task: {index_type} and docid: {doc_id} and priority: {priority}")
            headers = {'index_type': index_type, 'id': guid, 'doc_id': doc_id}
            properties = pika.BasicProperties(headers=headers, priority=priority)
            message = ''
            # 2.2 Publish the message to the rabbitmq service
            channel = None
            try:
                channel = channel_pool.acquire_channel()
                channel.basic_publish(
                    exchange=EXCHANGE_NAME,
                    routing_key=ROUTING_KEY,
                    body=message,
                    properties=properties
                )
            finally:
                if channel:
                    channel_pool.release_channel(channel)
        else:
            print(f"[{thread_name}] No GUID found in payload: {payload}")

    except json.JSONDecodeError:
        print(f"[ERROR] [{thread_name}] Invalid JSON payload received: {notify.payload}")
    except pika.exceptions.AMQPError as amqp_err:
        print(f"[ERROR] [{thread_name}] AMQP error while processing PID {guid}: {amqp_err}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] Unexpected error while processing PID {guid}: {e}")
    return None  # Return None if any step failed

def listen_and_submit():
    # Connect to PostgreSQL
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)

    cur = conn.cursor()
    cur.execute("LISTEN systemmetadata_event;")
    print("Listening for PostgreSQL notifications on channel 'systemmetadata_event'...")

    # Set up RabbitMQ channel manager
    channel_pool = ThreadSafeChannelPool(RABBITMQ_USERNAME, RABBITMQ_PASSWORD, POOL_SIZE)
    # Set up thread pool
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='TriggerProcessor') as executor:
        try:
            while True:
                if select.select([conn], [], [], 5) == ([], [], []):
                    continue  # Timeout
                conn.poll()
                while conn.notifies:
                    notify = conn.notifies.pop(0)
                    executor.submit(process_pid_wrapper, channel_pool, notify)
        except KeyboardInterrupt:
            print("Listener interrupted. Exiting.")
        finally:
            cur.close()
            conn.close()

if __name__ == "__main__":
    listen_and_submit()
