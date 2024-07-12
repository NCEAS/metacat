#!/bin/bash

# * * NOTE: NEEDS TO BE RUN AS K8S ADMIN, OR A CONTEXT THAT CAN MODIFY THE NGINX NAMESPACE * *
# 
# This script populates the `nginx-config-mutual-auth.yaml` file with values passed in from the
# command line. This, in turn, tells the nginx ingress where to find a configmap that contains a
# custom "X-Proxy-Key" header, which enables it to share a secret with metacat, so metacat can
# trust that incoming client-cert-authorized requests have really been authorized by the ingress,
# and are not being spoofed.
#

if ! command -v envsubst &> /dev/null; then
    echo -n "$(basename "$0") -
        This script requires the 'envsubst' command to be available - see, for example:
            https://manpages.ubuntu.com/manpages/lunar/en/man1/envsubst.1.html
        To install envsubst on Mac OS using homebrew:
            $  brew install gettext && brew link --force gettext"
    exit
fi

if [ "$#" -lt 2 ]; then

    echo -n "$(basename "$0")  <release namespace>  <nginx ingress namespace>

    This script populates the nginx-config-mutual-auth.yaml file, which enables the nginx ingress to
    share a secret (via a custom header) with metacat, so metacat can trust that incoming
    client-cert requests have really been authorized by the ingress, and are not being spoofed.

            <release namespace>        is the namespace that will be used for the metacat
                                       installation (typically:  default, knb, etc)
            <nginx ingress namespace>  is the namespace where the nginx ingress is running
                                       (typically:  ingress-nginx)"
    exit
fi
echo
echo "*************   METACAT_NAMESPACE: ${1}"
echo
echo "*************   NGINX_NAMESPACE:   ${2}"
echo
echo "*************   APPLYING YAML:"
echo
METACAT_NAMESPACE="${1}" NGINX_NAMESPACE="${2}" envsubst < ./nginx-config-mutual-auth.yaml \
    | kubectl apply -n "${2}" -f -
