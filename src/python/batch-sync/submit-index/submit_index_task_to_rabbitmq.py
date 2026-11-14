import argparse
import io
import threading
import concurrent.futures
import os
import pika
import pika.exceptions
import sys
import time  # For potential delays or timeouts
from dataclasses import dataclass, field
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
# RabbitMQ queue configuration (defaults for --queue, --routing-key and --exchange). The
# defaults are the names Metacat and dataone-indexer use.
QUEUE_NAME = "index"
ROUTING_KEY = "index"
EXCHANGE_NAME = "dataone-index"
# The queue's x-max-priority. It must match the existing queue, or RabbitMQ refuses the
# declaration, so it is deliberately not a command-line option.
MAX_PRIORITY = 10
# Environment variable to read the RabbitMQ password from, if it is not given as an argument
PASSWORD_ENV_VAR = "RMQ_PASSWORD"

# --- End Configuration ---


@dataclass(frozen=True)
class Config:
    """Settings for one run. Built from the command line and environment by parse_args()."""
    username: str
    password: str = field(repr=False)
    host: str
    port: int
    queue: str
    exchange: str
    routing_key: str
    pids_file: str
    results_basepath: str

# Lock for thread-safe writing to the results file
results_file_lock = threading.Lock()

def open_rabbitmq_channel(config):
    """
    Connect to RabbitMQ and declare the exchange and queue that index tasks are published to.

    :param config: a Config, for the connection settings and the queue, exchange and routing
        key names
    :return: a (connection, channel) tuple. The caller must close the connection when done.
        If setup fails after the connection is opened, the connection is closed before the
        exception is re-raised.
    """
    connection = None
    try:
        credentials = pika.PlainCredentials(config.username, config.password)
        print(f"Trying to create a RabbitMQ channel at: {config.host}:{config.port} with "
              f"username: {config.username} and password: {'*' * len(config.password)}")
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host=config.host, port=config.port,
                                      credentials=credentials))
        channel = connection.channel()
        # Declare the exchange before binding to it; on a fresh broker it does not exist yet.
        # The arguments must match those used by Metacat and dataone-indexer.
        channel.exchange_declare(exchange=config.exchange, exchange_type="direct", durable=True)
        channel.queue_declare(queue=config.queue, durable=True,
                              arguments={'x-max-priority': MAX_PRIORITY})
        channel.queue_bind(exchange=config.exchange,
                           queue=config.queue,
                           routing_key=config.routing_key)
    except Exception as e:
        print(f"[ERROR] Could not create a RabbitMQ channel: {e!r}")
        if connection is not None and connection.is_open:
            connection.close()
        raise
    return connection, channel

def process_pid_wrapper(pid, channel, results_path, config):
    """
    Processes a single PID:
    1. Construct the rabbitmq message
    2. Publish the message to config.exchange with config.routing_key
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
            exchange=config.exchange,
            routing_key=config.routing_key,
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

def make_parser():
    """
    Build the command-line parser. Defaults are read from the module constants each time it
    is called.
    """
    parser = argparse.ArgumentParser(
        description="Publish an index task to RabbitMQ for each PID in a file.",
        epilog=f"The password is read from the {PASSWORD_ENV_VAR} environment variable if it "
               f"is not given as an argument. {PASSWORD_ENV_VAR} is preferred: it keeps the "
               f"password out of the process list and shell history. When using it, give the "
               f"host with --host.")
    parser.add_argument("rabbitmq_username", help="RabbitMQ username")
    parser.add_argument("rabbitmq_password", nargs="?", default=None,
                        help=f"RabbitMQ password (legacy; prefer {PASSWORD_ENV_VAR})")
    parser.add_argument("rabbitmq_hostname", nargs="?", default=None,
                        help="RabbitMQ hostname (legacy; prefer --host)")
    parser.add_argument("--host", default=None,
                        help=f"RabbitMQ hostname (default: {RABBITMQ_HOST})")
    parser.add_argument("--port", type=int, default=RABBITMQ_PORT_NUMBER,
                        help="RabbitMQ port (default: %(default)s)")
    parser.add_argument("--queue", default=QUEUE_NAME,
                        help="queue to declare and bind (default: %(default)s)")
    parser.add_argument("--exchange", default=EXCHANGE_NAME,
                        help="exchange to declare and publish to (default: %(default)s)")
    parser.add_argument("--routing-key", default=ROUTING_KEY,
                        help="routing key for binding and publishing (default: %(default)s)")
    parser.add_argument("--pids-file", default=PIDS_FILE_PATH,
                        help="file of PIDs to submit, one per line (default: %(default)s)")
    parser.add_argument("--results-basepath", default=RESULTS_FILE_BASEPATH,
                        help="results file path, to which '_<UTC timestamp>.txt' is "
                             "appended (default: %(default)s)")
    return parser


def parse_args(argv=None, environ=None):
    """
    Parse command-line arguments and the password environment variable into a Config.

    The password is taken from the rabbitmq_password argument if given, otherwise from the
    PASSWORD_ENV_VAR environment variable. If both are given and differ, the argument is used
    and a warning is printed. If neither is given, the parser exits with an error.

    :param argv: the arguments, excluding the program name; defaults to sys.argv[1:]
    :param environ: mapping to read PASSWORD_ENV_VAR from; defaults to os.environ
    :return: a Config
    """
    parser = make_parser()
    # parse_intermixed_args lets options appear between positionals, e.g. "user --host h".
    args = parser.parse_intermixed_args(argv)
    environ = os.environ if environ is None else environ
    env_password = environ.get(PASSWORD_ENV_VAR)

    if args.rabbitmq_password is not None:
        password = args.rabbitmq_password
        if env_password and env_password != password:
            print(f"[WARNING] A password was given as an argument, so {PASSWORD_ENV_VAR} is "
                  f"ignored. If the second argument was meant to be the host, use --host "
                  f"instead.", file=sys.stderr)
    elif env_password:
        password = env_password
    else:
        parser.error(f"a RabbitMQ password is required: set {PASSWORD_ENV_VAR}, or give it "
                     f"as the second argument")

    if args.rabbitmq_hostname is not None and args.host is not None:
        parser.error("give the RabbitMQ hostname either as the third argument or with --host, "
                     "not both")
    host = args.host or args.rabbitmq_hostname or RABBITMQ_HOST

    if not 1 <= args.port <= 65535:
        parser.error(f"--port must be between 1 and 65535, not {args.port}")

    return Config(
        username=args.rabbitmq_username,
        password=password,
        host=host,
        port=args.port,
        queue=args.queue,
        exchange=args.exchange,
        routing_key=args.routing_key,
        pids_file=args.pids_file,
        results_basepath=args.results_basepath,
    )


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


def main(argv=None, environ=None):
    """
    Main function to read PIDs from a file and process them using a thread pool.

    :param argv: command-line arguments, excluding the program name; defaults to sys.argv[1:]
    :param environ: mapping to read PASSWORD_ENV_VAR from; defaults to os.environ
    """
    config = parse_args(argv, environ)
    print(f"RabbitMQ username: {config.username}")
    print(f"RabbitMQ hostname: {config.host}")

    results_path = build_results_path(config.results_basepath)

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
        all_pids = read_pids(config.pids_file)
        if not all_pids:
            print(f"No PIDs found in {config.pids_file}. Exiting.")
            return
        print(f"Found {len(all_pids)} PIDs to process from {config.pids_file}.")
    except FileNotFoundError:
        print(f"[ERROR] PIDs file not found: {config.pids_file}. Please create it with one PID per line.")
        return
    except Exception as e:
        print(f"[ERROR] Could not read PIDs file {config.pids_file}: {e}")
        return

    connection, channel = open_rabbitmq_channel(config)
    try:
        processed_count = 0
        failed_count = 0
        total_pids = len(all_pids)

        print(f"Starting processing with {MAX_WORKERS} worker threads...")
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='PIDProcessor') as executor:
            # Submit all PIDs to the executor
            future_to_pid = {}
            for pid in all_pids:
                future = executor.submit(process_pid_wrapper, pid, channel, results_path,
                                         config)
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
