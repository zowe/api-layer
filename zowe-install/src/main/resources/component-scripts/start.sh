#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019
################################################################################

# Variables required on shell:
# - ZOWE_PREFIX
# - DISCOVERY_PORT - the port the discovery service will use
# - CATALOG_PORT - the port the api catalog service will use
# - GATEWAY_PORT - the port the api gateway service will use
# - VERIFY_CERTIFICATES - boolean saying if we accept only verified certificates
# - DISCOVERY_PORT - The port the data sets server will use
# - KEY_ALIAS
# - KEYSTORE - The keystore to use for SSL certificates
# - KEYSTORE_TYPE - The keystore type to use for SSL certificates
# - KEYSTORE_PASSWORD - The password to access the keystore supplied by KEYSTORE
# - KEY_ALIAS - The alias of the key within the keystore
# - ALLOW_SLASHES - Allows encoded slashes on on URLs through gateway
# - ZOWE_MANIFEST - The full path to Zowe's manifest.json file

# Script is ran from run-zowe.sh context so use ROOT_DIR to make a full path to each script

. "${ROOT_DIR}"/components/api-mediation/bin/setup.sh

. "${ROOT_DIR}"/components/api-mediation/bin/start-discovery.sh &
. "${ROOT_DIR}"/components/api-mediation/bin/start-catalog.sh &
. "${ROOT_DIR}"/components/api-mediation/bin/start-gateway.sh &

if [[ ! -z ${ZOWE_CACHING_SERVICE_START} && ${ZOWE_CACHING_SERVICE_START} == true ]]
then
  . "${ROOT_DIR}"/components/api-mediation/bin/start-cache.sh &
fi

wait
