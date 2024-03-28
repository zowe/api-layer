#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019, 2020
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

if [[ -z "${LAUNCH_COMPONENT}" ]]
then
  # component should be started from component home directory
  LAUNCH_COMPONENT=$(pwd)/bin
fi

JAR_FILE="${LAUNCH_COMPONENT}/discovery-service-lite.jar"
# script assumes it's in the discovery component directory and common_lib needs to be relative path
if [[ -z ${CMMN_LB} ]]
then
    COMMON_LIB="../apiml-common-lib/bin/api-layer-lite-lib-all.jar"
else
    COMMON_LIB=${CMMN_LB}
fi

if [[ -z ${LIBRARY_PATH} ]]
then
    LIBRARY_PATH="../common-java-lib/bin/"
fi
# API Mediation Layer Debug Mode
export LOG_LEVEL=

if [[ ! -z ${APIML_DEBUG_MODE_ENABLED} && ${APIML_DEBUG_MODE_ENABLED} == true ]]
then
  export LOG_LEVEL="debug"
fi

if [[ ! -z "${APIML_DIAG_MODE_ENABLED}" ]]
then
    LOG_LEVEL=${APIML_DIAG_MODE_ENABLED}
fi

# If set append $ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES to $STATIC_DEF_CONFIG_DIR
export APIML_STATIC_DEF=${STATIC_DEF_CONFIG_DIR}
if [[ ! -z "$ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES" ]]
then
  export APIML_STATIC_DEF="${APIML_STATIC_DEF};${ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES}"
fi
if [ `uname` = "OS/390" ]; then
    QUICK_START=-Xquickstart
fi

DISCOVERY_LOADER_PATH=${COMMON_LIB}

if [[ ! -z ${ZWE_DISCOVERY_SHARED_LIBS} ]]
then
    DISCOVERY_LOADER_PATH=${ZWE_DISCOVERY_SHARED_LIBS},${DISCOVERY_LOADER_PATH}
fi

EXPLORER_HOST=${ZOWE_EXPLORER_HOST:-localhost}
DISCOVERY_SERVICE_PORT=${DISCOVERY_PORT:-7553}

LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/j9vm
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/default
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"
java_v=$(java -version 2>&1)
echo "$java_v"
echo ${JAVA_HOME}
DISCOVERY_CODE=AD
_BPX_JOBNAME=${ZOWE_PREFIX}${DISCOVERY_CODE} java -Xms32m -Xmx256m ${QUICK_START} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.active=${APIML_SPRING_PROFILES:-https} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=0.0.0.0 \
    -Dapiml.discovery.userid=eureka \
    -Dapiml.discovery.password=password \
    -Dapiml.discovery.allPeersUrls=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${EXPLORER_HOST}:${DISCOVERY_SERVICE_PORT}/eureka/"} \
    -Dapiml.logs.location=${WORKSPACE_DIR}/api-mediation/logs \
    -Dapiml.service.hostname=${EXPLORER_HOST} \
    -Dapiml.service.port=${DISCOVERY_SERVICE_PORT} \
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS:-127.0.0.1} \
    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS:-false} \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${APIML_STATIC_DEF} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${NONSTRICT_VERIFY_CERTIFICATES:-false} \
    -Dserver.ssl.enabled=${APIML_SSL_ENABLED:-true} \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStoreType="${KEYSTORE_TYPE:-PKCS12}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
    -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.trustStore="${TRUSTSTORE}" \
    -Dserver.ssl.trustStoreType="${KEYSTORE_TYPE:-PKCS12}" \
    -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${DISCOVERY_LOADER_PATH} \
    -Djava.library.path=${LIBPATH} \
    -jar "${JAR_FILE}" 2>&1 &
pid=$!
echo "pid=${pid}"

wait %1
