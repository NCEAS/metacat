#!/bin/bash

## Script to restore the contents of the checksums table. Run this in the metacat pod. It should
## be located in:
##   /usr/local/tomcat/webapps/metacat/WEB-INF/scripts/bash/k8s/backup-restore-checksums-table/
## It will automatically configure itself from existing env properties and configMap values, and
## will restore the table from values found in the "checksums-backup.txt" file that should be
## located in the metacat backups directory (defined as "application.backupDir" in the
## metacat-site.properties file)

source "$(dirname "$0")/k8s-settings-initializer.sh"

echo "POPULATING CHECKSUMS TABLE FROM $path: enter to continue or Ctrl+C to abort..."
read

PGPASSWORD=$POSTGRES_PASSWORD psql -w -U $POSTGRES_USER -d $dbName -h $dbHost <<EOF
BEGIN;
ALTER TABLE checksums ALTER COLUMN checksum_id DROP DEFAULT;
DROP INDEX IF EXISTS checksums_guid;
DROP INDEX IF EXISTS checksums_checksum_algorithm;
DROP INDEX IF EXISTS checksums_checksum;
ALTER TABLE checksums DROP CONSTRAINT IF EXISTS checksums_fk;
\COPY checksums FROM '$path';
ALTER TABLE checksums ALTER COLUMN checksum_id SET DEFAULT nextval('checksums_id_seq');
SELECT setval('checksums_id_seq', (SELECT max(checksum_id) FROM checksums));
CREATE INDEX IF NOT EXISTS checksums_guid ON checksums(guid);
CREATE INDEX IF NOT EXISTS checksums_checksum_algorithm ON checksums(checksum_algorithm);
CREATE INDEX IF NOT EXISTS checksums_checksum ON checksums(checksum);
ALTER TABLE checksums ADD CONSTRAINT checksums_fk FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE;
COMMIT;
EOF
echo "The checksums table was restored from the file ${path} successfully"
