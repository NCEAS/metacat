#!/bin/bash
if [ $# -ne 2 ]; then
    echo "Usage: $0 <databaseName> <backupFilePath>"
    exit 1
fi
databaseName=$1
backupFilePath=$2
psql  -d $databaseName -c "\set AUTOCOMMIT OFF"
###Drop indexes and other stuff
psql  -d $databaseName -c "ALTER TABLE checksums ALTER COLUMN checksum_id DROP DEFAULT;"
psql  -d $databaseName -c "DROP INDEX IF EXISTS checksums_guid;"
psql  -d $databaseName -c "DROP INDEX IF EXISTS checksums_checksum_algorithm;"
psql  -d $databaseName -c "DROP INDEX IF EXISTS checksums_checksum;"
psql  -d $databaseName -c "ALTER TABLE checksums DROP CONSTRAINT checksums_fk;"
psql  -d $databaseName -c "\COPY checksums FROM $backupFilePath"
psql  -d $databaseName -c "ALTER TABLE checksums ALTER COLUMN checksum_id SET DEFAULT nextval('checksums_id_seq');"
psql  -d $databaseName -c "SELECT setval('checksums_id_seq', (SELECT max(checksum_id) FROM checksums));"
psql  -d $databaseName -c "CREATE INDEX IF NOT EXISTS checksums_guid on checksums(guid);"
psql  -d $databaseName -c "CREATE INDEX IF NOT EXISTS checksums_checksum_algorithm on checksums(checksum_algorithm);"
psql  -d $databaseName -c "CREATE INDEX IF NOT EXISTS checksums_checksum on checksums(checksum);"
psql  -d $databaseName -c "ALTER TABLE checksums ADD CONSTRAINT checksums_fk FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE;"
psql  -d $databaseName -c "COMMIT;"
echo "The checksums table was restored from the file ${backupFilePath} successfully"
