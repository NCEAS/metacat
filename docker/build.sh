#!/bin/bash

set -e
TAG=$1
DEFAULT_TAG="2.19.0"

if [ -z "$1" ] || [[ "$1" == "true" ]]; then
    TAG=${DEFAULT_TAG}
    echo "Usage: $0  <tag>  <use_devtools>"
    echo "or:    $0         <use_devtools>    (omitting TAG defaults it to ${$DEFAULT_TAG})"
    echo "where: <tag>          is typically set to the metacat version. Setting to default: ${TAG}"
    echo "       <use_devtools> FOR DEV & DEBUGGING ONLY - NOT FOR PRODUCTION USE!"
    echo "                      Ignored unless the value is \"true\". Installs dev tools such as"
    echo "                      vim, procps, lsof, and telnet in the container, thus REDUCING"
    echo "                      ITS SECURITY (see Dockerfile for complete list)"
fi

DEVTOOLS="false"
if [[ "$1" == "true" ]] || [[ "$2" == "true" ]]; then
    DEVTOOLS="true"
    echo "DEVTOOLS mode is for DEV & DEBUGGING ONLY - NOT FOR PRODUCTION USE!"
    echo "Installing dev tools such as vim, procps, lsof, and telnet in the container,"
    echo "thus REDUCING ITS SECURITY (see Dockerfile for complete list)"
fi
echo "Building with \"DEVTOOLS\" set to: ${DEVTOOLS}"

RELEASE="metacat-bin-${TAG}.tar.gz"

# Grab the Metacat release
if [ ! -f "../${RELEASE}" ]; then
    echo "You must first build the metacat release with 'ant distbin'"
fi

cp ../"${RELEASE}" .

docker image build --no-cache --tag metacat:"$TAG" \
--build-arg TAG="$TAG" --build-arg DEVTOOLS="$DEVTOOLS" .
