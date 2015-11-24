#!/bin/sh
# Jing Tao
# This program will invoke DBQuery class to test query performance with path (index) or nested query(non-index)

echo "---------------------Result------------------"
echo "                      PrepQuery       Execut        OpenDB        Read        Size    "
echo -n "INDEXED DEPTH 0     "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t -index pathquerydepth0
echo -n "NONINDEXED DEPTH 0  "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t pathquerydepth0

echo -n "INDEXED DEPTH 1     "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t -index pathquerydepth1
echo -n "NONINDEXED DEPTH 1  "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t pathquerydepth1


echo -n "INDEXED DEPTH 2     "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t -index pathquerydepth2
echo -n "NONINDEXED DEPTH 2  "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t pathquerydepth2


echo -n "INDEXED DEPTH 3     "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t -index pathquerydepth3
echo -n "NONINDEXED DEPTH 3  "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t pathquerydepth3


echo -n "INDEXED DEPTH 4     "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t -index pathquerydepth4
echo -n "NONINDEXED DEPTH 4  "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t pathquerydepth4


echo -n "INDEXED DEPTH 5     "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t -index pathquerydepth5
echo -n "NONINDEXED DEPTH 5  "
java -cp /usr/local/devtools/jakarta-tomcat/lib/common/servlet.jar:../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DBQuery -t pathquerydepth5



echo "-------------"
echo "TEST COMPLETE"
echo "-------------"
