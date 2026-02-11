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
#   - ZOWE_CONSOLE_LOG_CHARSET
#   - JAVA21_CONSOLE_ENCODING (Java 21+ on z/OS only)
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
    COMMON_LIB="../apiml-common-lib/bin/BOOT-INF/lib/"
else
    COMMON_LIB="${CMMN_LB}"
fi

if [ -z "${LIBRARY_PATH}" ]; then
    LIBRARY_PATH="../common-java-lib/bin/"
fi

################################################################################
# JVM security properties
################################################################################
JVM_SECURITY_PROPERTIES=""
if [ "${JVM_SECURITY_PROPERTIES_OVERRIDE:-false}" = "true" ]; then
    JVM_SECURITY_PROPERTIES="-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties"
fi

################################################################################
# Certificate verification configuration
################################################################################
verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
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
# Platform detection and Java version check
################################################################################
ZOWE_CONSOLE_LOG_CHARSET=UTF-8
if [ "$(uname)" = "OS/390" ]; then
    QUICK_START="-Xquickstart"
    SHARED_CLASSES_OPTS="-Xshareclasses:name=apiml_shared_classes,nonfatal"

    JAVA_VERSION=$(${JAVA_HOME}/bin/javap -J-Xms4m -J-Xmx16m -verbose java.lang.String \
        | grep "major version" \
        | cut -d " " -f5)

    if [ $JAVA_VERSION -ge 65 ]; then # Java 21
        ZOWE_CONSOLE_LOG_CHARSET=IBM-1047
        # Java 21+ changed default encoding to UTF-8 (JEP 400). Set console encoding
        # to EBCDIC for z/OS SYSPRINT to prevent garbled characters in early startup logs
        JAVA21_CONSOLE_ENCODING="-Dstdout.encoding=${ZOWE_CONSOLE_LOG_CHARSET} -Dstderr.encoding=${ZOWE_CONSOLE_LOG_CHARSET}"
    fi
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

# Handle RACF keyring URL transformations
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
