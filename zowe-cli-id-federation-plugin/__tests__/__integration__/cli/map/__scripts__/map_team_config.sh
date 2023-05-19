#!/bin/bash
set -e # fail the script if we get a non zero exit code

CSV=$1

cd ../../../../
zowe idf map "${CSV}" --system TST2
