#!/bin/bash

## IMPORTANT! EDIT THIS!
PVC_NAME="metacatbrooke-metacat-metacatbrooke-0"  # <-- set your actual PVC name here

## Probably no need to edit anything below this line...
POD_NAME="python-pod"
SCRIPT_FILE="submit_index_task_to_rabbitmq.py"
CONFIGMAP_NAME="submit-index-task-python-script"

echo "Creating a pod to run the Python script for submitting index tasks to RabbitMQ..."
echo "IMPORTANT"
echo "1. CHECK THIS IS CORRECT: Using PVC: $PVC_NAME"
echo "2. Make sure the values are set correctly in $SCRIPT_FILE before starting!"
echo "<Enter> to continue or <Ctrl-C> to cancel."
read

# Generate and apply combined YAML
cat <<EOF | kubectl apply -f -
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${CONFIGMAP_NAME}
data:
  ${SCRIPT_FILE}: |
$(sed 's/^/    /' "${SCRIPT_FILE}")

---
apiVersion: v1
kind: Pod
metadata:
  name: ${POD_NAME}
spec:
  restartPolicy: Always
  containers:
    - name: python
      image: python:latest
      command:
        - bash
        - -c
        - |
          mkdir -p "/var/metacat/.metacat/reindex-script";
          pip install pika;
          tail -f /dev/null;
      volumeMounts:
        - name: metacat-volume
          mountPath: /var/metacat
        - name: script-volume
          mountPath: /var/metacat/.metacat/reindex-script/python
  volumes:
    - name: metacat-volume
      persistentVolumeClaim:
        claimName: ${PVC_NAME}
    - name: script-volume
      configMap:
        name: ${CONFIGMAP_NAME}
EOF

LOC="/var/metacat/.metacat/reindex-script"
echo "Pod and ConfigMap created successfully."
echo
echo "To delete the pod and configmap, run:"
echo "  $ kubectl delete pod ${POD_NAME}; kubectl delete configmap ${CONFIGMAP_NAME}"
echo
echo "To run the script:"
echo "(NOTE: The following paths assuming defaults have not been changed in ${SCRIPT_FILE}):"
echo; echo "1. ensure input PIDs are available at:"
echo "  ${LOC}/pids_to_process.txt"
echo; echo "2. access a bash shell in the pod:"
echo "  $ kubectl exec -it pod/python-pod -- bash"
echo; echo "3. run the script inside the pod:"
echo "$ { date; time python3 -u ${LOC}/python/${SCRIPT_FILE} rabbitmq_username rabbitmq_password; } > ${LOC}/output.log 2>&1 &"
echo; echo "Successfully enqueued PIDs are saved to ${LOC}/sysmeta-processed.txt"
