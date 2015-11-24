#!/bin/sh

# this script file will send 5 xml documents to db.

for ((i=1000;i<1500;i+=5))
do
  echo $i
  let j=i
  java -cp ../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DocumentImpl -a INSERT -d jin.$j.1 -f 532.2
  let j=i+1
#  java -cp ../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DocumentImpl -a INSERT -d jin.$j.1 -f 530.18
  let j=i+2
  java -cp ../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DocumentImpl -a INSERT -d jin.$j.1 -f 533.4
  let j=i+3
  java -cp ../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DocumentImpl -a INSERT -d jin.$j.1 -f 529.1
  let j=i+4
  java -cp ../build/metacat.jar:../lib/xerces.jar:/oracle01/app/oracle/product/8.1.6/jdbc/lib/classes111.zip edu.ucsb.nceas.metacat.DocumentImpl -a INSERT -d jin.$j.1 -f 534.1

done
