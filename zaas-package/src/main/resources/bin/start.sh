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
# - ZWE_configs_apiml_security_zosmf_applid
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

if [ -n "${LAUNCH_COMPONENT}" ]
then
    JAR_FILE="${LAUNCH_COMPONENT}/zaas-service-lite.jar"
else
    JAR_FILE="$(pwd)/bin/zaas-service-lite.jar"
fi
echo "jar file: "${JAR_FILE}
# script assumes it's in the ZAAS component directory and common_lib needs to be relative path

if [ -z "${CMMN_LB}" ]
then
    COMMON_LIB="../apiml-common-lib/bin/api-layer-lite-lib-all.jar"
else
    COMMON_LIB=${CMMN_LB}
fi

if [ -z "${LIBRARY_PATH}" ]
then
    LIBRARY_PATH="../common-java-lib/bin/"
fi

# API Mediation Layer Debug Mode
export LOG_LEVEL=

if [ "${ZWE_configs_debug}" = "true" ]
then
  export LOG_LEVEL="debug"
fi

# setting the cookieName based on the instances

if [  "${ZWE_configs_apiml_security_auth_uniqueCookie}" = "true" ]; then
    cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
fi

# FIXME: APIML_DIAG_MODE_ENABLED is not officially mentioned. We can still use it behind the scene,
# or we can define configs.diagMode in manifest, then use "$ZWE_configs_diagMode".
# DIAG_MODE=${APIML_DIAG_MODE_ENABLED}
# if [ ! -z "$DIAG_MODE" ]
# then
#     LOG_LEVEL=$DIAG_MODE
# fi

# how to verifyCertificates
verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
if [ "${verify_certificates_config}" = "DISABLED" ]; then
  verifySslCertificatesOfServices=false
  nonStrictVerifySslCertificatesOfServices=true
elif [ "${verify_certificates_config}" = "NONSTRICT" ]; then
  verifySslCertificatesOfServices=true
  nonStrictVerifySslCertificatesOfServices=true
else
  # default value is STRICT
  verifySslCertificatesOfServices=true
  nonStrictVerifySslCertificatesOfServices=false
fi

if [ "$(uname)" = "OS/390" ]
then
    QUICK_START=-Xquickstart
    ZAAS_LOADER_PATH=${COMMON_LIB},/usr/include/java_classes/IRRRacf.jar
else
    ZAAS_LOADER_PATH=${COMMON_LIB}
fi

# Check if the directory containing the ZAAS shared JARs was set and append it to the ZAAS loader path
if [ -n "${ZWE_ZAAS_SHARED_LIBS}" ]
then
    ZAAS_LOADER_PATH=${ZWE_ZAAS_SHARED_LIBS},${ZAAS_LOADER_PATH}
fi

echo "Setting loader path: "${ZAAS_LOADER_PATH}

ATTLS_ENABLED="false"
ATTLS_CLIENT_ENABLED="false"

if [ "${ZWE_zowe_network_server_tls_attls}" = "true" ]; then
  ATTLS_ENABLED="true"
fi
if [ "${ZWE_zowe_network_client_tls_attls}" = "true" ]; then
  ATTLS_CLIENT_ENABLED="true"
fi

if [ "${ATTLS_ENABLED}" = "true" ]; then
  ZWE_configs_server_ssl_enabled="false"
  if [ -n "${ZWE_configs_spring_profiles_active}" ]; then
    ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active},"
  fi
  ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active}attls"
fi

internalProtocol="https"
ZWE_DISCOVERY_SERVICES_LIST=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"}
if [ "${ATTLS_CLIENT_ENABLED}" = "true" ]; then
    ZWE_DISCOVERY_SERVICES_LIST=$(echo "${ZWE_DISCOVERY_SERVICES_LIST=}" | sed -e 's|https://|http://|g')
    internalProtocol=http
fi

if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" -o "$ATTLS_ENABLED" = "true" ]; then
    externalProtocol="https"
else
    externalProtocol="http"
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

if [ -n "${ZWE_ZAAS_LIBRARY_PATH}" ]
then
    LIBPATH="$LIBPATH":"${ZWE_ZAAS_LIBRARY_PATH}"
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
    key_component="ZWE_configs_zowe_network_${target}_tls_${type}Tls"
    value_component=$(eval echo \$$key_component)
    key_gateway="ZWE_components_gateway_zowe_network_${target}_tls_${type}Tls"
    value_gateway=$(eval echo \$$key_gateway)
    key_zowe="ZWE_zowe_network_${target}_tls_${type}Tls"
    value_zowe=$(eval echo \$$key_zowe)
    enabled_protocol_limit=${value_component:-${value_gateway:-${value_zowe:-}}}
}

extract_between() {
    echo "$1" | sed -e "s/.*$2,//" -e "s/$3.*//"
}

get_enabled_protocol() {
    target=$1
    get_enabled_protocol_limit "${target}" "min"
    enabled_protocols_min=${enabled_protocol_limit}
    get_enabled_protocol_limit "${target}" "max"
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

get_enabled_protocol_limit "server" "max"
server_protocol=${enabled_protocol_limit:-"TLS"}
get_enabled_protocol "server"
server_enabled_protocols=${result:-"TLSv1.3"}
server_ciphers=${ZWE_configs_zowe_network_server_tls_ciphers:-${ZWE_components_gateway_zowe_network_server_tls_ciphers:-${ZWE_zowe_network_server_tls_ciphers:-TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384}}}
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

if [ "${ATTLS_ENABLED}" = "true" -a "${APIML_ATTLS_LOAD_KEYRING:-false}" = "true" ]; then
  keystore_type=
  keystore_pass=
  key_pass=
  key_alias=
  keystore_location=
fi

# NOTE: these are moved from below
#    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS:-false} \
#    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS:-127.0.0.1} \
#    -Dapiml.security.auth.jwtKeyAlias=${PKCS11_TOKEN_LABEL:-jwtsecret} \

if [ -n "${ZWE_java_home}" ]; then
    JAVA_BIN_DIR=${ZWE_java_home}/bin/
fi

ZAAS_CODE=AZ
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${ZAAS_CODE} ${JAVA_BIN_DIR}java \
    -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
    ${QUICK_START} \
    ${ADD_OPENS} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dspring.profiles.include=$LOG_LEVEL \
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
    -Dapiml.httpclient.ssl.enabled-protocols=${client_enabled_protocols} \
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
    -Dapiml.security.x509.acceptForwardedCert=${ZWE_configs_apiml_security_x509_enabled:-${ZWE_components_gateway_apiml_security_x509_enabled:-${ZWE_components_gateway_apiml_security_x509_enabled:-true}}} \
    -Dapiml.security.x509.certificatesUrl=${ZWE_configs_apiml_security_x509_certificatesUrl:-${ZWE_components_gateway_apiml_security_x509_certificatesUrl:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/gateway/certificates"}} \
    -Dapiml.security.authorization.provider=${ZWE_configs_apiml_security_authorization_provider:-${ZWE_components_gateway_apiml_security_authorization_provider:-}} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_configs_apiml_security_authorization_endpoint_enabled:-${ZWE_components_gateway_apiml_security_authorization_endpoint_enabled:-false}} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_configs_apiml_security_authorization_endpoint_url:-${ZWE_components_gateway_apiml_security_authorization_endpoint_url:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf-auth"}} \
    -Dapiml.security.saf.provider=${ZWE_configs_apiml_security_saf_provider:-${ZWE_components_gateway_apiml_security_saf_provider:-"rest"}} \
    -Dapiml.security.saf.urls.authenticate=${ZWE_configs_apiml_security_saf_urls_authenticate:-${ZWE_components_gateway_apiml_security_saf_urls_authenticate:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf/authenticate"}} \
    -Dapiml.security.saf.urls.verify=${ZWE_configs_apiml_security_saf_urls_verify:-${ZWE_components_gateway_apiml_security_saf_urls_verify:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/saf/verify"}} \
    -Dapiml.security.authorization.resourceClass=${ZWE_configs_apiml_security_authorization_resourceClass:-${ZWE_components_gateway_apiml_security_authorization_resourceClass:-ZOWE}} \
    -Dapiml.security.authorization.resourceNamePrefix=${ZWE_configs_apiml_security_authorization_resourceNamePrefix:-${ZWE_components_gateway_apiml_security_authorization_resourceNamePrefix:-APIML.}} \
    -Dapiml.security.zosmf.applid=${ZWE_configs_apiml_security_zosmf_applid:-${ZWE_components_gateway_apiml_security_zosmf_applid:-IZUDFLT}} \
    -Dapiml.security.oidc.enabled=${ZWE_configs_apiml_security_oidc_enabled:-${ZWE_components_gateway_apiml_security_oidc_enabled:-false}} \
    -Dapiml.security.oidc.clientId=${ZWE_configs_apiml_security_oidc_clientId:-${ZWE_components_gateway_apiml_security_oidc_clientId:-}} \
    -Dapiml.security.oidc.clientSecret=${ZWE_configs_apiml_security_oidc_clientSecret:-${ZWE_components_gateway_apiml_security_oidc_clientSecret:-}} \
    -Dapiml.security.oidc.registry=${ZWE_configs_apiml_security_oidc_registry:-${ZWE_components_gateway_apiml_security_oidc_registry:-}} \
    -Dapiml.security.oidc.identityMapperUrl=${ZWE_configs_apiml_security_oidc_identityMapperUrl:-${ZWE_components_gateway_apiml_security_oidc_identityMapperUrl:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/zss/api/v1/certificate/dn"}} \
    -Dapiml.security.oidc.identityMapperUser=${ZWE_configs_apiml_security_oidc_identityMapperUser:-${ZWE_components_gateway_apiml_security_oidc_identityMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}}} \
    -Dapiml.security.oidc.jwks.uri=${ZWE_configs_apiml_security_oidc_jwks_uri:-${ZWE_components_gateway_apiml_security_oidc_jwks_uri:-}} \
    -Dapiml.security.oidc.jwks.refreshInternalHours=${ZWE_configs_apiml_security_oidc_jwks_refreshInternalHours:-${ZWE_components_gateway_apiml_security_oidc_jwks_refreshInternalHours:-1}} \
    -Dapiml.security.oidc.userInfo.uri=${ZWE_configs_apiml_security_oidc_userInfo_uri:-${ZWE_components_gateway_apiml_security_oidc_userInfo_uri:-}} \
    -Dapiml.security.oidc.validationType=${ZWE_configs_apiml_security_oidc_validationType:-${ZWE_components_gateway_apiml_security_oidc_validationType:-"JWK"}} \
    -Dapiml.security.allowTokenRefresh=${ZWE_configs_apiml_security_allowtokenrefresh:-${ZWE_components_gateway_apiml_security_allowtokenrefresh:-false}} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${ZAAS_LOADER_PATH} \
    -Djava.library.path=${LIBPATH} \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -jar ${JAR_FILE} &

pid=$!
echo "pid=${pid}"

wait %1
