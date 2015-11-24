@echo off

::
:: '$Id$' 
:: 
:: Matthew Brooke
:: 8 December 2005
:: 
:: Command-line XSLT 
:: 
:: Takes 3 command-line arguments: 
:: 1) an existing XML document, 
:: 2) an existing XSL document, and 
:: 3) an HTML document (existing or not) 
:: It then uses the XSL document to transform 
:: the XML into HTML. 
:: (Note that HTML is the common usage - however, 
:: many other types of output documents are supported, 
:: limited only buy the XSL stylesheet itself)
::
:: TROUBLESHOOTING:
::   
:: i) ASSUMES the existence of:
::              .\lib\xalan.jar, 
::              .\lib\xalan.jar\xercesImpl.jar, and 
::              .\lib\xalan.jar\xml-apis.jar
:: 
:: ii) Use absolute paths, not relative paths, for 
:: the XML, XSL and HTML files. Make sure file paths 
:: do NOT have spaces in them (eg - NOT like: c:\Program Files\)
::

IF NOT EXIST %1 GOTO USAGE
IF NOT EXIST %2 GOTO USAGE

SET LIB=.\lib

SET CPATH=%LIB%\xalan.jar;%LIB%\xercesImpl.jar;%LIB%\xml-apis.jar


:: transformer parameters that are usually set by the java 
:: code in morpho or in metacat. We have to pass them on the command line:

SET TPARAMS=-PARAM qformat knb -PARAM action read 

echo Transforming XML file %1 to HTML format, using XSL file %2...
echo (sending the following transform parameters to the parser)
echo %TPARAMS%
echo .

java -cp %CPATH% org.apache.xalan.xslt.Process %TPARAMS% -IN %1 -XSL %2 -OUT %3

echo "Done. Results are in file %3

GOTO END

:USAGE

echo .
echo . USAGE:
echo . transform.bat INPUTXMLFILE.xml   XSLFILE.xsl   OUTPUTHTMLFILE.html
echo . 
echo . Use absolute paths, not relative paths, 
echo . and ensure paths have no spaces in them
echo . 

:END
