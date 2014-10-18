#!/bin/bash
#This script will install openjdk-7 and tomcat7.
#It also updates the alternatives for java, javac, keytool and javaws to openjdk-7.
#It also modifies the /etc/tomcat7/catalina.properties to allow DataONE idenifiers.
#It modifies the workers.properties file for apache-tomcat connector.
#The user running the script should have the sudo permission.

echo "install openjdk-7-jdk"
sudo apt-get install openjdk-7-jdk
sleep 3
echo "configure java, java, keytool and javaws"
sudo update-alternatives --set java /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-7-openjdk-amd64/bin/javac
sudo update-alternatives --set keytool /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/keytool
sudo update-alternatives --set javaws /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/javaws

echo "install tomcat7"
sudo apt-get install tomcat7
sudo sed -i.bak '$ a\org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true' /etc/tomcat7/catalina.properties 
sudo sed -i '$ a\org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH=true' /etc/tomcat7/catalina.properties

echo "read the location of the workers.properties file from the jk_conf"
while read f1 f2 
do
        if [ "$f1" = "JkWorkersFile" ]; then
		JKWORKERPATH="$f2"	
	fi
done < /etc/apache2/mods-enabled/jk.conf
echo "the jk workers.properties location is $JKWORKERPATH"

echo "update the tomcat home and java home in workers.properties file"
sudo sed -i.bak --regexp-extended "s/(workers\.tomcat_home=).*/\1\/usr\/share\/tomcat7/;
                s/(workers\.java_home=).*/\1\/usr\/lib\/jvm\/java-7-openjdk-amd64/;"\
                $JKWORKERPATH
