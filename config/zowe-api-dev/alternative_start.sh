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


LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/j9vm
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/default
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"

GATEWAY_CODE=AG
_BPX_JOBNAME=${ZOWE_PREFIX}${GATEWAY_CODE} java \
    -Xms32m -Xmx256m \
    ${QUICK_START} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${APIML_SPRING_PROFILES:-} \
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
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.gateway.timeoutMillis=${APIML_GATEWAY_TIMEOUT_MILLIS} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${NONSTRICT_VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.auth.zosmf.serviceId=${APIML_ZOSMF_ID:-zosmf} \
    -Dapiml.security.auth.provider=${APIML_SECURITY_AUTH_PROVIDER} \
    -Dapiml.zoweManifest=${ZOWE_MANIFEST} \
    -Dserver.address=0.0.0.0 \
    -Dserver.maxConnectionsPerRoute=${APIML_MAX_CONNECTIONS_PER_ROUTE:-10} \
    -Dserver.maxTotalConnections=${APIML_MAX_TOTAL_CONNECTIONS:-100} \
    -Dserver.ssl.enabled=${APIML_SSL_ENABLED:-true} \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStoreType="${KEYSTORE_TYPE}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
    -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.trustStore="${TRUSTSTORE}" \
    -Dserver.ssl.trustStoreType="${KEYSTORE_TYPE}" \
    -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.internal.enabled=${APIML_GATEWAY_INTERNAL_ENABLED:-false} \
    -Dserver.internal.ssl.enabled=${APIML_SSL_ENABLED:-true} \
    -Dserver.internal.port=${APIML_GATEWAY_INTERNAL_PORT:-10017} \
    -Dserver.internal.ssl.keyAlias=${APIML_GATEWAY_INTERNAL_SSL_KEY_ALIAS:-localhost-multi} \
    -Dserver.internal.ssl.keyStore=${APIML_GATEWAY_INTERNAL_SSL_KEYSTORE:-keystore/localhost/localhost-multi.keystore.p12} \
    -Dapiml.security.auth.zosmf.jwtAutoconfiguration=${APIML_SECURITY_ZOSMF_JWT_AUTOCONFIGURATION_MODE:-auto} \
    -Dapiml.security.x509.enabled=${APIML_SECURITY_X509_ENABLED:-false} \
    -Dapiml.security.x509.externalMapperUrl=${APIML_GATEWAY_EXTERNAL_MAPPER} \
    -Dapiml.security.x509.externalMapperUser=${APIML_GATEWAY_MAPPER_USER:-ZWESVUSR} \
    -Dapiml.security.authorization.provider=${APIML_SECURITY_AUTHORIZATION_PROVIDER:-} \
    -Dapiml.security.authorization.endpoint.enabled=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_ENABLED:-false} \
    -Dapiml.security.authorization.endpoint.url=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_URL} \
    -Dapiml.security.authorization.resourceClass=${RESOURCE_CLASS:-ZOWE} \
    -Dapiml.security.authorization.resourceNamePrefix=${RESOURCE_NAME_PREFIX:-APIML.} \
    -Dapiml.security.zosmf.applid=${APIML_SECURITY_ZOSMF_APPLID} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${GATEWAY_LOADER_PATH} \
    -Djava.library.path=${LIBPATH} \
    -jar ${ROOT_DIR}"/components/api-mediation/gateway-service-lite.jar" &

_BPX_JOBNAME=${ZOWE_PREFIX}${DISCOVERABLECLIENT_CODE} java -Xms32m -Xmx256m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dapiml.service.serviceId=discoverableclient \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${DISCOVERABLECLIENT_PORT} \
    -Dapiml.service.serviceIpAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.service.discoveryServiceUrls=https://${ZOWE_EXPLORER_HOST}:${DISCOVERY_PORT}/eureka/ \
    -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
    -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.trustStore="${TRUSTSTORE}" \
    -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dapiml.service.preferIpAddress=false \
    -jar ${ROOT_DIR}"/components/api-mediation/discoverable-client.jar" &

_BPX_JOBNAME=${ZOWE_PREFIX}${DISCOVERY_CODE} java -Xms32m -Xmx256m ${QUICK_START} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${APIML_SPRING_PROFILES:-https} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=0.0.0.0 \
    -Dapiml.discovery.userid=eureka \
    -Dapiml.discovery.password=password \
    -Dapiml.discovery.allPeersUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.logs.location=${WORKSPACE_DIR}/api-mediation/logs \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${DISCOVERY_PORT} \
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS} \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${APIML_STATIC_DEF} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${NONSTRICT_VERIFY_CERTIFICATES:-false} \
    -Dserver.ssl.enabled=${APIML_SSL_ENABLED:-true} \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStoreType="${KEYSTORE_TYPE}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
    -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.trustStore="${TRUSTSTORE}" \
    -Dserver.ssl.trustStoreType="${KEYSTORE_TYPE}" \
    -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${COMMON_LIB} \
    -Djava.library.path=${LIBPATH} \
    -jar ${ROOT_DIR}"/components/api-mediation/discovery-service-lite.jar" &

_BPX_JOBNAME=${ZOWE_PREFIX}${CATALOG_CODE} java \
    -Xms16m -Xmx512m \
    ${QUICK_START} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${APIML_SPRING_PROFILES:-} \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${CATALOG_PORT} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS} \
    -Dapiml.service.gatewayHostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.logs.location=${WORKSPACE_DIR}/api-mediation/logs \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${NONSTRICT_VERIFY_CERTIFICATES:-false} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=0.0.0.0 \
    -Dserver.ssl.enabled=${APIML_SSL_ENABLED:-true}  \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStoreType="${KEYSTORE_TYPE}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
    -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
    -Dserver.ssl.trustStore="${TRUSTSTORE}" \
    -Dserver.ssl.trustStoreType="${KEYSTORE_TYPE}" \
    -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${COMMON_LIB} \
    -Djava.library.path=${LIBPATH} \
    -jar ${ROOT_DIR}"/components/api-mediation/api-catalog-services-lite.jar" &

_BPX_JOBNAME=${ZOWE_PREFIX}${CACHING_CODE} java -Xms16m -Xmx512m \
   ${QUICK_START} \
  -Dibm.serversocket.recover=true \
  -Dfile.encoding=UTF-8 \
  -Djava.io.tmpdir=${TMPDIR:-/tmp} \
  -Dspring.profiles.active=${APIML_SPRING_PROFILES:-} \
  -Dspring.profiles.include=$LOG_LEVEL \
  -Dapiml.service.port=${ZWE_CACHING_SERVICE_PORT} \
  -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
  -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
  -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
  -Dapiml.service.customMetadata.apiml.gatewayPort=${GATEWAY_PORT} \
  -Dapiml.service.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES:-false} \
  -Dapiml.service.ssl.nonStrictVerifySslCertificatesOfServices=${NONSTRICT_VERIFY_CERTIFICATES:-false} \
  -Dcaching.storage.evictionStrategy=${ZWE_CACHING_EVICTION_STRATEGY:-reject} \
  -Dcaching.storage.size=${ZWE_CACHING_STORAGE_SIZE:-100} \
  -Dcaching.storage.mode=${ZWE_CACHING_SERVICE_PERSISTENT:-inMemory} \
  -Djgroups.bind.port=7098 \
  -Djgroups.bind.address=${ZOWE_EXPLORER_HOST} \
  -Dcaching.storage.infinispan.initialHosts="${ZOWE_EXPLORER_HOST}[7099]" \
  -Dcaching.storage.vsam.name=${VSAM_FILE_NAME} \
  -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS} \
  -Dserver.address=0.0.0.0 \
  -Dserver.ssl.enabled=${APIML_SSL_ENABLED:-true}  \
  -Dserver.ssl.keyStore="${KEYSTORE}" \
  -Dserver.ssl.keyStoreType="${KEYSTORE_TYPE}" \
  -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWORD}" \
  -Dserver.ssl.keyAlias="${KEY_ALIAS}" \
  -Dserver.ssl.keyPassword="${KEYSTORE_PASSWORD}" \
  -Dserver.ssl.trustStore="${TRUSTSTORE}" \
  -Dserver.ssl.trustStoreType="${KEYSTORE_TYPE}" \
  -Dserver.ssl.trustStorePassword="${KEYSTORE_PASSWORD}" \
  -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
  -Djava.library.path=${LIBPATH} \
  -jar ${ROOT_DIR}"/components/api-mediation/caching-service.jar"

echo "Done"
