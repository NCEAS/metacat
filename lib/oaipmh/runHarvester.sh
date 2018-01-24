METACAT_CLASSES=$METACAT_HOME/build/classes
METACAT_LIB=$METACAT_HOME/lib
JDBC=$METACAT_HOME/build/war/lib/jdbc.jar:$METACAT_LIB/postgresql-8.0-312.jdbc3.jar
LIB_JARS=$METACAT_LIB/xercesImpl.jar:$METACAT_LIB/utilities.jar:$METACAT_LIB/log4j-1.2.12.jar:$METACAT_LIB/xalan.jar
export CLASSPATH=$METACAT_CLASSES:$JDBC:$LIB_JARS
cd $METACAT_CLASSES
java edu.ucsb.nceas.metacat.oaipmh.harvester.OaipmhHarvester $*
