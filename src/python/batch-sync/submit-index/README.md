## Goal

These Python scripts are for auditing and submitting indexing tasks

## 1. submit_index_task_to_rabbitmq.py

Reads a list of object PIDs from a file, and publishes them to a RabbitMQ service

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

## 2. find_objects_to_reindex.py

Compares systemmetadata and solr entries that were modified within the last N minutes, and saves a list of object PIDs that need to be reindexed. Optionally calls `submit_index_task_to_rabbitmq.py` to submit the tasks to RabbitMQ.

### Option 1: run manually (either locally, using port forwarding, or on K8s, in an existing pod):

```shell
$ python3 find_objects_to_reindex.py \
    --metacat-host <your_ssl_metacat_host> \
    --solr-host <your_non_ssl_solr_host> \
    --interval 10 \
    --debug
```

If you want the script to submit the tasks to RabbitMQ, set the `RMQ_PASSWORD` environment variable, add the `--submit` flag, and provide the RabbitMQ username and host:

```shell
export RMQ_PASSWORD="<your-rmq-password>"
# then add the following flags to the command above:
python3 [...etc...] --rmq-user <your_rmq_user> --rmq-host <your_non_ssl_rmq_host> --submit
```

### Option 2: install as a Kubernetes `Cronjob` in using the provided `k8s-index-audit.sh` file:

```shell
# Set the following env variables:
# PVC_NAME is typically the metacat pod's "/var/metacat" PVC.
# Used to store logs, state, etc.
export PVC_NAME="your-pvc"

# RMQ_SECRET_NAME is usually in the existing metacat Secret
export RMQ_SECRET_NAME="your-rmq-secret"

# CMD_ARGS - see above for available command-line args to pass
# to the find_objects_to_reindex.py script
export CMD_ARGS="--metacat-host arcticdata.io --solr-host metacatarctic-solr --interval 10 --debug"

# run the script to create the cronjob
./k8s-index-audit.sh
```
