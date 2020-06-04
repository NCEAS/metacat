#!/bin/bash

#############################################################################
# Create 300 objects on the member node and record the time
#############################################################################
host_url="https://test.arcticdata.io/metacat/d1/mn/v2"
session="1A56618D2EC6C67DB255593A7166A8B"
n=300
object_file=../pangaea.xml
sysmeta_file=../sysmeta-pangaea.xml
object_url="${host_url}/object"

runtime=0
for (( i=1; i<=$n; i++))
do
  pid=$(uuidgen)
  #modify the pid in the system metadata file
  #sysmeta=$(xmlstarlet ed -u "//identifier" -v $pid $sysmeta_file)
  #echo "$sysmeta"
  xml edit -L --update "//identifier" -v $pid $sysmeta_file
  start=`date +%s`
  curl -k --cookie "JSESSIONID=${session}" -F "sysmeta=@${sysmeta_file}"  -F "object=@${object_file}" -F "pid=${pid}" -X POST ${object_url}
  end=`date +%s`
  difference=$((end-start))
  runtime=$(expr ${runtime} + ${difference})
  echo "Create the object with pid $pid successfully"
done
echo "The total run time is $runtime seconds"