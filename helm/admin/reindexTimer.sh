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

handle_sigint() {
    echo -e "\nCaught Ctrl+C, exiting loop..."
    do_loop=false
    break
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
echo "Starting at: $(date)"; echo

if [[ "$action" == "$reindex_command" ]]; then
    echo "calling $base_url/d1/mn/v2/index?all=true"
    # Start reindex-all
    curl -X PUT -H "Authorization: Bearer $TOKEN" "$base_url/d1/mn/v2/index?all=true"

    # delay for reindex to begin populating queue, otherwise we exit too early
    sleep 10
fi

max_queue_size=0
max_worker_count=0
do_loop=true
trap 'handle_sigint' SIGINT

echo -e  "Minutes\t\tQueue size\tQueue max\t# Indexers\tIndexer max"

while [ $do_loop ]; do
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
    now=$(date +%s)
    echo -ne "\033[K$(((now - start)/60))\t\t$queue_size\t\t$max_queue_size\t\t$idx_worker_count\t\t$max_worker_count\n\n\r\033[1A\033[1A"

    if [ "$queue_size" -eq 0 ] && [ "$action" == "$reindex_command" ]; then
        do_loop=false
    else
        sleep $monitor_period_sec
    fi
done

finish=$(date +%s)
time_min=$(((finish - start)/60))
echo; echo; echo "Finished at: $(date)"
echo "Total objects indexed:  $max_queue_size"
echo "Max index workers used: $max_worker_count"
echo "Total time for reindex: $time_min minutes"
if [ $time_min -ne 0 ]; then
    echo "objects per minute:     $((max_queue_size / time_min))"
fi
if [ $max_queue_size -ne 0 ]; then
    echo "milliSec per object:    $((1000 * (finish - start) / max_queue_size))"
fi
