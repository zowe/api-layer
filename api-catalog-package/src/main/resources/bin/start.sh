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
# - ZWE_components_gateway_server_ssl_enabled
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
# - ZWE_haInstance_hostname
# - ZWE_zowe_certificate_keystore_type - The default keystore type to use for SSL certificates
# - ZWE_zowe_verifyCertificates - if we accept only verified certificates
if [ -n "${LAUNCH_COMPONENT}" ]
then
    JAR_FILE="${LAUNCH_COMPONENT}/api-catalog-services-lite.jar"
else
    JAR_FILE="$(pwd)/bin/api-catalog-services-lite.jar"
fi
echo "jar file: "${JAR_FILE}
# script assumes it's in the catalog component directory and common_lib needs to be relative path

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

# FIXME: APIML_DIAG_MODE_ENABLED is not officially mentioned. We can still use it behind the scene,
# or we can define configs.diagMode in manifest, then use "$ZWE_configs_diagMode".
# if [[ ! -z "${APIML_DIAG_MODE_ENABLED}" ]]
# then
#     LOG_LEVEL=${APIML_DIAG_MODE_ENABLED}
# fi

# NOTE: ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES is not defined in Zowe level any more, never heard anyone use it.
#        will just use $ZWE_STATIC_DEFINITIONS_DIR directly.
# If set append $ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES to $ZWE_STATIC_DEFINITIONS_DIR
# export APIML_STATIC_DEF=${ZWE_STATIC_DEFINITIONS_DIR}
# if [[ ! -z "$ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES" ]]
# then
#   export APIML_STATIC_DEF="${APIML_STATIC_DEF};${ZWEAD_EXTERNAL_STATIC_DEF_DIRECTORIES}"
# fi

# setting the cookieName based on the instances

if [  "${ZWE_components_gateway_apiml_security_auth_uniqueCookie}" = "true" ]; then
    cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
fi

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

ADD_OPENS="--add-opens=java.base/java.lang=ALL-UNNAMED
        --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
        --add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED
        --add-opens=java.base/java.util=ALL-UNNAMED
        --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
        --add-opens=java.base/javax.net.ssl=ALL-UNNAMED"

ATTLS_ENABLED="false"
if [ -n "$(echo ${ZWE_configs_spring_profiles_active:-} | awk '/^(.*,)?attls(,.*)?$/')" ]; then
    ATTLS_ENABLED="true"
fi

if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" -o "$ATTLS_ENABLED" = "true" ]; then
    httpProtocol="https"
else
    httpProtocol="http"
fi

# Verify discovery service URL in case AT-TLS is enabled, assumes outgoing rules are in place
ZWE_DISCOVERY_SERVICES_LIST=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"}
if [ "$ATTLS_ENABLED" = "true" ]; then
    ZWE_DISCOVERY_SERVICES_LIST=$(echo "${ZWE_DISCOVERY_SERVICES_LIST=}" | sed -e 's|https://|http://|g')
fi

keystore_type="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}"
keystore_pass="${ZWE_configs_certificate_keystore_password:-${ZWE_zowe_certificate_keystore_password}}"
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

# NOTE: these are moved from below
#    -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS:-127.0.0.1} \
#    -Dapiml.service.preferIpAddress=false \

CATALOG_CODE=AC
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${CATALOG_CODE} java \
    -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
    ${QUICK_START} \
    ${ADD_OPENS} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-7552} \
    -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dapiml.service.gatewayHostname=${ZWE_GATEWAY_HOST:-${ZWE_haInstance_hostname:-localhost}} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.service.externalUrl="${httpProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}" \
    -Dapiml.discovery.staticApiDefinitionsDirectories=${ZWE_STATIC_DEFINITIONS_DIR} \
    -Dapiml.security.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices:-false} \
    -Dapiml.security.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
    -Dapiml.security.authorization.provider=${ZWE_components_gateway_apiml_security_authorization_provider:-} \
    -Dapiml.security.authorization.endpoint.enabled=${ZWE_components_gateway_apiml_security_authorization_endpoint_enabled:-false} \
    -Dapiml.security.authorization.endpoint.url=${ZWE_components_gateway_apiml_security_authorization_endpoint_url:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port}/zss/api/v1/saf-auth"} \
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
    -Dapiml.httpclient.ssl.enabled-protocols=${ZWE_components_gateway_apiml_httpclient_ssl_enabled_protocols:-"TLSv1.2"} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dserver.address=0.0.0.0 \
    -Dserver.ssl.enabled=${ZWE_components_gateway_server_ssl_enabled:-true}  \
    -Dserver.ssl.protocol=${ZWE_components_gateway_server_ssl_protocol:-"TLSv1.2"}  \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStoreType="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyAlias="${ZWE_configs_certificate_keystore_alias:-${ZWE_zowe_certificate_keystore_alias}}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStoreType="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Dloader.path=${COMMON_LIB} \
    -Djava.library.path=${LIBPATH} \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -jar "${JAR_FILE}" &
pid=$!
echo "pid=${pid}"

wait %1
