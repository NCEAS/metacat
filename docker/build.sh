#!/bin/bash

set -e
TAG=$1
DEFAULT_TAG="DEVELOP"
DEFAULT_VERSION="2.19.0"

echo
echo "Usage:   $0 [tag] [-devtools]"
echo "   or:   $0       [-devtools]"
echo
echo "where:  tag       is the image tag, which is typically the metacat version being released."
echo "                  If this is not a release build, then omitting [tag] results in the image"
echo "                  tag defaulting to $DEFAULT_TAG, and the container build defaulting to"
echo "                  Metacat version $DEFAULT_VERSION."
echo "       -devtools  is FOR DEV/DEBUGGING ONLY - NOT FOR PRODUCTION USE!"
echo "                  *** Note that: ***"
echo "                  -devtools mode does NOT start tomcat/metacat, but instead starts a bash"
echo "                   infinite loop to keep the container running, for debugging purposes."
echo "                  Consequently, you need to COMMENT OUT livenessProbe and readinessProbe in"
echo "                   values.yaml, otherwise k8s WILL KEEP TRYING TO RESTART THE CONTAINER!"
echo "                  -devtools installs additional command-line tools in the container (see"
echo "                   Dockerfile for complete list), thus REDUCING ITS SECURITY!"
echo
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo "* * *"
if [ -z "$1" ] || [[ "$1" == "-devtools" ]]; then
    TAG=$DEFAULT_TAG
    MC_VERSION=$DEFAULT_VERSION
    echo "* * *  Setting image tag to $TAG and the Metacat release version to $MC_VERSION"

elif [[ -n "$1" ]]; then
    TAG="$1"
    MC_VERSION="$TAG"
    echo "* * *  Setting both the image tag and the Metacat release version to ${TAG})"
fi
echo "* * *      (Remember to set 'image.tag' in values.yaml to match tag: $TAG!!)"
echo "* * *"

DEVTOOLS="false"
DEV_BUILD_OPTS=""
if [[ "$1" == "-devtools" ]] || [[ "$2" == "-devtools" ]]; then
    DEVTOOLS="true"
    DEV_BUILD_OPTS="--no-cache --progress=plain"
    echo "* * *  Setting build flags to: ${DEV_BUILD_OPTS}"
    echo "* * *"
    echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
    echo "* * *"
    echo "* * *      (Remember to disable the livenessProbe in values.yaml, otherwise the"
    echo "* * *       pod will keep restarting!!)"
    echo "* * *"
    echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
    echo "* * *"
fi
echo "* * *  Building with \"DEVTOOLS\" set to: ${DEVTOOLS}"
echo "* * *"
echo "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *"
echo

DISTBIN="metacat-bin-${MC_VERSION}.tar.gz"

# Grab the Metacat release
if [ ! -f "../${DISTBIN}" ]; then
    echo "Could not find ../${DISTBIN}"
    echo "You must first build the metacat release with 'ant distbin'. Exiting..."
    exit 1
fi

cp ../"${DISTBIN}" .

docker image build $DEV_BUILD_OPTS \
    --tag metacat:"$TAG" --build-arg DISTBIN="$DISTBIN" --build-arg DEVTOOLS="$DEVTOOLS" .
