# This script is a listener of a trigger on the Metacat's systemmetadata table.
# It parses the payload from the trigger and sends a index task to RabbitMQ.
# Now it only handles the delete events
# Needed libraries:
# pip3 install psycopg2-binary
# pip3 install amqpstorm

import psycopg2
import select
import json
import threading
import concurrent.futures
import queue
import time
import sys
import os
from psycopg2 import pool
from amqpstorm import Connection
from amqpstorm.exception import AMQPError

# Import *reused* components from pull_systemmetadata_submitter.py
from pull_systemmeta_submitter import (
    DB_CONFIG,
    DB_CONNECTION_POOL_SIZE,
    DOCID_WAIT_SEC,
    DOCID_MAX_RETRIES,
    AMQPStormChannelPool,
    RABBITMQ_URL,
    RABBITMQ_PORT_NUMBER,
    RABBITMQ_USERNAME,
    RABBITMQ_PASSWORD,
)
# Number of worker threads to listen the database events. Since it only handle the delete actions,
# it can be one.
MAX_WORKERS = 1


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
    return None

# Method to listen the database tigger and handle the events in a multiple-thread way
def listen_and_submit():
    # Connect to PostgreSQL
    conn = psycopg2.connect(**DB_CONFIG)
    conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)
    cur = conn.cursor()
    cur.execute("LISTEN systemmetadata_event;")
    print("Listening on PostgreSQL channel 'systemmetadata_event'...")
    # Set up RabbitMQ channel pool
    channel_pool = AMQPStormChannelPool(RABBITMQ_URL, RABBITMQ_PORT_NUMBER, RABBITMQ_USERNAME, RABBITMQ_PASSWORD, MAX_WORKERS)
    # Set up thread pool
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='TriggerProcessor') as executor:
        try:
            while True:
                if select.select([conn], [], [], 5) == ([], [], []):
                    continue
                conn.poll()
                while conn.notifies:
                    notify = conn.notifies.pop(0)
                    executor.submit(process_pid_wrapper, channel_pool, notify)
        except KeyboardInterrupt:
            print("Interrupted.")
        finally:
            cur.close()
            conn.close()
            channel_pool.close()

if __name__ == "__main__":
    listen_and_submit()
