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

# JAR file location
if [ -n "${LAUNCH_COMPONENT}" ]; then
    JAR_FILE="${LAUNCH_COMPONENT}/caching-service.jar"
    . "scripts/apiml-common-scripts.sh"
    . "scripts/parse_jvm_args.sh"
else
    JAR_FILE="$(pwd)/bin/caching-service.jar"
    . "$(pwd)/bin/apiml-common-scripts.sh"
    . "$(pwd)/bin/parse_jvm_args.sh"
fi
echo "jar file: ${JAR_FILE}"

# Debug profile
if [ "${ZWE_configs_debug}" = "true" ]; then
    add_profile "debug"
fi

# VSAM file name for caching
if [ -n "${ZWE_configs_storage_vsam_name}" ]; then
    VSAM_FILE_NAME=//\'${ZWE_configs_storage_vsam_name}\'
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

# migration step of Infinispan since version 3.2 (see #https://github.com/zowe/api-layer/pull/3960)
original_infinispan_data_location="${ZWE_configs_storage_infinispan_persistence_dataLocation:-${ZWE_zowe_workspaceDirectory:-$(pwd)}}/caching-service/data"
if [ -d "${original_infinispan_data_location}" ]; then
    mv -f "${original_infinispan_data_location}" "${ZWE_zowe_workspaceDirectory:-$(pwd)}/caching-service/${ZWE_haInstance_id:-localhost}/data"
fi
original_infinispan_index_location="${ZWE_configs_storage_infinispan_persistence_indexLocation:-${ZWE_zowe_workspaceDirectory:-$(pwd)}}/caching-service/index"
if [ -d "${original_infinispan_index_location}" ]; then
    mv -f "${original_infinispan_index_location}" "${ZWE_zowe_workspaceDirectory:-$(pwd)}/caching-service/${ZWE_haInstance_id:-localhost}/index"
fi

CACHING_CODE=CS
_BPXK_AUTOCVT=OFF
_BPX_JOBNAME=${ZWE_zowe_job_prefix}${CACHING_CODE} ${JAVA_BIN_DIR}java \
  -Xms${ZWE_configs_heap_init:-32}m -Xmx${ZWE_configs_heap_max:-512}m \
  -XX:+ExitOnOutOfMemoryError \
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
  -Dapiml.logs.location=${ZWE_zowe_logDirectory} \
  -Dapiml.health.protected=${ZWE_configs_apiml_health_protected:-true} \
  -Dapiml.service.port=${ZWE_configs_port:-7555} \
  -Dapiml.service.hostname=${ZWE_haInstance_hostname:-localhost} \
  -Dapiml.service.discoveryServiceUrls=${ZWE_DISCOVERY_SERVICES_LIST} \
  -Dapiml.service.customMetadata.apiml.gatewayPort=${ZWE_components_gateway_port:-7554} \
  -Dapiml.service.ssl.verifySslCertificatesOfServices=${verifySslCertificatesOfServices:-false} \
  -Dapiml.service.ssl.nonStrictVerifySslCertificatesOfServices=${nonStrictVerifySslCertificatesOfServices:-false} \
  -Dapiml.service.ssl.enabled-protocols=${ZWE_configs_apiml_ssl_enabled_protocols:-${client_enabled_protocols}} \
  -Dapiml.service.ssl.ciphers=${ZWE_configs_apiml_ssl_ciphers:-${client_ciphers}} \
  -Dapiml.service.ssl.key-alias="${ZWE_configs_apiml_ssl_key_alias:-${key_alias}}" \
  -Dapiml.service.ssl.key-password="${ZWE_configs_apiml_ssl_key_password:-${key_pass}}" \
  -Dapiml.service.ssl.key-store="${ZWE_configs_apiml_ssl_key_store:-${keystore_location}}" \
  -Dapiml.service.ssl.key-store-password="${ZWE_configs_apiml_ssl_key_store_password:-${keystore_pass}}" \
  -Dapiml.service.ssl.key-store-type="${ZWE_configs_apiml_ssl_key_store_type:-${keystore_type}}" \
  -Dapiml.service.ssl.protocol=${ZWE_configs_apiml_ssl_protocol:-${server_protocol}} \
  -Dapiml.service.ssl.trust-store="${ZWE_configs_apiml_ssl_trust_store:-${truststore_location}}" \
  -Dapiml.service.ssl.trust-store-password="${ZWE_configs_apiml_ssl_trust_store_password:-${truststore_pass}}" \
  -Dapiml.service.ssl.trust-store-type="${ZWE_configs_apiml_ssl_trust_store_type:-${truststore_type}}" \
  -Djdk.tls.client.cipherSuites=${client_ciphers} \
  -Dserver.ssl.ciphers=${server_ciphers} \
  -Dserver.ssl.protocol=${server_protocol} \
  -Dserver.ssl.enabled-protocols=${server_enabled_protocols} \
  -Dcaching.storage.evictionStrategy=${ZWE_configs_storage_evictionStrategy:-reject} \
  -Dcaching.storage.size=${ZWE_configs_storage_size:-10000} \
  -Dcaching.storage.mode=${ZWE_configs_storage_mode:-inMemory} \
  -Dcaching.storage.vsam.name=${VSAM_FILE_NAME} \
  -Djgroups.bind.address=${ZWE_configs_storage_infinispan_jgroups_host:-${ZWE_haInstance_hostname:-localhost}} \
  -Djgroups.bind.port=${ZWE_configs_storage_infinispan_jgroups_port:-7600} \
  -Djgroups.keyExchange.port=${ZWE_configs_storage_infinispan_jgroups_keyExchange_port:-7601} \
  -Djgroups.tcp.diag.enabled=${ZWE_configs_storage_infinispan_jgroups_tcp_diag_enabled:-false} \
  -Dcaching.storage.infinispan.initialHosts=${ZWE_configs_storage_infinispan_initialHosts:-localhost[7600]} \
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
  -Djava.net.preferIPv4Stack=true \
  -Djavax.net.debug=${ZWE_configs_sslDebug:-""} \
  -Djava.library.path=${LIBPATH} \
  -Dotel.sdk.disabled=true \
  -jar "${JAR_FILE}" &
pid=$!
echo "pid=${pid}"

wait %1
