#!/bin/bash

## This script is designed to be sourced from other scripts, not run on its own.
## Common functionality for the scripts that back up and restore the checksums table.

if [ -z "$METACAT_IN_K8S" ] || [[ "$METACAT_IN_K8S" != "true" ]]; then
    echo "MUST RUN IN THE METACAT POD! \$METACAT_IN_K8S = $METACAT_IN_K8S"
    exit 1
fi

## should already be available in pod
if [ -z "$POSTGRES_USER" ]; then
    echo "Error: unable to get \$POSTGRES_USER from environment. exiting"
    exit 1
fi
## should already be available in pod
if [ -z "$POSTGRES_PASSWORD" ]; then
    echo "Error: unable to get \$POSTGRES_PASSWORD from environment. exiting"
    exit 1
fi

props_path=/var/metacat/config/metacat-site.properties
props=$(cat $props_path)

dbHost=$(echo $props | awk -F 'database\.connectionURI=jdbc:postgresql://' '{print $2}' | awk -F '/' '{print $1}')
if [ -z "$dbHost" ]; then
    echo "Error: unable to get \$dbHost from site properties: $props_path. exiting"
    exit 1
fi

dbName=$(echo $props | awk -F 'database\.connectionURI=jdbc:postgresql://' '{print $2}' | awk -F'[ /]' '{print $2}')
if [ -z "$dbName" ]; then
    echo "Error: unable to get \$dbName from site properties: $props_path. exiting"
    exit 1
fi

targetDir=$(echo $props |  awk -F 'application\.backupDir=' '{print $2}' | awk -F'[ ]' '{print $1}')
if [ -z "$targetDir" ]; then
    echo "Error: unable to get application.backupDir from site properties: $props_path. exiting"
    exit 1
fi

fileName="checksums-backup.txt"
path="$targetDir/$fileName"
echo "*************************************************************************"
echo "POSTGRES_USER  = $POSTGRES_USER"
echo "dbName         = $dbName"
echo "dbHost         = $dbHost"
echo "*************************************************************************"
