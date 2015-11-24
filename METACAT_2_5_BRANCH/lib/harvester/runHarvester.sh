#!/bin/sh
#
# Shell script for launching Metacat Harvester from unix systems
#
# '$RCSfile: morpho,v $'
# '$Author$'
# '$Date$'
# '$Revision$'

# use java launcher under JAVA_HOME if set, otherwise try search path
if [ -n "$JAVA_HOME" ]; then
    if [ ! -d "$JAVA_HOME" ]; then
        echo "Error: JAVA_HOME is not a valid directory"
        exit 1
    fi
    JAVA_HOME="$( cd $JAVA_HOME ; pwd -P )"
    echo "Using JAVA_HOME=$JAVA_HOME"
    JAVA_BIN="$JAVA_HOME/bin/java"
    ERR="Your JAVA_HOME does not contain bin/java"
else
    JAVA_BIN="$(command -v java)"
    echo "No JAVA_HOME variable set"
    ERR="JAVA_HOME was not set, and no other java was found on your path"
fi
if [ -f "$JAVA_BIN" ]; then
    echo "Attempting to use java launcher $JAVA_BIN"
else
    echo "Error: $ERR"
    exit 1
fi
echo "----------------------------------"
"$JAVA_BIN" -version || exit 1
echo "----------------------------------"

LIB=$METACAT_HOME/WEB-INF/lib

# generate classpath, with some error checking
CPATH="."
for JAR in $LIB/*.jar; do
    [ -f "$JAR" ] || continue
	CPATH="$CPATH:$JAR"
done
if [ -z "$CPATH" ]; then
    echo "Error: No Morpho JAR files found in $LIB"
    exit 1
fi
echo "Using dynamic classpath: $CPATH"

# launch it
"$JAVA_BIN" -cp "$CPATH" edu.ucsb.nceas.metacat.harvesterClient.Harvester $1