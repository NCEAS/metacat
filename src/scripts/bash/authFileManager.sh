#!/bin/bash
#A mamanger script to help the Metacat administrator to manage the users and groups
#in the AuthFile mechanism.

echo "This script should be run at the directory of /tomcat-webapps/your-context/WEB-INF/WEB-INF/scripts/bash/."

#variables
#BASE=../../../build/war
#BASE_LIB=$BASE/lib
BASE=../../../
BASE_LIB=$BASE/WEB-INF/lib
BASE_WEB_INF=$BASE/WEB-INF

#run the class
java -classpath "$BASE_LIB/*:" edu.ucsb.nceas.metacat.authentication.AuthFile $BASE_WEB_INF "$@"
