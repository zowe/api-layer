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

# Optional variables:
# - CMMN_LB
# - LIBPATH
# - LIBRARY_PATH
# - ZWE_components_discovery_port - the port the discovery service will use
# - ZWE_components_gateway_apiml_security_authorization_endpoint_enabled
# - ZWE_components_gateway_apiml_security_authorization_endpoint_url
# - ZWE_components_gateway_apiml_security_authorization_provider
# - ZWE_components_gateway_apiml_security_authorization_resourceClass
# - ZWE_components_gateway_port - the port the api gateway service will use
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_heap_max
# - ZWE_configs_heap_init
# - ZWE_configs_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_truststore_file
# - ZWE_configs_certificate_truststore_type
# - ZWE_configs_debug
# - ZWE_configs_port - the port the api catalog service will use
# - ZWE_configs_spring_profiles_active
# - ZWE_DISCOVERY_SERVICES_LIST
# - ZWE_GATEWAY_HOST
# - ZWE_zowe_network_server_tls_attls
# - ZWE_haInstance_hostname
# - ZWE_zowe_certificate_keystore_type - The default keystore type to use for SSL certificates
# - ZWE_zowe_verifyCertificates - if we accept only verified certificates

# Source common APIML scripts (sets up common variables and functions)
if [ -n "${LAUNCH_COMPONENT}" ]; then
    echo "lnch"
    echo "${LAUNCH_COMPONENT}/apiml-common-scripts.sh"
    . "${LAUNCH_COMPONENT}/apiml-common-scripts.sh"
else
    echo "pwd"
    echo "$(pwd)/bin/apiml-common-scripts.sh"
    . "$(pwd)/bin/apiml-common-scripts.sh"
fi

# JAR file location
if [ -n "${LAUNCH_COMPONENT}" ]; then
    JAR_FILE="${LAUNCH_COMPONENT}/api-catalog-services-lite.jar"
else
    JAR_FILE="$(pwd)/bin/api-catalog-services-lite.jar"
fi
echo "jar file: ${JAR_FILE}"

# Debug profile
if [ "${ZWE_configs_debug}" = "true" ]; then
    add_profile "debug"
fi

# Cookie name for unique cookie support
if [ "${ZWE_components_gateway_apiml_security_auth_uniqueCookie}" = "true" ]; then
    cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
fi

# AT-TLS server profile
if [ "${ATTLS_SERVER_ENABLED}" = "true" ]; then
    add_profile "attlsServer"
    ZWE_configs_server_ssl_enabled="false"
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

# External URL
if [ -n "${externalProtocol}" ] && [ -n "${ZWE_zowe_externalDomains_0}" ] && [ -n "${ZWE_zowe_externalPort}" ]; then
    EXTERNAL_URL="-Dapiml.service.externalUrl=${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}"
fi

CATALOG_CODE=AC
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${CATALOG_CODE} ${JAVA_BIN_DIR}java \
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
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Dlogging.charset.console=${ZOWE_CONSOLE_LOG_CHARSET} \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-7552} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.service.gatewayHostname=${ZWE_GATEWAY_HOST:-${ZWE_haInstance_hostname:-localhost}} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.health.protected=${ZWE_configs_apiml_health_protected:-true} \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${ZWE_STATIC_DEFINITIONS_DIR} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
    -Dapiml.security.authorization.provider=${ZWE_configs_apiml_security_authorization_provider:-${ZWE_components_gateway_apiml_security_authorization_provider:-"native"}} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_components_gateway_apiml_security_authorization_endpoint_enabled:-false} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_components_gateway_apiml_security_authorization_endpoint_url:-"${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port}/zss/api/v1/saf-auth"} \
    -Dapiml.security.authorization.resourceClass=${ZWE_components_gateway_apiml_security_authorization_resourceClass:-ZOWE} \
    -Dapiml.security.auth.cookieProperties.cookieName=${cookieName:-apimlAuthenticationToken} \
    -Dapiml.catalog.hide.serviceInfo=${ZWE_configs_apiml_catalog_hide_serviceInfo:-false} \
    -Dapiml.catalog.customStyle.logo=${ZWE_configs_apiml_catalog_customStyle_logo:-} \
    -Dapiml.catalog.customStyle.fontFamily=${ZWE_configs_apiml_catalog_customStyle_fontFamily:-} \
    -Dapiml.catalog.customStyle.backgroundColor=${ZWE_configs_apiml_catalog_customStyle_backgroundColor:-} \
    -Dapiml.catalog.customStyle.titlesColor=${ZWE_configs_apiml_catalog_customStyle_titlesColor:-} \
    -Dapiml.catalog.customStyle.headerColor=${ZWE_configs_apiml_catalog_customStyle_headerColor:-} \
    -Dapiml.catalog.customStyle.textColor=${ZWE_configs_apiml_catalog_customStyle_textColor:-} \
    -Dapiml.catalog.customStyle.docLink=${ZWE_configs_apiml_catalog_customStyle_docLink:-} \
    -Dapiml.httpclient.ssl.enabled-protocols=${client_enabled_protocols} \
    -Djdk.tls.client.cipherSuites=${client_ciphers} \
    -Dserver.ssl.ciphers=${server_ciphers} \
    -Dserver.ssl.protocol=${server_protocol} \
    -Dserver.ssl.enabled-protocols=${server_enabled_protocols} \
    -Dserver.address=${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"}} \
    -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true}  \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStoreType="${keystore_type}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyAlias="${key_alias}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStoreType="${truststore_type}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${COMMON_LIB} \
    -Djava.library.path=${LIBPATH} \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -Dotel.sdk.disabled=true \
    -jar "${JAR_FILE}" &
pid=$!
echo "pid=${pid}"

wait %1
