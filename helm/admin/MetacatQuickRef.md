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

- [ ] Make a copy of the [`metacat/helm/admin/secrets.yaml`](secret--metacat.yaml) file and rename to
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
> creating your own secret](./Installation-Upgrade-Tips.md#creating-volume-credentials-secret-for-the-pvs),
> and [this DataONEorg/k8s-cluster example
> ](https://github.com/DataONEorg/k8s-cluster/blob/main/storage/Ceph/CephFS/helm/secret.yaml).

- [ ] Get the current volume sizes from the legacy installation, to help with sizing the PVs -
      [example](./Installation-Upgrade-Tips.md#get-sizing-information-for-pvs)
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
        [hostname aliases tip](./Installation-Upgrade-Tips.md#where-to-find-existing-hostname-aliases)
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

### MetacatUI:
Carefully review all Metacat's `global.metacatUi*` values and update as needed, depending upon
whether you are using the MetacatUI sub-chart or not:
- **For separately-deployed MetacatUI:**
  - [ ] in the Metacat values overrides, set `global.includeMetacatUi: false` and override the
    `global.metacatUiIngressBackend` settings subtree, and `global.metacatUiWebRoot`, if needed.
    (`global.metacatUiThemeName` is not needed in Metacat values, for this type of deployment.)
  - [ ] in the values overrides for the separate MetacatUI chart:
    - Set `global.metacatUiThemeName` and `global.metacatExternalBaseUrl` (REQUIRED)
    - Override `global.metacatAppContext` if needed (default is 'metacat')
    - Override `global.metacatUiWebRoot` if needed (default is '/')
    - Override `global.d1ClientCnUrl` to point at the sandbox CN
      ("https://cn-sandbox.test.dataone.org/cn"), until final release (default is production CN)

- **If using a bundled theme (arctic, knb etc.):**
  - for PROD, no further action required
  - [ ] for TEST, override at least `global.metacatExternalBaseUrl` and `global.d1ClientCnUrl`

- **If using a theme from [metacatui-themes](https://github.com/NCEAS/metacatui-themes):**
  - [ ] it must be made available on a ceph/PV/PVC mount; e.g:

      ```yaml
      customTheme:
        enabled: true
        claimName: metacatsfwmd-metacatui-customtheme
        subPath: metacatui-themes/src/cerp/js/themes/cerp
      ```

  - [ ] Ensure metacatui has read access

      ```shell
      chmod -R o+r .
      find . -type d -print0 | xargs -0 chmod o+x
      # set default ACLs so new files and directories from `git pull` will also be readable
      sudo setfacl -R -d -m o:rx .
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

- [ ] Point the deployment at the **SANDBOX CN** (and if you're not using the included MetacatUI
      sub-chart, make sure your external MetacatUI instance is also pointing to the sandbox CN):

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
> upgrades after incremental rsyncs will be very fast.
> [This tip](./Installation-Upgrade-Tips.md#see-how-many-old-datasets-exist-in-db-before-the-upgrade)
> shows how to check the number of "old" datasets exist in DB, before the upgrade

- [ ] set `storage.hashstore.disableConversion: true`, so the hashstore converter won't run yet
- [ ] `helm install`, debug any startup and configuration issues, and allow database upgrade to
      finish.

  > See [this tip](./Installation-Upgrade-Tips.md#monitor-database-upgrade-completion), for how
  > to detect when database conversion finishes

> [!TIP]
> because hashstore conversion has not yet been done, it is expected for metacatUI to
> display `Oops! It looks like there was a problem retrieving your search results.`, and for
> `/metacat/d1/mn/v2/` api calls to display `Metacat has not been configured`

- [ ] Delete the `storage.hashstore.disableConversion:` setting, so the hashstore converter will
      run, and do a `helm upgrade`. (How to detect when hashstore conversion finishes? See [This
      tip](./Installation-Upgrade-Tips.md#monitor-hashstore-conversion-progress-and-completion))
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

  > [See this tip](./Installation-Upgrade-Tips.md#monitor-indexing-progress) for monitoring indexing
  > progress.


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
> reflect the new hostname(s) - see [this tip](./Installation-Upgrade-Tips.md#where-to-find-existing-hostname-aliases).

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

- [ ] Move or delete the current PG data directory being used by k8s, so that the pg_dump will
  automatically be ingested on next startup

    ```shell
    cd /mnt/ceph/repos/$REPO/postgresql
    sudo mv 17 17-deleteme  # or delete
    ```

- [ ] Disable probes again, until Database Upgrade is finished
- [ ] Set `storage.hashstore.disableConversion: true`, so the hashstore converter won't run.

### = = = = = = = = = = = = = ON LEGACY HOST = = = = = = = = = = = = =

**ENSURE NOBODY IS IN THE MIDDLE OF A BIG UPLOAD!** (Can schedule off-hours, but how to monitor?)

- [ ] Edit `/var/lib/tomcat9/webapps/metacat/WEB-INF/metacat.properties` to change to:

     ```properties
     application.readOnlyMode=true
     ```

- [ ] Restart tomcat (not postgres)

     ```shell
     sudo systemctl restart tomcat9
     ```

- [ ] Check it's in RO mode! `https://$HOSTNAME/CONTEXT/d1/mn/v2/node` - look for:

     ```xml
      <property key="read_only_mode">true</property>
     ```

- [ ] `pg_dump` on legacy host

    ```shell
    JOBS=20 # adjust for available cpu cores
    DEST="/var/lib/postgresql/14-pg_dump"
    PGDB=metacat
    POSTGRES_USER=metacat
    sudo pg_dump -U $POSTGRES_USER -h localhost --format=directory --file=$DEST --jobs=$JOBS $PGDB
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

     sudo chown -R 59997:59997 data documents logs

     sudo chmod -R g+rw data documents dataone
     ```

- [ ] `helm-install`, and ensure the `pgupgrade` initContainer finished successfully.

**When the `pgupgrade` initContainer has finished successfully...**

> [!CAUTION]
> - [ ] If the legacy host machine was running in a non-UTC timezone (e.g. Pacific time), we must
>       convert the timestamps in several tables to UTC by running the SQL queries defined in
>       [Installation-Upgrade-Tips.md](./Installation-Upgrade-Tips.md#convert-timestamps-to-utc).
>
> - A better strategy is to make the metacat pod run in the same timezone as the legacy instance,
>   until the entire migration is complete, and only then convert the timestamps to UTC using the
>   above queries.

- [ ] Restore the `checksums` table from the backup, so hashstore won't try to reconvert
      completed files:

    ```shell
    kubectl exec ${RELEASE_NAME}-0 -- bash -c \
      "SCRIPT=\$TC_HOME/webapps/metacat/WEB-INF/scripts/sql/backup-restore-checksums-table/restore-checksums-table.sh \
      && chmod 750 \$SCRIPT \
      && bash -c \$SCRIPT"
    ```

- [ ] Delete the `storage.hashstore.disableConversion:` setting, so the hashstore converter will
      run, and do a `helm upgrade`. Watch for completion:

    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      select storage_upgrade_status from version_history where version='3.1.0';
    EOF"
    ```

    ...or see [this tip](./Installation-Upgrade-Tips.md#monitor-hashstore-conversion-progress-and-completion) for other monitoring options

**When hashstore conversion has finished successfully...**

- [ ] Run `ANALYZE` to ensure PostgreSQL's stats are updated. This will ensure that `autovacuum`
      will run automatically (`ANALYZE` is run by `autovacuum`, but `autovacuum` won't run unless
      `ANALYZE` has been manually run after large updates):

    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
      ANALYZE;
    EOF"
    ```

- [ ] If an object went through the initial hashstore conversion, but then its sysmeta was
      subsequently updated in legacy, we need to copy the new sysmeta from database to hashstore (because the "delta" conversion will have ignored that object). To do this, run:
    ```shell
    kubectl exec ${RELEASE_NAME}-0 -- bash -c \
      "SCRIPT=\$TC_HOME/webapps/metacat/WEB-INF/scripts/sql/hashstore-conversion/copy-sysmeta-to-hashstore.sh \
      && chmod 750 \$SCRIPT \
      && bash -c \$SCRIPT"
    ```

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
- [ ] If you're not using the included MetacatUI sub-chart, make sure your external MetacatUI
      instance is also pointing to the production CN (delete `global.d1ClientCnUrl` in its values
      overrides, since it defaults to production). In either case, ensure the MetacatUI pod restarts
- [ ] ONLY if you changed any `dataone.*` member node properties (`dataone.nodeId`,
      `dataone.subject`, `dataone.nodeSynchronize`, `dataone.nodeReplicate`), push them to the CN by
      setting:

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

- [ ] Stop Tomcat, PostgreSQL and Apache on the legacy VM instance
  - [ ] create an [issue here](https://github.nceas.ucsb.edu/NCEAS/Computing/issues) to retire the
    VM ([template](https://github.nceas.ucsb.edu/NCEAS/Computing/blob/master/server_archiving.md#virtual-servers)
  - [ ] Create an [issue here](https://github.nceas.ucsb.edu/NCEAS/Computing/issues) to Set Backups
    for the Ceph Repo ([template](https://github.nceas.ucsb.edu/NCEAS/Computing/issues/364))
