# Submit index tasks for a list of identifiers (one per line).
# Please edit config.ini first.
#Usage:
#    python3 reindex.py -f ids.txt

import argparse
import concurrent.futures
import logging
import os
import sys
import time

from datetime import datetime
from psycopg2 import pool


# Import *reused* components from pull_systemmetadata_submitter.py
from pull_systemmeta_submitter import (
    DB_CONFIG,
    DB_CONNECTION_POOL_SIZE,
    MAX_WORKERS,
    DOCID_WAIT_SEC,
    DOCID_MAX_RETRIES,
    SYSTEM_METADATA_COLUMNS,
    process_pid_wrapper,
    load_non_data_format_ids,
    lookup_docid_with_retry,
    AMQPStormChannelPool,
    RABBITMQ_URL,
    RABBITMQ_PORT_NUMBER,
    RABBITMQ_USERNAME,
    RABBITMQ_PASSWORD,
    setup_logging,
    build_system_metadata_for_guid,
    logger as submitter_logger
)

DEFAULT_IDS_FILE_PATH = "/var/metacat/.metacat/reindex-script/pids_to_process.txt"

log_name = "reindex"
log_file = f"log/{log_name}.log"

"""
   Read ids from a file to and array
"""
def read_ids_from_file(path):
    ids = []
    with open(path, "r") as f:
        for line in f:
            v = line.strip()
            if v:
                ids.append(v)
    return ids

"""
   Lookup main systemmetadata and docid.rev for a guid.
"""
def lookup_docid_and_main_sysmeta(conn, guid):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT
                sm.guid,
                sm.series_id,
                sm.serial_version,
                sm.date_uploaded,
                sm.rights_holder,
                sm.checksum,
                sm.checksum_algorithm,
                sm.origin_member_node,
                sm.authoritive_member_node,
                sm.date_modified,
                sm.submitter,
                sm.object_format,
                sm.size,
                sm.archived,
                sm.replication_allowed,
                sm.number_replicas,
                sm.obsoletes,
                sm.obsoleted_by,
                sm.media_type,
                sm.file_name,
                i.docid || '.' || i.rev AS doc_id
            FROM systemmetadata sm
            LEFT JOIN identifier i
              ON sm.guid = i.guid
            WHERE sm.guid = %s
            """,
            (guid,)
        )
        return cur.fetchone()


# ------------------------------------------------------------
# Main
# ------------------------------------------------------------

def submit_from_file(id_file):
    pg_pool = None
    channel_pool = None
    try:
        ids = read_ids_from_file(id_file)
        if not ids:
            logger.warning("No IDs found in file.")
            return
        non_data_formats = load_non_data_format_ids()
        logger.debug(f"Loaded {len(ids)} IDs from {id_file}")

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
                    # The row contains the information from the systemmetadata table and docid
                    # for the given guid. It only return one row with values
                    rowResult = lookup_docid_and_main_sysmeta(conn, guid)
                    if not rowResult:
                        logger.warning(f"[WARN] GUID not found in systemmetadata: {guid}")
                        continue
                    # Process the rowResult
                    try:
                        # Turn the main query rowResult into a dictionary.
                        row = dict(zip(SYSTEM_METADATA_COLUMNS, rowResult))
                        guid = row["guid"]
                        object_format = row["object_format"]
                        doc_id = row["doc_id"]
                        if object_format in non_data_formats and not doc_id:
                            doc_id = lookup_docid_with_retry(conn, guid)
                        if object_format in non_data_formats and not doc_id:
                            logger.warning(f"[WARN] Skipping {guid}: doc_id not found after multiple try")
                            continue
                        # Build SystemMetadata XML
                        system_metadata_xml = build_system_metadata_for_guid(conn, row)
                        futures.append(
                            executor.submit(
                                process_pid_wrapper,
                                channel_pool,
                                guid,
                                object_format,
                                doc_id,
                                system_metadata_xml
                            )
                        )
                    except Exception as error:
                        logger.error(f"[ERROR] Process {guid}: {error}")
            finally:
                pg_pool.putconn(conn)
            # Wait for all tasks
            concurrent.futures.wait(futures)
        logger.info(f"Finished submitting {len(ids)} IDs.")
    except Exception as error:
        logger.error(f"Reindexing failed: {error}")
    finally:
        if (channel_pool):
            channel_pool.close()
        if (pg_pool):
            pg_pool.closeall()

if __name__ == "__main__":
    setup_logging(log_file=log_file, level=logging.DEBUG)
    # Create a "reindex" child logger using the same handlers
    logger = submitter_logger.getChild(log_name)
    # Remove the parent prefix
    logger.name = log_name
    file = None
    parser = argparse.ArgumentParser(description="Submit objects from a file for reindexing.")
    parser.add_argument(
        "-f",
        "--file",
        nargs="?",                 # Makes the argument optional after -f
        const=None,                # If -f is given without value
        default=None,              # If -f is not given at all
        help="Path to the results file"
    )
    # This allows unknown arguments
    args, unknown = parser.parse_known_args()
    if args.file:
        file = args.file
        logger.info(f"Use the user specified path {file}")
    else:
        file = DEFAULT_IDS_FILE_PATH
        logger.info(f"Use the default path {file}")
    submit_from_file(file)
