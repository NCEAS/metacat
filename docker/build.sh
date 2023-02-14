#!/bin/bash

set -e
VERSION=$1

if [ -z "$1" ] ;
  then
    echo "Usage: $0 <version>"
    echo "setting to default"
    VERSION="2.18.0"
    # exit
fi
echo "VERSION = $VERSION"
#
RELEASE="metacat-bin-${VERSION}.tar.gz"

# Grab the Metacat release
if [ !  -f "../${RELEASE}" ]
    then
        echo "You must first build the metacat release with 'ant distbin'"
fi

# Launch docker
cp ../"${RELEASE}" .
docker image build --build-arg METACAT_VERSION="$VERSION" --tag metacat:"$VERSION" .
