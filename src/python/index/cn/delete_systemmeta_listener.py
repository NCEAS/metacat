# This script is a listener of a trigger on the Metacat's systemmetadata table.
# It parses the payload from the trigger and sends a index task to RabbitMQ.
# Now it only handles the delete events
# Needed libraries:
# pip3 install psycopg2-binary
# pip3 install amqpstorm
# You may run this script on the background by this command:
# nohup python3 delete_systemmeta_listener.py > delete_systemmeta_listener.log 2>&1 &

import psycopg2
import psycopg2.extensions
import select
import json
import threading
import concurrent.futures
import time

from amqpstorm.exception import AMQPError

# Import *reused* components from pull_systemmetadata_submitter.py
from pull_systemmeta_submitter import (
    DB_CONFIG,
    AMQPStormChannelPool,
    RABBITMQ_URL,
    RABBITMQ_PORT_NUMBER,
    RABBITMQ_USERNAME,
    RABBITMQ_PASSWORD,
    ROUTING_KEY,
    EXCHANGE_NAME,
)
# Number of worker threads to listen the database events. Since it only handle the delete actions,
# it can be one.
MAX_WORKERS = 1

# They are seconds
SELECT_TIMEOUT = 5
RECONNECT_DELAY = 5
MAX_RECONNECT_DELAY = 60


"""
    Note: this method only handles the delete actions.
    Parse the payload and submit the index task based the payload information
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
        if guid:
            if index_type == 'delete':
                print(f"[{thread_name}] Processing PID: {guid} with type: {index_type}, priority: {priority}")
                headers = {'index_type': index_type, 'id': guid}
                message = ''
                # 2.2 Publish the message to the rabbitmq service
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
                print(f"[{thread_name}] This script only handles the delete events, rather than {index_type}")
        else:
            print(f"[{thread_name}] No GUID found in payload: {payload}")
    except json.JSONDecodeError:
        print(f"[ERROR] [{thread_name}] Invalid JSON: {notify.payload}")
    except AMQPError as amqp_err:
        print(f"[ERROR] [{thread_name}] AMQPStorm error while processing PID {guid}: {amqp_err}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] Unexpected error while processing PID {guid}: {e}")

def connect_postgres():
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)

    with conn.cursor() as cur:
        cur.execute("LISTEN systemmetadata_event;")

    print("Listening on PostgreSQL channel 'systemmetadata_event'")
    return conn

def create_channel_pool():
    print("Creating RabbitMQ channel pool")
    return AMQPStormChannelPool(
        RABBITMQ_URL,
        RABBITMQ_PORT_NUMBER,
        RABBITMQ_USERNAME,
        RABBITMQ_PASSWORD,
        MAX_WORKERS
    )

"""
   Method to listen the database tigger and handle the events in a multiple-thread way
"""
def listen_and_submit():
    conn = None
    channel_pool = None
    backoff = RECONNECT_DELAY

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=MAX_WORKERS,
        thread_name_prefix='TriggerProcessor'
    ) as executor:

        try:
            while True:
                try:
                    # Ensure PostgreSQL connection
                    if conn is None or conn.closed:
                        print("PostgreSQL disconnected, reconnecting...")
                        conn = connect_postgres()
                        backoff = RECONNECT_DELAY

                    # Ensure RabbitMQ pool
                    if channel_pool is None:
                        print("RabbitMQ channel pool unavailable, recreating...")
                        channel_pool = create_channel_pool()

                    ready, _, _ = select.select([conn], [], [], SELECT_TIMEOUT)
                    if not ready:
                        continue
                    conn.poll()
                    while conn.notifies:
                        notify = conn.notifies.pop(0)
                        executor.submit(
                            process_pid_wrapper,
                            channel_pool,
                            notify
                        )
                except psycopg2.OperationalError as e:
                    print(f"[ERROR] PostgreSQL error: {e}")
                    try:
                        conn.close()
                    except Exception:
                        pass
                    conn = None
                    time.sleep(backoff)
                    backoff = min(backoff * 2, MAX_RECONNECT_DELAY)
                except Exception as e:
                    print(f"[ERROR] Listener loop error: {e}")
                    time.sleep(2)
        except KeyboardInterrupt:
            print("Interrupted by user, shutting down")
        finally:
            if conn:
                try:
                    conn.close()
                except Exception:
                    pass
            if channel_pool:
                try:
                    channel_pool.close()
                except Exception:
                    pass

if __name__ == "__main__":
    listen_and_submit()
