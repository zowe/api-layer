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

keystore_type="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}"
keystore_pass="${ZWE_configs_certificate_keystore_password:-${ZWE_zowe_certificate_keystore_password}}"
truststore_type="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}"
truststore_pass="${ZWE_configs_certificate_truststore_password:-${ZWE_zowe_certificate_truststore_password}}"

# There was an issue where java could throw an exception on keyring read if the password was blank
# But, keyrings dont use passwords, so we just put a dummy value here.
if [ "${keystore_type}" = "JCERACFKS" ]; then
  keystore_pass="dummy"
fi
if [ "${truststore_type}" = "JCERACFKS" ]; then
  truststore_pass="dummy"
fi

# Workaround for Java desiring safkeyring://// instead of just ://
# We can handle both cases of user input by just adding extra "//" if we detect its missing.
ensure_keyring_slashes() {
  keyring_string="${1}"
  only_two_slashes=$(echo "${keyring_string}" | grep "^safkeyring://[^//]")
  if [ -n "${only_two_slashes}" ]; then
    keyring_string=$(echo "${keyring_string}" | sed "s#safkeyring://#safkeyring:////#")
  fi
  # else, unmodified, perhaps its even p12
  echo $keyring_string
}

keystore_location=$(ensure_keyring_slashes "${ZWE_configs_certificate_keystore_file:-${ZWE_zowe_certificate_keystore_file}}")
truststore_location=$(ensure_keyring_slashes "${ZWE_configs_certificate_truststore_file:-${ZWE_zowe_certificate_truststore_file}}")


CLOUD_GATEWAY_CODE=CG
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${CLOUD_GATEWAY_CODE} java \
    -Xms32m -Xmx256m \
    ${QUICK_START} \
    -Dibm.serversocket.recover=true \
    -Dfile.encoding=UTF-8 \
    -Djava.io.tmpdir=/tmp \
    -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
    -Dspring.profiles.include=$LOG_LEVEL \
    -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
    -Dapiml.service.port=${ZWE_configs_port:-10023} \
    -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
    -Dapiml.zoweManifest=${ZWE_zowe_runtimeDirectory}/manifest.json \
    -Dserver.address=0.0.0.0 \
    -Deureka.client.serviceUrl.defaultZone=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"} \
    -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true} \
    -Dserver.maxConnectionsPerRoute=${ZWE_configs_server_maxConnectionsPerRoute:-100} \
    -Dserver.maxTotalConnections=${ZWE_configs_server_maxTotalConnections:-1000} \
    -Dserver.ssl.keyStore="${keystore_location}" \
    -Dserver.ssl.keyStoreType="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}" \
    -Dserver.ssl.keyStorePassword="${keystore_pass}" \
    -Dserver.ssl.keyAlias="${ZWE_configs_certificate_keystore_alias:-${ZWE_zowe_certificate_keystore_alias}}" \
    -Dserver.ssl.keyPassword="${keystore_pass}" \
    -Dserver.ssl.trustStore="${truststore_location}" \
    -Dserver.ssl.trustStoreType="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}" \
    -Dserver.ssl.trustStorePassword="${truststore_pass}" \
    -Djava.protocol.handler.pkgs=com.ibm.crypto.provider \
    -Djava.library.path=${LIBPATH} \
    -jar ${JAR_FILE} &

pid=$!
echo "pid=${pid}"

wait %1
