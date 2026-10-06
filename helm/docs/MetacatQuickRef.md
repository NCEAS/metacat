# Checklist: Metacat K8s Installation Steps

> **= = = THIS IS A TEMPLATE - MAKE YOUR OWN COPY BEFORE CHECKING BOXES! = = =**

## PURPOSE: This ordered checklist is for either:

### Creation
- Creating a new, empty, latest-version Metacat installation on a Kubernetes (K8s) cluster, or

### Migration
- Migrating an existing, non-K8s Metacat v3.x instance to become a Metacat latest-version K8s installation.
- For migrating from Metacat v2.19.x, see the [Migrate-from-2.19.md guide](./Migrate-from-2.19.md), and the [Installation-Upgrade-Tips.md](Installation-Upgrade-Tips.md) document for tips on how to handle specific steps in the migration process.

> [!IMPORTANT]
> * Before starting a migration, you **must** have a fully-functioning installation of Metacat version 3.x.x, running outside of Kubernetes.

## Before You Begin

For more in-depth explanation and details of configuration steps, see the [Metacat Helm README](https://github.com/NCEAS/metacat/tree/main/helm#readme).

Some references below are specific to NCEAS infrastructure (e.g. ssh username/hostname; use of CephFS storage, etc); adjust as needed for your own installation.

> [!NOTE]
> - Sections marked `(FRESH INSTALL ONLY)` are needed ONLY for **new, empty installations.**
> - Sections marked `(MIGRATION ONLY)` are needed ONLY for a **migration from an existing Metacat 3.x.x instance.**
> - Unmarked sections are required for BOTH types of installation


## 1. `(MIGRATION ONLY)` Copy Data and Set Ownership & Permissions

- [ ] first `rsync` the data from the 3.x.x instance over to cephfs (OK to metacat running)
    ```shell
    # ON LEGACY HOST
    sudo rsync -aHAX -e "ssh -i $HOME/.ssh/id_ed25519" \
                /var/metacat/      $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/
    ```

- [ ] fix ownership and permissions of rsynced files:

    ```shell
    # ON CEPH MOUNT
    sudo chown -R 59997:59997 /mnt/ceph/repos/$REPO/metacat
    ```

## 2. Create Secrets

- [ ] Edit [`metacat/helm/admin/secret--metacat.yaml`](../admin/secret--metacat.yaml), deploy to k8s, then rename the file to `${RELEASE_NAME}-metacat-secrets.yaml`
- [ ] Edit [metacat/helm/admin/secret--cloudnative-pg.yaml](../admin/secret--cloudnative-pg.yaml), deploy to k8s, then rename the file to `${RELEASE_NAME}-cnpg-secret.yaml`
- [ ] Create a **GPG-ENCRYPTED** copy of each and **Delete** both your local unencrypted copies
- [ ] Push the GPG-ENCRYPTED copies to secure storage

## 3. install CNPG chart

- [ ] Deploy the `dataone-cnpg` chart with values overrides that include:

    ```yaml
    dbName: metacat
    # name of secret created above. Omit to have CNPG
    # create its own secret
    existingSecret: myrelease-metacat-cnpg 

    init:
      enabled: true

    # ...and ensure backups are enabled
    ```
  Don't `helm uninstall`, or the PVCs will be deleted and your data will become orphaned!

## 4. (MIGRATION ONLY) Import Data into CNPG

> [!WARNING]
> **BEFORE COPYING THE DATABASE!**
> - [ ] If the legacy host machine was running in a non-UTC timezone (e.g. Pacific time), we must convert the timestamps in several tables to UTC by running the SQL queries defined in [Installation-Upgrade-Tips.md](Installation-Upgrade-Tips.md#convert-timestamps-to-utc)!

The easiest way to move data to cnpg is using `pg_dump`/`pg_restore`, but this involves downtime. For more elegant approaches with less downtime, see ["Importing Data" in the DataONE CNPG README](https://github.com/DataONEorg/dataone-cnpg/blob/main/README.md#importing-data).

- [ ] Do a `pg_dump` on the legacy host:

  ```shell
  DUMP="/home/your-home-dir/pg-backup.dump"
  sudo pg_dump -U metacat -h localhost -F c -d metacat -f $DUMP
  # (you will be prompted first for your sudo password, then the database password)

  sudo chown your-uname:your-group $DUMP
  ```

- [ ] then copy this dump file to your local machine

  ```shell
  # from local machine:
  scp yourname@legacyhost:~/pg_backup.dump .
  ```

- [ ] Copy it to the cnpg primary pod, and restore it with `pg_restore`:
  ```shell
  kubectl cp ./pg_backup.dump <primary-pod-name>:/var/lib/postgresql/data/pg_backup.dump

  kubectl exec -i <cnpg-primary-pod-name> -- \
              bash -c 'pg_restore -d metacat --verbose < /var/lib/postgresql/data/pg_backup.dump'
  ```

## 5. Create Persistent Volumes

- [ ] Create PV for metacat data directory
- [ ] `Only if using a custom theme:` Create a PV for the MetacatUI theme directory

## 6. Values: Create a new values override file

**e.g. see the [values-dev-cluster-example.yaml](../examples/values-dev-cluster-example.yaml) file.**

- [ ] TLS ("SSL") setup (`ingress.tls.hosts` - leave blank to use default, or change if aliases needed...
  - [ ] `(MIGRATION ONLY)` transfer any existing aliases and rewrite rules from legacy host; see [hostname aliases tip](Installation-Upgrade-Tips.md#where-to-find-existing-hostname-aliases)

- [ ] [Install the ca chain](https://github.com/NCEAS/metacat/tree/main/helm#install-the-ca-chain), and also enable incoming client cert forwarding:

    ```yaml
    metacat:
      dataone.certificate.fromHttpHeader.enabled: true
    ```
- [ ] Set up Node cert and replication etc. as needed -  [see README](https://github.com/NCEAS/metacat/tree/main/helm#setting-up-certificates-for-dataone-replication).

- [ ] `(MIGRATION ONLY)` Do a `diff` between the v3.x.x properties file at `$TC_HOME/webapps/metacat/WEB-INF/metacat.properties` on the legacy host, and the newest `metacat.properties` from GitHub for the new version being installed, to see if any other custom `metacat:` settings need to be transferred (e.g. `auth.allowedSubmitters:`, `guid.doi.username:`, `guid.doi.uritemplate.metadata:`, `guid.doi.doishoulder.1:`, etc.)

### MetacatUI:

Carefully review all Metacat's `global.metacatUi*` values and update as needed, depending upon whether you are using the MetacatUI sub-chart or not:

- **For separately-deployed MetacatUI:**
  - [ ] in the Metacat values overrides, set `global.includeMetacatUi: false` and override the `global.metacatUiIngressBackend` settings subtree, and `global.metacatUiWebRoot`, if needed. (`global.metacatUiThemeName` is not needed in Metacat values, for this type of deployment.)
  - [ ] in the values overrides for the separate MetacatUI chart:
    - Set `global.metacatUiThemeName` and `global.metacatExternalBaseUrl` (REQUIRED)
    - Override `global.metacatAppContext` if needed (default is 'metacat')
    - Override `global.metacatUiWebRoot` if needed (default is '/')
    - Override `global.d1ClientCnUrl` to point at the **sandbox CN** ("https://cn-sandbox.test.dataone.org/cn"), until final release (default is production CN)

- **If using a theme from [metacatui-themes](https://github.com/NCEAS/metacatui-themes):**
  - [ ] it must be made available on a ceph/PV/PVC mount; e.g:

      ```yaml
      customTheme:
        enabled: true
        autoUpdate: true
        claimName: metacatsdr-metacatui-customtheme
        subPath: "metacatui-themes/src/sdr/js/themes/sdr"
      ```

  - [ ] Ensure metacatui has read access

      ```shell
      chmod -R o+r .
      find . -type d -print0 | xargs -0 chmod o+x
      # set default ACLs so new files and directories from `git pull` will also be readable
      sudo setfacl -R -d -m o:rx .
      ```

- [ ] If the custom theme needs to be partially overridden by a separate config.js file (e.g. `sfwmd.js` is used to override [the CERP theme](https://github.com/NCEAS/metacatui-themes/tree/main/src/cerp/js/themes/cerp) above):

  - [ ] set `metacatui.appConfig.enabled:` to `false`
  - [ ] Create a configMap to replace `config.js`, as follows:

    ```shell
    kubectl create configmap metacatsfwmd-metacatui-config-js --from-file=config.js=sfwmd.js
    ```


## 7F. `(FRESH INSTALL ONLY)` First Install

- [ ] `helm install` the MetacatUI chart, and debug any startup and configuration issues.
- [ ] `helm install` the Metacat chart, and debug any startup and configuration issues.
- [ ] Create a DNS entry to point to your k8s ingress.

- You're done! 🎉


## 7M. `(MIGRATION ONLY)` First Install

> [!CAUTION]
> IF MOVING DATA FROM AN EXISTING DEPLOYMENT THAT IS ALSO A DATAONE MEMBER NODE, DO NOT REGISTER THIS NODE WITH THE PRODUCTION CN UNTIL YOU'RE READY TO GO LIVE, or bad things will happen...

Add a `# TODO` comment to each of the following settings, and make sure to update those TODOs with the new values when you finally go live:
- [ ] ⚠ Use the **SANDBOX CN** for first deploy; set `global.d1ClientCnUrl: https://cn-sandbox.test.dataone.org/cn`  (and if you're not using the included MetacatUI sub-chart, make sure your external MetacatUI instance is also pointing to the sandbox CN)
- [ ] ⚠ The Node ID (in `metacat.dataone.nodeId` and `metacat.dataone.subject`) **MUST MATCH the legacy deployment!** (Don't use a temp ID, or it will be persisted into hashstore!)
- [ ] ⚠ The `metacat.dataone.autoRegisterMemberNode:` flag **MUST NOT match today's date!**
- [ ] Existing node already syncing to D1? Set `dataone.nodeSynchronize: false` until after final switch-over!
- [ ] Existing node already accepting D1 replicas? Set `dataone.nodeReplicate: false` after final switch-over!
- [ ] `helm install`, and debug any startup and configuration issues

> [!TIP]
> Set the log level `INFO` before you start indexing, if you need to determine **exactly** when indexing is complete (for benchmarking purposes). To do so, `kc edit configmaps ${RELEASE_NAME}-indexer-configfiles`.

- [ ] Temporarily scale up to 10 indexer workers, and re-index all datasets

  ```shell
  kubectl scale deployment ${RELEASE_NAME}-d1index --replicas 10

  kubectl get secret ${RELEASE_NAME}-d1-client-cert -o jsonpath="{.data.d1client\.crt}" | \
      base64 -d > DELETEME_NODE_CERT.pem
  curl -X PUT --cert ./DELETEME_NODE_CERT.pem "https://$HOSTNAME/CONTEXT/d1/mn/v2/index?all=true"
  rm DELETEME_NODE_CERT.pem  # don't forget to delete the cert file:
  ```

## 8. `(MIGRATION ONLY)` FINAL SWITCH-OVER FROM LEGACY TO K8S

> [!NOTE]
> If you need to accommodate hostname aliases, you'll need to update the `ingress.tls` section to reflect the new hostname(s) - see [this tip](Installation-Upgrade-Tips.md#where-to-find-existing-hostname-aliases).

**ENSURE NOBODY IS IN THE MIDDLE OF A BIG UPLOAD!** (Schedule off-hours)

- [ ] Edit `/var/lib/tomcat9/webapps/metacat/WEB-INF/metacat.properties` to set `application.readOnlyMode=true`

- [ ] Restart tomcat on the legacy host: `sudo systemctl restart tomcat9`

- [ ] Check it's in RO mode! `https://$HOSTNAME/CONTEXT/d1/mn/v2/node` - look for:
      `<property key="read_only_mode">true</property>`

- [ ] "top-up" `rsync` from legacy to ceph:

     ```shell
     # NOTES:
     # 1. Don't use -aHAX (like orig. rsync); use -rltDHX to not overwrite ownership or permissions
     # 2. Don't use --delete option for /var/metacat/ rsync
     # 3. Optionally use --dry-run to check first
     #
     sudo rsync -rltDHX -e "ssh -i $HOME/.ssh/id_ed25519" --stats --human-readable \
               /var/metacat/hashstore         $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/hashstore
     ```

- [ ] fix ownership and permissions of newly-copied hashstore files (no others should have changed):

     ```shell
     # IN K8S CLUSTER
     #
     sudo chown -R 59997:59997 /mnt/ceph/repos/$REPO/metacat/hashstore
     ```

- [ ] Do another `pg_dump` on legacy host, and `pg_restore` to CNPG - [see section 4, above](#4-migration-only-import-data-into-cnpg)

- [ ] Check values overrides and update any @TODOs to match live settings. See [BEFORE STARTING, above](#8-migration-only-final-switch-over-from-legacy-to-k8s).
- [ ] If applicable, re-enable `dataone.nodeSynchronize` and/or `dataone.nodeReplicate`
- [ ] Point the deployment at the **PRODUCTION CN** by deleting the `global.d1ClientCnUrl` entry
- [ ] Ensure the external MetacatUI instance is also pointing to the production CN (delete `global.d1ClientCnUrl` in its values overrides - may need to restart the pod manually)
- [ ] ONLY if you changed any `dataone.*` member node properties (`dataone.nodeId`, `dataone.subject`, `dataone.nodeSynchronize`, `dataone.nodeReplicate`), push them to the CN by setting`dataone.autoRegisterMemberNode` to today's date: UTC timezone, YYYY-MM-DD format
- [ ] Do a final `helm upgrade`

**When everything is up and running...**

- [ ] Switch DNS to point to k8s ingress instead of legacy. To get current IP address and hostname:

    ```shell
    kubectl get ingress -o yaml | egrep "(\- ip:)|(\- host:)"
    ```

- [ ] Index only the newer datasets:

    ```shell
    # on your local machine:
    cd <metacat>/src/scripts/bash/k8s
    ./index-delta.sh <start-time>

    # where <start-time> is the time an hour or more before the previous rsync,
    #     in the format: yyyy-mm-dd HH:MM:SS (with a space; e.g. 2024-11-01 14:01:00)
    ```

- [ ] Run `ANALYZE` to ensure PostgreSQL's stats are updated. This will ensure that `autovacuum` will run automatically (`ANALYZE` is run by `autovacuum`, but `autovacuum` won't run unless `ANALYZE` has been manually run after large updates):

    ```shell
    kubectl cnpg status ${RELEASE_NAME}-cnpg  # to determine which is the primary node (usually #1)
    kubectl exec ${RELEASE_NAME}-cnpg-1 -- bash -c "psql -U metacat << EOF
      ANALYZE;
    EOF"
    ```

- [ ] `git commit` a copy of the values overrides file used for this release, and update ChangeLog with the commit `sha`.

- [ ] Stop Tomcat, PostgreSQL and Apache on the legacy VM instance
  - [ ] create an [issue here](https://github.nceas.ucsb.edu/NCEAS/Computing/issues) to retire the VM ([template](https://github.nceas.ucsb.edu/NCEAS/Computing/blob/master/server_archiving.md#virtual-servers)
