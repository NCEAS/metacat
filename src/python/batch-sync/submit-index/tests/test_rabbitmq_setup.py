"""Tests for open_rabbitmq_channel(): exchange and queue declaration, and cleanup on failure."""
import pika.exceptions
import pytest

import submit_index_task_to_rabbitmq as submit


def test_returns_connection_and_channel(mock_connection, config):
    connection, channel = submit.open_rabbitmq_channel(config)

    assert connection is mock_connection
    assert channel is mock_connection.channel.return_value


def test_declares_exchange_before_binding(mock_connection, config):
    _, channel = submit.open_rabbitmq_channel(config)

    channel.exchange_declare.assert_called_once_with(
        exchange="dataone-index", exchange_type="direct", durable=True)
    channel.queue_declare.assert_called_once_with(
        queue="index", durable=True, arguments={"x-max-priority": 10})
    channel.queue_bind.assert_called_once_with(
        exchange="dataone-index", queue="index", routing_key="index")

    call_names = [c[0] for c in channel.method_calls]
    assert call_names.index("exchange_declare") < call_names.index("queue_bind")


def test_uses_configured_names_and_connection(mock_connection, make_config):
    config = make_config(host="rmq.example", port=5673, queue="q-test",
                         exchange="x-test", routing_key="rk-test")

    _, channel = submit.open_rabbitmq_channel(config)

    params = submit.pika.BlockingConnection.call_args.args[0]
    assert (params.host, params.port) == ("rmq.example", 5673)
    assert (params.credentials.username, params.credentials.password) == ("user", "pw")
    channel.exchange_declare.assert_called_once_with(
        exchange="x-test", exchange_type="direct", durable=True)
    channel.queue_declare.assert_called_once_with(
        queue="q-test", durable=True, arguments={"x-max-priority": submit.MAX_PRIORITY})
    channel.queue_bind.assert_called_once_with(
        exchange="x-test", queue="q-test", routing_key="rk-test")


def test_closes_connection_if_setup_fails(mock_connection, config):
    channel = mock_connection.channel.return_value
    channel.queue_bind.side_effect = pika.exceptions.ChannelClosedByBroker(404, "NOT_FOUND")

    with pytest.raises(pika.exceptions.ChannelClosedByBroker):
        submit.open_rabbitmq_channel(config)

    mock_connection.close.assert_called_once()
