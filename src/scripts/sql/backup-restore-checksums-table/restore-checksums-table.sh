#!/bin/bash
if [ $# -ne 2 ]; then
    echo "Usage: $0 <databaseName> <backupFilePath>"
    exit 1
fi
databaseName=$1
backupFilePath=$2
psql -d "$databaseName" <<EOF
BEGIN;
ALTER TABLE checksums ALTER COLUMN checksum_id DROP DEFAULT;
DROP INDEX IF EXISTS checksums_guid;
DROP INDEX IF EXISTS checksums_checksum_algorithm;
DROP INDEX IF EXISTS checksums_checksum;
ALTER TABLE checksums DROP CONSTRAINT IF EXISTS checksums_fk;
\COPY checksums FROM '$backupFilePath';
ALTER TABLE checksums ALTER COLUMN checksum_id SET DEFAULT nextval('checksums_id_seq');
SELECT setval('checksums_id_seq', (SELECT max(checksum_id) FROM checksums));
CREATE INDEX IF NOT EXISTS checksums_guid ON checksums(guid);
CREATE INDEX IF NOT EXISTS checksums_checksum_algorithm ON checksums(checksum_algorithm);
CREATE INDEX IF NOT EXISTS checksums_checksum ON checksums(checksum);
ALTER TABLE checksums ADD CONSTRAINT checksums_fk FOREIGN KEY (guid) REFERENCES systemMetadata DEFERRABLE;
COMMIT;
EOF
echo "The checksums table was restored from the file ${backupFilePath} successfully"
