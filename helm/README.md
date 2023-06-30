# Metacat Helm Chart

Metacat is repository software for preserving data and metadata (documentation about data) that
helps scientists find, understand and effectively use data sets they manage or that have been
created by others. For more details, see https://github.com/NCEAS/metacat

## TL;DR
For now, you need to have an existing instance of
[solr](https://solr.apache.org/downloads.html#solr-8112) installed, running and
[configured for metacat](https://knb.ecoinformatics.org/knb/docs/install.html#solr-server).
Starting in the root directory of the `metacat` repo:

```console  
# 1. build metacat's binary distribution
$  ant distbin

# 2. build the docker image
$ pushd docker ; ./build.sh ; popd

# 3. FIRST TIME ONLY: add your credentials to helm/admin/secrets.yaml, and add to cluster
$ vim helm/admin/secrets.yaml    ## follow the instructions in this file

# 4. deploy and enjoy! Assuming your release name is "my-release":
$ helm install my-release ./helm
```

You should then be able to access the application via http://localhost/metacat! Note you should
not need to edit anything in [values.yaml](./values.yaml), if your dev setup is fairly standard,
but it's worth checking, particularly the values in the `metacat` section, such as `solr.baseURL`

## Introduction

This chart deploys a [Metacat](https://github.com/NCEAS/metacat) deployment on a
[Kubernetes](https://kubernetes.io) cluster using the
[Helm](https://helm.sh) package manager.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- PV provisioner support in the underlying infrastructure
- An existing instance of [solr](https://solr.apache.org/downloads.html#solr-8112), suitably
[configured](https://knb.ecoinformatics.org/knb/docs/install.html#solr-server) to be accessed by
metacat, and with its index
[regenerated.](https://knb.ecoinformatics.org/knb/docs/query-index.html#regenerating-the-index)

## Installing the Chart

To install the chart with the release name `my-release`:

```console
helm install my-release ./helm
```

The command deploys Metacat on the Kubernetes cluster in the default configuration. The
[Parameters](#parameters) section lists the parameters that can be configured during
installation. Parameters may be provided on the command line to override those in values.yaml; e.g.

```console
helm install my-release ./helm --set image.debug=true
```

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```console
helm delete my-release
```

The `helm delete` command removes all the Kubernetes components associated with the chart (with the
exception of Secrets, PVCs and PVs) and deletes the release.

There are two PVCs associated with `my-release`; one for Metacat data files, and the other for
the PostgreSQL database. To delete:

```console
kubectl delete pvc <myMetacatPVCName> (or <myPostgresPVCName>)   ## deletes named PVC
or:
kubectl delete pvc -l release=my-release                         ## deletes both
```

> **NOTE**: DELETING THE PVC's WILL DELETE ALL YOUR DATA AS WELL! Please be cautious!


## Parameters

### Metacat Application-Specific Properties

| Name                             | Description                                                   | Value                                                        |
| -------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------ |
| `metacat.application.context`    | The application context to use                                | `metacat`                                                    |
| `metacat.administrator.username` | The admin username that will be used to authenticate          | `admin@localhost`                                            |
| `metacat.auth.administrators`    | A colon-separated list of admin usernames or LDAP-style DN    | `admin@localhost:uid=jones,o=NCEAS,dc=ecoinformatics,dc=org` |
| `metacat.database.connectionURI` | Connection URI for the postgres database                      | `jdbc:postgresql://mc-postgresql/metacat`                    |
| `metacat.guid.doi.enabled`       | Allow users to publish Digital Object Identifiers at doi.org? | `true`                                                       |
| `metacat.server.httpPort`        | The http port exposed internally by the metacat container     | `8080`                                                       |
| `metacat.server.name`            | The hostname for the server, as exposed by the ingress        | `localhost`                                                  |
| `metacat.solr.baseURL`           | The url to access solr                                        | `http://host.docker.internal:8983/solr`                      |
| `metacat.replication.logdir`     | Location for the replication logs                             | `/var/metacat/logs`                                          |

### Metacat Image, Container & Pod Parameters

| Name                         | Description                                                                  | Value          |
| ---------------------------- | ---------------------------------------------------------------------------- | -------------- |
| `image.repository`           | Metacat image repository                                                     | `metacat`      |
| `image.tag`                  | Metacat image tag (immutable tags are recommended)                           | `DEVELOP`      |
| `image.pullPolicy`           | Metacat image pull policy                                                    | `IfNotPresent` |
| `image.tag`                  | Overrides the image tag. Will default to the chart appVersion if set to ""   | `DEVELOP`      |
| `image.debug`                | Specify if container debugging should be enabled (sets log level to "DEBUG") | `false`        |
| `imagePullSecrets`           | Optional list of references to secrets in the same namespace                 | `[]`           |
| `serviceAccount.create`      | Should a service account be created to run Metacat?                          | `false`        |
| `serviceAccount.annotations` | Annotations to add to the service account                                    | `{}`           |
| `serviceAccount.name`        | The name to use for the service account.                                     | `""`           |
| `podAnnotations`             | Map of annotations to add to the pods                                        | `{}`           |
| `podSecurityContext.enabled` | Enable security context                                                      | `false`        |
| `podSecurityContext.fsGroup` | Group ID for the pod                                                         | `nil`          |
| `securityContext`            | Holds pod-level security attributes and common container settings            | `{}`           |
| `resources`                  | Resource limits for the deployment                                           | `{}`           |
| `tolerations`                | Tolerations for pod assigment                                                | `[]`           |

### Metacat Persistence

| Name                        | Description                                                    | Value               |
| --------------------------- | -------------------------------------------------------------- | ------------------- |
| `persistence.enabled`       | Enable metacat data persistence using Persistent Volume Claims | `true`              |
| `persistence.storageClass`  | Storage class of backing PV                                    | `local-path`        |
| `persistence.existingClaim` | Name of an existing Persistent Volume Claim to re-use          | `""`                |
| `persistence.accessModes`   | PVC Access Mode for metacat volume                             | `["ReadWriteOnce"]` |
| `persistence.size`          | PVC Storage Request for metacat volume                         | `1Gi`               |

### Networking & Monitoring

| Name                          | Description                                                   | Value            |
| ----------------------------- | ------------------------------------------------------------- | ---------------- |
| `ingress.enabled`             | Enable or disable the ingress                                 | `true`           |
| `ingress.className`           | ClassName of the ingress provider in your cluster             | `traefik`        |
| `ingress.hosts`               | A collection of rules mapping different hosts to the backend. | `[]`             |
| `ingress.annotations`         | Annotations for the ingress                                   | `{}`             |
| `ingress.tls`                 | The TLS configuration                                         | `[]`             |
| `service.type`                | Kubernetes Service type                                       | `ClusterIP`      |
| `service.ports`               | The port(s) to be exposed                                     | `[]`             |
| `livenessProbe.enabled`       | Enable livenessProbe for Metacat container                    | `true`           |
| `livenessProbe.httpGet.path`  | The url path to probe.                                        | `/metacat/`      |
| `livenessProbe.httpGet.port`  | The named containerPort to probe                              | `metacat-web`    |
| `readinessProbe.enabled`      | Enable readinessProbe for Metacat container                   | `true`           |
| `readinessProbe.httpGet.path` | The url path to probe.                                        | `/metacat/admin` |
| `readinessProbe.httpGet.port` | The named containerPort to probe                              | `metacat-web`    |

### Postgresql Sub-Chart

| Name                                           | Description                                         | Value               |
| ---------------------------------------------- | --------------------------------------------------- |---------------------|
| `postgresql.enabled`                           | enable the postgresql sub-chart                     | `true`              |
| `postgresql.auth.username`                     | Username for accessing the database used by metacat | `metacat`           |
| `postgresql.auth.database`                     | The name of the database used by metacat.           | `metacat`           |
| `postgresql.auth.existingSecret`               | Find the password in metacat's existing secrets     | `mc-secrets`        |
| `postgresql.auth.secretKeys.userPasswordKey`   | Identifies metacat db's account password            | `POSTGRES_PASSWORD` |
| `postgresql.auth.secretKeys.adminPasswordKey`  | Dummy value - not used (see notes):                 | `POSTGRES_PASSWORD` |
| `postgresql.primary.pgHbaConfiguration`        | PostgreSQL Primary client authentication            | (See values.yaml)   |
| `postgresql.primary.persistence.enabled`       | Enable data persistence using PVC                   | `true`              |
| `postgresql.primary.persistence.existingClaim` | Existing PVC to re-use                              | `""`                |
| `postgresql.primary.persistence.storageClass`  | Storage class of backing PV                         | `""`                |
| `postgresql.primary.persistence.size`          | PVC Storage Request for postgres volume             | `1Gi`               |


Specify non-secret parameters in the default [values.yaml](values.yaml), which will be used
automatically each time you deploy.

> NOTE: Once the chart is deployed, it is not possible to change the postgreSQL access
> credentials, such as usernames or passwords, nor is it possible to change the
> Metacat primary administrator password, using Helm. To change these application
> credentials after deployment, delete any persistent volumes (PVs) used by the relevant
> application (Metacat or PostgreSQL) and re-deploy.
> 
> **Warning**: Setting a password will be ignored on new installations in cases when a previous
> Posgresql release was deleted through the helm command. In that case, the old PVC will have an
> old password, and setting it through helm won't take effect. Deleting persistent volumes (PVs)
> will solve the issue. Refer to [issue 2061](https://github.com/bitnami/charts/issues/2061) for
> more details

Parameters may be provided on the command line to override those in values.yaml; e.g.

```console
helm install my-release ./helm  --set metacat.solr.baseURL=http://mysolrhost:8983/solr
```

Alternatively, a YAML file that specifies the values for the parameters can be provided
while installing the chart. For example:

```console
helm install my-release -f myValues.yaml ./helm
```

> **Tip**: You can use the default [values.yaml](values.yaml)

## Configuration and installation details

### Metacat Application-Specific Properties

The parameters in the
[Metacat Application-Specific Properties](#metacat-application-specific-properties)
section, above, map to the values required by Metacat at runtime. For more information please refer
to the [Metacat Properties](https://knb.ecoinformatics.org/knb/docs/metacat-properties.html) section
of the [Metacat Administrators' Guide](https://knb.ecoinformatics.org/knb/docs/).

### Secrets

Secret parameters (such as login credentials, certificates etc.) should be installed as
kubernetes Secrets in the cluster. The file [admin/secrets.yaml](./admin/secrets.yaml) provides a
template that you can complete and apply using `kubectl` - see file comments for details. Please
remember to NEVER ADD SECRETS TO GITHUB!

## Persistence

The Metacat image stores the Metacat data and configurations at the `/var/metacat` path in the
container. The PostgreSQL image stores the database data at the `/bitbami/pgdata` path in its
container  Persistent Volume Claims are used to keep the data across deployments. See the Parameters
section to configure the PVCs or to disable persistence for either application.

With the default setup in values.yaml, two persistent volumes will be provisioned automatically (one
for Metacat, and one for PostgreSQL) with a PVC bound to each. If you want to have the application
use a specific directory on the host machine, for example, see the documentation in the
[admin/pv-hostpath.yaml](./admin/pv-hostpath.yaml) file.

## Networking

By default, the chart will install an
[Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/) (see the `ingress.*`
parameters under [Networking & Monitoring](#networking--monitoring)), which will expose HTTP
and HTTPS routes from outside the cluster to the Metacat application within the cluster. Note that
your cluster must have an Ingress controller in order for this to work.
