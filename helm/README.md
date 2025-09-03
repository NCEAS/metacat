# Metacat Helm Chart

Metacat is repository software for preserving data and metadata (documentation about data) that
helps scientists find, understand and effectively use data sets they manage or that have been
created by others. For more details, see https://github.com/NCEAS/metacat

> [!IMPORTANT]
> ### Before You Start:
> 1. **This Metacat Helm chart is a beta feature**. It has been tested, and we believe it to be
>    working well, but it has not yet been used in production - so we recommend caution with this
>    early release. If you try it, [we'd love to hear your
>    feedback](https://www.dataone.org/contact/)! After you have read the details below, [this
>    checklist](./admin/MetacatQuickRef.md) may be helpful in guiding you through the necessary
>    installation steps.
>
>
> 2. If you are considering **migrating an existing Metacat installation to Kubernetes**, note that
>    ***before starting a migration, you must have a fully-functioning installation of Metacat
>    version 2.19, running with PostgreSQL version 14. Migrating from other versions of Metacat
>    and/or PostgreSQL is not supported.*** See [this checklist](./admin/MetacatQuickRef.md)
>    for the necessary migration steps.
>
>
> 3. If you are upgrading from a previous Helm Chart major version (e.g. from chart v1.2.0 to chart
>    v.2.0.0), first check the [Metacat Release Notes](../RELEASE-NOTES.md) to see if this involves
>    a change in the major version of the PostgreSQL application deployed by the included Bitnami
>    PostgreSQL sub-chart. If it does, you will first need to dump your database contents before you
>    upgrade -- see the [Major Version Upgrades](#major-version-upgrades) section.
>
>
> 4. This deployment does not currently work on Apple Silicon machines (e.g. in Rancher Desktop),
>    because the official Docker image for at least one of the dependencies (RabbitMQ) doesn't yet
>    work in that environment.

---

## Table of Contents

- [Metacat Helm Chart](#metacat-helm-chart)
    * [TL;DR](#tldr)
    * [Introduction](#introduction)
    * [Prerequisites](#prerequisites)
    * [Installing the Chart](#installing-the-chart)
    * [Major Version Upgrades](#major-version-upgrades)
    * [Uninstalling the Chart](#uninstalling-the-chart)
    * [Parameters](#parameters)
    * [Configuration and installation details](#configuration-and-installation-details)
        + [Metacat Application-Specific Properties](#metacat-application-specific-properties-1)
        + [Secrets](#secrets)
    * [Persistence](#persistence)
    * [Networking and Certificates](#networking-and-certificates)
    * [Setting up a TLS Certificate for HTTPS Traffic](#setting-up-a-tls-certificate-for-https-traffic)
    * [Setting up Certificates for DataONE Replication](#setting-up-certificates-for-dataone-replication)
    * [Appendix 1: Self-Signing TLS Certificates for HTTPS Traffic](#appendix-1-self-signing-tls-certificates-for-https-traffic)
    * [Appendix 2: Self-Signing Certificates for Testing Mutual Authentication](#appendix-2-self-signing-certificates-for-testing-mutual-authentication)
    * [Appendix 3: Troubleshooting Mutual Authentication](#appendix-3-troubleshooting-mutual-authentication)
    * [Appendix 4: Debugging and Logging](#appendix-4-debugging-and-logging)
    * [Appendix 5: Upgrader InitContainer Sample Logs](#appendix-5-upgrader-initcontainer-sample-logs)

---

## TL;DR
Starting in the root directory of the `metacat` repo:

1. You should not need to edit much in [values.yaml](./values.yaml), but you can look at the
   contents of the values overlay files (like the ones in the [./examples directory](./examples)),
   to see which settings typically need to be overridden. Save your settings in a yaml file,
   e.g: `/your/values-overrides.yaml`

2. Add your credentials to [./admin/secrets.yaml](helm/admin/secret--metacat.yaml), and add to cluster:

    ```shell
    $ vim helm/admin/secret--metacat.yaml    ## follow the instructions in this file
    ```

3. Deploy

   (*Note: Your k8s service account must have the necessary permissions to get information about the
   resource `roles` in the API group `rbac.authorization.k8s.io`*).

    ```shell
    $ ./helm-upstall.sh  myreleasename  mynamespace oci://ghcr.io/nceas/charts/metacat  \
                                          --version [version-here]  -f  /your/values-overrides.yaml
    ```

To access Metacat, you'll need to create a mapping between your ingress IP address (found by:
`kubectl describe ingress | grep "Address:"`) and your metacat hostname. Do this either by adding a
permanent DNS record for everyone to use, or by adding a line to the `/etc/hosts` file on your
local machine, providing temporary local access for your own testing. You should then be able to
access the application via http://your-host-name/metacat.

Read on for more in-depth information about the various installation and configuration options that
are available...

## Introduction

This chart deploys a [Metacat](https://github.com/NCEAS/metacat) deployment on a [Kubernetes](https://kubernetes.io) cluster,
using the [Helm](https://helm.sh) package manager.

## Prerequisites

- Kubernetes 1.23.3+
- Helm 3.16.1+
- PV provisioner support in the underlying infrastructure

## Installing the Chart

To install the chart with the release name `my-release`:

```shell
helm install my-release oci://ghcr.io/nceas/charts/metacat --version [version-here]
```

This command deploys Metacat on the Kubernetes cluster in the default configuration that is defined
by the parameters in the [values.yaml file](./values.yaml). The [Parameters](#parameters) section,
below, lists the parameters that can be configured during installation.

It is likely that you will need to override some of these default parameters. This can be achieved
by creating a YAML file that specifies only those values that need to be overridden, and providing
that file as part of the helm install command. For example:

```shell
helm install my-release  -f myValues.yaml  oci://ghcr.io/nceas/charts/metacat --version [version-here]
```
(where `myValues.yaml` contains only the values you wish to override.)

Parameters may also be provided on the command line to override those in
[values.yaml](./values.yaml); e.g.

```shell
helm install my-release oci://ghcr.io/nceas/charts/metacat --version [version-here]  \
                        --set postgres.auth.existingSecret=my-release-secrets
```

> **Note**: Some settings need to be edited to include release name that you choose. See the
> [values.yaml](./values.yaml) file for settings that include `${RELEASE_NAME}`. The instructions
> at the beginning of [values.yaml](./values.yaml) suggest simple ways to achieve this.

## Major Version Upgrades

> [!IMPORTANT]
> If you are upgrading across Metacat Helm chart **major** versions (e.g. from chart v1.x.x to chart
> v.2.x.x), always check the [Metacat Release Notes](../RELEASE-NOTES.md) to see if this involves a
> change in the major version of the underlying **PostgreSQL** application that is deployed by the
> included Bitnami PostgreSQL sub-chart. **If it does, you will first need to dump your database
> contents before you upgrade** -- see below. Note that the **Bitnami helm chart version** is
> different from the **PostgreSQL application version**; to see how they correspond, use the
> command: `helm search repo bitnami/postgresql --versions`

If the Metacat helm chart upgrade involves a major-version upgrade of PostgreSQL, the following
steps are required. (Note: this procedure assumes that both the old and the new data directories
will be on the same volume and mount-point):

### **BEFORE UPGRADING**: with the current ("old version") chart deployed:

1. Put Metacat into Read-Only mode:

> [!WARNING]
> Always put Metacat in "Read Only" mode during the database upgrade, or you may lose data!

   ```shell
      helm upgrade $RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \
          --version [OLD-chart-version] \
          -f [your-values-overrides] \
          --set metacat.application\\.readOnlyMode=true

      # # # IMPORTANT: Note the TWO backslashes in: metacat.application\\.readOnlyMode
   ```

2. Run the script: [metacat/helm/admin/pg_dump_for_upgrades.sh](./admin/pg_dump_for_upgrades.sh),
   which will check your data directory location is correctly versioned, and then carry out a
   `pg_dump` of your existing database.

> [!TIP]
> The script is safe and non-destructive. It never deletes data, and seeks permission before
> making copies to new locations.

### **UPGRADE PROCESS**
1. Ensure your Values overrides are correct for the section:

   ```yaml
   postgresql:
     upgrader:
       persistence:
        existingClaim: # [existing-postgresql-pvc-name-here]
   ```
> [!IMPORTANT]
> You must remove the `postgresql.postgresqlDataDir: /bitnami/postgresql/14/main` override, if you
> added it for the previous (`pg_dump`) step, since the new chart will contain the correct
> (versioned) location by default. (Alternatively, if you are using a custom path for the next
> DB version, change it to point there).

2. `helm uninstall` the OLD version of the Metacat chart, and then `helm install` the NEW
   Metacat chart. This will automatically detect the pg_dump directory, and use it to `pg_restore`
   the data into the new version of PostgreSQL. (We recommend you don't use `helm upgrade` across
   major versions.) Don't forget the `metacat.application\\.readOnlyMode` flag.

   ```shell
   helm uninstall $RELEASE_NAME

   ## ...wait for uninstall to complete...

   helm install $RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \
       --version {NEW-chart-version} \
       -f {your-values-overrides} \
       --set metacat.application\\.readOnlyMode=true  ## Two slashes!

   ## ...as metacat pod is starting up, view the initContainer logs:
   kubectl logs -f pod/${RELEASE_NAME}-0 -c pgupgrade
   ```

> [!TIP]
> See example `initContainer` logs from a successful upgrade in [Appendix 5: Upgrader InitContainer
> Sample Logs](#appendix-5-upgrader-initcontainer-sample-logs).

3. Finally, verify that the upgrade has completed successfully, the new version of PostgreSQL is
   running, and your data is intact. If so, you can unset "Read Only" mode by doing a `helm upgrade`
   without the `readOnlyMode` flag, so Metacat will once again be able to accept edits and uploads:

   ```shell
   # First verify that Metacat is working correctly: you should see a non-zero number of objects
   # returned when you browse:
   #  https://YOUR-HOST/metacat/d1/mn/v2/object
   #
   # If so, then:
   #
   helm upgrade $RELEASE_NAME oci://ghcr.io/nceas/charts/metacat \
       --version {NEW-chart-version} \
       -f {your-values-overrides}  , as soon
   ```

### Troubleshooting

1. **Troubleshooting the `pg_dump` step** (from the script:
   [metacat/helm/admin/pg_dump_for_upgrades.sh](./admin/pg_dump_for_upgrades.sh))
   - If the `pg_dump` fails, use `kubectl describe pod...` to check whether the PostgreSQL container
     ran out of memory and was `OOMKilled`. If so, you may need to increase the memory limits in
     your values overrides for `postgresql.primary.resources`.

2. **Troubleshooting the `pg_restore` step** (from the `initContainer` in the new chart)
   - If the upgrade initContainer fails for some reason, metacat will continue to start up, and will
     initialize the new (empty) database. In this case, you will also see:
     - Error messages in the Metacat pod's `pgupgrade` `initContainer` logs (See log examples in
       [Appendix 5](#appendix-5-upgrader-initcontainer-sample-logs).)

       ```shell
       kubectl logs -f pod/${RELEASE_NAME}-0 -c pgupgrade
       ```

     - `total="0"` objects in the database when browsing to `https://YOUR-HOST/metacat/d1/mn/v2/object`:

       ```xml
       <ns2:objectList xmlns:ns2="http://ns.dataone.org/service/types/v1" count="0" start="0" total="0"/>
       ```

   - Re-running the upgrader (`initContainer`) will NOT `pg_restore` the database, since it will
     detect the presence of Metacat tables, and will refuse to overwrite what it believes to be
     existing data. We therefore recommend:
     1. Investigating and fixing the `initContainer` error. (If you need to re-install the chart in
        order to do this, **make sure it is still in Read Only mode!**)
     2. Doing a `helm uninstall` of the chart
     3. Manually moving the NEW data directory to a different location, so PostgreSQL can make
        another new one on next startup.
     4. Doing a `helm install` of the chart, and watching the initContainer logs again.

> [!Note]
> the data still exists in the OLD data directory, and is backed up in the `pg_dump` output!

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```shell
helm delete my-release
```

The `helm delete` command removes all the Kubernetes components associated with the chart
(except for Secrets, PVCs and PVs) and deletes the release.

There are multiple PVCs associated with `my-release`, for Metacat data files, the PostgreSQL
database, and for components of the indexer sub-chart. To delete:

```shell
kubectl delete pvc <myPVCName>   ## deletes specific named PVC
or:
kubectl delete pvc -l release=my-release   ## DANGER! deletes all PVCs associated with the release
```

> [!CAUTION]
> DELETING THE PVCs MAY ALSO DELETE ALL YOUR DATA. depending upon your setup! Please be
> cautious!


## Parameters

### Global Properties Shared Across Sub-Charts Within This Deployment

| Name                                                 | Description                                                                 | Value                             |
| ---------------------------------------------------- | --------------------------------------------------------------------------- | --------------------------------- |
| `global.metacatExternalBaseUrl`                      | Metacat base url accessible from outside cluster.                           | `https://localhost/`              |
| `global.d1ClientCnUrl`                               | The url of the CN; used to populate metacat's 'D1Client.CN_URL'             | `https://cn.dataone.org/cn`       |
| `global.passwordsSecret`                             | The name of the Secret containing application passwords                     | `${RELEASE_NAME}-metacat-secrets` |
| `global.metacatAppContext`                           | The application context to use                                              | `metacat`                         |
| `global.storageClass`                                | default name of the storageClass to use for PVs                             | `local-path`                      |
| `global.ephemeralVolumeStorageClass`                 | Optional global storageClass override                                       | `""`                              |
| `global.sharedVolumeSubPath`                         | The subdirectory of the metacat data volume to mount                        | `""`                              |
| `global.dataone-indexer.enabled`                     | Enable the dataone-indexer sub-chart                                        | `true`                            |
| `global.includeMetacatUi`                            | Enable or disable the MetacatUI sub-chart.                                  | `true`                            |
| `global.metacatUiIngressBackend.enabled`             | Enable or disable MetacatUI support via Ingress                             | `false`                           |
| `global.metacatUiIngressBackend.service.name`        | MetacatUI service name (used only if 'global.includeMetacatUi: false')      | `metacatui${RELEASE_NAME}`        |
| `global.metacatUiIngressBackend.service.port.number` | Port for MetacatUI service (used only if 'global.includeMetacatUi: false')  | `80`                              |
| `global.metacatUiThemeName`                          | MetacatUI theme name to use. (used only if 'global.includeMetacatUi: true') | `knb`                             |
| `global.metacatUiWebRoot`                            | The url root to be appended after the MetacatUI baseUrl.                    | `/`                               |

### Metacat Application-Specific Properties

| Name                              | Description                                                     | Value               |
| --------------------------------- | --------------------------------------------------------------- | ------------------- |
| `metacat.application.context`     | see global.metacatAppContext                                    | `metacat`           |
| `metacat.auth.administrators`     | A semicolon-separated list of admin ORCID iDs                   | `""`                |
| `metacat.database.connectionURI`  | postgres database URI (or blank if using CloudNative PG)        | `""`                |
| `metacat.guid.doi.enabled`        | Allow users to publish Digital Object Identifiers at doi.org?   | `true`              |
| `metacat.server.port`             | The http port exposed externally, if NOT using the ingress      | `""`                |
| `metacat.server.name`             | The hostname for the server, as exposed by the ingress          | `localhost`         |
| `metacat.solr.baseURL`            | The url to access solr, or leave blank to use sub-chart         | `""`                |
| `metacat.solr.coreName`           | The solr core (solr standalone) or collection name (solr cloud) | `""`                |
| `metacat.replication.logdir`      | Location for the replication logs                               | `/var/metacat/logs` |
| `metacat.index.rabbitmq.hostname` | the hostname of the rabbitmq instance that will be used         | `""`                |
| `metacat.index.rabbitmq.username` | the username for connecting to the RabbitMQ instance            | `metacat-rmq-guest` |

### OPTIONAL DataONE Member Node (MN) Parameters

| Name                                                          | Description                                                       | Value                                        |
| ------------------------------------------------------------- | ----------------------------------------------------------------- | -------------------------------------------- |
| `metacat.cn.server.publiccert.filename`                       | optional cert(s) used to validate jwt auth tokens,                | `/var/metacat/pubcerts/DataONEProdIntCA.pem` |
| `metacat.dataone.certificate.fromHttpHeader.enabled`          | Enable mutual auth with client certs                              | `false`                                      |
| `metacat.dataone.autoRegisterMemberNode`                      | Automatically push MN updates to CN? (yyyy-MM-dd)                 | `2023-02-28`                                 |
| `metacat.dataone.nodeId`                                      | The unique ID of your DataONE MN - must match client cert subject | `urn:node:METACAT_TEST`                      |
| `metacat.dataone.subject`                                     | The "subject" string from your DataONE MN client certificate      | `CN=urn:node:METACAT1,DC=dataone,DC=org`     |
| `metacat.dataone.nodeName`                                    | short name for the node that can be used in user interfaces       | `My Metacat Node`                            |
| `metacat.dataone.nodeDescription`                             | What is the node's intended scope and purpose?                    | `Describe your Member Node briefly.`         |
| `metacat.dataone.contactSubject`                              | registered contact for this MN                                    | `http://orcid.org/0000-0002-8888-999X`       |
| `metacat.dataone.nodeSynchronize`                             | Enable Synchronization of Metadata to DataONE                     | `false`                                      |
| `metacat.dataone.nodeSynchronization.schedule.year`           | sync schedule year                                                | `*`                                          |
| `metacat.dataone.nodeSynchronization.schedule.mon`            | sync schedule month                                               | `*`                                          |
| `metacat.dataone.nodeSynchronization.schedule.mday`           | sync schedule day of month                                        | `*`                                          |
| `metacat.dataone.nodeSynchronization.schedule.wday`           | sync schedule day of week                                         | `?`                                          |
| `metacat.dataone.nodeSynchronization.schedule.hour`           | sync schedule hour                                                | `*`                                          |
| `metacat.dataone.nodeSynchronization.schedule.min`            | sync schedule minute                                              | `0/3`                                        |
| `metacat.dataone.nodeSynchronization.schedule.sec`            | sync schedule second                                              | `10`                                         |
| `metacat.dataone.nodeReplicate`                               | Accept and Store Replicas?                                        | `false`                                      |
| `metacat.dataone.replicationpolicy.default.numreplicas`       | # copies to store on other nodes                                  | `0`                                          |
| `metacat.dataone.replicationpolicy.default.preferredNodeList` | Preferred replication nodes                                       | `""`                                         |
| `metacat.dataone.replicationpolicy.default.blockedNodeList`   | Nodes blocked from replication                                    | `""`                                         |

### OPTIONAL (but Recommended) Site Map Parameters

| Name                            | Description                                                     | Value      |
| ------------------------------- | --------------------------------------------------------------- | ---------- |
| `metacat.sitemap.enabled`       | Enable sitemaps to tell search engines which URLs are available | `false`    |
| `metacat.sitemap.interval`      | Interval (in milliseconds) between rebuilding the sitemap       | `86400000` |
| `metacat.sitemap.location.base` | The first part of the URLs listed in sitemap_index.xml          | `/`        |
| `metacat.sitemap.entry.base`    | base URI of the dataset landing page, listed in the sitemap     | `/view`    |

### robots.txt file (search engine indexing)

| Name               | Description                                                          | Value |
| ------------------ | -------------------------------------------------------------------- | ----- |
| `robots.userAgent` | "User-agent:" defined in robots.txt file. Defaults to "*" if not set | `""`  |
| `robots.disallow`  | the "Disallow:" value defined in robots.txt file.                    | `""`  |

### Metacat Image, Container & Pod Parameters

| Name                                     | Description                                                                  | Value                   |
| ---------------------------------------- | ---------------------------------------------------------------------------- | ----------------------- |
| `image.repository`                       | Metacat image repository                                                     | `ghcr.io/nceas/metacat` |
| `image.pullPolicy`                       | Metacat image pull policy                                                    | `IfNotPresent`          |
| `image.tag`                              | Overrides the image tag. Will default to the chart appVersion if set to ""   | `""`                    |
| `image.debug`                            | Specify if container debugging should be enabled (sets log level to "DEBUG") | `false`                 |
| `imagePullSecrets`                       | Optional list of references to secrets in the same namespace                 | `[]`                    |
| `container.ports`                        | Optional list of additional container ports to expose within the cluster     | `[]`                    |
| `serviceAccount.create`                  | Should a service account be created to run Metacat?                          | `false`                 |
| `serviceAccount.annotations`             | Annotations to add to the service account                                    | `{}`                    |
| `serviceAccount.name`                    | The name to use for the service account.                                     | `""`                    |
| `podAnnotations`                         | Map of annotations to add to the pods                                        | `{}`                    |
| `podSecurityContext.enabled`             | Enable security context                                                      | `true`                  |
| `podSecurityContext.runAsUser`           | numerical User ID for the pod                                                | `59997`                 |
| `podSecurityContext.runAsGroup`          | numerical Group ID for the pod                                               | `59997`                 |
| `podSecurityContext.fsGroup`             | numerical Group ID used to access mounted volumes                            | `59997`                 |
| `podSecurityContext.supplementalGroups`  | additional GIDs used to access vol. mounts                                   | `[]`                    |
| `podSecurityContext.runAsNonRoot`        | ensure all containers run as a non-root user.                                | `true`                  |
| `podSecurityContext.fsGroupChangePolicy` | control how Kubernetes manages ownership & perms...                          | `OnRootMismatch`        |
| `securityContext`                        | holds container-level security attributes that override those at pod level   | `{}`                    |
| `resources`                              | Resource limits for the deployment                                           | `{}`                    |
| `tolerations`                            | Tolerations for pod assignment                                               | `[]`                    |

### Metacat Persistence

| Name                        | Description                                                          | Value               |
| --------------------------- | -------------------------------------------------------------------- | ------------------- |
| `persistence.enabled`       | Enable metacat data persistence using Persistent Volume Claims       | `true`              |
| `persistence.storageClass`  | Override here or leave blank to use 'global.storageClass'            | `""`                |
| `persistence.existingClaim` | Name of an existing Persistent Volume Claim to re-use                | `""`                |
| `persistence.volumeName`    | Name of an existing Volume to use for volumeClaimTemplate            | `""`                |
| `persistence.subPath`       | The subdirectory of the volume (see persistence.volumeName) to mount | `""`                |
| `persistence.accessModes`   | PVC Access Mode for metacat volume                                   | `["ReadWriteMany"]` |
| `persistence.size`          | PVC Storage Request for metacat volume                               | `1Gi`               |

### Networking & Monitoring

| Name                              | Description                                                              | Value                            |
| --------------------------------- | ------------------------------------------------------------------------ | -------------------------------- |
| `ingress.enabled`                 | Enable or disable the ingress                                            | `true`                           |
| `ingress.className`               | ClassName of the ingress provider in your cluster                        | `nginx`                          |
| `ingress.annotations`             | `nginx.ingress.kubernetes.io` annotations                                | `see values.yaml`                |
| `ingress.defaultBackend.enabled`  | enable the optional defaultBackend                                       | `false`                          |
| `ingress.defaultBackend.enabled`  | enable the optional defaultBackend                                       | `false`                          |
| `ingress.rewriteRules`            | formatted text rewrite rules for the nginx ingress                       | `""`                             |
| `ingress.configurationSnippet`    | added to nginx ingress config...                                         | `see values.yaml`                |
| `ingress.tls`                     | The TLS configuration                                                    | `[]`                             |
| `ingress.rules`                   | The Ingress rules can be defined here or left blank to be auto-populated | `[]`                             |
| `ingress.d1CaCertSecretName`      | Name of Secret containing DataONE CA certificate chain                   | `d1-ca-chain`                    |
| `service.enabled`                 | Enable another optional service in addition to headless svc              | `false`                          |
| `service.type`                    | Kubernetes Service type. Defaults to ClusterIP if not set                | `LoadBalancer`                   |
| `service.clusterIP`               | IP address of the service. Auto-generated if not set                     | `""`                             |
| `service.ports`                   | The port(s) to be exposed                                                | `[]`                             |
| `startupProbe.enabled`            | Enable startupProbe for the Metacat container                            | `true`                           |
| `startupProbe.httpGet.path`       | The url path to probe during startup                                     | `/metacat/d1/mn/v2/monitor/ping` |
| `startupProbe.httpGet.port`       | The named containerPort to probe                                         | `metacat-web`                    |
| `startupProbe.successThreshold`   | Min consecutive successes for probe to be successful                     | `1`                              |
| `startupProbe.failureThreshold`   | No. of consecutive failures before the container restarted               | `30`                             |
| `startupProbe.periodSeconds`      | Interval (in seconds) between startup checks                             | `10`                             |
| `startupProbe.timeoutSeconds`     | Timeout (in seconds) for each startup check                              | `5`                              |
| `livenessProbe.enabled`           | Enable livenessProbe for Metacat container                               | `false`                          |
| `readinessProbe.enabled`          | Enable readinessProbe for Metacat container                              | `true`                           |
| `readinessProbe.httpGet.path`     | The url path to probe.                                                   | `/metacat/d1/mn/v2/monitor/ping` |
| `readinessProbe.httpGet.port`     | The named containerPort to probe                                         | `metacat-web`                    |
| `readinessProbe.periodSeconds`    | Period seconds for readinessProbe                                        | `15`                             |
| `readinessProbe.timeoutSeconds`   | Timeout seconds for readinessProbe                                       | `10`                             |
| `readinessProbe.successThreshold` | Min consecutive successes for probe to be successful                     | `1`                              |
| `readinessProbe.failureThreshold` | No. consecutive failures before container marked unhealthy               | `6`                              |

### Postgresql Sub-Chart

| Name                            | Description                                               | Value             |
| ------------------------------- | --------------------------------------------------------- | ----------------- |
| `cnpg.database`                 | The name of the database used by metacat.                 | `metacat`         |
| `cnpg.existingSecret`           | override name of Secret holding username and password     | `""`              |
| `cnpg.postgresql.pg_hba`        | client authentication pg_hba.conf                         | `see values.yaml` |
| `cnpg.postgresql.pg_ident`      | username mappings: pg_ident.conf                          | `see values.yaml` |
| `cnpg.persistence.storageClass` | Override here or leave blank to use 'global.storageClass' | `""`              |
| `cnpg.persistence.size`         | PVC Storage size request for postgres volumes             | `1Gi`             |

### Tomcat Configuration

| Name                       | Description                                                           | Value                         |
| -------------------------- | --------------------------------------------------------------------- | ----------------------------- |
| `tomcat.jmxEnabled`        | Enable JMX for Tomcat, to inspect JVM usage of CPU, memory, etc.      | `false`                       |
| `tomcat.jmxPort`           | The port to use for JMX connections. IMPORTANT: If you change this... | `9010`                        |
| `tomcat.jmxCatalinaOpts`   | Tomcat JVM options for enabling JMX                                   | `see values.yaml`             |
| `tomcat.heapMemory.min`    | minimum memory heap size for Tomcat (-Xms JVM parameter)              | `""`                          |
| `tomcat.heapMemory.max`    | maximum memory heap size for Tomcat (-Xmx JVM parameter)              | `""`                          |
| `tomcat.extraCatalinaOpts` | Extra JVM options to pass to Tomcat. SEE IMPORTANT NOTES BELOW:       | `["-XX:MaxRAMPercentage=75"]` |

### dataone-indexer Sub-Chart

| Name                                                         | Description                                       | Value                                 |
| ------------------------------------------------------------ | ------------------------------------------------- | ------------------------------------- |
| `dataone-indexer.podSecurityContext.fsGroup`                 | gid used to access mounted volumes                | `59997`                               |
| `dataone-indexer.podSecurityContext.supplementalGroups`      | additional vol access gids                        | `[]`                                  |
| `dataone-indexer.podSecurityContext.fsGroupChangePolicy`     | ownership & perms mgmt                            | `OnRootMismatch`                      |
| `dataone-indexer.persistence.subPath`                        | The subdirectory of the volume to mount           | `""`                                  |
| `dataone-indexer.rabbitmq.extraConfiguration`                | extra config, to be appended to rmq config        | `consumer_timeout = 144000000`        |
| `dataone-indexer.rabbitmq.auth.username`                     | set the username that rabbitmq will use           | `metacat-rmq-guest`                   |
| `dataone-indexer.rabbitmq.auth.existingPasswordSecret`       | location of rabbitmq password                     | `${RELEASE_NAME}-metacat-secrets`     |
| `dataone-indexer.solr.javaMem`                               | Java memory options to pass to the Solr container | `-Xms512m -Xmx2g`                     |
| `dataone-indexer.solr.customCollection`                      | name of the solr collection to use                | `metacat-index`                       |
| `dataone-indexer.solr.coreNames`                             | Solr core names to be created                     | `["metacat-core"]`                    |
| `dataone-indexer.solr.persistence.size`                      | solr Persistent Volume size                       | `100Gi`                               |
| `dataone-indexer.solr.extraVolumes[0].name`                  | DO NOT EDIT - referenced by sub-chart             | `solr-config`                         |
| `dataone-indexer.solr.extraVolumes[0].configMap.name`        | see notes in values.yaml                          | `${RELEASE_NAME}-indexer-configfiles` |
| `dataone-indexer.solr.extraVolumes[0].configMap.defaultMode` | DO NOT EDIT                                       | `777`                                 |
| `dataone-indexer.solr.zookeeper.persistence.size`            | Persistent Volume size                            | `100Gi`                               |


## Configuration and installation details

### Metacat Application-Specific Properties

The parameters in the
[Metacat Application-Specific Properties](#metacat-application-specific-properties)
section, above, map to the values required by Metacat at runtime. For more information please refer
to the [Metacat Properties](https://knb.ecoinformatics.org/knb/docs/metacat-properties.html) section
of the [Metacat Administrators' Guide](https://knb.ecoinformatics.org/knb/docs/).

### Secrets

Secret parameters (such as login credentials, auth tokens, private keys etc.) should be installed as
kubernetes Secrets in the cluster. The files [admin/secrets.yaml](helm/admin/secret--metacat.yaml) and
[admin/cloudnative-pg-secret.yaml](helm/admin/secret--cloudnative-pg.yaml) provide templates that you
can complete and apply using `kubectl apply -f <filename>` -- for details, see the instructions in
the comments inside those files. Please remember to NEVER ADD UNENCRYPTED SECRETS TO GITHUB!

> [!IMPORTANT]
> The names of the deployed Secrets include the release name as a prefix
> (e.g. `my-release-metacat-secrets`), so it's important to ensure that the secrets name matches
> the release name referenced whenever you use `helm` commands.

## User Interface

The Metacat helm chart also installs [MetacatUI](https://nceas.github.io/metacatui/), which is
included as a sub-chart. The MetacatUI sub-chart is highly configurable, and can be used with
included themes, or you can provide your own custom theme, mounted on a PVC. At a minimum, you
should provide values for [the `global`
properties](#global-properties-shared-across-sub-charts-within-this-deployment). More information
can be found in the [MetacatUI README](https://github.com/NCEAS/metacatui/tree/develop/helm#readme).

If you wish to disable the subchart altogether, set `global.includeMetacatUi: false` and provide
your own MetacatUI installation. deployed separately. Note that you can use the values in
`global.metacatUiIngressBackend` to configure the ingress for your separate MetacatUI installation;
see the documentation in [Values.yaml](./Values.yaml) for details.

## Persistence

Persistent Volume Claims are used to keep the data across deployments. See the
[Parameters](#parameters) section to configure the PVCs or to disable persistence for either
application.

The Metacat image stores the Metacat data and configurations on a PVC mounted at the `/var/metacat`
path in the metacat container.

The PostgreSQL image stores the database data at the `/bitbami/pgdata` path in its own container.

Details of the `dataone-indexer` sub-chart PV/PVC requirements can be found in the [dataone-indexer
repository](https://github.com/DataONEorg/dataone-indexer). DataONE Indexer also needs read access
to the same PVC used by Metacat for its file-storage.

Details of the `MetacatUI` sub-chart (optional) PV/PVC requirements can be found in the [MetacatUI
README](https://github.com/NCEAS/metacatui/tree/develop/helm#readme).


## Networking and Certificates

By default, the chart will install an
[Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/) (see the `ingress.*`
parameters under [Networking & Monitoring](#networking--monitoring)), which will expose HTTP and/or
HTTPS routes from outside the cluster to the Metacat application within the cluster. Note that
your cluster must have an Ingress controller in order for this to work.

**Note:** We strongly recommend that you use the Kubernetes open source community version of [the
nginx ingress controller](https://kubernetes.github.io/ingress-nginx/). (Full functionality may not
be available if you choose an alternative). You can install it as follows:

```shell
$  helm upgrade --install ingress-nginx ingress-nginx \
                --repo https://kubernetes.github.io/ingress-nginx \
                --namespace ingress-nginx --create-namespace
```

> [!TIP]
> You can inspect available Ingress classes in your cluster using:
> `$ kubectl get ingressclasses`
>
> Note that there are significant differences between the community version of [the
> nginx ingress controller](https://kubernetes.github.io/ingress-nginx/) and the one provided by the
> NGINX company. This helm chart relies on the functionality of the community version.


### Setting up a TLS Certificate for HTTPS Traffic

HTTPS traffic is served on port 443 (a.k.a. the "SSL" port), and requires the ingress to have
access to a TLS certificate and private key. A certificate signed by a trusted Certificate
Authority is needed for public servers, or you can create your own self-signed certificate for
development purposes - see
[Appendix 1](#appendix-1-self-signing-tls-certificates-for-https-traffic) below, for self-signing
instructions.

Once you have obtained the server certificate and private key, you can add them to the
Kubernetes secrets, as follows (creates a Secret named `tls-secret`, assuming the server
certificate and private key are named `server.crt` and `server.key`):

```shell
kubectl create secret tls tls-secret --key server.key --cert server.crt
```

Then simply tell the ingress which secret to use:

```yaml
ingress:
  tls:
    - hosts:
        # hostname is auto-populated from the value of
        #     metacat:
        #       server.name: &extHostname myHostName.com
        - knb.test.dataone.org
      secretName: tls-secret
```

> [!TIP]
> You can save time and reduce complexity by using a certificate manager service. For
> example, our NCEAS k8s clusters include [a cert-manager
> service](https://github.com/DataONEorg/k8s-cluster/blob/main/authentication/LetsEncrypt.md) that
> constantly watches for Ingress modifications, and updates letsEncrypt certificates automatically,
> so this step is as simple as ensuring the ingress includes:
>
> ```yaml
> ingress:
>   annotations:
>     cert-manager.io/cluster-issuer: "letsencrypt-prod"
>   className: "nginx"
>   tls:
>     - hosts:
>         - knb-dev.test.dataone.org
>       secretName: ingress-nginx-tls-cert
> ```
>
> ...and a tls cert will be created and applied automatically, matching the hostname defined in the
> `tls:` section. It will be created in a new secret: `ingress-nginx-tls-cert`, in the ingress'
> namespace


### Setting up Certificates for DataONE Replication

For full details on becoming part of the DataONE network, see the [Metacat Administrator's Guide
](https://knb.ecoinformatics.org/knb/docs/dataone.html) and [Authentication and Authorization in DataONE
](https://releases.dataone.org/online/api-documentation-v2.0.1/design/AuthorizationAndAuthentication.html)
.

DataONE Replication relies on mutual authentication with x509 client-side certs. As a DataONE
Member Node (MN) or Coordinating Node (CN), your metacat instance will act as both a server and
as a client, at different times during the replication process. It is therefore necessary to
configure certificates and settings for both these roles.

#### Prerequisites
1. First make sure you have the Kubernetes version of the
   [nginx ingress installed](#networking-and-certificates)
2. Ensure [HTTPS access is set up](#setting-up-a-tls-certificate-for-https-traffic) and
   working correctly. This allows other nodes, acting as "clients" to verify your server's identity
   during mutual authentication.
3. Download a copy of the **DataONE Certificate Authority (CA) certificate chain**. This enables
   your node (when acting as server) to verify that other nodes' client certificates were signed
   by the DataONE Certificate Authority.
   1. DataONE **Production** CA Chain:
      [DataONEProdCAChain.crt](https://raw.githubusercontent.com/DataONEorg/ca/main/DataONEProdCAChain.crt)
   2. DataONE **Test** CA Chain:
      [DataONETestCAChain.crt](https://raw.githubusercontent.com/DataONEorg/ca/main/DataONETestCAChain.crt)
4. From the DataONE administrators ([support@dataone.org](mailto:support@dataone.org)), obtain a
   **Client Certificate** (sometimes referred to as a **DataONE Node Certificate**) that uniquely
   identifies your Metacat instance. This allows another node (acting as server) to verify your
   node's identity (acting as "client") during mutual authentication. The client certificate
   contains sensitive information, and must be kept private and secure.

#### Install the CA Chain

* Create the Kubernetes Secret (named `d1-ca-chain`) to hold the ca chain
  (e.g. assuming it's in a file named `DataONEProdCAChain.crt`):

  ```shell
  kubectl create secret generic d1-ca-chain --from-file=ca.crt=DataONEProdCAChain.crt
  # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
  ```

#### Install the Client Certificate

* Create the Kubernetes Secret (named `<yourReleaseName>-d1-client-cert`) to hold the Client
  Certificate, identified by the key `d1client.crt` (e.g. assuming the cert is in a file
  named `urn_node_TestNAME.pem`):

  ```shell
  kubectl create secret generic <yourReleaseName>-d1-client-cert \
                                --from-file=d1client.crt=urn_node_TestNAME.pem
  # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
  ```

#### Set the correct parameters in `values.yaml`

1. set the CA secret name

    ```yaml
    ingress:
      className: "nginx"
      d1CaCertSecretName: d1-ca-chain
    ```
2. Enable the shared secret header

    ```yaml
    metacat:
      dataone.certificate.fromHttpHeader.enabled: true
    ```

3. Ensure you have already defined a value for the shared secret that will enable metacat to verify
   the validity of incoming requests. The secret should be defined [in metacat Secrets](#secrets),
   identified by the key: `METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY`.
4. Finally, re-install or upgrade to apply the changes

See [Appendix 3](#appendix-3-troubleshooting-mutual-authentication) for help with troubleshooting

---

## Appendices

## Appendix 1: Self-Signing TLS Certificates for HTTPS Traffic

> [!NOTE]
> For development and testing purposes only!**
>
> Also see the [Kubernetes nginx
> documentation](https://kubernetes.github.io/ingress-nginx/user-guide/tls)

You can create your own self-signed certificate as follows:

```shell
HOST=myHostName.com \
&&  openssl req -x509 -nodes -days 365  \
    -newkey rsa:2048 -keyout server.key \
    -out server.crt                     \
    -subj "/CN=${HOST}/O=${HOST}"       \
    -addext "subjectAltName = DNS:${HOST}"
```

The output will be a server certificate file named `server.crt`, and a private key file named
`server.key`. For the `${HOST}`, you can use `localhost`, or your machine's real hostname.

Alternatively, you can use any other valid hostname, but you'll need to add an entry to your
`/etc/hosts` file to map it to your localhost IP address (`127.0.0.1`) so that your browser can
resolve it; e.g.:

```shell
# add entry in /etc/hosts
127.0.0.1       myHostName.com
```

Whatever hostname you are using, don't forget to set the
`metacat.server.name` accordingly, in `values.yaml`!

## Appendix 2: Self-Signing Certificates for Testing Mutual Authentication

> [!NOTE]
> For development and testing purposes only!**
>
> Also see the [Kubernetes nginx documentation
> ](https://kubernetes.github.io/ingress-nginx/examples/PREREQUISITES/#client-certificate-authentication)

Assuming you already have a [server certificate installed
](#setting-up-a-tls-certificate-for-https-traffic) (either signed by a trusted CA or [self-signed
](#appendix-1-self-signing-tls-certificates-for-https-traffic) for development & testing), you can
create your own self-signed Mutual Auth Client certificate and CA certificate as follows:

1. Generate the CA Key and Certificate:

    ```shell
    openssl req -x509 -sha256 -newkey rsa:4096 -keyout ca.key -out ca.crt -days 356 -nodes \
            -subj '/CN=My Cert Authority'
    ```

2. Generate the Client Key and Certificate Signing Request:

    ```shell
    openssl req -new -newkey rsa:4096 -keyout client.key -out client.csr -nodes \
            -subj '/CN=My Client'
    ```

3. Sign with the CA Key:

    ```shell
    openssl x509 -req -sha256 -days 365 -in client.csr -CA ca.crt -CAkey ca.key \
            -set_serial 02 -out client.crt
    ```

## Appendix 3: Troubleshooting Mutual Authentication

If you're having trouble getting Mutual Authentication working, you can run metacat in debug mode
and view the logs (see [Appendix 4](#appendix-4-debugging-and-logging) for details).

If you see the message: `X-Proxy-Key is null or blank`, it means the nginx ingress has not been set
up correctly (see [Setting up Certificates for DataONE Replication
](#setting-up-certificates-for-dataone-replication)).

You can check the configuration as follows:

1. first check the ingress definition

    ```shell
    kubectl get ingress <yourReleaseName>-metacat -o yaml
    ```

    ...and ensure the output contains these lines:

    ```yaml
      metadata:
        annotations:
        # NOTE: more lines above, omitted for clarity
          nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true"
          nginx.ingress.kubernetes.io/auth-tls-secret: default/d1-ca-chain
        ## NOTE: above may differ for you. Format is: <namespace>/<ingress.d1CaCertSecretName>
          nginx.ingress.kubernetes.io/auth-tls-verify-client: optional_no_ca
          nginx.ingress.kubernetes.io/auth-tls-verify-depth: "10"
          nginx.ingress.kubernetes.io/configuration-snippet: |
            more_set_input_headers "X-Proxy-Key: <your-secret-here>";
    ```
> [!NOTE]
> `<your-secret-here>` is the plaintext value associated with the key
> `METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY` in your secret
> `<releaseName>-metacat-secrets` -- ensure it has been set correctly!

- If you don't see these, or they are incorrect, check values.yaml for:

    ```yaml
      metacat:
        dataone.certificate.fromHttpHeader.enabled: true    # must be true for mutual auth to work!

      ingress:
        tls:    # needs to have been set up properly [see ref 1]

        d1CaCertSecretName:  # needs to match the secret name holding your ca cert chain [see ref 2]
    ```
    - *[[ref 1]](#setting-up-a-tls-certificate-for-https-traffic)*
    - *[[ref 2]](#install-the-ca-chain)*

2. If you have access to the correct namespace, you can also view the nginx ingress logs using:

    ```shell
      NS=ingress-nginx    # this is the ingress controller's namespace. Typically ingress-nginx
      kubectl logs -n ${NS} -f $(kc get pods -n ${NS} | grep "nginx" | awk '{print $1}')
    ```

## Appendix 4: Debugging and Logging

### To run Metacat in debug mode

Set the debug flag in values.yaml:

```yaml
image:
  debug: true

# (or you can do the same thing temporarily, via the `--set image.debug=true` command-line flag)
```
This has the following effects:
1. sets the logging level to DEBUG
> [!TIP]
> you can also temporarily change logging settings without needing to
> upgrade or re-install the application, by editing the log4J configuration
> ConfigMap:
>
> `kc edit configmaps <releaseName>-metacat-configfiles`
>
> (look for the key `log4j2.k8s.properties`). The config is automatically reloaded every
> `monitorInterval` seconds.
>
> **Note** that these edits will be overwritten next time you do a `helm install` or
> `helm upgrade`!

2. enables remote Java debugging via port 5005. You will need to forward this port, in order to
   access it on localhost:

    ```shell
    $ kubectl  port-forward  --namespace myNamespace  pod/mypod-0  5005:5005
    ```
> [!TIP]
> For the **indexer**, you can also set the debug flag in `values.yaml` (Note that this
 only sets the logging level to DEBUG; it does **not** enable remote debugging for the indexer):
>
> ```yaml
> dataone-indexer:
>   image:
>     debug: true
> ```

### To view the logs

#### General syntax:

Application logs for all containers running this application:

```shell
  $ kubectl logs -f -l app.kubernetes.io/name=<my-application-name>

  # example: logs from all running index worker containers
  $ kubectl logs -f -l app.kubernetes.io/name=dataone-indexer
```

Application logs from one specific pod:

```shell
  $ kubectl logs -f <specific-pod-name>

  # example: Metacat logs
  $ kubectl logs -f metacatknb-0

  # example: previous Metacat logs from (now exited) pod
  $ kubectl logs -p metacatknb-0
```

Logs from an `initContainer`:

```shell
  $ kubectl logs -f <specific-pod-name> -c <init-container-name>

  # example: Metacat's `init-solr-metacat-dep` initContainer logs
  $ kubectl logs -f metacatknb-0 -c dependencies
```

## Appendix 5: Upgrader InitContainer Sample Logs

Sample Logs from upgrader initContainer during PostgreSQL Major Upgrades:

### First Startup - Successful Upgrade

```text
$ kc logs -f pod/metacatbrooke-0 -c pgupgrade

Checking if a PostgreSQL upgrade is necessary...
Found version 17; checking if pg_restore needed...
Result of psql -h metacatbrooke-postgresql-hl -U metacat -d metacat -c "\dt":
Did not find any relations.
No Metacat tables found in /bitnami/postgresql/17/main
Looking for directories named {version}-pg_dump to restore from...
Current PostgreSQL Major Version: 17
All dump files:
14-pg_dump
Choosing newest dump file before current version (17): 14-pg_dump
Restoring from dump file 14-pg_dump, using command:
pg_restore -U metacat -h metacatbrooke-postgresql-hl -d metacat --format=directory --jobs=20 /bitnami/postgresql/14-pg_dump
FINISHED restoring from dump file 14-pg_dump; exiting initContainer...
````

### Subsequent Startups - No Action Required

```text
$ kc logs -f pod/metacatbrooke-0 -c pgupgrade

Checking if a PostgreSQL upgrade is necessary...
Found version 17; checking if pg_restore needed...
Result of psql -h metacatbrooke-postgresql-hl -U metacat -d metacat -c "\dt":
List of relations
Schema |         Name          | Type  |  Owner
--------+-----------------------+-------+---------
public | access_log            | table | metacat
public | checksums             | table | metacat
public | harvest_detail_log    | table | metacat
public | harvest_log           | table | metacat
public | harvest_site_schedule | table | metacat
public | identifier            | table | metacat
public | index_event           | table | metacat
public | node_id_revisions     | table | metacat
public | quota_usage_events    | table | metacat
public | scheduled_job         | table | metacat
public | scheduled_job_params  | table | metacat
public | smmediatypeproperties | table | metacat
public | smreplicationpolicy   | table | metacat
public | smreplicationstatus   | table | metacat
public | systemmetadata        | table | metacat
public | version_history       | table | metacat
public | xml_access            | table | metacat
public | xml_catalog           | table | metacat
public | xml_documents         | table | metacat
public | xml_relation          | table | metacat
public | xml_revisions         | table | metacat
(21 rows)
Metacat tables found in /bitnami/postgresql/17/main; will NOT do a pg_restore. Exiting initContainer...
```
