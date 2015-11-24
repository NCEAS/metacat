echo off
set METACAT_CLASSES=%METACAT_HOME%\build\classes
set METACAT_LIB=%METACAT_HOME%\lib
set LOG4J_PATH=%METACAT_HOME%\build\war\WEB-INF\log4j.properties
set JDBC=%METACAT_HOME%\build\war\lib\jdbc.jar;%METACAT_LIB%\postgresql-8.0-312.jdbc3.jar
set LIB_JARS=%METACAT_LIB%\cos.jar;%METACAT_LIB%\xercesImpl.jar;%METACAT_LIB%\utilities.jar;%METACAT_LIB%\log4j-1.2.12.jar;%METACAT_LIB%\xalan.jar
set CLASSPATH=%METACAT_CLASSES%;%JDBC%;%LIB_JARS%
cd %METACAT_CLASSES%
java -Dlog4j.configuration=%LOG4J_PATH% edu.ucsb.nceas.metacat.harvesterClient.Harvester
