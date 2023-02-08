#!/bin/bash
set -e # fail the script if we get a non zero exit code
zowe idf map $1 --esm $2 --lpar $3