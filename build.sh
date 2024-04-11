#!/bin/sh
#
# Build script to call the ant build system
#

umask 002; ant $*
