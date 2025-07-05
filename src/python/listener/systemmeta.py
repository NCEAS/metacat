# This script is a listener of a trigger on the systemmetadata table.
# It prints out the payload from the trigger and send the information to RabbitMQ.
# You need to install psycopg2 first: pip install psycopg2-binary
import psycopg2
import select
import json
import threading
import pika
import concurrent.futures


# --- Configuration ---
# Replace with your RabbitMQ credentials
RABBITMQ_USERNAME = "guest"
RABBITMQ_PASSWORD = "guest"
DB_USERNAME = "tao"
DB_PASSWORD = "your_db_password"
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

class ThreadSafeChannel:
    def __init__(self, username, password):
        self.username = username
        self.password = password
        self._lock = threading.Lock()
        self._channel = None
        self._connection = None
        self._ensure_channel()

    def _ensure_channel(self):
        if self._connection and self._connection.is_open and self._channel and self._channel.is_open:
            return  # still good

        if self._connection:
            try:
                self._connection.close()
            except:
                pass  # suppress errors on close

        if self._channel:
            try:
                self._channel.close()
            except:
                pass  # suppress errors on close

        credentials = pika.PlainCredentials(self.username, self.password)
        self._connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_URL,
                port=RABBITMQ_PORT_NUMBER,
                credentials=credentials
            )
        )
        self._channel = self._connection.channel()
        self._channel.queue_declare(queue=QUEUE_NAME, durable=True, arguments={'x-max-priority': 10})
        self._channel.queue_bind(exchange=EXCHANGE_NAME, queue=QUEUE_NAME, routing_key=ROUTING_KEY)

    def get_channel(self):
        with self._lock:
            self._ensure_channel()
            return self._channel


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
def process_pid_wrapper(channel_manager, notify):
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
            channel = channel_manager.get_channel()
            channel.basic_publish(
                exchange=EXCHANGE_NAME,
                routing_key=ROUTING_KEY,
                body=message,
                properties=properties
            )
        else:
            print(f"[{thread_name}] No GUID found in payload: {payload}")

    except json.JSONDecodeError:
        print(f"[ERROR] [{thread_name}] Invalid JSON payload received: {notify.payload}")
    except channel.exceptions.HTTPError as http_err:
        print(f"[ERROR] [{thread_name}] HTTP error for PID {guid}: {http_err}")
    except channel.exceptions.ConnectionError as conn_err:
        print(f"[ERROR] [{thread_name}] Connection error for PID {guid}: {conn_err}")
    except channel.exceptions.Timeout as timeout_err:
        print(f"[ERROR] [{thread_name}] Timeout for PID {guid}: {timeout_err}")
    except channel.exceptions.RequestException as req_err:
        print(f"[ERROR] [{thread_name}] General request error for PID {guid}: {req_err}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] An unexpected error occurred while processing PID {guid}: {e}")
    return None  # Return None if any step failed

def listen_and_submit():
    # Connect to PostgreSQL
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)

    cur = conn.cursor()
    cur.execute("LISTEN systemmetadata_event;")
    print("Listening for PostgreSQL notifications on channel 'systemmetadata_event'...")

    # Set up RabbitMQ channel manager
    channel_manager = ThreadSafeChannel(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
    # Set up thread pool
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='TriggerProcessor') as executor:
        try:
            while True:
                if select.select([conn], [], [], 5) == ([], [], []):
                    continue  # Timeout
                conn.poll()
                while conn.notifies:
                    notify = conn.notifies.pop(0)
                    executor.submit(process_pid_wrapper, channel_manager, notify)
        except KeyboardInterrupt:
            print("Listener interrupted. Exiting.")
        finally:
            cur.close()
            conn.close()

if __name__ == "__main__":
    listen_and_submit()
