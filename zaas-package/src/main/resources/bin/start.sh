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
# - ZWE_zowe_certificate_keystore_alias - The default alias of the key within the keystore
# - ZWE_zowe_certificate_keystore_file - The default keystore to use for SSL certificates
# - ZWE_zowe_certificate_keystore_password - The default password to access the keystore supplied by KEYSTORE
# - ZWE_zowe_certificate_truststore_file
# - ZWE_zowe_job_prefix
# - ZWE_zowe_logDirectory
# - ZWE_zowe_runtimeDirectory
# - ZWE_zowe_workspaceDirectory

# Optional variables:
# - CMMN_LB
# - LIBPATH
# - LIBRARY_PATH
# - ZWE_components_discovery_port - the port the discovery service will use
# - ZWE_configs_heap_max
# - ZWE_configs_heap_init
# - ZWE_configs_apiml_security_auth_provider
# - ZWE_configs_apiml_security_allowtokenrefresh
# - ZWE_configs_apiml_security_auth_zosmf_jwtAutoconfiguration
# - ZWE_configs_apiml_security_auth_zosmf_serviceId
# - ZWE_configs_apiml_security_authorization_endpoint_enabled
# - ZWE_configs_apiml_security_authorization_endpoint_url
# - ZWE_configs_apiml_security_authorization_provider
# - ZWE_configs_apiml_security_authorization_resourceClass
# - ZWE_configs_apiml_security_authorization_resourceNamePrefix
# - ZWE_configs_apiml_security_jwtInitializerTimeout
# - ZWE_configs_apiml_security_useInternalMapper
# - ZWE_configs_apiml_security_x509_enabled
# - ZWE_configs_apiml_security_x509_externalMapperUrl
# - ZWE_configs_apiml_security_x509_externalMapperUser
# - ZWE_configs_apiml_security_x509_acceptForwardedCert
# - ZWE_configs_apiml_security_x509_certificatesUrl
# - ZWE_zosmf_applId
# - ZWE_configs_apiml_security_oidc_enabled
# - ZWE_configs_apiml_security_oidc_clientId
# - ZWE_configs_apiml_security_oidc_clientSecret
# - ZWE_configs_apiml_security_oidc_registry
# - ZWE_configs_apiml_security_oidc_identityMapperUrl
# - ZWE_configs_apiml_security_oidc_identityMapperUser
# - ZWE_configs_apiml_security_oidc_jwks_uri
# - ZWE_configs_apiml_security_oidc_jwks_refreshInternalHours
# - ZWE_configs_apiml_security_oidc_userInfo_uri
# - ZWE_configs_apiml_security_oidc_validationType
# - ZWE_configs_apiml_service_corsEnabled
# - ZWE_configs_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_truststore_file
# - ZWE_configs_certificate_truststore_type
# - ZWE_configs_debug
# - ZWE_configs_port - the port the ZAAS will use
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_spring_profiles_active
# - ZWE_DISCOVERY_SERVICES_LIST
# - ZWE_haInstance_hostname
# - ZWE_zowe_network_server_tls_attls
# - ZWE_zowe_network_client_tls_attls
# - ZWE_zowe_certificate_keystore_type - The default keystore type to use for SSL certificates
# - ZWE_zowe_verifyCertificates - if we accept only verified certificates


# JAR file location
if [ -n "${LAUNCH_COMPONENT}" ]; then
    JAR_FILE="${LAUNCH_COMPONENT}/zaas-service-lite.jar"
    . "scripts/apiml-common-scripts.sh"
    . "scripts/parse_jvm_args.sh"
else
    JAR_FILE="$(pwd)/bin/zaas-service-lite.jar"
    . "$(pwd)/bin/apiml-common-scripts.sh"
    . "$(pwd)/bin/parse_jvm_args.sh"
fi
echo "jar file: ${JAR_FILE}"

# ZAAS loader path (includes IRRRacf.jar on z/OS)
if [ "$(uname)" = "OS/390" ]; then
    ZAAS_LOADER_PATH=${COMMON_LIB},/usr/include/java_classes/IRRRacf.jar
else
    ZAAS_LOADER_PATH=${COMMON_LIB}
fi

# Check if the directory containing the ZAAS shared JARs was set and append it to the ZAAS loader path
if [ -n "${ZWE_ZAAS_SHARED_LIBS}" ]; then
    ZAAS_LOADER_PATH=${ZWE_ZAAS_SHARED_LIBS},${ZAAS_LOADER_PATH}
fi
echo "Setting loader path: ${ZAAS_LOADER_PATH}"

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
    ZWE_configs_server_ssl_enabled="false"
    add_profile "attlsServer"
fi

# AT-TLS client profile
if [ "${ATTLS_CLIENT_ENABLED}" = "true" ]; then
    add_profile "attlsClient"
fi

# External protocol determination
if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" ] || [ "$ATTLS_SERVER_ENABLED" = "true" ]; then
    externalProtocol="https"
else
    externalProtocol="http"
fi

# ZAAS-specific library path
if [ -n "${ZWE_ZAAS_LIBRARY_PATH}" ]; then
    LIBPATH="$LIBPATH":"${ZWE_ZAAS_LIBRARY_PATH}"
fi

# Certificates URLs
CERTIFICATES_URLS=${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/gateway/certificates
CERTIFICATES_URLS=${ZWE_configs_apiml_security_x509_certificatesUrl:-${ZWE_components_gateway_apiml_security_x509_certificatesUrl:-${CERTIFICATES_URLS}}}
CERTIFICATES_URLS=${ZWE_configs_apiml_security_x509_certificatesUrls:-${ZWE_components_gateway_apiml_security_x509_certificatesUrls:-${CERTIFICATES_URLS}}}

ZAAS_CODE=AZ
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${ZAAS_CODE} ${JAVA_BIN_DIR}java \
    -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
    ${QUICK_START} \
    ${SHARED_CLASSES_OPTS} \
    ${JAVA21_CONSOLE_ENCODING} \
    ${ADD_OPENS} \
    ${LOGBACK} \
    ${JVM_SECURITY_PROPERTIES} \
    ${CUSTOM_JVM_OPTS} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Dlogging.charset.console=${ZOWE_CONSOLE_LOG_CHARSET} \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-7558} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.connection.timeout=${ZWE_configs_apiml_connection_timeout:-60000} \
    -Dapiml.connection.timeToLive=${ZWE_configs_apiml_connection_timeToLive:-60000} \
    -Dapiml.connection.idleConnectionTimeoutSeconds=${ZWE_configs_apiml_connection_idleConnectionTimeoutSeconds:-5} \
    -Dapiml.cache.storage.location=${ZWE_zowe_workspaceDirectory}/api-mediation/${ZWE_haInstance_id:-localhost} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices:-true} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
    -Dapiml.security.auth.zosmf.serviceId=${ZWE_configs_apiml_security_auth_zosmf_serviceId:-${ZWE_components_gateway_apiml_security_auth_zosmf_serviceId:-ibmzosmf}} \
    -Dapiml.security.auth.provider=${ZWE_configs_apiml_security_auth_provider:-${ZWE_components_gateway_apiml_security_auth_provider:-zosmf}} \
    -Dapiml.security.auth.cookieProperties.cookieName=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.security.personalAccessToken.enabled=${ZWE_configs_apiml_security_personalAccessToken_enabled:-${ZWE_components_gateway_apiml_security_personalAccessToken_enabled:-false}} \
    -Dapiml.zoweManifest=${ZWE_zowe_runtimeDirectory}/manifest.json \
    -Dserver.address=${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"}} \
    -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true} \
    -Dapiml.service.ssl.enabled-protocols=${ZWE_configs_apiml_service_ssl_enabled_protocols:-${client_enabled_protocols}} \
    -Dapiml.service.ssl.ciphers=${ZWE_configs_apiml_service_ssl_ciphers:-${client_ciphers}} \
    -Dapiml.service.ssl.key-alias="${ZWE_configs_apiml_service_ssl_key_alias:-${key_alias}}" \
    -Dapiml.service.ssl.key-password="${ZWE_configs_apiml_service_ssl_key_password:-${key_pass}}" \
    -Dapiml.service.ssl.key-store="${ZWE_configs_apiml_service_ssl_key_store:-${keystore_location}}" \
    -Dapiml.service.ssl.key-store-password="${ZWE_configs_apiml_service_ssl_key_store_password:-${keystore_pass}}" \
    -Dapiml.service.ssl.key-store-type="${ZWE_configs_apiml_service_ssl_key_store_type:-${keystore_type}}" \
    -Dapiml.service.ssl.protocol=${ZWE_configs_apiml_service_ssl_protocol:-${server_protocol}} \
    -Dapiml.service.ssl.trust-store="${ZWE_configs_apiml_service_ssl_trust_store:-${truststore_location}}" \
    -Dapiml.service.ssl.trust-store-password="${ZWE_configs_apiml_service_ssl_trust_store_password:-${truststore_pass}}" \
    -Dapiml.service.ssl.trust-store-type="${ZWE_configs_apiml_service_ssl_trust_store_type:-${truststore_type}}" \
    -Djdk.tls.client.cipherSuites=${client_ciphers} \
    -Dserver.ssl.ciphers=${server_ciphers} \
    -Dserver.ssl.protocol=${server_protocol} \
    -Dserver.ssl.enabled-protocols=${server_enabled_protocols} \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStoreType="${keystore_type}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyAlias="${key_alias}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStoreType="${truststore_type}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Dapiml.health.protected=${ZWE_configs_apiml_health_protected:-true} \
    -Dapiml.security.auth.zosmf.jwtAutoconfiguration=${ZWE_configs_apiml_security_auth_zosmf_jwtAutoconfiguration:-${ZWE_components_gateway_apiml_security_auth_zosmf_jwtAutoconfiguration:-jwt}} \
    -Dapiml.security.jwtInitializerTimeout=${ZWE_configs_apiml_security_jwtInitializerTimeout:-${ZWE_components_gateway_apiml_security_jwtInitializerTimeout:-5}} \
    -Dapiml.security.useInternalMapper=${ZWE_configs_apiml_security_useInternalMapper:-${ZWE_components_gateway_apiml_security_useInternalMapper:-true}} \
    -Dapiml.security.x509.enabled=${ZWE_configs_apiml_security_x509_enabled:-${ZWE_components_gateway_apiml_security_x509_enabled:-false}} \
    -Dapiml.security.x509.externalMapperUrl=${ZWE_configs_apiml_security_x509_externalMapperUrl:-${ZWE_components_gateway_apiml_security_x509_externalMapperUrl:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/certificate/x509/map"}} \
    -Dapiml.security.x509.externalMapperUser=${ZWE_configs_apiml_security_x509_externalMapperUser:-${ZWE_components_gateway_apiml_security_x509_externalMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}}} \
    -Dapiml.security.x509.acceptForwardedCert=${ZWE_configs_apiml_security_x509_enabled:-${ZWE_components_gateway_apiml_security_x509_enabled:-true}} \
    -Dapiml.security.x509.certificatesUrls=${CERTIFICATES_URLS} \
    -Dapiml.security.authorization.provider=${ZWE_configs_apiml_security_authorization_provider:-${ZWE_components_gateway_apiml_security_authorization_provider:-"native"}} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_configs_apiml_security_authorization_endpoint_enabled:-${ZWE_components_gateway_apiml_security_authorization_endpoint_enabled:-false}} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_configs_apiml_security_authorization_endpoint_url:-${ZWE_components_gateway_apiml_security_authorization_endpoint_url:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf-auth"}} \
    -Dapiml.security.saf.provider=${ZWE_configs_apiml_security_saf_provider:-${ZWE_components_gateway_apiml_security_saf_provider:-"rest"}} \
    -Dapiml.security.saf.urls.authenticate=${ZWE_configs_apiml_security_saf_urls_authenticate:-${ZWE_components_gateway_apiml_security_saf_urls_authenticate:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf/authenticate"}} \
    -Dapiml.security.saf.urls.verify=${ZWE_configs_apiml_security_saf_urls_verify:-${ZWE_components_gateway_apiml_security_saf_urls_verify:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf/verify"}} \
    -Dapiml.security.authorization.resourceClass=${ZWE_configs_apiml_security_authorization_resourceClass:-${ZWE_components_gateway_apiml_security_authorization_resourceClass:-ZOWE}} \
    -Dapiml.security.authorization.resourceNamePrefix=${ZWE_configs_apiml_security_authorization_resourceNamePrefix:-${ZWE_components_gateway_apiml_security_authorization_resourceNamePrefix:-APIML.}} \
    -Dapiml.security.zosmf.applid=${ZWE_zosmf_applId:-IZUDFLT} \
    -Dapiml.security.oidc.enabled=${ZWE_configs_apiml_security_oidc_enabled:-${ZWE_components_gateway_apiml_security_oidc_enabled:-false}} \
    -Dapiml.security.oidc.registry=${ZWE_configs_apiml_security_oidc_registry:-${ZWE_components_gateway_apiml_security_oidc_registry:-}} \
    -Dapiml.security.oidc.identityMapperUrl=${ZWE_configs_apiml_security_oidc_identityMapperUrl:-${ZWE_components_gateway_apiml_security_oidc_identityMapperUrl:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/certificate/dn"}} \
    -Dapiml.security.oidc.identityMapperUser=${ZWE_configs_apiml_security_oidc_identityMapperUser:-${ZWE_components_gateway_apiml_security_oidc_identityMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}}} \
    -Dapiml.security.oidc.jwks.uri=${ZWE_configs_apiml_security_oidc_jwks_uri:-${ZWE_components_gateway_apiml_security_oidc_jwks_uri:-}} \
    -Dapiml.security.oidc.jwks.refreshInternalHours=${ZWE_configs_apiml_security_oidc_jwks_refreshInternalHours:-${ZWE_components_gateway_apiml_security_oidc_jwks_refreshInternalHours:-1}} \
    -Dapiml.security.oidc.userIdField=${ZWE_configs_apiml_security_oidc_userIdField:-${ZWE_components_gateway_apiml_security_oidc_userIdField:-"sub"}} \
    -Dapiml.security.oidc.userInfo.uri=${ZWE_configs_apiml_security_oidc_userInfo_uri:-${ZWE_components_gateway_apiml_security_oidc_userInfo_uri:-}} \
    -Dapiml.security.oidc.validationType=${ZWE_configs_apiml_security_oidc_validationType:-${ZWE_components_gateway_apiml_security_oidc_validationType:-"JWK"}} \
    -Dapiml.security.allowTokenRefresh=${ZWE_configs_apiml_security_allowtokenrefresh:-${ZWE_components_gateway_apiml_security_allowtokenrefresh:-false}} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${ZAAS_LOADER_PATH} \
    -Djava.library.path=${LIBPATH} \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -Dotel.sdk.disabled=true \
    -jar ${JAR_FILE} &

pid=$!
echo "pid=${pid}"

wait %1
