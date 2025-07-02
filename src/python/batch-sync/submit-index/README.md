## Goal
The Python script publishes a list object PIDs, stored in a file, to a RabbitMQ service

## Dependencies
- Python 3.10.0
- Required package
	```shell
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
```shell
python3 submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password
```
To run the script in the background, time the execution, and redirect both standard output and error to a log file, use:
```shell
{ date; time python3 -u submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password; } > output.log 2>&1 &
```

To run the script against a rabbitmq pod in a kubernetes cluster, first ensure you are in the correct k8s contaxt, then use:
```shell
./startup.sh
```
...and follow the instructions
