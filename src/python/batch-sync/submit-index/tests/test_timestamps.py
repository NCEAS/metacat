"""Timestamps are UTC and keep their existing format (naive "%Y-%m-%dT%H:%M:%S", no offset)."""
import re
from datetime import datetime, timedelta, timezone

import find_objects_to_reindex as finder
import submit_index_task_to_rabbitmq as submit

TS_PATTERN = r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}"


def test_finder_get_iso_date_is_utc_without_offset():
    value = finder.get_iso_date(10)

    assert re.fullmatch(TS_PATTERN, value)
    parsed = datetime.strptime(value, "%Y-%m-%dT%H:%M:%S").replace(tzinfo=timezone.utc)
    expected = datetime.now(timezone.utc) - timedelta(minutes=10)
    assert abs(parsed - expected) < timedelta(seconds=5)


def test_submit_results_file_name_has_utc_timestamp(mock_connection, run_env, tmp_path):
    run_env.write_text("pid-1\n")

    submit.main(["user", "pw"])

    [results] = list(tmp_path.glob("processed_*.txt"))
    stamp = results.name[len("processed_"):-len(".txt")]
    # Colons are replaced by dashes in the file name.
    assert re.fullmatch(TS_PATTERN.replace(":", "-"), stamp)
    parsed = datetime.strptime(stamp, "%Y-%m-%dT%H-%M-%S").replace(tzinfo=timezone.utc)
    assert abs(parsed - datetime.now(timezone.utc)) < timedelta(seconds=5)
