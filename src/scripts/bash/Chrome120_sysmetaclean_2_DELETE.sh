#!/bin/bash

#
# Script to * * * DELETE * * *  from the database, any systemmetadata
# records without an identifier record, that were created or updated after 10/26/2023 - first ever
# possible use of chrome v120 (Android beta) - unlikely to be so early, but better to be cautious
# https://chromereleases.googleblog.com/search?updated-max=2023-10-31T14:06:00-07:00&max-results=7&start=35&by-date=false
#
# DELETE *should* be safe, because without an identifier record, we have no way of linking the
# sysmeta to any other tables, as they all use docid/rev as their primary keys (xml_documents,
# xml_revisions, xml_access, etc).
#
# Files are also named by their docid/rev, so we have no way of tracking the sysmeta to a file path
#

DATABASE_NAME="metacat"
DATABASE_USER="metacat"


if [[ -z $1 ]] || [[ $1 -ne "-CONFIRM_DELETE" ]]; then
    echo "* * * * * *  C A U T I O N  * * * * * *"
    echo "This script DELETES database entries. Please make sure"
    echo "you have done the following before executing:"
    echo "  First, read the comments in this file."
    echo "  Then ensure that the following values are correct:"
    echo "    DATABASE_NAME: $DATABASE_NAME"
    echo "    DATABASE_USER: $DATABASE_USER"
    echo "  (if not, edit the values bin this file.)"
    echo "Finally, when you are sure you want to proceed, use the command:"
    echo "        $0 -CONFIRM_DELETE"
    echo
    exit 1
fi
if [ -z "$PGPASSWORD" ]; then
    echo "WARNING: Optional 'PGPASSWORD' env variable containing database user password not set"
    echo "You may get prompted to enter the password"
fi

# date (2023-10-26 00:00:00) as a Unix timestamp. Can get via:
# GNU/UNIX:     CUTOFF_DATE=$(date -d "2023-10-26 00:00:00" +%s)
# MacOs:        CUTOFF_DATE=$(date -j -f "%Y-%m-%d %H:%M:%S" "2023-10-26 00:00:00" "+%s")
# but "1698303600" is the final unix timestamp value for 2023-10-26 00:00:00
#
CUTOFF_DATE="1698303600"

SELECT_QUERY=\
"SELECT sm.guid                                                                                \
FROM systemmetadata sm LEFT JOIN identifier i ON (sm.guid = i.guid) WHERE i.guid IS NULL \
AND sm.obsoleted_by IS NULL AND sm.obsoletes IS NULL AND ( \
sm.date_uploaded > to_timestamp($CUTOFF_DATE) OR sm.date_modified > to_timestamp($CUTOFF_DATE) \
) ORDER BY date_modified DESC"
SELECT_RESULTS=$(psql -h localhost -U $DATABASE_USER -d $DATABASE_NAME -tAX -c "$SELECT_QUERY")

ROW_COUNT=$(echo $SELECT_RESULTS | wc -w)

echo; echo "Found guids for $ROW_COUNT systemmetadata entries likely caused by Chrome v120 bug:"
echo "${SELECT_RESULTS}"

echo; echo "NOW DELETING these $ROW_COUNT systemmetadata entries..."

DELETE_QUERY="DELETE from systemmetadata sm WHERE sm.guid in ( $SELECT_QUERY );"

DELETE_RESULTS=$(psql -h localhost -U $DATABASE_USER -d $DATABASE_NAME -tAX -c "$DELETE_QUERY")

echo "DELETED!"
echo
echo "${DELETE_RESULTS}"
