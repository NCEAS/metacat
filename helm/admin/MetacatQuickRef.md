# Checklist: Metacat K8s Installation Steps

> **= = = THIS IS A TEMPLATE - MAKE YOUR OWN COPY BEFORE CHECKING BOXES! = = =**

> ## IMPORTANT NOTES - before you begin...
> 1. **PURPOSE:** This ordered checklist is for either:
>    * Creating a new, empty Metacat v3.1.0+ installation on a Kubernetes (K8s) cluster, or
>    * Migrating and upgrading an existing, non-K8s Metacat v2.19.x instance to become a Metacat
>      v3.1.0+ K8s installation.
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
> 6. Some references below are specific to NCEAS infrastructure (e.g. CephFS storage); adjust as
>    needed for your own installation.
> 7. Assumptions: you have a working knowledge of Kubernetes deployment, including working with yaml
>    files, helm and kubectl commands, and your kubectl context is set for the target deployment
>    location

## 1. `(MIGRATION ONLY)` Copy Data and Set Ownership & Permissions

- [ ] first `rsync` the data from the 2.19 instance over to cephfs (OK to leave postgres & tomcat
      running)
    ```shell
    # can also prepend with time, and use --stats --human-readable, and/or --dry-run
    #
    # metacat:
    sudo rsync -aHAX /var/metacat/data/      /mnt/ceph/repos/REPO-NAME/metacat/data/
    sudo rsync -aHAX /var/metacat/dataone/   /mnt/ceph/repos/REPO-NAME/metacat/dataone/
    sudo rsync -aHAX /var/metacat/documents/ /mnt/ceph/repos/REPO-NAME/metacat/documents/
    sudo rsync -aHAX /var/metacat/logs/      /mnt/ceph/repos/REPO-NAME/metacat/logs/

    # postgres:
    sudo rsync -aHAX /var/lib/postgresql/    /mnt/ceph/repos/REPO-NAME/postgresql/
    ```

- [ ] After rsyncs are complete, change ownership **ON CEPHFS** as follows:

    ```shell
    ## postgres (59996:59996) in postgresql data directory
    sudo chown -R 59996:59996 /mnt/ceph/repos/REPO-NAME/postgresql

    ## tomcat (59997:59997) in metacat directory
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

    ```yaml
    metadata:
      name: ${RELEASE_NAME}-metacat-secrets
    ```

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
      needed - see [hostname aliases tip, below](#where-to-find-existing-hostname-aliases))
- [ ] Set up Node cert and replication etc. as needed -  [see
      README](https://github.com/NCEAS/metacat/tree/main/helm#setting-up-certificates-for-dataone-replication).
  - [ ] Don't forget to [install the ca
        chain](https://github.com/NCEAS/metacat/tree/main/helm#install-the-ca-chain), and also
        enable incoming client cert forwarding:
      ```yaml
      metacat:
        dataone.certificate.fromHttpHeader.enabled: true
      ```

* MetacatUI:
  - [ ] ALWAYS set `global.metacatUiThemeName`
  - [ ] If using a bundled theme (arctic, knb etc.):
    - for PROD, no further action required
    - for TEST, override at least `global.metacatExternalBaseUrl` and
      `global.d1ClientCnUrl`

  - [ ] If using a theme from [metacatui-themes](https://github.com/NCEAS/metacatui-themes), this
        must be made available on a ceph/PV/PVC mount; e.g:

      ```yaml
        customTheme:
          enabled: true
          claimName: metacatsfwmd-metacatui-customtheme
          subPath: metacatui-themes/src/cerp/js/themes/cerp
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

**== IMPORTANT! == IF MOVING DATA FROM AN EXISTING DEPLOYMENT THAT IS ALSO A DATAONE MEMBER NODE, DO
NOT REGISTER THIS NODE WITH THE PRODUCTION CN UNTIL YOU'RE READY TO GO LIVE, or bad things will
happen...**

- [ ] Point the deployment at the **SANDBOX CN**

    ```yaml
    global:
      d1ClientCnUrl: https://cn-sandbox.dataone.org/cn
    ```

- [ ] The Node ID (in `metacat.dataone.nodeId` and `metacat.dataone.subject`) **MUST MATCH the
      legacy deployment!** (Don't use a temp ID; this will be persisted into
      hashstore during conversion!)
- [ ] The `metacat.dataone.autoRegisterMemberNode:` flag **MUST NOT match today's date!**
- [ ] Existing node already syncing to D1? Set `dataone.nodeSynchronize: false` until after final
      switch-over!
- [ ] Existing node already accepting D1 replicas? Set `dataone.nodeReplicate: false` after final
      switch-over!
- [ ] If legacy DB version was < 3.0.0, Disable `livenessProbe` & `readinessProbe` until Database
      Upgrade is finished.

  > NOTE: Upgrade only writes OLD datasets -- ones not starting `autogen` -- from DB to disk.
  These should all be finished after the first upgrade - so provided subsequent `/var/metacat/` rsyncs are
  only additive (don't `--delete` destination files not on source), then subsequent DB upgrades
  after incremental rsyncs will be very fast. [Tips,
  below](#see-how-many-old-datasets-exist-in-db-before-the-upgrade) show how to check the number
  of "old" datasets exist in DB, before the upgrade

- [ ] set `storage.hashstore.disableConversion: true`, so the hashstore converter won't run yet
- [ ] `helm install`, debug any startup and configuration issues, and allow database upgrade to
      finish.

  > See [Tips, below](#monitor-database-upgrade-completion), for how to detect when
  > database conversion finishes

  > **NOTE:** because hashstore conversion has not yet been done, it is expected for metacatUI to
  > display `Oops! It looks like there was a problem retrieving your search results.`, and for
  > `/metacat/d1/mn/v2/` api calls to display `Metacat has not been configured`

- [ ] In the metacat database, verify that all the `systemmetadata.checksum_algorithm` entries are
      on the [list of supported
      algorithms](https://github.com/DataONEorg/hashstore-java/blob/main/src/main/java/org/dataone/hashstore/filehashstore/FileHashStore.java#L63)
      (NOTE: syntax matters! E.g. `sha-1` is OK, but `sha1` isn't):
    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      SELECT DISTINCT checksum_algorithm FROM systemmetadata WHERE checksum_algorithm NOT IN
            ('MD2','MD5','SHA-1','SHA-256','SHA-384','SHA-512','SHA-512/224','SHA-512/256');
    EOF"

    # then manually update each to the correct syntax; e.g:
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      UPDATE systemmetadata SET checksum_algorithm='SHA-1' WHERE checksum_algorithm='SHA1';
    EOF"
    # ...etc
    ```

- [ ] Delete the `storage.hashstore.disableConversion:` setting, so the hashstore converter will
      run, and do a `helm upgrade`.

  > See [Tips, below](#monitor-hashstore-conversion-progress-and-completion) for
  > how to detect when hashstore conversion finishes

- [ ] When database upgrade and hashstore conversion have both finished, re-enable probes
- [ ] Re-index all datasets (Did with 25 indexers for test.adc on dev; 50 on prod)
    ```shell
    kubectl get secret ${RELEASE_NAME}-d1-client-cert -o jsonpath="{.data.d1client\.crt}" | \
        base64 -d > DELETEME_NODE_CERT.pem

    curl -X PUT --cert ./DELETEME_NODE_CERT.pem "https://HOSTNAME/CONTEXT/d1/mn/v2/index?all=true"
    # look for <scheduled>true</scheduled> in response

    # don't forget to delete the cert file:
    rm DELETEME_NODE_CERT.pem
    ```
  > [See Tips, below](#monitor-indexing-progress-via-rabbitmq-dashboard) for monitoring indexing
  > progress via RabbitMQ dashboard.


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
> NOTE: If you need to accommodate hostname aliases, you'll need to update the `ingress.tls`
> section to reflect the new hostname(s) - see [Tips,
> below](#where-to-find-existing-hostname-aliases).

### = = = = = = = = = = = = = IN K8S CLUSTER: = = = = = = = = = = = = =
- [ ] Make a backup of the `checksums` table so hashstore won't try to reconvert completed files:

     ```shell
     # inside metacat pod, run the backup script:
     kubectl exec ${RELEASE_NAME}-0 -- bash -c \
       "$TC_HOME/webapps/metacat/WEB-INF/scripts/sql/backup-restore-checksums-table/backup-checksums-table.sh"
     ```

- [ ] `helm delete` the running installation. (keep all secrets, PVCs etc!)

### = = = = = = = = = = = = = ON LEGACY HOST = = = = = = = = = = = = =

**ENSURE NOBODY IS IN THE MIDDLE OF A BIG UPLOAD!** (Can schedule off-hours, but how to monitor?)

- [ ] Stop postgres and tomcat

     ```shell
     # ssh to legacy host, then...
     sudo systemctl stop postgresql
     sudo systemctl stop tomcat9
     ```

- [ ] "top-up" `rsync` from legacy to ceph:

     ```shell
     # NOTES:
     # 1. Don't use -aHAX (like orig. rsync); use -rltDHX to not overwrite ownership or permissions
     # 2. Don't use --delete option for /var/metacat/ rsync
     # 3. Optionally use --dry-run to check first
     #
     sudo rsync -rltDHX --stats --human-readable \
               /var/metacat/data/         /mnt/ceph/repos/REPO-NAME/metacat/data/

     sudo rsync -rltDHX --stats --human-readable \
               /var/metacat/dataone/      /mnt/ceph/repos/REPO-NAME/metacat/dataone/

     sudo rsync -rltDHX --stats --human-readable \
               /var/metacat/documents/    /mnt/ceph/repos/REPO-NAME/metacat/documents/

     sudo rsync -rltDHX --stats --human-readable \
               /var/metacat/logs/         /mnt/ceph/repos/REPO-NAME/metacat/logs/

     # postgres
     sudo rsync -rltDHX --stats --human-readable --delete \
               /var/lib/postgresql/       /mnt/ceph/repos/REPO-NAME/postgresql/
     ```

- [ ] While rsync is in progress, edit `/var/metacat/config/metacat-site.properties` to add:

     ```properties
     application.readOnlyMode=true
     ```

- [ ] When rsync done, start postgres, and then start tomcat.

     ```shell
     sudo systemctl start postgresql
     sudo systemctl start tomcat9
     ```

- [ ] Check it's in RO mode! https://HOSTNAME/CONTEXT/d1/mn/v2/node - look for:

     ```xml
      <property key="read_only_mode">true</property>
     ```

### = = = = = = = = = = = = = IN K8S CLUSTER: = = = = = = = = = = = = =

- [ ] fix ownership and permissions of newly-copied files:

     ```shell
     ## postgres (59996:59996) in postgresql data directory
     sudo chown -R 59996:59996 /mnt/ceph/repos/REPO-NAME/postgresql

     ## tomcat (59997:59997) in metacat directory
     cd /mnt/ceph/repos/REPO-NAME/metacat

     sudo chown -R 59997:59997 data dataone documents logs

     sudo chmod -R g+rw data documents dataone
     ```

- [ ] If (legacy DB version) < (k8s db version), disable Probes until Database Upgrade is finished
- [ ] Set `storage.hashstore.disableConversion: true`, so the hashstore converter won't run.
- [ ] `helm-install`
- [ ] Repeat correction of checksum algorithm names for any new records:

    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      SELECT DISTINCT checksum_algorithm FROM systemmetadata WHERE checksum_algorithm NOT IN
            ('MD2','MD5','SHA-1','SHA-256','SHA-384','SHA-512','SHA-512/224','SHA-512/256');
    EOF"

    # then manually update each to the correct syntax; e.g:
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      UPDATE systemmetadata SET checksum_algorithm='SHA-1' WHERE checksum_algorithm='SHA1';
    EOF"
    # ...etc
    ```

- [ ] Restore the `checksums` table from the backup, so hashstore won't try to reconvert
      completed files:

    ```shell
    # inside metacat pod, run the restore script:
    kubectl exec ${RELEASE_NAME}-0 -- bash -c \
      "$TC_HOME/webapps/metacat/WEB-INF/scripts/sql/backup-restore-checksums-table/restore-checksums-table.sh"
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
    ## TODO: DELETE ME WHEN READY TO GO LIVE!
    global:
      d1ClientCnUrl: https://cn-sandbox.dataone.org/cn
    ```

- [ ] In order to push `dataone.*` member node properties (`dataone.nodeId`, `dataone.subject`,
      `dataone.nodeSynchronize`, `dataone.nodeReplicate`) to the CN, set:

    ```yaml
    metacat:
      ## Set to today's date (UTC timezone), in the YYYY-MM-DD format; example:
      dataone.autoRegisterMemberNode: 2024-11-29
    ```
- [ ] Do a final `helm upgrade`
- [ ] Make sure metacatui picked up the changes - may need to do some pod-kicking

**When everything is up and running...**

- [ ] Switch DNS to point to k8s ingress instead of legacy. To get current IP address and hostname:

    ```shell
    kubectl get ingress -o yaml | egrep "(\- ip:)|(\- host:)"
    ```

- [ ] Take down the legacy instance
- [ ] Index only the newer datasets:

    ```shell
    # on your local machine:
    cd <metacat>/src/scripts/bash/k8s
    ./index-delta.sh <start-time>
    # where <start-time> is the time an hour or more before the previous rsync,
    #     in the format: yyyy-mm-dd HH:MM:SS (with a space; e.g. 2024-11-01 14:01:00)
    ```

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
  count can be found from `https://HOSTNAME/CONTEXT/d1/mn/v2/object`
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

### Fix Hashstore Error - PID Doesn't Exist in `identifier` Table:

```shell
# If you see this in the metacat logs:
Pid <autogen pid> is missing system metadata. Since the pid starts with autogen and looks like to be
created by DataONE api, it should have the systemmetadata. Please look at the systemmetadata and
identifier table to figure out the real pid.
```
Steps to resolve:

1. Given the docid, get all revisions:
   ```sql
   select * from identifier where docid='<docid>';
   ```
2. Look for pid beginning 'autogen', and note its revision number
3. pid should be the `obsoleted_by` from the previous revision's system metadata:
   ```sql
   select obsoleted_by from systemmetadata where guid='<previous revision pid>';
   ```
4. Check by look at `obsoletes` from the following revision, if one exists:
   ```sql
   select obsoletes from systemmetadata where guid='<following revision pid>';
   ```
5. Check if systemmetadata table has an entry for autogen pid
   ```sql
   select checksum from systemmetadata where guid='<autogen pid>';
   ```
   ...and the checksum matches that of the original file, found in:
   ```shell
   /var/metacat/(data or documents)/<'autogen' docid>.<revision number>
   ```

#### = = = If these exist and do not match, STOP HERE AND INVESTIGATE FURTHER! = = =

6. If an autogen-pid entry was found, update it with the new pid:
   ```sql
   update systemmetadata set guid='<pid from steps 3 & 4>' where guid='<autogen pid>';
   ```
7. Replace the 'autogen' pid with the real pid in the 'identifier' table:
   ```sql
   update identifier set guid='<pid from steps 3 & 4>' where guid='<autogen pid>';
   ```
8. Set the hashstore conversion status back to `pending`:
   ```sql
   update version_history set storage_upgrade_status='pending' where status='1';
   ```
   ...and restart the metacat pod to re-run the hashstore conversion and generate the correct
   sysmeta file in hashstore

### Monitor Indexing Progress via RabbitMQ Dashboard:

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


### Creating Volume Credentials Secret for the PVs

**VERY IMPORTANT when creating volume credentials secret:**
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
* Ensure the config directory on the PV (for example: `/mnt/ceph/repos/REPO-NAME/metacat/config`) allows
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
  **NOTE:** it may not be necessary to incorporate all these aliases in the k8s environment. For
  prod ADC, for example, we left apache running with these aliases in place, and transferred only
  the `arcticdata.io` domain. [see Issue #1954](https://github.com/NCEAS/metacat/issues/1954)
