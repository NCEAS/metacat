#!/bin/sh
#This script will send a buildIndex command to 
#remote metacat with docid parameter. Then metacat will build index for this docid.
#This script will have one argument - it is a text file name which contains docid list.
#The text file will look like
#nceas.1.1
#nceas.2.1
#nceas.3.1

if  [ $# -ne 1 ]
 then
    echo "USAGE: $0 docidfile"
    exit 1
fi 
echo 'Please type metacat url (It should look like "http://pacific.msi.ucsb.edu:8080/metacat/metacat")'
read metacatBaseURL
echo 'Please type KNB user name (It should look like "uid=tao,o=NCEAS,dc=ecoinformatics,dc=org")'
read username
echo 'Please type password'
read password
date
#login to metacat
loginurl="$metacatBaseURL?action=login&username=$username&password=$password"
wget -q -O - --save-cookies cookies --keep-session-cookies "$loginurl"

suffix="?action=buildindex"
metacatURL="$metacatBaseURL$suffix"

FILE=$1
# make sure file exist and readable
if [ ! -f $FILE ]; then 
  	echo "$FILE : does not exists"
  	exit 1
elif [ ! -r $FILE ]; then
  	echo "$FILE: can not read"
  	exit 2
fi


index=0
length=3
# read $FILE using the file descriptors
exec 3<&0
exec 0<$FILE
while read line
do
    if [ "$line" != "" ]
    then
      # construct metacat with given length of docid
      metacatURL="$metacatURL&docid=$line"
      index=`expr $index + 1`
	    if [ $index -eq $length ]
	    then
	       echo "here is url $metacatURL"
	       wget -q -O - --load-cookies cookies "$metacatURL"
	       index=0;
	       metacatURL="$metacatBaseURL$suffix"
	    fi
	fi		
done

#This will send out some remained docid
if [ $index -ne 0 ]
then   
    echo "here is url $metacatURL"
    wget -q -O - --load-cookies cookies "$metacatURL"
fi

exec 0<&3
date
exit 0
