#!/bin/bash

set -e
TAG=$1
DEFAULT_TAG="2.19.0"

if [ -z "$1" ] || [[ "$1" == "-devtools" ]]; then
    TAG=${DEFAULT_TAG}
    echo
    echo "Usage: ${0} [tag] [-devtools]"
    echo " or:   ${0}       [-devtools]    (omitting tag defaults it to ${DEFAULT_TAG})"
    echo
    echo "where:  tag       is typically set to the metacat version #. Setting to default: ${TAG}"
    echo "       -devtools  is FOR DEV/DEBUGGING ONLY - NOT FOR PRODUCTION USE! Installs dev tools:"
    echo "                    vim, procps, lsof, and telnet in the container (see Dockerfile for"
    echo "                    complete list), thus REDUCING ITS SECURITY!"
    echo
fi

DEVTOOLS="false"
if [[ "$1" == "-devtools" ]] || [[ "$2" == "-devtools" ]]; then
    DEVTOOLS="true"
fi
echo "Building with \"DEVTOOLS\" set to: ${DEVTOOLS}"
echo

RELEASE="metacat-bin-${TAG}.tar.gz"

# Grab the Metacat release
if [ ! -f "../${RELEASE}" ]; then
    echo "Could not find ../${RELEASE}"
    echo "You must first build the metacat release with 'ant distbin'. Exiting..."
    exit 1
fi

cp ../"${RELEASE}" .

docker image build --no-cache --tag metacat:"$TAG" \
--build-arg TAG="$TAG" --build-arg DEVTOOLS="$DEVTOOLS" .
