# Metacat Helm Chart

Metacat is repository software for preserving data and metadata (documentation about data) that helps scientists find, understand and effectively use data sets they manage or that have been created by others. For more details, see https://github.com/NCEAS/metacat

> [!IMPORTANT]
> ### Before You Start:
> 1. After you have read the details below, [this checklist](./admin/MetacatQuickRef.md) may be helpful in guiding you through the necessary installation steps.
>
>
> 2. If you are considering **migrating an existing Metacat installation to Kubernetes**, note that ***before starting a migration, you must have a fully-functioning installation of Metacat version 2.19, running with PostgreSQL version 14. Migrating from other versions of Metacat and/or PostgreSQL is not supported.*** See [this checklist](./admin/MetacatQuickRef.md) for the necessary migration steps.
>
>
> 3. If you are upgrading from a previous Helm Chart major version (e.g. from chart v1.2.0 to chart v.2.0.0), first check the [Metacat Release Notes](../RELEASE-NOTES.md) to see if this involves a change in the major version of the PostgreSQL application deployed by the included Bitnami PostgreSQL sub-chart. If it does, manual intervention may be required -- see the [Major Version Upgrades](#major-version-upgrades) section.
>
>
> 4. This deployment does not currently work on Apple Silicon machines (e.g. in Rancher Desktop), because the official Docker image for at least one of the dependencies (RabbitMQ) doesn't yet work in that environment.

---

## Table of Contents -- [Metacat Helm Chart](#metacat-helm-chart)
- [Introduction](#introduction)
- [TL;DR](#tldr)
- [Prerequisites](#prerequisites)
- [Installing the Chart](#installing-the-chart)
- [Uninstalling the Chart](#uninstalling-the-chart)
- [Major Version Upgrades](#major-version-upgrades)
- [Parameters](#parameters)
- [Configuration and installation details](#configuration-and-installation-details)
  - [Metacat Application-Specific Properties](#metacat-application-specific-properties-1)
  - [Secrets](#secrets)
- [Persistence](#persistence)
- [Networking and Certificates](#networking-and-certificates)
- [Setting up a TLS Certificate for HTTPS Traffic](#setting-up-a-tls-certificate-for-https-traffic)
- [Setting up Certificates for DataONE Replication](#setting-up-certificates-for-dataone-replication)
- [Appendices](#appendices)
  - [Appendix 1: Self-Signing TLS Certificates for HTTPS Traffic](#appendix-1-self-signing-tls-certificates-for-https-traffic)
  - [Appendix 2: Self-Signing Certificates for Testing Mutual Authentication](#appendix-2-self-signing-certificates-for-testing-mutual-authentication)
  - [Appendix 3: Troubleshooting Mutual Authentication](#appendix-3-troubleshooting-mutual-authentication)
  - [Appendix 4: Debugging and Logging](#appendix-4-debugging-and-logging)
  - [Appendix 5: Initial Creation of a PostgreSQL Cluster using CloudNative PG](#appendix-5-initial-creation-of-a-postgresql-cluster-using-cloudnative-pg)

---

## Introduction

This chart deploys a [Metacat](https://github.com/NCEAS/metacat) deployment on a [Kubernetes](https://kubernetes.io) cluster, using the [Helm](https://helm.sh) package manager.

## TL;DR

> [!NOTE]
> This chart assumes you have a pre-existing PostgreSQL database (deployed either within or outside your Kubernetes cluster). We recommend CloudNative PG - see [Appendix 5](#appendix-5-initial-creation-of-a-postgresql-cluster-using-cloudnative-pg) to create a PostgreSQL cluster.

1. You should not need to edit much in [values.yaml](./values.yaml), but you can look at the contents of the values override files (like the ones in the [./examples directory](./examples)), to see which settings typically need to be overridden (e.g. your postrges connection params, plus any values that contain the release name). Save your settings in a yaml file, e.g: `/path/to/your/values-overrides.yaml`
2. Add your credentials to [./admin/secrets.yaml](helm/admin/secret--metacat.yaml), and add to cluster:

    ```shell
    $ vim helm/admin/secret--metacat.yaml    ## follow the instructions in this file
    ```

3. Deploy

    ```shell
    $ helm upgrade --install  <myreleasename>  -n <mynamespace> oci://ghcr.io/nceas/charts/metacat \
                   --version <version-here>  -f  /path/to/your/values-overrides.yaml
    ```

To access Metacat, you'll need to create a mapping between your ingress IP address (found by:
`kubectl describe ingress | grep "Address:"`) and your metacat hostname. Do this either by adding a
permanent DNS record for everyone to use, or by adding a line to the `/etc/hosts` file on your
local machine, providing temporary local access for your own testing. You should then be able to
access the application via http://your-host-name/metacat.

Read on for more in-depth information about the various installation and configuration options that
are available...

## Prerequisites

- Kubernetes 1.23.3+
- Helm 3.16.1+
- A pre-existing PostgreSQL database. We recommend using the [CloudNative PG Operator v1.27.0+](https://cloudnative-pg.io/), although any suitable PostgreSQL database may be used (deployed either within or outside your Kubernetes cluster).
- The Kubernetes open source community version of [the nginx ingress controller](https://kubernetes.github.io/ingress-nginx/) (see the [Networking section](#networking-and-certificates), below).

## Installing the Chart

To install the chart with the release name `myrelease`:

```shell
helm install myrelease oci://ghcr.io/nceas/charts/metacat --version <version-here>
```
(See a [list of available versions here](https://github.com/NCEAS/metacat/pkgs/container/charts%2Fmetacat))

This command deploys Metacat on the Kubernetes cluster in the default configuration that is defined by the parameters in the [values.yaml file](./values.yaml). The [Parameters](#parameters) section, below, lists the parameters that can be configured during installation.

You will need to override some of these default parameters.

> [!NOTE]
> Some settings need to be edited to match your chosen release name. See the instructions at the beginning of the [values.yaml](./values.yaml) file

This can be achieved by creating a YAML file that specifies only those values that need to be overridden, and providing that file as part of the helm install command. For example:

```shell
helm install myrelease oci://ghcr.io/nceas/charts/metacat --version <version-here> -f myValues.yaml
```
(where `myValues.yaml` contains only the values you wish to override.)

Parameters may also be provided on the command line to override those in [values.yaml](./values.yaml); e.g.

```shell
helm install myrelease oci://ghcr.io/nceas/charts/metacat --version [version-here]  \
                        --set database.existingSecret=myrelease-secrets
```

## Uninstalling the Chart

To uninstall/delete the `myrelease` deployment:

```shell
helm uninstall myrelease
```

The `helm uninstall` command removes all the Kubernetes components associated with the chart
(except for Secrets, PVCs and PVs) and removes the release.


## Major Version Upgrades

> [!IMPORTANT]
> If you are upgrading across **major** versions of the Metacat Helm chart (e.g. from chart 1.x.x to chart 2.x.x), always check the [Metacat Release Notes](../RELEASE-NOTES.md) to determine whether you will need to take specific actions before or during the upgrade (for example, if there is a change in the major version of the underlying **PostgreSQL** application).

### Upgrading from the Bitnami PostgreSQL sub-chart to the CloudNative PG Operator

The Metacat chart now assumes you will provide your own PostgreSQL instance, and the chart needs only its connection parameters. If you wish to use the CloudNative PG Operator to deploy your PostgreSQL cluster, follow the instructions in [Appendix 5: Initial Creation of a PostgreSQL Cluster using CloudNative PG](#appendix-5-initial-creation-of-a-postgresql-cluster-using-cloudnative-pg)

### Upgrading between other major versions of PostgreSQL Using the CloudNative PG Operator

These scenarios will be covered in future releases. In the meantime, please refer to the [CloudNative PG documentation](https://cloudnative-pg.io/documentation/current/postgres_upgrades/).


## Parameters

### Global Properties Shared Across Sub-Charts Within This Deployment

| Name                                                 | Description                                                                 | Value                                 |
| ---------------------------------------------------- | --------------------------------------------------------------------------- | ------------------------------------- |
| `global.metacatExternalBaseUrl`                      | Metacat base url accessible from outside cluster.                           | `https://localhost/`                  |
| `global.d1ClientCnUrl`                               | The url of the CN; used to populate metacat's 'D1Client.CN_URL'             | `https://cn.dataone.org/cn`           |
| `global.metacatAppContext`                           | The application context to use                                              | `metacat`                             |
| `global.storageClass`                                | default name of the storageClass to use for PVs                             | `local-path`                          |
| `global.ephemeralVolumeStorageClass`                 | Optional global storageClass override                                       | `""`                                  |
| `global.sharedVolumeSubPath`                         | The subdirectory of the metacat data volume to mount                        | `""`                                  |
| `global.dataone-indexer.enabled`                     | Enable the dataone-indexer sub-chart                                        | `true`                                |
| `global.includeMetacatUi`                            | Enable or disable the MetacatUI sub-chart.                                  | `true`                                |
| `global.metacatUiIngressBackend.enabled`             | Enable or disable MetacatUI support via Ingress                             | `false`                               |
| `global.metacatUiIngressBackend.service.name`        | MetacatUI service name (ignored if 'global.includeMetacatUi: true')         | `${METACATUI_RELEASE_NAME}-metacatui` |
| `global.metacatUiIngressBackend.service.port.number` | Port for MetacatUI service (used only if 'global.includeMetacatUi: false')  | `80`                                  |
| `global.metacatUiThemeName`                          | MetacatUI theme name to use. (used only if 'global.includeMetacatUi: true') | `knb`                                 |
| `global.metacatUiWebRoot`                            | The url root to be appended after the MetacatUI baseUrl.                    | `/`                                   |

### Metacat Application-Specific Properties

| Name                              | Description                                                     | Value               |
| --------------------------------- | --------------------------------------------------------------- | ------------------- |
| `metacat.application.context`     | see global.metacatAppContext                                    | `metacat`           |
| `metacat.auth.administrators`     | A semicolon-separated list of admin ORCID iDs                   | `""`                |
| `metacat.database.connectionURI`  | postgres database URI (or blank if using CloudNative PG)        | `""`                |
| `metacat.guid.doi.enabled`        | Allow users to publish Digital Object Identifiers at doi.org?   | `true`              |
| `metacat.server.port`             | The http port exposed externally, if NOT using the ingress      | `""`                |
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

### PostgreSQL Database Connection Parameters

| Name                      | Description                                                       | Value     |
| ------------------------- | ----------------------------------------------------------------- | --------- |
| `database.existingSecret` | REQUIRED Name of Secret holding the database username & password  | `""`      |
| `database.dbName`         | The name of the PostgreSQL database to connect to                 | `metacat` |
| `database.serviceName`    | (REQUIRED if DB on k8s) name of the Service exposing the database | `""`      |
| `database.port`           | Override default database port (5432) - only if not using CNPG    | `5432`    |

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

The parameters in the [Metacat Application-Specific Properties](#metacat-application-specific-properties) section, above, map to the values required by Metacat at runtime. For more information please refer to the [Metacat Properties](https://knb.ecoinformatics.org/knb/docs/metacat-properties.html) section of the [Metacat Administrators' Guide](https://knb.ecoinformatics.org/knb/docs/).

### Secrets

Secret parameters (such as login credentials, auth tokens, private keys etc.) should be installed as kubernetes Secrets in the cluster. The files [admin/secrets.yaml](helm/admin/secret--metacat.yaml) and [admin/cloudnative-pg-secret.yaml](helm/admin/secret--cloudnative-pg.yaml) provide templates that you can complete and apply using `kubectl apply -f <filename>` -- for details, see the instructions in the comments inside those files. Please remember to NEVER ADD UNENCRYPTED SECRETS TO GITHUB!

> [!IMPORTANT]
> The names of the deployed Secrets include the release name as a prefix
> (e.g. `myrelease-metacat-secrets`), so it's important to ensure that the secrets name matches
> the release name referenced whenever you use `helm` commands.

## User Interface

The Metacat helm chart also installs [MetacatUI](https://nceas.github.io/metacatui/), which is included as a sub-chart. The MetacatUI sub-chart is highly configurable, and can be used with included themes, or you can provide your own custom theme, mounted on a PVC. At a minimum, you
should provide values for [the `global` properties](#global-properties-shared-across-sub-charts-within-this-deployment). More information can be found in the [MetacatUI README](https://github.com/NCEAS/metacatui/tree/develop/helm#readme).

If you wish to disable the subchart altogether, set `global.includeMetacatUi: false` and provide your own MetacatUI installation. deployed separately. Note that you can use the values in `global.metacatUiIngressBackend` to configure the ingress for your separate MetacatUI installation; see the documentation in [Values.yaml](./Values.yaml) for details.

## Persistence

Persistent Volume Claims are used to keep the data across deployments. See the [Parameters](#parameters) section to configure the PVCs or to disable persistence.

The Metacat image stores the Metacat data and configurations on a PVC mounted at the `/var/metacat` path in the metacat container.

Details of the `dataone-indexer` sub-chart PV/PVC requirements can be found in the [dataone-indexer repository](https://github.com/DataONEorg/dataone-indexer). Note that DataONE Indexer needs read access to the same PVC used by Metacat for its file-storage.

Details of the `MetacatUI` sub-chart (optional) PV/PVC requirements can be found in the [MetacatUI README](https://github.com/NCEAS/metacatui/tree/develop/helm#readme).


## Networking and Certificates

By default, the chart will install an [Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/) (see the `ingress.*` parameters under [Networking & Monitoring](#networking--monitoring)), which will expose HTTP and/or HTTPS routes from outside the cluster to the Metacat application within the cluster. Note that your cluster must have an Ingress controller in order for this to work.

> [!TIP]
>If you have admin access, you can inspect available Ingress classes in your cluster using: `$ kubectl get ingressclasses`
>
> We strongly recommend that you use the Kubernetes open source community version of [the nginx ingress controller](https://kubernetes.github.io/ingress-nginx/). You can install it as follows:

```shell
$  helm upgrade --install ingress-nginx ingress-nginx \
                --repo https://kubernetes.github.io/ingress-nginx \
                --namespace ingress-nginx --create-namespace
```

Note that there are significant differences between the community version of [the nginx ingress controller](https://kubernetes.github.io/ingress-nginx/) and the one provided by the NGINX company. This helm chart relies on the functionality of the community version; full functionality may not be available if you choose an alternative.


### Setting up a TLS Certificate for HTTPS Traffic

HTTPS traffic is served on port 443 (a.k.a. the "SSL" port), and requires the ingress to have access to a TLS certificate and private key. A certificate signed by a trusted Certificate Authority is needed for public servers, or you can create your own self-signed certificate for development purposes - see [Appendix 1](#appendix-1-self-signing-tls-certificates-for-https-traffic) below, for self-signing instructions.

Once you have obtained the server certificate and private key, you can add them to the Kubernetes secrets, as follows (creates a Secret named `tls-secret`, assuming the server certificate and private key are named `server.crt` and `server.key`):

```shell
kubectl create secret tls tls-secret --key server.key --cert server.crt
```

Then simply tell the ingress which secret to use:

```yaml
ingress:
  tls:
    - hosts:
        # hostname is auto-populated from the value of global.metacatExternalBaseUrl
        - knb.test.dataone.org
      secretName: tls-secret
```

> [!TIP]
> You can save time and reduce complexity by using a certificate manager service. For example, our NCEAS k8s clusters include [a cert-manager service](https://github.com/DataONEorg/k8s-cluster/blob/main/authentication/LetsEncrypt.md) that constantly watches for Ingress modifications, and updates letsEncrypt certificates automatically, so this step is as simple as ensuring the ingress includes:
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
> ...and a tls cert will be created and applied automatically, matching the hostname defined in the `tls:` section. It will be created in a new secret: `ingress-nginx-tls-cert`, in the ingress' namespace


### Setting up Certificates for DataONE Replication

For full details on becoming part of the DataONE network, see the [Metacat Administrator's Guide](https://knb.ecoinformatics.org/knb/docs/dataone.html) and [Authentication and Authorization in DataONE](https://releases.dataone.org/online/api-documentation-v2.0.1/design/AuthorizationAndAuthentication.html).

DataONE Replication relies on mutual authentication with x509 client-side certs. As a DataONE Member Node (MN) or Coordinating Node (CN), your metacat instance will act as both a server and as a client, at different times during the replication process. It is therefore necessary to configure certificates and settings for both these roles.

#### Prerequisites
1. First make sure you have the Kubernetes version of the [nginx ingress installed](#networking-and-certificates)
2. Ensure [HTTPS access is set up](#setting-up-a-tls-certificate-for-https-traffic) and working correctly. This allows other nodes, acting as "clients" to verify your server's identity during mutual authentication.
3. Download a copy of the **DataONE Certificate Authority (CA) certificate chain**. This enables your node (when acting as server) to verify that other nodes' client certificates were signed by the DataONE Certificate Authority.
   1. DataONE **Production** CA Chain: [DataONEProdCAChain.crt](https://raw.githubusercontent.com/DataONEorg/ca/main/DataONEProdCAChain.crt)
   2. DataONE **Test** CA Chain: [DataONETestCAChain.crt](https://raw.githubusercontent.com/DataONEorg/ca/main/DataONETestCAChain.crt)
4. From the DataONE administrators ([support@dataone.org](mailto:support@dataone.org)), obtain a **Client Certificate** (sometimes referred to as a **DataONE Node Certificate**) that uniquely identifies your Metacat instance. This allows another node (acting as server) to verify your node's identity (acting as "client") during mutual authentication. The client certificate contains sensitive information, and must be kept private and secure.

#### Install the CA Chain

* Create the Kubernetes Secret (named `d1-ca-chain`) to hold the ca chain (e.g. assuming it's in a file named `DataONEProdCAChain.crt`):

  ```shell
  kubectl create secret generic d1-ca-chain --from-file=ca.crt=DataONEProdCAChain.crt
  # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
  ```

#### Install the Client Certificate

* Create the Kubernetes Secret (named `<yourReleaseName>-d1-client-cert`) to hold the Client Certificate, identified by the key `d1client.crt` (e.g. assuming the cert is in a file named `urn_node_TestNAME.pem`):

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

3. Ensure you have already defined a value for the shared secret that will enable metacat to verify the validity of incoming requests. The secret should be defined [in metacat Secrets](#secrets), identified by the key: `METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY`.
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

The output will be a server certificate file named `server.crt`, and a private key file named `server.key`. For the `${HOST}`, you can use `localhost`, or your machine's real hostname.

Alternatively, you can use any other valid hostname, but you'll need to add an entry to your `/etc/hosts` file to map it to your localhost IP address (`127.0.0.1`) so that your browser can resolve it; e.g.:

```shell
# add entry in /etc/hosts
127.0.0.1       myHostName.com
```

Whatever hostname you are using, don't forget to set the `global.metacatExternalBaseUrl` accordingly, in `values.yaml`!

## Appendix 2: Self-Signing Certificates for Testing Mutual Authentication

> [!NOTE]
> For development and testing purposes only!**
>
> Also see the [Kubernetes nginx documentation](https://kubernetes.github.io/ingress-nginx/examples/PREREQUISITES/#client-certificate-authentication)

Assuming you already have a [server certificate installed](#setting-up-a-tls-certificate-for-https-traffic) (either signed by a trusted CA or [self-signed](#appendix-1-self-signing-tls-certificates-for-https-traffic) for development & testing), you can create your own self-signed Mutual Auth Client certificate and CA certificate as follows:

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

If you're having trouble getting Mutual Authentication working, you can run metacat in debug mode and view the logs (see [Appendix 4](#appendix-4-debugging-and-logging) for details).

If you see the message: `X-Proxy-Key is null or blank`, it means the nginx ingress has not been set up correctly (see [Setting up Certificates for DataONE Replication](#setting-up-certificates-for-dataone-replication)).

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
> `<your-secret-here>` is the plaintext value associated with the key `METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY` in your secret `<releaseName>-metacat-secrets` -- ensure it has been set correctly!

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
> you can also temporarily change logging settings without needing to upgrade or re-install the application, by editing the log4J configuration ConfigMap:
>
> `kc edit configmaps <releaseName>-metacat-configfiles`
>
> (look for the key `log4j2.k8s.properties`). The config is automatically reloaded every `monitorInterval` seconds.
>
> **Note** that these edits will be overwritten next time you do a `helm install` or `helm upgrade`!

2. enables remote Java debugging via port 5005. You will need to forward this port, in order to access it on localhost:

    ```shell
    $ kubectl  port-forward  --namespace myNamespace  pod/mypod-0  5005:5005
    ```
> [!TIP]
> For the **indexer**, you can also set the debug flag in `values.yaml` (Note that this only sets the logging level to DEBUG; it does **not** enable remote debugging for the indexer):
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

## Appendix 5: Initial Creation of a PostgreSQL Cluster using CloudNative PG

> [!NOTE]
> This is a separate process from installing the Metacat Helm chart, and only needs to be done once. See [these important warnings](https://github.com/DataONEorg/dataone-cnpg?tab=readme-ov-file#secrets--credentials) about not changing credentials!

We recomment using the [CloudNative PG](https://cloudnative-pg.io/) operator to install and manage a PostgreSQL cluster for use with Metacat. The operator automates many of the tasks involved in installing, configuring, and managing a PostgreSQL cluster, including backups, failover, and scaling. Installing CNPG is beyond the scope of this document, but you can find detailed instructions in the [DataONE K8s Cluster documentation](https://github.com/DataONEorg/k8s-cluster/blob/main/postgres/postgres.md#cloudnativepg-operator-installation).

Once the CNPG operator is installed, you can create a PostgreSQL cluster easily, using the [DataONE CNPG Helm Chart](https://github.com/DataONEorg/dataone-cnpg).

### Before installing the chart

1. Ensure you have set the correct values for the database name and the database owner (username) in your values overrides. (If you deploy with the wrong values, it's difficult to change them after the fact).
2. If you are planning to provide your own database password instead of having CNPG create one for you (e.g. if you're migrating data from an existing database), you can use [this yaml template](helm/admin/secret--cloudnative-pg.yaml) to create your secret - see the instructions in the file.
3. Finally, double-check all your values, and then install the chart, as follows:

### EITHER: Fresh Metacat Install with an Empty Database

```shell
$ helm install <releasename> oci://dataoneorg.github.io/dataone-cnpg --version <version> \
        -f ./examples/cnpg-cluster-empty-example.yaml  # or replace with your own file
```

### OR: Migrating Metacat from an Existing Database (e.g. Bitnami Postgres)

Follow the detailed instructions on ["Importing Data" in the DataONE CNPG README](https://github.com/DataONEorg/dataone-cnpg/blob/main/README.md#importing-data).
