import requests
import io
import threading
import concurrent.futures
import os
import time  # For potential delays or timeouts

# --- Placeholder for storeMetadata ---
# IMPORTANT: You need to ensure that the actual `storeMetadata` function
# is available in the context where this script is run.
# This might involve importing it from another module if it's part of your project.
#
# For example:
# from your_metacat_api_module import storeMetadata
#
# If storeMetadata is not defined, this script will raise a NameError.
# Below is a placeholder definition for testing purposes.
# Remove or replace it with your actual implementation.
def storeMetadata(metadata_stream, pid):
    """
    Placeholder for the actual storeMetadata function.
    This function should take an InputStream-like object (e.g., io.BytesIO)
    and a PID string as input.
    """
    print(f"  [{threading.current_thread().name}] [INFO] storeMetadata called for PID: {pid}")
    pass
# --- End of Placeholder ---

# --- Configuration ---
# Path to the input file containing PIDs, one PID per line
PIDS_FILE_PATH = "pids_to_process.txt"
# Path to the output file for logging successfully processed PIDs
RESULTS_FILE_PATH = "/var/metacat/.metacat/sysmeta-processed.txt"
# Number of worker threads
MAX_WORKERS = 10
# Request timeout in seconds
REQUEST_TIMEOUT = 30
# --- End Configuration ---

# Lock for thread-safe writing to the results file
results_file_lock = threading.Lock()

def process_pid_wrapper(pid, session):
    """
    Processes a single PID:
    1. Fetch sysmeta XML.
    2. Call storeMetadata.
    3. Call index API.
    4. Log PID to results file on success.
    Returns PID on success, None on failure.
    """
    thread_name = threading.current_thread().name
    print(f"[{thread_name}] Processing PID: {pid}")

    try:
        # 1. Fetch sysmeta XML document
        meta_url = f"https://arcticdata.io/metacat/d1/mn/v2/meta/{pid}"
        print(f"  [{thread_name}] Fetching metadata from: {meta_url}")
        response_meta = session.get(meta_url, timeout=REQUEST_TIMEOUT)
        response_meta.raise_for_status()  # Raise an HTTPError for bad responses (4XX or 5XX)
        print(f"  [{thread_name}] Successfully fetched metadata for {pid}. Status: {response_meta.status_code}")
        metadata_content = response_meta.content

        # 2. Call storeMetadata with the sysmeta document
        metadata_stream = io.BytesIO(metadata_content)
        try:
            storeMetadata(metadata_stream, pid)  # Call the (placeholder or actual) Python API method
            print(f"  [{thread_name}] Successfully called storeMetadata for PID: {pid}")
        except Exception as e:
            print(f"[ERROR] [{thread_name}] storeMetadata failed for PID {pid}: {e}")
            return None  # Stop processing this PID if storeMetadata fails

        # 3. Call the index API
        index_url = f"https://arcticdata.io/metacat/d1/mn/v2/index?pid={pid}"
        print(f"  [{thread_name}] Calling index API: {index_url}")
        response_index = session.get(index_url, timeout=REQUEST_TIMEOUT)
        response_index.raise_for_status()
        print(f"  [{thread_name}] Successfully called index API for {pid}. Status: {response_index.status_code}")

        # 4. Add PID to results file (thread-safe)
        try:
            with results_file_lock:
                with open(RESULTS_FILE_PATH, "a") as f:
                    f.write(f"{pid}\n")
            print(f"  [{thread_name}] Successfully processed and logged PID: {pid}")
            return pid
        except IOError as ioe:
            print(f"[ERROR] [{thread_name}] Could not write to results file {RESULTS_FILE_PATH} for PID {pid}: {ioe}")
            return None

    except requests.exceptions.HTTPError as http_err:
        print(f"[ERROR] [{thread_name}] HTTP error for PID {pid} at URL {http_err.request.url}: {http_err}")
    except requests.exceptions.ConnectionError as conn_err:
        print(f"[ERROR] [{thread_name}] Connection error for PID {pid} (URL: {conn_err.request.url if conn_err.request else 'N/A'}): {conn_err}")
    except requests.exceptions.Timeout as timeout_err:
        print(f"[ERROR] [{thread_name}] Timeout for PID {pid} (URL: {timeout_err.request.url if timeout_err.request else 'N/A'}): {timeout_err}")
    except requests.exceptions.RequestException as req_err:
        print(f"[ERROR] [{thread_name}] General request error for PID {pid}: {req_err}")
    except Exception as e:
        print(f"[ERROR] [{thread_name}] An unexpected error occurred while processing PID {pid}: {e}")

    return None  # Return None if any step failed

def main():
    """
    Main function to read PIDs from a file and process them using a thread pool.
    """
    # Ensure the directory for results_file_path exists
    try:
        results_dir = os.path.dirname(RESULTS_FILE_PATH)
        if results_dir:  # Ensure it's not an empty string if RESULTS_FILE_PATH is just a filename
            os.makedirs(results_dir, exist_ok=True)
        print(f"Results will be logged to: {RESULTS_FILE_PATH}")
    except OSError as e:
        print(f"[ERROR] Could not create directory for results file {RESULTS_FILE_PATH}: {e}. Please check permissions.")
        return

    # Create a dummy pids_to_process.txt for testing if it doesn't exist
    if not os.path.exists(PIDS_FILE_PATH):
        print(f"Warning: PIDs file '{PIDS_FILE_PATH}' not found. Creating a dummy file for testing.")
        print(f"Please replace '{PIDS_FILE_PATH}' with your actual list of PIDs.")
        try:
            with open(PIDS_FILE_PATH, "w") as f:
                f.write("arctic-data.10020.1\n")
                f.write("arctic-data.10021.1\n")
                f.write("nonexistent.pid.test\n")  # To test error handling for a PID
                for i in range(22, 40):  # Add more PIDs for threading demo
                    f.write(f"arctic-data.100{i:02d}.1\n")
        except IOError as e:
            print(f"[ERROR] Could not create dummy PIDs file '{PIDS_FILE_PATH}': {e}")
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
    with requests.Session() as session:
        processed_count = 0
        failed_count = 0
        total_pids = len(all_pids)

        print(f"Starting processing with {MAX_WORKERS} worker threads...")
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS, thread_name_prefix='PIDProcessor') as executor:
            # Submit all PIDs to the executor
            future_to_pid = {executor.submit(process_pid_wrapper, pid, session): pid for pid in all_pids}

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
