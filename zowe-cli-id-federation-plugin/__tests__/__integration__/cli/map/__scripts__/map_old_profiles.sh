#!/bin/bash
set -e # fail the script if we get a non zero exit code

CSV=$1

# create old school profiles
zowe profiles create id-federation my_idf --esm ACF2 --lpar TST1 --registry ldap://zowe.org > /dev/null

zowe idf map "${CSV}"
