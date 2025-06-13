## Goal
The Python script publishes a list object PIDs, stored in a file, to a RabbitMQ service

## Dependencies
- Python 3.10.0
- Required package
	```
  pip3 install pika
  ```
- Required files (must be in the same directory as the Python script)
	- `pids_to_process.txt`: a file containing the list of identifiers to be processed

## Configuration
Edit the following variables in the Python script before running:
- `RABBITMQ_URL`, The URL of the RabbitMQ service
- `RABBITMQ_PORT_NUMBER`, The port number of the RabbitMQ service

## Running
Place the Python script on a host with access to the RabbitMQ service, then run:
```
python3 submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password
```
To run the script in the background, time the execution, and redirect both standard output and error to a log file, use:
```
{ date; time python3 submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password; } > output.log 2>&1 &
```
