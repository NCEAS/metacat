# Metacat Helm Chart

Metacat is repository software for preserving data and metadata (documentation about data) that
helps scientists find, understand and effectively use data sets they manage or that have been
created by others. For more details, see https://github.com/NCEAS/metacat

> ### Before You Start:
> 1. **This Metacat Helm chart is a beta feature**. It has been tested, and we believe it to be
>    working well, but it has not yet been used in production - so we recommend caution with this
>    early release. If you try it, [we'd love to hear your
>    feedback](https://www.dataone.org/contact/)!
>
>
> 2. If you are considering **migrating an existing Metacat installation to Kubernetes**, see
>    [Appendix 5](#appendix-5-migrating-to-kubernetes-from-an-existing-metacat-219-installation)
>    for important information
>
>
> 3. For non-public dataset support, see: [Setting up a Token and Optional CA certificate for
>    Indexer Access](#setting-up-a-token-and-optional-ca-certificate-for-indexer-access)
>
>
> 4. This deployment does not currently work on Apple Silicon machines (e.g. in Rancher Desktop),
>    because the official Docker image for at least one of the dependencies (RabbitMQ) doesn't yet
>    work in that environment.

---

- [Metacat Helm Chart](#metacat-helm-chart)
    * [TL;DR](#tldr)
    * [Introduction](#introduction)
    * [Prerequisites](#prerequisites)
    * [Installing the Chart](#installing-the-chart)
    * [Uninstalling the Chart](#uninstalling-the-chart)
    * [Parameters](#parameters)
    * [Configuration and installation details](#configuration-and-installation-details)
        + [Metacat Application-Specific Properties](#metacat-application-specific-properties-1)
        + [Secrets](#secrets)
    * [Persistence](#persistence)
    * [Networking, Certificates, and Auth Tokens](#networking-certificates-and-auth-tokens)
    * [Setting up a Token and Optional CA certificate for Indexer Access](#setting-up-a-token-and-optional-ca-certificate-for-indexer-access)
    * [Setting up a TLS Certificate for HTTPS Traffic](#setting-up-a-tls-certificate-for-https-traffic)
    * [Setting up Certificates for DataONE Replication](#setting-up-certificates-for-dataone-replication)
    * [Appendix 1: Self-Signing TLS Certificates for HTTPS Traffic](#appendix-1-self-signing-tls-certificates-for-https-traffic)
    * [Appendix 2: Self-Signing Certificates for Testing Mutual Authentication](#appendix-2-self-signing-certificates-for-testing-mutual-authentication)
    * [Appendix 3: Troubleshooting Mutual Authentication](#appendix-3-troubleshooting-mutual-authentication)
    * [Appendix 4: Debugging and Logging](#appendix-4-debugging-and-logging)
    * [Appendix 5: Migrating to Kubernetes from an Existing Metacat 2.19 Installation](#appendix-5-migrating-to-kubernetes-from-an-existing-metacat-219-installation)

---

## TL;DR
Starting in the root directory of the `metacat` repo:

1. You should not need to edit much in [values.yaml](./values.yaml), but you can look at the
   contents of the values overlay files (like
   [values-dev-cluster-example.yaml](./examples/values-dev-cluster-example.yaml)
   , for example), to see which settings typically need to be changed.


2. Add your credentials to [./admin/secrets.yaml](./admin/secrets.yaml), and add to cluster:

    ```shell
    $ vim helm/admin/secrets.yaml    ## follow the instructions in this file
    ```

3. Deploy

   (*Note: Your k8s service account must have the necessary permissions to get information about the
   resource `roles` in the API group `rbac.authorization.k8s.io`*).

    ```shell
    $ ./helm-install.sh  myreleasename  mynamespace  ./helm
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

- Kubernetes 1.23.4+
- Helm 3.14.0+
- PV provisioner support in the underlying infrastructure

## Installing the Chart

To install the chart with the release name `my-release`:

```shell
helm install my-release ./helm
```

This command deploys Metacat on the Kubernetes cluster in the default configuration. The
[Parameters](#parameters) section lists the parameters that can be configured during
installation.

> **Note**: Some settings need to be edited to include release name that you choose. See the
> [values.yaml](./values.yaml) file for settings that include `${RELEASE_NAME}`. The instructions
> at the beginning of [values.yaml](./values.yaml) suggest simple ways to achieve this.

Parameters may be provided on the command line to override those in values.yaml; e.g.

```shell
helm install my-release ./helm  --set postgres.auth.existingSecret=my-release-secrets
```

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

> **NOTE**: DELETING THE PVCs MAY DELETE ALL YOUR DATA AS WELL! Please be cautious!


## Parameters

### Global Properties Shared Across Sub-Charts Within This Deployment

| Name                                 | Description                                             | Value                             |
| ------------------------------------ | ------------------------------------------------------- | --------------------------------- |
| `global.passwordsSecret`             | The name of the Secret containing application passwords | `${RELEASE_NAME}-metacat-secrets` |
| `global.metacatAppContext`           | The application context to use                          | `metacat`                         |
| `global.storageClass`                | default name of the storageClass to use for PVs         | `local-path`                      |
| `global.ephemeralVolumeStorageClass` | Optional global storageClass override                   | `""`                              |
| `global.sharedVolumeSubPath`         | The subdirectory of the metacat data volume to mount    | `""`                              |

### Metacat Application-Specific Properties

| Name                              | Description                                                     | Value               |
| --------------------------------- | --------------------------------------------------------------- | ------------------- |
| `metacat.application.context`     | see global.metacatAppContext                                    | `metacat`           |
| `metacat.includeMetacatUi`        | Include MetacatUI in the same container as metacat              | `true`              |
| `metacat.auth.administrators`     | A semicolon-separated list of admin ORCID iDs                   | `""`                |
| `metacat.database.connectionURI`  | postgres database URI, or lave blank to use sub-chart           | `""`                |
| `metacat.guid.doi.enabled`        | Allow users to publish Digital Object Identifiers at doi.org?   | `true`              |
| `metacat.server.port`             | The http port exposed externally, if NOT using the ingress      | `""`                |
| `metacat.server.name`             | The hostname for the server, as exposed by the ingress          | `localhost`         |
| `metacat.solr.baseURL`            | The url to access solr, or leave blank to use sub-chart         | `""`                |
| `metacat.solr.coreName`           | The solr core (solr standalone) or collection name (solr cloud) | `""`                |
| `metacat.replication.logdir`      | Location for the replication logs                               | `/var/metacat/logs` |
| `metacat.index.rabbitmq.hostname` | the hostname of the rabbitmq instance that will be used         | `""`                |
| `metacat.index.rabbitmq.username` | the username for connecting to the RabbitMQ instance            | `metacat-rmq-guest` |

### OPTIONAL DataONE Member Node (MN) Parameters

| Name                                                          | Description                                                       | Value                                                    |
| ------------------------------------------------------------- | ----------------------------------------------------------------- | -------------------------------------------------------- |
| `metacat.cn.server.publiccert.filename`                       | optional cert(s) used to validate jwt auth tokens,                | `/var/metacat/pubcerts/DataONEProdIntCA.pem`             |
| `metacat.dataone.certificate.fromHttpHeader.enabled`          | Enable mutual auth with client certs                              | `false`                                                  |
| `metacat.dataone.autoRegisterMemberNode`                      | Automatically push MN updates to CN? (yyyy-MM-dd)                 | `2023-02-28`                                             |
| `metacat.D1Client.CN_URL`                                     | the url of the CN                                                 | `https://cn.dataone.org/cn`                              |
| `metacat.dataone.nodeId`                                      | The unique ID of your DataONE MN - must match client cert subject | `urn:node:CHANGE_ME_TO_YOUR_VALUE!`                      |
| `metacat.dataone.subject`                                     | The "subject" string from your DataONE MN client certificate      | `CN=urn:node:CHANGE_ME_TO_YOUR_VALUE!,DC=dataone,DC=org` |
| `metacat.dataone.nodeName`                                    | short name for the node that can be used in user interfaces       | `My Metacat Node`                                        |
| `metacat.dataone.nodeDescription`                             | What is the node's intended scope and purpose?                    | `Describe your Member Node briefly.`                     |
| `metacat.dataone.contactSubject`                              | registered contact for this MN                                    | `http://orcid.org/0000-0002-8888-999X`                   |
| `metacat.dataone.nodeSynchronize`                             | Enable Synchronization of Metadata to DataONE                     | `false`                                                  |
| `metacat.dataone.nodeSynchronization.schedule.year`           | sync schedule year                                                | `*`                                                      |
| `metacat.dataone.nodeSynchronization.schedule.mon`            | sync schedule month                                               | `*`                                                      |
| `metacat.dataone.nodeSynchronization.schedule.mday`           | sync schedule day of month                                        | `*`                                                      |
| `metacat.dataone.nodeSynchronization.schedule.wday`           | sync schedule day of week                                         | `?`                                                      |
| `metacat.dataone.nodeSynchronization.schedule.hour`           | sync schedule hour                                                | `*`                                                      |
| `metacat.dataone.nodeSynchronization.schedule.min`            | sync schedule minute                                              | `0/3`                                                    |
| `metacat.dataone.nodeSynchronization.schedule.sec`            | sync schedule second                                              | `10`                                                     |
| `metacat.dataone.nodeReplicate`                               | Accept and Store Replicas?                                        | `false`                                                  |
| `metacat.dataone.replicationpolicy.default.numreplicas`       | # copies to store on other nodes                                  | `0`                                                      |
| `metacat.dataone.replicationpolicy.default.preferredNodeList` | Preferred replication nodes                                       | `nil`                                                    |
| `metacat.dataone.replicationpolicy.default.blockedNodeList`   | Nodes blocked from replication                                    | `nil`                                                    |

### OPTIONAL (but Recommended) Site Map Parameters

| Name                            | Description                                                     | Value             |
| ------------------------------- | --------------------------------------------------------------- | ----------------- |
| `metacat.sitemap.enabled`       | Enable sitemaps to tell search engines which URLs are available | `false`           |
| `metacat.sitemap.interval`      | Interval (in milliseconds) between rebuilding the sitemap       | `86400000`        |
| `metacat.sitemap.location.base` | The first part of the URLs listed in sitemap_index.xml          | `/`               |
| `metacat.sitemap.entry.base`    | base URI of the dataset landing page, listed in the sitemap     | `/metacatui/view` |

### robots.txt file (search engine indexing)

| Name               | Description                                                          | Value |
| ------------------ | -------------------------------------------------------------------- | ----- |
| `robots.userAgent` | "User-agent:" defined in robots.txt file. Defaults to "*" if not set | `""`  |
| `robots.disallow`  | the "Disallow:" value defined in robots.txt file.                    | `""`  |

### Metacat Image, Container & Pod Parameters

| Name                           | Description                                                                  | Value                   |
| ------------------------------ | ---------------------------------------------------------------------------- | ----------------------- |
| `image.repository`             | Metacat image repository                                                     | `ghcr.io/nceas/metacat` |
| `image.pullPolicy`             | Metacat image pull policy                                                    | `IfNotPresent`          |
| `image.tag`                    | Overrides the image tag. Will default to the chart appVersion if set to ""   | `""`                    |
| `image.debug`                  | Specify if container debugging should be enabled (sets log level to "DEBUG") | `false`                 |
| `imagePullSecrets`             | Optional list of references to secrets in the same namespace                 | `[]`                    |
| `container.ports`              | Optional list of additional container ports to expose within the cluster     | `[]`                    |
| `serviceAccount.create`        | Should a service account be created to run Metacat?                          | `false`                 |
| `serviceAccount.annotations`   | Annotations to add to the service account                                    | `{}`                    |
| `serviceAccount.name`          | The name to use for the service account.                                     | `""`                    |
| `podAnnotations`               | Map of annotations to add to the pods                                        | `{}`                    |
| `podSecurityContext.enabled`   | Enable security context                                                      | `true`                  |
| `podSecurityContext.fsGroup`   | numerical Group ID for the pod                                               | `1000`                  |
| `securityContext.runAsNonRoot` | ensure containers run as a non-root user.                                    | `true`                  |
| `resources`                    | Resource limits for the deployment                                           | `{}`                    |
| `tolerations`                  | Tolerations for pod assignment                                               | `[]`                    |

### Metacat Persistence

| Name                        | Description                                                          | Value               |
| --------------------------- | -------------------------------------------------------------------- | ------------------- |
| `persistence.enabled`       | Enable metacat data persistence using Persistent Volume Claims       | `true`              |
| `persistence.storageClass`  | Storage class of backing PV                                          | `local-path`        |
| `persistence.existingClaim` | Name of an existing Persistent Volume Claim to re-use                | `""`                |
| `persistence.volumeName`    | Name of an existing Volume to use for volumeClaimTemplate            | `""`                |
| `persistence.subPath`       | The subdirectory of the volume (see persistence.volumeName) to mount | `""`                |
| `persistence.accessModes`   | PVC Access Mode for metacat volume                                   | `["ReadWriteMany"]` |
| `persistence.size`          | PVC Storage Request for metacat volume                               | `1Gi`               |

### Networking & Monitoring

| Name                                 | Description                                                   | Value            |
| ------------------------------------ | ------------------------------------------------------------- | ---------------- |
| `ingress.enabled`                    | Enable or disable the ingress                                 | `true`           |
| `ingress.className`                  | ClassName of the ingress provider in your cluster             | `traefik`        |
| `ingress.annotations`                | Annotations for the ingress                                   | `{}`             |
| `ingress.tls`                        | The TLS configuration                                         | `[]`             |
| `ingress.d1CaCertSecretName`         | Name of Secret containing DataONE CA certificate chain        | `d1-ca-chain`    |
| `service.enabled`                    | Enable another optional service in addition to headless svc   | `false`          |
| `service.type`                       | Kubernetes Service type. Defaults to ClusterIP if not set     | `LoadBalancer`   |
| `service.clusterIP`                  | IP address of the service. Auto-generated if not set          | `""`             |
| `service.ports`                      | The port(s) to be exposed                                     | `[]`             |
| `livenessProbe.enabled`              | Enable livenessProbe for Metacat container                    | `true`           |
| `livenessProbe.httpGet.path`         | The url path to probe.                                        | `/metacat/`      |
| `livenessProbe.httpGet.port`         | The named containerPort to probe                              | `metacat-web`    |
| `livenessProbe.initialDelaySeconds`  | Initial delay seconds for livenessProbe                       | `45`             |
| `livenessProbe.periodSeconds`        | Period seconds for livenessProbe                              | `15`             |
| `livenessProbe.timeoutSeconds`       | Timeout seconds for livenessProbe                             | `10`             |
| `readinessProbe.enabled`             | Enable readinessProbe for Metacat container                   | `true`           |
| `readinessProbe.httpGet.path`        | The url path to probe.                                        | `/metacat/admin` |
| `readinessProbe.httpGet.port`        | The named containerPort to probe                              | `metacat-web`    |
| `readinessProbe.initialDelaySeconds` | Initial delay seconds for readinessProbe                      | `45`             |
| `readinessProbe.periodSeconds`       | Period seconds for readinessProbe                             | `5`              |
| `readinessProbe.timeoutSeconds`      | Timeout seconds for readinessProbe                            | `5`              |

### Postgresql Sub-Chart

| Name                                           | Description                                         | Value                                |
| ---------------------------------------------- | --------------------------------------------------- | ------------------------------------ |
| `postgresql.enabled`                           | enable the postgresql sub-chart                     | `true`                               |
| `postgresql.auth.username`                     | Username for accessing the database used by metacat | `metacat`                            |
| `postgresql.auth.database`                     | The name of the database used by metacat.           | `metacat`                            |
| `postgresql.auth.existingSecret`               | Secrets location for postgres password              | `${RELEASE_NAME}-metacat-secrets`    |
| `postgresql.auth.secretKeys.userPasswordKey`   | Identifies metacat db's account password            | `POSTGRES_PASSWORD`                  |
| `postgresql.auth.secretKeys.adminPasswordKey`  | Dummy value - not used (see notes):                 | `POSTGRES_PASSWORD`                  |
| `postgresql.primary.pgHbaConfiguration`        | PostgreSQL Primary client authentication            | `(See [values.yaml](./values.yaml))` |
| `postgresql.primary.extendedConfiguration`     | Extended configuration, appended to defaults        | `max_connections = 250`              |
| `postgresql.primary.persistence.enabled`       | Enable data persistence using PVC                   | `true`                               |
| `postgresql.primary.persistence.existingClaim` | Existing PVC to re-use                              | `""`                                 |
| `postgresql.primary.persistence.storageClass`  | Storage class of backing PV                         | `""`                                 |
| `postgresql.primary.persistence.size`          | PVC Storage Request for postgres volume             | `1Gi`                                |

### Tomcat Configuration

| Name                    | Description                                              | Value |
| ----------------------- | -------------------------------------------------------- | ----- |
| `tomcat.heapMemory.min` | minimum memory heap size for Tomcat (-Xms JVM parameter) | `""`  |
| `tomcat.heapMemory.max` | maximum memory heap size for Tomcat (-Xmx JVM parameter) | `""`  |

### dataone-indexer Sub-Chart

| Name                                                         | Description                                       | Value                                 |
| ------------------------------------------------------------ | ------------------------------------------------- | ------------------------------------- |
| `dataone-indexer.enabled`                                    | enable the dataone-indexer sub-chart              | `true`                                |
| `dataone-indexer.persistence.subPath`                        | The subdirectory of the volume to mount           | `""`                                  |
| `dataone-indexer.rabbitmq.auth.username`                     | set the username that rabbitmq will use           | `metacat-rmq-guest`                   |
| `dataone-indexer.rabbitmq.auth.existingPasswordSecret`       | location of rabbitmq password                     | `${RELEASE_NAME}-metacat-secrets`     |
| `dataone-indexer.solr.javaMem`                               | Java memory options to pass to the Solr container | `-Xms2g -Xmx2g`                       |
| `dataone-indexer.solr.customCollection`                      | name of the solr collection to use                | `metacat-index`                       |
| `dataone-indexer.solr.coreNames`                             | Solr core names to be created                     | `["metacat-core"]`                    |
| `dataone-indexer.solr.extraVolumes[0].name`                  | DO NOT EDIT - referenced by sub-chart             | `solr-config`                         |
| `dataone-indexer.solr.extraVolumes[0].configMap.name`        | see notes in values.yaml                          | `${RELEASE_NAME}-indexer-configfiles` |
| `dataone-indexer.solr.extraVolumes[0].configMap.defaultMode` | DO NOT EDIT                                       | `777`                                 |

Specify non-secret parameters in the default [values.yaml](./values.yaml), which will be used
automatically each time you deploy.

> **NOTE**: Once the chart is deployed, it is not possible to change the postgreSQL access
> credentials, such as usernames or passwords, nor is it possible to change the
> Metacat primary administrator password, using Helm. To change these application
> credentials after deployment, delete any persistent volumes (PVs) used by the relevant
> application (Metacat or PostgreSQL) and re-deploy.
>
> **Warning**: Setting a password will be ignored on new installations in cases when a previous
> PostgreSQL release was deleted through the helm command. In that case, the old PVC will have an
> old password, and setting it through helm won't take effect. Deleting persistent volumes (PVs)
> will solve the issue. Refer to [issue 2061](https://github.com/bitnami/charts/issues/2061) for
> more details

Parameters may be provided on the command line to override those in [values.yaml](./values.yaml);
for example:

```shell
helm install my-release ./helm  --set metacat.solr.baseURL=http://mysolrhost:8983/solr
```

Alternatively, a YAML file that specifies the override values for the parameters can be provided
while installing the chart. For example:

```shell
helm install my-release -f myValues.yaml ./helm
```

> **Tip**: You can also edit and use the default [values.yaml](./values.yaml)

## Configuration and installation details

### Metacat Application-Specific Properties

The parameters in the
[Metacat Application-Specific Properties](#metacat-application-specific-properties)
section, above, map to the values required by Metacat at runtime. For more information please refer
to the [Metacat Properties](https://knb.ecoinformatics.org/knb/docs/metacat-properties.html) section
of the [Metacat Administrators' Guide](https://knb.ecoinformatics.org/knb/docs/).

### Secrets

Secret parameters (such as login credentials, auth tokens, private keys etc.) should be installed as
kubernetes Secrets in the cluster. The file [admin/secrets.yaml](./admin/secrets.yaml) provides a
template that you can complete and apply using `kubectl` - see file comments for details. Please
remember to NEVER ADD SECRETS TO GITHUB!

> **Important**:
> 1. The deployed Secrets name includes the release name as a prefix,
> (e.g. `my-release-metacat-secrets`), so it's important to ensure that the secrets name matches
> the release name referenced whenever you use `helm` commands.
> 2. The parameter `postgresql.auth.existingSecret` in [values.yaml](./values.yaml) must be set to
> match the name of these installed secrets (which will change if the release name is changed).

## Persistence

Persistent Volume Claims are used to keep the data across deployments. See the
[Parameters](#parameters) section to configure the PVCs or to disable persistence for either
application.

The Metacat image stores the Metacat data and configurations on a PVC mounted at the `/var/metacat`
path in the metacat container.

The PostgreSQL image stores the database data at the `/bitbami/pgdata` path in its own container.

Details of the sub-chart PV/PVC requirements can be found in the [dataone-indexer
repository](https://github.com/DataONEorg/dataone-indexer)

## Networking, Certificates, and Auth Tokens

By default, the chart will install an
[Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/) (see the `ingress.*`
parameters under [Networking & Monitoring](#networking--monitoring)), which will expose HTTP
and HTTPS routes from outside the cluster to the Metacat application within the cluster. Note that
your cluster must have an Ingress controller in order for this to work.

> **Tip**: You can inspect available Ingress classes in your cluster using:
> `$ kubectl get ingressclasses`

We recommend using the Kubernetes open source community version of
[the nginx ingress](https://kubernetes.github.io/ingress-nginx/). You can install it as follows:

```shell
$  helm upgrade --install ingress-nginx ingress-nginx \
                --repo https://kubernetes.github.io/ingress-nginx \
                --namespace ingress-nginx --create-namespace
```

...and don't forget to set the `ingress.className` to `nginx` in your `values.yaml`.

### Setting up a Token and Optional CA certificate for Indexer Access

**IMPORTANT:** In order for Metacat 3.0.0 to function correctly, the
[dataone-indexer](#dataone-indexer-sub-chart) needs a valid authentication token, to enable
indexing for private datasets, via calls to metacat's DataONE API.

> Note that this is only an interim requirement; a future release of Metacat will remove the need
> for this auth token.
>
> If you are only evaluating metacat, you can do so without using a token, but
> note that only public datasets can be uploaded and searched; private datasets will not be
> supported without the token setup.

#### Prerequisites

1. [Contact DataONE administrators](https://www.dataone.org/contact/) for an authentication token, issued against the DataONE
   Certificate Authority (CA), that will be valid for one year.

   - if your Metacat site is already a DataONE member node, we will issue a token linked to your
     DataONE Node identity.

   - if your site is not a DataONE member node, we [encourage you to
     join](https://www.dataone.org/jointhenetwork/). Otherwise, we can issue a token linked to your administrator's ORCID iD.

        > **Tip:**  if you want a temporary auth token in order to evaluate Metacat's private
          dataset functionality, you can get a short-term auth token (valid for only 24 hours!),
          by logging into [the KNB
          website](https://knb.ecoinformatics.org), and navigating to "My Profile" -> "Settings" ->
          "Authentication Token".

2. Download a copy of the DataONE Intermediate CA certificate,
either for the Production or the Test environment, depending upon your needs:

   - [DataONE Production Intermediate CA
     Certificate](https://raw.githubusercontent.com/DataONEorg/ca/main/DataONEProdIntCA/certs/DataONEProdIntCA.pem)
   - [DataONE Test Intermediate CA
     Certificate](https://raw.githubusercontent.com/DataONEorg/ca/main/DataONETestIntCA/certs/DataONETestIntCA.pem)

       > **Note:** the DataONE Intermediate CA certificate is a single certificate, NOT a
         certificate chain!

#### Install the Token

- Install the token in a Kubernetes Secret named `<yourReleaseName>-indexer-token`,
  identified by the key: `DataONEauthToken`.
- For example, assuming the token is in a file `urn_node_TestNAME.jwt`:

      ```shell
      kubectl create secret generic <yourReleaseName>-indexer-token \
                                    --from-file=DataONEauthToken=urn_node_TestNAME.jwt
      ```

#### Install the CA Intermediate Certificate

- Install the cert in a Kubernetes ConfigMap named `<yourReleaseName>-d1-certs-public`,
  identified by the key: `DataONEProdIntCA.pem`.
- For example, assuming the token is in a file `DataONEProdIntCA.pem`:

      ```shell
      kubectl create configmap  <yourReleaseName>-d1-certs-public \
                                    --from-file=DataONEProdIntCA.pem=DataONEProdIntCA.pem
      ```
    > **Tip:**
      If you change the ConfigMap key from `DataONEProdIntCA.pem` to a different value, make
      sure that
      `metacat.cn.server.publiccert.filename` in values.yaml has a filename that matches the new
       key!
    >
    >  Also note that you may include more than one cert, if you need to authenticate requests from
       tokens issued by different CAs. See the documentation in values.yaml

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
# (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
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

### Setting up Certificates for DataONE Replication

For full details on becoming part of the DataONE network, see the [Metacat Administrator's Guide
](https://knb.ecoinformatics.org/knb/docs/dataone.html) and
[Authentication and Authorization in DataONE
](https://releases.dataone.org/online/api-documentation-v2.0.1/design/AuthorizationAndAuthentication.html)
.

DataONE Replication relies on mutual authentication with x509 client-side certs. As a DataONE
Member Node (MN) or Coordinating Node (CN), your metacat instance will act as both a server and
as a client, at different times during the replication process. It is therefore necessary to
configure certificates and settings for both these roles.

#### Prerequisites
1. First make sure you have the Kubernetes version of the
   [nginx ingress installed](#networking-certificates-and-auth-tokens)
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
4. From the DataONE administrators ([support@dataone.org](mailto:support@dataone.org)), obtain a **Client Certificate**,
   that uniquely identifies your Metacat instance. This allows another node (acting as server)
   to verify your node's identity (acting as "client") during mutual authentication. The client
   certificate contains sensitive information, and should be kept private.

#### Install the CA Chain

1. Create the Kubernetes Secret (named `d1-ca-chain`) to hold the ca chain
   (e.g. assuming it's in a file named `DataONEProdCAChain.crt`):

    ```shell
    kubectl create secret generic d1-ca-chain --from-file=ca.crt=DataONEProdCAChain.crt
    # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
    ```

2. Run the [`configure-nginx-mutual-auth.sh` script](./admin/configure-nginx-mutual-auth.sh).
  This will configure your nginx ingress controller to add a shared secret header that Metacat
  requires for added security. Ensure you have already defined a value for this shared secret,
  named `METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY`, [in metacat Secrets](#secrets).

#### Install the Client Certificate

   1. Create the Kubernetes Secret (named `<yourReleaseName>-d1-client-cert`) to hold the Client
      Certificate, identified by the key `d1client.crt` (e.g. assuming the cert is in a file
      named `urn_node_TestNAME.pem`):

      ```shell
      kubectl create secret generic <yourReleaseName>-d1-client-cert \
                                    --from-file=d1client.crt=urn_node_TestNAME.pem
      # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
      ```

#### Set the correct parameters in `values.yaml`

1. Enable the shared secret header

    ```yaml
    metacat:
      dataone.certificate.fromHttpHeader.enabled: true
    ```

2. set the CA secret name

    ```yaml
    ingress:
      className: "nginx"
      d1CaCertSecretName: d1-ca-chain
    ```

3. Finally, re-install or upgrade to apply the changes

See [Appendix 3](#appendix-3-troubleshooting-mutual-authentication) for help with troubleshooting

---

## Appendices

## Appendix 1: Self-Signing TLS Certificates for HTTPS Traffic

> **NOTE: For development and testing purposes only!**
>
> Also see the [Kubernetes nginx documentation](<https://kubernetes.github>.
> io/ingress-nginx/user-guide/tls)

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

> **NOTE: For development and testing purposes only!**
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
          # more lines above
          nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true"
          nginx.ingress.kubernetes.io/auth-tls-secret: default/d1-ca-chain
          ## above may differ for you. Format is: <namespace>/<ingress.d1CaCertSecretName>
          nginx.ingress.kubernetes.io/auth-tls-verify-client: optional_no_ca
          nginx.ingress.kubernetes.io/auth-tls-verify-depth: "10"
    ```

    If you don't see these, or they are incorrect, check values.yaml for:

    ```yaml
      metacat:
        dataone.certificate.fromHttpHeader.enabled: #true for mutual auth

      ingress:
        tls: # needs to have been set up properly [ref 1]

        d1CaCertSecretName: # needs to match secret name holding your ca cert chain [ref 2]
    ```
    - *[[ref 1]](#setting-up-a-tls-certificate-for-https-traffic)*
    - *[[ref 2]](#install-the-ca-chain)*

2. then check the configmaps for the ingress controller:

    ```shell
    kc get -n ingress-nginx configmap ingress-nginx-controller -o yaml
    # this is the ingress controller's namespace. Typically ingress-nginx
    ```

    and look for:

    ```yaml
      data:
        allow-snippet-annotations: "true"
        proxy-set-headers: default/ingress-nginx-custom-headers
        ## above may differ for you: <namespace>/ingress-nginx-custom-headers
    ```

    If you don't see these, or they are incorrect, make sure you have run the
    [`configure-nginx-mutual-auth.sh` script](./admin/configure-nginx-mutual-auth.sh) script
    successfully, and you have provided it with the correct namespaces. (Run it with no additional
    parameters to see usage notes)

3. verifying the `ingress-nginx-custom-headers`:

    ```shell
    kc get configmaps ingress-nginx-custom-headers -n myNameSpace -o yaml
    # this one's in your own namespace
    ```

    and look for:

    ```yaml
      data:
        X-Proxy-Key: yourSecretProxyKey
    ```

    ...where yourSecretProxyKey is populated from the secret with the key
    `METACAT_DATAONE_CERT_FROM_HTTP_HEADER_PROXY_KEY` - make sure you defined this in your Secrets
    and applied them using [admin/secrets.yaml](./admin/secrets.yaml)

4. You can also view the nginx ingress logs using:

    ```shell
      NS=ingress-nginx    # this is the ingress controller's namespace. Typically ingress-nginx
      kubectl logs -n ${NS} -f $(kc get pods -n ${NS} | grep -v "NAME" | sed -e 's/\ [0-9].*//g')
    ```

    ...and look for entries like: `Error reading ConfigMap`

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
   > **Tip:** you can also temporarily change logging settings without needing to
   > upgrade or re-install the application, by editing the log4J configuration
   > ConfigMap:
   >
   >   $ `kc edit configmaps <releaseName>-metacat-configfiles`
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
   **Tip:**
   > For the **indexer**, you can also set the debug flag in `values.yaml` (Note that this
     only sets the logging level to DEBUG; it does **not** enable remote debugging for the indexer):

    ```yaml
    dataone-indexer:
      image:
        debug: true
    ```

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
  $ kubectl logs -p -f metacatknb-0
```

Logs from an `initContainer`:

```shell
  $ kubectl logs -f <specific-pod-name> -c <init-container-name>

  # example: Metacat's `init-solr-metacat-dep` initContainer logs
  $ kubectl logs -f metacatknb-0 -c init-solr-metacat-dep
```

## Appendix 5: Migrating to Kubernetes from an Existing Metacat 2.19 Installation

The following additional information may be helpful if you wish to migrate your data from an
existing Metacat installation, so that it can be deployed in Kubernetes. The steps below were
required to migrate the existing Metacat 2.19 deployment (data and files) at
https://knb.ecoinformatics.org, to run on our development Kubernetes cluster, here at NCEAS.

### Important Notes - Before You Start

> 1. By default, the Metacat helm chart has a MetacatUI installation that runs in the same Tomcat
>    container, on the same pod as Metacat, primarily for evaluation purposes. For a production
>    installation, therefore, we strongly recommend deploying your own version of MetacatUI, in a
>    dedicated pod, and configuring it to use the Metacat back-end internally, via the Kubernetes
>    headless Service provided. That process is not described here, but (non-k8s) MetacatUI
>    installation instructions can be found in the [MetacatUI GitHub
>    repository](https://nceas.github.io/metacatui/install/). The default co-deployment can be
>    disabled by setting `metacat.includeMetacatUi: false` in values.yaml
>
>
> 2. Before starting the migration, you must have a fully-functioning installation of **Metacat
>    version 2.19**, running with **PostgreSQL version 14**. Migrating from other versions of
>    Metacat and/or PostgreSQL is not supported

### Assumptions
* You have a working knowledge of Kubernetes deployment, including working with yaml files, helm
  and kubectl commands
* You have `envsubst` installed
* You have your kubectl context set for the target deployment location

### Steps

1. Edit the metacat secrets file and add to your cluster

   Add your credentials to [./admin/secrets.yaml](./admin/secrets.yaml), and add to the cluster
    ```shell
    # knb example
     RELEASE_NAME=metacatknb envsubst <  secrets_KNB_NOCOMMIT.yaml | kubectl apply -n knb -f -
    ```

2. Copy the existing data to a ceph subvolume

   * See the [DataONEOrg/k8s-cluster
     repo](https://github.com/DataONEorg/k8s-cluster/blob/main/storage/storage.md) for more
     detail and examples.

   1. Create a cephfs subvolume, and rsync the existing data there from the knb host. Our subvolume
      has the following directory structure:

       ```shell
       /mnt/ceph/
       └── repos
           └── knb
               ├── metacat
               └── postgresql
                   └── 14
                      └── main
       ```

    2. Get information about volumes, for use in values.yaml
       ```shell
       # Get sizes for volumes
       $ du -sh metacat postgresql
       5.6T metacat
       255.4G postgresql

       # Get group ids for volumes
       $ stat -c %g  /mnt/ceph/repos/knb/metacat/
       997
       $ stat -c %g  /mnt/ceph/repos/knb/postgresql/
       114
       ```

   3. Create the secret needed to mount the volume:

      1. Create a yaml file containing your credentials
         ```yaml
           # cephSecretFile.yaml - credentials needed to mount ceph subvolume
           apiVersion: v1
           kind: Secret
           metadata:
             name: csi-cephfs-metacatknb-pdg-subvol
             namespace: ceph-csi-cephfs
           type: Opaque
           data:
             userID: <your base64-encoded value here>
             userKey: <your base64-encoded value here>
         ```

         * **VERY IMPORTANT:**
             * for the userID, omit the “client.” from the beginning of the user before base64
               encoding - eg: if your user is `client.k8s-dev-metacatknb-subvol-user`, use only
               `k8s-dev-metacatknb-subvol-user`
             * use echo -n when encoding; i.e:
               ```shell
               echo -n userID      |  base64
               echo -n mypassword  |  base64
               ```
      2. Create the secret (`kubectl apply -f cephSecretFile.yaml`)

3. Create a Persistent Volume (PV) for the metacat data directory at `/mnt/ceph/repos/knb/metacat`

   ```yaml
    ## Create a PV pointing to the metacat data directory at /mnt/ceph/repos/knb/metacat
    apiVersion: v1
    kind: PersistentVolume
    metadata:
      name: &pv-name cephfs-metacatknb-metacat-varmetacat
    spec:
      accessModes:
      - ReadWriteMany
      capacity:
        storage: 10Ti
      csi:
        driver: cephfs.csi.ceph.com
        nodeStageSecretRef:
          # node stage secret name
          name: csi-cephfs-metacatknb-pdg-subvol
          # node stage secret namespace where above secret is created
          namespace: ceph-csi-cephfs
        volumeAttributes:
          clusterID: 8aa4d4a0-a209-11ea-baf5-ffc787bfc812
          fsName: cephfs
          rootPath: /volumes/pdg-subvol-group/pdg-subvol/a5c90f20-f824-4ce9-b175-6685d7846520/repos/knb/metacat
          staticVolume: "true"
        volumeHandle: *pv-name
      persistentVolumeReclaimPolicy: Retain
      storageClassName: csi-cephfs-sc
      volumeMode: Filesystem
   ```

4. Create a Persistent Volume (PV) for the PostgreSQL data directory at
   `/mnt/ceph/repos/knb/postgresql`

   ```yaml
   ## Create a PV pointing to the PostgreSQL data directory at /mnt/ceph/repos/knb/postgresql
   ## For the postgres PV, include a label, so the Bitnami postgres chart can match to this.
   ## See metacat values.yaml: postgresql.primary.persistence.selector.matchLabels
   ##
    apiVersion: v1
    kind: PersistentVolume
    metadata:
      name: &pv-name cephfs-metacatknb-metacat-postgresdata
    labels:
      metacatVolumeName: *pv-name
    spec:
      accessModes:
      - ReadWriteOnce
      capacity:
        storage: 500Gi
      csi:
        driver: cephfs.csi.ceph.com
        nodeStageSecretRef:
          # node stage secret name
          name: csi-cephfs-metacatknb-pdg-subvol
          # node stage secret namespace where above secret is created
          namespace: ceph-csi-cephfs
        volumeAttributes:
          clusterID: 8aa4d4a0-a209-11ea-baf5-ffc787bfc812
          fsName: cephfs
          rootPath: /volumes/pdg-subvol-group/pdg-subvol/a5c90f20-f824-4ce9-b175-6685d7846520/repos/knb/postgresql
          staticVolume: "true"
        volumeHandle: *pv-name
      persistentVolumeReclaimPolicy: Retain
      storageClassName: csi-cephfs-sc
      volumeMode: Filesystem
   ```

5. Verify:
   ```shell
    $ kc get pv -o wide | grep knb
    cephfs-metacatknb-metacat-varmetacat    10Ti   RWX    Retain     Available    17m    Filesystem
    cephfs-metacatknb-metacat-postgresdata  100Gi  RWX    Retain     Available    25s    Filesystem
   ```

   > IMPORTANT NOTE: Kubernetes sometimes has trouble changing a PV mount, even if you delete
   > and re-create it, so if you create a PV and then decide you need to change the `rootPath`, the
   > old version may still be 'cached' on any nodes where it has previously been accessed by a pod.
   > This can lead to confusing behavior that is inconsistent across nodes. To work around this,
   > first delete the PV (after deleting any PVC that reference it), and then create it with a
   > different name.

6. Spend some time familiarizing yourself with the [parameters](#parameters) in the README file, and
   reading through [values.yaml](./values.yaml), to determine which configuration parameters
   need to be overridden for your specific installation requirements.

   > **NOTE:** In the kubernetes Metacat installation, you will no longer be able to customize
   > settings via Metacat's administrator interface. Instead, these must be included in the
   > helm chart values. In addition to overriding those values, the chart defaults may be needed
   > to be overridden for memory requirements, disk sizes etc. on a production system.

   Create a new file that contains the values you wish to override. As an example, for the values
   that were overridden for the KNB installation, see the [values-dev-cluster-knb-example.yaml
   file](./examples/values-dev-cluster-knb-example.yaml).

7. At this point, you should be able to `helm install` and debug any startup and configuration
   issues.

    > ### Important Note: BEFORE starting Metacat for the first time!
    >
    > The first time you start Metacat successfully, it will automatically upgrade the Metacat
    > database version from 2.19 to 3.0.0. This can take a few minutes, or in some cases, a few
    > hours, depending on your cluster node resources and how much data you have!
    >
    > Metacat may become unresponsive during the upgrade, causing the Kubernetes liveness probe to
    > fail, and the pod to be continuously restarted. In order to avoid this, you must disable the
    > liveness and readiness probes, until the database upgrade has completed successfully. (Verify
    > by logging into the administrator interface at yourhost.org/metacat/admin (You cannot edit
    > any values here, but you can see  the status of the database upgrade)

   * Disable Probes Until Database Upgrade is Finished

     ```yaml
       livenessProbe:
         enabled: false
       readinessProbe:
         enabled: false
     ```

   **When the database upgrade has finished, you can re-enable the probes (remove the above
   `enabled: false` probe values) and continue with the remaining items:**

8. TLS ("SSL") setup.

   See the README section on [Setting up a TLS Certificate for HTTPS
   Traffic](#setting-up-a-tls-certificate-for-https-traffic)

   Our NCEAS dev cluster includes [a cert-manager
   service](https://github.com/DataONEorg/k8s-cluster/blob/main/authentication/LetsEncrypt.md) that
   automatically watches for Ingresses and updates letsEncrypt certificates automatically, so
   this step is as simple as ensuring the ingress includes:

   ```yaml
   ingress:
     annotations:
       cert-manager.io/cluster-issuer: "letsencrypt-prod"
     className: "nginx"
     tls:
       - hosts:
           - knb-dev.test.dataone.org
         secretName: ingress-nginx-tls-cert
   ```

   ...and a tls cert will be applied automatically, matching the hostname defined in the `tls:`
   section. It will be created in a new secret: `ingress-nginx-tls-cert`, in the ingress' namespace

9. Create a DNS entry, to map your chosen domain name onto the ingress IP, found via:

   ```shell
   $ kubectl get ingress -o yaml | egrep "(\- ip:)|(\- host:)"
    - host: knb-dev.test.dataone.org
    - ip: 128.111.85.190
   ```

10. Install the DataONE jwt auth token Secret and public cert configmap for the indexer to use - see
   the README section [Setting up a Token and Optional CA certificate for Indexer
   Access](#setting-up-a-token-and-optional-ca-certificate-for-indexer-access).

11. Finally, you can now re-index all your datasets, so they will show up in Metacat search:

    > **Caution:** If you deploy large numbers of index workers, they can overwhelm Metacat with API
      requests when doing a large re-index. This can lead to errors and indexing failures. A future
      release will fix this, but in the meantime, we recommend starting with a low number of
      indexers (3 - 5), and finding the optimal number for your own installation.

    1. (*Beta workaround*) After deploying Metacat, but before starting the re-index, check that
       all the deployed indexer pods have started up cleanly. This can only be determined by
       inspecting the logs for each indexer pod (e.g.
       `kubectl logs -f -l app.kubernetes.io/name=d1index`), to ensure there are no exceptions.
       If any indexers did not start correctly, use `kubectl delete pod <podname>` to delete
       them, and k8s will then recreate them.

    2. Re-indexing can take anywhere from seconds to hours or even days, depending on how much
       data you have, and how many index workers you choose to deploy. You can override the
       number of index workers in the dataone-indexer sub-chart by adding the following to your
       metacat values.yaml:

        ```yaml
        dataone-indexer:
          # increase minReplicas from default 3
          autoscaling:
            minReplicas: 5
            # set max to the same value, so we don't
            # overwhelm Metacat (see "Caution" note, above):
            maxReplicas: 5
        ```

    3. When you are ready to reindex, issue the following command (`$TOKEN` should contain your
       administrator auth token -- [see this
       section](#setting-up-a-token-and-optional-ca-certificate-for-indexer-access)). Replace
       `myHostName.org` and `myContext` with your own:

        ```shell
        $  curl -X PUT -H "Authorization: Bearer $TOKEN" \
                       "https://myHostName.org/myContext/d1/mn/v2/index?all=true

               # expected output:
               # <?xml version="1.0" encoding="UTF-8"?>
               #     <scheduled>true</scheduled>
        ```

    4. You can monitor indexing progress via the RabbitMQ dashboard. Enable port forwarding:

       ```shell
       $  kubectl port-forward service/<yourReleaseName>-rabbitmq-headless 15672:15672
       ```

       ...and then point your browser at http://localhost:15672, and log in with the username
       `metacat-rmq-guest` and the RabbitMQ password you set in your metacat Secrets, or obtain by:

        ```shell
        secret_name=$(kubectl get secrets | egrep ".*\-metacat-secrets" | awk '{print $1}')
        rmq_pwd=$(kubectl get secret "$secret_name" \
                -o jsonpath="{.data.rabbitmq-password}" | base64 -d)
        echo "rmq_pwd: $rmq_pwd"
        ```

---

> ### Tips:
>
> 1. If you need to change the database user's password for your existing database, `kubectl exec`
>    into the postgres pod and do:
>    ```shell
>       /opt/bitnami/postgresql/bin/psql -U postgres <your-db-name>
>
>           ALTER USER metacat WITH PASSWORD 'new-password-here'
>    ```
>
> 2. If a PV can't be unmounted -- if the PV name is cephfs-metacatknb-metacat-varmetacat:
>    ```shell
>    kubectl patch pv cephfs-metacatknb-metacat-varmetacat -p '{"metadata":{"finalizers":null}}'
>    ```
