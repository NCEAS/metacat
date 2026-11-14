"""Shared fixtures. pika is mocked, so no RabbitMQ broker is needed."""
from unittest import mock

import pytest

import submit_index_task_to_rabbitmq as submit


@pytest.fixture
def mock_connection(monkeypatch):
    """Replace pika.BlockingConnection with a mock, and return the mock connection.

    The connection's channel is ``mock_connection.channel.return_value``. The connection
    reports itself as open until ``close()`` is called.
    """
    connection = mock.MagicMock(name="connection")
    connection.is_open = True

    def close():
        connection.is_open = False

    connection.close.side_effect = close
    monkeypatch.setattr(submit.pika, "BlockingConnection",
                        mock.MagicMock(return_value=connection))
    return connection


@pytest.fixture
def results_file(tmp_path):
    """A results file path in a temporary directory (not created)."""
    return tmp_path / "results.txt"


@pytest.fixture
def run_env(monkeypatch, tmp_path):
    """Point main()'s PIDs file and results files at tmp_path, and remove the submission gap.

    Returns the PIDs file path; write PIDs to it before calling main(). Results files are
    written as tmp_path / "processed_<timestamp>.txt".
    """
    pids_file = tmp_path / "pids.txt"
    monkeypatch.setattr(submit, "PIDS_FILE_PATH", str(pids_file))
    monkeypatch.setattr(submit, "RESULTS_FILE_BASEPATH", str(tmp_path / "processed"))
    monkeypatch.setattr(submit, "SUBMISSION_GAP_SEC", 0)
    return pids_file
