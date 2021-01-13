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

# API Mediation Layer Debug Mode
# To activate `debug` mode, set LOG_LEVEL=debug (in lowercase)
LOG_LEVEL=diag

# If set append $ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES to $STATIC_DEF_CONFIG_DIR
APIML_STATIC_DEF=${STATIC_DEF_CONFIG_DIR}
if [[ ! -z "$ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES" ]]
then
  APIML_STATIC_DEF="${APIML_STATIC_DEF};${ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES}"
fi
echo "active profile" $LOG_LEVEL
echo "starting " ${DISCOVERY_CODE}

_BPX_JOBNAME=${ZOWE_PREFIX}${DISCOVERY_CODE} java -Xms32m -Xmx256m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.active=https \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=${ZOWE_IP_ADDRESS} \
    -Dapiml.discovery.userid=eureka \
    -Dapiml.discovery.password=password \
    -Dapiml.discovery.allPeersUrls=https://${ZOWE_EXPLORER_HOST}:${DISCOVERY_PORT}/eureka/ \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${DISCOVERY_PORT} \
    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.service.preferIpAddress=false \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${APIML_STATIC_DEF} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES} \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore=${KEYSTORE} \
    -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.keyAlias=${KEY_ALIAS} \
    -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.trustStore=${TRUSTSTORE} \
    -Dserver.ssl.trustStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dapiml.service.preferIpAddress=false \
    -Dloader.path="components/api-mediation/lib/api-layer-lite-lib-all.jar" \
    -jar ${ROOT_DIR}"/components/api-mediation/discovery-service-lite.jar" &
echo "starting " ${CATALOG_CODE}
_BPX_JOBNAME=${ZOWE_PREFIX}${CATALOG_CODE} java -Xms16m -Xmx512m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Denvironment.hostname=${ZOWE_EXPLORER_HOST} \
    -Denvironment.port=${CATALOG_PORT} \
    -Denvironment.discoveryLocations=https://${ZOWE_EXPLORER_HOST}:${DISCOVERY_PORT}/eureka/ \
    -Denvironment.ipAddress=${ZOWE_IP_ADDRESS} \
    -Denvironment.preferIpAddress=false \
    -Denvironment.gatewayHostname=${ZOWE_EXPLORER_HOST} \
    -Denvironment.eurekaUserId=eureka \
    -Denvironment.eurekaPassword=password \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=${ZOWE_IP_ADDRESS} \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore=${KEYSTORE} \
    -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.keyAlias=${KEY_ALIAS} \
    -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.trustStore=${TRUSTSTORE} \
    -Dserver.ssl.trustStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path="components/api-mediation/lib/api-layer-lite-lib-all.jar" \
    -jar ${ROOT_DIR}"/components/api-mediation/api-catalog-services-lite.jar" &
echo "starting " ${GATEWAY_CODE}
_BPX_JOBNAME=${ZOWE_PREFIX}${GATEWAY_CODE} java -Xms32m -Xmx256m -Xquickstart -Xdiag \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${GATEWAY_PORT} \
    -Dapiml.service.discoveryServiceUrls=https://${ZOWE_EXPLORER_HOST}:${DISCOVERY_PORT}/eureka/ \
    -Dapiml.service.preferIpAddress=false \
    -Dapiml.service.allowEncodedSlashes=true \
    -Dapiml.cache.storage.location=${WORKSPACE_DIR}/api-mediation/ \
    -Denvironment.ipAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.gateway.timeoutMillis=30000 \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES} \
    -Dapiml.security.auth.zosmfServiceId=zosmf \
    -Dapiml.zoweManifest=${ZOWE_MANIFEST} \
    -Dserver.address=${ZOWE_IP_ADDRESS} \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.keyStore=${KEYSTORE} \
    -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.keyAlias=${KEY_ALIAS} \
    -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.trustStore=${TRUSTSTORE} \
    -Dserver.ssl.trustStoreType=${KEYSTORE_TYPE} \
    -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dapiml.security.authorization.provider=${APIML_SECURITY_AUTHORIZATION_PROVIDER:-} \
    -Dapiml.security.authorization.endpoint.enabled=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_ENABLED:-false} \
    -Dapiml.security.authorization.endpoint.url=${APIML_SECURITY_AUTHORIZATION_ENDPOINT_URL:-http://localhost:${ZOWE_ZSS_SERVER_PORT}/saf-auth} \
    -Dapiml.security.authorization.resourceClass=${RESOURCE_CLASS:-ZOWE} \
    -Dapiml.security.authorization.resourceNamePrefix=${RESOURCE_NAME_PREFIX:-APIML.} \
    -Dapiml.service.corsEnabled=true \
    -Dloader.path="components/api-mediation/lib/api-layer-lite-lib-all.jar,/usr/include/java_classes/IRRRacf.jar" \
    -jar ${ROOT_DIR}"/components/api-mediation/gateway-service-lite.jar" &
echo "starting DC"
DISCOVERABLECLIENT_CODE=DC
_BPX_JOBNAME=${ZOWE_PREFIX}${DISCOVERABLECLIENT_CODE} java -Xms32m -Xmx256m -Xquickstart \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dapiml.service.serviceId=discoverableclient \
    -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
    -Dapiml.service.port=${DISCOVERABLECLIENT_PORT} \
    -Dapiml.service.serviceIpAddress=${ZOWE_IP_ADDRESS} \
    -Dapiml.service.discoveryServiceUrls=https://${ZOWE_EXPLORER_HOST}:${DISCOVERY_PORT}/eureka/ \
    -Dserver.ssl.keyAlias=${KEY_ALIAS} \
    -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.keyStore=${KEYSTORE} \
    -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
    -Dserver.ssl.trustStore=${TRUSTSTORE} \
    -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dapiml.service.preferIpAddress=false \
    -jar ${ROOT_DIR}"/components/api-mediation/discoverable-client.jar" &
echo "starting CS"
_BPX_JOBNAME=${ZOWE_PREFIX}${CACHING_CODE} java -Xms16m -Xmx512m -Xquickstart \
  -Dibm.serversocket.recover=true \
  -Dfile.encoding=UTF-8 \
  -Djava.io.tmpdir=/tmp \
  -Dspring.profiles.include=$LOG_LEVEL \
  -Dapiml.service.port=${CACHING_SERVICE_PORT} \
  -Dapiml.service.hostname=${ZOWE_EXPLORER_HOST} \
  -Dapiml.service.discoveryServiceUrls=https://${ZOWE_EXPLORER_HOST}:${DISCOVERY_PORT}/eureka/ \
  -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS} \
  -Dapiml.service.customMetadata.apiml.gatewayPort=${GATEWAY_PORT} \
  -Dapiml.security.ssl.verifySslCertificatesOfServices=${VERIFY_CERTIFICATES} \
  -Denvironment.preferIpAddress=false \
  -Dserver.address=0.0.0.0 \
  -Dserver.ssl.enabled=true \
  -Dserver.ssl.keyStore=${KEYSTORE} \
  -Dserver.ssl.keyStoreType=${KEYSTORE_TYPE} \
  -Dserver.ssl.keyStorePassword=${KEYSTORE_PASSWORD} \
  -Dserver.ssl.keyAlias=${KEY_ALIAS} \
  -Dserver.ssl.keyPassword=${KEYSTORE_PASSWORD} \
  -Dserver.ssl.trustStore=${TRUSTSTORE} \
  -Dserver.ssl.trustStoreType=${KEYSTORE_TYPE} \
  -Dserver.ssl.trustStorePassword=${KEYSTORE_PASSWORD} \
  -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
  -Dcaching.storage.mode=vsam \
  -Dcaching.storage.vsam.name="//'TABAN03.CACHE5'" \
  -Delastic.apm.service_name=CACHING-SERVICE \
  -Delastic.apm.server_urls=http://mundev001682.bpc.broadcom.net:8200 \
  -Delastic.apm.application_packages=org.zowe \
  -Delastic.apm.trace_methods=org.zowe.* \
  -jar ${ROOT_DIR}"/components/api-mediation/caching-service.jar" &
echo "Done"
