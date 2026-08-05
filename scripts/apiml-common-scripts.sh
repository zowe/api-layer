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

# Common utility functions and configurations for APIML start scripts
# This script should be sourced by individual component start.sh scripts
#
# Usage: . "${SCRIPT_DIR}/apiml-common-scripts.sh"
#
# After sourcing, the following variables will be set:
#   - QUICK_START (z/OS only)
#   - ADD_OPENS
#   - LIBPATH
#   - ATTLS_SERVER_ENABLED
#   - ATTLS_CLIENT_ENABLED
#   - internalProtocol
#   - externalProtocol
#   - verifySslCertificatesOfServices
#   - nonStrictVerifySslCertificatesOfServices
#   - server_protocol
#   - server_enabled_protocols
#   - server_ciphers
#   - client_enabled_protocols
#   - client_ciphers
#   - keystore_type, keystore_pass, key_alias, key_pass
#   - truststore_type, truststore_pass
#   - keystore_location, truststore_location
#   - LOGBACK
#   - JVM_SECURITY_PROPERTIES
#   - JAVA_BIN_DIR
#   - COMMON_LIB
#   - LIBRARY_PATH
#   - ZWE_DISCOVERY_SERVICES_LIST
#   - CERTIFICATES_URLS
#   - discoveryUserid
#   - discoveryPassword

################################################################################
# Function: add_profile
# Adds a Spring profile to ZWE_configs_spring_profiles_active
################################################################################
add_profile() {
    new_profile=$1
    if [ -n "${ZWE_configs_spring_profiles_active}" ]; then
        ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active},"
    fi
    ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active}${new_profile}"
}

################################################################################
# Common library path setup
################################################################################
if [ -z "${CMMN_LB}" ]; then
    if [ -d "apiml-common-lib/bin/BOOT-INF/lib/" ]; then
        COMMON_LIB="apiml-common-lib/bin/BOOT-INF/lib/"
    else
        COMMON_LIB="../apiml-common-lib/bin/BOOT-INF/lib/"
    fi
else
    COMMON_LIB="${CMMN_LB}"
fi

if [ -z "${LIBRARY_PATH}" ]; then
    if [ -d "common-java-lib/bin/" ]; then
        LIBRARY_PATH="common-java-lib/bin/"
    else
        LIBRARY_PATH="../common-java-lib/bin/"
    fi
fi

################################################################################
# JVM security properties
################################################################################
JVM_SECURITY_PROPERTIES=""
if [ "${JVM_SECURITY_PROPERTIES_OVERRIDE:-false}" = "true" ]; then
    if [ -f "apiml-common-lib/bin/jvm.security.override.properties" ]; then
        JVM_SECURITY_PROPERTIES="-Djava.security.properties=apiml-common-lib/bin/jvm.security.override.properties"
    else
        JVM_SECURITY_PROPERTIES="-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties"
    fi
fi

################################################################################
# Certificate verification configuration
################################################################################
verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates:-STRICT}" | tr '[:lower:]' '[:upper:]')
if [ "${verify_certificates_config}" = "DISABLED" ]; then
    verifySslCertificatesOfServices=false
    nonStrictVerifySslCertificatesOfServices=false
elif [ "${verify_certificates_config}" = "NONSTRICT" ]; then
    verifySslCertificatesOfServices=true
    nonStrictVerifySslCertificatesOfServices=true
else
    # default value is STRICT
    verifySslCertificatesOfServices=true
    nonStrictVerifySslCertificatesOfServices=false
fi

################################################################################
# Eureka discovery credentials
# Map from ZWE_configs_apiml_discovery_userid/password; when certificate
# verification is disabled and no explicit value is set, default to
# "eureka"/"password" so services can authenticate to the discovery endpoint.
################################################################################
discoveryUserid=${ZWE_configs_apiml_discovery_userid:-${ZWE_components_discovery_apiml_discovery_userid:-}}
discoveryPassword=${ZWE_configs_apiml_discovery_password:-${ZWE_components_discovery_apiml_discovery_password:-}}

if [ "${verifySslCertificatesOfServices}" = "false" ]; then
    discoveryUserid=${discoveryUserid:-eureka}
    discoveryPassword=${discoveryPassword:-password}
fi

################################################################################
# Platform detection and Java version check
################################################################################
if [ "$(uname)" = "OS/390" ]; then
    QUICK_START="-Xquickstart"
    SHARED_CLASSES_OPTS="-Xshareclasses:name=apiml_shared_classes,nonfatal,silent"
fi

################################################################################
# AT-TLS configuration
################################################################################
ATTLS_SERVER_ENABLED="false"
ATTLS_CLIENT_ENABLED="false"

if [ "${ZWE_zowe_network_server_tls_attls}" = "true" ]; then
    ATTLS_SERVER_ENABLED="true"
fi
if [ "${ZWE_zowe_network_client_tls_attls}" = "true" ]; then
    ATTLS_CLIENT_ENABLED="true"
fi

################################################################################
# Discovery services list and protocol configuration
################################################################################
internalProtocol="https"
ZWE_DISCOVERY_SERVICES_LIST=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"}

if [ "${ATTLS_CLIENT_ENABLED}" = "true" ]; then
    ZWE_DISCOVERY_SERVICES_LIST=$(echo "${ZWE_DISCOVERY_SERVICES_LIST=}" | sed -e 's|https://|http://|g')
    internalProtocol=http
fi

################################################################################
# Certificates URLs
################################################################################
CERTIFICATES_URLS=${internalProtocol:-https}://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_gateway_port:-7554}/gateway/certificates
CERTIFICATES_URLS=${ZWE_configs_apiml_security_x509_certificatesUrl:-${ZWE_components_gateway_apiml_security_x509_certificatesUrl:-${CERTIFICATES_URLS}}}
CERTIFICATES_URLS=${ZWE_configs_apiml_security_x509_certificatesUrls:-${ZWE_components_gateway_apiml_security_x509_certificatesUrls:-${CERTIFICATES_URLS}}}

################################################################################
# LIBPATH setup
################################################################################
LIBPATH="$LIBPATH":"/lib"
LIBPATH="$LIBPATH":"/usr/lib"
LIBPATH="$LIBPATH":"${JAVA_HOME}/bin"
LIBPATH="$LIBPATH":"${JAVA_HOME}/bin/classic"
LIBPATH="$LIBPATH":"${JAVA_HOME}/bin/j9vm"
LIBPATH="$LIBPATH":"${JAVA_HOME}/lib/s390x/classic"
LIBPATH="$LIBPATH":"${JAVA_HOME}/lib/s390x/default"
LIBPATH="$LIBPATH":"${JAVA_HOME}/lib/s390x/j9vm"
LIBPATH="$LIBPATH":"${LIBRARY_PATH}"

################################################################################
# Java module opens for reflection
################################################################################
ADD_OPENS="--add-opens=java.base/java.lang=ALL-UNNAMED
        --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
        --add-opens=java.base/java.nio.channels.spi=ALL-UNNAMED
        --add-opens=java.base/java.util=ALL-UNNAMED
        --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
        --add-opens=java.base/javax.net.ssl=ALL-UNNAMED
        --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
        --add-opens=java.base/java.io=ALL-UNNAMED"

################################################################################
# TLS Protocol configuration functions
################################################################################
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

################################################################################
# Server and client TLS configuration
################################################################################
DEFAULT_CIPHERS="TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_DSS_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256,TLS_RSA_WITH_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_RSA_WITH_AES_256_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV"

server_protocol="TLS"
get_enabled_protocol "server"
server_enabled_protocols=${result:-"TLSv1.3"}
server_ciphers=${ZWE_configs_zowe_network_server_tls_ciphers:-${ZWE_components_gateway_zowe_network_server_tls_ciphers:-${ZWE_zowe_network_server_tls_ciphers:-${DEFAULT_CIPHERS}}}}

get_enabled_protocol "client"
client_enabled_protocols=${ZWE_components_gateway_apiml_httpclient_ssl_enabled_protocols:-${result:-${server_enabled_protocols}}}
client_ciphers=${ZWE_configs_zowe_network_client_tls_ciphers:-${ZWE_components_gateway_zowe_network_client_tls_ciphers:-${ZWE_zowe_network_client_tls_ciphers:-${server_ciphers}}}}

################################################################################
# Keystore and truststore configuration
################################################################################
keystore_type="${ZWE_configs_certificate_keystore_type:-${ZWE_zowe_certificate_keystore_type:-PKCS12}}"
keystore_pass="${ZWE_configs_certificate_keystore_password:-${ZWE_zowe_certificate_keystore_password}}"
key_alias="${ZWE_configs_certificate_keystore_alias:-${ZWE_zowe_certificate_keystore_alias}}"

key_pass="${ZWE_configs_certificate_key_password:-${ZWE_zowe_certificate_key_password:-${keystore_pass}}}"
truststore_type="${ZWE_configs_certificate_truststore_type:-${ZWE_zowe_certificate_truststore_type:-PKCS12}}"
truststore_pass="${ZWE_configs_certificate_truststore_password:-${ZWE_zowe_certificate_truststore_password}}"

keystore_location="${ZWE_configs_certificate_keystore_file:-${ZWE_zowe_certificate_keystore_file}}"
truststore_location="${ZWE_configs_certificate_truststore_file:-${ZWE_zowe_certificate_truststore_file}}"

client_key_alias="${ZWE_configs_apiml_service_ssl_keystore_alias:-${ZWE_zowe_certificate_keystore_clientCertificateAlias:-${key_alias}}}"
client_keystore_type="${ZWE_configs_apiml_service_ssl_keystore_type:-${keystore_type}}"
client_keystore_pass="${ZWE_configs_apiml_service_ssl_keystore_password:-${keystore_pass}}"
client_key_pass="${ZWE_configs_apiml_service_ssl_key_password:-${key_pass}}"
client_keystore_location="${ZWE_configs_apiml_service_ssl_keystore_file:-${keystore_location}}"
client_truststore_type="${ZWE_configs_apiml_service_ssl_truststore_type:-${truststore_type}}"
client_truststore_pass="${ZWE_configs_apiml_service_ssl_truststore_password:-${truststore_pass}}"
client_truststore_location="${ZWE_configs_apiml_service_ssl_truststore_file:-${truststore_location}}"

# Handle RACF keyring URL transformations
if [ "${keystore_type}" = "JCERACFKS" ]; then
    keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjce://_)
    client_keystore_location=$(echo "${client_keystore_location}" | sed s_safkeyring://_safkeyringjce://_)
    truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjce://_)
    client_truststore_location=$(echo "${client_truststore_location}" | sed s_safkeyring://_safkeyringjce://_)
elif [ "${keystore_type}" = "JCECCARACFKS" ]; then
    keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
    client_keystore_location=$(echo "${client_keystore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
    truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
    client_truststore_location=$(echo "${client_truststore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
elif [ "${keystore_type}" = "JCEHYBRIDRACFKS" ]; then
    keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
    client_keystore_location=$(echo "${client_keystore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
    truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
    client_truststore_location=$(echo "${client_truststore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
fi

################################################################################
# Logback configuration
################################################################################
LOGBACK=""
if [ -n "${ZWE_configs_logging_config}" ]; then
    LOGBACK="-Dlogging.config=${ZWE_configs_logging_config}"
fi

################################################################################
# Java binary directory
################################################################################
if [ -n "${ZWE_java_home}" ]; then
    JAVA_BIN_DIR=${ZWE_java_home}/bin/
fi

################################################################################
# Eureka instance IP address override - falls back to Eureka's own auto-detection by leaving the value unset when none of envs below are set
################################################################################
TMP_EUREKA_IP_ADDRESS=${ZWE_configs_apiml_service_ipAddress:-${ZWE_components_gateway_apiml_service_ipAddress:-${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0}}}}
if [ -n "${TMP_EUREKA_IP_ADDRESS}" ] && [ "${TMP_EUREKA_IP_ADDRESS}" != "0.0.0.0" ]; then
    EUREKA_IP_ADDRESS="-Deureka.instance.ipAddress=${ZWE_configs_apiml_service_ipAddress:-${ZWE_components_gateway_apiml_service_ipAddress:-${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0}}}}"
fi
