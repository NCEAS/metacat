# This script is a listener of a trigger on the systemmetadata table.
# It prints out the payload from the tigger and send the information to RabbitMQ.
# You need to install psycopg2 first: pip install psycopg2-binary
import psycopg2
import select
import json
import threading
import pika
import concurrent.futures
import time


# --- Configuration ---
# Replace with your RabbitMQ credentials
RABBITMQ_USERNAME = "guest"
RABBITMQ_PASSWORD = "guest"
DB_USERNAME = "tao"
DB_PASSWORD = "your_db_password"
# Number of worker threads
MAX_WORKERS = 5
# If the type of the index task is systemMetacat_change_only
SYSMETA_CHANGE_ONLY = False
# RabbitMQ URL
RABBITMQ_URL = "localhost"
# RabbitMQ port number
RABBITMQ_PORT_NUMBER = 5672
DB_DATABASE_NAME = "metacat"
DB_HOST_NAME = "localhost"
DB_PORT_NUMBER = 5432
# The time gap between two submission in seconds
SUBMISSION_GAP_SEC = 0.5
# Priority of the message. We set it 0, which is the lowest one, since this should be run in background
PRIORITY = 0
# Request timeout in seconds
REQUEST_TIMEOUT = 30
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"


# Database connection parameters
DB_CONFIG = {
    'dbname': DB_DATABASE_NAME,
    'user': DB_USERNAME,
    'password': DB_PASSWORD,
    'host': DB_HOST_NAME,
    'port': DB_PORT_NUMBER
}

def get_rabbitmq_channel(username, password):
    try:
        credentials = pika.PlainCredentials(username, password)
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=RABBITMQ_URL, port=RABBITMQ_PORT_NUMBER,
                                      credentials=credentials))
        channel = connection.channel()
        channel.queue_declare(queue=QUEUE_NAME, durable=True, arguments={'x-max-priority': 10})
        channel.queue_bind(exchange=EXCHANGE_NAME,
                           queue=QUEUE_NAME,
                           routing_key=ROUTING_KEY)
    except Exception as e:
        print(f"[ERROR] Could not create a RabbitMQ channel: {e}")
        raise e
    return channel

def process_pid_wrapper(pid, channel):
    """
    Processes a single PID:
    1. Construct the rabbitmq message
    2. Publish the message to the rabbitmq service
    3. Add PID to results file
    """
    thread_name = threading.current_thread().name
    try:
        # 1 Construct the rabbitmq message
        if (SYSMETA_CHANGE_ONLY):
            index_type = "sysmeta"
        else:
            index_type = "create"
        print(f"[{thread_name}] Processing PID: {pid} with the type of index task: {index_type}")
        headers={'index_type': index_type, 'id': pid}
        properties = pika.BasicProperties(headers=headers, priority=PRIORITY)
        message = ''
        # 2 Publish the message to the rabbitmq service
        channel.basic_publish(
            exchange=EXCHANGE_NAME,
            routing_key=ROUTING_KEY,
            body=message,
            properties=properties
        )

    except channel.exceptions.HTTPError as http_err:
        print(f"[ERROR] [{thread_name}] HTTP error for PID {pid} at URL {http_err.request.url}: {http_err}")
    except channel.exceptions.ConnectionError as conn_err:
        print(f"[ERROR] [{thread_name}] Connection error for PID {pid} (URL: {conn_err.request.url if conn_err.request else 'N/A'}): {conn_err}")
    except channel.exceptions.Timeout as timeout_err:
        print(f"[ERROR] [{thread_name}] Timeout for PID {pid} (URL: {timeout_err.request.url if timeout_err.request else 'N/A'}): {timeout_err}")
    except channel.exceptions.RequestException as req_err:
        print(f"[ERROR] [{thread_name}] General request error for PID {pid}: {req_err}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] An unexpected error occurred while processing PID {pid}: {e}")

    return None  # Return None if any step failed

def listen_and_submit():
    # Connect to PostgreSQL
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)

    cur = conn.cursor()
    cur.execute("LISTEN core_db_event;")
    print("Listening for PostgreSQL notifications on channel 'core_db_event'...")

    # Set up RabbitMQ channel
    channel = get_rabbitmq_channel(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)

    # Set up thread pool
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='TriggerProcessor') as executor:
        try:
            while True:
                if select.select([conn], [], [], 5) == ([], [], []):
                    continue  # Timeout
                conn.poll()
                while conn.notifies:
                    notify = conn.notifies.pop(0)
                    try:
                        payload = json.loads(notify.payload)
                        guid = payload.get("record", {}).get("guid")
                        if guid:
                            print(f"[LISTENER] Submitting task for PID: {guid}")
                            executor.submit(process_pid_wrapper, guid, channel)
                            time.sleep(SUBMISSION_GAP_SEC)
                        else:
                            print(f"[LISTENER] No GUID found in payload: {payload}")
                    except json.JSONDecodeError:
                        print("[LISTENER] Invalid JSON payload received")
        except KeyboardInterrupt:
            print("Listener interrupted. Exiting.")
        finally:
            cur.close()
            conn.close()

if __name__ == "__main__":
    listen_and_submit()
