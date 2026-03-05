#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2021
################################################################################

# Variables required on shell:
# - JAVA_HOME
# - ZWE_STATIC_DEFINITIONS_DIR
# - ZWE_configs_certificate_keystore_alias / ZWE_zowe_certificate_keystore_alias - The default alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file / ZWE_zowe_certificate_keystore_file - The default keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password / ZWE_zowe_certificate_keystore_password - The default password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_truststore_file / ZWE_zowe_certificate_truststore_file
# - ZWE_zowe_externalDomains_0
# - ZWE_zowe_externalPort
# - ZWE_zowe_job_prefix
# - ZWE_zowe_logDirectory
# - ZWE_zowe_runtimeDirectory
# - ZWE_zowe_workspaceDirectory

# Optional variables:
# - LAUNCH_COMPONENT
# - CMMN_LB
# - LIBPATH
# - LIBRARY_PATH
# - QUICK_START
# - TMPDIR
# - ZWE_GATEWAY_SHARED_LIBS
# - ZWE_haInstance_hostname
# - ZWE_components_discovery_port - the port the discovery service will use
# - ZWE_configs_heap_max
# - ZWE_configs_heap_init
# - ZWE_configs_sslDebug
# - ZWE_configs_apimlId
# - ZWE_configs_apiml_connection_timeout
# - ZWE_configs_apiml_connection_idleConnectionTimeoutSeconds
# - ZWE_configs_apiml_connection_timeToLive
# - ZWE_configs_apiml_health_protected
# - ZWE_configs_apiml_security_auth_jwt_customAuthHeader
# - ZWE_configs_apiml_security_auth_passticket_customUserHeader
# - ZWE_configs_apiml_security_auth_passticket_customAuthHeader
# - ZWE_configs_apiml_security_authorization_endpoint_enabled
# - ZWE_configs_apiml_security_authorization_endpoint_url
# - ZWE_configs_apiml_security_authorization_provider
# - ZWE_configs_apiml_security_x509_enabled
# - ZWE_configs_apiml_security_x509_acceptForwardedCert
# - ZWE_configs_apiml_security_x509_certificatesUrl
# - ZWE_configs_apiml_security_x509_registry_allowedUsers
# - ZWE_configs_apiml_service_allowEncodedSlashes
# - ZWE_configs_apiml_service_corsEnabled
# - ZWE_configs_apiml_service_corsAllowedMethods
# - ZWE_configs_apiml_gateway_registry_enabled
# - ZWE_configs_apiml_gateway_registry_cachePeriodSec
# - ZWE_configs_apiml_gateway_registry_maxSimultaneousRequests
# - ZWE_configs_apiml_gateway_registry_metadataKeyAllowList
# - ZWE_configs_apiml_gateway_registry_refreshIntervalMs
# - ZWE_configs_certificate_keystore_alias / ZWE_zowe_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file / ZWE_zowe_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password / ZWE_zowe_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type / ZWE_zowe_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_key_password / ZWE_zowe_certificate_key_password
# - ZWE_configs_certificate_truststore_file / ZWE_zowe_certificate_truststore_file
# - ZWE_configs_certificate_truststore_type / ZWE_zowe_certificate_truststore_type
# - ZWE_configs_certificate_truststore_password / ZWE_zowe_certificate_truststore_password
# - ZWE_configs_certificate_ciphers / ZWE_configs_ciphers
# - ZWE_configs_debug
# - ZWE_configs_port - the port the api gateway service will use
# - ZWE_configs_server_maxConnectionsPerRoute
# - ZWE_configs_server_maxTotalConnections
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_server_webSocket_maxIdleTimeout
# - ZWE_configs_server_webSocket_connectTimeout
# - ZWE_configs_server_webSocket_asyncWriteTimeout
# - ZWE_configs_server_webSocket_requestBufferSize
# - ZWE_configs_spring_profiles_active
# - ZWE_zowe_network_server_tls_attls
# - ZWE_DISCOVERY_SERVICES_LIST

# JAR file location
if [ -n "${LAUNCH_COMPONENT}" ]; then
    JAR_FILE="${LAUNCH_COMPONENT}/gateway-service-lite.jar"
    . "scripts/apiml-common-scripts.sh"
    . "scripts/parse_jvm_args.sh"
else
    JAR_FILE="$(pwd)/bin/gateway-service-lite.jar"
    . "$(pwd)/bin/apiml-common-scripts.sh"
    . "$(pwd)/bin/parse_jvm_args.sh"
fi
echo "jar file: ${JAR_FILE}"

# Gateway-specific loader path
GATEWAY_LOADER_PATH=${COMMON_LIB}
if [ -n "${ZWE_GATEWAY_SHARED_LIBS}" ]; then
    GATEWAY_LOADER_PATH=${ZWE_GATEWAY_SHARED_LIBS},${GATEWAY_LOADER_PATH}
fi
echo "Setting loader path: ${GATEWAY_LOADER_PATH}"

# Debug profile
if [ "${ZWE_configs_debug}" = "true" ]; then
    add_profile "debug"
fi

# Cookie name for unique cookie support
if [ "${ZWE_configs_apiml_security_auth_uniqueCookie}" = "true" ]; then
    cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
fi

# AT-TLS server profile
if [ "${ATTLS_SERVER_ENABLED}" = "true" ]; then
    add_profile "attlsServer"
    ZWE_configs_server_ssl_enabled="false"
    ZWE_configs_apiml_service_corsEnabled=true
fi

# AT-TLS client profile
if [ "${ATTLS_CLIENT_ENABLED}" = "true" ]; then
    add_profile "attlsClient"
    ZWE_configs_apiml_service_corsEnabled=true
fi

# External protocol determination
if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" ] || [ "$ATTLS_SERVER_ENABLED" = "true" ]; then
    externalProtocol="https"
else
    externalProtocol="http"
fi

# External URL
if [ -n "${externalProtocol}" ] && [ -n "${ZWE_zowe_externalDomains_0}" ] && [ -n "${ZWE_zowe_externalPort}" ]; then
    EXTERNAL_URL="-Dapiml.service.externalUrl=${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}"
fi

# Gateway-specific library path
if [ -n "${ZWE_GATEWAY_LIBRARY_PATH}" ]; then
    LIBPATH="$LIBPATH":"${ZWE_GATEWAY_LIBRARY_PATH}"
fi

GATEWAY_CODE=AG
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${GATEWAY_CODE} ${JAVA_BIN_DIR}java \
    -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
    -XX:+ExitOnOutOfMemoryError \
    ${QUICK_START} \
    ${SHARED_CLASSES_OPTS} \
    ${JAVA21_CONSOLE_ENCODING} \
    ${ADD_OPENS} \
    ${LOGBACK} \
    ${JVM_SECURITY_PROPERTIES} \
    ${EXTERNAL_URL} \
    ${CUSTOM_JVM_OPTS} \
    -Dapiml.connection.idleConnectionTimeoutSeconds=${ZWE_configs_apiml_connection_idleConnectionTimeoutSeconds:-5} \
    -Dapiml.connection.timeout=${ZWE_configs_apiml_connection_timeout:-60000} \
    -Dapiml.connection.timeToLive=${ZWE_configs_apiml_connection_timeToLive:-10000} \
    -Dapiml.gateway.cachePeriodSec=${ZWE_configs_apiml_gateway_registry_cachePeriodSec:-120} \
    -Dapiml.gateway.cookieNameForRateLimit=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.gateway.maxSimultaneousRequests=${ZWE_configs_apiml_gateway_registry_maxSimultaneousRequests:-20} \
    -Dapiml.gateway.rateLimiterCapacity=${ZWE_configs_apiml_gateway_rateLimiterCapacity:-20} \
    -Dapiml.gateway.rateLimiterRefillDuration=${ZWE_configs_apiml_gateway_rateLimiterRefillDuration:-1} \
    -Dapiml.gateway.rateLimiterTokens=${ZWE_configs_apiml_gateway_rateLimiterTokens:-20} \
    -Dapiml.gateway.refresh-interval-ms=${ZWE_configs_apiml_gateway_registry_refreshIntervalMs:-30000} \
    -Dapiml.gateway.registry.enabled=${ZWE_configs_apiml_gateway_registry_enabled:-false} \
    -Dapiml.gateway.registry.metadata-key-allow-list=${ZWE_configs_apiml_gateway_registry_metadataKeyAllowList:-} \
    -Dapiml.gateway.servicesToLimitRequestRate=${ZWE_configs_apiml_gateway_servicesToLimitRequestRate:-} \
    -Dapiml.gateway.servicesToDisableRetry=${ZWE_configs_apiml_gateway_servicesToDisableRetry:-} \
    -Dapiml.health.protected=${ZWE_configs_apiml_health_protected:-true} \
    -Dapiml.httpclient.ssl.enabled-protocols=${client_enabled_protocols} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.security.auth.cookieProperties.cookieName=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.security.auth.jwt.customAuthHeader=${ZWE_configs_apiml_security_auth_jwt_customAuthHeader:-} \
    -Dapiml.security.auth.passticket.customAuthHeader=${ZWE_configs_apiml_security_auth_passticket_customAuthHeader:-} \
    -Dapiml.security.auth.passticket.customUserHeader=${ZWE_configs_apiml_security_auth_passticket_customUserHeader:-} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_configs_apiml_security_authorization_endpoint_enabled:-false} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_configs_apiml_security_authorization_endpoint_url:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf-auth"} \
    -Dapiml.security.authorization.provider=${ZWE_configs_apiml_security_authorization_provider:-"native"} \
    -Dapiml.security.forwardHeader.trustedProxies=${ZWE_configs_apiml_security_forwardHeader_trustedProxies:-} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices} \
    -Dapiml.security.x509.acceptForwardedCert=${ZWE_configs_apiml_security_x509_acceptForwardedCert:-false} \
    -Dapiml.security.x509.certificatesUrls=${ZWE_configs_apiml_security_x509_certificatesUrls:-${ZWE_configs_apiml_security_x509_certificatesUrl:-}} \
    -Dapiml.security.x509.enabled=${ZWE_configs_apiml_security_x509_enabled:-false} \
    -Dapiml.security.x509.registry.allowedUsers=${ZWE_configs_apiml_security_x509_registry_allowedUsers:-} \
    -Dapiml.service.allowEncodedSlashes=${ZWE_configs_apiml_service_allowEncodedSlashes:-true} \
    -Dapiml.service.apimlId=${ZWE_configs_apimlId:-} \
    -Dapiml.service.corsEnabled=${ZWE_configs_apiml_service_corsEnabled:-false} \
    -Dapiml.service.corsAllowedMethods=${ZWE_configs_apiml_service_corsAllowedMethods:-GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS} \
    -Dapiml.service.forwardClientCertEnabled=${ZWE_configs_apiml_security_x509_enabled:-false} \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-7554} \
    -Dapiml.zoweManifest=${ZWE_zowe_runtimeDirectory}/manifest.json \
    -Deureka.client.serviceUrl.defaultZone=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dfile.encoding=COMPAT \
    -Dibm.serversocket.recover=true \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Djava.library.path=${LIBPATH} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -Djdk.tls.client.cipherSuites=${client_ciphers} \
    -Dloader.path=${GATEWAY_LOADER_PATH} \
    -Dlogging.charset.console=${ZOWE_CONSOLE_LOG_CHARSET} \
    -Dserver.address=${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"}} \
    -Dserver.maxConnectionsPerRoute=${ZWE_configs_server_maxConnectionsPerRoute:-100} \
    -Dserver.maxTotalConnections=${ZWE_configs_server_maxTotalConnections:-1000} \
    -Dserver.ssl.ciphers=${server_ciphers} \
    -Dserver.ssl.enabled-protocols=${server_enabled_protocols} \
    -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true} \
    -Dserver.ssl.keyAlias="${key_alias}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyStoreType="${keystore_type}" \
    -Dserver.ssl.protocol=${server_protocol} \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Dserver.ssl.trustStoreType="${truststore_type}" \
    -Dserver.webSocket.asyncWriteTimeout=${ZWE_configs_server_webSocket_asyncWriteTimeout:-60000} \
    -Dserver.webSocket.connectTimeout=${ZWE_configs_server_webSocket_connectTimeout:-45000} \
    -Dserver.webSocket.maxIdleTimeout=${ZWE_configs_server_webSocket_maxIdleTimeout:-3600000} \
    -Dspring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=${ZWE_configs_server_webSocket_requestBufferSize:-24576} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dapiml.security.rauditx.onOidcUserIsMapped=${ZWE_configs_apiml_security_rauditx_onOidcUserIsMapped:-false} \
    -Dapiml.security.rauditx.oidcSourceUserPaths=${ZWE_configs_apiml_security_rauditx_oidcSourceUserPaths:-sub} \
    -Dotel.sdk.disabled=true \
    -jar "${JAR_FILE}" &

pid=$!
echo "pid=${pid}"

wait %1
