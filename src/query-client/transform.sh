#!/bin/bash
# Matt Jones
#Command-line XSLT 
# 
# Takes 3 command-line arguments: 
# 1) an existing XML document, 
# 2) an existing XSL document, and 
# 3) an HTML document (existing or not) 
# It then uses the XSL document to transform 
# the XML into HTML. 
# (Note that HTML is the common usage - however, 
# many other types of output documents are supported, 
# limited only buy the XSL stylesheet itself)
# 26 August 2005
# '$Id$'

LIB=../../lib
PARSER=$LIB/xalan/xalan.jar:$LIB/xercesImpl.jar:$LIB/xalan/xml-apis.jar
TPARAMS="-PARAM qformat knb -PARAM action read"
if [[ -e $1 && -e $2 && -n $3 ]]
then
java -cp $PARSER org.apache.xalan.xslt.Process $TPARAMS -IN $1 -XSL $2 -OUT $3
echo "Done. Results are in file \"$OUT\"."
else
echo "USAGE:"
echo "transform.bat INPUTXMLFILE.xml   XSLFILE.xsl   OUTPUTHTMLFILE.html"
fi
