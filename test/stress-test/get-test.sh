#!/bin/bash

#############################################################################
# Call the DataONE object rest command to get list of object of a member node. 
# Then go through the list to get the objects
#############################################################################
host_url="https://test.arcticdata.io/metacat/d1/mn/v2"
object_file_name="objects.txt"

object_url="${host_url}/object"

curl -k ${object_url} > ${object_file_name}

all_pids=(`xmlstarlet sel -t --match "//objectInfo/identifier"  --value-of "concat(., ' ')"  "${object_file_name}"`)
for i in ${all_pids[@]}
do
	curl -k -I ${object_url}/${i}
  curl -k ${object_url}/${i}
done