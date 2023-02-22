#!/bin/bash
set -e # fail the script if we get a non zero exit code

CSV=$1
ESM=$2
SYSTEM=$3
REGISTRY=$4

zowe idf map "$CSV" --esm "$ESM" --system "$SYSTEM" --registry "$REGISTRY"
