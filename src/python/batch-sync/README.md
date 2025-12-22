## Goal
The Python script synchronizes the system metadata of a list objects from Metacat database to the corresponding files in Hash Store.

## Dependencies
- Python 3.13.0
- Required packages
	```
  pip3 install git+https://github.com/DataONEorg/hashstore.git
  pip3 install requests
  ```
- Required files (must be in the same directory as the Python script)
	- `pids_to_process.txt`: a file containing the list of identifiers to be processed
  - `token`: a file containing the JWT token of the member node administrator

## Configuration
Edit the following variables in the Python script:
- `NODE_BASE_URL`, the base URL of the member node.
- `HASH_STORE_PATH`, the path to the Hash Store directory.

## Running
Place the Python script on a host that has access to the Hash Store directory, and run:
```
python3 batch_sync_db_to_hashstore.py
```
If you want to run the Python script in the background and redirect its output (both standard output and standard error) to a file, you can use the following command:
```
{ date; time python3 -u batch_sync_db_to_hashstore.py; } > output.log 2>&1 &
```
