```bash
cd ~/projects/cib-data-infrastructure/spikes/metacat/metacat/helm
```

```bash
ls
```


# Prerequisites

Build dependencies

```bash
helm dependency build
```

Install minikube and start the minikube cluster

```bash
minikube start
```

Enable the ingress addon

```bash
minikube addons enable ingress
```

[Alllow snippets](https://kubernetes.github.io/ingress-nginx/examples/customization/custom-headers/) on the ingress controller

```bash
kubectl apply -f ingress-configmap.yaml
```


# Install the helm chart


## Secrets

Update `POSTGRES_PASSWORD`, `rabbitmq-password`, and `SOLR_ADMIN_PASSWORD` to some cryptographically secure values.

```bash
RELEASE_NAME=my-release envsubst < ./admin/secrets.yaml | kubectl apply -n default -f -
```


## Install the certificate

```bash
rm -f DataONETestIntCA.pem
wget -q https://raw.githubusercontent.com/DataONEorg/ca/main/DataONETestIntCA/certs/DataONETestIntCA.pem
```

```bash
kubectl create configmap my-release-d1-certs-public \
        --from-file=DataONETestIntCA.pem=DataONETestIntCA.pem
```


## Install the token

Get a short-lived token from KCB UI <https://knb.ecoinformatics.org/profile/http://orcid.org/0000-0002-2661-8912>

and save to `urn_node_robert.jwt` <file:///home/robert/projects/cib-data-infrastructure/spikes/metacat/metacat/helm/urn_node_robert.jwt>

```bash
kubectl create secret generic my-release-indexer-token \
        --from-file=DataONEauthToken=urn_node_robert.jwt
```


## Create a persistent volume

```bash
RELEASE_NAME=my-release envsubst < ./admin/pv-hostpath.yaml | kubectl apply -n default -f -
kubectl apply -n default -f ./admin/pvclaim.yaml
```


## Install the helm chart

```bash
./helm-upstall.sh my-release default -f examples/values-dev-docker-desktop-example.yaml 
```

╰─$ ./helm-upstall.sh my-release default -f examples/values-dev-docker-desktop-example.yaml executing command: RELEASE\_NAME=my-release envsubst < ./values.yaml | helm upgrade &#x2013;install my-release -n default -f - . -f examples/values-dev-docker-desktop-example.yaml Release "my-release" does not exist. Installing it now. coalesce.go:286: warning: cannot overwrite table with non table for rabbitmq.service.ports (map[amqp:5672 amqpTls:5671 dist:25672 epmd:4369 manager:15672 metrics:9419]) coalesce.go:286: warning: cannot overwrite table with non table for solr.service.ports (map[<http:8983>]) coalesce.go:286: warning: cannot overwrite table with non table for zookeeper.service.ports (map[client:2181 election:3888 follower:2888 tls:3181]) NAME: my-release LAST DEPLOYED: Thu Oct 17 11:19:32 2024 NAMESPACE: default STATUS: deployed REVISION: 1 NOTES:

1.  Get the application URL by running these commands:
