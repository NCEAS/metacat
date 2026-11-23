"""Tests for message priorities: --priority, --pid, the PIDs file format, and ordering."""
import pytest

import submit_index_task_to_rabbitmq as submit


def published(mock_connection):
    """(pid, priority) for each published message, in publish order."""
    channel = mock_connection.channel.return_value
    return [(c.kwargs["properties"].headers["id"], c.kwargs["properties"].priority)
            for c in channel.basic_publish.call_args_list]


def parse_error(argv, capsys):
    with pytest.raises(SystemExit) as exc:
        submit.parse_args(argv, environ={})
    assert exc.value.code == 2
    return capsys.readouterr().err


# --- defaults and --priority (Stage 5) ---

def test_default_priority_is_metacat_low_priority():
    assert submit.PRIORITY == 1
    assert submit.MAX_PRIORITY == 10
    assert submit.parse_args(["user", "pw"], environ={}).priority == 1


@pytest.mark.parametrize("value, expected", [("0", 0), ("7", 7), ("10", 10)])
def test_priority_option(value, expected):
    config = submit.parse_args(["user", "pw", "--priority", value], environ={})
    assert config.priority == expected


@pytest.mark.parametrize("value", ["-1", "11", "abc", "4.5", ""])
def test_invalid_priority_option(value, capsys):
    err = parse_error(["user", "pw", f"--priority={value}"], capsys)
    assert "priority must be" in err


@pytest.mark.parametrize("text", ["-1", "11", "x", None])
def test_parse_priority_rejects(text):
    with pytest.raises(ValueError):
        submit.parse_priority(text)


# --- --pid (Stage 6) ---

def test_pid_option_is_repeatable():
    config = submit.parse_args(["user", "pw", "--pid", "a", "--pid", "b"], environ={})
    assert config.pids == ("a", "b")


@pytest.mark.parametrize("value", ["", "has space", "tab\there"])
def test_invalid_pid_option(value, capsys):
    err = parse_error(["user", "pw", "--pid", value], capsys)
    assert "PID must be non-empty" in err


# --- load_pid_priorities (Stage 6) ---

def test_load_pid_priorities(tmp_path, capsys):
    path = tmp_path / "pids.txt"
    path.write_text(
        "# comment line\n"
        "pid-default\n"
        "  pid-seven   7  \n"
        "\n"
        "12345\t0\n"              # a numeric PID, with a tab
        "12345678\n"              # a numeric PID alone uses the default
        "   # indented comment\n"
        "pid-zero 0\n"
        "pid-ten 10\n"
    )

    assert submit.load_pid_priorities(path, default_priority=3) == [
        ("pid-default", 3), ("pid-seven", 7), ("12345", 0), ("12345678", 3),
        ("pid-zero", 0), ("pid-ten", 10),
    ]
    assert capsys.readouterr().out == ""


@pytest.mark.parametrize("line, message", [
    ("pid-a 11", "from 0 to 10, not 11"),
    ("pid-a -1", "from 0 to 10, not -1"),
    ("pid-a high", "integer from 0 to 10"),
    ("pid-a 2.5", "integer from 0 to 10"),
    ("pid-a 3 extra", "expected 'PID [priority]'"),
])
def test_bad_lines_are_reported_and_skipped(tmp_path, capsys, line, message):
    path = tmp_path / "pids.txt"
    path.write_text(f"pid-ok\n{line}\npid-after 2\n")

    assert submit.load_pid_priorities(path, default_priority=1) == \
        [("pid-ok", 1), ("pid-after", 2)]
    out = capsys.readouterr().out
    assert f"{path}:2:" in out
    assert message in out


def test_load_pid_priorities_missing_file(tmp_path):
    with pytest.raises(FileNotFoundError):
        submit.load_pid_priorities(tmp_path / "missing.txt", 1)


# --- order_submissions (Stage 6) ---

def test_order_is_descending_priority_and_stable():
    entries = [("a", 1), ("b", 5), ("c", 1), ("d", 10), ("e", 5)]
    assert submit.order_submissions(entries) == \
        [("d", 10), ("b", 5), ("e", 5), ("a", 1), ("c", 1)]


def test_duplicates_keep_highest_priority_at_first_position():
    entries = [("a", 1), ("b", 1), ("a", 7), ("c", 1), ("b", 0)]
    assert submit.order_submissions(entries) == [("a", 7), ("b", 1), ("c", 1)]


def test_order_submissions_empty():
    assert submit.order_submissions([]) == []


# --- main (Stages 5 and 6) ---

def test_main_publishes_file_priorities_highest_first(mock_connection, run_env, tmp_path):
    run_env.write_text("low\nhigh 9\nmid 4\nlow2\nhigh 2\n")

    submit.main(["user", "pw", "--priority", "2"])

    assert published(mock_connection) == [("high", 9), ("mid", 4), ("low", 2), ("low2", 2)]
    [results] = list(tmp_path.glob("processed_*.txt"))
    assert results.read_text() == "high\t9\nmid\t4\nlow\t2\nlow2\t2\n"


def test_main_default_priority_without_option(mock_connection, run_env):
    run_env.write_text("pid-1\n")

    submit.main(["user", "pw"])

    assert published(mock_connection) == [("pid-1", 1)]


def test_main_pid_option_ignores_file(mock_connection, run_env, capsys):
    run_env.write_text("from-file 3\n")

    submit.main(["user", "pw", "--pid", "x", "--pid", "y", "--pid", "x", "--priority", "8"])

    assert published(mock_connection) == [("x", 8), ("y", 8)]
    out = capsys.readouterr().out
    assert "Using 3 PID(s) from --pid" in out
    assert "Removed 1 duplicate" in out
    assert "Total PIDs from --pid: 2" in out


def test_main_pid_option_works_without_a_pids_file(mock_connection, run_env):
    # run_env's PIDs file is never created
    submit.main(["user", "pw", "--pid", "x"])
    assert published(mock_connection) == [("x", 1)]


def test_main_with_only_bad_lines_does_not_connect(mock_connection, run_env, capsys):
    run_env.write_text("pid-a 99\n# nothing else\n")

    submit.main(["user", "pw"])

    submit.pika.BlockingConnection.assert_not_called()
    assert "No PIDs found" in capsys.readouterr().out


def test_results_file_can_be_used_as_input(mock_connection, run_env, tmp_path):
    run_env.write_text("a 6\nb\n")
    submit.main(["user", "pw"])
    [first_results] = list(tmp_path.glob("processed_*.txt"))

    mock_connection.channel.return_value.basic_publish.reset_mock()
    submit.main(["user", "pw", "--pids-file", str(first_results),
                 "--results-basepath", str(tmp_path / "second")])

    assert published(mock_connection) == [("a", 6), ("b", 1)]
