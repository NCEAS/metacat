#!/bin/sh

#This script is used to move the data of a postgresql 8.4 instance to the
#new installed postgresql 9.3 instance after the os was upgraded from ubuntu 10.04 to 14.04.
#Basically it will decompress a metacat backup file which contains a dumped-all file and
#resotre it in a postgresql 9.3 instance.
#You need to pass the metacat backup file to this script. We assume the backup file 
#locating at /var/metacat/metacat-backup directory.
#This script should be run as the root user. 
#Usage: nohup ./restore-dumped-data-to-pg9.3.sh metacat_backup_file_name &

#Check the argument of the script. It only can have one - the metacat backup file name.

METACAT_BACKUP_DIR=/var/metacat/metacat-backup
SQL_FILE=metacat-postgres-backup.gz
METACAT_BACKUP_FILE_SUFFIX=.tgz
DB_BASE=/var/lib/postgresql
OLD_DB_VERSION=8.4
ANOTHER_OLD_DB_VERSION=9.1
NEW_DB_VERSION=9.3
NEW_DB_CONFIG=/etc/postgresql/$NEW_DB_VERSION/main/postgresql.conf
OLD_DB_CONFIG=/etc/postgresql/$OLD_DB_VERSION/main/postgresql.conf
OLD_DB_DATA_DIR=$DB_BASE/$OLD_DB_VERSION
OLD_DB_BACKUP_FILE=postgresql-$OLD_DB_VERSION.tar.gz
POSTGRES_USER=postgres
PORT=5432
PORT1=5435
echo "start to move database from $OLD_DB_VERSION to $NEW_DB_VERSION at"
echo `date`

echo "the length of argument is $#"
#echo $@
if [ $# -ne 1 ]; then
   echo "This script should take one and only one parameter as the metacat backup file name.";
   exit 1;
fi
METACAT_BACKUP_FILE_NAME=$1
echo "the backup file name is $METACAT_BACKUP_FILE_NAME"

if [ -f $METACAT_BACKUP_DIR/$METACAT_BACKUP_FILE_NAME ]; then
    echo "the metacat backup file $METACAT_BACKUP_DIR/$METACAT_BACKUP_FILE_NAME exists.";
else 
    echo "the metacat backup file $METACAT_BACKUP_DIR/$METACAT_BACKUP_FILE_NAME doesn't exist, please double check the name.";
    exit 1;
fi

echo "stop apache"
/etc/init.d/apache2 stop

echo "stop tomcat"
/etc/init.d/tomcat6 stop

echo "stop postgresql"
/etc/init.d/postgresql stop

DECOMPRESS_DIR_NAME=${METACAT_BACKUP_FILE_NAME%%.*}
echo "the decmporessed dir is $DECOMPRESS_DIR_NAME"

if [ -f "$DB_BASE/$OLD_DB_BACKUP_FILE" ]; then
        echo "$DB_BASE/$OLD_DB_BACKUP_FILE does exist and we don't need to backup it again"   
else
	echo "back up the old db data at $OLD_DB_DATA_DIR"
        su - $POSTGRES_USER -c "tar -zcvf $DB_BASE/$OLD_DB_BACKUP_FILE $OLD_DB_DATA_DIR"
	#echo "delete the data directory - $OLD_DB_DATA_DIR"
	#rm -rf $OLD_DB_DATA_DIR/main/*
fi

#echo "remove postgresql 8.4 and 9.1"

#apt-get -y remove postgresql-$OLD_DB_VERSION
#apt-get -y remove postgresql-$ANOTHER_OLD_DB_VERSION

echo "modify the port to 5435 in the old new db configuraiton file"
sed -i.bak --regexp-extended "s/(port =).*/\1${PORT1}/;" $OLD_DB_CONFIG

echo "modify the port to 5432 in the new db configuration file"
sed -i.bak --regexp-extended "s/(port =).*/\1${PORT}/;" $NEW_DB_CONFIG

if [ -d "$METACAT_BACKUP_DIR/$DECOMPRESS_DIR_NAME" ]; then
        echo "$METACAT_BACKUP_DIR/$DECOMPRESS_DIR_NAME does exist and we don't need to decompress the metacat backup file again"   
else
	echo "decompress the metacat backup file"
	tar zxvf $METACAT_BACKUP_DIR/$METACAT_BACKUP_FILE_NAME -C $METACAT_BACKUP_DIR
fi

echo "restart postgresql"
/etc/init.d/postgresql start

echo "change the groupship of $METACAT_BACKUP_DIR"
chown -R :$POSTGRES_USER $METACAT_BACKUP_DIR

echo "restore database"
su - $POSTGRES_USER -c "gunzip -c $METACAT_BACKUP_DIR/$DECOMPRESS_DIR_NAME/$SQL_FILE | psql postgres"

echo "end to move database from $OLD_DB_VERSION to $NEW_DB_VERSION at"
echo `date`

echo "start to vacuum the db at"
echo `date` >> /tmp/vacuumdb.out
su - postgres  -c "/usr/lib/postgresql/$NEW_DB_VERSION/bin/vacuumdb --all"
echo "end to vacuum the db at "
echo `date`
exit 0
