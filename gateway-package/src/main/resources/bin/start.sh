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
# - ZWE_configs_apiml_catalog_serviceId
# - ZWE_configs_apiml_gateway_timeoutMillis
# - ZWE_configs_apiml_gateway_externalProtocol
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
# - ZWE_configs_apiml_service_allowEncodedSlashes - Allows encoded slashes on on URLs through gateway
# - ZWE_configs_apiml_service_corsEnabled
# - ZWE_configs_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_truststore_file
# - ZWE_configs_certificate_truststore_type
# - ZWE_configs_debug
# - ZWE_configs_port - the port the api gateway service will use
# - ZWE_configs_apimlId
# - ZWE_configs_server_internal_ssl_certificate_keystore_alias
# - ZWE_configs_server_internal_ssl_certificate_keystore_file
# - ZWE_configs_server_internal_enabled
# - ZWE_configs_server_internal_port
# - ZWE_configs_server_internal_ssl_enabled
# - ZWE_configs_server_maxConnectionsPerRoute
# - ZWE_configs_server_maxTotalConnections
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_spring_profiles_active
# - ZWE_DISCOVERY_SERVICES_LIST
# - ZWE_GATEWAY_SHARED_LIBS
# - ZWE_haInstance_hostname
# - ZWE_zowe_network_server_tls_attls
# - ZWE_zowe_certificate_keystore_type - The default keystore type to use for SSL certificates
# - ZWE_zowe_verifyCertificates - if we accept only verified certificates

if [ -n "${LAUNCH_COMPONENT}" ]
then
    JAR_FILE="${LAUNCH_COMPONENT}/gateway-service-lite.jar"
else
    JAR_FILE="$(pwd)/bin/gateway-service-lite.jar"
fi
echo "jar file: "${JAR_FILE}
# script assumes it's in the gateway component directory and common_lib needs to be relative path

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

if [ -z "${ZWE_configs_apiml_catalog_serviceId}" ]
then
    APIML_GATEWAY_CATALOG_ID="apicatalog"
fi

if [ "${ZWE_configs_apiml_catalog_serviceId}" = "none" ]
then
    APIML_GATEWAY_CATALOG_ID=""
fi

if [ "$(uname)" = "OS/390" ]
then
    QUICK_START=-Xquickstart
    GATEWAY_LOADER_PATH=${COMMON_LIB},/usr/include/java_classes/IRRRacf.jar
else
    GATEWAY_LOADER_PATH=${COMMON_LIB}
fi

ATTLS_ENABLED="false"
# ZWE_configs_spring_profiles_active for back compatibility, should be removed in v3 - enabling via Spring profile
if [ "${ZWE_zowe_network_server_tls_attls}" = "true" -o "$(echo ${ZWE_configs_spring_profiles_active:-} | awk '/^(.*,)?attls(,.*)?$/')" ]; then
  ATTLS_ENABLED="true"
fi
if [ "${ATTLS_ENABLED}" = "true" ]; then
  ZWE_configs_server_ssl_enabled="false"
  if [ -n "${ZWE_configs_spring_profiles_active}" ]; then
    ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active},"
  fi
  ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active}attls"
fi

# Verify discovery service URL in case AT-TLS is enabled, assumes outgoing rules are in place
ZWE_DISCOVERY_SERVICES_LIST=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"}
if [ "${ATTLS_ENABLED}" = "true" ]; then
    ZWE_DISCOVERY_SERVICES_LIST=$(echo "${ZWE_DISCOVERY_SERVICES_LIST=}" | sed -e 's|https://|http://|g')
    ZWE_configs_server_internal_ssl_enabled="${ZWE_configs_server_internal_ssl_enabled:-false}"
    ZWE_configs_apiml_service_corsEnabled=true
fi

if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" -o "$ATTLS_ENABLED" = "true" ]; then
    externalProtocol="https"
else
    externalProtocol="http"
fi


# Check if the directory containing the Gateway shared JARs was set and append it to the GW loader path
if [ -n "${ZWE_GATEWAY_SHARED_LIBS}" ]
then
    GATEWAY_LOADER_PATH=${ZWE_GATEWAY_SHARED_LIBS},${GATEWAY_LOADER_PATH}
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
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"

if [ -n "${ZWE_GATEWAY_LIBRARY_PATH}" ]
then
    LIBPATH="$LIBPATH":"${ZWE_GATEWAY_LIBRARY_PATH}"
fi

keystore_type="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}"
keystore_pass="${ZWE_configs_certificate_keystore_password:-${ZWE_zowe_certificate_keystore_password}}"
key_alias="${ZWE_configs_certificate_keystore_alias:-${ZWE_zowe_certificate_keystore_alias}}"
key_pass="${ZWE_configs_certificate_key_password:-${ZWE_zowe_certificate_key_password:-${keystore_pass}}}"
truststore_type="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}"
truststore_pass="${ZWE_configs_certificate_truststore_password:-${ZWE_zowe_certificate_truststore_password}}"

keystore_location="${ZWE_configs_certificate_keystore_file:-${ZWE_zowe_certificate_keystore_file}}"
truststore_location="${ZWE_configs_certificate_truststore_file:-${ZWE_zowe_certificate_truststore_file}}"

# NOTE: these are moved from below
#    -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS:-false} \
#    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS:-127.0.0.1} \
#    -Dapiml.security.auth.jwtKeyAlias=${PKCS11_TOKEN_LABEL:-jwtsecret} \

# Check for Java version and set --add-opens Java option in case the version is 17 or later
JAVA_VERSION=$(${JAVA_HOME}/bin/javap -verbose java.lang.String \
    | grep "major version" \
    | cut -d " " -f5)
ADD_OPENS=""
if [ $JAVA_VERSION -ge 61 ]; then
    ADD_OPENS="--add-opens=java.base/java.lang=ALL-UNNAMED
                --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
                --add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED
                --add-opens=java.base/java.util=ALL-UNNAMED
                --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
                --add-opens=java.base/javax.net.ssl=ALL-UNNAMED
                --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
                --add-opens=java.base/java.io=ALL-UNNAMED"

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
fi

if [ "${ATTLS_ENABLED}" = "true" ]; then
  keystore_type=
  keystore_pass=
  key_pass=
  key_alias=
  keystore_location=
fi

GATEWAY_CODE=AG
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${GATEWAY_CODE} java \
    -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
    -XX:+ExitOnOutOfMemoryError \
    ${QUICK_START} \
    ${ADD_OPENS} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-7554} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.service.allowEncodedSlashes=${ZWE_configs_apiml_service_allowEncodedSlashes:-true} \
    -Dapiml.service.corsEnabled=${ZWE_configs_apiml_service_corsEnabled:-false} \
    -Dapiml.service.externalUrl="${ZWE_configs_apiml_gateway_externalProtocol:-${externalProtocol}}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}" \
    -Dapiml.service.apimlId=${ZWE_configs_apimlId:-} \
    -Dapiml.catalog.serviceId=${APIML_GATEWAY_CATALOG_ID:-apicatalog} \
    -Dapiml.cache.storage.location=${ZWE_zowe_workspaceDirectory}/api-mediation/${ZWE_haInstance_id:-localhost} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.gateway.timeoutMillis=${ZWE_configs_apiml_gateway_timeoutMillis:-600000} \
    -Dapiml.gateway.health.protected=${ZWE_configs_apiml_gateway_health_protected:-false} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
    -Dapiml.security.auth.zosmf.serviceId=${ZWE_configs_apiml_security_auth_zosmf_serviceId:-zosmf} \
    -Dapiml.security.auth.provider=${ZWE_configs_apiml_security_auth_provider:-zosmf} \
    -Dapiml.security.auth.jwt.customAuthHeader=${ZWE_configs_apiml_security_auth_jwt_customAuthHeader:-} \
    -Dapiml.security.auth.cookieProperties.cookieName=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.security.auth.passticket.customUserHeader=${ZWE_configs_apiml_security_auth_passticket_customUserHeader:-} \
    -Dapiml.security.auth.passticket.customAuthHeader=${ZWE_configs_apiml_security_auth_passticket_customAuthHeader:-} \
    -Dapiml.security.personalAccessToken.enabled=${ZWE_configs_apiml_security_personalAccessToken_enabled:-false} \
    -Dapiml.httpclient.ssl.enabled-protocols=${ZWE_components_gateway_apiml_httpclient_ssl_enabled_protocols:-ZWE_configs_zowe_network_client_tls_maxTls:-ZWE_zowe_network_client_tls_maxTls:-ZWE_configs_zowe_network_server_tls_maxTls:-ZWE_zowe_network_server_tls_maxTls:-"TLSv1.2,TLSv1.3"} \
    -Dapiml.zoweManifest=${ZWE_zowe_runtimeDirectory}/manifest.json \
    -Dserver.address=${ZWE_configs_zowe_network_server_listenAddresses_0:-ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"} \
    -Dserver.maxConnectionsPerRoute=${ZWE_configs_server_maxConnectionsPerRoute:-100} \
    -Dserver.maxTotalConnections=${ZWE_configs_server_maxTotalConnections:-1000} \
    -Dserver.webSocket.maxIdleTimeout=${ZWE_configs_server_webSocket_maxIdleTimeout:-3600000} \
    -Dserver.webSocket.connectTimeout=${ZWE_configs_server_webSocket_connectTimeout:-45000} \
    -Dserver.webSocket.stopTimeout=${ZWE_configs_server_webSocket_stopTimeout:-30000} \
    -Dserver.webSocket.asyncWriteTimeout=${ZWE_configs_server_webSocket_asyncWriteTimeout:-60000} \
    -Dserver.webSocket.requestBufferSize=${ZWE_configs_server_webSocket_requestBufferSize:-8192} \
    -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true} \
    -Dserver.ssl.protocol=${ZWE_configs_server_ssl_protocol:-ZWE_configs_zowe_network_server_tls_maxTls:-ZWE_zowe_network_server_tls_maxTls:-"TLSv1.2,TLSv1.3"}  \
    -Dserver.ssl.ciphers=${ZWE_configs_zowe_network_server_tls_ciphers:-ZWE_zowe_network_server_tls_ciphers:-} \
    -Djdk.tls.client.cipherSuites=${ZWE_configs_zowe_network_client_tls_ciphers:-ZWE_zowe_network_client_tls_ciphers:-ZWE_configs_zowe_network_server_tls_ciphers:-ZWE_zowe_network_server_tls_ciphers:-} \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStoreType="${keystore_type}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyAlias="${key_alias}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStoreType="${truststore_type}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Dserver.internal.enabled=${ZWE_configs_server_internal_enabled:-false} \
    -Dserver.internal.ssl.enabled=${ZWE_configs_server_internal_ssl_enabled:-true} \
    -Dserver.internal.port=${ZWE_configs_server_internal_port:-10017} \
    -Dserver.internal.ssl.keyAlias=${ZWE_configs_server_internal_ssl_certificate_keystore_alias:-localhost-multi} \
    -Dserver.internal.ssl.keyStore=${ZWE_configs_server_internal_ssl_certificate_keystore_file:-keystore/localhost/localhost-multi.keystore.p12} \
    -Dapiml.httpclient.conn-pool.requestConnectionTimeout=${ZWE_configs_apiml_gateway_httpclient_requestConnectionTimeout:-10000} \
    -Dapiml.security.auth.zosmf.jwtAutoconfiguration=${ZWE_configs_apiml_security_auth_zosmf_jwtAutoconfiguration:-auto} \
    -Dapiml.security.jwtInitializerTimeout=${ZWE_configs_apiml_security_jwtInitializerTimeout:-5} \
    -Dapiml.security.useInternalMapper=${ZWE_configs_apiml_security_useInternalMapper:-false} \
    -Dapiml.security.x509.enabled=${ZWE_configs_apiml_security_x509_enabled:-false} \
    -Dapiml.security.x509.externalMapperUrl=${ZWE_configs_apiml_security_x509_externalMapperUrl:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_configs_port:-7554}/zss/api/v1/certificate/x509/map"} \
    -Dapiml.security.x509.externalMapperUser=${ZWE_configs_apiml_security_x509_externalMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}} \
    -Dapiml.security.x509.acceptForwardedCert=${ZWE_configs_apiml_security_x509_acceptForwardedCert:-false} \
    -Dapiml.security.x509.certificatesUrl=${ZWE_configs_apiml_security_x509_certificatesUrl:-} \
    -Dapiml.security.authorization.provider=${ZWE_configs_apiml_security_authorization_provider:-} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_configs_apiml_security_authorization_endpoint_enabled:-false} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_configs_apiml_security_authorization_endpoint_url:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_configs_port:-7554}/zss/api/v1/saf-auth"} \
    -Dapiml.security.saf.provider=${ZWE_configs_apiml_security_saf_provider:-"rest"} \
    -Dapiml.security.saf.urls.authenticate=${ZWE_configs_apiml_security_saf_urls_authenticate:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_configs_port:-7554}/zss/api/v1/saf/authenticate"} \
    -Dapiml.security.saf.urls.verify=${ZWE_configs_apiml_security_saf_urls_verify:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_configs_port:-7554}/zss/api/v1/saf/verify"} \
    -Dapiml.security.authorization.resourceClass=${ZWE_configs_apiml_security_authorization_resourceClass:-ZOWE} \
    -Dapiml.security.authorization.resourceNamePrefix=${ZWE_configs_apiml_security_authorization_resourceNamePrefix:-APIML.} \
    -Dapiml.security.zosmf.applid=${ZWE_configs_apiml_security_zosmf_applid:-IZUDFLT} \
    -Dapiml.security.oidc.enabled=${ZWE_configs_apiml_security_oidc_enabled:-false} \
    -Dapiml.security.oidc.clientId=${ZWE_configs_apiml_security_oidc_clientId:-} \
    -Dapiml.security.oidc.clientSecret=${ZWE_configs_apiml_security_oidc_clientSecret:-} \
    -Dapiml.security.oidc.registry=${ZWE_configs_apiml_security_oidc_registry:-} \
    -Dapiml.security.oidc.identityMapperUrl=${ZWE_configs_apiml_security_oidc_identityMapperUrl:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_configs_port:-7554}/zss/api/v1/certificate/dn"} \
    -Dapiml.security.oidc.identityMapperUser=${ZWE_configs_apiml_security_oidc_identityMapperUser:-${ZWE_zowe_setup_security_users_zowe:-ZWESVUSR}} \
    -Dapiml.security.oidc.jwks.uri=${ZWE_configs_apiml_security_oidc_jwks_uri} \
    -Dapiml.security.oidc.jwks.refreshInternalHours=${ZWE_configs_apiml_security_oidc_jwks_refreshInternalHours:-1} \
    -Dapiml.security.allowTokenRefresh=${ZWE_configs_apiml_security_allowtokenrefresh:-false} \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${GATEWAY_LOADER_PATH} \
    -Djava.library.path=${LIBPATH} \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -jar ${JAR_FILE} &

pid=$!
echo "pid=${pid}"

wait %1
