#!/bin/bash

###############################################################################
# This script will read a file named ids which contains of list of ids and reindex
# those ids. 
# Note: you need to modify the metacat url and set an environment variable "token"
# with a token from the node administrator
################################################################################
inputFileName=ids
metacatURL="https://valley.duckdns.org/metacat/d1/mn/v2/index?pid="

while read line || [[ -n "$line" ]];
do 
  echo "The id is $line"
  indexURL=$metacatURL$line
  echo "The index url is $indexURL"
  curl -H "Authorization: Bearer $token" -X PUT "$indexURL" || true
  sleep 1
done < $inputFileName
