#!/bin/sh

#
# Backup files needed for metacat.  This script creates a temporary directory,
# backs up the postgres database, metacat data files, and ldap directory, and 
# then creates a gzipped tar file from those files.  It then writes the backup
# file to a DVD drive and then cleans up any old backup files.
#
# To run this file, install it in /usr/sbin or a similar location and add an
# entry in the root user's crontab to run the command periodically.  The
# following crontab entry would run it every night at 2:00 am
# 0 2 * * *       /usr/sbin/backup-metacat.sh >> $HOME/cron-output 2>&1
#
# This is really just an example script and may not work in your environment
# uless you modify it appropriately.
#
# 13 March 2006 Matt Jones

# Name of the metacat database in postgres to be backed up
DBNAME=metacat

# The day of the week on which the DVD is swapped (1-7, 1=Monday)
# This must be correct or else the write to DVD will not work
SWAPDAY=2

# Number of days of backups to keep online, anything older is removed
DAYSTOKEEP=7

# Device to be used for the DVD writer -- this may vary on your system
DVD=/dev/dvd

# Location of the metacat.properties file
METACATPROPERTIESPATH=/var/lib/tomcat6/webapps/knb/WEB-INF/metacat.properties

# Location of the apache configuration file
APACHECONF=/etc/apache2/sites-enabled

#Location of the server key
KEYLOCATION=/etc/ssl/private 

#Location of the server certificate
CERTLOCATION=/etc/ssl/certs/_.test.dataone.org.crt
#
# Below here lie demons
#

# Set up our umask to protect files from prying eyes
umask 007

# Make a temp dir for the backed up files
TAG=`date +%F-%H%M%S`
DATADIR="/var/metacat"
ARCHROOT="/var/metacat/metacat-backup"
mkdir $ARCHROOT
chgrp postgres $ARCHROOT
chmod g+rwxs $ARCHROOT

ARCHNAME="metacat-backup-$TAG"
ARCHDIR="$ARCHROOT/$ARCHNAME"
mkdir $ARCHDIR

# Shut down the tomcat server so nobody else changes anything while we backup
/etc/init.d/tomcat6 stop

# Shut down ldap too
#/etc/init.d/slapd stop

# Copy the metacat.properties file to /var/metacat
cp $METACATPROPERTIESPATH $DATADIR

# Backup postgres
su - postgres -c "pg_dumpall | gzip > $ARCHDIR/metacat-postgres-backup.gz"

# Backup the data files
tar czhf $ARCHDIR/datafiles-backup.tgz --exclude=$ARCHROOT $DATADIR

# Backup the apache configuration files
tar czhf $ARCHDIR/apache-config-backup.tgz $APACHECONF $KEYLOCATION $CERTLOCATION

# Backup LDAP to an LDIF file
#slapcat -l $ARCHDIR/$DBNAME-ldap.ldif

# Restart LDAP
#/etc/init.d/slapd start

# Restart tomcat
/etc/init.d/tomcat6 start

# Tar up the archive and copy it to archive media
cd $ARCHROOT
tar czhf $ARCHDIR.tgz $ARCHNAME

# Clean up the temp files
rm -rf $ARCHDIR

# Write the backup file to DVD
#DAY=`date +%u`
#DVDFLAG=-M
#if [ $DAY == $SWAPDAY ] ; then
  #DVDFLAG=-Z
#fi
#growisofs $DVDFLAG $DVD -R -J $ARCHDIR.tgz

# clean up any of the backup files that are older than DAYSTOKEEP
find $ARCHROOT -mtime +$DAYSTOKEEP -exec rm -f {} \;
