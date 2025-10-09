## Goal
These Python scripts are for auditing and submitting indexing tasks

## submit_index_task_to_rabbitmq.py

Reads a list object PIDs from a file, and publishes them to a RabbitMQ service

### Dependencies
- Python 3.10.0
- Required package
  ```shell
  pip3 install pika
  ```
- Required files (see `PIDS_FILE_PATH` for location)
  - `pids_to_process.txt`: a file containing the list of identifiers to be processed

### Configuration
Edit the following variables in the Python script before running:
- `RABBITMQ_HOST`, The hostname of the RabbitMQ service (alternatively pass as the third argument)
- `RABBITMQ_PORT_NUMBER`, The port number of the RabbitMQ service

### Running
Place the Python script on a host with access to the RabbitMQ service, then run:
```shell
python3 submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password rabbitmq_hostname
```
To run the script in the background, time the execution, and redirect both standard output and error to a log file, use:
```shell
{ date; time python3 -u submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password rabbitmq_hostname; } > output.log 2>&1 &
```

To run the script against a rabbitmq pod in a kubernetes cluster, first ensure you are in the correct k8s context, then use:
```shell
./startup_submissions.sh
```
...and follow the instructions

## find_objects_to_reindex.py

Compares systemmetadata and solr entries that were modified within the last N minutes, and saves a list of object PIDs that need to be reindexed. Optionally calls `submit_index_task_to_rabbitmq.py` to submit the tasks to RabbitMQ.

### Running

Can run manually (either in an existing pod, or locally using port forwarding):
```shell
$ python3 find_objects_to_reindex.py \
    --metacat-host metacat-dev.test.dataone.org \
    --solr-host localhost \
    --debug  --interval 10
    # optionally, add:
    --submit \
    --rmq-user metacat-rmq-guest --rmq-pwd mysecret123 --rmq-host localhost
```

Can install as a cronjob in a kubernetes cluster using the provided `k8s-index-audit.sh` file.
