#!/bin/bash

set -e

# Initialize variables
TAG=""
DEFAULT_TAG="DEVELOP"
TEST_TAG="TEST"
MC_VERSION=""
DEFAULT_MC_VERSION="3.0.0"
DEVTOOLS=false
ENVIRONMENT='prod'
DEV_BUILD_OPTS=""
DISTBIN=""
DISTSRC="./README.md"

# Function to display usage
usage() {
    echo "Usage: $0 [-t <TAG>] [-v <VERSION>] [--devtools]"
    echo
    echo "where:  -t <TAG>  is the image tag, which is typically the metacat version being released."
    echo "                  The image tag defaults to $TAG if the -t option is omitted"
    echo
    echo "                  You can use '-t $TEST_TAG' to build an image that includes the metacat"
    echo "                  sourcecode and test suite. Note that:"
    testmode-info
    echo
    echo "        -v <VERSION> is the metacat build version number; e.g. 2.19.1"
    echo "                  The version number defaults to $MC_VERSION if the -v option is omitted"
    echo
    echo "        --devtools is FOR DEV/DEBUGGING ONLY - NOT FOR PRODUCTION USE!"
    devtools-info
    echo
    exit 1
}

devtools-info() {
    echo "                  --devtools mode does NOT start tomcat/metacat; instead it starts a bash"
    echo "                   infinite loop to keep the container running, for debugging purposes."
    echo "                   ** IMPORTANT: you need to COMMENT OUT livenessProbe and readinessProbe"
    echo "                   includes values.yaml, or k8s WILL KEEP RESTARTING THE CONTAINER! **"
}

testmode-info() {
    echo "                  TEST MODE (-t TEST) is FOR DEV/DEBUGGING ONLY - NOT FOR PRODUCTION USE!"
    echo "                  It installs the FULL JDK and additional command-line build tools in the"
    echo "                  container (see Dockerfile for complete list), thus REDUCING CONTAINER"
    echo "                  SECURITY!"
    echo "                  The metacat source files will be installed in the 'metacat' user's home"
    echo "                  directory (is.e. under /home/metacat/)"
}

# Parse command-line arguments
while getopts ":t:v:-:" opt; do
    case "$opt" in
        t) TAG="$OPTARG" ;;
        v) MC_VERSION="$OPTARG" ;;
        -)
            case "${OPTARG}" in
                devtools) DEVTOOLS=true ;;
                *) usage ;;
            esac
            ;;
        ?) usage ;;
    esac
done

if [[ -z $TAG ]] && [[ -z $MC_VERSION ]] && [[ $devtools == false ]]; then
    usage
fi

if [[ -n $MC_VERSION ]]; then
    echo "Version provided: $MC_VERSION"
else
    MC_VERSION=$DEFAULT_MC_VERSION
    echo "No Version provided. Defaulting to: $DEFAULT_MC_VERSION"
fi

if [[ -n $TAG ]]; then
    echo "Tagname provided: $TAG"
    if [ ${TAG} == ${TEST_TAG} ]; then
        echo
        echo "* * *  BUILDING A TEST IMAGE. Including TEST Suite  * * *"
        echo
        testmode-info
        echo
        ENVIRONMENT='test'

        DISTSRC="metacat-src-${MC_VERSION}.tar.gz"

        if [ -f ../"${DISTSRC}" ]; then
            cp ../"${DISTSRC}" .
            echo "found sourcecode files: ${DISTSRC}"
        else
            echo
            echo "Could not find ../${DISTSRC}"
            echo "You must first build the metacat release with 'ant fulldist', and ensure the"
            echo "filename matches ${DISTSRC}. Exiting..."
            exit 1
        fi
    fi
else
    TAG=$DEFAULT_TAG
    echo "No Tagname provided. Defaulting to: $DEFAULT_TAG"
fi

if [[ $DEVTOOLS == true ]]; then
    echo "Devtools enabled"
    DEV_BUILD_OPTS="--no-cache --progress=plain"
    devtools-info
fi

DISTBIN="metacat-bin-${MC_VERSION}.tar.gz"

# Grab the Metacat release
if [ -f "../${DISTBIN}" ]; then
    cp ../"${DISTBIN}" .
else
    echo "Could not find ../${DISTBIN}"
    echo "You must first build the metacat release with 'ant distbin', and ensure the filename"
    echo "matches ${DISTBIN}. Exiting..."
    exit 2
fi

echo
echo "* * *  Starting docker image build: $(date), using:"
echo "  TAG:                  $TAG"
echo "  VERSION:              $MC_VERSION"
echo "  BINARY DISTRIBUTION:  $DISTBIN"
echo "  ENVIRONMENT?          $ENVIRONMENT"
if [[ $ENVIRONMENT == "test" ]]; then
    echo "  SOURCE DISTRIBUTION:  $DISTSRC"
fi
echo "  DEVTOOLS:             $DEVTOOLS"
if [[ -n $DEV_BUILD_OPTS ]]; then
    echo "  BUILDING OPTIONS:  $DEV_BUILD_OPTS"
fi

docker image build $DEV_BUILD_OPTS       \
    --tag ghcr.io/nceas/metacat:"$TAG"   \
    --build-arg MC_VERSION="$MC_VERSION" \
    --build-arg ENVIRONMENT="$ENVIRONMENT" \
    --build-arg DISTBIN="$DISTBIN"       \
    --build-arg DISTSRC="$DISTSRC"       \
    --build-arg DEVTOOLS="$DEVTOOLS"  .
