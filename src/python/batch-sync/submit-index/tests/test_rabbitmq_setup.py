"""Tests for open_rabbitmq_channel(): exchange and queue declaration, and cleanup on failure."""
import pika.exceptions
import pytest

import submit_index_task_to_rabbitmq as submit


def test_returns_connection_and_channel(mock_connection):
    connection, channel = submit.open_rabbitmq_channel("user", "pw", "rmq.example")

    assert connection is mock_connection
    assert channel is mock_connection.channel.return_value


def test_declares_exchange_before_binding(mock_connection):
    _, channel = submit.open_rabbitmq_channel("user", "pw", "rmq.example")

    channel.exchange_declare.assert_called_once_with(
        exchange="dataone-index", exchange_type="direct", durable=True)
    channel.queue_declare.assert_called_once_with(
        queue="index", durable=True, arguments={"x-max-priority": 10})
    channel.queue_bind.assert_called_once_with(
        exchange="dataone-index", queue="index", routing_key="index")

    call_names = [c[0] for c in channel.method_calls]
    assert call_names.index("exchange_declare") < call_names.index("queue_bind")


def test_closes_connection_if_setup_fails(mock_connection):
    channel = mock_connection.channel.return_value
    channel.queue_bind.side_effect = pika.exceptions.ChannelClosedByBroker(404, "NOT_FOUND")

    with pytest.raises(pika.exceptions.ChannelClosedByBroker):
        submit.open_rabbitmq_channel("user", "pw", "rmq.example")

    mock_connection.close.assert_called_once()
