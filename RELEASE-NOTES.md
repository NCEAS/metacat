# Metacat Release Notes

> [!CAUTION]
> If you are upgrading from a helm chart version earlier than 2.1.0, please see the [Upgrade
> Notes](#chart-upgrade-notes) below. Failure to do so may result in loss of data!

## Release Notes for helm chart 2.1.3

**Release date: 2025-07-29**

> [!CAUTION]
> We strongly recommend that you upgrade to this version of the helm chart before August 28th, 2025,
> since previous versions will stop working after Bitnami introduces restrictions to container image
> availability!

This is a patch release to account for upcoming changes to Bitnami container image availability; see
Bitnami's announcements:

- [Upcoming changes to the Bitnami catalog (effective August 28th, 2025)](https://github.com/bitnami/containers/issues/83267)
- [Clarification on bitnami/charts after August 28th](https://github.com/bitnami/charts/issues/35256)

This latest chart pulls any Bitnami image versions used by Metacat from the `bitnamilegacy`
repository, which will remain functional after the August cutoff. Changes in this release:
- Bump PostgreSQL sub-chart to Helm chart version 16.7.21 (PostgreSQL app version 17.5.0), and
  override repository source to `bitnamilegacy` for the PostgreSQL image.
- Bump dataone-indexer sub-chart to version 1.3.3, which is a patch release to override repository
  sources to `bitnamilegacy` for the indexer sub-charts ([see indexer release notes for
  details](https://github.com/DataONEorg/dataone-indexer/blob/main/RELEASE-NOTES.md))


## Release Notes for Metacat 3.2.2

**Release date: 2025-07-02**

### Version Upgrades and Bug Fixes:

- Fixed: Bug: Bagit setting incorrect Content-Type ([Issue #1953](https://github.com/NCEAS/metacat/issues/1953))
- Fixed: Metacat DataONE admin page doesn't show the error message from CN ([Issue #2040](https://github.com/NCEAS/metacat/issues/2040))
- Fixed: Hashstore convertor may miss some objects which need to be converted ([Issue #2182](https://github.com/NCEAS/metacat/issues/2182))
- Update `eml2osti.xsl` for OSTI Service ([Issue #2185](https://github.com/NCEAS/metacat/issues/2185)), to incorporate bug fix for [OSTI XML Sponsor List starting with semicolon causes error in OSTI submission](https://github.com/ess-dive/essdive-toolset/issues/368)
- Upgrade Postgres from 42.7.4 to 42.7.7 ([issue #2191](https://github.com/NCEAS/metacat/issues/2191))
- Upgrade DataONE-Indexer library to 3.1.5 in metacat-index (see [dataone-indexer Release Notes](https://github.com/DataONEorg/dataone-indexer/blob/main/RELEASE-NOTES.md#dataone-indexer-version-315--helm-chart-version-132) for details)

### Other Enhancements:

- Ensure Metacat Startup Doesn't Fail if CN Registration Unsuccessful ([Issue #2181](https://github.com/NCEAS/metacat/issues/2181))
- Increase `index.resourcemap.waitingComponent.max.attempts` to 200 as a temporary fix for resourcemaps not being successfully indexed
- Utility Scripts for Metacat Administrators:
  - (Python) submit index tasks as low priority in the background, given a list of object IDs (PIDs) ([Issue #2176](https://github.com/NCEAS/metacat/issues/2176))
  - (Python) Synchronize the system metadata of a list of object IDs (PIDs) from Metacat database to the corresponding files in HashStore ([Issue #2166](https://github.com/NCEAS/metacat/issues/2166))
  - (Bash) Determine the subset of objects that have not been indexed, and submit them for indexing ([Issue #2165](https://github.com/NCEAS/metacat/issues/2165))

## Release Notes for helm chart 2.1.2

**Release date: 2025-07-02**

- Bump dataone-indexer sub-chart to version 1.3.2 ([see indexer release notes for
  details](https://github.com/DataONEorg/dataone-indexer/blob/3.1.5/RELEASE-NOTES.md))
- Set [additional Ingress annotations](https://github.com/NCEAS/metacat/commit/80aa42ad) for enabling upload and download of large files and data packages, without timeouts/disconnects.
- Increase `.Values.dataone-indexer.idxworker.resourcemapMaxTries` to 200 as a temporary fix for resourcemaps not being successfully indexed.


## Release Notes for helm chart 2.1.1

**Release date: 2025-05-20**

This is a patch release to upgrade the indexer sub-chart from 1.3.0 to 1.3.1. It does not include
any other changes to the metacat codebase or the metacat helm chart, beyond the documentation edits
listed below.

- Bump dataone-indexer sub-chart to version 1.3.1 ([see indexer release notes for
  details](https://github.com/DataONEorg/dataone-indexer/blob/3.1.4/RELEASE-NOTES.md))
- Documentation-only updates to `MetacatQuickRef.md` and `Installation-Upgrade-Tips.md`
- Updates to the example values-override files in `helm/examples`

> [!CAUTION]
> If you are upgrading from a helm chart version earlier than 2.1.0, please see the [Upgrade
> Notes](#chart-upgrade-notes) below. Failure to do so may result in loss of data!


## Release Notes for Metacat 3.2.1

**Release date: 2025-05-01**

### Version Upgrades and Bug Fixes:
- Fixed [Auto-restart Hashstore Conversion if Interrupted](https://github.com/NCEAS/metacat/issues/2123)
- Fixed [Reclassify Certain Errors as WARN Instead of ERROR During Hashstore Conversion](https://github.com/NCEAS/metacat/issues/2091)
- [Add support for both OSTI E-Link v1 and v2 APIs](https://github.com/NCEAS/metacat/issues/2146)
- [Upgrade DataONE-Indexer library from 3.1.2 to 3.1.3 in metacat-index with the fixed bug of
  restoration of disconnected RabbitMQ connections](https://github.com/DataONEorg/dataone-indexer/issues/176)
- [Upgrade log4j library from 2.17.1 to 2.24.3 in metacat-common](https://github.com/NCEAS/metacat/pull/2124)


## Release Notes for helm chart 2.1.0

**Release date: 2025-05-01**

> [!CAUTION]
> If you are upgrading from an earlier helm chart version, please see the [Upgrade
> Notes](#chart-upgrade-notes) below. Failure to do so may result in loss of data!

### New Features & Enhancements:
- Add `.Values.global.metacatUiIngressBackend` to configure the ingress for a separately-deployed
  MetacatUI installation, if the MetacatUI subchart is not being used; see the [documentation in
  Values.yaml](https://github.com/NCEAS/metacat/blob/b5bd144ae3bf8304546e3d95880f3eba5f7d1b2d/helm/values.yaml#L119-L148)
  for details.
- Reverted the change from chart 1.2.0, which set default container resources requests & limits for
  Metacat, and for PostgreSQL subchart.
  - Reverted based on feedback and helm conventions. Note that resources should still be
    overridden appropriately, for your own production needs.
- Updates to the [helm/admin/MetacatQuickRef.md](./helm/admin/MetacatQuickRef.md) installation,
  upgrade, and migration checklist.
- Reduce RabbitMQ startup time (see [dataone-indexer Issue #
  202](https://github.com/DataONEorg/dataone-indexer/issues/202)). Important: see [Chart Upgrade
  Notes](#chart-upgrade-notes) below, before upgrading.

### Version Upgrades:
- Increase application versions:
  - Metacat app version: 3.2.1
  - MetacatUI app version: 2.33.1 (subchart version 1.0.7)
  - dataone-indexer app version: 3.1.3 (subchart version 1.3.0)

### Chart Upgrade Notes

#### STEPS YOU MUST COMPLETE BEFORE UPGRADING:

> [!WARNING]
> If you are upgrading from version 1.x.x of the helm chart, this will involve a major version
> upgrade of the PostgreSQL sub-chart, and there are steps you must complete BEFORE upgrading;
> please consult [Upgrading From Chart Version 1.x.x](#upgrading-from-chart-version-1xx), below.

> [!CAUTION]
> **ENSURE THAT THE RABBITMQ QUEUE IS EMPTY**, before upgrading (or uninstalling/re-installing) a
> new chart version for the first time! Each new chart version will create a new PV/PVC for
> RabbitMQ, so any queue remnants saved by the previous chart version will be ignored when the new
> chart is deployed!

- This applies only the initial installation or upgrade. After this, RabbitMQ will continue to use
  the same newly-created PV/PVC, and the queue will not be lost. The new PVC will be named:
  `data-[release-name]-rabbitmq-[rmq-version]-[idx]`, where `[rmq-version]` is the rabbitmq app
  version (not the chart version), with periods replaced by dashes, and `[idx]` is the statefulset
  ordinal index; e.g.: `data-metacatarctic-rabbitmq-3-13-7-0`

- When upgrade or installation is complete, you can then safely `kubectl delete` both the old PVC
  and the old PV (provided you're certain the queue was empty).

- (This behavior can be overridden by setting `.Values.dataone-indexer.rabbitmq.nameOverride` to the
  same name used in the previous chart version, but this is **NOT recommended**, since the RabbitMQ
  installation then becomes an upgrade instead of a fresh install, and may require significant
  manual intervention; see the [RabbitMQ upgrade
  documentation](https://www.rabbitmq.com/docs/feature-flags#version-compatibility).)

> [!TIP]
> Setting `.Values.metacat.application.readOnlyMode: true` and restarting the metacat pod will put
> Metacat in Read Only mode, which will prevent edits and uploads, and thus stop indexing tasks
> being added to the queue, so it can be drained in preparation for the upgrade. To speed up the
> queue consumption, You could also temporarily increase the number of index workers, using:
> `kubectl scale deployment [release-name]-d1index --replicas=[n]`, where `[n]` is the number of
> workers you want to run.

## Release Notes for Metacat 3.2.0

**Release date: 2024-04-02**

### New Features & Enhancements:
- [Use executor service and rabbitMQ channel pool to improve the performance of submitting index
  tasks.](https://github.com/NCEAS/metacat/issues/2032)
- [Increase the max number of attempts when solr version conflict errors
  occur](https://github.com/NCEAS/metacat/pull/2060)

### Version Upgrades and Bug Fixes:
- Fixed [failing calls to `/d1/mn/v2/object` for Objects without a format
  id](https://github.com/NCEAS/metacat/issues/1708)
- Fixed [Tomcat startup failure when Metacat is located on a mounted volume](https://github.com/NCEAS/metacat/issues/2069)
- Fixed [A freshly installed Metacat 3.2.0 instance always has an incorrect hashstore conversion status](https://github.com/NCEAS/metacat/issues/2114)
- [Upgrade solr library from 8.11.3 to 9.8 in metacat and
  metacat-common](https://github.com/NCEAS/metacat/issues/1817)
- [Bump org.apache.wicket:wicket-core from 9.18.0 to 9.19.0](https://github.com/NCEAS/metacat/pull/2071)
- [Bump com.rabbitmq:amqp-client from 5.21.0 to 5.25.0](https://github.com/NCEAS/metacat/pull/2065/files)


## Release Notes for helm chart 2.0.0

**Release date: 2025-04-02**

> [!IMPORTANT]
> If you are upgrading from an earlier helm chart version, please see the [Upgrade
> Notes](#upgrading-from-chart-version-1xx) below.

### New Features & Enhancements:
- [Automate k8s Postgresql Upgrades](https://github.com/NCEAS/metacat/issues/1748) - see [Upgrade
  Notes](#upgrading-from-chart-version-1xx) below.
- Set default container resources requests & limits for Metacat, and for PostgreSQL subchart (these
  should be overridden for your own production needs).
- [Enable CORS for k8s Metacat Deployments](https://github.com/NCEAS/metacat/issues/2050)
- [Add the ability to include arbitrary yaml subtree under `spec.rules` in
  ingress.yaml](https://github.com/NCEAS/metacat/pull/2076)
- [allow multiline rewrite rules](https://github.com/NCEAS/metacat/pull/2077)
- faster startup: [reduce 2 `initContainers` (Solr & RabbitMQ) to 1
  (`dependencies`)](https://github.com/NCEAS/metacat/pull/2097)
- [add `fsGroupChangePolicy: OnRootMismatch`](https://github.com/NCEAS/metacat/pull/2098) to speed
  up volume mounting

### Version Upgrades and Bug Fixes:
- [Update Tomcat base image](https://github.com/NCEAS/metacat/issues/2104) from
  tomcat:9.0.96-jre17-temurin-jammy to tomcat:9.0.102-jre17-temurin-noble, to fix potential
  vulnerability ([CVE-2025-24813](https://nvd.nist.gov/vuln/detail/CVE-2025-24813)).
- Increase application versions:
  - Metacat app version: 3.2.0
  - MetacatUI app version: 2.32.2 (subchart version 1.0.5)
  - dataone-indexer app version: 3.1.2 (subchart version 1.2.0)
  - PostgreSQL app version 17.4.0 (subchart version 16.4.14) - see [Upgrade
    Notes](#upgrading-from-chart-version-1xx) below.
- Fixed [multiline YAML values splitting into multiple
  properties](https://github.com/NCEAS/metacat/issues/2053)
- Fixed `413 Request Entity Too Large` error from ingress-nginx when trying to upload large files

### Upgrading From Chart Version 1.x.x

> [!WARNING]
> BEFORE trying to upgrade, you will need to run a script with your old 1.x.x chart running!

Upgrading from Metacat Helm chart version 1.x.x to 2.0.0 and above, involves a major-version upgrade
of the underlying PostgreSQL database. Thus requires the use of either `pg_upgrade`, or a `pg_dump`
followed by a `pg_restore`. [The Metacat Helm chart](https://github.com/NCEAS/metacat/issues/1748)
uses the `pg_dump` and `pg_restore` approach. This involves two steps:
1. Running a script to dump the old database (with your old 1.x.x chart running), and then
2. Doing a `helm uninstall`, then a `helm install` with the new 2.0.0 chart. The new chart has an
   `initContainer` that will automatically restore the database from the dump.

> [!IMPORTANT]
> Please be sure to follow the upgrade steps described in the ["Major Version Upgrades" section of
> the Helm README](https://github.com/NCEAS/metacat/tree/main/helm#major-version-upgrades)!


## Release Notes for Metacat 3.1.0

**Release date: 2024-12-18**

> **IMPORTANT** If you are upgrading from version 2.19, please see the [Upgrade
> Notes](#upgrade-notes-2190-to-300-or-above) below.

The Metacat version 3.1.0 release introduces significant performance enhancements for managing large
datasets. This was achieved by integrating with the new Dataone hash-based storage library,
[`hashstore-java`](https://github.com/DataONEorg/hashstore-java), which necessitated substantial
changes to disk-based file-storage methods.

### New Features & Enhancements:
* Metacat uses the DataONE [`hashstore-java`](https://github.com/DataONEorg/hashstore-java)
  hash-based storage library.
* Existing stored files are seamlessly migrated to the new storage system.
* **Setup no longer requires an auth token in order to index private datasets**, thus simplifying
  installation.
* Upgraded datacite version to 4.5 from 4.3 for EZID DOI service ([Issue
  #2005](https://github.com/NCEAS/metacat/issues/2005))
* Docker changes:
  * Updated base image to `tomcat:9.0.96-jre17-temurin-jammy` (from `9.0.82`)
  * Run container as tomcat user & group, with uid 59997 and gid 59997

### Upgrades and Bug Fixes:
* Bump com.rabbitmq:amqp-client from 5.14.2 to 5.21.0
  ([Issue #1922](https://github.com/NCEAS/metacat/issues/1922))
* Add indexes to the access_log table ([Issue #1965](https://github.com/NCEAS/metacat/issues/1965))
* Optimize the query to get log record ([Issue #1976](https://github.com/NCEAS/metacat/issues/1976))


## Release Notes for helm chart 1.2.0

**Release date: 2024-12-18**

### New Features & Enhancements:
* Increase Metacat Application version to 3.1.0
* Increase MetacatUI subchart version to 1.0.2 (MetacatUI app version 2.31.0)
* Increase dataone-indexer subchart version to 1.1.1 (dataone-indexer app version 3.1.1)
  * Setup no longer requires an auth token in order to index private datasets
* DataONE Member Node setup simplified, and no longer requires admin access to the nginx controller;
  all [X-Proxy-Header setup now takes place in the current release
  namespace](https://github.com/NCEAS/metacat/pull/2027/files)
* Helm chart README updated with new setup for v3.1.0, and new [checklist added, for Metacat K8s
  Installation and Migration/Upgrade
  Steps](https://github.com/NCEAS/metacat/blob/main/helm/admin/MetacatQuickRef.md)
* Example PV and PVC creation files added in the [`helm/admin` directory](./helm/admin).
* Changes to UID & GID defaults:
  * Metacat `podSecurityContext` defaults updated to `runAsUser: 59997`,
    `runAsGroup: 59997` and `fsGroup: 59997`.
  * Dataone-indexer subchart `podSecurityContext` defaults updated to `fsGroup: 59997`.
  * Postgresql subchart `containerSecurityContext` defaults updated to `runAsUser: 59996`, and
    `podSecurityContext` defaults updated to `runAsNonRoot: true`
  * (NOTE you will need to chmod any existing `metacat` directory to `59997:59997`
    and chmod any existing `postgresql` directory to `59996:59996`)
* RabbitMQ subchart `consumer_timeout` set to `144000000`, to prevent the indexer from timing out
  when indexing large datasets
* Solr and Zookeeper subchart default Persistent Volume size increased to 100Gi.


## Release Notes for Helm Chart 1.1.1

Setting up a Token and Optional CA certificate for Indexer Access

**IMPORTANT:** In order for Metacat 3.0.0 to function correctly, the
dataone-indexer needs a valid authentication token, to enable
indexing for private datasets, via calls to metacat's DataONE API.

Release date: 2024-07-25

> Note the helm chart is a beta feature. It has been tested, and we believe it to be working well,
but it has not yet been used in production - so we recommend caution with this early release. If
you try it, [we'd love to hear your feedback](https://www.dataone.org/contact/)!

- Change version of `dataone-indexer` sub-chart from 1.0.1 to 1.0.2
- See [`dataone-indexer` release
  notes](https://github.com/DataONEorg/dataone-indexer/blob/main/RELEASE-NOTES.md) for details
- No changes to Metacat code - still uses `Metacat:3.0.0-no-mcui` docker image, which is identical
  to the Metacat 3.0.0 release

## Release Notes for Helm Chart 1.1.0

Release date: 2024-07-11

- The helm chart now includes an optional sub-chart for
  [MetacatUI](https://nceas.github.io/metacatui/). The MetacatUI sub-chart is highly configurable,
  and can be used with included themes, or you can provide your own custom theme, mounted on a PVC.
  More information can be found in the [MetacatUI
  README](https://github.com/NCEAS/metacatui/tree/develop/helm#readme).
- The docker image has been updated to `Metacat:3.0.0-no-mcui`, reducing its size by almost 0.5GB,
  since it now no longer includes metacatui files bundled in the same container as metacat. Note
  that the version of metacat itself has not changed - it is identical to the [3.0.0
  release](#release-notes-for-300)
- The dataone-indexer subchart has been updated to version 1.0.1, which includes the dataone-indexer
  app version 3.0.1 - see [indexer release
  notes](https://github.com/DataONEorg/dataone-indexer/blob/main/RELEASE-NOTES.md#dataone-indexer-version-301--helm-chart-version-101) for details
- `values.yaml` changes:
  - Some values have been moved to `.Values.global`, and a new one added:
    - `global.dataone-indexer.enabled` (moved from `dataone-indexer.enabled`)
    - `global.includeMetacatUi` (moved from `metacat.includeMetacatUi`)
    - `global.metacatExternalBaseUrl`
    - `global.d1ClientCnUrl` (moved from `metacat.D1Client.CN_URL`)
  - `ingress.rewriteRules` allows for arbitrary nginx rewrite rules to be defined
  - Improvements to `ingress.hosts` and `ingress.tls`, allowing optional auto-population of hostname

## Release Notes for 3.0.0

Release date: 2024-04-30

### Note: Your Metacat instance MUST be either 2.19.0 or 2.19.1 before you can upgrade it to 3.0.0.

### Overview of Major Changes

This major release introduces breaking changes:
- A valid admin (auth) token is required for a Metacat 3.0.0 installation to function correctly (i.e. to handle private datasets).
  - Please [contact DataONE](https://www.dataone.org/contact/) to obtain a long-term token (valid for 1 year)
    - If you are already part of the DataONE network and have a member node, we will issue you a token linked to your DataONE Node identity.
    - If you're not a DataONE member node, we [encourage you to join](https://www.dataone.org/jointhenetwork/) (it's free!) so that your data can partake in DataONE's goal of the preservation of scientific data for future use.
    - Otherwise, we can issue a token linked to your Metacat administrator's ORCID iD.
  - This is only an interim requirement; a future release of Metacat will remove the need for this token.
  - If you are checking out Metacat for the first time, you can obtain a temporary token to continue evaluating Metacat
    by logging into [the KNB website](https://knb.ecoinformatics.org), and navigating to
    "My Profile" -> "Settings" -> "Authentication Token".
    - Note - this token is only valid for 24 hours, but you can continue manually retrieving short-term tokens
- The original Metacat API is no longer supported.
  - `Morpho` and any other clients depending on this API will no longer work
  - Client access is available only via the DataONE API.
  - LTER and OAI-PMH harvesters have been removed. Please see the Metacat administrator's guide for details.
- `Skin-based deployments` are no longer supported.
  - If you wish to upgrade to Metacat 3.0.0, you can use `metacatui` which is shipped with Metacat, create your own
    frontend against the API, or use a standalone version of [metacatui](https://nceas.github.io/metacatui/).
- Metacat admin authentication is no longer supported via LDAP or Password-based logins.
  - An ORCID iD is now required in order to log in as a Metacat administrator.
  - Please [sign up for an ORCID iD](https://orcid.org/register) if you do not already have one.

Also in this release:
- You can now deploy Metacat on Kubernetes, using a Helm chart. Note this is a beta feature. It has
  been tested, and we believe it to be working well, but it has not yet been used in production - so
  we recommend caution with this early release. If you try it, [we'd love to hear your
  feedback](https://www.dataone.org/contact/)!

### Upgrade Notes (2.19.0 to 3.0.0 or above):

- Starting Requirements:
  - Your existing Metacat installation must already have been successfully upgraded to [version v2.19.0](https://github.com/NCEAS/metacat/releases/tag/2.19.0) before you can begin upgrading to version 3.0.0 or above.
    - If not, please upgrade to v2.19.0 first, before proceeding.
  - You must have Java 17 installed
    - If it is not installed, please install it and set it as the default version
      - ex. `sudo update-alternatives --config java` which will bring up a list of versions to select from
      - If Tomcat uses the `default-java` directory, ensure that it points to Java 17
          ```sh
          cd /usr/lib/jvm
          sudo rm -r default-java
          sudo ln -s java-17-openjdk-amd64 default-java
          ```
  - If Metacat is currently running:
    - Stop Tomcat - ex. `sudo systemctl stop tomcat9`
    - Stop solr - ex. `sudo systemctl stop solr`
- Download/upgrade your solr version to 9.5.0
  > NOTE: Solr upgrade from older cores is not supported. Instead, you must choose a new value for
  > `solr-home` in the property configuration page, pointing to a new, empty directory. Metacat will
  > then create the new solr core without any data. Finally, you will need to reindex all objects
  > ***after the upgrade process is done***, due to incompatibility between old data and the new Solr
  > schema. ([Instructions below](#you-are-now-ready-to-install-metacat-300).)
  - Ensure that `/etc/default/solr.in.sh` is group writable
    - ex. `sudo chmod g+w /etc/default/solr.in.sh`
  - In `solr.in.sh`, be sure to delete the old solr home add a new solr path:
    - `SOLR_OPTS="$SOLR_OPTS -Dsolr.allowPaths=/var/metacat"`
  - Optionally, add/adjust memory settings to `SOLR_JAVA_MEM="-Xms2g -Xmx2g"`
- Start/restart solr
  - ex. `sudo systemctl restart solr`
- Install RabbitMQ if you do not already have it running
   ```sh
   sudo apt install rabbitmq-server
   sudo systemctl restart rabbitmq-server
   ```

#### You are now ready to install Metacat 3.0.0!
Additional notes:
- `metacat.properties` no longer contains custom settings, and should not be edited.
  - Please first re-configure Metacat through the Metacat Admin UI after upgrading.
  - If you have custom properties that are not available for configuration in the Metacat Admin UI, these can be added to `metacat-site.properties`.
- The database upgrade process may require several minutes or longer to complete.
- **Solr Re-Index:** as mentioned above, Solr upgrade of old cores is not supported. You should have a new, empty
  solr core available, and can now reindex all objects:
  - first, a metacat administrator should obtain a temporary auth token by [logging into the KNB
    website with their ORCID](https://knb.ecoinformatics.org), and navigating to "My Profile" ->
    "Settings" -> "Authentication Token".
    > Note - each token is only valid for 24 hours!
  - then issue the command to reindex all:
    ```sh
    $ curl -X PUT -H "Authorization: Bearer $TOKEN" https://<your-host>/<your-context>/d1/mn/v2/index?all=true
    # where $TOKEN is an environment variable containing your auth token (see overview of major changes above).
    #
    # example:
    # curl -X PUT -H "Authorization: Bearer $TOKEN" https://knb.ecoinformatics.org/knb/d1/mn/v2/index?all=true
    ```


### New Features & Enhancements:

- DataONE API actions (create, update, delete) are now transactions (fully succeeds or rolls back) [I-1642](https://github.com/NCEAS/metacat/issues/1642)
- Metacat Admin Login with ORCID authentication [I-1694](https://github.com/NCEAS/metacat/issues/1694)
  - Operators must use an ORCID as an admin identity for Metacat authorization
  - LDAP and Password-based login is no longer supported
- Metacat admin users now have member node admin privileges [I-1816](https://github.com/NCEAS/metacat/issues/1816)
- Storage and Indexing Enhancements [PR-1695](https://github.com/NCEAS/metacat/pull/1695)
  - Revised use of port numbers to determine https vs. http [I-1697](https://github.com/NCEAS/metacat/issues/1697)
  - Re-implemented mechanism to handle index tasks that failed to be put into RabbitMQ [I-1603](https://github.com/NCEAS/metacat/issues/1603)
  - Increased indexing speeds by replacing hazelcast with dataone-indexer
  - Removed hazelcast dependency and added new SystemMetadataManager class to handle system metadata
  - Removed unnecessary monitor/status request endpoint [I-1609](https://github.com/NCEAS/metacat/issues/1609)
  - Moved reindexing action from old MetacatAPI to new DataONE MN.admin API [PR-1738](https://github.com/NCEAS/metacat/pull/1738)
- Upgrade from Java 8 to 17 [I-1481](https://github.com/NCEAS/metacat/issues/1481), [PR-1731](https://github.com/NCEAS/metacat/pull/1731), [PR-1735](https://github.com/NCEAS/metacat/pull/1735)
- Disabled old MetacatAPI and significant code clean-up (removed obsolete legacy code) [PR-1713](https://github.com/NCEAS/metacat/pull/1713), [PR-1725](https://github.com/NCEAS/metacat/pull/1725), [PR-1726](https://github.com/NCEAS/metacat/pull/1726), [PR-1744](https://github.com/NCEAS/metacat/pull/1744)
  - Removed obsolete skin configurations [PR-1767](https://github.com/NCEAS/metacat/pull/1767)
- MNAdmin API Enhancements
  - Added 'reindex' and 'reindexall' methods [I-1716](https://github.com/NCEAS/metacat/issues/1716)
  - Added 'updateIdMetadata' method [I-1766](https://github.com/NCEAS/metacat/issues/1766)
- Metacat Containerization MVP (Kubernetes, Helm, Docker) [I-1623](https://github.com/NCEAS/metacat/issues/1623)
  - Metacat deployment in a Kubernetes cluster [I-1218](https://github.com/NCEAS/metacat/issues/1218)
    - Allows helm to install or upgrade a Metacat Docker image.
  - Bundled MetacatUI installation is now optional [I-1784](https://github.com/NCEAS/metacat/issues/1784)
  - Added OCI annotations [PR-1747](https://github.com/NCEAS/metacat/pull/1747)
  - Added dataone-indexer subchart & dockerfile labels, removed Hazelcast and fixed liveness & readiness probes [PR-1714](https://github.com/NCEAS/metacat/pull/1714)
    - Removed built-in indexer and related code [PR-1719](https://github.com/NCEAS/metacat/pull/1719), [PR-1720](https://github.com/NCEAS/metacat/pull/1720)
    - Moved solr authentication settings to indexer [PR-1724](https://github.com/NCEAS/metacat/pull/1724)
  - Added new K8SAdminInitializer class to streamline startup process, including auto db updates [PR-1691](https://github.com/NCEAS/metacat/pull/1691)
  - DataONE Member Node Configuration support when running in k8s [I-1662](https://github.com/NCEAS/metacat/issues/1662), [PR-1681](https://github.com/NCEAS/metacat/pull/1681), [PR-1696](https://github.com/NCEAS/metacat/pull/1696)
  - Improved clarity around administrative user set up [PR-1646](https://github.com/NCEAS/metacat/pull/1645)
  - Added postgres subchart [PR-1653](https://github.com/NCEAS/metacat/pull/1653), [PR-1657](https://github.com/NCEAS/metacat/pull/1657)
  - Added ingress, templatized configuration values (values.yaml) and removed hardcoded values [PR-1647](https://github.com/NCEAS/metacat/pull/1647), [PR-1746](https://github.com/NCEAS/metacat/pull/1746)
    - Enabled HTTPS access (x509 certificates) [PR-1672](https://github.com/NCEAS/metacat/pull/1672), [PR-1677](https://github.com/NCEAS/metacat/pull/1677), [PR-1676](https://github.com/NCEAS/metacat/pull/1676)
      - DataONE Replication is now functional
  - Enabled passing of secret credentials to Metacat via environment variables [I-1635](https://github.com/NCEAS/metacat/issues/1635)
    - Added new secret mapping to use for X-Proxy-Key header [PR-1670](https://github.com/NCEAS/metacat/pull/1670)
  - Prevent startup from aborting within a container if site properties is read-only [PR-1646](https://github.com/NCEAS/metacat/pull/1646)
  - Update tomcat version and added memory options [PR-1675](https://github.com/NCEAS/metacat/pull/1675)
  - Adjusted catalina property settings to resolve issues related to encoded slash and backslashes [I-1674](https://github.com/NCEAS/metacat/pull/1674)
  - Added to default command line tools, fixes for log path issues & tailing, and optimizations to dev environment [PR-1648](https://github.com/NCEAS/metacat/pull/1648)
  - Improvements to test suite and logging exceptions [PR-1663](https://github.com/NCEAS/metacat/pull/1663), [PR-1687](https://github.com/NCEAS/metacat/pull/1687), [PR-1688](https://github.com/NCEAS/metacat/pull/1688), [PR-1737](https://github.com/NCEAS/metacat/pull/1737), [PR-1762](https://github.com/NCEAS/metacat/pull/1762),  [PR-1763](https://github.com/NCEAS/metacat/pull/1763), [PR-1770](https://github.com/NCEAS/metacat/pull/1770), [PR-1776](https://github.com/NCEAS/metacat/pull/1776), [I-1796](https://github.com/NCEAS/metacat/issues/1796)
- Metacat Configuration and Startup Enhancements
  - Revised property setting for dataone-indexer url so Metacat-index now works out of box [I-1828](https://github.com/NCEAS/metacat/issues/1828)
    - PropertiesAdmin now attempts to determine where the indexer is hosted [PR-1834](https://github.com/NCEAS/metacat/pull/1834)
  - Added a new field to specify public certificates to be used by the d1_portal to verify requests [I-1812](https://github.com/NCEAS/metacat/issues/1812)
  - Improved user experience by highlighting new properties [I-1804](https://github.com/NCEAS/metacat/issues/1804)
  - Added new 'MetacatInitializer' class to verify that Metacat has the essential components for start up [I-1721](https://github.com/NCEAS/metacat/issues/1721)
  - Added solr startup configuration checks (configured, not configured) [PR-1752](https://github.com/NCEAS/metacat/pull/1752), [PR-1656](https://github.com/NCEAS/metacat/pull/1656)
  - Improved Metacat startup process by forcing operators to configure settings when it is necessary [PR-1644](https://github.com/NCEAS/metacat/pull/1644/files)
  - Added default database properties for postgres [PR-1641](https://github.com/NCEAS/metacat/pull/1641)
- Metacat startup now aborts if it is misconfigured and/or critical resources are inaccessible [I-1631](https://github.com/NCEAS/metacat/issues/1631)
  - The related issues can be found in the metacat logs (ex. '/tomcat/logs/catalina.out') and host logs (ex. '/tomcat/logs/hostname(data).log') with messaging to assist with debugging.
- Metacat Properties Overlay [I-1611](https://github.com/NCEAS/metacat/issues/1611)
  - Properties saved in the Metacat admin configuration pages are now saved in 'metacat-site.properties'.
- Removed obsolete Metacat replication (replaced by DataONE replication mechanism) [I-1620](https://github.com/NCEAS/metacat/issues/1620)
- Added new property to control enabling or disabling of the postgres event log [I-1769](https://github.com/NCEAS/metacat/issues/1769)
- Support recent versions of python3 and sphinx7 for admindoc builds [PR-1728](https://github.com/NCEAS/metacat/pull/1728)
- Upgraded to speedbagit 1.0.4 to properly handle duplicate filenames [I-1443](https://github.com/NCEAS/metacat/pull/1614)
- Streamlined ant build process to reduce build time [I-1612](https://github.com/NCEAS/metacat/issues/1612)
- Improved error messaging when trying to submit data as a denied submitter [I-1573](https://github.com/NCEAS/metacat/issues/1573)
- Improved test suite execution speeds [PR-1707](https://github.com/NCEAS/metacat/pull/1707)
- Fixed test targets in build.xml to assist with tests running out of the box [I-1260](https://github.com/NCEAS/metacat/issues/1260)

### Bug Fixes:

- Fixed solr 9.5.0 solrconfig.xml compatibility issue [I-1822](https://github.com/NCEAS/metacat/issues/1822)
- Resolved issue with submitting a dataset with Chrome version 120.0.6099.71 where a chrome bug prevented data objects from being saved [I-2235](https://github.com/NCEAS/metacatui/issues/2235#issuecomment-1865232012)
- Fixed potential infinite loop created by cyclic create system metadata calls [I-1779](https://github.com/NCEAS/metacat/issues/1779)
- Internalized Metacat exception that has no bearing on a request to save a dataset with a resource map created outside of MetacatUI [I-1781](https://github.com/NCEAS/metacat/issues/1781)
- Fixed orphaned system metadata issue due to Chrome 120 bug [PR-1764](https://github.com/NCEAS/metacat/pull/1764)
- Removed references to redundant HarvesterRegistration which threw misleading exceptions [PR-1742](https://github.com/NCEAS/metacat/pull/1742)
- Resolved 'sphinx' auto build docs from breaking due to conflicting indentation [PR-1661](https://github.com/NCEAS/metacat/pull/1661)
- Resolved EZID related tests breaking due to a new AWS-WAF Rule that denies requests if the body contains a url that includes 'localhost' or '127.0.0.1' by adding mock properties [I-1625](https://github.com/NCEAS/metacat/issues/1625)
- Fixed sampling citations not showing up in the view service [I-31](https://github.com/NCEAS/metacat/issues/31)

## Release Notes for 2.19.1

New features, and bugs fixed in this release:
* Handle additional OSTI DOIs
* Upgrade Libraries

## Release Notes for 2.19.0

New features and bugs fixed in this release:

* getPackage implementation doesn't handle duplicate data object filenames
* PackageDownloaderV2 doesn't increment filenames for duplicate science metadata objects
* Deleting objects failed to remove solr doc
* Sampling citation not showing up in view service
* Mis-Formatting of Data Package Contents
* Unhelpful error message when trying to create as a denied submitter
* Data objects missing after a package was published
* Multiple updates on a single DOI happen when users use the metacat admin page to update DOIs
* Metacat updated the DOI metadata (datacite) when the system metadata of an obsoleted object was updated if the obsolescent chain has a DOI Sid
* getPackage fails to include system metadata
* OSTI DOI Plugin Notifications need more information

## Release Notes for 2.18.0

This release focuses on improving indexing performance.
New features and bugs fixed in this release:
* Use a multiple-tread process to index objects
* Eliminate unnecessary index tasks
* Fix bugs in Bagit implementation

## Release Notes for 2.17.0

New features and bugs fixed in this release:
* Support the OSTI DOI service
* Support hierarchical packages in getPackage API
* Use the file protocol rather than the https/http protocols to access schema files during the xml validation process
* Metacat threw the error of the user not authorized during the publish process even though the EZID setting is correct
* Metacat did not check if the checksum algorithm was changed in the MN.updateSystemmetadata method
* EML to HTML .xslt skips methods section in attributes
* Upgrade xerces to 2.12.2
* Remove the GeoServer war file
* Remove the oa4mp-client-oauth1-3.3.war file

## Release Notes for 2.16.2

Bugs fixed in this release:
* Upgrade some library jar files to fix severe security vulnerabilities

## Release Notes for 2.16.1
Bugs fixed in this release:
* Upgrade some library jar files to fix severe security vulnerabilities
* Metacat cannot create objects without DOI setting enabled

## Release Notes for 2.16.0
New features and bugs fixed in this release:
* Upgrade some library jar files to fix critical security vulnerabilities
* Refactor the DOI service to use the plug-in architecture
* CN subjects cannot query private objects
* Users with the write permission cannot update system metadata
* Metacat should return a not-found error rather than the internal error when there is a typo in the old Metacat API url

## Release Notes for 2.15.1
New features and bugs fixed in this release:
* Metacat hangs with excessive thread counts
* Support multiple DOI shoulders
* Update EML Semantic Annotation indexing to include and expand property URIs
* MNodeService.getPackage() takes too long for large packages
* Geohashes, text fields not being indexed for schema.org documents
* Expand elements covered by EML's attribute index fields beyond just dataTable
* EML to HTML/PDF is broken in the getPackage() method
* Have MNCore.getCapabilities() report on auth.allowSubmitters parameter setting
* GetPackage API doesn't work from R on Windows
* Add new indexes for column archvied and object_format in the systemmetadata table

## Release Notes for 2.15.0
New features and bugs fixed in this release:
* Support non-XML metadata objects in Metacat
* Upgrade Solr to 8.8.2
* Enforce the node replication policy in the MN.replicate method
* Fix the bug of incorrect geohash
* Fix the bug of incorrect collectionQuery
* Change the default order to dateModified for the listObject method
* Remove extra logged event in the MN.update method
* Upgrade some library jar files to fix security threats

## Release Notes for 2.14.1
New features and bugs fixed in this release:
* Support new XML schemas for collections-1.1.0 and portals-1.1.0
* Metacat is creating too many Timer objects which leads to out of memory issues
  and excessive numbers of threads under high request loads
* Users with only write permission can change the access policy on update() requests,
  when this operation is reserved only for users with changePermission permission
* Close OutputStream objects after fulfilling DataONE API requests
* Fix how exclude filters are translated in the collectionQuery
* Update Apache setup docs to match current practices
* Update documentation build to Python3
* Fix an issue where a client editor sees a "Nothing was found" error despite
  having all permissions
* Project abstract displays oddly
* View service rendering EML project abstract incorrectly
* Add Methods section heading to metadata view

## Release Notes for 2.14.0
New features and bugs fixed in this release:
* Support the quota service for portals
* Upgrade the namespace of the datacite schema to 4.3
* Upgrade the version of log4j from 1.2 to 2.11
* Upgrade some library jar files to fix security threats

## Release Notes for 2.13.0
New features in this release:
* Upgrade Solr to version 8 and use the external Solr server
* Use the streaming HTTP multipart handler to improve performance
Notice
* Metacat Servlet APIs will be deprecated in the near future and its client software, Morpho, will not work with future versions of Metacat.
* Solr dynamic fields such as read_count_i will be deprecated.

## Release Notes for 2.12.4
Bugs fixed in this release:
* Remove an unnecessary expensive query to synchronize the systemmetadata tables among DataONE coordinating nodes.

## Release Notes for 2.12.3
New features and bugs fixed in this release:
* Return an invalid response to the call of cn.getLogRecords if some log records have a null ip address
* Add the indication of completing the database upgrade process during configuration

## Release Notes for 2.12.2
Bugs fixed in this release:
* Modify the schema files of the format ids of portal and collections

## Release Notes for 2.12.1:
Bugs fixed in this release:
* Patch a security bug

## Release Notes for 2.12.0:
New features and bugs fixed in this release:
* Support a new format id - portal, which will be used on the MetacatUI portal service
* Change how Metacat generates sitemaps. Sitemaps will now be generated automatically when Metacat starts, if enabled
* Add a mechanism that operators can pass paramters to the stylesheets of the view service
* Upgrade the version of the Xerces library to fix the issue of statck overflow when users upload XML objects having long text fields

## Release Notes for 2.11.1:
Bugs fixed in this release:
* Metacat can't index the award tile element in an EML 2.2.0 object

## Release Notes for 2.11.0:
New features in this release:
* Support the new metadata standard - EML 2.2.0

## Release Notes for 2.10.4:
Bugs fixed in this release:
* Publishing dataset with provenance loses all provenance relationships
* Publish call fails after adding provenance information

## Release Notes for 2.10.3:
New features and bugs fixed in this release:
* Update DOI registrations for EML objects with complete metadata
* Can't update DOI metadata on the Metacat admin page if the doi is an SID
* Make Solr field "replicationAllowed" to be searchable
* Add an Metacat admin method to clear cached the style sheets
* Add a DataONE method to display the size of the index queue
* Not check if the new index pathes were added during the Metacat startup

## Release Notes for 2.10.2:
New features and bugs fixed in this release:
* Allow POSTs to the query service
* Make ant build auto-detect maven location
* Upgrade the version of MetacatUI to 2.5.1
* Indirect error messages (CONCURRENT_MAP_PUT) on malformed sysmeta update
* Taxonomic rank fields in Solr index are case-sensitive

## Release Notes for 2.10.1:
New features and bugs fixed in this release:
* Optimize the DataONE authorization process
* Allow the node subject to read all Solr records
* Improve the slow performance of the view service
* Index the replicationStatus field
* Failed to publish a private object
* Disable the index of the event counts by default


## Release Notes for 2.10.0:
After installing this release, you need to issue the "reindexall" command since a new SOLR field has been added.
New features and bugs fixed in this release:
* Exclude EcoGrid on Metacat
* Do not allow restrictive access control change to content with a DOI
* MN/CN.updateSystemMetadata doesn't check the field - authoritativeMemberNode
* Integrate the fixed SeriesIdResolver class which gets SystemMetadata locally
* EZID metadata registration doesn't seem to work with SIDs
* Metacat white space pid filter overlooks some cases
* Update DOI registrations for EML objects with complete metadata
* Keep archived object solr index but they don't show up at the default search
* D1NodeService read api methods / getPidForSid bypassing Hz lookup
* Enable a blacklist configuration for IP addresses and subjects
* Replace the statement of "System.out.print" by the Log statement on D1URLFilter and D1HttpRequest classes
* Publish DOI includes old metadata PID in new resource map
* Support format id - http://www.openarchives.org/OAI/2.0/oai_dc/
* EML Attribute XSLT re-using anchors in such as way that they're not unique / changing attribute tabs doesn't for some entities
* Switch ORCID icons in EML XSLTs to be inlined instead of reference to MetacatUI


## Release Notes for 2.9.0:
New features and bugs fixed in this release:
* Specify internal calls to Metacat on localhost or internal domain name
* Upgrade to the 2.0 API to access the ORCID service
* Remove the duplicated authorization code on D1NodeService.create and CNodeService.create method
* XMLSchemaService progressively builds massively long string by calling doRefresh
* Simplify the configuration of the metacat.properties file when users run junit tests
* Run MDQ suite on insert/update
* MN.updateSystemmetadata shouldn't call CN.synchronize if the synchronize is off on the MN
* Metacat is double-decoding incoming urls on the CNs
* Upgrade the version of MetacatUI to 1.14.15
* Fixed improperly formatted Alternate Data Access URLs
* Fixed a bug causing ISO19115 documents to render with empty Attributes tables
* Support the formatID : http://datacite.org/schema/kernel-3.0 and http://datacite.org/schema/kernel-3.1


## Release Notes for 2.8.7:
Bugs fixed in this release:
* Allowed submitters list fails with ORCIDs
* Allowed submitters list fails with groups
* The property auth.allowedSubmitter doesn't work with submitting data objects on both DataONE and Metacat APIs


## Release Notes for 2.8.6:
New features and bugs fixed in this release:
* D1ResourceHandler.serializeException should not log all exceptions returned as errors.
* Invalidly formatted dates in listObjects fromDate / toDate parameters do not result in InvalidRequest
* Support the formatID : http://www.isotc211.org/2005/gmd-pangaea
* Metacat index error message doesn't have the pid information
* metacat.EventLog.getReport should downgrade a log level
* /object/id command get a null exception if there the object format is not found
* Remove the duplicated the log4j file in Metacat
* reCAPTCHA v1 will be turned off on March 31, 2018
* Metacat sets a wrong default value for the deploy path during the totally fresh installation
* Change the behavior to archive the older version of the resource map object when a new package is pushed to Metacat
* Remove redundant alternate identifier form in register-dataset.cgi template


## Release Notes for 2.8.5:
New bugs fixed in this release:
*	Some DataONE service packages not being reported
* Support for XSL transform of iso19115 metadata format


## Release Notes for 2.8.4:
New bugs fixed in this release:
* LDAP-based group authorization is failing
* Display the dc:source field from DCX metadata documents in the stylesheet
* MN.Delete should continue even though it can't find the local file


## Release Notes for 2.8.3:
New bugs fixed in this release:
* MNodeService.replicate() is failing
* MN.publish method fails because of the mismatched checksum
* MN.update method doesn't check the checksum
* Set file names correctly when reading objects from Metacat
* MN.publish method can't generate a new ore document when the metadata is published


## Release Notes for 2.8.2:
New bugs fixed in this release:
* The listObjects method fails if there is pid with a white space in the list
* EML SAX parser will not check if the user has the all permission on data objects (described by the eml object) when the parser is called by DataONE API
* The count number is -1 when the expandRightsHolder method lists the subjects
* CN V1/V2.archive implementation only allows CN and MN administrators to act
* Metacat-index RDF/XML subprocessor not populating prov_hasDerivations field


## Release Notes for 2.8.1:
New features and bugs fixed in this release:
* Register schema files for the format id FGDC-STD-100.1-1999
* ReIndexing a document lost some prov information in the new generated solr doc
* MNodeService.updateSystemMetadata() needs to validate obsolescence chain pids
* Metacat DataONE base url always gets 404 not found page
* Use the provenance Spring bean file from d1_cn_index_processor in Metacat
* Calling cn.synchronize method asynchronously in mn.updatesystemmeta on Metacat
* Uploading a resource map with provenance data causes an NPE during indexing
* replicationPolicy missing numberReplicas and replicationAllowed attributes
* Archive operation not permitted for V1 readonly MNs
* Solr index still keeps the obsoletedBy field even though it was removed from the system metadata and reindexed
* Private metadata is indexed as isPublic=true
* Action reindexall should be asynchronized


## Release Notes for 2.8.0:
New features and bugs fixed in this release:
* Modify Metacat code according to the change of dataone library migrating from JibX to JAXB
* Upgrade Solr to 3.6.2 to fix searching issues
* Metacat is not expanding groups in the rightsHolder field during authorization
* Improve error message for: User tried to update an access module when they dont have ‘ALL’ permission!"


## Release Notes for 2.7.2:
New features and bugs fixed in this release:
* Legacy Metacat "shortcut" url does not work if revision is omitted
* Migrate metacat build to use EML git repo
* All links on the registry page should open up in a new tab
* Metadata/data objects which have obsoletedBy field ignore the resource map index
* Metacat creates an Invalid Content-Disposition value for some filenames
* External links in the registry should open in new tab
* Remove the support of Oracle on documentation
* Changing a Metacat member node's synchronization value on the d1 admin page doesn't work
* Add the feature to support the noNamespaceSchemaLocation attribute in xml objects
* Provide clear messages to clients if the namespaces/formatids of the schemas of xml objects are not registered in Metacat
* Disable the feature of downloading external schemas for unregistered namespaces
* Metacat-index picks up the changes in log4j.properteis without restarting tomcat

## Release Notes for 2.7.1:
* Fixed several bugs on the data registry form
* Support the onedcx schema
  - Fixed several onedcx stylesheet issues

## Release Notes for 2.7.0:
* Use different format ids to identity variants of the schema with same namespace
* Add EML 2.1.1 to Darwin Core supporting for OAI-PMH provider
* Bugs fixed include:
  - Series head resolution should use obsoletes field as part of determination(7020)
  - The InputStream (parameter) in the CN/MN.create and MN.update method is not closed(7005)
  - Use the AutoCloseClientInputStream class in the d1_libclient_java in Metacat replication(7004)

## Release Notes for 2.6.0:
This release supports token-based authentication in the data registry
as well as:
* MetacatUI 1.11.0

## Release Notes for 2.5.1:
This a minor release that improves:
* Authentication token handling for the DataONE API

## Release Notes for 2.5.0:
This major release adds DataONE V2 API support
as well as:
* MetacatUI 1.8.1
* Data registry bug fixes

## Release Notes for 2.4.3:
This release includes:
* Support for large file uploads
* Recording User-Agent when logging requests

## Release Notes for 2.4.2:
This release includes:
* Updates to improve character encoding handling
* Updated SOLR index and UI to use geohashes for map rendering
* Note: this is the last release of Metacat that will support Java 6

## Release Notes for 2.4.1:
This is a patch release that addresses the following:
* Allows LDAP authentication using aliased referrals
* Re-indexes previous document revisions in SOLR when
new revisions are added on update
* Allows authentication in SANParks skin
* Enables OpenLayer.js mapping plug-in over https

## Release Notes for 2.4.0:
These release adds a new default file-based authentication mechanism
that allows administrators to more easily deploy Metacat without a
LDAP server.
Other enhancements include:
* Correct use of DataONE 'archive' flag
* Access policy synchronization with the DataONE Coordinating Node
* Indexing support for EML singleDateTime coverage values
* Indexing support document read/download events
* Improved package download structure and file naming conventions

## Release Notes for 2.3.1:
This is a minor patch release to fix an issue that arises when
Metacat is operating as a DataONE Coordinating Node.
* See: https://projects.ecoinformatics.org/ecoinfo/issues/6315
* Updated MetacatUI with support for spatial query criteria
* Updated account/identity management script

## Release Notes for 2.3.0:
This release adds solr indexing features to metacat-index
for querying and sorting by authors and taxonomic coverage.
Also includes:
* Control over the log level from the SOLR libraries using Metacat's
log4j file.
* access_log DB indexes for better log reporting performance
via the DataONE API
* Provide 'reindexall' action instead of 'reindex' without a 'pid' parameter.


## Release Notes for 2.2.1:
This is a critical patch release of Metacat that includes addresses
a bug in file stream handling. It is recommended for all Metacat deployments.

## Release Notes for 2.2.0:
This is a major release of Metacat that includes a new customizable UI and
improved LDAP account management features

## Release Notes for 2.1.1:
This is a minor patch release of Metacat that addresses
a bug in DOI publishing.

## Release Notes for 2.1.0:
This is a major release of Metacat that includes a SOLR-based search feature
* Optional SOLR search index
* Client certificate delegation (using a service provider like CILogon)

## Release Notes for 2.0.8:
This is a patch release for Metacat
* Enforce DataONE prohibition on whitespace in identifiers
* Use unique filenames for Registry data uploads

## Release Notes for 2.0.7:
This is a patch release for Metacat replication
* Replication SQL performance enhancement
* Comply with DataONE schema for SystemMetadata.submitter

## Release Notes for 2.0.6:
This patch release focuses on Metacat support for DataONE-enabled Morpho clients
* MN.generateIdentifier() support for UUID and DOI
* DOI registration support using EZID service
* Pathquery support for specifying multiple document owners


## Release Notes for 2.0.5:
This patch release focuses on Metacat support for Oracle
* SystemMetadata table names have been shortened to comply with Oracle limits.
* SANParks/SAEON spatial zip file download fixed
* Pathquery performance fix
* Hazelcast 2.x upgrade
* Correct handling for deleted and archived objects for both Metacat and DataONE APIs

## Release Notes for 2.0.4:
This patch release focuses on Metacat-Metacat replication.
* Allows Metacat to stream replication update information between servers to avoid connection timeout issues
* Allows Metacat replication when source host does not provide SystemMetadata (DataONE)
* Fixes an access control issue that could allow search results to include protected documents

## Release Notes for 2.0.3:
This is another critical patch release of Metacat 2.0.
* Addresses a bug that prevented Metacat replication from completing (timeout error)
* Uses more efficient SystemMetadata synchronization using shared Hazelcast map

## Release Notes for 2.0.2:
This is a critical patch release of Metacat 2.0.
* Addresses a bug that prevented updates to DataONE identified datapackages (e.g., using DOIs)
* Addresses a bug that prevented the use of the Metacat API 'getaccesscontrol' action
* Updates the Foresite ORE library to correctly serialize resource map documents

## Release Notes for 2.0.1:
This is a minor patch release of Metacat 2.0. Please see the previous release notes for complete information.
* Addresses an identifier issue during ORE generation for DataONE services
* Increases compatibility with older EML content and the Xalan XSLT processor
* Removes XSLT 2.0 support (Saxon)

## Release Notes for 2.0.0:
This major release includes support for DataONE.
* The DataONE v1.0.0 Member Node service APIs are now the preferred method for communicating with Metacat
* The existing EcoGrid and Metacat Servlet APIs are deprecated but still available.
* EML-embedded access control rules using permOrder="denyFirst" are no longer supported (https://redmine.dataone.org/issues/2614)
* Replication now utilizes client certificates to establish SSL connections between replication source and target servers
* Access control rules now apply to objects on a per-revision basis rather than per-docid
* Bugs fixed include:
    - SANParks skin/TPC (5561, 5530, 5533, 5542, 5543, 5544, 5551, 5563, 5566, 5567, 5569, 5353)
    - Registry (5114, 5244)
    - Replication (4907, 5537, 5536, 5534, 5520, 5519, 3296)
    - Spatial cache regeneration can be skipped during reconfiguration/redeployment/upgrade (3811)
    - Pathquery support for temporal search criteria (2084)
    - Character encoding of XML now respected (internationalization) (2495, 4083, 3815)


## Release Notes for 1.9.5:
This release fixes those bugs:
* Metacat could not download the included schemas in an external schema file during inserting.
* Workflow scheduler could not work since the metadata standard of kepler kar file was changed.
* Earthgrid could not transform the Earthgrid query with concept "/" to the Metacat path query correctly.
* Document access rules are now preserved when documents are archived during the 'delete' action.

## Release Notes for 1.9.4:
This release fixes bugs in the FGDC data package upload and download interface utilized
by the SANParks and SAEON skins.

## Release Notes for 1.9.3:
This release fixes a harvester bug that prevented the EML harvester from processing
harvest lists.
- The Kepler skin has been updated to support KarXML 2.1.
- Ecogrid query services now support multiple namespace searches.

## Release Notes for 1.9.2:
This release primarily holds the addition of the Threshold of Potential
Concern workflow functionality (TPC).  The following issues were addressed:

* Create a properties file for use by JUnit Tests (Bug 2994)
* security issue with skins (Bug 3368)
* reorganize classes into a more functional specific structure (Bug 3510)
* Add admin names as dropdown in configuration login (Bug 3729)
* KNB metacat replication error log file is empty (Bug 3885)
* Create TPC Report web browse/search pages (Bug 4165)
* Create archive extraction functionality (Bug 4166)
* Create Workflow Scheduler (Bug 4167)
* metacat didn't update xml_path_index table while a document was updated (Bug 4367)
* Enforce permissions for tpc workflow viewing and scheduling (Bug 4420)
* Fix cross platform TPC GUI issues (Bug 4556)
* TPC Sanparks page content lenth issue (Bug 4557)
* Create validate and isAuthorized unit tests (Bug 4558)
* Cannot insert replication server via gui (Bug 4594)
* Timed Replication takes many hours and drives the load up on KNB (Bug 4616)
* [ESA] Update fails when document rev number is missing (Bug 4627)
* fails to catch some insert and update failures (Bug 4637)
* must support java 1.6 (Bug 4641)
* Convert build to pull eml from svn instead of cvs (Bug 4644)
* handleGetRevisionAndDocTypeAction should search both xml_documents and xml_revisions table (Bug 4645)
* Metacat couldn't update a document from client, which previous versions only stay in xml_revisions table (Bug 4649)
* metacat runs out of memory (Bug 4658)
* ESA skin links are hard coded to data.esa.org (Bug 4698)
* Update release 1.9.2 release notes (Bug 4707)
* Handle writing a text node > 4000 characters to the db. (Bug 4708)
* Metacat should run against Tomcat 6 (Bug 4716)

## Release Notes for 1.9.1:
The 1.9.1 release holds the bug fixes found after releasing 1.9.0 beta.
These bugs were primarily replication issues.  There is no difference
in functionality between 1.9.0 and 1.9.1

## Release Notes for 1.9.0:
This release focuses on simplifying the Metacat installation process by
creating a binary (war) installation.  The need to build the application
on the server has been removed (although the option is still available).
In order to facilitate this method of installation, a few major modifications
were made to the code:

- Ant token replacement was removed for all non-build variables in the application (most of this was already done in 1.8.1).
- The Metacat properties confguration was moved into the application itself.
- Database schema version detection and install/upgrade utilities were added to the application.

Also, this release includes several enhancements:
- it supports the new EML 2.1.0 version.
- Documents are now stored on the local filesystem as well as in the database in order to preserve document integrity.
- Metacat verifies new schemas when they are added.
- Additional access is propegated with documents during replication.

High priority bugs were addressed in this release as well.

The enhancements/bugs addressed are:

* escaped "less than" in inlinedata causes invalid eml output (Bug 2564)
* need to set filename for download files (Bug 2566)
* add ability for search engines to index metacat documents (Bug 2826)
* EML citation section should include both organizationNames and individualNames (Bug 3059)
* refactor skins to get organization list from metacat getOrganizations() function (Bug 3114)
* Update dataknp.sanparks.org packageId attributes to match Metacat Ids (Bug 3258)
* New documents explicitly set as public access don't show up correctly (Bug 3262)
* Modify confguration utility in metacat (Bug 3371)
* Integrate perl token replacement changes (Bug 3372)
* Create skin specific configuration utility (Bug 3373)
* Add authentication for configuration utility (Bug 3374)
* Create a sorted properties utility (Bug 3375)
* Add DB upgrade/install functionality to java code (Bug 3376)
* Create ANT install target for developers (Bug 3377)
* Create LSID server installation (Bug 3380)
* Create unit test code for 1.9 additions (Bug 3381)
* create simple turnkey installer for metacat Phase I (Bug 3461)
* Maps do not display in Firefox version 3 (Bug 3462)
* Replicate access rules in replication (Bug 3464)
* Metacat casesensitive="true" option in pathquery expressions is broken (Bug 3472)
* Add full schema checking when metacat register new schema (Bug 3474)
* Incorporate EML 2.1.0 access changes in metacat (Bug 3495)
* getprincipals action returns invalid XML document (Bug 3527)
* Update knbweb to serve new metacat install (Bug 3545)
* Replication should write to disk (Bug 3554)
* Update acknowlegements in metacat readme (Bug 3588)
* Validate the ldap administrator field. (Bug 3616)
* Allow for different authentication services in metacat configuration (Bug 3680)
* Tag utilities module and have metacat check out that tag (Bug 3685)

## Release Notes for 1.8.1:
This release focuses on bug fixes. In this release, the problem that Metacat 1.8.0 and previous
versions use illegitimate EML 2.0.1 schemas is addressed: first, Metacat 1.8.1 comes with
legitimate EML 2.0.1 schemas; second, existing invalid EML 2.0.1 documents will be
automatically fixed while maintaining their package ID during the Metacat 1.8.1 upgrading process.
Details please see:
- http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3239
- http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3241

Note: after upgrading the Metacat to version 1.8.1, some replication issues may be seen, e.g., the
EML 2.0.1 documents from Metacat 1.8.0 or ealier verions may be rejected since Metacat 1.8.1 uses
different EML 2.0.1 schemas (see bug 3239). If this is case, Metacat administrators should notify
their counterparts to upgrade previous version Metacats to 1.8.1.

The fixed bugs include:

* Character set (charset) problem when filling out form (Bug2797)
* Charset bug: > "less than" symbol does not display correctly (Bug2517)
* Metacat performance issue in Sanparks skin (Bug 3174)
* Metacat using illegitimate versions of EML schema (Bug 3239)
* Update older invalidated eml201 document in Metacat (Bug 3241)
* Verify existing token changes to skins (Bug 3176)
* The edit feature in the NCEAS Data Repoistory does not work (Bug 2644)
* Expose permission options for registry uploads (Bug 3014)
* ESA registry doesn't have LTER in login dropdown menu (Bug 2647)
* Data Catalog Map is slow when selecting (Bug2732)
* Only one attribute per element returned with pathquery results(Bug 2999)
* The field "skinconfigs" in the MetacatUtil class is public (Bug 3057)
* Generalize the reading of skin.configs in DBTransform (Bug3058)
* The query example in metacatquery.html document need to be changed (Bug3137)
* Add upload method into Metacat tour (Bug 3139)
* Query resultset doesn't show component name in kepler skin (Bug 3178)
* Add fields used in Morpho search to the default indexPaths property (Bug 3259)
* Private data cannot be downloaded from metadata display page (xsl) (Bug 3263)
* An error message happen during the metacat start up (Bug 3279)
* Replication: Timed replication failures occur twice instead of once (Bug 3304)
* Inline Data does not work correctly without the `pre` tag (Bug 3088)
* Include FGDC metadata in KNB and NCEAS skin search results (Bug 3146)

## Release Notes for 1.8.0:
This release focuses on improving query performance.  Both the selection
and access control queries were rewritten to execute efficiently.
To improve search performance, a query caching mechanism was introduced.
Cached queries are currently supported only for public users.

New Features:
* Data registries now support uploading of data objects along with data
  packages (Bug 1982)

Bug Fixes:
* Metacat Performace: Rewrite the xml_nodes queries (Bug 2155)
* Metacat Performace: Rewrite the xml_access part of the query (Bug 2557)
* Query cache mechanism (Bug 2905)
* Metacat Performance: updates from Morpho of data packages are taking
  longer than five minutes (Bug 2805)
* Metacat Performance: Optimize Postgres and Tomcat (Bug 2157)
* Metacat Performace: Upgrading hardware setup (Bug 2175)
* Metacat Performace: Add/drop indices on key columns (Bug 2153)
* Display of metadata documents reorganized, including a new citation format,
  obvious download options, and a redesigned data table layout (Bugs 2981, 2832)
* I.E. display fixes on default skin (Bug 2579)
* Web Address links fixed (Bug 2147)
* NCEAS skin redesigned to use IFRAME-less layout, match new website.
* Spatial query:  data packages must be fully contained by the selection
  bounding box in order to appear in the search results (Bugs 2972, 2732).
* FGDC support: upload, delete, update, access control, and download
  (Bugs 2807, 2925, 2926, 2929, 2030).
Compatibility Notices:
* Tomcat 3 and 4 are no longer tested or supported. Users are
  highly encouraged to upgrade to Tomcat 5.5

## Release Notes for 1.7.0:
This release features only a few minor changes to the core Metacat engine.
The major focus of this release is the addition of the spatial functionality.
The geographic coverage of metadata documents can now be cached in a GIS-
accessible format and published via web mapping services and an online
interactive map.

New Features:
* Three new open source libraries have been integrated with Metacat:
  - Geoserver:  A standards-compliant web map server to publish
                geographic data to the web.
               (http://docs.codehaus.org/display/GEOS/Home)
  - GeoTools:   A java-based GIS toolkit to programatically
                manipulate spatial datasets.
                (http://geotools.codehaus.org/)
  - Mapbuilder: A javascript application to provide an interactive
                web map front-end to Geoserver.
                (http://docs.codehaus.org/display/MAP/Home)
* Spatial Caching/Indexing mechanism; documents with geographic
  coverages will be added to the spatial cache.
* Spatial Query action in metacat servlet.
* Interactive Web map to visualize and query the spatial
  distribution of metadata documents.
* Experimental python, ruby and php client libraries to interface with metacat.
* Preliminary process in place for web based configuration of metacat.

Compatibility Notices:
* Tomcat 3 and 4 are no longer tested or supported. Users are
  highly encouraged to upgrade to Tomcat 5.5

Bug Fixes:
* Metacat administrators are able to modify and delete documents
  created by other users.
* Fixed problems with indexing XML attributes, see bug 2469


## Release Notes for 1.6.0:

This release comes with more performance improvements, initial support for
LSIDs (Life Sciences Identifiers) and FGDC standard, more controls for
Metacat administrators and bug fixes. Below is the list of bug fixes and
performance improvements.

Bug Fixes:
* Improved the indexing algorithm. Earlier indexing failed for some documents
  randomly and used to take a lot of time to complete for others. The
  code for indexing was changed to take care of both these problems.
* Earlier, xmlns prefix was used to determine the namespace of the document.
  Now schemaLocation is used instead xmlns prefix to determine the namespace
  of the document as that is a better indicator of document type.
* INSTR was used in some SQL statements and it is not supported by Postgres.
  So SQL statements were modified so that INSTR is not used.
* Replication was changed to include replication of deleted documents also.
* Fixed bug in squery which didnt allow use of not-contains

New Features:
* Added FGDC schema and XSLT so that Metacat can handle FGDC standard
* Added support for LSID. This includes modifying the stylesheets to
  display the LSIDs, modifying the build to include a new target
  'install-ldap'.
* Added following access control levels to Metacat:
    1. Administrators - users who can do the following:
       a. Run replication
       b. Delete any document
       c. Modify any document
       d. run indexing for any document
       e. check the logs
    2. allowedUsers - User who are allowed to submit
    3. deniedUsers - Users not allowed to submit
* Added a new skin for Ecological Society of America.
* Created an Advanced search servlet which can be used from the web.
* Various connections have been modified to be secure. e.g. connection between
ldaps is made secure now, replication is done over secure channels.

Performance:
* Reduced size of xml_nodes by creating a new table for holding nodes from
  deleted documents and old revisions of the documents.
* Provided a new variable in metacat.properties where more frequently searched
  for paths can be specified. Seperate indexes are created for these paths
  so that search is faster.
* Included log4j for logging and debuging to prevent wasting of time in printing log statements.

## Release Notes for 1.5.0:

This release comes with performace improvement, user interface enhancements,
and bug fixes. Below is the list of bug fixes and performance improvements.

Bug Fixes:
* Modify property values of harvester registration servlets to match the
  servlet-mapping URL values in web.xml. The old values used the servlet
  class names. This worked in Tomcat 4 but seems to break in Tomcat 5 on
  Windows. The new values use the servlet-mapping URL values. This should
  work in both Tomcat 4 and Tomcat 5. (Bug 162)
* Re-implement logic to prune old log entries from the HARVEST_LOG and
  HARVEST_DETAIL_LOG tables. The old logic caused integrity constraint
  violations in the database because it tried to delete parent records from
  HARVEST_LOG prior to deleting child records from
  HARVEST_DETAIL_LOG. (Bug 162)
* In data registry, minor bugs fixed like the error page shows the link back
  to the form when error page was generated because of a document read
  request from search results page,  the successful delete button leading
  to a blank screen and the old ACL overwritten by the registry.
  (Bugs 1307, 1310, 1322, 1344, 1551)
* Changed the code to support insertion of following characters: single quote, backtick,
  characters in the Numeric Character Reference format (&#xyz;) and
  characters like µ. Characters like µ will be converted to and stored in
  Numeric Character Reference format(&#xyz;). They will also be returned
  in Numeric Character Reference and it is upto the client to convert them
  and display them accordingly in the application. Partial fix provided by
  Johnoel Ancheta. (Bug 1538,1711)
* Spatial search failed on Oracle because of invalid entries made by
  some documents in the nodedata column of xml_nodes for paths involving
  'eastBoundingCoordinate', 'westBoundingCoordinate',
  'northBoundingCoordinate', 'southBoundingCoordinate'. A new column
  nodedatanumerical was added to xml_nodes table such that all the numerical
  nodedata is stored in this column and this column is now used for spatial
  search. (Bug 1703, 1718)
* In the default skin, clicking on the keywords on the home page did not
  result is a search being done. Fix provided by Owen Jones. (Bug 1768)
* Metacat generated a success message even when a document which didnt
  exsist was requested for deletion. This has been fixed. (Bug 1850)
* "packagetype" in xml-relation table was entered as eml200 namespace when
  an eml201 document was inserted. This is fixed now so that "packagetype"
  in xml-relation table points to eml201 namespace. (Bug 1979)
* System_id in xml_catalog pointed to http://knb.ecoinformatics.org/knb/
  earlier. Fixed such that it points to the locally installed metacat url.
  (Bug 1986)
* Changes to the Query subsystem fix bugs that prevented attributes from
  being expressed solely in the xpath query statement and the returnfield
  values. For instance, a query URL may now include search strings like
  '@packageId=sbclter%25' and return strings like 'returnfield=@packageId'.
  Previously, the attribute had to be appended to an element:
  '/eml/@packageId=sbclter%25'. These fixes change DBQuery.java,
  QuerySpecification.java, and QueryTerm.java (Bug 2052)
* Search query failed in case of Oracle if number of documents in the result
  were more than 1000. This has been fixed. (Bug 2000)
* Removed any occurence of enum which is now a keyword in Java 1.5


Enhancements:
* Major improvements done in user interface for the data registeries and
  various other skins. Improvements done based on suggestions from Mark
  Stromberg, Laura Downey and others. Improvements also done in resultset.xsl,
  the default skin and ldap templates. (Bug 1948)
* Code added such that administrator can delete documents irrespective of
  who the document belongs to.
* Performance improvement done in searching of datasets. A part of the search
  time was taken up because of generation of resultset after the search had be
  done. So this was the sequence of events in earlier metacat
  1. Search for the given text
  2. A list of docids generated
  3. For each of the docids
     3a) Use xml_index and xml_nodes table to generate the string describing the document including the returnfields requested in the search
  4. Add all the strings from step 3a to send back the resultant
     document. Here a decent amount of time was being taken by step 3a.

  The algorithm is now modified by addition of two tables xml_queryresult and
  xml_returnfields and a user defined parameter xml_returnfield_count. The
  new algorithm works as follows:
  1. Search for the given text
  2. A list of docids is generated
  3. For the given set of return fields generate a unique string and check
     if that string exists in xml_returnfields
     3a) If string does not exist in xml_returnfield, then enter a new record usage_count as 1 and returnfield_string as the unique string generated above.
     3b) Else if the string exists, increment usage_count
  4. Given the docids from step 2 and the id of the returnfield record from
     step 3, query for any docids that already exist in xml_queryresult.
     For the docids that do exist, get the queryresult_string.
  5. For rest of the docids from step2 which were not found in step 4, do
     the following for each of the documents:
     5a) Use xml_index and xml_nodes table to generate the string descibing the document including the returnfields requested in the search
     5b) If usage_count of the record from step is more than xml_returnfield_count set in metacat.properties, then store the string in xml_queryresult as a record which has the returnfield_id representing the set of returnfields, docid representing the document and the string as queryresult_string.
  6. Add all the strings from step 4 and step 5a to send back the resultant
     document
  So the results from step 3a in previous algorithm are effectively cached
  and hence same computation is not done again and again for each search.
  When a document is deleted, all the entries for that document in
  xml_queryresult table are also deleted. When a document is updated, all
  the entries for that document in xml_queryresult table are deleted. This
  works fine because those entries will be generated and cached again the
  next time the document is part of a search is requested.
* Performance improvement done for % search.
* Following new functions added to metacat client API: a method to set access
  on an xml document and a method to get newest version for a given document.
* Implement a new HarvesterServlet for running Harvester as a servlet. This
  eliminates the need to run Harvester in a terminal window. By default, the
  HarvesterServlet is commented out in lib/web.xml.tomcat(3,4,5). The user
  documentation will be modified to instruct Harvester administrators to
  uncomment the HarvesterServlet entry.
* Minor enhancement to support multiple email addresses for harvester
  administrator and site contact. Each address is separated by a comma or
  semicolon.
* Increase number of rows in Harvest List Editor from 300 to 1200.
* Changed default maxHarvests value to 0. Added logic to ignore maxHarvests
  value when it is set to 0 or a negative number. This allows Harvester to
  run indefinitely without shutting down after reaching a maximum number of
  harvests. The previous default value of 30 would cause Harvester to
  terminate after 30 harvests.

Details of all changes can be found in the bug database here:
http://bugzilla.ecoinformatics.org/buglist.cgi?product=Metacat&target_milestone=1.5


## Release Notes for 1.4.0:

This is a major release, and is the first to introduce many new features to
Metacat.  Some of the major new features in this version of Metacat include:

* Added a new 'Harvester' that allows documents to be periodically pulled
  from distributed sources into metacat, useful for interfacing with other
  system types. See the Metacat Tour for details.
* Added new 'skins' capability to allow the GUI for the metacat web interface
  to be more easily customized to fit into site needs.  The skin is based on
  a flexible mix of XSLT, CSS, and Javascript.
* Added a web-based metadata entry form for submitting limited EML documents
  as part of a data registry.  The GUI for the registry is customizable and
  is integrated with the 'skins' system for the main metacat interface.  The
  registry subsystem is written in Perl, and can simultaneously be used to
  present multiple 'registry' interfaces.
* A new 'client API' has been developed and implemented in Java and in Perl
  so that metacat functions can be accessed from any program in those
  languages.  The client API supports the major metacat servlet actions,
  including login(), logout(), query(), insert(), update(), delete(), and
  upload(). See the Metacat Tour for details.
* Added a new 'getlog' action that produces a usage history for all major
  events.  So now an XML report can be generated for document reads, inserts,
  deletes, etc.  See the new section in the Metacat Tour.
* Added a new 'buildindex' action that can rebuild the index entries for any
  document in the database.
* The configuration file for metacat (metacat.properties) has been moved out
  of the jar file and into the WEB-INF directory, allowing far easier changes
  to the configuration parameters.
* Results in default web interface are sorted by title in the XSLT
  (configurable)
* Many bugs were fixed.

Details of all changes can be found in the bug database here:
http://bugzilla.ecoinformatics.org/buglist.cgi?product=Metacat&target_milestone=1.4

## Release Notes for 1.3.1:

This is a simple interim bug fix. No major functionality changes. Bugs fixed
include:
* Metacat 1.3.0 doesn't work in Java 1.3 because a subtle API differnce in
    Java 1.3 and Java 1.4. Currently, Metacat 1.3.1 will work in both Java 1.3
    and Java 1.4.
* Distribution package size was reduced.


## Release Notes for 1.3.0

In 1.3.0 release, the structure of the xml_access table was changed and a new
table, xml_accesssubtree was added. If you try to update a previously
installed Metacat, you should run a script file to updated the table structure
before installation.
For Oracle user: At the SQLPLUS prompt type the following -
@src/reviseformetacat13.sql
For Postgresql user: At install directory prompt type the following -
psql exp < src/reviseformetacat13_postgres.sql
(where 'exp' is the database name).

After installation, user should run "ant schemasql" command to register EML2
schema in xml_catalog table.

If you are a new Metacat user, this step is unneeded.

Note: 1) We recommend to use Tomcat 4 and JAVA 1.4 to run Metacat. Otherwise,
         it will cause potential replication issues.
      2) Delete the xercesImpl.jar and xmlParserAPIs.jar files
         which are in $CATALINA_HOME/common/endorsed. They are old version and
         don't support XML schema validation.

New Features in 1.3.0
  1) Partialy support EML2 document. User can store, query, read and write
     EML2 documents. However, Metacat only support access control in resource
     level. The subtree level access control will be ignored.
  2) Support other xml document base on namespace/schema.
  3) Support query for attribute value
  4) Assign MIME type to data file base on metadata when user try to read it.
  5) Owner can assign access rules to a document which does not have access
     document to apply it.
  6) Support exporting single file, not only whole package
  7) Resupport Microsoft SQL Server.

Fixes in 1.3.0:
  1) Couldn't finish delta T replication for large set of documents.
  2) Couldn't create access control during delta T replication.
  3) Eorr will be written to a seperated log file if some documents
     were failed in replication.
  4) Decrease the time to create access rules during insert or update
     a package.
