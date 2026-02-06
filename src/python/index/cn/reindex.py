# Submit index tasks for a list of identifiers (one per line).
# Please edit the pull_systemmetadata_submitter properties first.
#Usage:
#    python3 submit_ids_from_file.py ids.txt


import sys
import time
import concurrent.futures
from psycopg2 import pool
from datetime import datetime

# Import *reused* components from pull_systemmetadata_submitter.py
from pull_systemmeta_submitter import (
    DB_CONFIG,
    DB_CONNECTION_POOL_SIZE,
    MAX_WORKERS,
    DOCID_WAIT_SEC,
    DOCID_MAX_RETRIES,
    process_pid_wrapper,
    load_non_data_format_ids,
    AMQPStormChannelPool,
    RABBITMQ_URL,
    RABBITMQ_PORT_NUMBER,
    RABBITMQ_USERNAME,
    RABBITMQ_PASSWORD,
)

def read_ids_from_file(path):
    ids = []
    with open(path, "r") as f:
        for line in f:
            v = line.strip()
            if v:
                ids.append(v)
    return ids


def lookup_docid_and_format(conn, guid):
    """
    Lookup object_format and docid.rev for a guid.
    """
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT
                sm.object_format,
                i.docid || '.' || i.rev AS doc_id
            FROM systemmetadata sm
            LEFT JOIN identifier i
              ON sm.guid = i.guid
            WHERE sm.guid = %s
            """,
            (guid,)
        )
        return cur.fetchone()


def lookup_docid_with_retry(conn, guid):
    """
    Retry docid lookup (mirrors logic in poller).
    """
    for attempt in range(1, DOCID_MAX_RETRIES + 1):
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT docid || '.' || rev
                FROM identifier
                WHERE guid = %s
                """,
                (guid,)
            )
            res = cur.fetchone()
            if res and res[0]:
                return res[0]

        print(f"Retry {attempt}/{DOCID_MAX_RETRIES}: doc_id still missing for {guid}")
        time.sleep(DOCID_WAIT_SEC)

    return None


# ------------------------------------------------------------
# Main
# ------------------------------------------------------------

def submit_from_file(id_file):
    ids = read_ids_from_file(id_file)
    if not ids:
        print("No IDs found in file.")
        return
    non_data_formats = load_non_data_format_ids()
    print(f"Loaded {len(ids)} IDs from {id_file}")

    # DB pool
    pg_pool = pool.ThreadedConnectionPool(
        minconn=1,
        maxconn=DB_CONNECTION_POOL_SIZE,
        **DB_CONFIG
    )

    # RabbitMQ channel pool
    channel_pool = AMQPStormChannelPool(
        RABBITMQ_URL,
        RABBITMQ_PORT_NUMBER,
        RABBITMQ_USERNAME,
        RABBITMQ_PASSWORD,
        MAX_WORKERS
    )

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=MAX_WORKERS,
        thread_name_prefix="FileSubmitter"
    ) as executor:

        futures = []
        conn = pg_pool.getconn()
        try:
            for guid in ids:
                row = lookup_docid_and_format(conn, guid)
                if not row:
                    print(f"[WARN] GUID not found in systemmetadata: {guid}")
                    continue
                object_format, doc_id = row
                if object_format in non_data_formats and not doc_id:
                    doc_id = lookup_docid_with_retry(conn, guid)

                if object_format in non_data_formats and not doc_id:
                    print(f"[WARN] Skipping {guid}: doc_id not found after multiple try")
                    continue

                futures.append(
                    executor.submit(
                        process_pid_wrapper,
                        channel_pool,
                        guid,
                        object_format,
                        doc_id
                    )
                )

        finally:
            pg_pool.putconn(conn)

        # Wait for all tasks
        concurrent.futures.wait(futures)

    channel_pool.close()
    pg_pool.closeall()

    print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Finished submitting {len(ids)} IDs.")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 submit_ids_from_file.py <id_file>")
        sys.exit(1)

    submit_from_file(sys.argv[1])
