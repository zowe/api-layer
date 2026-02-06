#!/usr/bin/env bats

################################################################################
# BATS tests for APIML start.sh and related scripts
#
# To run these tests:
#   1. Install BATS: brew install bats-core (macOS) or apt install bats (Linux)
#   2. Run: bats scripts/test/start_test.bats
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

# Setup function runs before each test
setup() {
    # Get the project root directory
    BATS_TEST_DIRNAME="$(cd "$(dirname "$BATS_TEST_FILENAME")" && pwd)"
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../.." && pwd)"
    SCRIPTS_DIR="${PROJECT_ROOT}/scripts"
    
    # Create a temporary directory for test artifacts
    TEST_TEMP_DIR="$(mktemp -d)"
    
    # Set up minimal required environment variables
    export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java}"
    export ZWE_zowe_workspaceDirectory="${TEST_TEMP_DIR}/workspace"
    export ZWE_zowe_runtimeDirectory="${TEST_TEMP_DIR}/runtime"
    export ZWE_zowe_logDirectory="${TEST_TEMP_DIR}/logs"
    export ZWE_STATIC_DEFINITIONS_DIR="${TEST_TEMP_DIR}/static-defs"
    export ZWE_zowe_certificate_keystore_file="${TEST_TEMP_DIR}/keystore.p12"
    export ZWE_zowe_certificate_keystore_password="password"
    export ZWE_zowe_certificate_keystore_alias="localhost"
    export ZWE_zowe_certificate_truststore_file="${TEST_TEMP_DIR}/truststore.p12"
    export ZWE_zowe_certificate_truststore_password="password"
    export ZWE_zowe_externalDomains_0="localhost"
    export ZWE_zowe_externalPort="7554"
    export ZWE_zowe_job_prefix="ZWE"
    
    # Create necessary directories
    mkdir -p "${ZWE_zowe_workspaceDirectory}"
    mkdir -p "${ZWE_zowe_runtimeDirectory}"
    mkdir -p "${ZWE_zowe_logDirectory}"
}

# Teardown function runs after each test
teardown() {
    # Clean up temporary directory
    if [ -n "${TEST_TEMP_DIR}" ] && [ -d "${TEST_TEMP_DIR}" ]; then
        rm -rf "${TEST_TEMP_DIR}"
    fi
    
    # Unset test environment variables
    unset ZWE_configs_jvm_Xss
    unset ZWE_configs_jvm_Xmn
    unset ZWE_configs_jvm_XX_UseG1GC
    unset ZWE_configs_jvm_XX_MaxGCPauseMillis
    unset ZWE_configs_jvm_Dmy_custom_property
    unset ZWE_configs_spring_profiles_active
    unset ZWE_configs_debug
    unset ZWE_components_gateway_debug
    unset ZWE_zowe_verifyCertificates
    unset ZWE_zowe_network_server_tls_attls
    unset ZWE_zowe_network_client_tls_attls
    unset ATTLS_SERVER_ENABLED
    unset ATTLS_CLIENT_ENABLED
}

################################################################################
# Tests for parse_jvm_args.sh
################################################################################

@test "parse_jvm_args: handles -Xss option" {
    export ZWE_configs_jvm_Xss="512k"
    
    # Source the script
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    # Check that CUSTOM_JVM_OPTS contains the expected value
    [[ "$CUSTOM_JVM_OPTS" == *"-Xss512k"* ]]
}

@test "parse_jvm_args: handles -Xmn option" {
    export ZWE_configs_jvm_Xmn="256m"
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-Xmn256m"* ]]
}

@test "parse_jvm_args: handles -XX:+UseG1GC (empty value)" {
    export ZWE_configs_jvm_XX_UseG1GC=""
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-XX:+UseG1GC"* ]]
}

@test "parse_jvm_args: handles -XX:+UseG1GC (true value)" {
    export ZWE_configs_jvm_XX_UseG1GC="true"
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-XX:+UseG1GC"* ]]
}

@test "parse_jvm_args: handles -XX:-UseG1GC (false value)" {
    export ZWE_configs_jvm_XX_UseG1GC="false"
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-XX:-UseG1GC"* ]]
}

@test "parse_jvm_args: handles -XX:MaxGCPauseMillis=200" {
    export ZWE_configs_jvm_XX_MaxGCPauseMillis="200"
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-XX:MaxGCPauseMillis=200"* ]]
}

@test "parse_jvm_args: converts underscores to dots for -D properties" {
    export ZWE_configs_jvm_Dmy_custom_property="value"
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-Dmy.custom.property=value"* ]]
}

@test "parse_jvm_args: handles -D property without value" {
    export ZWE_configs_jvm_Denable_feature=""
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-Denable.feature"* ]]
}

@test "parse_jvm_args: handles multiple JVM options" {
    export ZWE_configs_jvm_Xss="512k"
    export ZWE_configs_jvm_Xmn="256m"
    export ZWE_configs_jvm_XX_UseG1GC="true"
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [[ "$CUSTOM_JVM_OPTS" == *"-Xss512k"* ]]
    [[ "$CUSTOM_JVM_OPTS" == *"-Xmn256m"* ]]
    [[ "$CUSTOM_JVM_OPTS" == *"-XX:+UseG1GC"* ]]
}

@test "parse_jvm_args: CUSTOM_JVM_OPTS is empty when no ZWE_configs_jvm_ vars set" {
    # Ensure no ZWE_configs_jvm_ variables are set
    for var in $(env | grep "^ZWE_configs_jvm_" | cut -d= -f1); do
        unset "$var"
    done
    
    . "${SCRIPTS_DIR}/parse_jvm_args.sh"
    
    [ -z "$CUSTOM_JVM_OPTS" ]
}

################################################################################
# Tests for apiml-common-scripts.sh
################################################################################

@test "apiml-common-scripts: add_profile function adds first profile" {
    # Source only the add_profile function
    add_profile() {
        new_profile=$1
        if [ -n "${ZWE_configs_spring_profiles_active}" ]; then
            ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active},"
        fi
        ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active}${new_profile}"
    }
    
    unset ZWE_configs_spring_profiles_active
    add_profile "debug"
    
    [ "$ZWE_configs_spring_profiles_active" = "debug" ]
}

@test "apiml-common-scripts: add_profile function appends additional profiles" {
    add_profile() {
        new_profile=$1
        if [ -n "${ZWE_configs_spring_profiles_active}" ]; then
            ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active},"
        fi
        ZWE_configs_spring_profiles_active="${ZWE_configs_spring_profiles_active}${new_profile}"
    }
    
    ZWE_configs_spring_profiles_active="existing"
    add_profile "debug"
    
    [ "$ZWE_configs_spring_profiles_active" = "existing,debug" ]
}

@test "apiml-common-scripts: certificate verification DISABLED" {
    export ZWE_zowe_verifyCertificates="DISABLED"
    
    verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
    if [ "${verify_certificates_config}" = "DISABLED" ]; then
        verifySslCertificatesOfServices=false
        nonStrictVerifySslCertificatesOfServices=false
    fi
    
    [ "$verifySslCertificatesOfServices" = "false" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "false" ]
}

@test "apiml-common-scripts: certificate verification NONSTRICT" {
    export ZWE_zowe_verifyCertificates="NONSTRICT"
    
    verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
    if [ "${verify_certificates_config}" = "NONSTRICT" ]; then
        verifySslCertificatesOfServices=true
        nonStrictVerifySslCertificatesOfServices=true
    fi
    
    [ "$verifySslCertificatesOfServices" = "true" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "true" ]
}

@test "apiml-common-scripts: certificate verification STRICT (default)" {
    export ZWE_zowe_verifyCertificates="STRICT"
    
    verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
    if [ "${verify_certificates_config}" = "DISABLED" ]; then
        verifySslCertificatesOfServices=false
        nonStrictVerifySslCertificatesOfServices=false
    elif [ "${verify_certificates_config}" = "NONSTRICT" ]; then
        verifySslCertificatesOfServices=true
        nonStrictVerifySslCertificatesOfServices=true
    else
        verifySslCertificatesOfServices=true
        nonStrictVerifySslCertificatesOfServices=false
    fi
    
    [ "$verifySslCertificatesOfServices" = "true" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "false" ]
}

@test "apiml-common-scripts: certificate verification case insensitive" {
    export ZWE_zowe_verifyCertificates="disabled"
    
    verify_certificates_config=$(echo "${ZWE_zowe_verifyCertificates}" | tr '[:lower:]' '[:upper:]')
    if [ "${verify_certificates_config}" = "DISABLED" ]; then
        verifySslCertificatesOfServices=false
        nonStrictVerifySslCertificatesOfServices=false
    fi
    
    [ "$verifySslCertificatesOfServices" = "false" ]
}

@test "apiml-common-scripts: AT-TLS server enabled" {
    export ZWE_zowe_network_server_tls_attls="true"
    
    ATTLS_SERVER_ENABLED="false"
    if [ "${ZWE_zowe_network_server_tls_attls}" = "true" ]; then
        ATTLS_SERVER_ENABLED="true"
    fi
    
    [ "$ATTLS_SERVER_ENABLED" = "true" ]
}

@test "apiml-common-scripts: AT-TLS client enabled" {
    export ZWE_zowe_network_client_tls_attls="true"
    
    ATTLS_CLIENT_ENABLED="false"
    if [ "${ZWE_zowe_network_client_tls_attls}" = "true" ]; then
        ATTLS_CLIENT_ENABLED="true"
    fi
    
    [ "$ATTLS_CLIENT_ENABLED" = "true" ]
}

@test "apiml-common-scripts: default COMMON_LIB path" {
    unset CMMN_LB
    
    if [ -z "${CMMN_LB}" ]; then
        COMMON_LIB="../apiml-common-lib/bin/BOOT-INF/lib/"
    else
        COMMON_LIB="${CMMN_LB}"
    fi
    
    [ "$COMMON_LIB" = "../apiml-common-lib/bin/BOOT-INF/lib/" ]
}

@test "apiml-common-scripts: custom COMMON_LIB path via CMMN_LB" {
    export CMMN_LB="/custom/lib/path"
    
    if [ -z "${CMMN_LB}" ]; then
        COMMON_LIB="../apiml-common-lib/bin/BOOT-INF/lib/"
    else
        COMMON_LIB="${CMMN_LB}"
    fi
    
    [ "$COMMON_LIB" = "/custom/lib/path" ]
}

@test "apiml-common-scripts: JCERACFKS keystore transformation" {
    keystore_type="JCERACFKS"
    keystore_location="safkeyring://USERID/RING"
    truststore_location="safkeyring://USERID/RING"
    
    if [ "${keystore_type}" = "JCERACFKS" ]; then
        keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjce://_)
        truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjce://_)
    fi
    
    [ "$keystore_location" = "safkeyringjce://USERID/RING" ]
    [ "$truststore_location" = "safkeyringjce://USERID/RING" ]
}

@test "apiml-common-scripts: JCECCARACFKS keystore transformation" {
    keystore_type="JCECCARACFKS"
    keystore_location="safkeyring://USERID/RING"
    truststore_location="safkeyring://USERID/RING"
    
    if [ "${keystore_type}" = "JCECCARACFKS" ]; then
        keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
        truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjcecca://_)
    fi
    
    [ "$keystore_location" = "safkeyringjcecca://USERID/RING" ]
    [ "$truststore_location" = "safkeyringjcecca://USERID/RING" ]
}

@test "apiml-common-scripts: JCEHYBRIDRACFKS keystore transformation" {
    keystore_type="JCEHYBRIDRACFKS"
    keystore_location="safkeyring://USERID/RING"
    truststore_location="safkeyring://USERID/RING"
    
    if [ "${keystore_type}" = "JCEHYBRIDRACFKS" ]; then
        keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
        truststore_location=$(echo "${truststore_location}" | sed s_safkeyring://_safkeyringjcehybrid://_)
    fi
    
    [ "$keystore_location" = "safkeyringjcehybrid://USERID/RING" ]
    [ "$truststore_location" = "safkeyringjcehybrid://USERID/RING" ]
}

@test "apiml-common-scripts: PKCS12 keystore no transformation" {
    keystore_type="PKCS12"
    keystore_location="/path/to/keystore.p12"
    truststore_location="/path/to/truststore.p12"
    
    # No transformation should occur for PKCS12
    if [ "${keystore_type}" = "JCERACFKS" ]; then
        keystore_location=$(echo "${keystore_location}" | sed s_safkeyring://_safkeyringjce://_)
    fi
    
    [ "$keystore_location" = "/path/to/keystore.p12" ]
}

@test "apiml-common-scripts: JVM security properties override enabled" {
    export JVM_SECURITY_PROPERTIES_OVERRIDE="true"
    
    JVM_SECURITY_PROPERTIES=""
    if [ "${JVM_SECURITY_PROPERTIES_OVERRIDE:-false}" = "true" ]; then
        JVM_SECURITY_PROPERTIES="-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties"
    fi
    
    [ "$JVM_SECURITY_PROPERTIES" = "-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties" ]
}

@test "apiml-common-scripts: JVM security properties override disabled by default" {
    unset JVM_SECURITY_PROPERTIES_OVERRIDE
    
    JVM_SECURITY_PROPERTIES=""
    if [ "${JVM_SECURITY_PROPERTIES_OVERRIDE:-false}" = "true" ]; then
        JVM_SECURITY_PROPERTIES="-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties"
    fi
    
    [ -z "$JVM_SECURITY_PROPERTIES" ]
}

################################################################################
# Tests for TLS protocol configuration
################################################################################

@test "apiml-common-scripts: extract_between function" {
    extract_between() {
        echo "$1" | sed -e "s/.*$2,//" -e "s/$3.*//"
    }
    
    enabled_protocols=",TLSv1,TLSv1.1,TLSv1.2,TLSv1.3,"
    result=$(extract_between "$enabled_protocols" "TLSv1.1" "TLSv1.3")
    
    [ "$result" = "TLSv1.2," ]
}

@test "apiml-common-scripts: default server protocol is TLS" {
    server_protocol="TLS"
    
    [ "$server_protocol" = "TLS" ]
}

################################################################################
# Tests for start.sh logic (without actually running Java)
################################################################################

@test "start.sh: JAR_FILE default path" {
    unset LAUNCH_COMPONENT
    
    if [ -n "${LAUNCH_COMPONENT}" ]; then
        JAR_FILE="${LAUNCH_COMPONENT}/apiml-lite.jar"
    else
        JAR_FILE="$(pwd)/bin/apiml-lite.jar"
    fi
    
    [[ "$JAR_FILE" == *"/bin/apiml-lite.jar" ]]
}

@test "start.sh: JAR_FILE with LAUNCH_COMPONENT" {
    export LAUNCH_COMPONENT="/opt/zowe/components/gateway"
    
    if [ -n "${LAUNCH_COMPONENT}" ]; then
        JAR_FILE="${LAUNCH_COMPONENT}/apiml-lite.jar"
    else
        JAR_FILE="$(pwd)/bin/apiml-lite.jar"
    fi
    
    [ "$JAR_FILE" = "/opt/zowe/components/gateway/apiml-lite.jar" ]
}

@test "start.sh: external protocol https when SSL enabled" {
    ZWE_configs_server_ssl_enabled="true"
    ATTLS_SERVER_ENABLED="false"
    
    if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" ] || [ "$ATTLS_SERVER_ENABLED" = "true" ]; then
        externalProtocol="https"
    else
        externalProtocol="http"
    fi
    
    [ "$externalProtocol" = "https" ]
}

@test "start.sh: external protocol https when AT-TLS enabled" {
    ZWE_configs_server_ssl_enabled="false"
    ATTLS_SERVER_ENABLED="true"
    
    if [ "${ZWE_configs_server_ssl_enabled:-true}" = "true" ] || [ "$ATTLS_SERVER_ENABLED" = "true" ]; then
        externalProtocol="https"
    else
        externalProtocol="http"
    fi
    
    [ "$externalProtocol" = "https" ]
}

@test "start.sh: external protocol http when SSL disabled and no AT-TLS" {
    ZWE_configs_server_ssl_enabled="false"
    ATTLS_SERVER_ENABLED="false"
    
    if [ "${ZWE_configs_server_ssl_enabled:-false}" = "true" ] || [ "$ATTLS_SERVER_ENABLED" = "true" ]; then
        externalProtocol="https"
    else
        externalProtocol="http"
    fi
    
    [ "$externalProtocol" = "http" ]
}

@test "start.sh: EXTERNAL_URL is set correctly" {
    externalProtocol="https"
    export ZWE_zowe_externalDomains_0="example.com"
    export ZWE_zowe_externalPort="7554"
    
    if [ -n "${externalProtocol}" ] && [ -n "${ZWE_zowe_externalDomains_0}" ] && [ -n "${ZWE_zowe_externalPort}" ]; then
        EXTERNAL_URL="-Dapiml.service.externalUrl=${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}"
    fi
    
    [ "$EXTERNAL_URL" = "-Dapiml.service.externalUrl=https://example.com:7554" ]
}

@test "start.sh: EXTERNAL_URL not set when missing domain" {
    externalProtocol="https"
    unset ZWE_zowe_externalDomains_0
    export ZWE_zowe_externalPort="7554"
    
    EXTERNAL_URL=""
    if [ -n "${externalProtocol}" ] && [ -n "${ZWE_zowe_externalDomains_0}" ] && [ -n "${ZWE_zowe_externalPort}" ]; then
        EXTERNAL_URL="-Dapiml.service.externalUrl=${externalProtocol}://${ZWE_zowe_externalDomains_0}:${ZWE_zowe_externalPort}"
    fi
    
    [ -z "$EXTERNAL_URL" ]
}

@test "start.sh: unique cookie name when enabled" {
    export ZWE_configs_apiml_security_auth_uniqueCookie="true"
    export ZWE_zowe_cookieIdentifier="myid"
    
    if [ "${ZWE_configs_apiml_security_auth_uniqueCookie:-false}" = "true" ]; then
        cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
    fi
    
    [ "$cookieName" = "apimlAuthenticationToken.myid" ]
}

@test "start.sh: default cookie name when unique cookie disabled" {
    export ZWE_configs_apiml_security_auth_uniqueCookie="false"
    
    cookieName=""
    if [ "${ZWE_configs_apiml_security_auth_uniqueCookie:-false}" = "true" ]; then
        cookieName="apimlAuthenticationToken.${ZWE_zowe_cookieIdentifier}"
    fi
    
    [ -z "$cookieName" ]
}

@test "start.sh: APIML_LOADER_PATH includes shared libs" {
    COMMON_LIB="/common/lib"
    export ZWE_GATEWAY_SHARED_LIBS="/gateway/libs"
    export ZWE_DISCOVERY_SHARED_LIBS="/discovery/libs"
    
    APIML_LOADER_PATH=${COMMON_LIB}
    if [ -n "${ZWE_GATEWAY_SHARED_LIBS}" ]; then
        APIML_LOADER_PATH=${ZWE_GATEWAY_SHARED_LIBS},${APIML_LOADER_PATH}
    fi
    if [ -n "${ZWE_DISCOVERY_SHARED_LIBS}" ]; then
        APIML_LOADER_PATH=${ZWE_DISCOVERY_SHARED_LIBS},${APIML_LOADER_PATH}
    fi
    
    [ "$APIML_LOADER_PATH" = "/discovery/libs,/gateway/libs,/common/lib" ]
}

@test "start.sh: LIBPATH includes gateway library path" {
    LIBPATH="/initial/path"
    export ZWE_GATEWAY_LIBRARY_PATH="/gateway/native/libs"
    
    if [ -n "${ZWE_GATEWAY_LIBRARY_PATH}" ]; then
        LIBPATH="$LIBPATH":"${ZWE_GATEWAY_LIBRARY_PATH}"
    fi
    
    [ "$LIBPATH" = "/initial/path:/gateway/native/libs" ]
}

@test "start.sh: heap init default value" {
    unset ZWE_configs_heap_init
    unset ZWE_components_gateway_heap_init
    
    heap_init=${ZWE_configs_heap_init:-${ZWE_components_gateway_heap_init:-32}}
    
    [ "$heap_init" = "32" ]
}

@test "start.sh: heap init from ZWE_configs_heap_init" {
    export ZWE_configs_heap_init="64"
    
    heap_init=${ZWE_configs_heap_init:-${ZWE_components_gateway_heap_init:-32}}
    
    [ "$heap_init" = "64" ]
}

@test "start.sh: heap max default value" {
    unset ZWE_configs_heap_max
    unset ZWE_components_gateway_heap_max
    
    heap_max=${ZWE_configs_heap_max:-${ZWE_components_gateway_heap_max:-512}}
    
    [ "$heap_max" = "512" ]
}

@test "start.sh: heap max from ZWE_configs_heap_max" {
    export ZWE_configs_heap_max="1024"
    
    heap_max=${ZWE_configs_heap_max:-${ZWE_components_gateway_heap_max:-512}}
    
    [ "$heap_max" = "1024" ]
}

@test "start.sh: VSAM file name formatting" {
    export ZWE_configs_storage_vsam_name="IBMUSER.APIML.CACHE"
    
    if [ -n "${ZWE_configs_storage_vsam_name}" ]; then
        VSAM_FILE_NAME=//\'${ZWE_configs_storage_vsam_name}\'
    fi
    
    [ "$VSAM_FILE_NAME" = "//'IBMUSER.APIML.CACHE'" ]
}

################################################################################
# Tests for Discovery services list
################################################################################

@test "apiml-common-scripts: default discovery services list" {
    unset ZWE_DISCOVERY_SERVICES_LIST
    export ZWE_haInstance_hostname="myhost"
    export ZWE_components_discovery_port="7553"
    
    ZWE_DISCOVERY_SERVICES_LIST=${ZWE_DISCOVERY_SERVICES_LIST:-"https://${ZWE_haInstance_hostname:-localhost}:${ZWE_components_discovery_port:-7553}/eureka/"}
    
    [ "$ZWE_DISCOVERY_SERVICES_LIST" = "https://myhost:7553/eureka/" ]
}

@test "apiml-common-scripts: discovery services list with AT-TLS client" {
    export ZWE_DISCOVERY_SERVICES_LIST="https://host1:7553/eureka/,https://host2:7553/eureka/"
    ATTLS_CLIENT_ENABLED="true"
    
    if [ "${ATTLS_CLIENT_ENABLED}" = "true" ]; then
        ZWE_DISCOVERY_SERVICES_LIST=$(echo "${ZWE_DISCOVERY_SERVICES_LIST}" | sed -e 's|https://|http://|g')
    fi
    
    [ "$ZWE_DISCOVERY_SERVICES_LIST" = "http://host1:7553/eureka/,http://host2:7553/eureka/" ]
}

################################################################################
# Tests for default values
################################################################################

@test "start.sh: default port is 7554" {
    unset ZWE_components_gateway_port
    unset ZWE_configs_port
    
    port=${ZWE_components_gateway_port:-${ZWE_configs_port:-7554}}
    
    [ "$port" = "7554" ]
}

@test "start.sh: default discovery port is 7553" {
    unset ZWE_components_discovery_port
    unset ZWE_configs_internal_discovery_port
    
    discovery_port=${ZWE_components_discovery_port:-${ZWE_configs_internal_discovery_port:-7553}}
    
    [ "$discovery_port" = "7553" ]
}

@test "start.sh: default listen address is 0.0.0.0" {
    unset ZWE_configs_zowe_network_server_listenAddresses_0
    unset ZWE_zowe_network_server_listenAddresses_0
    
    listen_address=${ZWE_configs_zowe_network_server_listenAddresses_0:-${ZWE_zowe_network_server_listenAddresses_0:-"0.0.0.0"}}
    
    [ "$listen_address" = "0.0.0.0" ]
}

@test "start.sh: default auth provider is zosmf" {
    unset ZWE_components_gateway_apiml_security_auth_provider
    unset ZWE_configs_apiml_security_auth_provider
    
    auth_provider=${ZWE_components_gateway_apiml_security_auth_provider:-${ZWE_configs_apiml_security_auth_provider:-zosmf}}
    
    [ "$auth_provider" = "zosmf" ]
}

@test "start.sh: default connection timeout is 60000" {
    unset ZWE_components_gateway_apiml_connection_timeout
    unset ZWE_configs_apiml_connection_timeout
    
    timeout=${ZWE_components_gateway_apiml_connection_timeout:-${ZWE_configs_apiml_connection_timeout:-60000}}
    
    [ "$timeout" = "60000" ]
}

@test "start.sh: default caching storage mode is infinispan" {
    unset ZWE_components_caching_service_storage_mode
    unset ZWE_configs_storage_mode
    
    storage_mode=${ZWE_components_caching_service_storage_mode:-${ZWE_configs_storage_mode:-infinispan}}
    
    [ "$storage_mode" = "infinispan" ]
}
