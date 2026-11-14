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
def results_file(monkeypatch, tmp_path):
    """Point the module-level results file (normally set by main()) at a temporary file."""
    path = tmp_path / "results.txt"
    monkeypatch.setattr(submit, "RESULTS_FILE_PATH", str(path), raising=False)
    return path
