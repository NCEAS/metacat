#!/bin/bash
#This script will install openjdk-7 and tomcat7.
#It will update the alternatives for java, javac, keytool and javaws to openjdk-7.
#It will modify the /etc/tomcat7/catalina.properties to allow DataONE idenifiers.
#It will modify the workers.properties file for apache-tomcat connector.
#It will move Metacat and other web applications from the old context directory to the new context directory.
#The user running the script should have the sudo permission.

NEW_JDK_PACKAGE=openjdk-7-jdk
NEW_JDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64

JK_CONF=/etc/apache2/mods-enabled/jk.conf

OLD_TOMCAT=tomcat6
OLD_TOMCAT_BASE=/var/lib/${OLD_TOMCAT}

NEW_TOMCAT=tomcat7
NEW_CATALINA_PROPERTIES=/etc/${NEW_TOMCAT}/catalina.properties
NEW_TOMCAT_HOME=/usr/share/${NEW_TOMCAT}
NEW_TOMCAT_BASE=/var/lib/${NEW_TOMCAT}

KNB=knb
METACAT=metacat
WEBAPPS=webapps
TOMCAT_CONFIG_SLASH='org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true'
TOMCAT_CONFIG_BACKSLASH='org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true'
INIT_START_DIR=/etc/init.d

echo "install ${NEW_JDK_PACKAGE}"
sudo apt-get install ${NEW_JDK_PACKAGE}
sleep 3
echo "configure java, java, keytool and javaws"
sudo update-alternatives --set java ${NEW_JDK_HOME}/jre/bin/java
sudo update-alternatives --set javac ${NEW_JDK_HOME}/bin/javac
sudo update-alternatives --set keytool ${NEW_JDK_HOME}/jre/bin/keytool
sudo update-alternatives --set javaws ${NEW_JDK_HOME}/jre/bin/javaws

echo "install ${NEW_TOMCAT}"
sudo apt-get install ${NEW_TOMCAT}
echo "configure ${NEW_TOMCAT}"
if grep -q "${TOMCAT_CONFIG_SLASH}" ${NEW_CATALINA_PROPERTIES}; then  
echo "${TOMCAT_CONFIG_SLASH} exists and don't need to do anything."  
else  
   echo "${TOMCAT_CONFIG_SLASH} don't exist and add it."
   sudo sed -i.bak "$ a\\${TOMCAT_CONFIG_SLASH}" ${NEW_CATALINA_PROPERTIES} 
fi
if grep -q "${TOMCAT_CONFIG_BACKSLASH}" ${NEW_CATALINA_PROPERTIES}; then
echo "${TOMCAT_CONFIG_BACKSLASH} exists and don't need to do anything."  
else
   echo "${TOMCAT_CONFIG_BACKSLASH} don't exist and add it."
   sudo sed -i "$ a\\${TOMCAT_CONFIG_BACKSLASH}" ${NEW_CATALINA_PROPERTIES}
fi


echo "read the location of the workers.properties file from the jk_conf"
while read f1 f2 
do
        if [ "$f1" = "JkWorkersFile" ]; then
		JK_WORKER_PATH="$f2"	
	fi
done < ${JK_CONF}
echo "the jk workers.properties location is $JK_WORKER_PATH"

echo "update the tomcat home and java home in workers.properties file"
SAFE_NEW_TOMCAT_HOME=$(printf '%s\n' "$NEW_TOMCAT_HOME" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
SAFE_NEW_JDK_HOME=$(printf '%s\n' "$NEW_JDK_HOME" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
sudo sed -i.bak --regexp-extended "s/(workers\.tomcat_home=).*/\1${SAFE_NEW_TOMCAT_HOME}/;
                s/(workers\.java_home=).*/\1${SAFE_NEW_JDK_HOME}/;"\
                $JK_WORKER_PATH

echo "move Metacat and other web applications from $OLD_TOMCAT to $NEW_TOMCAT"
sudo ${INIT_START_DIR}/${OLD_TOMCAT} stop
sudo ${INIT_START_DIR}/${NEW_TOMCAT} stop
sudo rm -rf ${NEW_TOMCAT_BASE}/${WEBAPPS}/*
sudo cp -R ${OLD_TOMCAT_BASE}/${WEBAPPS}/*  ${NEW_TOMCAT_BASE}/${WEBAPPS}/.
sudo chown -R ${NEW_TOMCAT}:${NEW_TOMCAT} ${NEW_TOMCAT_BASE}/${WEBAPPS}/*
echo "change the value of the application.deployDir in the metacat.properties file"
SAFE_NEW_TOMCAT_WEBAPPS=$(printf '%s\n' "$NEW_TOMCAT_BASE/$WEBAPPS" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
#echo "the escaped webpass value is ${SAFE_NEW_TOMCAT_WEBAPPS}"
if [ -f "$NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties" ]; then
	echo "$NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties exists and the application.deployDir will be updated"	
	sudo sed -i.bak --regexp-extended "s/(application\.deployDir=).*/\1${SAFE_NEW_TOMCAT_WEBAPPS}/;" $NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties
else
	echo "$NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties does NOT exists and the application.deployDir will NOT be updated"
fi

if [ -f "$NEW_TOMCAT_BASE/$WEBAPPS/$METACAT/WEB-INF/metacat.properties" ]; then
                echo "$NEW_TOMCAT_BASE/$WEBAPPS/$METACAT/WEB-INF/metacat.properties eixsts and the application.deployDir will be updated" 
                sudo sed -i.bak --regexp-extended "s/(application\.deployDir=).*/\1${SAFE_NEW_TOMCAT_WEBAPPS}/;" $NEW_TOMCAT_BASE/$WEBAPPS/$METACAT/WEB-INF/metacat.properties
else 
  echo "$NEW_TOMCAT_BASE/$WEBAPPS/$METACAT/WEB-INF/metacat.properties doesn't eixt and the application.deployDir will NOT be updated"
fi
