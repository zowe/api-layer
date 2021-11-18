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

JAR_FILE="${LAUNCH_COMPONENT}/api-catalog-services-lite.jar"
# script assumes it's in the catalog component directory and common_lib needs to be relative path
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

EXPLORER_HOST=${ZOWE_EXPLORER_HOST:-localhost}

if [[ -z "${GATEWAY_HOST}" ]]
then
    GATEWAY_HOST=${EXPLORER_HOST}
fi

if [ `uname` = "OS/390" ]; then
    QUICK_START=-Xquickstart
fi
LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/j9vm
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/default
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"

CATALOG_CODE=AC
_BPX_JOBNAME=${ZOWE_PREFIX}${CATALOG_CODE} java \
    -Xms16m -Xmx512m \
    ${QUICK_START} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.active=${APIML_SPRING_PROFILES:-} \
    -Dapiml.service.hostname=${EXPLORER_HOST} \
    -Dapiml.service.port=${CATALOG_PORT:-7552} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${EXPLORER_HOST}:${DISCOVERY_PORT:-7553}/eureka/"} \
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS:-127.0.0.1} \
    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS:-false} \
    -Dapiml.service.gatewayHostname=${GATEWAY_HOST} \
    -Dapiml.service.eurekaUserName=eureka \
    -Dapiml.service.eurekaPassword=password \
    -Dapiml.logs.location=${WORKSPACE_DIR}/api-mediation/logs \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${APIML_STATIC_DEF} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${NONSTRICT_VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.authorization.provider=${APIML_SECURITY_AUTHORIZATION_PROVIDER:-} \
    -Dapiml.security.authorization.endpoint.enabled=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_ENABLED:-false} \
    -Dapiml.security.authorization.endpoint.url=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_URL:-"https://${EXPLORER_HOST}:${GATEWAY_SERVICE_PORT}/zss/api/v1/saf-auth"} \
    -Dapiml.security.authorization.resourceClass=${RESOURCE_CLASS:-ZOWE} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=0.0.0.0 \
    -Dserver.ssl.enabled=${APIML_SSL_ENABLED:-true}  \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStoreType="${KEYSTORE_TYPE:-PKCS12}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
    -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.trustStore="${TRUSTSTORE}" \
    -Dserver.ssl.trustStoreType="${KEYSTORE_TYPE:-PKCS12}" \
    -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${COMMON_LIB} \
    -Djava.library.path=${LIBPATH} \
    -jar "${JAR_FILE}" &
pid=$!
echo "pid=${pid}"

wait %1
