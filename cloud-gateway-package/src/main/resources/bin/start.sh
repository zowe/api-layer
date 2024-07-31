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
# - ZWE_configs_apiml_service_forwardClientCertEnabled
# - ZWE_configs_cloudGateway_registry_enabled
# - ZWE_configs_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_truststore_file
# - ZWE_configs_certificate_truststore_type
# - ZWE_configs_debug
# - ZWE_configs_port - the port the api gateway service will use
# - ZWE_configs_server_maxConnectionsPerRoute
# - ZWE_configs_server_maxTotalConnections
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_spring_profiles_active
# - ZWE_zowe_network_server_tls_attls
# - ZWE_DISCOVERY_SERVICES_LIST

if [ -n "${LAUNCH_COMPONENT}" ]
then
    JAR_FILE="${LAUNCH_COMPONENT}/cloud-gateway-service.jar"
else
    JAR_FILE="$(pwd)/bin/cloud-gateway-service.jar"
fi
echo "jar file: "${JAR_FILE}
# script assumes it's in the gateway component directory and common_lib needs to be relative path

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

LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/bin/j9vm
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/classic
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/default
LIBPATH="$LIBPATH":"${JAVA_HOME}"/lib/s390/j9vm
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"

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
fi

keystore_type="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}"
keystore_pass="${ZWE_configs_certificate_keystore_password:-${ZWE_zowe_certificate_keystore_password}}"
key_alias="${ZWE_configs_certificate_keystore_alias:-${ZWE_zowe_certificate_keystore_alias}}"
key_pass="${ZWE_configs_certificate_key_password:-${ZWE_zowe_certificate_key_password:-${keystore_pass}}}"
truststore_type="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}"
truststore_pass="${ZWE_configs_certificate_truststore_password:-${ZWE_zowe_certificate_truststore_password}}"

keystore_location="${ZWE_configs_certificate_keystore_file:-${ZWE_zowe_certificate_keystore_file}}"
truststore_location="${ZWE_configs_certificate_truststore_file:-${ZWE_zowe_certificate_truststore_file}}"

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

LOGBACK=""
if [ -n "${ZWE_configs_logging_config}" ]; then
    LOGBACK="-Dlogging.config=${ZWE_configs_logging_config}"
fi

# Disable Java keyring loading for ICSF hardware private key storage.
# Only z/OSMF JWT authentication provider is supported with this type of keyrings.
if [ "${ATTLS_ENABLED}" = "true" -a "${APIML_ATTLS_LOAD_KEYRING:-false}" = "true" ]; then
  keystore_type=
  keystore_pass=
  key_pass=
  key_alias=
  keystore_location=
fi

CLOUD_GATEWAY_CODE=CG
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${CLOUD_GATEWAY_CODE} java \
    -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
    -XX:+ExitOnOutOfMemoryError \
    ${QUICK_START} \
    ${ADD_OPENS} \
    ${LOGBACK} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=${TMPDIR:-/tmp} \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.security.x509.registry.allowedUsers=${ZWE_configs_apiml_security_x509_registry_allowedUsers:-} \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-10023} \
    -Dapiml.service.forwardClientCertEnabled=${ZWE_configs_apiml_service_forwardClientCertEnabled:-false} \
    -Dapiml.security.x509.registry.allowedUsers=${ZWE_configs_apiml_security_x509_registry_allowedUsers:-} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.zoweManifest=${ZWE_zowe_runtimeDirectory}/manifest.json \
    -Dapiml.cloudGateway.registry.enabled=${ZWE_configs_cloudGateway_registry_enabled:-false} \
    -Dserver.address=0.0.0.0 \
    -Deureka.client.serviceUrl.defaultZone=${ZWE_DISCOVERY_SERVICES_LIST} \
    -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true} \
    -Dserver.maxConnectionsPerRoute=${ZWE_configs_server_maxConnectionsPerRoute:-100} \
    -Dserver.maxTotalConnections=${ZWE_configs_server_maxTotalConnections:-1000} \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStoreType="${keystore_type}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyAlias="${key_alias}" \
    -Dserver.ssl.keyPassword="${key_pass}" \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStoreType="${truststore_type}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
    -Djava.library.path=${LIBPATH} \
    -jar ${JAR_FILE} &

pid=$!
echo "pid=${pid}"

wait %1
