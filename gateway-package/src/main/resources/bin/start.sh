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

JAR_FILE="${LAUNCH_COMPONENT}/gateway-service-lite.jar"
# script assumes it's in the gateway component directory and common_lib needs to be relative path
COMMON_LIB="../apiml-common-lib/bin/api-layer-lite-lib-all.jar"

# API Mediation Layer Debug Mode
export LOG_LEVEL=

if [[ ! -z ${APIML_DEBUG_MODE_ENABLED} && ${APIML_DEBUG_MODE_ENABLED} == true ]]
then
  export LOG_LEVEL="debug"
fi

if [[ -z ${APIML_GATEWAY_CATALOG_ID} ]]
then
    APIML_GATEWAY_CATALOG_ID="apicatalog"
fi

if [ ${APIML_GATEWAY_CATALOG_ID} = "none" ]
then
    APIML_GATEWAY_CATALOG_ID=""
fi

if [ `uname` = "OS/390" ]; then
    GATEWAY_LOADER_PATH=${COMMON_LIB},/usr/include/java_classes/IRRRacf.jar
else
    GATEWAY_LOADER_PATH=${COMMON_LIB}
fi

echo "Setting loader path: "${GATEWAY_LOADER_PATH}

LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/j9vm
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/default
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm
export LIBPATH="$LIBPATH":

stop_jobs()
{
  kill -15 $pid
}

trap 'stop_jobs' INT

GATEWAY_CODE=AG
_BPX_JOBNAME=${ZOWE_PREFIX}${GATEWAY_CODE} java \
    -Xms32m -Xmx256m \
    -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${GATEWAY_PORT} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS} \
    -Dapiml.service.allowEncodedSlashes=${APIML_ALLOW_ENCODED_SLASHES} \
    -Dapiml.service.corsEnabled=${APIML_CORS_ENABLED} \
    -Dapiml.catalog.serviceId=${APIML_GATEWAY_CATALOG_ID} \
    -Dapiml.cache.storage.location=${WORKSPACE_DIR}/api-mediation/ \
    -Dapiml.logs.location=${WORKSPACE_DIR}/api-mediation/logs \
    -Denvironment.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.gateway.timeoutMillis=${APIML_GATEWAY_TIMEOUT_MILLIS} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES} \
    -Dapiml.security.auth.zosmfServiceId=zosmf \
    -Dapiml.security.auth.provider=${APIML_SECURITY_AUTH_PROVIDER} \
    -Dapiml.zoweManifest=${ZOWE_MANIFEST} \
    -Dserver.address=0.0.0.0 \
    -Dserver.maxConnectionsPerRoute=${APIML_MAX_CONNECTIONS_PER_ROUTE} \
    -Dserver.maxTotalConnections=${APIML_MAX_TOTAL_CONNECTIONS} \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore=${KEYSTORE} \
    -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.keyAlias=${KEY_ALIAS} \
    -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.trustStore=${TRUSTSTORE} \
    -Dserver.ssl.trustStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
    -Dapiml.security.auth.zosmfJwtAutoconfiguration=${APIML_SECURITY_ZOSMF_JWT_AUTOCONFIGURATION_MODE:-auto} \
    -Dapiml.security.x509.enabled=${APIML_SECURITY_X509_ENABLED:-false} \
    -Dapiml.security.x509.externalMapperUrl=http://localhost:${ZOWE_ZSS_SERVER_PORT}/certificate/x509/map \
    -Dapiml.security.x509.externalMapperUser=ZWESVUSR \
    -Dapiml.security.authorization.provider=${APIML_SECURITY_AUTHORIZATION_PROVIDER:-} \
    -Dapiml.security.authorization.endpoint.enabled=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_ENABLED:-false} \
    -Dapiml.security.authorization.endpoint.url=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_URL:-http://localhost:${ZOWE_ZSS_SERVER_PORT}/saf-auth} \
    -Dapiml.security.authorization.resourceClass=${RESOURCE_CLASS:-ZOWE} \
    -Dapiml.security.authorization.resourceNamePrefix=${RESOURCE_NAME_PREFIX:-APIML.} \
    -Dapiml.security.zosmf.applid=${APIML_SECURITY_ZOSMF_APPLID} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${GATEWAY_LOADER_PATH} \
    -jar ${JAR_FILE} &
pid=$?

wait
