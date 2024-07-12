#!/bin/bash
#
# Time how long it takes to do a full reindex in k8s
#

####################################################################################################
# VARS
####################################################################################################

monitor_period_sec=5

base_url="${1}"

release_name=$(helm list | grep metacat | awk '{print $1}')
echo "release_name: $release_name"

secret_name=$(kubectl get secrets | egrep ".*\-metacat-secrets" | awk '{print $1}')
echo "secret_name: $secret_name"

rmq_pwd=$(kubectl get secret "$secret_name" \
            -o jsonpath="{.data.rabbitmq-password}" | base64 -d)

####################################################################################################
# FUNCTIONS
####################################################################################################

function usage() {
  echo
  echo "USAGE:"
  echo
  echo "* Monitoring only; NO re-index:"
  echo
  echo "    $    $0"
  echo
  echo "* Monitoring WITH a full re-index:  (IMPORTANT: used 'export' when setting TOKEN!)"
  echo
  echo "    $    export TOKEN=\$(cat /path/to/admin/token)"
  echo "    $    $0  <base url for metacat api>"
  echo
  echo "    # EXAMPLES:"
  echo
  echo "    # jwt token from filesystem:"
  echo "               $    export TOKEN=\$(cat urn_node_KNB_dev_from_DataONETestIntCA.jwt)"
  echo "               $    $0  https://knb-dev.test.dataone.org/metacat"
  echo
  echo "    # jwt token from k8s Secret"
  echo "    #          $    export TOKEN=\$( kubectl get secret MYRELEASE-indexer-token \\"
  echo "                                -o jsonpath=\"{.data.DataONEauthToken}\" | base64 -d )"
  echo "               $    $0  https://knb-dev.test.dataone.org/metacat"
  echo
}

function reindexWarningMsg() {
  echo
  echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
  echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
  echo
  echo "  BEWARE! the FULL RE-INDEX option will trigger a FULL RE-INDEX of all datasets on:"
  echo "          $base_url"
  echo "  ...which cannot before stopped, and which may take a very long time (hours or days),"
  echo "  and will likely affect the responsiveness of your metacat instance during that time"
  echo
  echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
  echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
  echo
}

function portWarning() {
    echo "ERROR: Ensure port forwarding is running before executing; e.g.:"
    echo "       $    kubectl port-forward service/$release_name-rabbitmq-headless 15672:15672"
    exit 3
}

handle_sigint() {
    echo -e "\nCaught Ctrl+C, exiting loop..."
    do_loop=false
    # shellcheck disable=SC2104
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

reindexWarningMsg

reindex_command="re-index-all"
echo "Available Options:"
echo "* FULL RE-INDEX: type '$reindex_command' and hit 'Enter'."
echo "* ONLY MONITOR queue size & # indexer workers, every $monitor_period_sec sec: hit 'Enter'."
echo "* EXIT: ctrl-c"
action=""
read -p "Option: " action


####################################################################################################
# MAIN
####################################################################################################

start=$(date +%s)
echo "Starting at: $(date)"; echo

if [[ "$action" == "$reindex_command" ]]; then

    if [ -z "$base_url" ]; then
        echo "ERROR: Must provide base url!"
        usage
        exit 1
    fi

    if [ -z "$TOKEN" ]; then
        echo "ERROR: Ensure env. variable \$TOKEN contains your indexer/admin jwt token"
        usage
        exit 2
    fi

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

echo -e  "Minutes\t\tQueue size\tQueue max\t# Indexers\tIndexer max\tObjects/minute"

while [[ "$do_loop" == "true" ]]; do
    queue_size=$(curl -u  metacat-rmq-guest:"$rmq_pwd" \
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
    objpermin=0
    time_min=$(((now - start)/60))
    if [ $time_min -ne 0 ]; then
        objpermin=$((max_queue_size / time_min))
    fi
    echo -ne "\033[K$time_min\t\t$queue_size\t\t$max_queue_size\t\t$idx_worker_count\t\t$max_worker_count\t\t$objpermin\n\n\r\033[1A\033[1A"

    if [ "$queue_size" -eq 0 ] && [ "$action" == "$reindex_command" ]; then
        do_loop=false
    else
        sleep $monitor_period_sec
    fi
done

finish=$(date +%s)
tot_time_min=$(((finish - start)/60))
echo; echo; echo "Finished at: $(date)"
echo "Total objects indexed:  $max_queue_size"
echo "Max index workers used: $max_worker_count"
echo "Total time for reindex: $time_min minutes"
if [ $tot_time_min -ne 0 ]; then
    echo "objects per minute:     $((max_queue_size / tot_time_min))"
fi
if [ $max_queue_size -ne 0 ]; then
    echo "milliSec per object:    $((1000 * (finish - start) / max_queue_size))"
fi
