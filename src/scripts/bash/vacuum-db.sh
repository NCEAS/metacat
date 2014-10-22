#!/bin/bash
#A script to vacuum the db.
#Usage: nohup ./
echo "start to vacuum the db at" >> /tmp/vacuumdb.out
echo `date` >> /tmp/vacuumdb.out
su - postgres /usr/lib/postgresql/9.3/bin/vacuumdb --all >> /tmp/vacuumdb.out 2> /tmp/vacuumdb.err < /dev/null
echo "end to vacuum the db at " >> /tmp/vacuumdb.out
echo `date` >>  /tmp/vacuumdb.out