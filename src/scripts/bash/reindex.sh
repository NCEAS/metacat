#!/bin/bash

###############################################################################
# This script will read a file named ids which contains of list of ids and reindex
# those ids. 
# Note: you need to modify the metacat url and provide an JSESSIONID cookie representing a adminitor
################################################################################
inputFileName=ids
metacatURL="https://knb.ecoinformatics.org/knb/metacat?action=reindex&pid="
session="RTGB9E1A83D764CA50B572CD871C5E1C4"
doubleQuote=%22

echo "Here"
while read line || [[ -n "$line" ]];
do 
  echo "The id is $line"
  indexURL=$metacatURL$line
  echo "The index url is $indexURL"
  curl -k --cookie "JSESSIONID=$session" "$indexURL" || true
  sleep 1
done < $inputFileName
