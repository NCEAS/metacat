# This script is a listener of a trigger on the Metacat's systemmetadata table.
# It parses the payload from the trigger and sends a index task to RabbitMQ.
# Now it only handles the delete events
# Needed libraries:
# pip3 install psycopg2-binary
# pip3 install amqpstorm
# You may run this script on the background by this command:
# nohup python3 delete_systemmeta_listener.py &

import concurrent.futures
import json
import logging
import psycopg2
import psycopg2.extensions
import select
import time
import threading

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
    setup_logging,
    logger as submitter_logger
)
# Number of worker threads to listen the database events. Since it only handle the delete actions,
# it can be one.
MAX_WORKERS = 1

# They are seconds
SELECT_TIMEOUT = 5
RECONNECT_DELAY = 5
MAX_RECONNECT_DELAY = 60

log_name = "delete_systemmeta_listener"
log_file = f"log/{log_name}.log"

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
                logger.debug(f"[{thread_name}] Processing PID: {guid} with type: {index_type}, priority: {priority}")
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
                logger.warn(f"[{thread_name}] This script only handles the delete events, rather than {index_type}")
        else:
            logger.warn(f"[{thread_name}] No GUID found in payload: {payload}")
    except json.JSONDecodeError:
        logger.error(f"[ERROR] [{thread_name}] Invalid JSON: {notify.payload}")
    except AMQPError as amqp_err:
        logger.error(f"[ERROR] [{thread_name}] AMQPStorm error while processing PID {guid}: {amqp_err}")
    except Exception as e:
        logger.error(f"[ERROR] [{thread_name}] Unexpected error while processing PID {guid}: {e}")

def connect_postgres():
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)

    with conn.cursor() as cur:
        cur.execute("LISTEN systemmetadata_event;")

    logger.debug("Listening on PostgreSQL channel 'systemmetadata_event'")
    return conn

def create_channel_pool():
    logger.debug("Creating RabbitMQ channel pool")
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
                        logger.debug("PostgreSQL disconnected, reconnecting...")
                        conn = connect_postgres()
                        backoff = RECONNECT_DELAY

                    # Ensure RabbitMQ pool
                    if channel_pool is None:
                        logger.debug("RabbitMQ channel pool unavailable, recreating...")
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
                    logger.error(f"[ERROR] PostgreSQL error: {e}")
                    try:
                        conn.close()
                    except Exception:
                        pass
                    conn = None
                    time.sleep(backoff)
                    backoff = min(backoff * 2, MAX_RECONNECT_DELAY)
                except Exception as e:
                    logger.error(f"[ERROR] Listener loop error: {e}")
                    time.sleep(2)
        except KeyboardInterrupt:
            logger.warn("Interrupted by user, shutting down")
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
    setup_logging(log_file=log_file, level=logging.DEBUG)
    # Create a "delete_systemmeta_listener" child logger using the same handlers
    logger = submitter_logger.getChild(log_name)
    # Remove the parent prefix
    logger.name = log_name
    logger.info("Starting to listen the delete events from the systemmetadata table...")
    listen_and_submit()
