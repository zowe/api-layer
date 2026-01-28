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
# Optional variables:

if [ -n "${LAUNCH_COMPONENT}" ]; then
    JAR_FILE="${LAUNCH_COMPONENT}/apiml-lite.jar"
else
    JAR_FILE="$(pwd)/bin/apiml-lite.jar"
fi
echo "jar file: "${JAR_FILE}
# script assumes it's in the apiml component directory and common_lib needs to be relative path

if [ -z "${CMMN_LB}" ]; then
    COMMON_LIB="../apiml-common-lib/bin/BOOT-INF/lib/"
else
    COMMON_LIB="${CMMN_LB}"
fi

# script assumes it's in the apiml component directory and jvm.security.override.properties needs to be relative path
JVM_SECURITY_PROPERTIES=""
if [ "${JVM_SECURITY_PROPERTIES_OVERRIDE:-false}" = "true" ]; then
    JVM_SECURITY_PROPERTIES="-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties"
fi

if [ -z "${LIBRARY_PATH}" ]; then
    LIBRARY_PATH="../common-java-lib/bin/"
fi

add_profile() {
    new_profile=$1
    if [ -n "${ZWE_configs_spring_profiles_active}" ]; then
        ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active},"
    fi
    ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active}${new_profile}"
}

if [ "${ZWE_components_gateway_debug:-${ZWE_configs_debug:-false}}" = "true" ]; then
    # TODO should this be a merge of the profiles in gateway and discovery (and other modules later added?)
    if [ -n "${ZWE_configs_spring_profiles_active:-${ZWE_components_gateway_spring_profiles_active:-${ZWE_components_discovery_spring_profiles_active}}}" ]; then
        ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active:-${ZWE_components_gateway_spring_profiles_active:-${ZWE_components_discovery_spring_profiles_active}}}"
    fi
    add_profile "debug"
fi

if [  "${ZWE_configs_apiml_security_auth_uniqueCookie:-${ZWE_components_gateway_apiml_security_auth_uniqueCookie:-false}}" = "true" ]; then
    cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
fi

# how to verifyCertificates
verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
if [ "${verify_certificates_config}" = "DISABLED" ]; then
  verifySslCertificatesOfServices=false
elif [ "${verify_certificates_config}" = "NONSTRICT" ]; then
  verifySslCertificatesOfServices=true
  nonStrictVerifySslCertificatesOfServices=true
else
  # default value is STRICT
  verifySslCertificatesOfServices=true
  nonStrictVerifySslCertificatesOfServices=false
fi

ZOWE_CONSOLE_LOG_CHARSET=UTF-8
if [ "$(uname)" = "OS/390" ]; then
    QUICK_START="-Xquickstart"
    APIML_LOADER_PATH=${COMMON_LIB},/usr/include/java_classes/IRRRacf.jar

    JAVA_VERSION=$(${JAVA_HOME}/bin/javap -J-Xms4m -J-Xmx16m -verbose java.lang.String \
        | grep "major version" \
        | cut -d " " -f5)

    if [ $JAVA_VERSION -ge 65 ]; then # Java 21
        ZOWE_CONSOLE_LOG_CHARSET=IBM-1047
    fi
else
    APIML_LOADER_PATH=${COMMON_LIB}
fi

# Check if the directory containing the ZAAS shared JARs was set and append it to the ZAAS loader path
if [ -n "${ZWE_GATEWAY_SHARED_LIBS}" ]; then
    APIML_LOADER_PATH=${ZWE_GATEWAY_SHARED_LIBS},${APIML_LOADER_PATH}
fi
if [ -n "${ZWE_DISCOVERY_SHARED_LIBS}" ]; then
    APIML_LOADER_PATH=${ZWE_DISCOVERY_SHARED_LIBS},${APIML_LOADER_PATH}
fi

echo "Setting loader path: "${APIML_LOADER_PATH}

LOGBACK=""
if [ -n "${ZWE_configs_logging_config}" ]; then
    LOGBACK="-Dlogging.config=${ZWE_configs_logging_config}"
fi

ATTLS_SERVER_ENABLED="false"
ATTLS_CLIENT_ENABLED="false"

if [ "${ZWE_zowe_network_server_tls_attls}" = "true" ]; then
  ATTLS_SERVER_ENABLED="true"
fi
if [ "${ZWE_zowe_network_client_tls_attls}" = "true" ]; then
  ATTLS_CLIENT_ENABLED="true"
fi

if [ "${ATTLS_SERVER_ENABLED}" = "true" ]; then
  add_profile "attlsServer"
  ZWE_configs_server_ssl_enabled="false"
  ZWE_configs_apiml_service_corsEnabled=true
fi

internalProtocol="https"
ZWE_DISCOVERY_SERVICES_LIST=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"}
if [ "${ATTLS_CLIENT_ENABLED}" = "true" ]; then
    add_profile "attlsClient"
    ZWE_DISCOVERY_SERVICES_LIST=$(echo "${ZWE_DISCOVERY_SERVICES_LIST=}" | sed -e 's|https://|http://|g')
    internalProtocol=http
    ZWE_configs_apiml_service_corsEnabled=true
fi

if [ "${ZWE_configs_server_ssl_enabled:-${ZWE_components_gateway_server_ssl_enabled:-${ZWE_components_discovery_server_ssl_enabled:-true}}}" = "true" -o "$ATTLS_SERVER_ENABLED" = "true" ]; then
    externalProtocol="https"
else
    externalProtocol="http"
fi

if [ -n "${ZWE_configs_storage_vsam_name}" ]; then
    VSAM_FILE_NAME=//\'${ZWE_configs_storage_vsam_name:-${ZWE_components_caching_service_storage_vsam_name}}\'
fi

#Set the external URL only if the variables are defined so the APIML can fallback if the property is null
if [ -n "${externalProtocol}" ] && [ -n "${ZWE_zowe_externalDomains_0}" ] && [ -n "${ZWE_zowe_externalPort}" ]; then
    EXTERNAL_URL="-Dapiml.service.externalUrl=${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}"
fi

LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}/bin"
LIBPATH="$LIBPATH":"${JAVA_HOME}/bin/classic"
LIBPATH="$LIBPATH":"${JAVA_HOME}/bin/j9vm"
LIBPATH="$LIBPATH":"${JAVA_HOME}/lib/s390x/classic"
LIBPATH="$LIBPATH":"${JAVA_HOME}/lib/s390x/default"
LIBPATH="$LIBPATH":"${JAVA_HOME}/lib/s390x/j9vm"
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"

if [ -n "${ZWE_GATEWAY_LIBRARY_PATH}" ]; then
    LIBPATH="$LIBPATH":"${ZWE_GATEWAY_LIBRARY_PATH}"
fi

ADD_OPENS="--add-opens=java.base/java.lang=ALL-UNNAMED
        --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
        --add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED
        --add-opens=java.base/java.util=ALL-UNNAMED
        --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
        --add-opens=java.base/javax.net.ssl=ALL-UNNAMED
        --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
        --add-opens=java.base/java.io=ALL-UNNAMED"

get_enabled_protocol_limit() {
    target=$1
    type=$2
    default=$3
    key_component="ZWE_configs_zowe_network_${target}_tls_${type}Tls"
    value_component=$(eval echo \$$key_component)
    key_zowe="ZWE_zowe_network_${target}_tls_${type}Tls"
    value_zowe=$(eval echo \$$key_zowe)
    enabled_protocol_limit=${value_component:-${value_zowe:-${default}}}
}

extract_between() {
    echo "$1" | sed -e "s/.*$2,//" -e "s/$3.*//"
}

get_enabled_protocol() {
    target=$1
    get_enabled_protocol_limit "${target}" "min" "TLSv1.2"
    enabled_protocols_min=${enabled_protocol_limit}
    get_enabled_protocol_limit "${target}" "max" "TLSv1.3"
    enabled_protocols_max=${enabled_protocol_limit}

    if [ "${enabled_protocols_min:-}" = "${enabled_protocols_max:-}" ]; then
        result="${enabled_protocols_max:-}"
    elif [ -z "${enabled_protocols_min:-}" ]; then
        result="${enabled_protocols_max:-}"
    else
        enabled_protocols_max=${enabled_protocols_max:-"TLSv1.3"}
        enabled_protocols=,TLSv1,TLSv1.1,TLSv1.2,TLSv1.3,TLSv1.4,
        # Extract protocols between min and max (inclusive)
        result=$(extract_between "$enabled_protocols" "$enabled_protocols_min" "$enabled_protocols_max")
        result="$enabled_protocols_min,$result$enabled_protocols_max"
    fi
}

server_protocol="TLS"
get_enabled_protocol "server"
server_enabled_protocols=${result:-"TLSv1.3"}
server_ciphers=${ZWE_configs_zowe_network_server_tls_ciphers:-${ZWE_components_gateway_zowe_network_server_tls_ciphers:-${ZWE_zowe_network_server_tls_ciphers:-TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_DSS_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256,TLS_RSA_WITH_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV}}}
get_enabled_protocol "client"
client_enabled_protocols=${ZWE_components_gateway_apiml_httpclient_ssl_enabled_protocols:-${result:-${server_enabled_protocols}}}
client_ciphers=${ZWE_configs_zowe_network_client_tls_ciphers:-${ZWE_components_gateway_zowe_network_client_tls_ciphers:-${ZWE_zowe_network_client_tls_ciphers:-${server_ciphers}}}}

keystore_type="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}"
keystore_pass="${ZWE_configs_certificate_keystore_password:-${ZWE_zowe_certificate_keystore_password}}"
key_alias="${ZWE_configs_certificate_keystore_alias:-${ZWE_zowe_certificate_keystore_alias}}"
key_pass="${ZWE_configs_certificate_key_password:-${ZWE_zowe_certificate_key_password:-${keystore_pass}}}"
truststore_type="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}"
truststore_pass="${ZWE_configs_certificate_truststore_password:-${ZWE_zowe_certificate_truststore_password}}"

keystore_location="${ZWE_configs_certificate_keystore_file:-${ZWE_zowe_certificate_keystore_file}}"
truststore_location="${ZWE_configs_certificate_truststore_file:-${ZWE_zowe_certificate_truststore_file}}"

if [ "${keystore_type}" = "JCERACFKS" ]; then
    keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjce://_)
    truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjce://_)
elif [ "${keystore_type}" = "JCECCARACFKS" ]; then
    keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
    truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
elif [ "${keystore_type}" = "JCEHYBRIDRACFKS" ]; then
    keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
    truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
fi

if [ "${ATTLS_SERVER_ENABLED}" = "true" -a "${APIML_ATTLS_LOAD_KEYRING:-false}" = "true" ]; then
  keystore_type=
  keystore_pass=
  key_pass=
  key_alias=
  keystore_location=
fi

if [ -n "${ZWE_java_home}" ]; then
    JAVA_BIN_DIR=${ZWE_java_home}/bin/
fi

if [ "$ZWE_configs_telemetry_enabled" = "true" ]; then
    DISABLE_OTEL=false
else
    DISABLE_OTEL=true
fi

APIML_CODE=AG

SHARED_CLASSES_OPTS="-Xshareclasses:name=apiml_shared_classes,nonfatal"
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${APIML_CODE} ${JAVA_BIN_DIR}java \
    -Xms${ZWE_configs_heap_init:-${ZWE_components_gateway_heap_init:-32}}m -Xmx${ZWE_configs_heap_max:-${ZWE_components_gateway_heap_max:-512}}m \
    -XX:+ExitOnOutOfMemoryError \
    ${QUICK_START} \
    ${SHARED_CLASSES_OPTS} \
    ${ADD_OPENS} \
    ${LOGBACK} \
    ${JVM_SECURITY_PROPERTIES} \
    ${EXTERNAL_URL} \
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
    -Dapiml.gateway.servicesToDisableRetry=${ZWE_components_gateway_apiml_gateway_servicesToDisableRetry:-${ZWE_configs_apiml_gateway_servicesToDisableRetry:-}} \
    -Dapiml.gateway.servicesToLimitRequestRate=${ZWE_components_gateway_apiml_gateway_servicesToLimitRequestRate:-${ZWE_configs_apiml_gateway_servicesToLimitRequestRate:-}} \
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
    -Dapiml.security.rauditx.oidcSourceUserPaths=${ZWE_configs_apiml_security_rauditx_oidcSourceUserPaths:-${ZWE_components_gateway_apiml_security_rauditx_oidcSourceUserPaths:-sub}} \
    -Dapiml.security.rauditx.onOidcUserIsMapped=${ZWE_configs_apiml_security_rauditx_onOidcUserIsMapped:-${ZWE_components_gateway_apiml_security_rauditx_onOidcUserIsMapped:-false}} \
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
    -Dapiml.service.corsAllowedMethods=${ZWE_components_gateway_apiml_service_corsAllowedMethods:-${ZWE_configs_apiml_service_corsAllowedMethods:-GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS}} \
    -Dapiml.service.corsEnabled=${ZWE_components_gateway_apiml_service_corsEnabled:-${ZWE_configs_apiml_service_corsEnabled:-false}} \
    -Dapiml.service.externalUrl="${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}" \
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
    -Dotel.exporter.otlp.endpoint="${ZWE_configs_telemetry_exporter_endpoint:-http://localhost:4318}" \
    -Dotel.resource.attributes.deployment.environment.name="${ZWE_configs_telemetry_attributes_deployment_environment_name:-}" \
    -Dotel.resource.attributes.deployment.environment="${ZWE_configs_telemetry_attributes_deployment_environment:-prod}" \
    -Dotel.resource.attributes.service.name="${ZWE_configs_telemetry_service_name:-${ZWE_components_gateway_apimlId:-${ZWE_configs_apimlId}}}" \
    -Dotel.resource.attributes.service.namespace="${ZWE_configs_telemetry_service_namespace:-}" \
    -Dotel.resource.attributes.zos.smf.id="${ZWE_configs_telemetry_attributes_zos_smf_id:-}" \
    -Dotel.resource.attributes.zos.sysplex.name="${ZWE_configs_telemetry_attributes_zos_sysplex_name:-}" \
    -Dotel.resource.attributes.zos.lpar.override="${ZWE_configs_telemetry_attributes_zos_lpar_override:-false}" \
    -Dotel.resource.attributes.zos.lpar.name="${ZWE_configs_telemetry_attributes_zos_lpar_name:-${ZWE_haInstance_sysname:-${ZWE_haInstance_id:-}}}" \
    -Dotel.sdk.disabled=${DISABLE_OTEL} \
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
    -jar "${JAR_FILE}" &

pid=$!
echo "pid=${pid}"

wait %1
