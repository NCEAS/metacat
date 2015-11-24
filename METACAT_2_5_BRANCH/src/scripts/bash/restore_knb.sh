#!/bin/sh
DB_NAME=knb
DUMP_FILE=/var/lib/postgresql/knb_dump
THREAD_NUM=4
echo "start to restore database by $THREAD_NUM threads at"
echo `date`
#sudo su - postgres -c "dropdb $DB_NAME"
#sudo su - postgres -c "createdb $DB_NAME"
sudo su - postgres -c "pg_restore -j $THREAD_NUM -d $DB_NAME $DUMP_FILE"
echo "end to restore database by $THREAD_NUM threads at"
echo `date`
