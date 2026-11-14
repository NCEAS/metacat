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


# --- catchup_from_date: no gap between the previous window and the catch-up window ---

def test_catchup_from_date_goes_back_delay_plus_margin():
    assert finder.catchup_from_date("2026-11-13T12:00:00", 10) == "2026-11-13T11:49:00"


def test_catchup_from_date_with_zero_delay():
    assert finder.catchup_from_date("2026-11-13T12:00:00", 0) == "2026-11-13T11:59:00"


def test_catchup_from_date_invalid():
    with pytest.raises(ValueError):
        finder.catchup_from_date("not-a-date", 10)


def test_catchup_window_reaches_back_to_previous_window_end(tmp_path, results_base):
    """The example from issue #2381: interval 10, delay 10, runs at 12:10 and 12:20 missed.

    The 12:00 run checked up to 11:50 and wrote its results file at 12:00:30. The 12:30 run's
    normal window would start at 12:10. Before the fix, catch-up started at 12:00:30, so
    changes between 11:50 and 12:00:30 were never checked.
    """
    interval, delay = 10, 10
    previous_run = datetime(2026, 11, 13, 12, 0, 0, tzinfo=timezone.utc)
    previous_window_end = previous_run - timedelta(minutes=delay)            # 11:50
    path = tmp_path / "pids_to_process.txt_2026-11-13T12-00-30"
    path.write_text("")
    mtime = (previous_run + timedelta(seconds=30)).timestamp()               # took 30 s
    os.utime(path, (mtime, mtime))
    now = datetime(2026, 11, 13, 12, 30, 0, tzinfo=timezone.utc)
    normal_start = (now - timedelta(minutes=interval + delay)).strftime(FMT)  # 12:10

    last_run = finder.needs_catchup(results_base, normal_start)
    assert last_run == "2026-11-13T12:00:30"

    start = finder.catchup_from_date(last_run, delay)
    assert start == "2026-11-13T11:49:30"
    assert datetime.strptime(start, FMT).replace(tzinfo=timezone.utc) <= previous_window_end


def test_main_uses_catchup_window(tmp_path, monkeypatch):
    """main() queries the database from (last run - delay - margin) after a missed run."""
    results_path = tmp_path / "pids_to_process.txt"
    monkeypatch.setattr(finder, "RESULTS_FILE_PATH", str(results_path))
    last_run = datetime.now(timezone.utc).replace(microsecond=0) - timedelta(minutes=45)
    old = tmp_path / "pids_to_process.txt_old"
    old.write_text("")
    os.utime(old, (last_run.timestamp(), last_run.timestamp()))
    queried = {}

    def fake_db(from_date, to_date, **kwargs):
        queried["from"], queried["to"] = from_date, to_date
        return {}

    monkeypatch.setattr(finder, "fetch_db_results", fake_db)
    monkeypatch.setattr(finder, "fetch_solr_results", lambda *a, **k: {})
    monkeypatch.setattr("sys.argv", ["find_objects_to_reindex.py", "--metacat-host", "m",
                                     "--solr-host", "s", "--interval", "10", "--delay", "10"])

    finder.main()

    expected = last_run - timedelta(minutes=10, seconds=finder.CATCHUP_MARGIN_SEC)
    assert queried["from"] == expected.strftime(FMT)
