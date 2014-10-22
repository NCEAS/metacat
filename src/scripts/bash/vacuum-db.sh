#!/bin/bash
#A script to vacuum the db.
#Usage: nohup ./
DB_VERSION=9.3
echo "start to vacuum the db at" >> /tmp/vacuumdb.out
echo `date` >> /tmp/vacuumdb.out
su - postgres  -c "/usr/lib/postgresql/$DB_VERSION/bin/vacuumdb --all >> /tmp/vacuumdb.out 2> /tmp/vacuumdb.err < /dev/null"
echo "end to vacuum the db at " >> /tmp/vacuumdb.out
echo `date` >>  /tmp/vacuumdb.out
