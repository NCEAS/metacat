echo off
set METACAT_CLASSES=%METACAT_HOME%\build\classes
set METACAT_LIB=%METACAT_HOME%\lib
set LIB_JARS=%METACAT_LIB%\xercesImpl.jar
set CLASSPATH=%METACAT_CLASSES%;%LIB_JARS%
cd %METACAT_LIB%\harvester
java edu.ucsb.nceas.metacat.harvesterClient.HarvestListEditor
