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
INTERVAL_MINUTES = 10
DELAY_MINUTES = 10
ERROR_UNSET = "ERROR_NOT_SET"
RESULTS_FILE_PATH = "/var/metacat/.metacat/reindex-script/pids_to_process.txt"
REINDEX_SCRIPT_PATH = "submit_index_task_to_rabbitmq.py"
# Highest message priority accepted by --priority (the index queue's x-max-priority)
MAX_PRIORITY = 10
# Gaps (in seconds) between the last completed window and this run's normal window that are
# larger than this are reported as a catch-up run. (Smaller gaps, such as a CronJob starting a
# few seconds late, are closed too, just without the message.) Also the allowance for how long
# a run takes, when the last window's end has to be estimated (see catchup_from_date()).
CATCHUP_MARGIN_SEC = 60
# File, in the same directory as RESULTS_FILE_PATH, that records the end (to_date) of the last
# completed audit window. Catch-up runs resume from it. See audit_state_path().
AUDIT_STATE_FILENAME = "last_audited_to_date.txt"

def get_iso_date(minutes_ago):
    dt = datetime.now(timezone.utc) - timedelta(minutes=minutes_ago)
    return dt.strftime("%Y-%m-%dT%H:%M:%S")

def fetch_db_results(from_date, to_date, retries=3, timeout=10):
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
    url = METACAT_URL_TEMPLATE.format(from_date=from_date, to_date=to_date)
    print("from_date: ", from_date)
    print("to_date:   ", to_date)
    print("url:       ", url)

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
    rename the previous results by appending "_" + run_ts (sanitize colons)
    """
    src = RESULTS_FILE_PATH
    run_ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")
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

def audit_state_path():
    """
    Path of the file recording the end (to_date) of the last completed audit window. It is
    next to RESULTS_FILE_PATH, and its name does not match the archived results files
    (RESULTS_FILE_PATH + "_*"), so clean_old_result_files() never removes it.
    """
    return os.path.join(os.path.dirname(RESULTS_FILE_PATH), AUDIT_STATE_FILENAME)


def read_last_audited(state_path):
    """
    Return the to_date recorded by the last completed run, as "%Y-%m-%dT%H:%M:%S" in UTC, or
    None if there is no state file (for example, the first run after upgrading). An unreadable
    or invalid state file is reported and treated as missing.
    """
    try:
        with open(state_path, "r", encoding="utf-8") as f:
            text = f.read().strip()
    except FileNotFoundError:
        return None
    except OSError as e:
        print(f"[WARNING] Could not read {state_path}: {e}; ignoring it")
        return None
    value = parse_iso_to_utc(text)
    if value is None:
        print(f"[WARNING] Invalid timestamp {text!r} in {state_path}; ignoring it")
        return None
    return value.strftime("%Y-%m-%dT%H:%M:%S")


def write_last_audited(state_path, to_date):
    """
    Record to_date as the end of the last completed audit window. The file is replaced
    atomically, so an interrupted write cannot leave it half-written.
    """
    os.makedirs(os.path.dirname(state_path) or ".", exist_ok=True)
    tmp_path = f"{state_path}.tmp"
    try:
        with open(tmp_path, "w", encoding="utf-8") as f:
            f.write(f"{to_date}\n")
        os.replace(tmp_path, state_path)
    except OSError:
        # Don't leave a partial temporary file behind; the existing state file is unchanged.
        try:
            os.remove(tmp_path)
        except OSError:
            pass
        raise


def catchup_from_date(last_run, delay_minutes):
    """
    Estimate where the previous run's window ended, as "%Y-%m-%dT%H:%M:%S" in UTC, from when
    that run wrote its results file (last_run). Only used when there is no state file yet.

    The previous run's window ended delay_minutes before it started, and last_run is a little
    after it started, so this goes back delay_minutes plus CATCHUP_MARGIN_SEC. If the previous
    run took longer than CATCHUP_MARGIN_SEC, the estimate is late by the difference, which is
    why the recorded to_date (read_last_audited()) is used whenever it is available.

    :param last_run: UTC time of the previous run's results file
    :param delay_minutes: the --delay value
    """
    start = parse_iso_to_utc(last_run)
    if start is None:
        raise ValueError(f"Invalid last_run: {last_run!r}")
    start -= timedelta(minutes=delay_minutes, seconds=CATCHUP_MARGIN_SEC)
    return start.strftime("%Y-%m-%dT%H:%M:%S")


def estimate_last_audited(results_path, delay_minutes):
    """
    Estimate where the last run's window ended from the newest archived results file
    (results_path + "_*"), for when there is no state file yet. Returns None if there are no
    archived results files.
    """
    matching_files = [path for path in glob.glob(f"{results_path}_*") if os.path.isfile(path)]
    if not matching_files:
        return None
    try:
        newest = max(matching_files, key=os.path.getmtime)
        mtime = os.path.getmtime(newest)
    except (OSError, ValueError):
        return None
    last_run = datetime.fromtimestamp(mtime, timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")
    print(f"No audit state file; estimating from {newest} (written at {last_run})")
    return catchup_from_date(last_run, delay_minutes)


def choose_from_date(normal_from, last_audited):
    """
    Choose the start of this run's window, so it begins no later than where the last completed
    window ended.

    :param normal_from: the usual start, current time - interval - delay (from get_iso_date())
    :param last_audited: where the last completed window ended, or None if unknown
    :return: (from_date, gap_seconds): last_audited and how far it is before normal_from if it
        is earlier, otherwise normal_from and 0
    """
    if last_audited is None:
        return normal_from, 0
    gap = (parse_iso_to_utc(normal_from) - parse_iso_to_utc(last_audited)).total_seconds()
    if gap > 0:
        return last_audited, gap
    return normal_from, 0

def priority_arg(text):
    """argparse type for --priority: an integer from 0 to MAX_PRIORITY."""
    try:
        value = int(text)
    except ValueError:
        raise argparse.ArgumentTypeError(
            f"priority must be an integer from 0 to {MAX_PRIORITY}, not {text!r}") from None
    if not 0 <= value <= MAX_PRIORITY:
        raise argparse.ArgumentTypeError(f"priority must be from 0 to {MAX_PRIORITY}, not {value}")
    return value


def build_submit_command(rmq_user, rmq_host=None, priority=None):
    """
    Build the command that runs the submit script on RESULTS_FILE_PATH.

    The password is not included: submit_reindex_tasks() passes it to the child process in
    the RMQ_PASSWORD environment variable, so it does not appear in the process list.

    :param rmq_user: RabbitMQ username
    :param rmq_host: RabbitMQ host, or None for the submit script's default
    :param priority: priority for the index tasks, or None for the submit script's default
    :return: the command, as a list of strings
    """
    command = [sys.executable, REINDEX_SCRIPT_PATH, rmq_user, "--pids-file", RESULTS_FILE_PATH]
    if rmq_host:
        command += ["--host", rmq_host]
    if priority is not None:
        command += ["--priority", str(priority)]
    return command


def submit_reindex_tasks(rmq_user, rmq_pwd, rmq_host=None, priority=None):
    """
    Run the submit script on RESULTS_FILE_PATH, passing the password in RMQ_PASSWORD.

    :raises subprocess.CalledProcessError: if the submit script fails
    """
    env = dict(os.environ, RMQ_PASSWORD=rmq_pwd)
    subprocess.run(build_submit_command(rmq_user, rmq_host, priority), env=env, check=True)


def main():
    """
    Main function to find objects to reindex and optionally submit them.
    1. Parse command-line arguments.
    2. Fetch results from Metacat and Solr.
    3. Compare results and find identifiers to reindex.
    4. Write identifiers to a file with a timestamp.
    5. Optionally call the RabbitMQ submission script.
    """
    global METACAT_HOST, SOLR_HOST, METACAT_URL_TEMPLATE, SOLR_URL_TEMPLATE, ERROR_UNSET, \
        INTERVAL_MINUTES, DELAY_MINUTES

    parser = argparse.ArgumentParser()
    parser.add_argument("--interval", type=int, default=INTERVAL_MINUTES, help="Interval in minutes")
    parser.add_argument("--delay", type=int, default=DELAY_MINUTES, help="Delay in minutes")
    parser.add_argument("--submit", action="store_true", help="Call RabbitMQ submit script after writing file")
    parser.add_argument("--rmq-user", dest="rmq_user", help="RabbitMQ username")
    parser.add_argument("--rmq-host", default=None, help="Host for RabbitMQ (overrides default)")
    parser.add_argument("--priority", type=priority_arg, default=None,
                        help=f"Priority (0-{MAX_PRIORITY}) for submitted index tasks (default: "
                             f"the submit script's default, 1)")
    parser.add_argument("--debug", action="store_true", help="Print debug output")
    parser.add_argument("--metacat-host", default=ERROR_UNSET, help="Host for Metacat")
    parser.add_argument("--solr-host", default=ERROR_UNSET, help="Host for Solr")
    args = parser.parse_args()
    rmq_pwd = os.environ.get("RMQ_PASSWORD")

    METACAT_HOST = args.metacat_host
    SOLR_HOST = args.solr_host
    RABBITMQ_HOST = args.rmq_host
    METACAT_URL_TEMPLATE = (f"https://{METACAT_HOST}/metacat/d1/mn/v2/object?fromDate={{from_date}}&toDate={{to_date}}")
    SOLR_URL_TEMPLATE = (
        f"http://{SOLR_HOST}:8983/solr/metacat-index/select?"
        "q=dateModified:[{date}Z%20TO%20NOW]&fl=id,dateModified&rows=1000000&wt=json"
    )

    normal_from = get_iso_date(args.interval + args.delay)
    to_date = get_iso_date(args.delay)

    # Start where the last completed window ended, if that is earlier than the normal start
    # (after missed, skipped or slow runs), so no period goes unchecked.
    state_path = audit_state_path()
    last_audited = read_last_audited(state_path)
    if last_audited is None:
        last_audited = estimate_last_audited(RESULTS_FILE_PATH, args.delay)
    from_date, gap = choose_from_date(normal_from, last_audited)
    if gap > CATCHUP_MARGIN_SEC:
        print(f"Needs a catch-up run; the last completed window ended at {last_audited}, "
              f"{gap / 60:.1f} minutes before the normal start ({normal_from}), so from_date "
              f"is {from_date}")
    else:
        print("Up to date - no catch-up needed.")

    if args.debug:
        print("from_date:", from_date)
        print("to_date:", to_date)
        print("METACAT_URL_TEMPLATE:", METACAT_URL_TEMPLATE.format(from_date=from_date, to_date=to_date))
        print("SOLR_URL_TEMPLATE:", SOLR_URL_TEMPLATE.format(date=from_date))

    try:
        db_results = fetch_db_results(from_date, to_date)
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
        submit_reindex_tasks(args.rmq_user, rmq_pwd, RABBITMQ_HOST, args.priority)
    else:
        print("Not calling RabbitMQ submission script (use --submit to enable).")

    # Record where this window ended, so the next run resumes from here. Not reached if the
    # queries or the submission failed, so the next run will check this window again. A failed
    # write is only a warning: the tasks are already submitted, and the next run will simply
    # start from the older recorded time, re-checking this window.
    try:
        write_last_audited(state_path, to_date)
        print(f"Recorded the end of this window ({to_date}) in {state_path}")
    except OSError as e:
        print(f"[WARNING] Could not record the end of this window in {state_path}: {e}. "
              f"The next run will check this window again.")

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
