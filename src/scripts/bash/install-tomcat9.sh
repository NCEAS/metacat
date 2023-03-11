#!/bin/bash
#This script will install tomcat9.
#It will modify the /etc/tomcat9/catalina.properties to allow DataONE idenifiers.
#It will modify the workers.properties file for apache-tomcat connector.
#It will move Metacat and other web applications from the old context directory to the new context directory.
#The user running the script should have the sudo permission.

APACHE_ENABLED_SITES_DIR=/etc/apache2/sites-enabled
APACHE_AVAILABLE_SITES_DIR=/etc/apache2/sites-available

JK_CONF=/etc/apache2/mods-enabled/jk.conf

OLD_TOMCAT=tomcat8
OLD_TOMCAT_BASE=/var/lib/${OLD_TOMCAT}

NEW_TOMCAT=tomcat9
NEW_TOMCAT_USER=tomcat
NEW_TOMCAT_COMMON=${NEW_TOMCAT}-common
NEW_TOMCAT_LIB=lib${NEW_TOMCAT}-java
NEW_CATALINA_PROPERTIES=/etc/${NEW_TOMCAT}/catalina.properties
NEW_TOMCAT_HOME=/usr/share/${NEW_TOMCAT}
NEW_TOMCAT_BASE=/var/lib/${NEW_TOMCAT}
NEW_TOMCAT_SERVER_CONIF=$NEW_TOMCAT_BASE/conf/server.xml
NEW_TOMCAT_CONTEXT_CONF=$NEW_TOMCAT_BASE/conf/context.xml
NEW_TOMCAT_DEFAULT_FILE=/etc/default/$NEW_TOMCAT

SSL=ssl
METACAT=metacat
WEBAPPS=webapps
METACAT_DATA_DIR=/var/metacat
TOMCAT_CONFIG_SLASH='org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true'
TOMCAT_CONFIG_BACKSLASH='org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true'
INIT_START_DIR=/etc/init.d
JAVA_OPT_MAX=-Xmx
JAVA_OPT_MIN=-Xms
JAVA_OPTS='${JAVA_OPTS}'
LOG4J='-Dlog4j2.formatMsgNoLookups=true'
NEW_JDK_HOME=/usr/lib/jvm/java-8-openjdk-amd64
DEFAULT_JAVA_HOME=/usr/lib/jvm/default-java

if [ $# -lt 1 ]; then
   echo "Usage: ./install-tomcat9 Metacat-context-name [tomcat_max_memory_size] [tomcat_min_memory_size]";
   exit 1;
fi
KNB=$1
MAX_MEM=$2
MIN_MEM=$3
echo "The context of Metacat is $KNB"
echo "The max memory size is $MAX_MEM"
echo "The min memory size is $MIN_MEM"

echo "instal xmlstarlet"
sudo apt-get install xmlstarlet

sudo /etc/init.d/apache2 stop
sudo /etc/init.d/solr stop

echo "install ${NEW_TOMCAT}"
sudo ${INIT_START_DIR}/${OLD_TOMCAT} stop
sudo apt-get install ${NEW_TOMCAT}
sudo systemctl stop tomcat9

sudo apt install libecj-java
sudo ln -s /usr/share/java/ecj.jar /var/lib/tomcat9/lib

echo "configure java, java, keytool and javaws"
sudo update-alternatives --set java ${NEW_JDK_HOME}/jre/bin/java
sudo update-alternatives --set javac ${NEW_JDK_HOME}/bin/javac
sudo update-alternatives --set keytool ${NEW_JDK_HOME}/jre/bin/keytool
echo "set the defaul-java to openjdk 8"
sudo rm -rf $DEFAULT_JAVA_HOME
sudo ln -s  $NEW_JDK_HOME $DEFAULT_JAVA_HOME

echo "configure ${NEW_TOMCAT}"
sudo cp ${NEW_CATALINA_PROPERTIES} "${NEW_CATALINA_PROPERTIES}.org"
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

echo "add an attribute useHttpOnly='false' to the element Context if it doesn't have one in the $NEW_TOMCAT_CONTEXT_CONF"
sudo cp $NEW_TOMCAT_CONTEXT_CONF "$NEW_TOMCAT_CONTEXT_CONF.org"
useHttpOnly=$(sudo xmlstarlet sel -t --value-of "/Context/@useHttpOnly" $NEW_TOMCAT_CONTEXT_CONF)
echo "the uerHttpOnly is $useHttpOnly"
if [[ -n $useHttpOnly ]]; then
	if [[ $useHttpOnly == 'false' ]]; then
		echo "Attribute useHttpOnly was set to false and we don't need to do anything"
        else
		echo "Update the attribute useHttpOnly's value to false"
		sudo xmlstarlet ed -L -P -u "/Context/@useHttpOnly" -v false $NEW_TOMCAT_CONTEXT_CONF
	fi
else 
	echo "Attribute useHttpOnly hasn't been set and we will add one"
	sudo xmlstarlet ed -L -P -s "/Context" --type attr -n useHttpOnly -v false $NEW_TOMCAT_CONTEXT_CONF
fi

echo "remove the 8080 ports and add the 8009 ports to the tomcat8 server.xml"
sudo cp $NEW_TOMCAT_SERVER_CONIF "$NEW_TOMCAT_SERVER_CONIF.org"
sudo xmlstarlet ed -L -P -d "//Connector[@port='8080']" $NEW_TOMCAT_SERVER_CONIF
#echo "the configuration file is $NEW_TOMCAT_SERVER_CONIF"
result=$(sudo xmlstarlet sel -t --value-of "/Server/Service[@name='Catalina']/Connector[@protocol='AJP/1.3']/@port" $NEW_TOMCAT_SERVER_CONIF)
#echo "the result is $result"
if [[ -n $result ]]; then
  echo "An ajp 1.3 connector exists and we don't need to do anything."
else
  echo "No aip 1.3 connector found and we should add one"
  sudo xmlstarlet ed -L -P -s "/Server/Service[@name='Catalina']" -t elem -name Connector -v "" $NEW_TOMCAT_SERVER_CONIF
  sudo xmlstarlet ed -L -P -s "/Server/Service/Connector[not(@port)]" --type attr -n port -v 8009 $NEW_TOMCAT_SERVER_CONIF
  sudo xmlstarlet ed -L -P -s "/Server/Service/Connector[not(@protocol)]" --type attr -n protocol -v AJP/1.3 $NEW_TOMCAT_SERVER_CONIF
  sudo xmlstarlet ed -L -P -s "/Server/Service/Connector[not(@address)]" --type attr -n address -v ::1 $NEW_TOMCAT_SERVER_CONIF
  sudo xmlstarlet ed -L -P -s "/Server/Service/Connector[not(@secretRequired)]" --type attr -n secretRequired -v false $NEW_TOMCAT_SERVER_CONIF
  sudo xmlstarlet ed -L -P -s "/Server/Service/Connector[not(@redirectPort)]" --type attr -n redirectPort -v 8443 $NEW_TOMCAT_SERVER_CONIF
fi


echo "move Metacat and other web applications from $OLD_TOMCAT to $NEW_TOMCAT"
sudo cp -R `ls -d -1 ${OLD_TOMCAT_BASE}/${WEBAPPS}/** -A | grep -v "ROOT"` ${NEW_TOMCAT_BASE}/${WEBAPPS}/.
sudo chown -R ${NEW_TOMCAT_USER}:${NEW_TOMCAT_USER} ${NEW_TOMCAT_BASE}/${WEBAPPS}/*
echo "change the value of the application.deployDir in the metacat.properties file"
SAFE_NEW_TOMCAT_WEBAPPS=$(printf '%s\n' "$NEW_TOMCAT_BASE/$WEBAPPS" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
#echo "the escaped webpass value is ${SAFE_NEW_TOMCAT_WEBAPPS}"
sudo cp $NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties $NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties.org
if [ -f "$NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties" ]; then
  echo "$NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties exists and the application.deployDir will be updated" 
  sudo sed -i.bak --regexp-extended "s/(application\.deployDir=).*/\1${SAFE_NEW_TOMCAT_WEBAPPS}/;" $NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties
        sudo sed -i --regexp-extended "s/(geoserver\.GEOSERVER_DATA_DIR=).*/\1${SAFE_NEW_TOMCAT_WEBAPPS}\/${KNB}\/spatial\/geoserver\/data/;" $NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties
else
  echo "$NEW_TOMCAT_BASE/$WEBAPPS/$KNB/WEB-INF/metacat.properties does NOT exists and the application.deployDir will NOT be updated"
fi


echo "change the ownership of $METACAT_DATA_DIR to $NEW_TOMCAT_USER"
sudo chown -R ${NEW_TOMCAT_USER}:${NEW_TOMCAT_USER} ${METACAT_DATA_DIR}
sudo chmod -R g+r ${METACAT_DATA_DIR}
sudo chmod -R g+w ${METACAT_DATA_DIR}

#Bring the system editor to open a file. When the file is saved, it will be /etc/systemd/system/tomcat9.service (the file doesn't exist before you save)
#in the security section, add the two lines:
#ReadWritePaths=/var/metacat
#ReadWritePaths=/etc/default/solr.in.sh
#Add a blank line and those two lines at end of file
#StandardOutput=append:/var/log/tomcat9/catalina.out
#StandardError=append:/var/log/tomcat9/catalina.out
 
sudo systemctl edit --full tomcat9.service

if [[ -z $MIN_MEM ]]; then
        echo "Either max or min memory size is not set for tomcat. So we skip the memory settings."
else
        echo "Both max ($MAX_MEM) and min ($MIN_MEM) memory sizes are set for tomcat."
        if grep -q "Xmx" ${NEW_TOMCAT_DEFAULT_FILE}; then
            echo "Java memory settings are set and don't need to do anything on ${NEW_TOMCAT_DEFAULT_FILE}."
        else
           echo "We need to add memory settings on ${NEW_TOMCAT_DEFAULT_FILE}."
           sudo sed -i.bak "$ a\\JAVA_OPTS=\"${JAVA_OPTS} -Xmx${MAX_MEM} -Xms${MIN_MEM} -XX:PermSize=128m -XX:MaxPermSize=512m\"" ${NEW_TOMCAT_DEFAULT_FILE}
        fi
fi

echo "Add the log4j safeguard"
if grep -q "log4j2.formatMsgNoLookups" ${NEW_TOMCAT_DEFAULT_FILE}; then
   echo "${LOG4J} exists in ${NEW_TOMCAT_DEFAULT_FILE} and don't need to do anything."  
else
   echo "${LOG4J} doesn't exist ${NEW_TOMCAT_DEFAULT_FILE} and we need to add it."
   sudo sed -i.bak "$ a\\JAVA_OPTS=\"${JAVA_OPTS} ${LOG4J}\"" ${NEW_TOMCAT_DEFAULT_FILE}
fi

echo "Change somethings on apache configuration"
echo "read the location of the workers.properties file from the jk_conf"
while read f1 f2 
do
        if [ "$f1" = "JkWorkersFile" ]; then
        JK_WORKER_PATH="$f2"
    fi
done < ${JK_CONF}
echo "the jk workers.properties location is $JK_WORKER_PATH"

echo "update the tomcat home in workers.properties file"
SAFE_NEW_TOMCAT_HOME=$(printf '%s\n' "$NEW_TOMCAT_HOME" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
SAFE_NEW_JDK_HOME=$(printf '%s\n' "$NEW_JDK_HOME" | sed 's/[[\.*^$(){}?+|/]/\\&/g')
sudo sed -i.bak --regexp-extended "s/(workers\.tomcat_home=).*/\1${SAFE_NEW_TOMCAT_HOME}/;"\
                $JK_WORKER_PATH

echo "update the apache site files by replacing $OLD_TOMCAT by $NEW_TOMCAT"
for j in $(sudo find $APACHE_AVAILABLE_SITES_DIR -type f -name "*.conf")
do
    sudo sed -i.bak "s/${OLD_TOMCAT}/${NEW_TOMCAT}/;" $j
done

echo "update ssl settings to support the DataONE CA"
sed -i.bak 's/DEFAULT:@SECLEVEL=2/DEFAULT:@SECLEVEL=0/' /etc/ssl/openssl.cnf

sudo /etc/init.d/apache2 start
sudo /etc/init.d/solr start
sudo systemctl start tomcat9

exit 0
