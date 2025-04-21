# Checklist: Metacat K8s Installation Steps

> **= = = THIS IS A TEMPLATE - MAKE YOUR OWN COPY BEFORE CHECKING BOXES! = = =**

> [!IMPORTANT]
> Before you begin...
> 1. **PURPOSE:** This ordered checklist is for either:
>    * Creating a new, empty Metacat v3.2.0+ installation on a Kubernetes (K8s) cluster, or
>    * Migrating and upgrading an existing, non-K8s Metacat v2.19.x instance to become a Metacat
>      v3.2.0+ K8s installation.
>    * ***Very Important: Before starting a migration, you **must** have a fully-functioning
>      installation of Metacat version 2.19, running with PostgreSQL version 14. Migrating from
>      other versions of Metacat and/or PostgreSQL is not supported.***
>
> 2. Sections marked `(FRESH INSTALL ONLY)` are needed ONLY for **new, empty installations.**
> 3. Sections marked `(MIGRATION ONLY)` are needed ONLY for a **migration from an existing Metacat
>    2.19.x instance.**
> 4. Unmarked sections are required for both types of installation
> 5. For more in-depth explanation and details of configuration steps, see the [Metacat Helm
>    README](https://github.com/NCEAS/metacat/tree/main/helm#readme).
> 6. Some references below are specific to NCEAS infrastructure (e.g. ssh username/hostname; use of
>    CephFS storage, etc); adjust as needed for your own installation.
> 7. Assumptions: you have a working knowledge of Kubernetes deployment, including working with yaml
>    files, helm and kubectl commands; your kubectl context is set for the target deployment
>    location; you are able to ssh from the legacy host to the host where the target filesystem is
>    mounted (in our case, cephfs).

## 1. `(MIGRATION ONLY)` Copy Data and Set Ownership & Permissions

### = = = = = = = = = = = = = ON LEGACY HOST: = = = = = = = = = = = = =
- [ ] first `rsync` the data from the 2.19 instance over to cephfs (OK to leave postgres & tomcat
      running)
    ```shell
    # can also prepend with time, and use --stats --human-readable, and/or --dry-run
    #
    # metacat:
    sudo rsync -aHAX -e "ssh -i $HOME/.ssh/id_ed25519" \
                /var/metacat/data/      $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/data/
    sudo rsync -aHAX -e "ssh -i $HOME/.ssh/id_ed25519" \
                /var/metacat/dataone/   $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/dataone/
    sudo rsync -aHAX -e "ssh -i $HOME/.ssh/id_ed25519" \
                /var/metacat/documents/ $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/documents/
    sudo rsync -aHAX -e "ssh -i $HOME/.ssh/id_ed25519" \
                /var/metacat/logs/      $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/logs/
    ```

- [ ] In the metacat database, verify that all the `systemmetadata.checksum_algorithm` entries are
  on the [list of supported algorithms](https://github.com/DataONEorg/hashstore-java/blob/main/src/main/java/org/dataone/hashstore/filehashstore/FileHashStore.java#L63)
  (NOTE: syntax matters! E.g. `SHA-1` is OK, but `SHA1` isn't):

  ```sql
    -- psql -U metacat -h 127.0.0.1
    SELECT DISTINCT checksum_algorithm FROM systemmetadata WHERE checksum_algorithm NOT IN
            ('MD2','MD5','SHA-1','SHA-256','SHA-384','SHA-512','SHA-512/224','SHA-512/256');

    -- then manually update each to the correct syntax; e.g:
    UPDATE systemmetadata SET checksum_algorithm='SHA-1' WHERE checksum_algorithm='SHA1';
    -- ...etc
    ```

> [!NOTE]
> The above step on the legacy host breaks search results in some cases (e.g. attempts to find
> specific data files -- by checksum -- across packages), unless you reindex-all. Therefore, if you
> do not anticipate that the transfer to k8s will complete quickly, it should instead be done in
> k8s, and repeated after each rsync.

- [ ] Do a `pg_dump` on the legacy host (since postgresql major version on legacy host is lower than that being deployed by helm chart):

  ```shell
  DUMP_DIR="/var/lib/postgresql/14-pg_dump"
  PGDB=metacat
  POSTGRES_USER=metacat
  sudo pg_dump -U $POSTGRES_USER -h localhost --format=directory --file=$DUMP_DIR --jobs=20 $PGDB
  ```

> [!IMPORTANT]
> 1. The dump directory must be named `[version]-pg_dump` - e.g. `14-pg_dump`
> 2. It must be located alongside the existing _**versioned**_ postgres data dir, so for PG data
>    dir at: `postgresql.postgresqlDataDir: /bitnami/postgresql/14/main`, for example, the existing
>    versioned directory would be `/var/lib/postgresql/14`, and the dump dir must be created
>    alongside it, at: `/var/lib/postgresql/14-pg_dump`

- [ ] then rsync this dump directory over to ceph

  ```shell
  # from legacy host:
  sudo rsync -aHAX -e "ssh -i $HOME/.ssh/id_ed25519" \
      /var/lib/postgresql/14-pg_dump/ $USER@$TARGET:/mnt/ceph/repos/$REPO/postgresql/14-pg_dump/
  ```

### = = = = = = = = = = = = = ON CEPHFS-MOUNT HOST: = = = = = = = = = = = = =

- [ ] After rsyncs are complete, change ownership **ON CEPHFS** as follows:
  ```shell
  sudo chown -R 59996:59996 /mnt/ceph/repos/$REPO/postgresql/14-pg_dump

  cd /mnt/ceph/repos/$REPO/metacat
  sudo chown -R 59997:59997 data dataone documents logs
  ```

- [ ] ...then ensure all metacat `data` and `documents` files have `g+rw` permissions, otherwise,
      hashstore converter can't create hard links:

    ```shell
    sudo chmod -R g+rw data documents dataone
    ```

## 2. Create Secrets

- [ ] Make a copy of the [`metacat/helm/admin/secrets.yaml`](./secrets.yaml) file and rename to
      `${RELEASE_NAME}-metacat-secrets.yaml`
- [ ] edit to replace `${RELEASE_NAME}` with the correct release name:
- [ ] edit to add the correct passwords for this release (some may be found in legacy
      `metacat.properties`; e.g. postgres, DOI, etc.)
- [ ] Deploy it to the cluster:

    ```shell
    kubectl apply -f ${RELEASE_NAME}-metacat-secrets.yaml
    ```

- [ ] Save a **GPG-ENCRYPTED** copy to secure storage.
- [ ] **Delete** your local unencrypted copy.

## 3. Create Persistent Volumes

> Assumes **cephfs volume credentials** already installed as a k8s Secret - see [this tip on
> creating your own secret](#creating-volume-credentials-secret-for-the-pvs), and [this
> DataONEorg/k8s-cluster example
> ](https://github.com/DataONEorg/k8s-cluster/blob/main/storage/Ceph/CephFS/helm/secret.yaml).

- [ ] Get the current volume sizes from the legacy installation, to help with sizing the PVs -
      [example](#get-sizing-information-for-pvs)
- [ ] Create PV for metacat data directory - [example](./pv--releasename-metacat-cephfs.yaml)
- [ ] Create PV for PostgreSQL data directory - [example](./pv--releasename-postgres-cephfs.yaml)
- [ ] Create PVC for PostgreSQL - [example](./pvc--releasename-postgres.yaml)
- [ ] `Only if using a custom theme:` Create a PV for the MetacatUI theme directory
      [example](./pv--releasename-metacatui-theme-cephfs.yaml)

## 4. Values: Create a new values override file

**e.g. see the
[values-dev-cluster-example.yaml](../examples/values-dev-cluster-example.yaml)
file.**

- [ ] TLS ("SSL") setup (`ingress.tls.hosts` - leave blank to use default, or change if aliases
      needed...
  - [ ] `(MIGRATION ONLY)` transfer any existing aliases and rewrite rules from legacy host; see
        [hostname aliases tip, below](#where-to-find-existing-hostname-aliases)`
- [ ] Set up Node cert and replication etc. as needed -  [see
      README](https://github.com/NCEAS/metacat/tree/main/helm#setting-up-certificates-for-dataone-replication).
- [ ] [Install the ca
      chain](https://github.com/NCEAS/metacat/tree/main/helm#install-the-ca-chain), and also
      enable incoming client cert forwarding:

    ```yaml
    metacat:
      dataone.certificate.fromHttpHeader.enabled: true
    ```

- [ ] `(MIGRATION ONLY)` Do a `diff` between the v2.19 properties file at
      `$TC_HOME/webapps/metacat/WEB-INF/metacat.properties` on the legacy host, and the newest
      `metacat.properties` from GitHub for the new version being installed, to see if any other
      custom `metacat:` settings need to be transferred (e.g. `auth.allowedSubmitters:`, `guid.doi.username:`,
      `guid.doi.uritemplate.metadata:`, `guid.doi.doishoulder.1:`, etc.)

* MetacatUI:
  - [ ] ALWAYS set `global.metacatUiThemeName`
  - [ ] If using a bundled theme (arctic, knb etc.):
    - for PROD, no further action required
    - for TEST, override at least `global.metacatExternalBaseUrl` and
      `global.d1ClientCnUrl`

  - If using a theme from [metacatui-themes](https://github.com/NCEAS/metacatui-themes):
    - [ ] it must be made available on a ceph/PV/PVC mount; e.g:

        ```yaml
        customTheme:
          enabled: true
          claimName: metacatsfwmd-metacatui-customtheme
          subPath: metacatui-themes/src/cerp/js/themes/cerp
        ```

    - [ ] Ensure metacatui has read access

        ```shell
        chmod -R o+rx metacatui
        ```

  - [ ] If the custom theme needs to be partially overridden by a separate config.js file (e.g.
    `sfwmd.js` is used to override [the CERP
    theme](https://github.com/NCEAS/metacatui-themes/tree/main/src/cerp/js/themes/cerp) above):

    - [ ] set `metacatui.appConfig.enabled:` to `false`
    - [ ] Create a configMap to replace `config.js`, as follows:

      ```shell
      kubectl create configmap metacatsfwmd-metacatui-config-js --from-file=config.js=sfwmd.js
      ```


## 5F. `(FRESH INSTALL ONLY)` First Install

- [ ] `helm install`, and debug any startup and configuration issues.
- [ ] Create a DNS entry to point to your k8s ingress. Get the current IP address and hostname with:

    ```shell
    kubectl get ingress -o yaml | egrep "(\- ip:)|(\- host:)"
    ```
- [ ] You're done! 🎉


## 5M. `(MIGRATION ONLY)` First Install

> [!CAUTION]
> IF MOVING DATA FROM AN EXISTING DEPLOYMENT THAT IS ALSO A DATAONE MEMBER NODE, DO NOT REGISTER
> THIS NODE WITH THE PRODUCTION CN UNTIL YOU'RE READY TO GO LIVE, or bad things will happen...

- [ ] Point the deployment at the **SANDBOX CN**

    ```yaml
    global:
      d1ClientCnUrl: https://cn-sandbox.test.dataone.org/cn
    ```

- [ ] The Node ID (in `metacat.dataone.nodeId` and `metacat.dataone.subject`) **MUST MATCH the
      legacy deployment!** (Don't use a temp ID, or it will be persisted into hashstore!)
- [ ] The `metacat.dataone.autoRegisterMemberNode:` flag **MUST NOT match today's date!**
- [ ] Existing node already syncing to D1? Set `dataone.nodeSynchronize: false` until after final
      switch-over!
- [ ] Existing node already accepting D1 replicas? Set `dataone.nodeReplicate: false` after final
      switch-over!
- [ ] Disable `livenessProbe` & `readinessProbe` temporarily (until DB and hashstore converted)

> [!NOTE]
> Metacat's DB upgrade only writes OLD datasets -- ones not starting `autogen` -- from DB to disk.
> These should all be finished after the first upgrade - so provided subsequent `/var/metacat/`
> rsyncs are only additive (don't `--delete` destination files not on source), then subsequent DB
> upgrades after incremental rsyncs will be very fast. [Tips,
> below](#see-how-many-old-datasets-exist-in-db-before-the-upgrade) show how to check the number
> of "old" datasets exist in DB, before the upgrade

- [ ] set `storage.hashstore.disableConversion: true`, so the hashstore converter won't run yet
- [ ] `helm install`, debug any startup and configuration issues, and allow database upgrade to
      finish.

  > See [Tips, below](#monitor-database-upgrade-completion), for how to detect when
  > database conversion finishes

> [!TIP]
> because hashstore conversion has not yet been done, it is expected for metacatUI to
> display `Oops! It looks like there was a problem retrieving your search results.`, and for
> `/metacat/d1/mn/v2/` api calls to display `Metacat has not been configured`

- [ ] Delete the `storage.hashstore.disableConversion:` setting, so the hashstore converter will
      run, and do a `helm upgrade`. (How to detect when hashstore conversion finishes? See [Tips,
      below](#monitor-hashstore-conversion-progress-and-completion))
- [ ] When database upgrade and hashstore conversion have both finished, re-enable probes

> [!NOTE]
> Set the log level `INFO` before you start indexing, if you need to determine **exactly** when
> indexing is complete (for benchmarking purposes).
> To do so, `kc edit configmaps ${RELEASE_NAME}-indexer-configfiles` and restart all indexer pods.

- [ ] Re-index all datasets (Did with 25 indexers for test.adc on dev; 50 on prod).

  ```shell
  kubectl get secret ${RELEASE_NAME}-d1-client-cert -o jsonpath="{.data.d1client\.crt}" | \
      base64 -d > DELETEME_NODE_CERT.pem

  curl -X PUT --cert ./DELETEME_NODE_CERT.pem "https://$HOSTNAME/CONTEXT/d1/mn/v2/index?all=true"
  # look for <scheduled>true</scheduled> in response

  # don't forget to delete the cert file:
  rm DELETEME_NODE_CERT.pem
  ```

  > [See Tips, below](#monitor-indexing-progress) for monitoring indexing progress.


## 6. `(MIGRATION ONLY)` FINAL SWITCH-OVER FROM LEGACY TO K8S

> BEFORE STARTING: To reduce downtime during switch-over, flag any required values override
> updates as @TODOs. E.g. If you've been using a temporary node name, hostname, and TLS setup,
> flag these as `TODO`, for updates during switchover, with the new values in handy comments:
> - [ ] `metacat.server.name`
> - [ ] `global.metacatExternalBaseUrl`
> - [ ] `global.d1ClientCnUrl`
> - [ ] Any others that will need changing, e.g. `dataone.nodeSynchronize`, `dataone.nodeReplicate`
>       etc.
>
> [!NOTE]
> If you need to accommodate hostname aliases, you'll need to update the `ingress.tls` section to
> reflect the new hostname(s) - see [Tips, below](#where-to-find-existing-hostname-aliases).

### = = = = = = = = = = = = = IN K8S CLUSTER: = = = = = = = = = = = = =
- [ ] Make a backup of the `checksums` table so hashstore won't try to reconvert completed files:

     ```shell
     kubectl exec ${RELEASE_NAME}-0 -- bash -c \
       "SCRIPT=\$TC_HOME/webapps/metacat/WEB-INF/scripts/sql/backup-restore-checksums-table/backup-checksums-table.sh \
        && chmod 750 \$SCRIPT \
        && bash -c \$SCRIPT"
     ```

- [ ] `helm uninstall` the running installation. (keep all secrets, PVCs etc!)
- [ ] temporarily chown metacat and pg_dump data on cephfs (since your $USER will be writing during
        `rsync`)

    ```shell
    sudo chown -R $USER:59996 /mnt/ceph/repos/$REPO/postgresql/14-pg_dump

    cd /mnt/ceph/repos/$REPO/metacat

    sudo chown -R $USER:59997 data dataone documents logs
    ```

### = = = = = = = = = = = = = ON LEGACY HOST = = = = = = = = = = = = =

**ENSURE NOBODY IS IN THE MIDDLE OF A BIG UPLOAD!** (Can schedule off-hours, but how to monitor?)

- [ ] Edit `/var/metacat/config/metacat-site.properties` to change to:

     ```properties
     application.readOnlyMode=true
     ```

- [ ] Stop tomcat (not postgres)

     ```shell
     sudo systemctl stop tomcat9
     ```

- [ ] `pg_dump` on legacy host

    ```shell
    DUMP_DIR="/var/lib/postgresql/14-pg_dump"
    PGDB=metacat
    POSTGRES_USER=metacat
    sudo pg_dump -U $POSTGRES_USER -h localhost --format=directory --file=$DUMP_DIR --jobs=20 $PGDB
    ```

- [ ] Start tomcat

     ```shell
     sudo systemctl start tomcat9
     ```

- [ ] Check it's in RO mode! `https://$HOSTNAME/CONTEXT/d1/mn/v2/node` - look for:

     ```xml
      <property key="read_only_mode">true</property>
     ```

- [ ] "top-up" `rsync` from legacy to ceph:

     ```shell
     # NOTES:
     # 1. Don't use -aHAX (like orig. rsync); use -rltDHX to not overwrite ownership or permissions
     # 2. Don't use --delete option for /var/metacat/ rsync
     # 3. Optionally use --dry-run to check first
     #
     sudo rsync -rltDHX -e "ssh -i $HOME/.ssh/id_ed25519" --stats --human-readable \
               /var/metacat/data/         $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/data/

     sudo rsync -rltDHX -e "ssh -i $HOME/.ssh/id_ed25519" --stats --human-readable \
               /var/metacat/dataone/      $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/dataone/

     sudo rsync -rltDHX -e "ssh -i $HOME/.ssh/id_ed25519" --stats --human-readable \
               /var/metacat/documents/    $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/documents/

     sudo rsync -rltDHX -e "ssh -i $HOME/.ssh/id_ed25519" --stats --human-readable \
               /var/metacat/logs/         $USER@$TARGET:/mnt/ceph/repos/$REPO/metacat/logs/

     sudo rsync -rltDHX -e "ssh -i $HOME/.ssh/id_ed25519" --stats --human-readable \
         /var/lib/postgresql/14-pg_dump/  $USER@$TARGET:/mnt/ceph/repos/$REPO/postgresql/14-pg_dump/
     ```

### = = = = = = = = = = = = = IN K8S CLUSTER: = = = = = = = = = = = = =

- [ ] fix ownership and permissions of newly-copied files:

     ```shell
     sudo chown -R 59996:59996 /mnt/ceph/repos/$REPO/postgresql

     cd /mnt/ceph/repos/$REPO/metacat

     sudo chown -R 59997:59997 data dataone documents logs

     sudo chmod -R g+rw data documents dataone
     ```

- [ ] Move or delete the current PG data directory being used by k8s, so that the pg_dump will
      automatically be ingested on next startup

    ```shell
    # ssh to host where cephfs is mounted, then:
    cd /mnt/ceph/repos/$REPO/postgresql
    sudo mv 17 17-deleteme  # or delete
    ```

- [ ] Disable probes again, until Database Upgrade is finished
- [ ] Set `storage.hashstore.disableConversion: true`, so the hashstore converter won't run.
- [ ] `helm-install`, and ensure the `pgupgrade` initContainer finished successfully.
- [ ] Restore the `checksums` table from the backup, so hashstore won't try to reconvert
      completed files:

    ```shell
    kubectl exec ${RELEASE_NAME}-0 -- bash -c \
      "SCRIPT=\$TC_HOME/webapps/metacat/WEB-INF/scripts/sql/backup-restore-checksums-table/restore-checksums-table.sh \
      && chmod 750 \$SCRIPT \
      && bash -c \$SCRIPT"
    ```

- [ ] Delete the `storage.hashstore.disableConversion:` setting, so the hashstore converter will
      run, and do a `helm upgrade`

  > See [Tips, below](#monitor-hashstore-conversion-progress-and-completion) for
  > how to detect when hashstore conversion finishes

**When hashstore conversion has finished successfully...**

- [ ] Check values overrides and update any @TODOs to match live settings. See [BEFORE STARTING,
  above](#6-migration-only-final-switch-over-from-legacy-to-k8s).
- [ ] If applicable, re-enable `dataone.nodeSynchronize` and/or `dataone.nodeReplicate`
- [ ] Point the deployment at the **PRODUCTION CN** (`https://cn.dataone.org/cn`, which is the
      default) by deleting this entry:

    ```yaml
    ## TODO: DELETE ME WHEN READY TO GO LIVE! Will then default to production CN
    global:
      d1ClientCnUrl: https://cn-sandbox.test.dataone.org/cn
    ```

- [ ] ONLY if you changed any `dataone.*` member node properties (`dataone.nodeId`, `dataone.subject`,
      `dataone.nodeSynchronize`, `dataone.nodeReplicate`), push them to the CN by setting:

    ```yaml
    metacat:
      ## Set to today's date (UTC timezone), in the YYYY-MM-DD format; example:
      dataone.autoRegisterMemberNode: 2024-11-29
    ```

- [ ] Re-enable probes
- [ ] Do a final `helm upgrade`
- [ ] Make sure metacatui picked up the CN changes - may need to restart the pod manually

**When everything is up and running...**

- [ ] Switch DNS to point to k8s ingress instead of legacy. To get current IP address and hostname:

    ```shell
    kubectl get ingress -o yaml | egrep "(\- ip:)|(\- host:)"
    ```

- [ ] Take down the legacy instance (and decommission later, as necessary)
- [ ] Index only the newer datasets:

    ```shell
    # on your local machine:
    cd <metacat>/src/scripts/bash/k8s
    ./index-delta.sh <start-time>
    # where <start-time> is the time an hour or more before the previous rsync,
    #     in the format: yyyy-mm-dd HH:MM:SS (with a space; e.g. 2024-11-01 14:01:00)
    ```

- [ ] `git commit` a copy of the values overrides file used for this release, and update ChangeLog
      with the commit `sha`.

---

## Tips:

### To change the database user's password for your existing database

*   Note `postgres` user:
    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U postgres metacat << EOF
      ALTER USER metacat WITH PASSWORD 'new-password-here';
    EOF"
    ```

### See how many "old" datasets exist in DB, before the upgrade:

*   ```shell
    kubectl exec metacatarctic-postgresql-0 -- bash -c "psql -U metacat << EOF
      select count(*) as docs from xml_documents where docid not like 'autogen%';
      select count(*) as revs from xml_revisions where docid not like 'autogen%';
    EOF"
    ```

### Monitor Database Upgrade Completion

* check in `version_history` table:
    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      select version from version_history where status='1';
    EOF"
    ```

### Monitor Hashstore Conversion Progress and Completion

* **To monitor progress:** check the number of rows in the `checksums` table: total # rows should
  be: `5 * (total objects)`, (approx; not accounting for conversion errors), where total object
  count can be found from `https://$HOSTNAME/CONTEXT/d1/mn/v2/object`
   ```shell
   # get number of entries in `checksums` table -- should be approx 5*(total objects)
   kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
     select count(*) from checksums;
   EOF"
   ```
* **To detect when hashstore conversion finishes:**
  ```shell
  # EITHER CHECK STATUS FROM DATABASE...
  kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
    select storage_upgrade_status from version_history where status='1';
  EOF"

  # ...OR CHECK LOGS
  # If log4j root level is INFO
  egrep "\[INFO\]: The conversion took [0-9]+ minutes.*HashStoreUpgrader:upgrade"

  # If log4j root level is WARN, can also grep for this, if errors:
  egrep "\[WARN\]: The conversion is complete"
  ```

### Fix Hashstore Conversion Errors

See the `Tips` section of the [Metacat K8s 3.0.0 to 3.1.0 Upgrade Steps Checklist](https://github.com/NCEAS/metacat/blob/main/helm/admin/3.0.0-to-3.1.0-upgrade-checklist.md#tips) for steps to resolve hashstore conversion errors

### Monitor Indexing Progress:

#### Using the RabbitMQ Dashboard:
* Enable port forwarding:
   ```shell
   kubectl port-forward service/${RELEASE_NAME}-rabbitmq-headless 15672:15672
   ```

* then browse [http://localhost:15672](http://localhost:15672). Username `metacat-rmq-guest` and
  RabbitMQ password from metacat Secrets, or from:
   ```shell
   secret_name=$(kubectl get secrets | egrep ".*\-metacat-secrets" | awk '{print $1}')
   rmq_pwd=$(kubectl get secret "$secret_name" \
           -o jsonpath="{.data.rabbitmq-password}" | base64 -d)
   echo "rmq_pwd: $rmq_pwd"
   ```
> [!NOTE]
> queue activity is not a reliable indicator of indexing progress, since the index
> workers continue to process tasks even after the queue has been emptied. The best way to
> determine when indexing is complete is to monitor the logs, as follows...

#### Determining when indexing is complete

* Ensure the indexer log level has been set to INFO
* grep the logs for the last occurrence of `Completed the index task from the index queue`:
   ```shell
   kubectl logs --max-log-requests 100 -f --tail=100 -l app.kubernetes.io/name=d1index \
        | grep "Completed the index task"
   ```
* You must be sure indexing has finished before trying to find the last occurrence. Note that some
    indexing tasks can take more than an hour.

### Creating Volume Credentials Secret for the PVs

> [!IMPORTANT]
> When creating volume credentials secret:

1. For the userID, omit the “client.” from the beginning of the username before base64 encoding
   it; e.g.: if your username is `client.k8s-dev-metacatknb-subvol-user`, use only
   `k8s-dev-metacatknb-subvol-user`
2. Use `echo -n` when encoding; i.e:
    ```shell
    echo -n myUserID    |  base64
    echo -n mypassword  |  base64
    ```

### Get sizing information for PVs

  ```shell
  $ du -sh /var/metacat /var/lib/postgresql/14
  5.6T /var/metacat
  255.4G /var/lib/postgresql/14
  ```


### If a PV can't be unmounted
* e.g. if the PV name is `cephfs-releasename-metacat-varmetacat`:

    ```shell
    kubectl patch pv cephfs-releasename-metacat-varmetacat -p '{"metadata":{"finalizers":null}}'
    ```

### If a PV Mount is Doing Strange Things... (e.g. you're unable to change the `rootPath`)

* Kubernetes sometimes has trouble changing a PV mount, even if you delete and re-create it
* If you create a PV and then decide you need to change the `rootPath`, the old version may still be
  'cached' on any nodes where it has previously been accessed by a pod. This can lead to confusing
* behavior that is inconsistent across nodes.
* To work around this, first delete the PV (after deleting any PVC that reference it), and then
  **create it with a different name.**

### If the metacat pod keeps restarting
* Look for this in the logs:
    ````
    rm: cannot remove '/var/metacat/config/metacat-site.properties': Permission denied
    ````
* Ensure the config directory on the PV (for example: `/mnt/ceph/repos/$REPO/metacat/config`) allows
  **group write** (`chmod 660`) after the rsync has been completed or repeated.

### Where to Find Existing Hostname Aliases

* Look at the legacy installation in the `/etc/apache2/sites-enabled/` directory; e.g.:

  ```shell
  # ls /etc/apache2/sites-enabled/
    aoncadis.org.conf      arcticdata.io.conf      beta.arcticdata.io.conf
    # ...etc
  ```

* the `ServerName` and `ServerAlias` directives are in these `.conf` files, e.g.:

  ```
   <IfModule mod_ssl.c>
   <VirtualHost *:443>
           DocumentRoot /var/www/arcticdata.io/htdocs
           ServerName arcticdata.io
           ServerAlias www.arcticdata.io permafrost.arcticdata.io
  ```

* Aliases can be set up easily, as follows:

  ```yaml
  ingress:
    rewriteRules: |
      if ($host = "evos.nceas.ucsb.edu") {
        return 301 https://goa.nceas.ucsb.edu$request_uri;
      }
      if ($host = "gulfwatch.nceas.ucsb.edu") {
        return 301 https://goa.nceas.ucsb.edu$request_uri;
      }
    annotations:
      cert-manager.io/cluster-issuer: "letsencrypt-prod"
      nginx.ingress.kubernetes.io/server-alias: evos.nceas.ucsb.edu, gulfwatch.nceas.ucsb.edu

    tls:
      - hosts:
          - goa.nceas.ucsb.edu
          - evos.nceas.ucsb.edu
          - gulfwatch.nceas.ucsb.edu
        secretName: ingress-nginx-tls-cert

    rules:
      - host: goa.nceas.ucsb.edu
        http:
          paths:
          ## ...etc.
          ## No need to add rules for evos.nceas.ucsb.edu or gulfwatch.nceas.ucsb.edu
  ```

> [!NOTE]
> _sometimes, it may not be necessary to incorporate all these aliases in the k8s
> environment. For prod ADC, for example, we left apache running with these aliases and complex
> rewrites in place, and transferred only the `arcticdata.io` domain. [see Issue
> #1954](https://github.com/NCEAS/metacat/issues/1954)_
