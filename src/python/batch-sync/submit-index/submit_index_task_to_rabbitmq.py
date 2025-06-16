import io
import threading
import concurrent.futures
import os
import pika
import sys
import time  # For potential delays or timeouts

# --- Configuration ---
# If the type of the index task is systemMetacat_change_only
SYSMETA_CHANGE_ONLY = False
# Path to the output file for logging successfully processed PIDs
RESULTS_FILE_PATH = "/var/metacat/.metacat/sysmeta-processed.txt"
# RabbitMQ URL
RABBITMQ_URL = "localhost"
# RabbitMQ port number
RABBITMQ_PORT_NUMBER = 5672
# The time gap between tow submission in seconds
SUBMISSION_GAP_SEC = 0.5
# Number of worker threads
MAX_WORKERS = 1
# Path to the input file containing PIDs, one PID per line
PIDS_FILE_PATH = "pids_to_process.txt"
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
        # 3. Add PID to results file (thread-safe)
        try:
            with results_file_lock:
                with open(RESULTS_FILE_PATH, "a") as f:
                    f.write(f"{pid}\n")
            print(f"  [{thread_name}] Successfully processed and logged PID: {pid}")
            return pid
        except IOError as ioe:
            print(f"[ERROR] [{thread_name}] Could not write to results file {RESULTS_FILE_PATH} for PID {pid}: {ioe}")
            return None

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

def main():
    """
    Main function to read PIDs from a file and process them using a thread pool.
    """
    arguments = sys.argv[1:]
    if len(arguments) == 2:
        print("Arguments:", arguments)
        rabbitmq_username = arguments[0]
        rabbitmq_password = arguments[1]
        print(f"RabbitMQ username: {rabbitmq_username}")
    else:
        print("Usage: python3 submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password")
        sys.exit()
    # Ensure the directory for results_file_path exists
    try:
        results_dir = os.path.dirname(RESULTS_FILE_PATH)
        if results_dir:  # Ensure it's not an empty string if RESULTS_FILE_PATH is just a filename
            os.makedirs(results_dir, exist_ok=True)
        print(f"Results will be logged to: {RESULTS_FILE_PATH}")
    except OSError as e:
        print(f"[ERROR] Could not create directory for results file {RESULTS_FILE_PATH}: {e}. Please check permissions.")
        return

    all_pids = []
    try:
        with open(PIDS_FILE_PATH, "r") as f:
            all_pids = [line.strip() for line in f if line.strip()]
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

    # Use a session object for connection pooling and default headers if needed
    # with requests.Session() as session:
    with get_rabbitmq_channel(rabbitmq_username, rabbitmq_password) as channel:
        processed_count = 0
        failed_count = 0
        total_pids = len(all_pids)

        print(f"Starting processing with {MAX_WORKERS} worker threads...")
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='PIDProcessor') as executor:
            # Submit all PIDs to the executor
            future_to_pid = {}
            for pid in all_pids:
                future = executor.submit(process_pid_wrapper, pid, channel)
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

    print(f"\n--- Processing Complete ---")
    print(f"Total PIDs from file: {total_pids}")
    print(f"Successfully processed and logged: {processed_count}")
    print(f"Failed to process: {failed_count}")
    if processed_count > 0:
        print(f"Successfully processed PIDs have been logged to: {RESULTS_FILE_PATH}")

if __name__ == "__main__":
    main()
