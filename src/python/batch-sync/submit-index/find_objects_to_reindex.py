import argparse
from datetime import datetime, timedelta, timezone
import glob
import json
import os
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

# Third-party
import requests

# --- Configurable parameters ---
INTERVAL_MINUTES = 15
METACAT_HOST = "metacatbrooke-0"
SOLR_HOST = "metacatbrooke-solr"
METACAT_URL_TEMPLATE = f"https://{METACAT_HOST}/metacat/d1/mn/v2/object?fromDate={{date}}"
SOLR_URL_TEMPLATE = (f"http://{SOLR_HOST}:8983/solr/metacat-index/select?q=dateModified:[{{date}}Z%20TO%20NOW]&fl=id,dateModified&rows=1000000&wt=json")
RESULTS_FILE_PATH = "/var/metacat/.metacat/reindex-script/pids_to_process.txt"
REINDEX_SCRIPT_PATH = "submit_index_task_to_rabbitmq.py"

def get_iso_date(minutes_ago):
    dt = datetime.utcnow() - timedelta(minutes=minutes_ago)
    return dt.strftime("%Y-%m-%dT%H:%M:%S")

def fetch_db_results(from_date, retries=3, timeout=10):
    """
    Fetch objectInfo entries from the Metacat service.
    :param from_date: ISO 8601 date string (e.g. "2023-10-01T12:00:00")
    :param retries: Number of retries for transient errors
    :param timeout: Request timeout in seconds
    :return: dict mapping identifier -> dateSysMetadataModified (or None if missing)

    - Retries a few times if the parsed result is empty (transient server issues).
    - Validates that the response looks like XML and prints a short debug snippet
      if it doesn't.
    """
    url = METACAT_URL_TEMPLATE.format(date=from_date)

    for attempt in range(1, retries + 1):
        try:
            resp = requests.get(url, timeout=timeout)
            resp.raise_for_status()
        except requests.RequestException as e:
            if attempt == retries:
                raise
            time.sleep(1)
            continue

        content = resp.content or b""
        ctype = resp.headers.get("Content-Type", "").lower()

        # Quick check that response looks like XML; if not, log a small snippet and retry.
        if ("xml" not in ctype) and (not content.strip().startswith(b"<?xml")):
            snippet = content[:500].decode(errors="replace")
            print(f"Warning: response for {url} does not look like XML (attempt {attempt}/{retries}). Snippet:\n{snippet}")
            if attempt == retries:
                return {}
            time.sleep(1)
            continue

        try:
            root = ET.fromstring(content)
        except ET.ParseError as e:
            snippet = content[:500].decode(errors="replace")
            print(f"XML parse error (attempt {attempt}/{retries}): {e}. Snippet:\n{snippet}")
            if attempt == retries:
                return {}
            time.sleep(1)
            continue

        db_results = {}
        # namespace-agnostic search for objectInfo elements
        for obj in root.findall('.//{*}objectInfo'):
            identifier_el = obj.find('{*}identifier')
            date_el = obj.find('{*}dateSysMetadataModified')
            if identifier_el is None or identifier_el.text is None:
                continue
            db_results[identifier_el.text] = (date_el.text if date_el is not None else None)

        if db_results:
            return db_results

        # empty result -- maybe transient; retry a couple times
        if attempt < retries:
            time.sleep(1)
            continue

    return {}


def fetch_solr_results(from_date):
    """
    Fetch documents from Solr modified since from_date.
    :param from_date: ISO 8601 date string (e.g. "2023-10-01T12:00:00")
    :return: dict mapping identifier -> dateModified
    """
    url = SOLR_URL_TEMPLATE.format(date=from_date)
    resp = requests.get(url)
    resp.raise_for_status()
    docs = resp.json()['response']['docs']
    solr_results = {}
    for doc in docs:
        solr_results[doc['id']] = doc['dateModified']
    return solr_results


def parse_iso_to_utc(s):
    """
    Parse an ISO-8601 timestamp string into a timezone-aware UTC datetime.
    - Accepts forms like '2025-09-29T23:02:37.427+00:00' and '2025-09-29T23:02:37.427Z'.
    - Returns None if `s` is falsy or cannot be parsed.
    """
    if not s:
        return None
    s = s.strip()
    if s.endswith("Z"):
        s = s[:-1] + "+00:00"
    try:
        dt = datetime.fromisoformat(s)
    except ValueError:
        return None
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    else:
        dt = dt.astimezone(timezone.utc)
    return dt


def compare_results(db_results, solr_results):
    """
    Compare db_results and solr_results after normalizing timestamps to UTC.
    Returns a list of identifiers that should be reindexed.
    """
    to_reindex = []
    for identifier, db_date in db_results.items():
        solr_date = solr_results.get(identifier)
        if solr_date is None:
            to_reindex.append(identifier)
            continue

        db_date_normalized = parse_iso_to_utc(db_date)
        solr_date_normalized = parse_iso_to_utc(solr_date)

        if db_date_normalized is None or solr_date_normalized is None:
            to_reindex.append(identifier)
            print(
                f"Warning: could not parse dates for identifier {identifier}: db_date='{db_date}', solr_date='{solr_date}'")
        else:
            if db_date_normalized != solr_date_normalized:
                to_reindex.append(identifier)
    return to_reindex

def write_to_file(identifiers, file_path):
    """
    Write the list of identifiers to the specified file, one per line.
    :param identifiers: list of identifiers
    :param file_path: path to the output file
    :return:
    """
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    with open(file_path, "w", encoding="utf-8") as f:
        for identifier in identifiers:
            f.write(f"{identifier}\n")

def archive_old_results():
    """
    rename the reindex script by appending "_" + run_ts (sanitize colons)
    """
    src = RESULTS_FILE_PATH
    run_ts = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S")
    if os.path.exists(src):
        dirn, base = os.path.split(src)
        safe_ts = run_ts.replace(":", "-")
        new_base = f"{base}_{safe_ts}"
        dst = os.path.join(dirn, new_base) if dirn else new_base
        os.rename(src, dst)
        print(f"Renamed {src} to `{dst}`")
    else:
        print(f"File {src} does not exist; nothing to rename.")

def clean_old_result_files(results_path, days=30):
    """
    Delete files matching `results_path_*` older than `days`.
    :param results_path: base path, e.g. `RESULTS_FILE_PATH`
    :param days: age threshold in days
    :return: list of removed file paths
    """
    now = time.time()
    cutoff = now - days * 86400 # days to seconds
    pattern = f"{results_path}_*"
    removed = []

    for path in glob.glob(pattern):
        if not os.path.isfile(path):
            continue
        try:
            if os.path.getmtime(path) < cutoff:
                os.remove(path)
                removed.append(path)
        except OSError as e:
            print(f"Failed to remove `{path}`: {e}")

    return removed

def main():
    """
    Main function to find objects to reindex and optionally submit them.
    1. Parse command-line arguments.
    2. Fetch results from Metacat and Solr.
    3. Compare results and find identifiers to reindex.
    4. Write identifiers to a file.
    5. Optionally call the RabbitMQ submission script.
    6. Archive the identifiers file by renaming it with a timestamp.
    """
    global METACAT_HOST, SOLR_HOST, METACAT_URL_TEMPLATE, SOLR_URL_TEMPLATE

    parser = argparse.ArgumentParser()
    parser.add_argument("--interval", type=int, default=INTERVAL_MINUTES, help="Interval in minutes")
    parser.add_argument("--submit", action="store_true", help="Call RabbitMQ submit script after writing file")
    parser.add_argument("--rmq-user", dest="rmq_user", help="RabbitMQ username")
    parser.add_argument("--rmq-host", default=None, help="Host for RabbitMQ (overrides default)")
    parser.add_argument("--debug", action="store_true", help="Print debug output")
    parser.add_argument("--metacat-host", default=METACAT_HOST, help="Host for Metacat (overrides METACAT_HOST)")
    parser.add_argument("--solr-host", default=SOLR_HOST, help="Host for Solr (overrides SOLR_HOST)")
    args = parser.parse_args()
    rmq_pwd = os.environ.get("RMQ_PASSWORD")

    METACAT_HOST = args.metacat_host
    SOLR_HOST = args.solr_host
    RABBITMQ_HOST = args.rmq_host
    METACAT_URL_TEMPLATE = f"https://{METACAT_HOST}/metacat/d1/mn/v2/object?fromDate={{date}}"
    SOLR_URL_TEMPLATE = (
        f"http://{SOLR_HOST}:8983/solr/metacat-index/select?"
        "q=dateModified:[{date}Z%20TO%20NOW]&fl=id,dateModified&rows=1000000&wt=json"
    )

    from_date = get_iso_date(args.interval)

    if args.debug:
        print("from_date:", from_date)
        print("METACAT_URL_TEMPLATE:", METACAT_URL_TEMPLATE.format(date=from_date))
        print("SOLR_URL_TEMPLATE:", SOLR_URL_TEMPLATE.format(date=from_date))

    try:
        db_results = fetch_db_results(from_date)
        solr_results = fetch_solr_results(from_date)
    except requests.exceptions.RequestException as e:
        print("Network error while contacting remote service:")
        print(f"  {e}")
        print("Possible causes:")
        print("  - Hostname is not resolvable from this machine.")
        print("  - You need to run this inside the cluster where those hostnames exist.")
        print("Options to resolve:")
        print("  - Use `--metacat-host` and `--solr-host` to supply reachable hostnames or IPs.")
        print("  - Add appropriate entries to the ` /etc/hosts ` file on this machine.")
        print("  - Port-forward or expose the services to a reachable address.")
        sys.exit(1)

    to_reindex = compare_results(db_results, solr_results)

    if args.debug:
        print("db_results:", db_results)
        print("solr_results:", solr_results)
        print("to_reindex:", to_reindex)

    write_to_file(to_reindex, RESULTS_FILE_PATH)
    print(f"Wrote {len(to_reindex)} identifiers to {RESULTS_FILE_PATH}")

    if args.submit:
        print(f"Calling RabbitMQ submission script: {REINDEX_SCRIPT_PATH}")
        if not (args.rmq_user and rmq_pwd):
            print("RabbitMQ username and password required for submission.")
            sys.exit(1)
        subprocess.run([
            sys.executable,
            REINDEX_SCRIPT_PATH,
            args.rmq_user,
            rmq_pwd,
            RABBITMQ_HOST
        ], check=True)
    else:
        print("Not calling RabbitMQ submission script (use --submit to enable).")

    # Archive the reindex script by renaming it with a timestamp
    archive_old_results()

    # Clean up old result files
    removed_files = clean_old_result_files(RESULTS_FILE_PATH, days=30)
    if removed_files:
        print(f"Removed {len(removed_files)} old result files:")
        for f in removed_files:
            print(f"  {f}")

if __name__ == "__main__":
    main()
