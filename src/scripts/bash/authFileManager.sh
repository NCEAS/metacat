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
METACAT=$BASE_LIB/metacat.jar
COMMONS_CONFIG=$BASE_LIB/commons-configuration-1.7.jar
COMMONS_LOG=$BASE_LIB/commons-logging-api-1.1.jar
COMMONS_LANG=$BASE_LIB/commons-lang-2.6.jar
COMMONS_COLLECTION=$BASE_LIB/commons-collections-3.2.jar
COMMONS_JXPATH=$BASE_LIB/commons-jxpath-1.3.jar
LOG4J=$BASE_LIB/log4j-1.2.16.jar
UTILITIES=$BASE_LIB/utilities.jar
D1_COMMON=$BASE_LIB/d1_common_java-1.1.3.jar
XALAN=$BASE_LIB/xalan-2.7.0.jar
JBCRYPT=$BASE_LIB/jbcrypt-0.3m.jar

#run the class
java -cp $METACAT:$XALAN:$COMMONS_CONFIG:$COMMONS_LOG:$COMMONS_LANG:$COMMONS_COLLECTION:$COMMONS_JXPATH:$LOG4J:$UTILITIES:$D1_COMMON:$JBCRYPT edu.ucsb.nceas.metacat.authentication.AuthFile $BASE_WEB_INF "$@"
