#!/bin/bash

set -e

if [ -z $1 ] ;
  then
    echo "Usage: $0 <version>"
    exit
fi

#
VERSION=$1
RELEASE="metacat-bin-${VERSION}.tar.gz"
DIST="https://knb.ecoinformatics.org/software/dist"

# Grab the Metacat release
if [ !  -f "../${RELEASE}" ]
    then
        echo "You must first build the metacat release with 'ant distbin'"
fi

# Launch docker
cp ../${RELEASE} .
docker build --build-arg METACAT_VERSION=$VERSION -t metacat:$VERSION .
