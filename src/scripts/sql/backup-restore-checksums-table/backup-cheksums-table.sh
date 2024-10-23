#!/bin/bash
if [ $# -ne 2 ]; then
    echo "Usage: $0 <databaseName> <backupDirectory>"
    exit 1
fi
databaseName=$1
targetDir=$2
fileName="checksums-backup.txt"
path="$targetDir/$fileName"
echo $path
psql  -d $databaseName -c "\COPY (SELECT * FROM checksums) TO $path"
