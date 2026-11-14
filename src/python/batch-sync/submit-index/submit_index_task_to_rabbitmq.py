import argparse
import io
import threading
import concurrent.futures
import os
import pika
import pika.exceptions
import time  # For potential delays or timeouts
from datetime import datetime, timezone

# --- Configuration ---
# If the type of the index task is systemMetacat_change_only
SYSMETA_CHANGE_ONLY = False
# Path to the output file for logging successfully processed PIDs
RESULTS_FILE_BASEPATH = "/var/metacat/.metacat/reindex-script/sysmeta-processed"
# RabbitMQ URL
RABBITMQ_HOST = "localhost"
# RabbitMQ port number
RABBITMQ_PORT_NUMBER = 5672
# The time gap between two submission in seconds
SUBMISSION_GAP_SEC = 0.5
# Number of worker threads. Must stay 1: all workers share one pika channel, and pika's
# BlockingConnection is not thread-safe.
MAX_WORKERS = 1
# Path to the input file containing PIDs, one PID per line
PIDS_FILE_PATH = "/var/metacat/.metacat/reindex-script/pids_to_process.txt"
# Priority of the message. We set it 0, which is the lowest one, since this should be run in background
PRIORITY = 0
# Request timeout in seconds
REQUEST_TIMEOUT = 30
# RabbitMQ queue configuration. They shouldn't be changed
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"

# --- End Configuration ---

# Lock for thread-safe writing to the results file
results_file_lock = threading.Lock()

def open_rabbitmq_channel(username, password, hostname):
    """
    Connect to RabbitMQ and declare the exchange and queue that index tasks are published to.

    Returns a (connection, channel) tuple. The caller must close the connection when done.
    If setup fails after the connection is opened, the connection is closed before the
    exception is re-raised.
    """
    connection = None
    try:
        credentials = pika.PlainCredentials(username, password)
        rmq_host = hostname if hostname else RABBITMQ_HOST
        print(f"Trying to create a RabbitMQ channel at: {rmq_host}:{RABBITMQ_PORT_NUMBER} with "
              f"username: {username} and password: {'*' * len(password)}")
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=rmq_host, port=RABBITMQ_PORT_NUMBER,
                                      credentials=credentials))
        channel = connection.channel()
        # Declare the exchange before binding to it; on a fresh broker it does not exist yet.
        # The arguments must match those used by Metacat and dataone-indexer.
        channel.exchange_declare(exchange=EXCHANGE_NAME, exchange_type="direct", durable=True)
        channel.queue_declare(queue=QUEUE_NAME, durable=True, arguments={'x-max-priority': 10})
        channel.queue_bind(exchange=EXCHANGE_NAME,
                           queue=QUEUE_NAME,
                           routing_key=ROUTING_KEY)
    except Exception as e:
        print(f"[ERROR] Could not create a RabbitMQ channel: {e!r}")
        if connection is not None and connection.is_open:
            connection.close()
        raise
    return connection, channel

def process_pid_wrapper(pid, channel, results_path):
    """
    Processes a single PID:
    1. Construct the rabbitmq message
    2. Publish the message to the rabbitmq service
    3. Add PID to the results file at results_path

    Returns the PID on success, or None if publishing failed.
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
        # 3. Add PID to results file (thread-safe)
        try:
            with results_file_lock:
                with open(results_path, "a") as f:
                    f.write(f"{pid}\n")
            print(f"  [{thread_name}] Successfully processed and logged PID: {pid}")
            return pid
        except IOError as ioe:
            print(f"[ERROR] [{thread_name}] Could not write to results file {results_path} for PID {pid}: {ioe}")
            os._exit(1)

    # repr() is used because str() of many pika exceptions is empty.
    except pika.exceptions.AMQPConnectionError as conn_err:
        print(f"[ERROR] [{thread_name}] RabbitMQ connection error for PID {pid}: {conn_err!r}")
    except pika.exceptions.AMQPChannelError as chan_err:
        print(f"[ERROR] [{thread_name}] RabbitMQ channel error for PID {pid}: {chan_err!r}")
    except pika.exceptions.AMQPError as amqp_err:
        print(f"[ERROR] [{thread_name}] RabbitMQ error for PID {pid}: {amqp_err!r}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] An unexpected error occurred while processing PID {pid}: {e}")

    return None  # Return None if any step failed

def parse_args(argv=None):
    """
    Parse command-line arguments.

    :param argv: the arguments, excluding the program name; defaults to sys.argv[1:]
    :return: an argparse.Namespace with rabbitmq_username, rabbitmq_password and
        rabbitmq_hostname (RABBITMQ_HOST if not given)
    """
    parser = argparse.ArgumentParser(
        description=f"Publish an index task to RabbitMQ for each PID in {PIDS_FILE_PATH}.")
    parser.add_argument("rabbitmq_username", help="RabbitMQ username")
    parser.add_argument("rabbitmq_password", help="RabbitMQ password")
    parser.add_argument("rabbitmq_hostname", nargs="?", default=RABBITMQ_HOST,
                        help=f"RabbitMQ hostname (default: {RABBITMQ_HOST})")
    return parser.parse_args(argv)


def build_results_path(basepath, now=None):
    """
    Return the results file path for a run: basepath + "_" + the UTC time + ".txt".
    Colons in the time are replaced by dashes, e.g. "<basepath>_2026-11-13T10-00-00.txt".

    :param basepath: the results file path without the timestamp suffix
    :param now: the run time, a timezone-aware datetime (default: the current time)
    """
    now = now or datetime.now(timezone.utc)
    safe_ts = now.astimezone(timezone.utc).strftime("%Y-%m-%dT%H-%M-%S")
    return f"{basepath}_{safe_ts}.txt"


def read_pids(path):
    """
    Read PIDs from a file, one per line. Surrounding whitespace is stripped, and blank lines
    are skipped.

    :param path: path to the PIDs file
    :return: list of PIDs, in file order
    :raises OSError: if the file cannot be read (FileNotFoundError if it does not exist)
    """
    with open(path, "r") as f:
        return [line.strip() for line in f if line.strip()]


def main(argv=None):
    """
    Main function to read PIDs from a file and process them using a thread pool.

    :param argv: command-line arguments, excluding the program name; defaults to sys.argv[1:]
    """
    args = parse_args(argv)
    rabbitmq_username = args.rabbitmq_username
    rabbitmq_password = args.rabbitmq_password
    rabbitmq_hostname = args.rabbitmq_hostname
    print(f"RabbitMQ username: {rabbitmq_username}")
    print(f"RabbitMQ hostname: {rabbitmq_hostname}")

    results_path = build_results_path(RESULTS_FILE_BASEPATH)

    # Ensure the directory for results_path exists
    try:
        results_dir = os.path.dirname(results_path)
        if results_dir:  # Ensure it's not an empty string if results_path is just a filename
            os.makedirs(results_dir, exist_ok=True)
        print(f"Results will be logged to: {results_path}")
    except OSError as e:
        print(f"[ERROR] Could not create directory for results file {results_path}: {e}. Please check permissions.")
        return

    try:
        all_pids = read_pids(PIDS_FILE_PATH)
        if not all_pids:
            print(f"No PIDs found in {PIDS_FILE_PATH}. Exiting.")
            return
        print(f"Found {len(all_pids)} PIDs to process from {PIDS_FILE_PATH}.")
    except FileNotFoundError:
        print(f"[ERROR] PIDs file not found: {PIDS_FILE_PATH}. Please create it with one PID per line.")
        return
    except Exception as e:
        print(f"[ERROR] Could not read PIDs file {PIDS_FILE_PATH}: {e}")
        return

    connection, channel = open_rabbitmq_channel(rabbitmq_username, rabbitmq_password,
                                                rabbitmq_hostname)
    try:
        processed_count = 0
        failed_count = 0
        total_pids = len(all_pids)

        print(f"Starting processing with {MAX_WORKERS} worker threads...")
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='PIDProcessor') as executor:
            # Submit all PIDs to the executor
            future_to_pid = {}
            for pid in all_pids:
                future = executor.submit(process_pid_wrapper, pid, channel, results_path)
                future_to_pid[future] = pid
                time.sleep(SUBMISSION_GAP_SEC)

            for future in concurrent.futures.as_completed(future_to_pid):
                pid_submitted = future_to_pid[future]
                try:
                    result_pid = future.result()  # result is the PID if successful, None otherwise
                    if result_pid:
                        processed_count += 1
                    else:
                        failed_count += 1
                except Exception as exc:
                    print(f"[ERROR] PID {pid_submitted} generated an unexpected exception in future: {exc}")
                    failed_count += 1

                # Optional: print progress
                current_done = processed_count + failed_count
                if current_done % (MAX_WORKERS * 2) == 0 or current_done == total_pids:
                    print(f"Progress: {current_done}/{total_pids} PIDs handled. Success: {processed_count}, Failed: {failed_count}")
    finally:
        if connection.is_open:
            connection.close()

    print(f"\n--- Processing Complete ---")
    print(f"Total PIDs from file: {total_pids}")
    print(f"Successfully processed and logged: {processed_count}")
    print(f"Failed to process: {failed_count}")
    if processed_count > 0:
        print(f"Successfully processed PIDs have been logged to: {results_path}")

if __name__ == "__main__":
    main()
