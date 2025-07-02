#!/bin/bash
# Get the delta of objects created or updated since a certain time, and index them
# See Issue #1990: https://github.com/NCEAS/metacat/issues/1990
#
# Use case: moving a Metacat instance to a Kubernetes cluster, we rsync the data initially to
# cephfs (at time 't'), then do all the setup and indexing while the legacy metacat is still
# online. Then, some time later (t + dt), we make the legacy host read-only, and do a "top-up" rsync
# again to cephfs. We then need to index only the objects that have been created or updated between
# t and t + dt.
#

cleanup() {
    rm -f "${cert_file}"
    rm -f "${result_file}"
}

# Initialize vars

if [ $# -ne 2 ]; then
    echo "Usage: $0 <start-time>"
    echo "       where <start-time> is the time an hour or more before the previous rsync,"
    echo "              in the format: yyyy-mm-dd HH:MM:SS (with a space; e.g. 2024-11-01 14:01:00)"
    exit 1
fi

start_time="$1 $2"
release_name=$(helm ls | grep "metacat" | awk  '{print $1}')
rls_count=$(echo ${release_name} | wc -w)

if [ "${rls_count}" -ne "1" ]; then
    echo "Can't get release name; found ${rls_count} releases containing 'metacat': ${release_name}"
    echo "Please type the correct release name (or Ctrl+C to abort):"
    echo
    read release_name
fi

cert_file="_DELETEME_$(date +%Y-%m-%d)_node.crt"
rm -f "${cert_file}"
kubectl get secret ${release_name}-d1-client-cert -o jsonpath="{.data.d1client\.crt}" | base64 -d \
    > ${cert_file}
chmod 600 ${cert_file}

host=$(kubectl get ingress | grep "${release_name}" | awk '{print $3}' | cut -d ',' -f 1)
api_url="https://${host}/metacat/d1/mn/v2/index"

result_file="modified-since-$(echo -n ${start_time} | sed -e 's/ /-/g').txt"
rm -f "${result_file}"
errors_file="errors-from-attempted-index-since-$(echo -n ${start_time} | sed -e 's/ /-/g').txt"
rm -f "${errors_file}"


# Get the pids of the objects modified since the start time
echo
echo "Finding and indexing any objects created/modified since ${start_time}, in release ${release_name}."
echo
echo "Double-check that ${start_time} is the time an hour or more before the previous rsync, and is in"
echo "the yyyy-mm-dd HH:MM:SS format (e.g. 2024-11-01 14:01:00)"
echo
echo "Results will be saved to ${result_file}. Enter to continue or Ctrl+C to abort..."
read

kubectl exec ${release_name}-postgresql-0 -- bash -c "psql -U metacat << EOF
  SELECT guid FROM systemmetadata WHERE date_modified > '${start_time}';
EOF" | sed '1,2d;$d' | sed '$d'  >  ${result_file}
# sed removes first 2 rows: "guid" and "----", and the last 2 rows: "(n rows)" and <blank>

cat ${result_file}
echo
echo "PIDs of modified objects saved to ./${result_file}"
echo


# Now, index the objects
result_count=$(cat ${result_file} | wc -w | xargs)
echo "Found ${result_count} objects created/modified since ${start_time}, in release"
echo "${release_name}."
echo
echo "Calling ${api_url} to index each. Enter to continue or Ctrl+C to abort..."
read

idx=0
while read -r pid1 pid2 pid3 pid4 pid5; do
    idx=$((idx + 1))
    first_idx=$idx
    # Process non-blank PIDs
    params="?pid=${pid1}"
    for pid in "$pid2" "$pid3" "$pid4" "$pid5"; do
        [[ -n "$pid" ]] && {
            params="${params}&pid=${pid}"
            idx=$((idx + 1))
        }
    done
    echo "--------- ${first_idx} to ${idx} of ${result_count} ---------"
    echo "PID:      ${pid}"
    echo "Response:"
    curl_result=$(curl  -s  -X  PUT  --cert ${cert_file}  "${api_url}${params}")
    echo "${curl_result}"
    echo
    if [ "$(echo "${curl_result}" | grep -cz "<scheduled>\s*true\s*</scheduled>")" -lt 1 ]; then
        echo
        echo "* * * Error: indexing failed. * * *"
        echo "Adding PIDs to errors file and continuing..."
        for pid in "$pid1" "$pid2" "$pid3" "$pid4" "$pid5"; do
            [[ -n "$pid" ]] && {
                echo "${pid}" >> ${errors_file}
            }
        done
        echo
    fi
done < <(paste -d ' ' - - - - - < ${result_file})

cleanup
if [ -f ${errors_file} ] && (( $(cat ${errors_file} | wc -l) > 0 )); then
    echo "Errors occurred during indexing. Please check the errors file (./${errors_file})."
fi
