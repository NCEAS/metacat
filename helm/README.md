# Metacat Helm Chart

Metacat is repository software for preserving data and metadata
(documentation about data) that helps scientists find, understand and
effectively use data sets they manage or that have been created by
others. For more details, see https://github.com/NCEAS/metacat

## TL;DR
Starting in root directory of metacat repo:
```console  
# 1. build the docker image
$ pushd docker ; ./build.sh ; popd

# 2. First time only: add your credentials to helm/admin/secrets.yaml, and add to cluster using: 
$ vim helm/admin/secrets.yaml
$ kubectl apply helm/admin/secrets.yaml  
 
# 3. deploy and enjoy!
$ helm install my-release ./helm
```

## Introduction

This chart deploys a Metacat deployment on a Kubernetes cluster using the Helm package 
manager.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- PV provisioner support in the underlying infrastructure
- An existing instance of solr, configured to be accessed by Metacat
- An existing postgres database, configured to be accessed by Metacat

## Installing the Chart

To install the chart with the release name `my-release`:

```console
helm install my-release ./helm
```

The command deploys Metacat on the Kubernetes cluster in the default configuration. The [Parameters](#parameters) section lists the parameters that can be configured during installation.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```console
helm delete my-release
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Parameters

(( TODO: See <https://github.com/bitnami-labs/readme-generator-for-helm> to create the table ))

The above parameters map to the env variables defined in Metacat. For more information please 
refer to the [Metacat Administrators' Guide](https://knb.ecoinformatics.org/knb/docs/).

Specify non-secret parameters in the default [values.yaml](values.yaml), which will be used 
automatically each time you deploy. 

**Tip**: You can also reference your own version of values.yaml as follows:
```console
helm install my-release -f myValues.yaml ./helm
```

> NOTE: Once this chart is deployed, it is not possible to change the application's access credentials, such as usernames or passwords, using Helm. To change these application credentials after deployment, delete any persistent volumes (PVs) used by the chart and re-deploy it, or use the application's built-in administrative tools if available.

## Configuration and installation details

## Secrets

Secret parameters (such as login credentials, certificates etc.) should be installed as
kubernetes Secrets in the cluster. The file [admin/secrets.yaml](./admin/secrets.yaml) provides a
template that you can complete and apply using `kubectl` - see file comments for details. Please
remember to NEVER ADD SECRETS TO GITHUB!

## Persistence

The Metacat image stores the Metacat data and configurations at the `/var/metacat` path of the 
container. Persistent Volume Claims are used to keep the data across deployments.
