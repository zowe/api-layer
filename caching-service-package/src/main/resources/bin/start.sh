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

# Optional variables:
# - LIBPATH
# - LIBRARY_PATH
# - ZWE_components_discovery_port - the port the discovery service will use
# - ZWE_components_gateway_port - the port the api gateway service will use
# - ZWE_configs_server_ssl_enabled
# - ZWE_configs_heap_max
# - ZWE_configs_heap_init
# - ZWE_configs_storage_evictionStrategy
# - ZWE_configs_storage_mode
# - ZWE_configs_storage_size
# - ZWE_configs_storage_vsam_name
# - ZWE_configs_certificate_keystore_alias - The alias of the key within the keystore
# - ZWE_configs_certificate_keystore_file - The keystore to use for SSL certificates
# - ZWE_configs_certificate_keystore_password - The password to access the keystore supplied by KEYSTORE
# - ZWE_configs_certificate_keystore_type - The keystore type to use for SSL certificates
# - ZWE_configs_certificate_truststore_file
# - ZWE_configs_certificate_truststore_type
# - ZWE_configs_debug
# - ZWE_configs_port - the port the caching service will use
# - ZWE_configs_spring_profiles_active
# - ZWE_DISCOVERY_SERVICES_LIST
# - ZWE_zowe_network_server_tls_attls
# - ZWE_haInstance_hostname
# - ZWE_zowe_certificate_keystore_type - The default keystore type to use for SSL certificates
# - ZWE_zowe_verifyCertificates - if we accept only verified certificates
if [ -n "${LAUNCH_COMPONENT}" ]
then
    JAR_FILE="${LAUNCH_COMPONENT}/caching-service.jar"
else
    JAR_FILE="$(pwd)/bin/caching-service.jar"
fi

echo "jar file: "${JAR_FILE}
# API Mediation Layer Debug Mode
export LOG_LEVEL=

if [ "${ZWE_configs_debug}" = "true" ]
then
  export LOG_LEVEL="debug"
fi

if [ -z "${LIBRARY_PATH}" ]
then
    LIBRARY_PATH="../common-java-lib/bin/"
fi

if [ -n "${ZWE_configs_storage_vsam_name}" ]
then
    VSAM_FILE_NAME=//\'${ZWE_configs_storage_vsam_name}\'
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

if [ -z "${ZWE_configs_storage_infinispan_persistence_dataLocation}" ]; then
  if [ -n "${ZWE_zowe_workspaceDirectory}" ]; then
    ZWE_configs_storage_infinispan_persistence_dataLocation="${ZWE_zowe_workspaceDirectory}/caching-service/data"
  fi
fi
if [ -z "${ZWE_configs_storage_infinispan_persistence_indexLocation}" ]; then
  if [ -n "${ZWE_zowe_workspaceDirectory}" ]; then
    ZWE_configs_storage_infinispan_persistence_indexLocation="${ZWE_zowe_workspaceDirectory}/caching-service/index"
  fi
fi
if [ -z "${ZWE_configs_storage_infinispan_initialHosts}" ]; then
  ZWE_configs_storage_infinispan_initialHosts="${ZWE_haInstance_hostname:-localhost}[${ZWE_configs_storage_infinispan_jgroups_port:-7098}]"
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
        --add-opens=java.base/javax.net.ssl=ALL-UNNAMED
        --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
        --add-opens=java.base/java.io=ALL-UNNAMED"

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
#   -Dapiml.service.ipAddress=${ZOWE_IP_ADDRESS:-127.0.0.1} \
#   -Dapiml.service.preferIpAddress=${APIML_PREFER_IP_ADDRESS:-false} \

if [ "${ATTLS_ENABLED}" = "true" -a "${APIML_ATTLS_LOAD_KEYRING:-false}" = "true" ]; then
  keystore_type=
  keystore_pass=
  key_pass=
  key_alias=
  keystore_location=
fi

CACHING_CODE=CS
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${CACHING_CODE} java \
  -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
  -XX:+ExitOnOutOfMemoryError \
  ${QUICK_START} \
  ${ADD_OPENS} \
  -Dibm.serversocket.recover=true \
  -Dfile.encoding=UTF-8 \
  -Djava.io.tmpdir=${TMPDIR:-/tmp} \
  -Dspring.profiles.active=${ZWE_configs_spring_profiles_active:-} \
  -Dspring.profiles.include=$LOG_LEVEL \
  -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
  -Dapiml.service.port=${ZWE_configs_port:-7555} \
  -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
  -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
  -Dapiml.service.customMetadata.apiml.gatewayPort=${ZWE_components_gateway_port:-7554} \
  -Dapiml.service.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices:-false} \
  -Dapiml.service.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
  -Dapiml.httpclient.ssl.enabled-protocols=${ZWE_components_gateway_apiml_httpclient_ssl_enabled_protocols:-"TLSv1.2"} \
  -Dcaching.storage.evictionStrategy=${ZWE_configs_storage_evictionStrategy:-reject} \
  -Dcaching.storage.size=${ZWE_configs_storage_size:-10000} \
  -Dcaching.storage.mode=${ZWE_configs_storage_mode:-inMemory} \
  -Dcaching.storage.vsam.name=${VSAM_FILE_NAME} \
  -Djgroups.bind.address=${ZWE_haInstance_hostname:-localhost} \
  -Djgroups.bind.port=${ZWE_configs_storage_infinispan_jgroups_port:-7098} \
  -Djgroups.keyExchange.port=${ZWE_configs_storage_infinispan_jgroups_keyExchange_port:-7118} \
  -Dcaching.storage.infinispan.persistence.dataLocation=${ZWE_configs_storage_infinispan_persistence_dataLocation:-data} \
  -Dcaching.storage.infinispan.persistence.indexLocation=${ZWE_configs_storage_infinispan_persistence_indexLocation:-index} \
  -Dcaching.storage.infinispan.initialHosts=${ZWE_configs_storage_infinispan_initialHosts:-localhost[7098]} \
  -Dserver.address=0.0.0.0 \
  -Dserver.ssl.enabled=${ZWE_configs_server_ssl_enabled:-true}  \
  -Dserver.ssl.protocol=${ZWE_configs_server_ssl_protocol:-"TLSv1.2"}  \
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
  -jar "${JAR_FILE}" &
pid=$!
echo "pid=${pid}"

wait %1
