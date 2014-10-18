#!/bin/bash
#This script will install openjdk-7 and tomcat7.
#It also updates the alternatives for java, javac, keytool and javaws to openjdk-7.
#It also modifies the /etc/tomcat7/catalina.properties to allow DataONE idenifiers.
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