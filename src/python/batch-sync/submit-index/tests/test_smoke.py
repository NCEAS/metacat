"""Smoke tests: the script imports without side effects, and its defaults are unchanged."""
import submit_index_task_to_rabbitmq as submit


def test_queue_defaults():
    assert submit.QUEUE_NAME == "index"
    assert submit.EXCHANGE_NAME == "dataone-index"
    assert submit.ROUTING_KEY == "index"


def test_connection_defaults():
    assert submit.RABBITMQ_HOST == "localhost"
    assert submit.RABBITMQ_PORT_NUMBER == 5672
