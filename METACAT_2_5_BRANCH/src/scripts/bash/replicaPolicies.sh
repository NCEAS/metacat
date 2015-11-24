# sets the replication policy for all objects in the MN where the calling member node is the  authoritative member node.
# This code can easily be changed to replace $identifiers with a custom list of ids
# the $xml_path should point to a XML file with the replication policy to apply to all based on xmlns:d1="http://ns.dataone.org/service/types/v1" 
# 
# Dependencies:
#	xmlstarlet - developed with version 1.0.1
#	curl - developed with version 7.19.7

authoritativeMN="urn:node:mnDemo8"
MN_base_URL="https://mn-demo-8.test.dataone.org/knb/d1/mn/v1"
CN_base_URL="https://cn-dev.test.dataone.org/cn/v1"
cert_path="/var/metacat/certs/urn_node_mnDemo8.pem"
xml_path="replicapolicy.xml"

identifiers=$(curl -s -o - -E $cert_path "$MN_base_URL/object?count=7&replicaStatus=false" | xmlstarlet sel -t -m "//objectInfo" -v "identifier" -n)

failedIDs=""
successCount=0
failCount=0
skipCount=0

for id in ${identifiers};
do
        metadata=$(curl -s -o - -E $cert_path "$CN_base_URL/meta/${id}" | xmlstarlet sel -t -v "concat(//serialVersion,';',//authoritativeMemberNode)" -n)

        OIFS=$IFS
        IFS=";"
        arr=($metadata)
        if [ ${arr[1]} = $authoritativeMN ]; then
#echo "skip"
            echo "Updating replica policy for id: $id"
           statusCode=$(curl -s -o - -w "%{http_code}" -X PUT -E $cert_path --capath "/etc/ssl/certs" -F "serialVersion=${arr[0]}" -F "policy=@$xml_path" "$CN_base_URL/replicaPolicies/${id}")
            if [ $statusCode = "200" ]; then
                successCount=$(($successCount + 1))
            else
                failCount=$(($failCount + 1))
            fi

        else
            echo "$authoritativeMN is not the authoritative member node for id: $id. (is ${arr[1]})"
            failedIDs=$(printf "$failedIDs\n$id")
            skipCount=$(($skipCount + 1))
        fi
done

echo -e "\n\nDONE\n----------------------------------"
echo -e "\nSUCCESSES: $successCount"
echo -e "\nFAILURES: $failCount"
echo -e "\n\nSKIPS (objects with a different authoritative member node): $skipCount"
echo -e "\nIDs of objects with a different authoritative member node: \n$failedIDs"



