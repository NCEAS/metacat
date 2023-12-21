#!/bin/bash

# NONDESTRUCTIVE script to retrieve and then back up locally any `systemmetadata` records without a
# corresponding `identifier` record, that were created or updated after 10/26/2023 - first ever
# possible use of chrome v120 (Android beta - unlikely to be so early, but better to be cautious)
# https://chromereleases.googleblog.com/search?updated-max=2023-10-31T14:06:00-07:00&max-results=7&start=35&by-date=false
#

DATABASE_NAME="metacat"
DATABASE_USER="metacat"

if [ -z "$TOKEN" ]; then
    echo "Requires 'TOKEN' env variable containing JWT token with access to all private packages"
    exit 1
fi
if [ -z "$PGPASSWORD" ]; then
    echo "Requires 'PGPASSWORD' env variable containing password for metacat db user"
    exit 2
fi
if [ -z "$METACAT_BASE_URL" ]; then
    echo "Requires 'METACAT_BASE_URL' env variable containing the protocol, host and optional"
    echo "port number. Examples:"
    echo "        export METACAT_BASE_URL=\"http://localhost:8080\""
    echo "        export METACAT_BASE_URL=\"https://arcticdata.io\""
    exit 3
fi


#1) Run a query against the local PostgreSQL database to get the list of guid, docid, rev,
# date_uploaded, date_modified values:
#

echo "Step 1: Get the list of affected systemmetadata entries from postgres..."

# date (2023-10-26 00:00:00) as a Unix timestamp. Can get via:
# GNU/UNIX:     CUTOFF_DATE=$(date -d "2023-10-26 00:00:00" +%s)
# MacOs:        CUTOFF_DATE=$(date -j -f "%Y-%m-%d %H:%M:%S" "2023-10-26 00:00:00" "+%s")
#CUTOFF_DATE="1698303600"
CUTOFF_DATE=$(date -j -f "%Y-%m-%d %H:%M:%S" "2023-10-26 00:00:00" "+%s")

QUERY_START="SELECT sm.guid, i.docid, i.rev, sm.date_uploaded, sm.date_modified \
FROM systemmetadata sm LEFT JOIN identifier i ON (sm.guid = i.guid) WHERE i.guid IS NULL AND ("

QUERY_END=") ORDER BY date_modified DESC;"

# First query: records where date_uploaded or date_modified is BEFORE or AT midnight on 10/25/2023
QUERY_BC="$QUERY_START \
sm.date_uploaded <= to_timestamp($CUTOFF_DATE) OR sm.date_modified <= to_timestamp($CUTOFF_DATE) \
$QUERY_END"
RESULT_BC=$(psql -h localhost -U $DATABASE_USER -d $DATABASE_NAME -tAX -c "$QUERY_BC")

echo; echo "systemmetadata entries UNRELATED to Chrome v120 bug:"
echo; echo "guid    |    docid    |    rev    |    sm.date_uploaded    |    sm.date_modified"
echo "${RESULT_BC}"

# Second query: records where date_uploaded or date_modified is AFTER midnight on 10/26/2023
QUERY_AC="$QUERY_START \
sm.date_uploaded > to_timestamp($CUTOFF_DATE) OR sm.date_modified > to_timestamp($CUTOFF_DATE) \
$QUERY_END"
RESULT_AC=$(psql -h localhost -U $DATABASE_USER -d $DATABASE_NAME -tAX -c "$QUERY_AC")

echo; echo "systemmetadata entries likely caused by Chrome v120 bug:"
echo; echo "guid    |    docid    |    rev    |    sm.date_uploaded    |    sm.date_modified"
echo "${RESULT_AC}"

#For each value of guid, make a call to API and save the returned XML document to disk in the
# "sysmeta" subdirectory of the current working directory:
#
echo; echo; echo "Making localhost backups of systemmetadata docs"
mkdir -p sysmeta
IFS=$'\n'
COUNT=0
for row in $RESULT_AC; do
    guid=$(echo $row | cut -d '|' -f 1)
    url="$METACAT_BASE_URL/metacat/d1/mn/v2/meta/${guid}"
    echo "Retrieving from: $url"
    curl --silent -H "Authorization: Bearer $TOKEN" -o sysmeta/${guid}.xml $url && COUNT=$(($COUNT+1))
done
unset IFS

echo; echo "FINISHED! Backed up $COUNT systemmetadata files are directory: $(pwd)/sysmeta/"
