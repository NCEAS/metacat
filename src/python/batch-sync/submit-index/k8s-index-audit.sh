#!/bin/bash

#
# Usage:
#   # PVC_NAME is typically the metacat pod's "/var/metacat" PVC. Used to store logs, state, etc.
#   export PVC_NAME="your-pvc"
#   # RMQ_SECRET_NAME is usually in the existing metacat Secret
#   export RMQ_SECRET_NAME="your-rmq-secret"
#   # See find_objects_to_reindex.py for available CMD_ARGS
#   export CMD_ARGS="--rabbitmq-host localhost --rabbitmq-username metacat-rmq-guest --interval 15 --other-flags ..."
#   ./k8s-index-audit.sh
#
# Or pass CMD_ARGS as first argument:
#   export PVC_NAME="your-pvc"
#   ./k8s-index-audit.sh "--rabbitmq-host localhost --interval 15"
#
# Optional environment variables:
#   CONFIGMAP_NAME="your-configmap-name" (default: <namespace>-idxaudit-conf)
#   CRONJOB_NAME="your-cronjob-name" (default: <namespace>-idxaudit-cronjob)
#
# require PVC_NAME be set (no default)
PVC_NAME="${PVC_NAME:-}"

if [[ -z "${PVC_NAME}" ]]; then
  echo "ERROR: PVC_NAME is required. Set it in the environment, e.g.:"
  echo "  PVC_NAME='your-pvc' ${0} \"<CMD_ARGS>\""
    echo "$  export PVC_NAME='your-existing-metacat-pvc'"
  exit 1
fi
if [[ -z "${RMQ_SECRET_NAME}" ]]; then
    echo "ERROR: RMQ_SECRET_NAME not set (name of the Secret containing the RabbitMQ password."
    echo "Set it in the environment, e.g.:"
    echo "$  export RMQ_SECRET_NAME='your-existing-metacat-secret'"
    exit 1
fi

NAMESPACE=$(kubectl config view --minify --output 'jsonpath={..namespace}' | sed 's/^$/default/')
CONFIGMAP_NAME="${CONFIGMAP_NAME:-${NAMESPACE}-idxaudit-conf}"
CRONJOB_NAME="${CRONJOB_NAME:-${NAMESPACE}-idxaudit-cronjob}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SUBMIT_SCRIPT="submit_index_task_to_rabbitmq.py"
FINDER_SCRIPT="find_objects_to_reindex.py"
MOUNT_PATH="/scripts"   # configMap mount
PV_MOUNT="/var/metacat" # PVC mount used by scripts
RMQ_SECRET_NAME="${RMQ_SECRET_NAME:-}"
RMQ_SECRET_MOUNT="/etc/rmq-secret"
RMQ_SECRET_KEY="rabbitmq-password"

# CMD_ARGS from first arg or environment
CMD_ARGS="${1:-${CMD_ARGS:-}}"
# pretty-print CMD_ARGS
cmd_args_pretty() {
#  echo "$CMD_ARGS" | sed -E 's/--/\n  --/g' | sed '/^\s*$/d' | sed 's/^\s+//'
#echo "$CMD_ARGS" \
#  | sed -E 's/--/\n  --/g' \
#  | sed '/^[[:space:]]*$/d' \
#  | sed -E 's/^[[:space:]]+//' \
#  | sed -E $'s/^(  --[^[:space:]]+)[[:space:]]+/\1\t/'

#echo "$CMD_ARGS" \
#  | sed -E 's/--/\n  --/g' \
#  | sed '/^\s*$/d' \
#  | sed -E $'s/^(  --[^[:space:]]+)[[:space:]]+/\1\t\t/'

echo "$CMD_ARGS" \
  | sed -E 's/--/\n  --/g' \
  | sed '/^\s*$/d' \
  | sed -E $'s/^( *--[^ ]+)[ ]+([^ ].*)$/\\1\t\\2/'

#  | sed -E $'s/^(\s*--[^\t ]+)[ ]+([^\t ].*)$/\\1\t\t\\2/'

}
#ARGS_PER_LINE=$(
#set -f                  # disable pathname expansion
#set -- $CMD_ARGS        # split into positional params
#while (( $# )); do
#  if (( $# < 2 )) || [[ -n $2 && $2 == --* ]]; then
#    printf '  %s\n' "$1"
#    shift
#  else
#    printf '  %s\t\t%s\n' "$1" "$2"
#    shift 2
#  fi
#done
#set +f                  # re-enable pathname expansion
#)

# extract interval in minutes from CMD_ARGS (expects --interval N)
extract_interval() {
  echo "$CMD_ARGS" | grep -oE -- '--interval[= ]*[0-9]+' | head -n1 | grep -oE '[0-9]+' || true
}

# convert minutes to a simple cron expression:
# 1..59 -> "*/N * * * *"
# 60 -> "0 * * * *"
# >60 and divisible by 60 -> "0 */H * * *"
# otherwise fallback "0 * * * *"
interval_to_cron() {
  local mins="$1"
  if [[ -z "$mins" || "$mins" -le 0 ]]; then
    echo "0 * * * *"
    return
  fi
  if [[ "$mins" -eq 60 ]]; then
    echo "0 * * * *"
    return
  fi
  if [[ "$mins" -lt 60 ]]; then
    echo "*/${mins} * * * *"
    return
  fi
  if (( mins % 60 == 0 )); then
    local hours=$(( mins / 60 ))
    echo "0 */${hours} * * *"
    return
  fi
  # fallback
  echo "0 * * * *"
}

INTERVAL_MINUTES="$(extract_interval)"
SCHEDULE="$(interval_to_cron "$INTERVAL_MINUTES")"
echo
echo "CMD_ARGS:"
echo "$(cmd_args_pretty)"
#echo "                          $ARGS_PER_LINE"
echo "Computed cron schedule:   $SCHEDULE"
echo
echo "PVC_NAME:                 $PVC_NAME"
echo "RMQ_SECRET_NAME:          $RMQ_SECRET_NAME"
echo
echo "ConfigMap to create:      $CONFIGMAP_NAME"
echo "CronJob to create:        $CRONJOB_NAME"
echo
read -p "Proceed to create/update resources? [y/N] " yn
case "$yn" in
  [Yy]*) ;;
  *) echo "Aborted."; exit 1;;
esac

# ensure scripts exist
for f in "${SCRIPT_DIR}/${SUBMIT_SCRIPT}" "${SCRIPT_DIR}/${FINDER_SCRIPT}"; do
  if [[ ! -f "$f" ]]; then
    echo "ERROR: expected script not found: $f" >&2
    exit 1
  fi
done

# write to ./<config>-<cron>.yaml and also pipe to kubectl
OUTFILE="./index-audit-job.yaml"
cat <<EOF | tee "$OUTFILE" | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${CONFIGMAP_NAME}
data:
  find_objects_args: |
$(if [ -n "${CMD_ARGS}" ]; then printf '    %s\n' "${CMD_ARGS}"; else printf '    %s\n' ""; fi)
  ${FINDER_SCRIPT}: |
$(sed 's/^/    /' "${SCRIPT_DIR}/${FINDER_SCRIPT}")
  ${SUBMIT_SCRIPT}: |
$(sed 's/^/    /' "${SCRIPT_DIR}/${SUBMIT_SCRIPT}")
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: ${CRONJOB_NAME}
spec:
  schedule: "${SCHEDULE}"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 1
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: runner
              image: python:3.11-slim
              env:
                - name: RMQ_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: ${RMQ_SECRET_NAME}
                      key: ${RMQ_SECRET_KEY}
              command:
                - bash
                - -c
                - |
                  set -euo pipefail
                  mkdir -p "${PV_MOUNT}/.metacat/reindex-script"
                  pip install --no-cache-dir pika requests || true
                  # run finder script with args taken from configmap key
                  # run finder script with args taken from configmap key
                  ARGS="\$(cat '${MOUNT_PATH}/find_objects_args')"
                  echo "ARGS: \$ARGS"
                  python3 -u "${MOUNT_PATH}/${FINDER_SCRIPT}" \$ARGS
              volumeMounts:
                - name: metacat-pvc
                  mountPath: ${PV_MOUNT}
                - name: scripts-cm
                  mountPath: ${MOUNT_PATH}
          volumes:
            - name: metacat-pvc
              persistentVolumeClaim:
                claimName: ${PVC_NAME}
            - name: scripts-cm
              configMap:
                name: ${CONFIGMAP_NAME}
EOF
echo
echo "Applied ${CONFIGMAP_NAME} and ${CRONJOB_NAME} (schedule: ${SCHEDULE})."
echo "To remove:"
echo "        kubectl delete cronjob ${CRONJOB_NAME} && kubectl delete configmap ${CONFIGMAP_NAME}"
echo
