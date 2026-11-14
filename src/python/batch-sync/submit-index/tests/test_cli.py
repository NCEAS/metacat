"""Tests for command-line parsing, PID file reading, results paths, and a full main() run."""
from datetime import datetime, timedelta, timezone

import pytest

import submit_index_task_to_rabbitmq as submit


# --- parse_args ---

def test_two_positionals_use_default_host():
    args = submit.parse_args(["user", "pw"])
    assert (args.rabbitmq_username, args.rabbitmq_password, args.rabbitmq_hostname) == \
        ("user", "pw", "localhost")


def test_three_positionals():
    args = submit.parse_args(["user", "pw", "rmq.example"])
    assert args.rabbitmq_hostname == "rmq.example"


@pytest.mark.parametrize("argv", [[], ["user"]])
def test_too_few_arguments_prints_usage(argv, capsys):
    with pytest.raises(SystemExit) as exc:
        submit.parse_args(argv)
    assert exc.value.code != 0
    assert "usage:" in capsys.readouterr().err


def test_parse_args_defaults_to_sys_argv(monkeypatch):
    monkeypatch.setattr("sys.argv", ["submit_index_task_to_rabbitmq.py", "u", "p", "h"])
    args = submit.parse_args()
    assert (args.rabbitmq_username, args.rabbitmq_password, args.rabbitmq_hostname) == \
        ("u", "p", "h")


# --- read_pids ---

def test_read_pids_strips_whitespace_and_skips_blank_lines(tmp_path):
    path = tmp_path / "pids.txt"
    path.write_text("  pid-1  \n\n\t\npid-2\n   \npid-3")
    assert submit.read_pids(path) == ["pid-1", "pid-2", "pid-3"]


def test_read_pids_missing_file(tmp_path):
    with pytest.raises(FileNotFoundError):
        submit.read_pids(tmp_path / "missing.txt")


# --- build_results_path ---

def test_build_results_path_format():
    now = datetime(2026, 11, 13, 10, 5, 9, tzinfo=timezone.utc)
    assert submit.build_results_path("/x/processed", now) == "/x/processed_2026-11-13T10-05-09.txt"


def test_build_results_path_converts_to_utc():
    now = datetime(2026, 11, 13, 2, 0, 0, tzinfo=timezone(timedelta(hours=-8)))
    assert submit.build_results_path("p", now) == "p_2026-11-13T10-00-00.txt"


# --- main ---

def test_main_publishes_each_pid_and_writes_results(mock_connection, run_env, tmp_path, capsys):
    run_env.write_text("pid-1\n\npid-2\npid-3\n")

    submit.main(["user", "pw", "rmq.example"])

    pika_params = submit.pika.BlockingConnection.call_args.args[0]
    assert pika_params.host == "rmq.example"
    channel = mock_connection.channel.return_value
    published = [c.kwargs["properties"].headers["id"] for c in channel.basic_publish.call_args_list]
    assert published == ["pid-1", "pid-2", "pid-3"]

    [results] = list(tmp_path.glob("processed_*.txt"))
    assert results.read_text() == "pid-1\npid-2\npid-3\n"
    out = capsys.readouterr().out
    assert "Successfully processed and logged: 3" in out
    assert str(results) in out


def test_main_with_empty_pids_file_does_not_connect(mock_connection, run_env):
    run_env.write_text("\n\n")

    submit.main(["user", "pw"])

    submit.pika.BlockingConnection.assert_not_called()


def test_main_with_missing_pids_file_does_not_connect(mock_connection, run_env, capsys):
    submit.main(["user", "pw"])

    submit.pika.BlockingConnection.assert_not_called()
    assert "PIDs file not found" in capsys.readouterr().out
