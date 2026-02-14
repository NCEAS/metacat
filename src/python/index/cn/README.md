# CN Directory Scripts

This directory contains three Python scripts for handling indexing tasks with **Metacat**, **RabbitMQ**, and **Solr**:

- `pull_systemmeta_submitter.py`
- `delete_systemmeta_listener.py`
- `reindex.py`

---

## Overview of Scripts

### `pull_systemmeta_submitter.py`
This is the main index task generator. It:

- Periodically pulls new records from the Metacat `systemmetadata` table.
- Generates and sends index tasks to RabbitMQ.

**Important:** Configure database, RabbitMQ, and Solr settings in the script before running.

### `delete_systemmeta_listener.py`
This script handles delete actions from Metacat and sends delete index tasks to RabbitMQ.

**Note:** Ensure database, RabbitMQ, and Solr are configured in `pull_systemmeta_submitter.py` before running.

### `reindex.py`
Used to handle objects that may have been missed by `pull_systemmeta_submitter.py`.

- Feed the script with a file containing a list of PIDs to index (one PID per line).
- The script will iterate through the file, submit the index tasks, and exit when done.

**Note:** Configure database, RabbitMQ, and Solr in `pull_systemmeta_submitter.py` before running.

---

## Usage

### `pull_systemmeta_submitter.py`

1. Configure database, RabbitMQ, and Solr in the script.
2. Run the script in the background:
   ```bash
   nohup python3 pull_systemmeta_submitter.py &
   ```  
3. Stop the process:
   ```bash
   kill -2 <process-id>
   ```  
4. Files created:
  - `.mn_latest_modified_map.json` – tracks the indexed latest modification date of objects for each member node. **Do not delete or modify this file.**
  - Log file: `log/pull_systemmeta_submitter.log` (customizable in the script).

### `delete_systemmeta_listener.py`

1. Ensure database, RabbitMQ, and Solr are configured in `pull_systemmeta_submitter.py`.
2. Create a trigger in the Metacat database:
   ```bash
   psql -h localhost -U <database-username> <database-name> < metacat/src/scripts/sql/systemmetadata-trigger.sql
   ```  
3. Run the script in the background:
   ```bash
   nohup python3 delete_systemmeta_listener.py &
   ```  
4. Stop the process:
   ```bash
   kill -2 <process-id>
   ```  
5. Default log file: `log/delete_systemmeta_listener.log` (customizable in the script).

### `reindex.py`

1. Ensure database, RabbitMQ, and Solr are configured in `pull_systemmeta_submitter.py`.
2. Run the script with a file containing PIDs:
   ```bash
   python3 reindex.py <filename-containing-ids>
   ```  
  - The file must contain **one PID per line**.
3. Default log file: `log/reindex.log` (customizable in the script).

---

**Tips:**
- Always configure settings before running scripts.
- Avoid deleting or modifying `.mn_latest_modified_map.json`.