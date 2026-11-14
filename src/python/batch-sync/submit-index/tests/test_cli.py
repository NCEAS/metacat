"""Tests for command-line parsing, results paths, and full main() runs."""
from datetime import datetime, timedelta, timezone

import pytest

import submit_index_task_to_rabbitmq as submit


ENV = {"RMQ_PASSWORD": "env-pw"}


def parse_error(argv, capsys, environ=None):
    """Run parse_args, expect a usage error, and return the error output."""
    with pytest.raises(SystemExit) as exc:
        submit.parse_args(argv, environ={} if environ is None else environ)
    assert exc.value.code == 2
    return capsys.readouterr().err


# --- parse_args: legacy positional form ---

def test_two_positionals_use_defaults():
    config = submit.parse_args(["user", "pw"], environ={})
    assert config == submit.Config(
        username="user", password="pw", host="localhost", port=5672, queue="index",
        exchange="dataone-index", routing_key="index",
        pids_file="/var/metacat/.metacat/reindex-script/pids_to_process.txt",
        results_basepath="/var/metacat/.metacat/reindex-script/sysmeta-processed",
        priority=1, pids=())


def test_three_positionals():
    config = submit.parse_args(["user", "pw", "rmq.example"], environ={})
    assert config.host == "rmq.example"


@pytest.mark.parametrize("argv", [[], ["user"]])
def test_missing_username_or_password(argv, capsys):
    assert "usage:" in parse_error(argv, capsys)


def test_parse_args_defaults_to_sys_argv(monkeypatch):
    monkeypatch.setattr("sys.argv", ["submit_index_task_to_rabbitmq.py", "u", "p", "h"])
    config = submit.parse_args(environ={})
    assert (config.username, config.password, config.host) == ("u", "p", "h")


def test_parse_args_defaults_to_os_environ(monkeypatch):
    monkeypatch.setenv("RMQ_PASSWORD", "from-os-env")
    assert submit.parse_args(["user"]).password == "from-os-env"


# --- parse_args: password from the environment ---

def test_password_from_environment():
    config = submit.parse_args(["user", "--host", "rmq.example"], environ=ENV)
    assert (config.password, config.host) == ("env-pw", "rmq.example")


def test_password_starting_with_dash_from_environment():
    config = submit.parse_args(["user"], environ={"RMQ_PASSWORD": "-secret"})
    assert config.password == "-secret"


def test_positional_password_wins_and_warns_when_different(capsys):
    config = submit.parse_args(["user", "rmq.example"], environ=ENV)
    # The likely mistake: a host given where the password goes. The warning points it out.
    assert config.password == "rmq.example"
    err = capsys.readouterr().err
    assert "RMQ_PASSWORD is ignored" in err
    assert "--host" in err


def test_no_warning_when_both_passwords_match(capsys):
    # find_objects_to_reindex.py passes the password as an argument and in RMQ_PASSWORD.
    config = submit.parse_args(["user", "env-pw", "rmq.example"], environ=ENV)
    assert config.password == "env-pw"
    assert capsys.readouterr().err == ""


@pytest.mark.parametrize("environ", [{}, {"RMQ_PASSWORD": ""}])
def test_no_password_at_all(environ, capsys):
    err = parse_error(["user", "--host", "rmq.example"], capsys, environ)
    assert "password is required" in err
    assert "RMQ_PASSWORD" in err


def test_password_not_shown_in_config_repr():
    config = submit.parse_args(["user", "s3cret"], environ={})
    assert "s3cret" not in repr(config)


# --- parse_args: --host ---

def test_positional_host_and_host_option_conflict(capsys):
    err = parse_error(["user", "pw", "h1", "--host", "h2"], capsys)
    assert "not both" in err


@pytest.mark.parametrize("argv", [
    ["--host", "rmq.example", "user"],
    ["user", "--host", "rmq.example"],
    ["user", "--host", "rmq.example", "pw"],
])
def test_options_can_appear_between_positionals(argv):
    environ = ENV if "pw" not in argv else {}
    config = submit.parse_args(argv, environ=environ)
    assert (config.username, config.host) == ("user", "rmq.example")


# --- parse_args: other switches ---

def test_each_switch_is_applied():
    config = submit.parse_args([
        "user", "pw",
        "--port", "5673", "--queue", "q", "--exchange", "x", "--routing-key", "rk",
        "--pids-file", "/tmp/p.txt", "--results-basepath", "/tmp/out",
    ], environ={})
    assert (config.port, config.queue, config.exchange, config.routing_key,
            config.pids_file, config.results_basepath) == \
        (5673, "q", "x", "rk", "/tmp/p.txt", "/tmp/out")


@pytest.mark.parametrize("port", ["0", "65536", "-1", "abc"])
def test_invalid_port(port, capsys):
    parse_error(["user", "pw", f"--port={port}"], capsys)


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
    assert results.read_text() == "pid-1\t1\npid-2\t1\npid-3\t1\n"
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


def test_main_with_env_password_and_switches(mock_connection, tmp_path):
    pids_file = tmp_path / "custom-pids.txt"
    pids_file.write_text("pid-1\n")
    results_base = tmp_path / "out" / "custom-results"

    submit.main(["user", "--host", "rmq.example", "--port", "5673",
                 "--queue", "q", "--exchange", "x", "--routing-key", "rk",
                 "--pids-file", str(pids_file), "--results-basepath", str(results_base)],
                environ={"RMQ_PASSWORD": "env-pw"})

    params = submit.pika.BlockingConnection.call_args.args[0]
    assert (params.host, params.port, params.credentials.password) == \
        ("rmq.example", 5673, "env-pw")
    channel = mock_connection.channel.return_value
    channel.queue_bind.assert_called_once_with(exchange="x", queue="q", routing_key="rk")
    kwargs = channel.basic_publish.call_args.kwargs
    assert (kwargs["exchange"], kwargs["routing_key"]) == ("x", "rk")
    # The results directory is created if needed.
    [results] = list((tmp_path / "out").glob("custom-results_*.txt"))
    assert results.read_text() == "pid-1\t1\n"
