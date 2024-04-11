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
monitor_period_sec=30

####################################################################################################
# FUNCTIONS
####################################################################################################

function portWarning() {
    echo "ERROR: Ensure port forwarding is running before executing; e.g.:"
    echo "       $    kubectl port-forward service/<myReleaseName>-rabbitmq-headless 15672:15672"
    exit 3
}

####################################################################################################
# INPUT & VALIDATION
####################################################################################################

if [[ $(nc -zv localhost 15672 2>&1 | grep -c "refused") -gt 0 ]]; then
  portWarning
else
  echo "RMQ port is open"
fi

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
reindex_command="re-index-all"
echo "Available Options:"
echo "* FULL RE-INDEX: type '$reindex_command' and hit 'Enter'."
echo "* ONLY MONITOR queue size & # indexer workers, every $monitor_period_sec sec: hit 'Enter'."
echo "* EXIT: ctrl-c"
action=""
read -p "Option: " action

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

####################################################################################################
# MAIN
####################################################################################################

start=$(date +%s)
echo "Starting at: $(date)"

if [[ "$action" == "$reindex_command" ]]; then
    echo "calling $base_url/d1/mn/v2/index?all=true"
    # Start reindex-all
    curl -X PUT -H "Authorization: Bearer $TOKEN" "$base_url/d1/mn/v2/index?all=true"

    # delay for reindex to begin populating queue, otherwise we exit too early
    sleep 10
fi

max_queue_size=0
max_worker_count=0

while true; do
    queue_size=$(curl -u  metacat-rmq-guest:"$pwd" \
                   http://localhost:15672/api/queues/%2f/index 2>/dev/null | \
                   jq  -r '.messages')

    if [ "$queue_size" -gt "$max_queue_size" ]; then
        max_queue_size=$queue_size
    fi

    idx_worker_count=$(kubectl get pods | grep -c d1index)
    if [ "$idx_worker_count" -gt "$max_worker_count" ]; then
        max_worker_count=$idx_worker_count
    fi

    echo -ne "$(date)\t\t Queue size: $queue_size (max: $max_queue_size)\t\t \
              Indexer count: $idx_worker_count (max: $max_worker_count)\n\n\r\033[1A\033[1A"

    if [ "$queue_size" -eq 0 ] && [ "$action" == "$reindex_command" ]; then
        break
    else
        sleep $monitor_period_sec
    fi
done

finish=$(date +%s)
time_min=$(((finish - start)/60))

echo; echo; echo "Finished at: $(date)"
echo "Max Queue size: $max_queue_size"
echo "Max index worker count: $max_worker_count"
echo "Total time for reindex = $time_min minutes"
