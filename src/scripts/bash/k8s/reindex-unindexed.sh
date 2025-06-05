#!/bin/bash
# Get a list of all the objects in systemmetadata, then query solr to check if each has been
# indexed, and index the ones that have not
#
# Use case: reindex objects that were missed due to indexing glitches
#

cleanup() {
    rm -f "${cert_file}"
}

# Initialize vars

release_name=$(helm ls | grep "metacat" | grep -v "mcui" | awk  '{print $1}')
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

readonly host=$(kubectl get ingress | grep "${release_name}" | awk '{print $3}' | cut -d ',' -f 1)
readonly mc_api_solr_url="https://${host}/metacat/d1/mn/v2/query/solr/q=id:"
readonly mc_api_index_url="https://${host}/metacat/d1/mn/v2/index"

readonly timestamp=`TZ=America/Los_Angeles date +%Y%m%dT%H%M%S`
readonly metacat_pids_file=all-metacat-guids--$timestamp
readonly export_filename=all-solr-guids--$timestamp
rm -f "${metacat_pids_file}"
readonly errors_file="errors-from-attempted-index-on-${timestamp}.txt"
rm -f "${errors_file}"

echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo "IMPORTANT! MAKE SURE YOU ARE PORT-FORWARDING SOLR FROM THE CORRECT CONTEXT BEFORE RUNNING!"
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo "e.g.  kc port-forward service/metacatbrooke-solr-headless 8983:8983"
echo
echo "Making export directory ${timestamp}"
mkdir ./${timestamp}

# Get the pids of the objects
echo
echo "Making a list of PIDs for all objects in release ${release_name}."
echo "Results will be saved to ./${timestamp}/${metacat_pids_file}. Enter to continue or Ctrl+C to
abort..."
read

kubectl exec ${release_name}-postgresql-0 -- bash -c "psql -U metacat << EOF
  SELECT guid FROM systemmetadata;
EOF" | sed '1,2d;$d' | sed '$d'  >  ./${timestamp}/${metacat_pids_file}
# sed removes first 2 rows: "guid" and "----", and the last 2 rows: "(n rows)" and <blank>
metacat_pids_count=$(cat ./${timestamp}/${metacat_pids_file} | wc -w | xargs)
echo "Found $metacat_pids_count objects in metacat (release name: ${release_name})"
echo

# NEEDS PORT FORWARDING RUNNING!
solr_url="http://localhost:8983/solr/metacat-index"

###########################################
# Function: Query Solr for data objects
#    usage: query <queryString> (e.g q=...)
function query {
   # cert is optional
   cert=""
   if [ -n "$node_cert" ]; then
     cert="-E ${node_cert}";
   fi
   curl --silent $cert "${solr_url}/query?${1-q=*:*&wt=json&rows=0}"
}

# Get the total count
readonly result=$(query)
readonly solr_total=$(echo $result | jq .response.numFound)

if [ -n "$solr_total" ] && [ "$solr_total" -gt 0 ] 2>/dev/null; then
  echo "Found a total of $solr_total pids in solr"
  start=0
else
  echo "No data found in Solr index. Exiting."
  exit 0
fi

delta=$((metacat_pids_count - solr_total))
echo "this is $delta fewer than the total pids in metacat."
echo "Calling $solr_url to get a total of $solr_total pids"
echo "Enter to continue or Ctrl+C to abort..."
read

readonly page_size=10000
echo "creating $((solr_total / page_size)) files"

# Initialize cursorMark - cursorMark is a parameter used for deep pagination to retrieve large sets of results
#   efficiently and consistently. Unlike the traditional start parameter, which can result in inconsistent results
#   when the index changes during queries, cursorMark ensures stable and predictable pagination by maintaining a
#   "cursor" through the result set.
cursorMark="*"
while [ "$cursorMark" != "" ]; do

    # pad the start value with zeros to 8 digits (e.g. 00000001)
    start_padded=$(printf "%08d" $start)
    export_file=${export_filename}_${start_padded}

    # Query entire index and record all the ids
    response=$(query "q=id:*&wt=json&rows=${page_size}&cursorMark=${cursorMark}&sort=id+asc")
    if [ $? -ne 0 ]; then
      echo "Error querying Solr index. Exiting."
      exit 1
    fi

    if [ -z "$response" ]; then
      echo "Empty response from Solr index. Exiting."
      exit 1
    fi
    response_docs=$( echo $response | jq -r '.response.docs[].id')
    if [ "$response_docs" != "" ]; then
      echo "$response_docs" > ./${timestamp}/${export_file}
      cursorMark=$(echo "$response" | jq -r .nextCursorMark)
      if [ ! -f ./${timestamp}/${export_file} ];
      then
        echo "Export file, ${export_file}, missing ";
        exit 5;
      else
        echo "Exported index to ./${timestamp}/${export_file}";
      fi

      start=$((start + page_size))
    else
      cursorMark=""
    fi
done

# Concatenate all Solr GUID files into one set
cat ./${timestamp}/${export_filename}_* > ./${timestamp}/all-solr-guids-merged.txt

# Sort both files for comparison
sort ./${timestamp}/all-solr-guids-merged.txt > ./${timestamp}/all-solr-guids-sorted.txt
sort ./${timestamp}/${metacat_pids_file} > ./${timestamp}/all-metacat-guids-sorted.txt


result_file="./${timestamp}/ids-only-in-metacat.txt"

# Strip all whitespace and use comm to find lines only in metacat (file 2)
# -2: suppress lines unique to file 2 (solr)
# -3: suppress lines that appear in both
comm -23 \
  <(sed 's/^[ \t]*//;s/[ \t]*$//' ./${timestamp}/all-metacat-guids-sorted.txt | sort) \
  <(sed 's/^[ \t]*//;s/[ \t]*$//' ./${timestamp}/all-solr-guids-sorted.txt | sort) \
  > $result_file

mc_ids_file_tot=$(cat $result_file | wc -w | xargs)

if [ "${mc_ids_file_tot}" -ne "${delta}" ]; then
    echo "* * * ERROR * * *: originally found $delta ids in metacat that were not in solr, but now"
    echo "found ${mc_ids_file_tot} ids in $result_file"
else
    echo "SUCCESS: Originally found $delta ids in metacat that were not in solr, and"
    echo "$result_file also contains ${mc_ids_file_tot} ids"
fi

idx=0

echo "Calling ${mc_api_index_url} to index as necessary."
echo "Enter to continue or Ctrl+C to abort..."
read

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
    echo "--------- ${first_idx} to ${idx} of ${delta} ---------"
    echo "PIDs:      ${params}"
    echo "Response:"
    curl_result=$(curl  -s  -X  PUT  --cert ${cert_file}  "${mc_api_index_url}${params}")
    echo "${curl_result}"
    echo
    err_count=$(echo "${curl_result}" | grep -c "<scheduled>[[:space:]]*true[[:space:]]*</scheduled>")
    err_count=${err_count:-0}
    if [ "$err_count" -lt 1 ]; then
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
