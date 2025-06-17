#!/bin/bash

logfile="/var/metacat/.metacat/nonMatchingChecksum_2024-11-20_22-55-18.txt"
results_file="/var/metacat/.metacat/results.tsv"
updates_file="/var/metacat/.metacat/updates.sql"
failed_log="/var/metacat/.metacat/failed_updates.log"


## Script to re-apply the checksum fixes that were done during the hashstore upgrade process, by
## reading the nonMatchingChecksum_[date].txt file and applying the checksum updates to postgres for
## each pid.
## Use case: if the postgres database is re-imported from the legacy installation, after hashstore
## conversion has been run, then the incoming values will overwrite the fixed values in the DB
##
## Run this in the metacat pod. It should be located in:
##   /usr/local/tomcat/webapps/metacat/WEB-INF/scripts/bash/k8s/
## It will automatically configure itself from existing env properties and configMap values, and
## will save the "checksums-backup.txt" file to the metacat backups directory (defined as
## "application.backupDir" in the metacat-site.properties file)

set -euo pipefail

source "$(dirname $0)/backup-restore-checksums-table/k8s-settings-initializer.sh"

# Usage help
usage() {
  echo "Usage: $0 [--dry-run]"
  exit 1
}

# Handle optional dry-run argument
dry_run=false
if [[ "${1:-}" == "--dry-run" ]]; then
  dry_run=true
elif [[ $# -gt 0 ]]; then
  usage
fi

# Ensure required environment variables
: "${POSTGRES_PASSWORD:?Need to set POSTGRES_PASSWORD}"
: "${POSTGRES_USER:?Need to set POSTGRES_USER}"
: "${dbName:?Need to set dbName}"
: "${dbHost:?Need to set dbHost}"

# Reset output files
> "$results_file"
> "$updates_file"
> "$failed_log"


echo "READ ENTRIES FROM $logfile: enter to continue or Ctrl+C to abort..."
read

# Write transaction start
echo "BEGIN;" >> "$updates_file"

# Read and parse log
while IFS= read -r line; do
  [[ "$line" != Object:* ]] && continue  # Skip lines that don't start with 'Object:'
  pid=$(echo "$line" | sed -n 's/^.*Object: \(.*\) was converted.*/\1/p')
  new_checksum=$(echo "$line" | sed -n 's/^.*new checksum (\([a-f0-9]*\)).*/\1/p')
  old_file=$(echo "$line" | sed -n 's/^.*for reference: \([^ ]*\)\( Note:.*\)\{0,1\}$/\1/p')

  echo -e "$pid\t$new_checksum\t$old_file" >> "$results_file"

  esc_pid=$(printf "%s" "$pid" | sed "s/'/''/g")
  echo "UPDATE systemmetadata SET checksum = '$new_checksum' WHERE guid = '$esc_pid';" >> "$updates_file"
done < "$logfile"

# Write rollback on error, commit at end
echo "COMMIT;" >> "$updates_file"

if $dry_run; then
  echo "Dry run complete. SQL file and results generated, no database updates applied."
else
  echo "EXECUTING SQL UPDATES FROM $updates_file: enter to continue or Ctrl+C to abort..."
  read

  if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -h "$dbHost" -d "$dbName" -v ON_ERROR_STOP=1 -f "$updates_file" 2> "$failed_log"; then
    echo "Error occurred. Transaction was rolled back."
    echo "See errors in: $failed_log"
  else
    echo "All updates committed successfully."
  fi
fi

echo "Extracted results written to: $results_file"
echo "SQL script: $updates_file"
