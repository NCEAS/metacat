#!/bin/bash

set -e
TAG=$1

if [ -z "$1" ] || [[ "$1" == "TRUE" ]]; then
    echo "Usage: $0 <tag> <debug>"
    TAG="2.19.0"
    echo "where: <tag>   is typically set to the metacat version. Setting to default: ${TAG}"
    echo "       <debug> is ignored unless the value is \"TRUE\"" 
fi

DEBUG="FALSE"
if [[ "$1" == "TRUE" ]] || [[ "$2" == "TRUE" ]]; then
    DEBUG="TRUE"
fi
echo "Building with \"DEBUG\" set to: ${DEBUG}"

RELEASE="metacat-bin-${TAG}.tar.gz"

# Grab the Metacat release
if [ ! -f "../${RELEASE}" ]; then
    echo "You must first build the metacat release with 'ant distbin'"
fi

cp ../"${RELEASE}" .

docker image build --no-cache --tag metacat:"$TAG" --build-arg TAG="$TAG" --build-arg DEBUG="$DEBUG" .
