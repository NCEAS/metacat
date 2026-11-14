"""Tests for find_objects_to_reindex.needs_catchup()."""
import os
import time
from datetime import datetime, timedelta, timezone

import pytest

import find_objects_to_reindex as finder

FMT = "%Y-%m-%dT%H:%M:%S"
# A fixed window start, as get_iso_date() would return it (UTC, no offset).
START = datetime(2026, 11, 13, 12, 0, 0, tzinfo=timezone.utc)
START_STR = START.strftime(FMT)


def make_results_file(tmp_path, offset_seconds):
    """Create an archived results file whose mtime is START + offset_seconds."""
    path = tmp_path / "pids_to_process.txt_2026-11-13T00-00-00"
    path.write_text("")
    mtime = START.timestamp() + offset_seconds
    os.utime(path, (mtime, mtime))
    return path


@pytest.fixture
def results_base(tmp_path):
    return str(tmp_path / "pids_to_process.txt")


@pytest.fixture
def local_timezone(monkeypatch):
    """Run the test with a non-UTC local timezone, then restore the original one."""
    def set_tz(name):
        monkeypatch.setenv("TZ", name)
        time.tzset()
    yield set_tz
    monkeypatch.undo()
    time.tzset()


def test_no_previous_results(results_base):
    assert finder.needs_catchup(results_base, START_STR) is None


def test_previous_run_inside_window(tmp_path, results_base):
    make_results_file(tmp_path, +10)
    assert finder.needs_catchup(results_base, START_STR) is None


def test_previous_run_within_margin(tmp_path, results_base):
    make_results_file(tmp_path, -(finder.CATCHUP_MARGIN_SEC - 30))
    assert finder.needs_catchup(results_base, START_STR) is None


def test_missed_run_triggers_catchup(tmp_path, results_base):
    # A one-hour gap. With the old 60,000-second margin, this went unnoticed.
    make_results_file(tmp_path, -3600)
    expected = (START - timedelta(hours=1)).strftime(FMT)
    assert finder.needs_catchup(results_base, START_STR) == expected


@pytest.mark.parametrize("tz", ["America/Los_Angeles", "Asia/Tokyo"])
def test_result_does_not_depend_on_local_timezone(tmp_path, results_base, local_timezone, tz):
    local_timezone(tz)

    make_results_file(tmp_path, +10)
    assert finder.needs_catchup(results_base, START_STR) is None

    make_results_file(tmp_path, -3600)
    expected = (START - timedelta(hours=1)).strftime(FMT)
    assert finder.needs_catchup(results_base, START_STR) == expected


def test_invalid_start_time(results_base, tmp_path):
    make_results_file(tmp_path, -3600)
    with pytest.raises(ValueError):
        finder.needs_catchup(results_base, "not-a-date")
