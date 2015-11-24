#!/bin/bash
# Matt Jones
# Metacat query using WGET to launch the query and an XSL document to 
# transform it into a comma-separated text file
# 26 August 2005
# '$Id$'
LIB=../../lib
PARSER=$LIB/xalan/xalan.jar:$LIB/xercesImpl.jar:$LIB/xalan/xml-apis.jar
TEMP=temp.xml
STYLE=wg-data-list.xsl
OUT=nceas-packages.txt
QUERY=http://knb.ecoinformatics.org/knb/metacat?action=query\&operator=INTERSECT\&organizationName=National%20Center%20for%20Ecological%20Analysis%20and%20Synthesis\&qformat=xml\&returndoctype=eml://ecoinformatics.org/eml-2.0.0\&returndoctype=eml://ecoinformatics.org/eml-2.0.1\&returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta6//EN\&returndoctype=-//ecoinformatics.org//eml-dataset-2.0.0beta4//EN\&returnfield=originator/organizationName\&returnfield=creator/organizationName
wget -O $TEMP $QUERY
echo "Query finished.  Transforming XML results to CSV format..."
java -cp $PARSER org.apache.xalan.xslt.Process -IN $TEMP -XSL $STYLE -OUT $OUT
rm $TEMP
echo "Done. Results are in file \"$OUT\"."
