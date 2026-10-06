# Installation and Upgrade Tips:

## Table of Contents

- [Database](#database)
  - [To change the database user's password for your existing database](#to-change-the-database-users-password-for-your-existing-database)
  - [See how many "old" datasets exist in DB, before the upgrade:](#see-how-many-old-datasets-exist-in-db-before-the-upgrade)
  - [Monitor Database Upgrade Completion](#monitor-database-upgrade-completion)
- [HashStore Conversion](#hashstore-conversion)
  - [Monitor Hashstore Conversion Progress and Completion](#monitor-hashstore-conversion-progress-and-completion)
  - [Fix Hashstore Error - PID Doesn't Exist in `identifier` Table:](#fix-hashstore-error---pid-doesnt-exist-in-identifier-table)
  - [Fix Hashstore Error - "null" error message:](#fix-hashstore-error---null-error-message)
- [Indexing](#indexing)
  - [Monitor Indexing Progress:](#monitor-indexing-progress)
    - [Using the RabbitMQ Dashboard:](#using-the-rabbitmq-dashboard)
    - [Determining when indexing is complete](#determining-when-indexing-is-complete)
- [Volumes (PVs and PVCs)](#volumes-pvs-and-pvcs)
  - [Creating Volume Credentials Secret for the PVs](#creating-volume-credentials-secret-for-the-pvs)
  - [Get sizing information for PVs](#get-sizing-information-for-pvs)
  - [If a PV can't be unmounted](#if-a-pv-cant-be-unmounted)
  - [If a PV Mount is Doing Strange Things... (e.g. you're unable to change the `rootPath`)](#if-a-pv-mount-is-doing-strange-things-eg-youre-unable-to-change-the-rootpath)
  - [If the metacat pod keeps restarting](#if-the-metacat-pod-keeps-restarting)
- [Ingress](#ingress)
  - [Where to Find Existing Hostname Aliases](#where-to-find-existing-hostname-aliases)

## Database

### To change the database user's password for your existing database

*   Note `postgres` user:
    ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U postgres metacat << EOF
      ALTER USER metacat WITH PASSWORD 'new-password-here';
    EOF"
    ```

### See how many "old" datasets exist in DB, before the upgrade:

*   ```shell
    kubectl exec ${RELEASE_NAME}-postgresql-0 -- bash -c "psql -U metacat << EOF
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

### Convert Timestamps to UTC

If the legacy host machine was running in a non-UTC timezone (e.g. Pacific time), we must convert
the timestamps in several tables to UTC by running the following SQL queries:

> [!CAUTION]
> THIS MUST BE DONE IMMEDIATELY AFTER RUNNING `pg_restore`, BEFORE ANY MODIFICATIONS ARE MADE TO THE
> DATABASE (i.e. BEFORE THE HASHSTORE CONVERSION)

```SQL
\set AUTOCOMMIT off

SELECT count(*) FROM systemmetadata;
UPDATE systemmetadata SET date_uploaded=(date_uploaded::timestamp at time zone 'America/Los_Angeles' at time zone 'UTC');
# If their numbers math, commit; otherwise rollback
commit;

SELECT count(*) FROM systemmetadata;
UPDATE systemmetadata SET date_modified=(date_modified::timestamp at time zone 'America/Los_Angeles' at time zone 'UTC');
# If their numbers math, commit; otherwise rollback
commit;

SELECT count(*) FROM smreplicationstatus;
UPDATE smreplicationstatus SET date_verified=(date_verified::timestamp at time zone 'America/Los_Angeles' at time zone 'UTC');
# If their numbers math, commit; otherwise rollback
commit;

SELECT count(*) FROM access_log;
UPDATE access_log SET date_logged=(date_logged::timestamp at time zone 'America/Los_Angeles' at time zone 'UTC');
# If their numbers math, commit; otherwise rollback
commit;

SELECT count(*) FROM index_event;
UPDATE index_event SET event_date=(event_date::timestamp at time zone 'America/Los_Angeles' at time zone 'UTC');

# If their numbers match, commit; otherwise rollback
commit;
```


## HashStore Conversion

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

### Fix Hashstore Error - "null" error message:

This fix applies when the `/var/metacat/.metacat/generalError_{timestamp}.txt` file contains entries like this:

```
esa.34.1 null
```

...(where "null" is where the exception message should appear), and the logs contain this error for each one:

```
metacat 20250212-16:59:13: [ERROR]: Cannot move the object esa.34.1 to hashstore since null
    [edu.ucsb.nceas.metacat.admin.upgrade.HashStoreUpgrader:convert:541]
org.dataone.exceptions.MarshallingException: null
    [...]
Caused by: javax.xml.bind.MarshalException
    [...]
Caused by: org.xml.sax.SAXParseException: cvc-pattern-valid: Value '' is not facet-valid with
    respect to pattern '[\s]*[\S][\s\S]*' for type 'NonEmptyString'.
    [...]
```

Steps to resolve:

1. Ensure it was a pre-existing issue, by requesting the system metadata from the original source, e.g.: https://HOSTNAME/CONTEXT/d1/mn/v2/meta/GUID

2. Ensure nothing is missing from the `systemmetadata` and `xml_access` tables:
    ```sql
    select * from systemmetadata where guid='esa.34.1';
    select * from xml_access where guid='esa.34.1';
    ```

3. Check the `smreplicationpolicy` table:

    ```sql
    > \x
    Expanded display is on.
    > select * from smreplicationpolicy where guid='df35b.5.1';
    -[ RECORD 1 ]----------
    guid        | df35b.5.1
    member_node |
    policy      | blocked
    policy_id   | 16167
    -[ RECORD 2 ]----------
    guid        | df35b.5.1
    member_node |
    policy      | preferred
    policy_id   | 16168
    ```
   Note that the `member_node` is missing in each case. This is the cause of the error.

4. Try to determine what the intended value should be, from `metacat.properties`:

```properties
dataone.replicationpolicy.default.preferredNodeList=urn:node:KNB
dataone.replicationpolicy.default.blockedNodeList=
```

(in this case, we can assume that `preferred member_node` can be updated to `urn:node:KNB`, and that the `blocked` entry can be deleted)

```sql
> delete from smreplicationpolicy where guid='df35b.5.1' and policy='blocked';
DELETE 1
> update smreplicationpolicy set member_node='urn:node:KNB'
                             where guid='df35b.5.1' AND policy='preferred';
>UPDATE 1
-- DON'T FORGET TO...
> COMMIT;
```

Finally, set the hashstore conversion status back to 'pending':

```sql
> update version_history set storage_upgrade_status='pending' where status='1';
UPDATE 1
> COMMIT;
```

...and restart the pod.

## Indexing

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

## Volumes (PVs and PVCs)

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

## Ingress

### Where to Find Existing Hostname Aliases

(when migrating from a legacy installation)

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
