# this file goes in $TOMCAT_HOME/bin/ and tells tomcat to allow remote  
# debugging connections to the port listed as "address="
export CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"