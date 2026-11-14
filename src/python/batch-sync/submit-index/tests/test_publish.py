"""Tests for process_pid_wrapper() and main(), with a mocked pika connection."""
from unittest import mock

import pika.exceptions
import pytest

import submit_index_task_to_rabbitmq as submit


def test_publish_success_logs_pid(results_file, config):
    channel = mock.MagicMock(name="channel")

    result = submit.process_pid_wrapper("urn:uuid:1234", channel, results_file, config)

    assert result == "urn:uuid:1234"
    kwargs = channel.basic_publish.call_args.kwargs
    assert kwargs["exchange"] == "dataone-index"
    assert kwargs["routing_key"] == "index"
    assert kwargs["properties"].headers == {"index_type": "create", "id": "urn:uuid:1234"}
    assert kwargs["properties"].priority == submit.PRIORITY
    assert results_file.read_text() == "urn:uuid:1234\n"


def test_publish_uses_configured_exchange_and_routing_key(results_file, make_config):
    channel = mock.MagicMock(name="channel")
    config = make_config(exchange="x-test", routing_key="rk-test")

    submit.process_pid_wrapper("pid-1", channel, results_file, config)

    kwargs = channel.basic_publish.call_args.kwargs
    assert (kwargs["exchange"], kwargs["routing_key"]) == ("x-test", "rk-test")


@pytest.mark.parametrize("error, label", [
    (pika.exceptions.AMQPConnectionError("broker went away"), "connection error"),
    (pika.exceptions.ChannelClosedByBroker(406, "PRECONDITION_FAILED"), "channel error"),
    (pika.exceptions.AMQPError("something else"), "RabbitMQ error"),
    (RuntimeError("unexpected"), "unexpected error"),
])
def test_publish_failure_is_reported(results_file, config, capsys, error, label):
    channel = mock.MagicMock(name="channel")
    channel.basic_publish.side_effect = error

    assert submit.process_pid_wrapper("urn:uuid:1234", channel, results_file, config) is None

    out = capsys.readouterr().out
    assert label in out
    assert "urn:uuid:1234" in out
    # The underlying error is reported, not hidden by a failure in the handler itself.
    assert repr(error) in out or str(error) in out
    assert not results_file.exists()


def test_main_closes_connection(mock_connection, run_env):
    run_env.write_text("pid-1\n\npid-2\n")

    submit.main(["user", "pw", "rmq.example"])

    channel = mock_connection.channel.return_value
    assert channel.basic_publish.call_count == 2
    mock_connection.close.assert_called_once()
