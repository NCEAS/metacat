#!/bin/sh

DB_NAME=knb
DUMP_FILE=/var/lib/postgresql/knb_dump

echo "start to dump database by a single thread at"
echo `date`

sudo su - postgres -c "pg_dump -Fc $DB_NAME > $DUMP_FILE"
echo "end to dump database by a single thread at"
echo `date`