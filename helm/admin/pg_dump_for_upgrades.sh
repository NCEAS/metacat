#!/bin/bash

cat <<EOF

Usage:  $0 RELEASE_NAME

This script is safe and non-destructive. It will never delete your data. It is needed
ONLY when upgrading across PostgreSQL major versions (e.g. v14 -> v17, etc.) See the helm README
for details:
    https://github.com/NCEAS/metacat/blob/main/helm/README.md

It will check your data location to ensure it meets pre-requisites for upgrading, and will then do a
'pg_dump' of your current database contents before you upgrade. The dump will then be automatically
restored to the new database by the new version of the Metacat helm chart.

PREREQUISITES:
- Make sure the "old" version of Metacat Helm chart is running
- Make sure you are in the correct context for the release you want to upgrade
- The old and new data directories must be on the same volume & mount-point.
- Make sure your Values overrides include sufficient 'postgresql.primary.resources' requests &
  limits for cpu & memory, to avoid the pod being 'OOMKilled' during dump/restore. We had success
  with setting cpu: 4 and memory: 32Gi for both requests & limits.

EOF

####################################################################################################
## Functions

# Get a value from Values.yaml, using helm get values --all
# USAGE: get_helm_value <YAML_PATH>
get_helm_value() {
    helm get values --all "$RELEASE_NAME" | yq "$1"
}

# Check that the DATA_DIR_PATH begins with the MOUNT_PATH
# USAGE: check_data_on_mount  <DATA_DIR_PATH>  <MOUNT_PATH>
check_data_on_mount() {
    if [[ "$1" =~ ^"$2" ]]; then
        echo "true"
    else
        echo ""
    fi
}

# Check that Metacat is set to read only mode (.Values.metacat.application.readOnlyMode: true)
# USAGE: check_read_only
check_read_only() {
    READ_ONLY=$(get_helm_value '.metacat."application.readOnlyMode"')
    if [ "$READ_ONLY" = "true" ]; then
        echo; echo -e "$_CHECK_  metacat is in READ-ONLY mode; OK to proceed"
    else
        echo
        echo "* * * * * * * * * * * * * *  IMPORTANT! * * * * * * * * * * * * * * * *"

        echo
        echo -e "$_ALERT_ METACAT MUST BE IN READ-ONLY MODE BEFORE YOU RUN THIS SCRIPT,"
        echo "                 OTHERWISE YOU MAY LOSE DATA!"
        cat <<EOF

        To do this, run 'helm upgrade' with the existing chart version
        (NOT the new chart yet!), and include the command-line option:
          '--set metacat.application\\\\.readOnlyMode=true'
        (NOTE the TWO backslashes!), i.e:

          $ helm upgrade RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \\
                --version {EXISTING-chart-version} \\
                -f {your-values-overrides} \\
                --set metacat.application\\\\.readOnlyMode=true

        THE SCRIPT WILL NOT RUN UNTIL THIS HAS BEEN DONE

    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

EOF
        exit 1
    fi
}

# Find last path segment that matches <VERSION> & Remove trailing slash if found
# USAGE: get_version_from_path  <VERSION> <DATA_DIR_PATH>  <MOUNT_PATH>
get_version_from_path() {
    _NUM_="$(echo "${2#$3}" | grep -o "/$1/\|/$1$" | tail -n 1)"
    echo "${_NUM_%/}"
}

## End Functions
####################################################################################################

_CHECK_="\033[32m\xE2\x9C\x94\033[0m"
_CROSS_="\033[31m\xE2\x9C\x97\033[0m"
_ALERT_="\033[33m\xE2\x96\xB2\033[0m"

if [ -z "$1" ]; then
    echo "Must provide RELEASE_NAME!"
    echo "Usage:    $0 RELEASE_NAME"
    exit 2
else
    RELEASE_NAME=$1
    echo "Using RELEASE_NAME: $RELEASE_NAME";
fi

if ! command -v yq >/dev/null 2>&1; then
    echo "'yq' command-line tool not found; install it before continuing"
    echo "e.g. (Mac OS): \$ brew install yq"
    echo "(If already installed, make sure it is added to your \$PATH)"
fi

check_read_only

PG_DATA=$(get_helm_value ".postgresql.postgresqlDataDir")
PG_MNT=$(get_helm_value ".postgresql.primary.persistence.mountPath")
PG_VER="$(kubectl exec ${RELEASE_NAME}-postgresql-0 -- cat $PG_DATA/PG_VERSION)"
if [ -z "$PG_VER" ]; then
    echo -e "$_CROSS_ ERROR: NO PostgreSQL VERSION FOUND. Is the PostgreSQL container running?"
    exit 3
fi
CTXT=$(kubectl config get-contexts | grep "\*")

echo; echo "CHECK THESE ENVIRONMENT DETAILS ARE CORRECT:"
echo "$CTXT" | awk '{print \
    "  cluster:       " $3 \
    "\n  context:       " $2 \
    "\n  namespace:     " $5}'
echo "  release name:  $RELEASE_NAME"
if [ "$(echo -n "$CTXT" | grep -ci 'prod')" -ne 0 ]; then
    echo -e "$_ALERT_ CAUTION: PRODUCTION ENVIRONMENT?!"
fi

echo; echo "...then:"
echo "* <Enter> to continue (checks that your data location meets requirements for upgrading), or"
echo "* <Ctrl-C> to exit"
read

echo -e "$_CHECK_ postgresql.postgresqlDataDir: $PG_DATA"
echo -e "$_CHECK_ postgresql.primary.persistence.mountPath: $PG_MNT"
echo -e "$_CHECK_ postgresql version: $PG_VER"

# Verify prefix and remove mount point
if [ -z "$(check_data_on_mount $PG_DATA $PG_MNT)" ]; then
    echo; echo -e "$_CROSS_ ERROR: .postgresql.postgresqlDataDir:"
    echo "    $PG_DATA"
    echo "does not start with .postgresql.primary.persistence.mountPath:"
    echo "    $PG_MNT.  Exiting"
    exit 4
fi

# Find last occurrence of the 2- or 3- digit number in path
PATH_VER="$(get_version_from_path $PG_VER $PG_DATA $PG_MNT)"

if [ -n "$PATH_VER" ]; then
    # Get everything up to and including the (last) version number
    PG_BASE="${PG_DATA%%${PG_VER}*}${PG_VER}"
    echo; echo -e "$_CHECK_ postgresqlDataDir IS versioned; OK to proceed"; echo
else
    PG_BASE="${PG_DATA%/*}/${PG_VER}"
    SUGGESTED_PGDATA=$PG_BASE/main
    echo; echo "* * * * * *"
    echo -e "$_ALERT_ ALERT!"
    echo "* * * * * *"; echo
    echo -e "$_CROSS_ Your current PostgreSQL version is $PG_VER. However, the path"
    echo "defined in Values does NOT contain this version number!"
    echo "==============================="
    echo "postrgesql:"
    echo "  postgresqlDataDir: $PG_DATA"
    echo "==============================="
    echo "Data files must be copied to a new, versioned file path, before upgrade can continue."
    echo "Please choose:"
    echo "* <Ctrl-C> to exit without making changes, and take care of this manually, or"
    echo "* <Enter> to copy your data to the suggested path: $SUGGESTED_PGDATA, or"
    until [ -n "$(check_data_on_mount $USR_DEST $PG_MNT)" ] \
        && [ -n "$(get_version_from_path $PG_VER $USR_DEST $PG_MNT)" ]; do
        echo "* type the new destination path, where the data will be copied..."
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

    # Copy the data from old to new location, from within the postgresql container
    command time -h kubectl exec ${RELEASE_NAME}-postgresql-0 -- \
        env OLD_DATA=${PG_DATA} NEW_DATA=${TARGET} _ALERT=${_ALERT_} \
        bash -c '
            if [ -d $NEW_DATA ]; then
                echo -e "$_ALERT ALERT: DIRECTORY ALREADY FOUND AT: $NEW_DATA. WILL NOT OVERWRITE!";
                exit 5;
            fi;
            echo "Creating new data location: $NEW_DATA";
            mkdir -p $NEW_DATA;
            shopt -s dotglob;
            echo "Copying data from $OLD_DATA to $NEW_DATA...";
            cp -pr $OLD_DATA/* $NEW_DATA/;
            echo "...Done\!";
            echo
            echo -e "$_ALERT === IMPORTANT NEXT STEPS: ===";
            echo
            echo "1. EDIT YOUR VALUES OVERRIDES to set the new PostgreSQL Data location to:";
            echo "   -------------------------------";
            echo "   postrgesql:";
            echo "     postgresqlDataDir: $NEW_DATA";
            echo "   -------------------------------";
            echo
            echo "2. Do a helm upgrade; remember to include the read-only command-line option\!:";
            echo "     --set metacat.application\\\\.readOnlyMode=true    ## 2 backslashes\!)";
            echo
            echo "3. Make sure Metacat is working correctly: you should see a non-zero number of"
            echo "   objects in the database returned when you browse:"
            echo "     https://YOUR-HOST/metacat/d1/mn/v2/object"
            echo
            echo "Time taken to move data:";
        '
    echo "Exiting. After you have completed all the \"=== IMPORTANT NEXT STEPS: ===\", above,"
    echo "please run this script again, and follow the instructions to do the pg_dump";
    exit 6
fi

PG_DUMP_DIR="${PG_BASE}-pg_dump"

echo "* <Enter> to create a pg_dump at the location: $PG_DUMP_DIR, or"
echo "* <Ctrl-C> to exit"
read

# do the pg_dump, from within the postgresql container
command time -h kubectl exec ${RELEASE_NAME}-postgresql-0 -- \
    env DUMP_DIR=${PG_DUMP_DIR} _CROSS=${_CROSS_} _ALERT=${_ALERT_} \
    bash -c '
        if [ -d $DUMP_DIR ]; then
          DUMP_BAK=${DUMP_DIR}-moved-"$(date +%s)";
          echo -e "$_ALERT ALERT: pg_dump already exists at: ${DUMP_DIR}\!";
          echo "- moving it to $DUMP_BAK...";
          mv -f $DUMP_DIR $DUMP_BAK;
          echo "...Done\!";
        fi;
        PGDB=$POSTGRES_DB;
        if [ -z "$PGDB" ]; then
            PGDB=$POSTGRES_DATABASE;
        fi;
        if [ -z "$PGDB" ] || [ -z "$POSTGRES_USER" ]; then
            echo -e "$_CROSS ERROR: One or more environment variables not found:";
            echo "\$POSTGRES_USER: ($POSTGRES_USER)";
            echo "\$PGDB: ($PGDB)";
            echo "- from \$POSTGRES_DB: ($POSTGRES_DB) or \$POSTGRES_DATABASE ($POSTGRES_DATABASE)";
            echo "Exiting"
            exit 7;
        fi
        echo; echo "Running pg_dump command:";
        echo "pg_dump -U $POSTGRES_USER --format=directory --file=$DUMP_DIR --jobs=20 $PGDB";
        pg_dump -U $POSTGRES_USER --format=directory --file=$DUMP_DIR --jobs=20 $PGDB;
        echo "...Done\!";
        echo "Time taken:";
    '

echo; echo -e "$_ALERT_ === IMPORTANT -- TO COMPLETE THE POSTGRESQL UPGRADE: ==="

PG_PVC=$(kubectl get pvc | grep "$RELEASE_NAME" | grep -o "^[^ ]*$RELEASE_NAME-postgres[^ ]*")
if [ -z "$PG_PVC" ]; then
    PG_PVC="[existing-postgresql-pvc-name-here]"
fi

cat <<EOF

1. Make sure to edit your Values overrides and set 'postgresql.upgrader.persistence.existingClaim',
   so the upgrader knows where to find the existing PostgreSQL PVC (and therefore the dump files):

     postgresql:
       upgrader:
         persistence:
          existingClaim: $PG_PVC

2. Now uninstall the old chart (recommended instead of using helm upgrade):

     helm uninstall $RELEASE_NAME

3. Finally, install the NEW Metacat chart. This will automatically detect the pg_dump you just
   created, and will use it to 'pg_restore' this data into the new version of PostgreSQL.
   For example:

    $ helm install $RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \\
        --version [NEW-chart-version] \\
        -f [your-values-overrides]

See the upgrade section of the helm README for troubleshooting help:
    https://github.com/NCEAS/metacat/blob/main/helm/README.md
EOF
