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
    LAUNCH_COMPONENT="${PROJECT_ROOT}/scripts"
    # Create a temporary directory for test artifacts
    TEST_TEMP_DIR="$(mktemp -d)"

    # Set up minimal required environment variables for sourcing scripts
    export JAVA_HOME="${TEST_TEMP_DIR}/java"
    mkdir -p "${JAVA_HOME}/bin"

    # Create a mock java binary that returns version info
    cat > "${JAVA_HOME}/bin/javap" << 'MOCK_JAVAP'
#!/bin/sh
echo "major version: 61"
MOCK_JAVAP
    chmod +x "${JAVA_HOME}/bin/javap"

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
    unset ZWE_configs_jvm_Denable_feature
    unset ZWE_configs_spring_profiles_active
    unset ZWE_configs_debug
    unset ZWE_components_gateway_debug
    unset ZWE_zowe_verifyCertificates
    unset ZWE_zowe_network_server_tls_attls
    unset ZWE_zowe_network_client_tls_attls
    unset ATTLS_SERVER_ENABLED
    unset ATTLS_CLIENT_ENABLED
    unset CMMN_LB
    unset LIBRARY_PATH
    unset JVM_SECURITY_PROPERTIES_OVERRIDE
    unset ZWE_configs_logging_config
    unset ZWE_java_home
    unset ZWE_haInstance_hostname
    unset ZWE_components_discovery_port
    unset ZWE_DISCOVERY_SERVICES_LIST
    unset ZWE_configs_certificate_keystore_type
    unset ZWE_zowe_certificate_keystore_type
    unset ZWE_configs_certificate_keystore_file
    unset ZWE_configs_certificate_truststore_file
}

################################################################################
# Tests for parse_jvm_args.sh
################################################################################

@test "parse_jvm_args: handles -Xss option" {
    export ZWE_configs_jvm_Xss="512k"

    . "${SCRIPTS_DIR}/parse_jvm_args.sh"

    [[ "$CUSTOM_JVM_OPTS" == *"-Xss512k"* ]]
}

@test "parse_jvm_args: handles -Xmn option" {
    export ZWE_configs_jvm_Xmn="256m"

    . "${SCRIPTS_DIR}/parse_jvm_args.sh"

    [[ "$CUSTOM_JVM_OPTS" == *"-Xmn256m"* ]]
}

@test "parse_jvm_args: handles -Xverbosegclog /verbousloggc.xml" {
    export ZWE_configs_jvm_Xverbosegclog=":/verbousloggc.xml"

    . "${SCRIPTS_DIR}/parse_jvm_args.sh"

    [[ "$CUSTOM_JVM_OPTS" == *"-Xverbosegclog:/verbousloggc.xml"* ]]
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

@test "parse_jvm_args: handles - property" {
    export ZWE_configs_jvm_agentpath=":/u/users/cai/sysview/runtime/cnm4h00/CNM4JVMD/libgsvoagt4.so"

    . "${SCRIPTS_DIR}/parse_jvm_args.sh"

    [[ "$CUSTOM_JVM_OPTS" == *"-agentpath:/u/users/cai/sysview/runtime/cnm4h00/CNM4JVMD/libgsvoagt4.so"* ]]
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
# Tests for apiml-common-scripts.sh - sourcing actual script
################################################################################

@test "apiml-common-scripts: sets default COMMON_LIB path" {
    unset CMMN_LB

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$COMMON_LIB" = "../apiml-common-lib/bin/BOOT-INF/lib/" ]
}

@test "apiml-common-scripts: uses custom COMMON_LIB path via CMMN_LB" {
    export CMMN_LB="/custom/lib/path"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$COMMON_LIB" = "/custom/lib/path" ]
}

@test "apiml-common-scripts: sets default LIBRARY_PATH" {
    unset LIBRARY_PATH

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$LIBRARY_PATH" = "../common-java-lib/bin/" ]
}

@test "apiml-common-scripts: certificate verification DISABLED sets both to false" {
    export ZWE_zowe_verifyCertificates="DISABLED"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$verifySslCertificatesOfServices" = "false" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "false" ]
}

@test "apiml-common-scripts: certificate verification NONSTRICT sets both to true" {
    export ZWE_zowe_verifyCertificates="NONSTRICT"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$verifySslCertificatesOfServices" = "true" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "true" ]
}

@test "apiml-common-scripts: certificate verification STRICT (default)" {
    export ZWE_zowe_verifyCertificates="STRICT"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$verifySslCertificatesOfServices" = "true" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "false" ]
}

@test "apiml-common-scripts: certificate verification is case insensitive" {
    export ZWE_zowe_verifyCertificates="disabled"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$verifySslCertificatesOfServices" = "false" ]
    [ "$nonStrictVerifySslCertificatesOfServices" = "false" ]
}

@test "apiml-common-scripts: AT-TLS server disabled by default" {
    unset ZWE_zowe_network_server_tls_attls

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ATTLS_SERVER_ENABLED" = "false" ]
}

@test "apiml-common-scripts: AT-TLS server enabled when configured" {
    export ZWE_zowe_network_server_tls_attls="true"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ATTLS_SERVER_ENABLED" = "true" ]
}

@test "apiml-common-scripts: AT-TLS client disabled by default" {
    unset ZWE_zowe_network_client_tls_attls

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ATTLS_CLIENT_ENABLED" = "false" ]
}

@test "apiml-common-scripts: AT-TLS client enabled when configured" {
    export ZWE_zowe_network_client_tls_attls="true"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ATTLS_CLIENT_ENABLED" = "true" ]
}

@test "apiml-common-scripts: sets default internalProtocol to https" {
    unset ZWE_zowe_network_client_tls_attls

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$internalProtocol" = "https" ]
}

@test "apiml-common-scripts: sets internalProtocol to http when AT-TLS client enabled" {
    export ZWE_zowe_network_client_tls_attls="true"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$internalProtocol" = "http" ]
}

@test "apiml-common-scripts: sets default discovery services list" {
    unset ZWE_DISCOVERY_SERVICES_LIST
    export ZWE_haInstance_hostname="myhost"
    export ZWE_components_discovery_port="7553"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ZWE_DISCOVERY_SERVICES_LIST" = "https://myhost:7553/eureka/" ]
}

@test "apiml-common-scripts: converts discovery services list to http when AT-TLS client enabled" {
    export ZWE_DISCOVERY_SERVICES_LIST="https://host1:7553/eureka/,https://host2:7553/eureka/"
    export ZWE_zowe_network_client_tls_attls="true"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ZWE_DISCOVERY_SERVICES_LIST" = "http://host1:7553/eureka/,http://host2:7553/eureka/" ]
}

@test "apiml-common-scripts: sets server_protocol to TLS" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$server_protocol" = "TLS" ]
}

@test "apiml-common-scripts: sets default keystore_type to PKCS12" {
    unset ZWE_configs_certificate_keystore_type
    unset ZWE_zowe_certificate_keystore_type

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_type" = "PKCS12" ]
}

@test "apiml-common-scripts: uses ZWE_configs_certificate_keystore_type when set" {
    export ZWE_configs_certificate_keystore_type="JCERACFKS"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_type" = "JCERACFKS" ]
}

@test "apiml-common-scripts: sets keystore_pass from environment" {
    export ZWE_zowe_certificate_keystore_password="mypassword"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_pass" = "mypassword" ]
}

@test "apiml-common-scripts: sets key_alias from environment" {
    export ZWE_zowe_certificate_keystore_alias="myalias"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$key_alias" = "myalias" ]
}

@test "apiml-common-scripts: sets keystore_location from environment" {
    export ZWE_zowe_certificate_keystore_file="/path/to/keystore.p12"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_location" = "/path/to/keystore.p12" ]
}

@test "apiml-common-scripts: sets truststore_location from environment" {
    export ZWE_zowe_certificate_truststore_file="/path/to/truststore.p12"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$truststore_location" = "/path/to/truststore.p12" ]
}

@test "apiml-common-scripts: transforms JCERACFKS keystore location" {
    export ZWE_configs_certificate_keystore_type="JCERACFKS"
    export ZWE_zowe_certificate_keystore_file="safkeyring://USERID/RING"
    export ZWE_zowe_certificate_truststore_file="safkeyring://USERID/RING"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_location" = "safkeyringjce://USERID/RING" ]
    [ "$truststore_location" = "safkeyringjce://USERID/RING" ]
}

@test "apiml-common-scripts: transforms JCECCARACFKS keystore location" {
    export ZWE_configs_certificate_keystore_type="JCECCARACFKS"
    export ZWE_zowe_certificate_keystore_file="safkeyring://USERID/RING"
    export ZWE_zowe_certificate_truststore_file="safkeyring://USERID/RING"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_location" = "safkeyringjcecca://USERID/RING" ]
    [ "$truststore_location" = "safkeyringjcecca://USERID/RING" ]
}

@test "apiml-common-scripts: transforms JCEHYBRIDRACFKS keystore location" {
    export ZWE_configs_certificate_keystore_type="JCEHYBRIDRACFKS"
    export ZWE_zowe_certificate_keystore_file="safkeyring://USERID/RING"
    export ZWE_zowe_certificate_truststore_file="safkeyring://USERID/RING"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_location" = "safkeyringjcehybrid://USERID/RING" ]
    [ "$truststore_location" = "safkeyringjcehybrid://USERID/RING" ]
}

@test "apiml-common-scripts: does not transform PKCS12 keystore location" {
    export ZWE_configs_certificate_keystore_type="PKCS12"
    export ZWE_zowe_certificate_keystore_file="/path/to/keystore.p12"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$keystore_location" = "/path/to/keystore.p12" ]
}

@test "apiml-common-scripts: JVM_SECURITY_PROPERTIES empty by default" {
    unset JVM_SECURITY_PROPERTIES_OVERRIDE

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ -z "$JVM_SECURITY_PROPERTIES" ]
}

@test "apiml-common-scripts: JVM_SECURITY_PROPERTIES set when override enabled" {
    export JVM_SECURITY_PROPERTIES_OVERRIDE="true"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$JVM_SECURITY_PROPERTIES" = "-Djava.security.properties=../apiml-common-lib/bin/jvm.security.override.properties" ]
}

@test "apiml-common-scripts: LOGBACK empty by default" {
    unset ZWE_configs_logging_config

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ -z "$LOGBACK" ]
}

@test "apiml-common-scripts: LOGBACK set when logging config provided" {
    export ZWE_configs_logging_config="/path/to/logback.xml"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$LOGBACK" = "-Dlogging.config=/path/to/logback.xml" ]
}

@test "apiml-common-scripts: JAVA_BIN_DIR set from ZWE_java_home" {
    export ZWE_java_home="/custom/java"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$JAVA_BIN_DIR" = "/custom/java/bin/" ]
}

@test "apiml-common-scripts: sets SHARED_CLASSES_OPTS and QUICK_START when runs on zOS" {
    uname() {
        echo "OS/390"
    }
    export -f uname
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"
    [ "$QUICK_START" = "-Xquickstart" ]
    [ "$SHARED_CLASSES_OPTS" = "-Xshareclasses:name=apiml_shared_classes,nonfatal" ]
}

@test "apiml-common-scripts: sets ADD_OPENS for Java modules" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [[ "$ADD_OPENS" == *"--add-opens=java.base/java.lang=ALL-UNNAMED"* ]]
    [[ "$ADD_OPENS" == *"--add-opens=java.base/java.util=ALL-UNNAMED"* ]]
    [[ "$ADD_OPENS" == *"--add-opens=java.base/javax.net.ssl=ALL-UNNAMED"* ]]
}

@test "apiml-common-scripts: sets LIBPATH with Java paths" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [[ "$LIBPATH" == *"/lib"* ]]
    [[ "$LIBPATH" == *"/usr/lib"* ]]
    [[ "$LIBPATH" == *"${JAVA_HOME}/bin"* ]]
}

@test "apiml-common-scripts: sets default truststore_type to PKCS12" {
    unset ZWE_configs_certificate_truststore_type
    unset ZWE_zowe_certificate_truststore_type

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$truststore_type" = "PKCS12" ]
}

@test "apiml-common-scripts: sets ZOWE_CONSOLE_LOG_CHARSET to UTF-8 on non-z/OS" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$ZOWE_CONSOLE_LOG_CHARSET" = "UTF-8" ]
}

@test "apiml-common-scripts: add_profile function is defined and works" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    # Test that add_profile function exists
    type add_profile

    # Test adding first profile
    unset ZWE_configs_spring_profiles_active
    add_profile "debug"
    [ "$ZWE_configs_spring_profiles_active" = "debug" ]

    # Test adding second profile
    add_profile "attls"
    [ "$ZWE_configs_spring_profiles_active" = "debug,attls" ]
}

@test "apiml-common-scripts: sets DEFAULT_CIPHERS" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [[ "$DEFAULT_CIPHERS" == *"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"* ]]
    [[ "$DEFAULT_CIPHERS" == *"TLS_AES_128_GCM_SHA256"* ]]
}

@test "apiml-common-scripts: sets server_ciphers from DEFAULT_CIPHERS" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$server_ciphers" = "$DEFAULT_CIPHERS" ]
}

@test "apiml-common-scripts: sets client_ciphers from server_ciphers by default" {
    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$client_ciphers" = "$server_ciphers" ]
}


@test "apiml-common-scripts: key_pass defaults to keystore_pass" {
    export ZWE_zowe_certificate_keystore_password="keystorepass"
    unset ZWE_configs_certificate_key_password
    unset ZWE_zowe_certificate_key_password

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$key_pass" = "keystorepass" ]
}

@test "apiml-common-scripts: key_pass can be set separately" {
    export ZWE_zowe_certificate_keystore_password="keystorepass"
    export ZWE_zowe_certificate_key_password="keypass"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ "$key_pass" = "keypass" ]
}

@test "apiml-common-scripts: clears keyring values when AT-TLS with APIML_ATTLS_LOAD_KEYRING" {
    export ZWE_zowe_network_server_tls_attls="true"
    export APIML_ATTLS_LOAD_KEYRING="true"
    export ZWE_zowe_certificate_keystore_file="/path/to/keystore"
    export ZWE_zowe_certificate_keystore_password="password"

    . "${SCRIPTS_DIR}/apiml-common-scripts.sh"

    [ -z "$keystore_type" ]
    [ -z "$keystore_pass" ]
    [ -z "$key_pass" ]
    [ -z "$key_alias" ]
    [ -z "$keystore_location" ]
}
