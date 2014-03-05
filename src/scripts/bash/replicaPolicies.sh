authoritativeMN="urn:node:mnDemo8"
MN_base_URL="https://mn-demo-8.test.dataone.org/knb/d1/mn/v1"
CN_base_URL="https://cn-dev.test.dataone.org/cn/v1"
cert_path="/var/metacat/certs/urn_node_mnDemo8.pem"
xml_path="replicapolicy.xml"


identifiers=$(curl -o - -E $cert_path "$MN_base_URL/object?count=7000&replicaStatus=false" | xmlstarlet sel -t -m "//objectInfo" -v "identifier" -n)

for id in ${identifiers};
do
        metadata=$(curl -o - -E $cert_path "$CN_base_URL/meta/${id}" | xmlstarlet sel -t -v "concat(//serialVersion,';',//authoritativeMemberNode)" -n)

        OIFS=$IFS
        IFS=";"
        arr=($metadata)
        if [ ${arr[1]} = $authoritativeMN ]; then
            echo "---- Updating replica policy for id: $id ----"
            curl -o - -X PUT -E $cert_path --capath "/etc/ssl/certs" -F "serialVersion=${arr[0]}" -F "policy=@$xml_path" "$CN_base_URL/replicaPolicies/${id}"
        else 
            echo "$authoritativeMN is not the authoritative member node for id: $id. (is ${arr[1]})"
        fi
done


