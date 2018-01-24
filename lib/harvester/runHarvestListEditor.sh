METACAT_CLASSES=$METACAT_HOME/build/classes
METACAT_LIB=$METACAT_HOME/lib
LIB_JARS=$METACAT_LIB/xercesImpl.jar
export CLASSPATH=$METACAT_CLASSES:$LIB_JARS
cd $METACAT_LIB/harvester
java edu.ucsb.nceas.metacat.harvesterClient.HarvestListEditor
