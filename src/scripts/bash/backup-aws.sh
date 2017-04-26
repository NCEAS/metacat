#!/bin/sh

#
# Backup files needed for metacat.  This script creates a directory in /var/metacat/backup,
# backs up the postgres database, metacat data files, and certificate and web config files,
# then syncs those files to an Amazon S3 bucket for backup.
#
# To run this file, install it in /usr/sbin or a similar location and add an
# entry in the root user's crontab to run the command periodically.  The
# following crontab entry would run it every night at 2:00 am
# 0 2 * * *       /usr/sbin/backup-aws.sh >> $HOME/cron-output 2>&1
#
# This is really just an example script and may not work in your environment
# uless you modify it appropriately.
#
# 25 Apr 2017 Matt Jones

# Name of the metacat database in postgres to be backed up
DBNAME=metacat

# Number of days of backups to keep online, anything older is removed
DAYSTOKEEP=7

# AWS S3 bucket to be used for backup
BUCKET=s3://arcticdata.io/backup

# Location of the metacat.properties file
METACATPROPERTIESPATH=/var/lib/tomcat7/webapps/metacat/WEB-INF/metacat.properties

# Location of the apache configuration file
APACHECONF=/etc/apache2/sites-enabled

#Location of the server key
KEYLOCATION=/etc/ssl/private 

#Location of the server certificate
CERTLOCATION=/etc/ssl/certs/www_arcticdata_io.crt
#
# Below here lie demons
#

# Set up our umask to protect files from prying eyes
umask 007

# Make a temp dir for the backed up files
TAG=`date +%F-%H%M%S`
DATADIR="/var/metacat"
ARCHROOT="/var/metacat/metacat-backup"
mkdir -p $ARCHROOT
chgrp postgres $ARCHROOT
chmod g+rwxs $ARCHROOT

ARCHDIR="$ARCHROOT"
mkdir -p $ARCHDIR

# Shut down the tomcat server so nobody else changes anything while we backup
#/etc/init.d/tomcat7 stop

echo Copy the metacat.properties file to /var/metacat
cp $METACATPROPERTIESPATH $ARCHDIR

echo Backup postgres
su - postgres -c "pg_dumpall | gzip > $ARCHDIR/metacat-postgres-backup.gz"

echo Copy the apache configuration files
tar czhf $ARCHDIR/apache-config-backup.tgz $APACHECONF $KEYLOCATION $CERTLOCATION

echo Sync the backup directory to Amazon S3
aws s3 sync $DATADIR $BUCKET

# Restart tomcat
#/etc/init.d/tomcat7 start

# Clean up the temp files
#rm -rf $ARCHDIR

# clean up any of the backup files that are older than DAYSTOKEEP
#find $ARCHROOT -mtime +$DAYSTOKEEP -exec rm -f {} \;

echo "DONE backup for $TAG"

