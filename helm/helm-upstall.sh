#!/bin/bash

if [ -z $1 ]; then
  echo "usage: $0  RELEASE_NAME  NAMESPACE [optional helm cmd options]"
fi
RLS=$1
NS=$2

echo "executing command:  RELEASE_NAME=$RLS envsubst < ./values.yaml | helm upgrade --install $RLS -n $NS -f - . ${@:3}"
RELEASE_NAME=$RLS envsubst < ./values.yaml | helm upgrade --install $RLS -n $NS -f - . ${@:3}
