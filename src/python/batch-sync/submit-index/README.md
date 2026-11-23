## Goal

These Python scripts are for auditing and submitting indexing tasks

## 1. submit_index_task_to_rabbitmq.py

Reads a list of object PIDs from a file, and publishes them to a RabbitMQ service

### Dependencies
- Python 3.11 or later
- Required package
  ```shell
  pip3 install pika
  ```
- Required files (location set by `--pids-file`; not needed when using `--pid`)
  - `pids_to_process.txt`: the identifiers to be processed, one per line, each optionally
    followed by a priority (see [Priorities](#priorities-and-the-pids-file))

### Configuration
Settings are given on the command line. Every option has a default, so the usual run needs
only the username, the host and the password:

| Option | Default | Meaning |
|---|---|---|
| `--host` | `localhost` | RabbitMQ hostname |
| `--port` | `5672` | RabbitMQ port |
| `--queue` | `index` | Queue to declare and bind |
| `--exchange` | `dataone-index` | Exchange to declare and publish to |
| `--routing-key` | `index` | Routing key for binding and publishing |
| `--priority` | `1` | Priority (0–10) for `--pid` PIDs and for PIDs-file lines without one |
| `--pid` | | Submit this PID at `--priority`; may be repeated. When given, the PIDs file is not read |
| `--pids-file` | `/var/metacat/.metacat/reindex-script/pids_to_process.txt` | `PID [priority]` lines to submit |
| `--results-basepath` | `/var/metacat/.metacat/reindex-script/sysmeta-processed` | Results file; `_<UTC timestamp>.txt` is appended |

The password is read from the `RMQ_PASSWORD` environment variable. This keeps it out of the
process list and shell history. Run `python3 submit_index_task_to_rabbitmq.py --help` for
the full usage.

### Running
Place the Python script on a host with access to the RabbitMQ service, then run:
```shell
read -rs RMQ_PASSWORD && export RMQ_PASSWORD    # type the password; it is not echoed
python3 submit_index_task_to_rabbitmq.py rabbitmq_username --host rabbitmq_hostname
```
To run the script in the background, time the execution, and redirect both standard output and error to a log file, use:
```shell
{ date; time python3 -u submit_index_task_to_rabbitmq.py rabbitmq_username --host rabbitmq_hostname; } > output.log 2>&1 &
```

### Priorities and the PIDs file
The index queue delivers higher-priority tasks first, on a scale of 0 to 10. Metacat itself
publishes ordinary changes at 4, resource maps at 3, and bulk reindexing at 1. Suggested
ranges for this script:

| Priority | Use |
|---|---|
| 0–1 | Bulk reindexing (the default is 1, so batch runs wait behind Metacat's live updates) |
| 3–4 | The same as Metacat's normal operations |
| 5–9 | Moving specific PIDs ahead of the queue |
| 10 | Emergencies |

Each line of the PIDs file is a PID, optionally followed by whitespace and a priority:
```text
# Lines starting with '#' and blank lines are skipped. Comments can't follow a PID.
# No priority given, so this uses --priority (default 1):
urn:uuid:0e5c...
doi:10.18739/A2...  8
resource_map_doi:10.18739/A2...  6
```
- A line with a priority outside 0–10, a priority that isn't a whole number, or more than two
  values is reported with its line number and skipped; the run continues.
- A PID listed more than once is submitted once, at its highest priority.
- PIDs are submitted highest priority first, keeping file order within each priority.
- The results file uses the same `PID<TAB>priority` format, so it can be passed back in with
  `--pids-file`.

To move a few PIDs to the front of the queue without a file:
```shell
python3 submit_index_task_to_rabbitmq.py rabbitmq_username --host rabbitmq_hostname \
    --priority 8 --pid doi:10.18739/A2... --pid urn:uuid:0e5c...
```
This only affects new messages: a PID that is already queued keeps its old message, which is
reindexed again later.

**Legacy form:** the password and hostname can still be given as the second and third
arguments: `python3 submit_index_task_to_rabbitmq.py rabbitmq_username rabbitmq_password
rabbitmq_hostname`. If both a password argument and a different `RMQ_PASSWORD` are given,
the argument is used and a warning is printed. When using `RMQ_PASSWORD`, give the host with
`--host`, because a second argument is always read as the password.

To run the script against a rabbitmq pod in a kubernetes cluster, first ensure you are in the correct k8s context, then use:
```shell
./startup_submissions.sh
```
...and follow the instructions

## 2. find_objects_to_reindex.py

Compares `systemmetadata` and solr entries that were modified within a given time window, and saves a list of object PIDs that need to be reindexed. It then optionally calls `submit_index_task_to_rabbitmq.py` to submit the tasks to RabbitMQ.

The time window is `--interval` minutes long, and ends `--delay` minutes in the past (to allow for jobs to complete, that were submitted moments before the window ended). For example, if `--interval` and `--delay` are both set to 10 minutes, the script runs as follows:

Current time | window start time<br/>(now - interval - delay) | window end time<br/>(now - interval)
-------------|------------------------------------------------|-----------------
12:00        | 11:40                                          | 11:50
12:10        | 11:50                                          | 12:00
12:20        | 12:00                                          | 12:10
...etc.

In each case, the database is queried from the window start time to the window end time, but solr is queried from the window start time to the current time, to allow for indexing lag.

**Catch-up runs.** At the end of each completed run, the window end time is saved in
`last_audited_to_date.txt`, in the same directory as the results files. Each run starts at
the earlier of its normal start time and that saved time. So if runs are missed or skipped
(for example, while the pod is down, or because a slow run made the CronJob skip the next
one), the next run widens its window to cover the whole gap. The time is only saved after
the queries, and the submission if `--submit` is used, succeed, so a failed run's window is
checked again by the next run. If the file can't be written (for example, the disk is full),
the run still finishes with a warning, and the next run re-checks from the older saved time.
The file only helps where this directory persists between runs, as on the CronJob's PVC; in
a container with no persistent storage, each run simply uses its normal window. If the file doesn't exist yet (the first run after upgrading),
the start is estimated from the newest archived results file. Delete the file to stop a
catch-up after a long outage.

### Option 1: run manually (either locally, using port forwarding, or on K8s, in an existing pod):

```shell
$ python3 find_objects_to_reindex.py \
    --metacat-host <your_ssl_metacat_host> \
    --solr-host <your_non_ssl_solr_host> \
    --interval 10 \
    --delay 10 \
    --debug
```

If you want the script to submit the tasks to RabbitMQ, set the `RMQ_PASSWORD` environment variable, add the `--submit` flag, and provide the RabbitMQ username and host:

```shell
read -rs RMQ_PASSWORD && export RMQ_PASSWORD    # type the password; it is not echoed
# then add the following flags to the command above:
python3 [...etc...] --rmq-user <your_rmq_user> --rmq-host <your_non_ssl_rmq_host> --submit
```

The script runs `submit_index_task_to_rabbitmq.py` on the identifiers it found, passing the
password to it in `RMQ_PASSWORD` rather than on its command line. If `--rmq-host` is
omitted, the submit script's default (`localhost`) is used. The tasks are submitted at
priority 1 unless you add `--priority N` (0–10), for example `--priority 3` to have
catch-up reindexing go ahead of bulk reindexing; see
[Priorities](#priorities-and-the-pids-file).

### Option 2: install as a Kubernetes `Cronjob` in using the provided `k8s-index-audit.sh` file:

```shell
# Set the following env variables:
# PVC_NAME is typically the metacat pod's "/var/metacat" PVC.
# Used to store logs, state, etc.
export PVC_NAME="your-pvc"

# RMQ_SECRET_NAME is usually in the existing metacat Secret
export RMQ_SECRET_NAME="your-rmq-secret"

# RMQ_SECRET_KEY is the one used in the existing metacat Secret (defaults to: 'password')
export RMQ_SECRET_KEY="rabbitmq-password"

# CMD_ARGS - see above for available command-line args to pass
# to the find_objects_to_reindex.py script (add --priority N to change the priority
# of submitted tasks from the default of 1)
export CMD_ARGS="--metacat-host arcticdata.io --solr-host metacatarctic-solr --interval 10 --delay 10 --debug"

# run the script to create the cronjob
./k8s-index-audit.sh
```

## Development

The scripts are described by `pyproject.toml`, as the `dataone-index-submit` package. Each
script still runs on its own as a single file, as shown above. Unit tests are in `tests/`
and need no RabbitMQ broker, because they mock `pika`.

Dependencies are managed with [uv](https://docs.astral.sh/uv/), and `uv.lock` pins the
versions used. From this directory:

```shell
uv sync          # creates .venv, installs the package, its dependencies and the dev group
uv run pytest    # runs the unit tests
uv build         # optional: builds a wheel and an sdist into dist/
```

Without uv, pip (25.1 or later) also works:

```shell
python3 -m venv .venv && source .venv/bin/activate
pip install -e . --group dev
pytest
```
