#!/bin/bash

set -e
TAG=$1

if [ -z "$1" ]; then
    echo "Usage: $0 <tag>"
    TAG="2.18.0"
    echo "where <tag> is typically set to the metacat version. setting to default: ${TAG}"
fi

RELEASE="metacat-bin-${TAG}.tar.gz"

# Grab the Metacat release
if [ ! -f "../${RELEASE}" ]; then
    echo "You must first build the metacat release with 'ant distbin'"
fi

# Launch docker
cp ../"${RELEASE}" .
docker image build --tag metacat:"$TAG" --build-arg TAG="$TAG" .
