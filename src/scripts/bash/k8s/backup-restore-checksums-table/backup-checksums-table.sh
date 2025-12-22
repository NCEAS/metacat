#!/bin/bash

## Script to back up the contents of the checksums table. Run this in the metacat pod. It should
## be located in:
##   /usr/local/tomcat/webapps/metacat/WEB-INF/scripts/bash/k8s/backup-restore-checksums-table/
## It will automatically configure itself from existing env properties and configMap values, and
## will save the "checksums-backup.txt" file to the metacat backups directory (defined as
## "application.backupDir" in the metacat-site.properties file)

source "$(dirname "$0")/k8s-settings-initializer.sh"

echo "BACK UP CHECKSUMS TABLE TO $path: enter to continue or Ctrl+C to abort..."
read

PGPASSWORD=$POSTGRES_PASSWORD \
    psql -w -U $POSTGRES_USER -d $dbName -h $dbHost  -c "\COPY (SELECT * FROM checksums) TO $path"

echo "The checksums table was backuped to the file ${path} successfully"
