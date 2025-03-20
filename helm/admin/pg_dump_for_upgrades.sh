#!/bin/bash

cat <<EOF
Usage:  $0 RELEASE_NAME

This script is safe and non-destructive. It will never delete your data. It is needed
ONLY when
upgrading across PostgreSQL major versions (e.g. v14 -> v17, etc.) See the helm README for details:
    https://github.com/NCEAS/metacat/blob/main/helm/README.md

It will check your data location to ensure it meets pre-requisites for upgrading, and will then do a
'pg_dump' of your current database contents before you upgrade. The dump will then be automatically
restored to the new database by the new version of the Metacat helm chart.

PREREQUISITES:
- Make sure the "old" version of Metacat Helm chart is running
- Make sure you are in the correct context for the release you want to upgrade

EOF

echo "Hit <Enter> to check if your data location meets pre-requisites for upgrading, or"
echo "Ctrl-C to exit"
read

####################################################################################################
## Functions

# Check that the DATA_DIR_PATH begins with the MOUNT_PATH
# USAGE: check_data_on_mount  <DATA_DIR_PATH>  <MOUNT_PATH>
check_data_on_mount() {
    if [[ "$1" =~ ^"$2" ]]; then
        echo "true"
    else
        echo ""
    fi
}

# Find last path segment that matches <VERSION> & Remove trailing slash if found
# USAGE: get_version_from_path  <VERSION> <DATA_DIR_PATH>  <MOUNT_PATH>
get_version_from_path() {
    _NUM_="$(echo "${2#$3}" | grep -o "/$1/\|/$1$" | tail -n 1)"
#    _NUM_="$(echo "${2#$3}" | grep -o '/[0-9]\{2,3\}/\|/[0-9]\{2,3\}$' | tail -n 1)"
    echo ${_NUM_%/}
}

## End Functions
####################################################################################################

if [ -z "$1" ]; then
    echo "Usage:    $0 RELEASE_NAME"
else
    RELEASE_NAME=$1
    echo "Using RELEASE_NAME: $RELEASE_NAME"
fi

if ! command -v yq >/dev/null 2>&1; then
    echo "'yq' command-line tool not found; install it before continuing"
    echo "e.g. (Mac OS): \$ brew install yq"
    echo "(If already installed, make sure it is added to your \$PATH)"
fi

READ_ONLY=$(helm get values --all "$RELEASE_NAME" | yq '.metacat."application.readOnlyMode"')
if [ "$READ_ONLY" = "true" ]; then
    echo "metacat is in READ-ONLY mode; check passed"
else
    cat <<EOF
    * * * * * * * * * * * * * * * * * * * * *  IMPORTANT! * * * * * * * * * * * * * * * * * * * * *

    METACAT MUST BE IN READ-ONLY MODE BEFORE YOU RUN THIS SCRIPT, OTHERWISE YOU MAY LOSE DATA!

    To do this, run `helm upgrade` with the existing chart version - don't use the new chart yet -
    and include the command-line option: '--set metacat.application\\.readOnlyMode=true' (NOTE the
    two backslashes!), i.e:

      $ helm upgrade RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \
            --version {EXISTING-chart-version} \
            -f {your-values-overrides} \
            --set metacat.application\\.readOnlyMode=true

    THE SCRIPT WILL NOT RUN UNTIL THIS HAS BEEN DONE

    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
EOF
fi

#PG_DATA=$(helm get values --all "$RELEASE_NAME" | yq '.postgresql.postgresqlDataDir')
PG_DATA=/bitnami/postgresql/14/main

PG_MNT=$(helm get values --all "$RELEASE_NAME" | yq '.postgresql.primary.persistence.mountPath')

PG_VER=$(kubectl exec ${RELEASE_NAME}-postgresql-0 -- cat $PG_DATA/PG_VERSION)
if [ -z "$PG_VER" ]; then
    echo "ERROR: NO PostgreSQL VERSION FOUND. Is the PostgreSQL container running?"
    exit 1
fi

echo "postgresql.postgresqlDataDir: $PG_DATA"
echo "postgresql.primary.persistence.mountPath: $PG_MNT"
echo "postgresql version: $PG_VER"

# Verify prefix and remove mount point
#if [[ ! "$PG_DATA" =~ ^"$PG_MNT" ]]; then
if [ -z "$(check_data_on_mount $PG_DATA $PG_MNT)" ]; then
    echo; echo "ERROR: .postgresql.postgresqlDataDir:"
    echo "    $PG_DATA"
    echo "does not start with .postgresql.primary.persistence.mountPath:"
    echo "    $PG_MNT.  Exiting"
    exit 2
fi

# Find last occurrence of 2/3 digit number in path
PATH_VER="$(get_version_from_path $PG_VER $PG_DATA $PG_MNT)"

if [ -n "$PATH_VER" ]; then
    # Get everything up to and including the last number
    PG_BASE="${PG_DATA%%${PG_VER}*}${PG_VER}"
    echo; echo "postgresqlDataDir IS versioned"; echo
else
    PG_BASE="${PG_DATA%/*}/${PG_VER}"
    SUGGESTED_PGDATA=$PG_BASE/main
    echo; echo "* * * * * *"
    echo "  ALERT!"
    echo "* * * * * *"; echo
    echo "Your current PostgreSQL version is $PG_VER. However, the path"
    echo "defined in Values does NOT contain this version number!"
    echo "==============================="
    echo "postrgesql:"
    echo "  postgresqlDataDir: $PG_DATA"
    echo "==============================="
    echo "Data files must be copied to a new, versioned file path, before upgrade can continue."
    echo "Please either:"
    echo "* Ctrl-C to exit without making changes, and take care of this manually, or"
    echo "* Hit <Enter> to copy your data to the suggested path: $SUGGESTED_PGDATA, or"
    until [ -n "$(check_data_on_mount $USR_DEST $PG_MNT)" ] \
        && [ -n "$(get_version_from_path $PG_VER $USR_DEST $PG_MNT)" ]; do
        echo "* input the new destination path, where the data will be copied..."
        echo "    - MUST start with $PG_MNT, AND include the current version: $PG_VER --"
        echo "      - Example: $PG_MNT/my/directory/$PG_VER/main"; echo
        USR_DEST=""
        read -rp "Path: " USR_DEST
        if [ -z "$USR_DEST" ]; then
            USR_DEST=$SUGGESTED_PGDATA
        fi
    done
    TARGET=$USR_DEST
    PG_BASE="${TARGET%%${PG_VER}*}${PG_VER}"

    # Copy the data from old to new location
    time kubectl exec ${RELEASE_NAME}-postgresql-0 -- env OLD_DATA=${PG_DATA} NEW_DATA=${TARGET} \
        bash -c '
            if [ -d $NEW_DATA ]; then
                echo "ERROR: DIRECTORY ALREADY EXISTS AT: $NEW_DATA. WILL NOT OVERWRITE!";
                exit 3;
            fi;
            echo "Creating new data location: $NEW_DATA";
            mkdir -p $NEW_DATA;
            shopt -s dotglob;
            echo "Copying data from $OLD_DATA to $NEW_DATA";
            cp -pr $OLD_DATA/* $NEW_DATA/;
            echo "Done!";
            echo "=== IMPORTANT NEXT STEPS: ===";
            echo "1. EDIT YOUR VALUES OVERRIDES to set the new PostgreSQL Data location to:";
            echo "   -------------------------------";
            echo "   postrgesql:";
            echo "     postgresqlDataDir: $NEW_DATA";
            echo "   -------------------------------";
            echo "2. Do a "helm upgrade"; remember to include the read-only command-line option!:";
            echo "     --set metacat.application\\\\.readOnlyMode=true    ## 2 backslashes!)";
            echo "3. Run this script again, and follow the instructions to do the pg_dump";
        '
fi

PG_DUMP_DIR="${PG_BASE}-pg_dump"

echo "Hit <Enter> to create a pg_dump at the location: $PG_DUMP_DIR, or Ctrl-C to exit"
read

time kubectl exec ${RELEASE_NAME}-postgresql-0 -- env DUMP_DIR=${PG_DUMP_DIR} bash -c '
    if [ -d $DUMP_DIR ]; then
      DUMP_BAK=${DUMP_DIR}-moved-"$(date +%s)";
      echo "pg_dump already exists at: $DUMP_DIR -- moving it to $DUMP_BAK";
      mv -f $DUMP_DIR $DUMP_BAK;
    fi;
    echo "Running pg_dump command:"
    echo "pg_dump -U $POSTGRES_USER --format=directory --file=${DUMP_DIR} --jobs=20 $POSTGRES_DB";
    pg_dump -U $POSTGRES_USER --format=directory --file=${DUMP_DIR} --jobs=20 $POSTGRES_DB;
    echo "Done!";
';

cat <<EOF

=== IMPORTANT -- TO COMPLETE THE POSTGRESQL UPGRADE: ===

1. Make sure to edit your Values overrides and set 'postgresql.upgrader.persistence.existingClaim',
   so the upgrader knows where to find the existing PostgreSQL PVC (and therefore the dump files):

     postgresql:
       upgrader:
         persistence:
          existingClaim: {existing-postgresql-pvc-name-here}

2. Now do a 'helm upgrade' using the NEW Metacat chart. This will automatically detect the pg_dump
   you just created, and will use it to 'pg_restore' this data into the new version of PostgreSQL.
   For example:

    $ helm upgrade $RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \\
        --version {NEW-chart-version} \\
        -f {your-values-overrides}

    (Upgrade WITHOUT the read-only command-line option, in order to unset "Read Only" mode, so
    Metacat will once again accept edits and uploads when the upgrade has finished.)
EOF
