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
# - CMMN_LB
# - LIBPATH
# - LIBRARY_PATH
# - ZWE_components_discovery_port - the port the discovery service will use
# - ZWE_configs_apiml_connection_idleConnectionTimeoutSeconds
# - ZWE_configs_apiml_connection_timeout
# - ZWE_configs_apiml_connection_timeToLive
# - ZWE_configs_apiml_discovery_serviceIdPrefixReplacer - The service ID prefix replacer to be V2 conformant
# - ZWE_configs_apiml_gateway_registry_cachePeriodSec
# - ZWE_configs_apiml_gateway_registry_enabled
# - ZWE_configs_apiml_gateway_registry_maxSimultaneousRequests
# - ZWE_configs_apiml_gateway_registry_metadataKeyAllowList
# - ZWE_configs_apiml_gateway_registry_refreshIntervalMs
# - ZWE_configs_apiml_health_protected
# - ZWE_configs_apiml_security_auth_jwt_customAuthHeader
# - ZWE_configs_apiml_security_auth_passticket_customAuthHeader
# - ZWE_configs_apiml_security_auth_passticket_customUserHeader
# - ZWE_configs_apiml_security_authorization_endpoint_enabled
# - ZWE_configs_apiml_security_authorization_endpoint_url
# - ZWE_configs_apiml_security_authorization_provider
# - ZWE_configs_apiml_security_x509_acceptForwardedCert
# - ZWE_configs_apiml_security_x509_certificatesUrl
# - ZWE_configs_apiml_security_x509_enabled
# - ZWE_configs_apiml_security_x509_registry_allowedUsers
# - ZWE_configs_apiml_service_allowEncodedSlashes
# - ZWE_configs_apiml_service_corsEnabled
# - ZWE_configs_apiml_service_forwardClientCertEnabled
# - ZWE_configs_apimlId
# - ZWE_configs_certificate_ciphers / ZWE_configs_ciphers
# - ZWE_configs_certificate_key_password / ZWE_zowe_certificate_key_password
# - ZWE_configs_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_alias / ZWE_zowe_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_file / ZWE_zowe_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_password / ZWE_zowe_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_keystore_type / ZWE_zowe_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_truststore_file
# - ZWE_configs_certificate_truststore_file / ZWE_zowe_certificate_truststore_file
# - ZWE_configs_certificate_truststore_password / ZWE_zowe_certificate_truststore_password
# - ZWE_configs_certificate_truststore_type
# - ZWE_configs_certificate_truststore_type / ZWE_zowe_certificate_truststore_type
# - ZWE_configs_debug
# - ZWE_configs_heap_init
# - ZWE_configs_heap_max
# - ZWE_configs_port - the port the api discovery service will use
# - ZWE_configs_port - the port the api gateway service will use
# - ZWE_configs_server_maxConnectionsPerRoute
# - ZWE_configs_server_maxTotalConnections
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_server_webSocket_asyncWriteTimeout
# - ZWE_configs_server_webSocket_connectTimeout
# - ZWE_configs_server_webSocket_maxIdleTimeout
# - ZWE_configs_server_webSocket_requestBufferSize
# - ZWE_configs_spring_profiles_active
# - ZWE_configs_sslDebug
# - ZWE_DISCOVERY_SERVICES_LIST
# - ZWE_GATEWAY_SHARED_LIBS
# - ZWE_haInstance_hostname
# - ZWE_zowe_certificate_keystore_alias - The default alias of the key within the keystore
# - ZWE_zowe_certificate_keystore_file - The default keystore to use for SSL certificates
# - ZWE_zowe_certificate_keystore_password - The default password to access the keystore supplied by KEYSTORE
# - ZWE_zowe_certificate_keystore_type - The default keystore type to use for SSL certificates
# - ZWE_zowe_certificate_truststore_file
# - ZWE_zowe_job_prefix
# - ZWE_zowe_logDirectory
# - ZWE_zowe_network_server_tls_attls
# - ZWE_zowe_verifyCertificates - if we accept only verified certificates
# - ZWE_configs_storage_evictionStrategy
# - ZWE_configs_storage_mode
# - ZWE_configs_storage_size
# - ZWE_configs_storage_vsam_name
# - ZWE_configs_jvm_* - Any environment variable with this prefix will be passed as a JVM parameter
#                       Example: ZWE_configs_jvm_Xss=512k becomes -Xss512k
#                                ZWE_configs_jvm_Dmy_custom_property=value becomes -Dmy.custom.property=value
#                       Note: Underscores in the property name (after 'D') are converted to dots

# Source common APIML scripts (sets up common variables and functions)
if [ -n "${LAUNCH_COMPONENT}" ]; then
    echo "lnch ${LAUNCH_COMPONENT}/apiml-common-scripts.sh"
    JAR_FILE="${LAUNCH_COMPONENT}/api-catalog-services-lite.jar"
    . "scripts/apiml-common-scripts.sh"
    . "scripts/parse_jvm_args.sh"
else
    echo "$(pwd)/bin/apiml-common-scripts.sh"
    JAR_FILE="$(pwd)/bin/api-catalog-services-lite.jar"
    . "$(pwd)/bin/apiml-common-scripts.sh"
    . "$(pwd)/bin/parse_jvm_args.sh"
fi

# JAR file location
if [ -n "${LAUNCH_COMPONENT}" ]; then
    JAR_FILE="${LAUNCH_COMPONENT}/apiml-lite.jar"
else
    JAR_FILE="$(pwd)/bin/apiml-lite.jar"
fi
echo "jar file: ${JAR_FILE}"

# APIML-lite loader path (includes IRRRacf.jar on z/OS)
if [ "$(uname)" = "OS/390" ]; then
    APIML_LOADER_PATH=${COMMON_LIB},/usr/include/java_classes/IRRRacf.jar
else
    APIML_LOADER_PATH=${COMMON_LIB}
fi

# Check if the directory containing the shared JARs was set and append it to the loader path
if [ -n "${ZWE_GATEWAY_SHARED_LIBS}" ]; then
    APIML_LOADER_PATH=${ZWE_GATEWAY_SHARED_LIBS},${APIML_LOADER_PATH}
fi
if [ -n "${ZWE_DISCOVERY_SHARED_LIBS}" ]; then
    APIML_LOADER_PATH=${ZWE_DISCOVERY_SHARED_LIBS},${APIML_LOADER_PATH}
fi
echo "Setting loader path: ${APIML_LOADER_PATH}"

# Debug profile
if [ "${ZWE_components_gateway_debug:-${ZWE_configs_debug:-false}}" = "true" ]; then
    if [ -n "${ZWE_configs_spring_profiles_active:-${ZWE_components_gateway_spring_profiles_active:-${ZWE_components_discovery_spring_profiles_active}}}" ]; then
        ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active:-${ZWE_components_gateway_spring_profiles_active:-${ZWE_components_discovery_spring_profiles_active}}}"
    fi
    add_profile "debug"
fi

# Cookie name for unique cookie support
if [ "${ZWE_configs_apiml_security_auth_uniqueCookie:-${ZWE_components_gateway_apiml_security_auth_uniqueCookie:-false}}" = "true" ]; then
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
if [ "${ZWE_configs_server_ssl_enabled:-${ZWE_components_gateway_server_ssl_enabled:-${ZWE_components_discovery_server_ssl_enabled:-true}}}" = "true" ] || [ "$ATTLS_SERVER_ENABLED" = "true" ]; then
    externalProtocol="https"
else
    externalProtocol="http"
fi

# VSAM file name for caching
if [ -n "${ZWE_configs_storage_vsam_name}" ]; then
    VSAM_FILE_NAME=//\'${ZWE_configs_storage_vsam_name:-${ZWE_components_caching_service_storage_vsam_name}}\'
fi

# External URL
if [ -n "${externalProtocol}" ] && [ -n "${ZWE_zowe_externalDomains_0}" ] && [ -n "${ZWE_zowe_externalPort}" ]; then
    EXTERNAL_URL="-Dapiml.service.externalUrl=${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}"
fi

# Gateway-specific library path
if [ -n "${ZWE_GATEWAY_LIBRARY_PATH}" ]; then
    LIBPATH="$LIBPATH":"${ZWE_GATEWAY_LIBRARY_PATH}"
fi

echo "QUICK_START: ${QUICK_START}"
echo "SHARED_CLASSES_OPTS: ${SHARED_CLASSES_OPTS}"
echo "JAVA21_CONSOLE_ENCODING: ${JAVA21_CONSOLE_ENCODING}"
echo "ADD_OPENS: ${ADD_OPENS}"
echo "LOGBACK: ${LOGBACK}"
echo "JVM_SECURITY_PROPERTIES: ${JVM_SECURITY_PROPERTIES}"
echo "EXTERNAL_URL: ${EXTERNAL_URL}"
echo "CUSTOM_JVM_OPTS: ${CUSTOM_JVM_OPTS}"

APIML_CODE=AG
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${APIML_CODE} ${JAVA_BIN_DIR}java \
    -Xms${ZWE_configs_heap_init:-${ZWE_components_gateway_heap_init:-32}}m -Xmx${ZWE_configs_heap_max:-${ZWE_components_gateway_heap_max:-512}}m \
    -XX:+ExitOnOutOfMemoryError \
    ${QUICK_START} \
    ${SHARED_CLASSES_OPTS} \
    ${JAVA21_CONSOLE_ENCODING} \
    ${ADD_OPENS} \
    ${LOGBACK} \
    ${JVM_SECURITY_PROPERTIES} \
    ${EXTERNAL_URL} \
    ${CUSTOM_JVM_OPTS} \
    -Dapiml.cache.storage.location=${ZWE_zowe_workspaceDirectory}/api-mediation/${ZWE_haInstance_id:-localhost} \
    -Dapiml.catalog.customStyle.backgroundColor=${ZWE_components_apicatalog_apiml_catalog_customStyle_backgroundColor:-${ZWE_configs_apiml_catalog_customStyle_backgroundColor:-}} \
    -Dapiml.catalog.customStyle.docLink=${ZWE_components_apicatalog_apiml_catalog_customStyle_docLink:-${ZWE_configs_apiml_catalog_customStyle_docLink:-}} \
    -Dapiml.catalog.customStyle.fontFamily=${ZWE_components_apicatalog_apiml_catalog_customStyle_fontFamily:-${ZWE_configs_apiml_catalog_customStyle_fontFamily:-}} \
    -Dapiml.catalog.customStyle.headerColor=${ZWE_components_apicatalog_apiml_catalog_customStyle_headerColor:-${ZWE_configs_apiml_catalog_customStyle_headerColor:-}} \
    -Dapiml.catalog.customStyle.logo=${ZWE_components_apicatalog_apiml_catalog_customStyle_logo:-${ZWE_configs_apiml_catalog_customStyle_logo:-}} \
    -Dapiml.catalog.customStyle.textColor=${ZWE_components_apicatalog_apiml_catalog_customStyle_textColor:-${ZWE_configs_apiml_catalog_customStyle_textColor:-}} \
    -Dapiml.catalog.customStyle.titlesColor=${ZWE_components_apicatalog_apiml_catalog_customStyle_titlesColor:-${ZWE_configs_apiml_catalog_customStyle_titlesColor:-}} \
    -Dapiml.catalog.hide.serviceInfo=${ZWE_components_apicatalog_apiml_catalog_hide_serviceInfo:-${ZWE_configs_apiml_catalog_hide_serviceInfo:-false}} \
    -Dapiml.connection.idleConnectionTimeoutSeconds=${ZWE_components_gateway_apiml_connection_idleConnectionTimeoutSeconds:-${ZWE_configs_apiml_connection_idleConnectionTimeoutSeconds:-5}} \
    -Dapiml.connection.timeout=${ZWE_components_gateway_apiml_connection_timeout:-${ZWE_configs_apiml_connection_timeout:-60000}} \
    -Dapiml.connection.timeToLive=${ZWE_components_gateway_apiml_connection_timeToLive:-${ZWE_configs_apiml_connection_timeToLive:-10000}} \
    -Dapiml.discovery.allPeersUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.discovery.password=password \
    -Dapiml.discovery.serviceIdPrefixReplacer=${ZWE_components_discovery_apiml_discovery_serviceIdPrefixReplacer:-${ZWE_configs_apiml_discovery_serviceIdPrefixReplacer}} \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${ZWE_STATIC_DEFINITIONS_DIR:-} \
    -Dapiml.discovery.userid=eureka \
    -Dapiml.gateway.cachePeriodSec=${ZWE_components_gateway_apiml_gateway_registry_cachePeriodSec:-${ZWE_configs_apiml_gateway_registry_cachePeriodSec:-120}} \
    -Dapiml.gateway.cookieNameForRateLimit=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.gateway.maxSimultaneousRequests=${ZWE_components_gateway_gateway_registry_maxSimultaneousRequests:-${ZWE_configs_gateway_registry_maxSimultaneousRequests:-20}} \
    -Dapiml.gateway.rateLimiterCapacity=${ZWE_components_gateway_apiml_gateway_rateLimiterCapacity:-${ZWE_configs_apiml_gateway_rateLimiterCapacity:-20}} \
    -Dapiml.gateway.rateLimiterRefillDuration=${ZWE_components_gateway_apiml_gateway_rateLimiterRefillDuration:-${ZWE_configs_apiml_gateway_rateLimiterRefillDuration:-1}} \
    -Dapiml.gateway.rateLimiterTokens=${ZWE_components_gateway_apiml_gateway_rateLimiterTokens:-${ZWE_configs_apiml_gateway_rateLimiterTokens:-20}} \
    -Dapiml.gateway.refresh-interval-ms=${ZWE_components_gateway_gateway_registry_refreshIntervalMs:-${ZWE_configs_gateway_registry_refreshIntervalMs:-30000}} \
    -Dapiml.gateway.registry.enabled=${ZWE_components_gateway_apiml_gateway_registry_enabled:-${ZWE_configs_apiml_gateway_registry_enabled:-false}} \
    -Dapiml.gateway.registry.metadata-key-allow-list=${ZWE_components_gateway_gateway_registry_metadataKeyAllowList:-${ZWE_configs_gateway_registry_metadataKeyAllowList:-}} \
    -Dapiml.gateway.servicesToLimitRequestRate=${ZWE_components_gateway_apiml_gateway_servicesToLimitRequestRate:-${ZWE_configs_apiml_gateway_servicesToLimitRequestRate:-}} \
    -Dapiml.gateway.servicesToDisableRetry=${ZWE_components_gateway_apiml_gateway_servicesToDisableRetry:-${ZWE_configs_apiml_gateway_servicesToDisableRetry:-}} \
    -Dapiml.health.protected=${ZWE_components_gateway_apiml_health_protected:-${ZWE_configs_apiml_health_protected:-true}} \
    -Dapiml.httpclient.ssl.enabled-protocols=${client_enabled_protocols} \
    -Dapiml.internal-discovery.port=${ZWE_components_discovery_port:-${ZWE_configs_internal_discovery_port:-7553}} \
    -Dapiml.internal-discovery.address=${ZWE_configs_internal_discovery_address:-${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"}}} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.security.allowTokenRefresh=${ZWE_components_gateway_apiml_security_allowtokenrefresh:-${ZWE_configs_apiml_security_allowtokenrefresh:-false}} \
    -Dapiml.security.auth.cookieProperties.cookieName=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.security.auth.jwt.customAuthHeader=${ZWE_components_gateway_apiml_security_auth_jwt_customAuthHeader:-${ZWE_configs_apiml_security_auth_jwt_customAuthHeader:-}} \
    -Dapiml.security.auth.passticket.customAuthHeader=${ZWE_components_gateway_apiml_security_auth_passticket_customAuthHeader:-${ZWE_configs_apiml_security_auth_passticket_customAuthHeader:-}} \
    -Dapiml.security.auth.passticket.customUserHeader=${ZWE_components_gateway_apiml_security_auth_passticket_customUserHeader:-${ZWE_configs_apiml_security_auth_passticket_customUserHeader:-}} \
    -Dapiml.security.auth.provider=${ZWE_components_gateway_apiml_security_auth_provider:-${ZWE_configs_apiml_security_auth_provider:-zosmf}} \
    -Dapiml.security.auth.zosmf.jwtAutoconfiguration=${ZWE_components_gateway_apiml_security_auth_zosmf_jwtAutoconfiguration:-${ZWE_configs_apiml_security_auth_zosmf_jwtAutoconfiguration:-jwt}} \
    -Dapiml.security.auth.zosmf.serviceId=${ZWE_components_gateway_apiml_security_auth_zosmf_serviceId:-${ZWE_configs_apiml_security_auth_zosmf_serviceId:-ibmzosmf}} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_components_gateway_apiml_security_authorization_endpoint_enabled:-${ZWE_configs_apiml_security_authorization_endpoint_enabled:-false}} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_components_gateway_apiml_security_authorization_endpoint_url:-${ZWE_configs_apiml_security_authorization_endpoint_url:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf-auth"}} \
    -Dapiml.security.authorization.provider=${ZWE_components_gateway_apiml_security_authorization_provider:-${ZWE_configs_apiml_security_authorization_provider:-"native"}} \
    -Dapiml.security.authorization.resourceClass=${ZWE_components_gateway_apiml_security_authorization_resourceClass:-${ZWE_configs_apiml_security_authorization_resourceClass:-ZOWE}} \
    -Dapiml.security.authorization.resourceNamePrefix=${ZWE_components_gateway_apiml_security_authorization_resourceNamePrefix:-${ZWE_configs_apiml_security_authorization_resourceNamePrefix:-APIML.}} \
    -Dapiml.security.jwtInitializerTimeout=${ZWE_components_gateway_apiml_security_jwtInitializerTimeout:-${ZWE_configs_apiml_security_jwtInitializerTimeout:-5}} \
    -Dapiml.security.oidc.enabled=${ZWE_components_gateway_apiml_security_oidc_enabled:-${ZWE_configs_apiml_security_oidc_enabled:-false}} \
    -Dapiml.security.oidc.identityMapperUrl=${ZWE_components_gateway_apiml_security_oidc_identityMapperUrl:-${ZWE_configs_apiml_security_oidc_identityMapperUrl:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/certificate/dn"}} \
    -Dapiml.security.oidc.identityMapperUser=${ZWE_components_gateway_apiml_security_oidc_identityMapperUser:-${ZWE_configs_apiml_security_oidc_identityMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}}} \
    -Dapiml.security.oidc.jwks.refreshInternalHours=${ZWE_components_gateway_apiml_security_oidc_jwks_refreshInternalHours:-${ZWE_configs_apiml_security_oidc_jwks_refreshInternalHours:-1}} \
    -Dapiml.security.oidc.jwks.uri=${ZWE_components_gateway_apiml_security_oidc_jwks_uri:-${ZWE_configs_apiml_security_oidc_jwks_uri:-}} \
    -Dapiml.security.oidc.registry=${ZWE_components_gateway_apiml_security_oidc_registry:-${ZWE_configs_apiml_security_oidc_registry:-}} \
    -Dapiml.security.oidc.userIdField=${ZWE_components_gateway_apiml_security_oidc_userIdField:-${ZWE_configs_apiml_security_oidc_userIdField:-"sub"}} \
    -Dapiml.security.oidc.userInfo.uri=${ZWE_components_gateway_apiml_security_oidc_userInfo_uri:-${ZWE_configs_apiml_security_oidc_userInfo_uri:-}} \
    -Dapiml.security.oidc.validationType=${ZWE_components_gateway_apiml_security_oidc_validationType:-${ZWE_configs_apiml_security_oidc_validationType:-"JWK"}} \
    -Dapiml.security.personalAccessToken.enabled=${ZWE_components_gateway_apiml_security_personalAccessToken_enabled:-${ZWE_configs_apiml_security_personalAccessToken_enabled:-false}} \
    -Dapiml.security.saf.provider=${ZWE_components_gateway_apiml_security_saf_provider:-${ZWE_configs_apiml_security_saf_provider:-"rest"}} \
    -Dapiml.security.saf.urls.authenticate=${ZWE_components_gateway_apiml_security_saf_urls_authenticate:-${ZWE_configs_apiml_security_saf_urls_authenticate:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf/authenticate"}} \
    -Dapiml.security.saf.urls.verify=${ZWE_components_gateway_apiml_security_saf_urls_verify:-${ZWE_configs_apiml_security_saf_urls_verify:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf/verify"}} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices} \
    -Dapiml.security.useInternalMapper=${ZWE_components_gateway_apiml_security_useInternalMapper:-${ZWE_configs_apiml_security_useInternalMapper:-true}} \
    -Dapiml.security.x509.acceptForwardedCert=${ZWE_components_gateway_apiml_security_x509_acceptForwardedCert:-${ZWE_configs_apiml_security_x509_acceptForwardedCert:-false}} \
    -Dapiml.security.x509.certificatesUrls=${ZWE_components_gateway_apiml_security_x509_certificatesUrls:-${ZWE_components_gateway_apiml_security_x509_certificatesUrl:-${ZWE_configs_apiml_security_x509_certificatesUrls:-${ZWE_configs_apiml_security_x509_certificatesUrl}}}} \
    -Dapiml.security.x509.enabled=${ZWE_components_gateway_apiml_security_x509_enabled:-${ZWE_configs_apiml_security_x509_enabled:-false}} \
    -Dapiml.security.x509.externalMapperUrl=${ZWE_components_gateway_apiml_security_x509_externalMapperUrl:-${ZWE_configs_apiml_security_x509_externalMapperUrl:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/certificate/x509/map"}} \
    -Dapiml.security.x509.externalMapperUser=${ZWE_components_gateway_apiml_security_x509_externalMapperUser:-${ZWE_configs_apiml_security_x509_externalMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}}} \
    -Dapiml.security.x509.registry.allowedUsers=${ZWE_components_gateway_apiml_security_x509_registry_allowedUsers:-${ZWE_configs_apiml_security_x509_registry_allowedUsers:-}} \
    -Dapiml.security.zosmf.applid=${ZWE_components_gateway_apiml_security_zosmf_applid:-${ZWE_configs_apiml_security_zosmf_applid:-IZUDFLT}} \
    -Dapiml.service.allowEncodedSlashes=${ZWE_components_gateway_apiml_service_allowEncodedSlashes:-${ZWE_configs_apiml_service_allowEncodedSlashes:-true}} \
    -Dapiml.service.apimlId=${ZWE_components_gateway_apimlId:-${ZWE_configs_apimlId:-}} \
    -Dapiml.service.corsEnabled=${ZWE_components_gateway_apiml_service_corsEnabled:-${ZWE_configs_apiml_service_corsEnabled:-false}} \
    -Dapiml.service.corsAllowedMethods=${ZWE_components_gateway_apiml_service_corsAllowedMethods:-${ZWE_configs_apiml_service_corsAllowedMethods:-GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS}} \
    -Dapiml.service.forwardClientCertEnabled=${ZWE_components_gateway_apiml_security_x509_enabled:-${ZWE_configs_apiml_security_x509_enabled:-false}} \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_components_gateway_port:-${ZWE_configs_port:-7554}} \
    -Dapiml.zoweManifest=${ZWE_zowe_runtimeDirectory}/manifest.json \
    -Dcaching.storage.evictionStrategy=${ZWE_components_caching_service_storage_evictionStrategy:-${ZWE_configs_storage_evictionStrategy:-reject}} \
    -Dcaching.storage.infinispan.initialHosts=${ZWE_components_caching_service_storage_infinispan_initialHosts:-${ZWE_configs_storage_infinispan_initialHosts:-"localhost[7600]"}} \
    -Dcaching.storage.mode=${ZWE_components_caching_service_storage_mode:-${ZWE_configs_storage_mode:-infinispan}} \
    -Dcaching.storage.size=${ZWE_components_caching_service_storage_size:-${ZWE_configs_storage_size:-10000}} \
    -Dcaching.storage.vsam.name=${VSAM_FILE_NAME} \
    -Deureka.client.serviceUrl.defaultZone=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dfile.encoding=UTF-8 \
    -Dibm.serversocket.recover=true \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Djava.library.path=${LIBPATH} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-${ZWE_components_gateway_sslDebug:-${ZWE_components_discovery_sslDebug:-""}}} \
    -Djdk.tls.client.cipherSuites=${client_ciphers} \
    -Djgroups.bind.address=${ZWE_components_caching_service_storage_infinispan_jgroups_host:-${ZWE_configs_storage_infinispan_jgroups_host:-${ZWE_haInstance_hostname:-localhost}}} \
    -Djgroups.bind.port=${ZWE_components_caching_service_storage_infinispan_jgroups_port:-${ZWE_configs_storage_infinispan_jgroups_port:-7600}} \
    -Djgroups.keyExchange.port=${ZWE_components_caching_service_storage_infinispan_jgroups_keyExchange_port:-${ZWE_configs_storage_infinispan_jgroups_keyExchange_port:-7601}} \
    -Djgroups.tcp.diag.enabled=${ZWE_components_caching_service_storage_infinispan_jgroups_tcp_diag_enabled:-${ZWE_configs_storage_infinispan_jgroups_tcp_diag_enabled:-false}} \
    -Dloader.path=${APIML_LOADER_PATH} \
    -Dlogging.charset.console=${ZOWE_CONSOLE_LOG_CHARSET} \
    -Dserver.address=${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"}} \
    -Dserver.maxConnectionsPerRoute=${ZWE_components_gateway_server_maxConnectionsPerRoute:-${ZWE_configs_server_maxConnectionsPerRoute:-100}} \
    -Dserver.maxTotalConnections=${ZWE_components_gateway_server_maxTotalConnections:-${ZWE_configs_server_maxTotalConnections:-1000}} \
    -Dserver.ssl.ciphers=${server_ciphers} \
    -Dserver.ssl.enabled-protocols=${server_enabled_protocols} \
    -Dserver.ssl.enabled=${ZWE_components_gateway_server_ssl_enabled:-${ZWE_configs_server_ssl_enabled:-true}} \
    -Dserver.ssl.keyAlias="${key_alias}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyStoreType="${keystore_type}" \
    -Dserver.ssl.protocol=${server_protocol} \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Dserver.ssl.trustStoreType="${truststore_type}" \
    -Dserver.webSocket.asyncWriteTimeout=${ZWE_components_gateway_server_webSocket_asyncWriteTimeout:-${ZWE_configs_server_webSocket_asyncWriteTimeout:-60000}} \
    -Dserver.webSocket.connectTimeout=${ZWE_components_gateway_server_webSocket_connectTimeout:-${ZWE_configs_server_webSocket_connectTimeout:-45000}} \
    -Dserver.webSocket.maxIdleTimeout=${ZWE_components_gateway_server_webSocket_maxIdleTimeout:-${ZWE_configs_server_webSocket_maxIdleTimeout:-3600000}} \
    -Dspring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=${ZWE_components_gateway_server_webSocket_requestBufferSize:-${ZWE_configs_server_webSocket_requestBufferSize:-8192}} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dapiml.security.rauditx.onOidcUserIsMapped=${ZWE_configs_apiml_security_rauditx_onOidcUserIsMapped:-${ZWE_components_gateway_apiml_security_rauditx_onOidcUserIsMapped:-false}} \
    -Dapiml.security.rauditx.oidcSourceUserPaths=${ZWE_configs_apiml_security_rauditx_oidcSourceUserPaths:-${ZWE_components_gateway_apiml_security_rauditx_oidcSourceUserPaths:-sub}} \
    -jar "${JAR_FILE}" &

pid=$!
echo "pid=${pid}"

wait %1
