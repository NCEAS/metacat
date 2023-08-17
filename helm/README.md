# Metacat Helm Chart

Metacat is repository software for preserving data and metadata (documentation about data) that
helps scientists find, understand and effectively use data sets they manage or that have been
created by others. For more details, see https://github.com/NCEAS/metacat

## TL;DR
For now, you need to have an existing instance of
[solr](https://solr.apache.org/downloads.html#solr-8112) installed, running and
[configured for metacat](https://knb.ecoinformatics.org/knb/docs/install.html#solr-server).
Starting in the root directory of the `metacat` repo:

```shell
# 1. build metacat's binary distribution
$  ant distbin

# 2. build the docker image
$ pushd docker ; ./build.sh ; popd

# 3. FIRST TIME ONLY: add your credentials to helm/admin/secrets.yaml, and add to cluster
$ vim helm/admin/secrets.yaml    ## follow the instructions in this file

# 4. deploy and enjoy! Assuming your release name is "mc" (see Note below):
$ helm install mc ./helm
```

You should then be able to access the application via http://localhost/metacat! **Note** you should
not need to edit anything in [values.yaml](./values.yaml), if you have used the release name
"mc" and your dev setup is fairly standard. If things don't work as expected, check the value of
`postgres.auth.existingSecret` (which should include the release name) and also check the
properties in the `metacat` section, such as `solr.baseURL`.

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

```shell
helm install my-release ./helm
```

This command deploys Metacat on the Kubernetes cluster in the default configuration. The
[Parameters](#parameters) section lists the parameters that can be configured during
installation.

> **Tip**: Some settings in [values.yaml](./values.yaml) depend upon the release name. See the
> [Parameters](#parameters) section for Descriptions that include "RELEASE PREFIX"

Parameters may be provided on the command line to override those in values.yaml; e.g.

```shell
helm install my-release ./helm  --set postgres.auth.existingSecret=my-release-secrets
```

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```shell
helm delete my-release
```

The `helm delete` command removes all the Kubernetes components associated with the chart (with the
exception of Secrets, PVCs and PVs) and deletes the release.

There are two PVCs associated with `my-release`; one for Metacat data files, and the other for
the PostgreSQL database (if the postgres sub-chart is enabled). To delete:

```shell
kubectl delete pvc <myMetacatPVCName> (or <myPostgresPVCName>)   ## deletes named PVC
or:
kubectl delete pvc -l release=my-release                         ## deletes both
```

> **NOTE**: DELETING THE PVC's WILL DELETE ALL YOUR DATA AS WELL! Please be cautious!


## Parameters

### Metacat Application-Specific Properties

| Name                                                 | Description                                                   | Value                                                           |
|------------------------------------------------------|---------------------------------------------------------------|-----------------------------------------------------------------|
| `metacat.application.context`                        | The application context to use                                | `metacat`                                                       |
| `metacat.administrator.username`                     | The admin username that will be used to authenticate          | `admin@localhost`                                               |
| `metacat.auth.administrators`                        | A colon-separated list of admin usernames or LDAP-style DN    | `admin@localhost:uid=jones,ou=Account,dc=ecoinformatics,dc=org` |
| `metacat.database.connectionURI`                     | postgres DB URI (RELEASE PREFIX, or blank for sub-chart)      | `jdbc:postgresql://mc-postgresql/metacat`                       |
| `metacat.guid.doi.enabled`                           | Allow users to publish Digital Object Identifiers at doi.org? | `true`                                                          |
| `metacat.server.httpPort`                            | The http port exposed externally, if NOT using the ingress    | `""`                                                            |
| `metacat.server.name`                                | The hostname for the server, as exposed by the ingress        | `localhost`                                                     |
| `metacat.solr.baseURL`                               | The url to access solr                                        | `http://host.docker.internal:8983/solr`                         |
| `metacat.replication.logdir`                         | Location for the replication logs                             | `/var/metacat/logs`                                             |
| `metacat.dataone.certificate.fromHttpHeader.enabled` | Enable mutual auth with client certs                          | `false`                                                         |

### Metacat Image, Container & Pod Parameters

| Name                         | Description                                                                  | Value          |
|------------------------------|------------------------------------------------------------------------------|----------------|
| `image.repository`           | Metacat image repository                                                     | `metacat`      |
| `image.tag`                  | Metacat image tag (immutable tags are recommended)                           | `DEVELOP`      |
| `image.pullPolicy`           | Metacat image pull policy                                                    | `IfNotPresent` |
| `image.tag`                  | Overrides the image tag. Will default to the chart appVersion if set to ""   | `DEVELOP`      |
| `image.debug`                | Specify if container debugging should be enabled (sets log level to "DEBUG") | `false`        |
| `imagePullSecrets`           | Optional list of references to secrets in the same namespace                 | `[]`           |
| `container.ports`            | Optional list of additional container ports to expose within the cluster     | `[]`           |
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
|-----------------------------|----------------------------------------------------------------|---------------------|
| `persistence.enabled`       | Enable metacat data persistence using Persistent Volume Claims | `true`              |
| `persistence.storageClass`  | Storage class of backing PV                                    | `local-path`        |
| `persistence.existingClaim` | Name of an existing Persistent Volume Claim to re-use          | `""`                |
| `persistence.accessModes`   | PVC Access Mode for metacat volume                             | `["ReadWriteOnce"]` |
| `persistence.size`          | PVC Storage Request for metacat volume                         | `1Gi`               |

### Networking & Monitoring

| Name                          | Description                                                   | Value            |
|-------------------------------|---------------------------------------------------------------|------------------|
| `ingress.enabled`             | Enable or disable the ingress                                 | `true`           |
| `ingress.className`           | ClassName of the ingress provider in your cluster             | `traefik`        |
| `ingress.hosts`               | A collection of rules mapping different hosts to the backend. | `[]`             |
| `ingress.annotations`         | Annotations for the ingress                                   | `{}`             |
| `ingress.tls`                 | The TLS configuration                                         | `[]`             |
| `ingress.d1CaCertSecretName`  | Name of Secret containing DataONE CA certificate              | `ca-secret`      |
| `service.enabled`             | Enable another optional service in addition to headless svc   | `false`          |
| `service.type`                | Kubernetes Service type. Defaults to ClusterIP if not set     | `LoadBalancer`   |
| `service.clusterIP`           | IP address of the service. Auto-generated if not set          | `""`             |
| `service.ports`               | The port(s) to be exposed                                     | `[]`             |
| `livenessProbe.enabled`       | Enable livenessProbe for Metacat container                    | `true`           |
| `livenessProbe.httpGet.path`  | The url path to probe.                                        | `/metacat/`      |
| `livenessProbe.httpGet.port`  | The named containerPort to probe                              | `metacat-web`    |
| `readinessProbe.enabled`      | Enable readinessProbe for Metacat container                   | `true`           |
| `readinessProbe.httpGet.path` | The url path to probe.                                        | `/metacat/admin` |
| `readinessProbe.httpGet.port` | The named containerPort to probe                              | `metacat-web`    |

### Postgresql Sub-Chart

| Name                                           | Description                                             | Value                              |
|------------------------------------------------|---------------------------------------------------------|------------------------------------|
| `postgresql.enabled`                           | enable the postgresql sub-chart                         | `true`                             |
| `postgresql.auth.username`                     | Username for accessing the database used by metacat     | `metacat`                          |
| `postgresql.auth.database`                     | The name of the database used by metacat.               | `metacat`                          |
| `postgresql.auth.existingSecret`               | Secrets location for postgres password (RELEASE PREFIX) | `mc-secrets`                       |
| `postgresql.auth.secretKeys.userPasswordKey`   | Identifies metacat db's account password                | `POSTGRES_PASSWORD`                |
| `postgresql.auth.secretKeys.adminPasswordKey`  | Dummy value - not used (see notes):                     | `POSTGRES_PASSWORD`                |
| `postgresql.primary.pgHbaConfiguration`        | PostgreSQL Primary client authentication                | (See [values.yaml](./values.yaml)) |
| `postgresql.primary.persistence.enabled`       | Enable data persistence using PVC                       | `true`                             |
| `postgresql.primary.persistence.existingClaim` | Existing PVC to re-use                                  | `""`                               |
| `postgresql.primary.persistence.storageClass`  | Storage class of backing PV                             | `""`                               |
| `postgresql.primary.persistence.size`          | PVC Storage Request for postgres volume                 | `1Gi`                              |


Specify non-secret parameters in the default [values.yaml](./values.yaml), which will be used
automatically each time you deploy.

> **NOTE**: Once the chart is deployed, it is not possible to change the postgreSQL access
> credentials, such as usernames or passwords, nor is it possible to change the
> Metacat primary administrator password, using Helm. To change these application
> credentials after deployment, delete any persistent volumes (PVs) used by the relevant
> application (Metacat or PostgreSQL) and re-deploy.
>
> **Warning**: Setting a password will be ignored on new installations in cases when a previous
> PosgreSQL release was deleted through the helm command. In that case, the old PVC will have an
> old password, and setting it through helm won't take effect. Deleting persistent volumes (PVs)
> will solve the issue. Refer to [issue 2061](https://github.com/bitnami/charts/issues/2061) for
> more details

Parameters may be provided on the command line to override those in [values.yaml](./values.yaml);
for example:

```shell
helm install my-release ./helm  --set metacat.solr.baseURL=http://mysolrhost:8983/solr
```

Alternatively, a YAML file that specifies the values for the parameters can be provided
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

Secret parameters (such as login credentials, certificates etc.) should be installed as
kubernetes Secrets in the cluster. The file [admin/secrets.yaml](./admin/secrets.yaml) provides a
template that you can complete and apply using `kubectl` - see file comments for details. Please
remember to NEVER ADD SECRETS TO GITHUB!

> **Important**:
> 1. The deployed Secrets name includes the release name as a prefix, (e.g. `my-release-secrets`)
> so it's important to ensure that the secrets name matches the release name referenced whenever
> you use `helm` commands.
> 2. The parameter `postgresql.auth.existingSecret` in [values.yaml](./values.yaml) must be set to
> match the name of these installed secrets (which will change if the release name is changed).

## Persistence

Persistent Volume Claims are used to keep the data across deployments. See the
[Parameters](#parameters) section to configure the PVCs or to disable persistence for either
application.

With the default setup in [values.yaml](./values.yaml), two persistent volumes will be provisioned
automatically (one for Metacat, and one for PostgreSQL) with a PVC bound to each. If you want to
have the application use a specific directory on the host machine, for example, see the
documentation in the [admin/pv-hostpath.yaml](./admin/pv-hostpath.yaml) file.

The Metacat image stores the Metacat data and configurations on a PVC mounted at the `/var/metacat`
path in the metacat container.

The PostgreSQL image stores the database data at the `/bitbami/pgdata` path in its own container.

## Networking and x.509 Certificates

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


### Setting up a TLS Certificate(s) for HTTPS Traffic

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
        - *extHostname
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

To set up the certificates for each:

1. First make sure you have the Kubernetes version of the
   [nginx ingress installed](#networking-and-x509-certificates)
1. Ensure [HTTPS access is set up](#setting-up-a-tls-certificates-for-https-traffic) and
   working correctly. This allows other nodes, acting as "clients" to verify your server's identity
   during mutual authentication.
1. From the DataONE administrators ([support@dataone.org](mailto:support@dataone.org)), obtain:

   1. a copy of the **DataONE Certificate Authority (CA) certificate chain**. This enables your node
      (when acting as server) to validate other nodes' client certificates signed by that authority.

   1. a **Client Certificate**, that uniquely identifies your Metacat instance. This allows another
      node (acting as server) to verify your node's identity (acting as "client") during mutual
      authentication.

1. Create the Kubernetes Secret (named `ca-secret`) to hold the ca chain (e.g. assuming it's in a
   file named `DataONECAChain.crt`):

    ```shell
        kubectl create secret generic ca-secret --from-file=ca.crt=DataONECAChain.crt
        # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
    ```

1. Run the [`configure-nginx-mutual-auth.sh` script](./admin/configure-nginx-mutual-auth.sh).
   This will configure your nginx ingress controller to add a shared secret header that Metacat
   requires for added security.

1. Create the Kubernetes Secret (named `client-secret`) to hold the Client Certificate (e.g.
   assuming it's in a file named `urn_node_TestMYNAME-1.crt`):

    ```shell
        kubectl create secret generic ca-secret --from-file=client.crt=urn_node_TestMYNAME-1.crt
        # (don't forget to define a non-default namespace if necessary, using `-n myNameSpace`)
    ```

1. Set the correct parameters in `values.yaml`:

    ```yaml
    metacat:
      dataone.certificate.fromHttpHeader.enabled: true


    ingress:
      className: "nginx"
      d1CaCertSecretName: ca-secret
    ```

1. re-install or upgrade to apply the changes

See [Appendix 3](#appendix-3-troubleshooting) for help with troubleshooting

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
](#setting-up-a-tls-certificates-for-https-traffic) (either signed by a trusted CA or [self-signed
](#appendix-1-self-signing-tls-certificates-for-https-traffic) for development & testing), you can
create your own self-signed Mutual Auth Client certificate and CA certificate as follows:

1. Generate the CA Key and Certificate:

    ```shell
    openssl req -x509 -sha256 -newkey rsa:4096 -keyout ca.key -out ca.crt -days 356 -nodes \
            -subj '/CN=My Cert Authority'
    ```

1. Generate the Client Key, and Certificate and Sign with the CA Certificate:

    ```shell
    openssl req -new -newkey rsa:4096 -keyout client.key -out client.csr -nodes \
            -subj '/CN=My Client'

    openssl x509 -req -sha256 -days 365 -in client.csr -CA ca.crt -CAkey ca.key \
            -set_serial 02 -out client.crt
    ```

## Appendix 3: Troubleshooting

If you're having trouble getting Mutual Authentication working, you can run metacat in debug mode:

```yaml
# in values.yaml
image:
  debug: true
```

or via `--set image.debug=true` command-line flag.

Then view the
metacat logs using:

```shell
  kubectl logs -f -l app.kubernetes.io/name=metacat
  # don't forget to include your namespace if necessary, using `-n myNameSpace`
```

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
          nginx.ingress.kubernetes.io/auth-tls-secret: default/ca-secret
          ## above may differ for you: <namespace>/<ingress.d1CaCertSecretName>
          nginx.ingress.kubernetes.io/auth-tls-verify-client: optional_no_ca
          nginx.ingress.kubernetes.io/auth-tls-verify-depth: "10"
    ```

    If you don't see these, or they are incorrect, check values.yaml for:

    ```yaml
      metacat:
        dataone.certificate.fromHttpHeader.enabled: #true for mutual auth

      ingress:
        tls: # needs to have been set up properly - see above

        d1CaCertSecretName: # needs to match secret name holding your ca cert
    ```

1. then check the configmaps for the ingress controller:

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

1. verifying the `ingress-nginx-custom-headers`:

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

1. You can also view the nginx ingress logs using:

    ```shell
      NS=ingress-nginx    # this is the ingress controller's namespace. Typically ingress-nginx
      kubectl logs -n ${NS} -f $(kc get pods -n ${NS} | grep -v "NAME" | sed -e 's/\ [0-9].*//g')
    ```

    ...and look for entries like: `Error reading ConfigMap`
