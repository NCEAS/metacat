"""Tests for how find_objects_to_reindex.py calls the submit script."""
import argparse
import subprocess
from unittest import mock

import pytest

import find_objects_to_reindex as finder
import submit_index_task_to_rabbitmq as submit


@pytest.fixture
def results_path(monkeypatch, tmp_path):
    path = str(tmp_path / "pids_to_process.txt")
    monkeypatch.setattr(finder, "RESULTS_FILE_PATH", path)
    return path


# --- build_submit_command ---

def test_command_with_defaults(results_path):
    command = finder.build_submit_command("rmq-user")
    assert command[1:] == [finder.REINDEX_SCRIPT_PATH, "rmq-user", "--pids-file", results_path]


def test_command_with_host_and_priority(results_path):
    command = finder.build_submit_command("rmq-user", "rmq.example", 0)
    assert command[-4:] == ["--host", "rmq.example", "--priority", "0"]


def test_command_never_contains_none(results_path):
    # Before Stage 7, a missing --rmq-host put None in the command, and subprocess.run() raised
    # TypeError.
    assert None not in finder.build_submit_command("rmq-user", None, None)


@pytest.mark.parametrize("host, priority", [(None, None), ("rmq.example", 6)])
def test_submit_script_accepts_the_command(results_path, capsys, host, priority):
    """Contract test: the submit script parses the command into the intended settings."""
    command = finder.build_submit_command("rmq-user", host, priority)

    config = submit.parse_args(command[2:], environ={"RMQ_PASSWORD": "s3cret"})

    assert (config.username, config.password, config.pids_file) == \
        ("rmq-user", "s3cret", results_path)
    assert config.host == (host or submit.RABBITMQ_HOST)
    assert config.priority == (submit.PRIORITY if priority is None else priority)
    assert capsys.readouterr().err == ""


# --- submit_reindex_tasks ---

def test_password_passed_in_environment_not_command(results_path, monkeypatch):
    run = mock.MagicMock()
    monkeypatch.setattr(finder.subprocess, "run", run)

    finder.submit_reindex_tasks("rmq-user", "s3cret", "rmq.example", 3)

    command = run.call_args.args[0]
    assert "s3cret" not in command
    assert run.call_args.kwargs["env"]["RMQ_PASSWORD"] == "s3cret"
    assert run.call_args.kwargs["check"] is True


def test_submit_failure_is_raised(results_path, monkeypatch):
    monkeypatch.setattr(finder.subprocess, "run",
                        mock.MagicMock(side_effect=subprocess.CalledProcessError(1, "x")))
    with pytest.raises(subprocess.CalledProcessError):
        finder.submit_reindex_tasks("rmq-user", "s3cret")


# --- --priority ---

@pytest.mark.parametrize("text, expected", [("0", 0), ("4", 4), ("10", 10)])
def test_priority_arg(text, expected):
    assert finder.priority_arg(text) == expected


@pytest.mark.parametrize("text", ["-1", "11", "abc"])
def test_priority_arg_rejects(text):
    with pytest.raises(argparse.ArgumentTypeError):
        finder.priority_arg(text)


# --- main() with --submit ---

def test_main_submit_without_rmq_host(results_path, monkeypatch):
    """--submit without --rmq-host now runs the submit script with its default host."""
    monkeypatch.setattr(finder, "fetch_db_results", lambda *a, **k: {"pid-1": None})
    monkeypatch.setattr(finder, "fetch_solr_results", lambda *a, **k: {})
    monkeypatch.setattr(finder, "compare_results", lambda db, solr: ["pid-1"])
    run = mock.MagicMock()
    monkeypatch.setattr(finder.subprocess, "run", run)
    monkeypatch.setenv("RMQ_PASSWORD", "s3cret")
    monkeypatch.setattr("sys.argv", [
        "find_objects_to_reindex.py", "--metacat-host", "m.example", "--solr-host", "s.example",
        "--rmq-user", "rmq-user", "--submit", "--priority", "3"])

    finder.main()

    command = run.call_args.args[0]
    assert "--host" not in command
    assert command[-2:] == ["--priority", "3"]
    assert run.call_args.kwargs["env"]["RMQ_PASSWORD"] == "s3cret"
