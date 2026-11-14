"""Tests for catch-up runs in find_objects_to_reindex.py: each run starts no later than where
the last completed window ended, as recorded in the audit state file."""
import os
import subprocess
import time
from datetime import datetime, timedelta, timezone
from unittest import mock

import pytest
import requests

import find_objects_to_reindex as finder

FMT = "%Y-%m-%dT%H:%M:%S"


def utc(hour, minute, second=0):
    return datetime(2026, 11, 13, hour, minute, second, tzinfo=timezone.utc)


def fmt(dt):
    return dt.strftime(FMT)


@pytest.fixture
def results_path(monkeypatch, tmp_path):
    path = str(tmp_path / "pids_to_process.txt")
    monkeypatch.setattr(finder, "RESULTS_FILE_PATH", path)
    return path


@pytest.fixture
def state_path(results_path):
    return finder.audit_state_path()


def make_results_file(results_path, written_at):
    """Create an archived results file whose mtime is written_at."""
    path = f"{results_path}_{written_at.strftime('%Y-%m-%dT%H-%M-%S')}"
    with open(path, "w") as f:
        f.write("")
    os.utime(path, (written_at.timestamp(), written_at.timestamp()))
    return path


@pytest.fixture
def local_timezone(monkeypatch):
    """Run the test with a non-UTC local timezone, then restore the original one."""
    def set_tz(name):
        monkeypatch.setenv("TZ", name)
        time.tzset()
    yield set_tz
    monkeypatch.undo()
    time.tzset()


# --- the state file ---

def test_state_file_is_next_to_results_and_not_an_archived_result(results_path, state_path):
    assert os.path.dirname(state_path) == os.path.dirname(results_path)
    assert not os.path.basename(state_path).startswith(os.path.basename(results_path) + "_")


def test_state_round_trip(state_path):
    assert finder.read_last_audited(state_path) is None
    finder.write_last_audited(state_path, "2026-11-13T11:50:00")
    assert finder.read_last_audited(state_path) == "2026-11-13T11:50:00"
    assert not os.path.exists(state_path + ".tmp")


def test_invalid_state_is_ignored(state_path, capsys):
    with open(state_path, "w") as f:
        f.write("garbage\n")
    assert finder.read_last_audited(state_path) is None
    assert "Invalid timestamp" in capsys.readouterr().out


def test_cleanup_never_removes_state_file(results_path, state_path):
    finder.write_last_audited(state_path, "2026-01-01T00:00:00")
    old = time.time() - 90 * 86400
    os.utime(state_path, (old, old))
    finder.clean_old_result_files(results_path, days=30)
    assert os.path.exists(state_path)


# --- choose_from_date ---

@pytest.mark.parametrize("last_audited, expected", [
    (None, ("2026-11-13T12:00:00", 0)),
    ("2026-11-13T12:00:00", ("2026-11-13T12:00:00", 0)),        # contiguous: normal run
    ("2026-11-13T12:00:05", ("2026-11-13T12:00:00", 0)),        # overlap: normal start
    ("2026-11-13T11:59:55", ("2026-11-13T11:59:55", 5)),        # CronJob 5 s late
    ("2026-11-13T11:50:00", ("2026-11-13T11:50:00", 600)),      # one missed run
])
def test_choose_from_date(last_audited, expected):
    assert finder.choose_from_date("2026-11-13T12:00:00", last_audited) == expected


# --- estimate, used only when there is no state file yet ---

def test_catchup_from_date_goes_back_delay_plus_margin():
    assert finder.catchup_from_date("2026-11-13T12:00:00", 10) == "2026-11-13T11:49:00"
    assert finder.catchup_from_date("2026-11-13T12:00:00", 0) == "2026-11-13T11:59:00"
    with pytest.raises(ValueError):
        finder.catchup_from_date("not-a-date", 10)


def test_estimate_without_results_files(results_path):
    assert finder.estimate_last_audited(results_path, 10) is None


def test_estimate_uses_newest_results_file(results_path):
    make_results_file(results_path, utc(11, 0, 30))
    make_results_file(results_path, utc(12, 0, 30))
    assert finder.estimate_last_audited(results_path, 10) == "2026-11-13T11:49:30"


@pytest.mark.parametrize("tz", ["America/Los_Angeles", "Asia/Tokyo"])
def test_estimate_does_not_depend_on_local_timezone(results_path, local_timezone, tz):
    local_timezone(tz)
    make_results_file(results_path, utc(12, 0, 30))
    assert finder.estimate_last_audited(results_path, 10) == "2026-11-13T11:49:30"


# --- main(): interval 10, delay 10 ---

@pytest.fixture
def run_main(results_path, monkeypatch):
    """Run finder.main() at a fixed time, and return the (from, to) window it queried."""
    def _run(now, submit_ok=True, db_error=False):
        monkeypatch.setattr(finder, "get_iso_date",
                            lambda minutes_ago: fmt(now - timedelta(minutes=minutes_ago)))
        queried = {}

        def fake_db(from_date, to_date, **kwargs):
            if db_error:
                raise requests.exceptions.ConnectionError("down")
            queried["window"] = (from_date, to_date)
            return {}

        monkeypatch.setattr(finder, "fetch_db_results", fake_db)
        monkeypatch.setattr(finder, "fetch_solr_results", lambda *a, **k: {})
        run = mock.MagicMock()
        if not submit_ok:
            run.side_effect = subprocess.CalledProcessError(1, "submit")
        monkeypatch.setattr(finder.subprocess, "run", run)
        monkeypatch.setenv("RMQ_PASSWORD", "pw")
        monkeypatch.setattr("sys.argv", [
            "find_objects_to_reindex.py", "--metacat-host", "m", "--solr-host", "s",
            "--interval", "10", "--delay", "10", "--rmq-user", "u", "--submit"])
        finder.main()
        return queried["window"]
    return _run


def test_normal_runs_are_contiguous(run_main, state_path, capsys):
    assert run_main(utc(12, 0)) == ("2026-11-13T11:40:00", "2026-11-13T11:50:00")
    assert run_main(utc(12, 10)) == ("2026-11-13T11:50:00", "2026-11-13T12:00:00")
    assert finder.read_last_audited(state_path) == "2026-11-13T12:00:00"
    assert "Needs a catch-up run" not in capsys.readouterr().out


def test_single_missed_run_is_caught_up(run_main):
    """Copilot review of #2383: 12:00 run, 12:10 missed, 12:20 run must start at 11:50."""
    run_main(utc(12, 0))
    assert run_main(utc(12, 20)) == ("2026-11-13T11:50:00", "2026-11-13T12:10:00")


def test_slow_previous_run_leaves_no_gap(run_main, results_path):
    """Copilot review of #2383: a slow previous run must not move the next window's start.

    The 12:00 run's results file is written at 12:07, but its window still ended at 11:50.
    """
    run_main(utc(12, 0))
    for path in [p for p in os.listdir(os.path.dirname(results_path)) if "_" in p]:
        full = os.path.join(os.path.dirname(results_path), path)
        os.utime(full, (utc(12, 7).timestamp(), utc(12, 7).timestamp()))
    assert run_main(utc(12, 30)) == ("2026-11-13T11:50:00", "2026-11-13T12:20:00")


def test_first_run_after_upgrade_estimates_from_results_file(run_main, results_path,
                                                              state_path):
    make_results_file(results_path, utc(12, 0, 30))

    assert run_main(utc(12, 30)) == ("2026-11-13T11:49:30", "2026-11-13T12:20:00")
    assert finder.read_last_audited(state_path) == "2026-11-13T12:20:00"


def test_failed_submission_does_not_advance_state(run_main, state_path):
    run_main(utc(12, 0))
    with pytest.raises(subprocess.CalledProcessError):
        run_main(utc(12, 10), submit_ok=False)
    assert finder.read_last_audited(state_path) == "2026-11-13T11:50:00"
    assert run_main(utc(12, 20)) == ("2026-11-13T11:50:00", "2026-11-13T12:10:00")


def test_failed_query_does_not_advance_state(run_main, state_path):
    run_main(utc(12, 0))
    with pytest.raises(SystemExit):
        run_main(utc(12, 10), db_error=True)
    assert finder.read_last_audited(state_path) == "2026-11-13T11:50:00"


def test_failed_state_write_is_only_a_warning(run_main, results_path, state_path,
                                               monkeypatch, capsys):
    """If the state file can't be written, the run still finishes (archive and cleanup), and
    the next run re-checks this window from the older recorded time."""
    run_main(utc(12, 0))
    capsys.readouterr()

    def fail_replace(src, dst):
        raise OSError(28, "No space left on device")

    monkeypatch.setattr(finder.os, "replace", fail_replace)
    run_main(utc(12, 10))                         # does not raise

    out = capsys.readouterr().out
    assert "Could not record the end of this window" in out
    assert "No space left on device" in out
    assert "Renamed" in out                       # archive_old_results() still ran
    assert not os.path.exists(state_path + ".tmp")
    assert finder.read_last_audited(state_path) == "2026-11-13T11:50:00"


def test_write_last_audited_failure_keeps_existing_state(state_path, monkeypatch):
    finder.write_last_audited(state_path, "2026-11-13T11:50:00")

    def fail_write(*args, **kwargs):
        raise OSError(28, "No space left on device")

    monkeypatch.setattr(finder.os, "replace", fail_write)
    with pytest.raises(OSError):
        finder.write_last_audited(state_path, "2026-11-13T12:00:00")

    assert finder.read_last_audited(state_path) == "2026-11-13T11:50:00"
    assert not os.path.exists(state_path + ".tmp")
