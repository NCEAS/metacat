#!/bin/bash
#A script to vacuum the db.
#Usage: nohup ./
DB_VERSION=9.3
echo "start to vacuum the db at" >> /tmp/vacuumdb.out
echo `date`
su - postgres /usr/lib/postgresql/$DB_VERSION/bin/vacuumdb --all >> /tmp/vacuumdb.out 2> /tmp/vacuumdb.err < /dev/null
echo "start to vacuum the db at " >> /tmp/vacuumdb.out
echo `date` >>  /tmp/vacuumdb.out