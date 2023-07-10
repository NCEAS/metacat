#!/bin/bash

########################################################################
###This script file will compare the checksums from two places - one ###
###from the object list (systemmetadata); the other is the calculation##
###from the file (the checksum api call). If they don't match the pid###
###will be written to a file                                         ###
########################################################################

count=3
url="https://sfwmd.dataone.org/metacat/d1/mn/v2/object?count=$count"
checksumUrl="https://sfwmd.dataone.org/metacat/d1/mn/v2/checksum/"
errorFile="sfwmd_wrong_checksum.txt"
objectFile="sfwmd_object.xml"
algorithm="MD5"
token=""


rm $errorFile
touch $errorFile
curl -H "Authorization: Bearer $token" $url > $objectFile
xmlstarlet sel -t -m "//objectInfo" -v "concat(identifier/text(), ' ', checksum/text())" -n $objectFile |
while IFS= read -r pid_checksum; do
    read -r pid checksum <<< "$pid_checksum"
    echo "------------------------"
    echo "$pid"
    echo "$checksum"
    result=$(curl -H "Authorization: Bearer $token" "$checksumUrl$pid?checksumAlgorithm=MD5") 
    checksumFromServer=$(xmlstarlet sel -N ns2="http://ns.dataone.org/service/types/v1" -t -m "//ns2:checksum[@algorithm='MD5']" -v . <<<"$result")
    echo "$checksumFromServer"
    if [[ "$checksum" == "$checksumFromServer" ]]; then
       echo "Checksums are equal."
    else
      echo  "$pid $checksum $checksumFromServer" >> "$errorFile" 
      echo "Checksums are not equal!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1"  
    fi
done

rm $objectFile
