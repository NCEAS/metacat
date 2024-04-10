#!/bin/bash
#
# Time how long it takes to do a full reindex in k8s
#

####################################################################################################
# VARS
####################################################################################################

base_url=${1}
pwd=$(kubectl get secret --namespace brooke metacatbrooke-metacat-secrets \
    -o jsonpath="{.data.rabbitmq-password}" | base64 -d)

####################################################################################################
# FUNCTIONS
####################################################################################################

function portWarning() {
    echo "ERROR: Ensure port forwarding is running before executing; e.g.:"
    echo "       $    kubectl port-forward service/<myReleaseName>-rabbitmq-headless 15672:15672"
    exit 3
}

function getQueueSize() {
    curl -u  metacat-rmq-guest:"$pwd"  \
      http://localhost:15672/api/queues/%2f/index 2>/dev/null | \
      jq  -r '.messages'
}

####################################################################################################
# VALIDATION
####################################################################################################

echo
if [[ -z $base_url ]]; then
    echo "ERROR: Must provide base url (including context) for the metacat instance, in the form:"
    echo "    https://metacat-dev.test.dataone.org/metacat (NO TRAILING SLASH)"
    exit 1
fi

if [ -z "$TOKEN" ]; then
    echo "ERROR: Ensure env. variable \$TOKEN contains your indexer/admin jwt token. Use:"
    echo "  export TOKEN=\$(cat /path/to/token)"
    exit 2
fi

if [[ $(nc -zv localhost 15672 2>&1 | grep -c "refused") -gt 0 ]]; then
  portWarning
else
  echo "RMQ port is open"
fi

####################################################################################################
# MAIN
####################################################################################################
echo
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo
echo "    BEWARE! This will trigger a FULL RE-INDEX of all the datasets on:"
echo "        $base_url"
echo "    ...which may take a very long time (hours or days), and will likely affect"
echo "    the responsiveness of your metacat instance during that time period!"
echo
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo
echo "Hit 'Enter' to continue, or ctrl-c to exit"
read -p ""

echo "calling $base_url/d1/mn/v2/index?all=true"

# Start reindex-all
curl -X PUT -H "Authorization: Bearer $TOKEN" "$base_url/d1/mn/v2/index?all=true"

start=$(date +%s)
echo "Starting at: $(date)"

# delay for reindex to begin populating queue, otherwise we exit too early
sleep 10

while true; do
    queueSize=$(getQueueSize)
    echo "Queue size: $queueSize"
    if [ "$queueSize" -ne 0 ]; then
        sleep 10
    else
        break
    fi
done

finish=$(date +%s)
echo "Finished at: $(date)"

time=$((finish - start))

echo "Total time for reindex = $time seconds"
