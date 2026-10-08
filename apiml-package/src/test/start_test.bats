#!/usr/bin/env bats

################################################################################
# BATS tests for APIML start.sh and related scripts
# Check README.MD in PROJECT_ROOT}/scripts
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
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../../../" && pwd)"
    APIML_DIR="${PROJECT_ROOT}/apiml-package/src/main/resources/bin"

    # Create a temporary directory for test artifacts
    TEST_TEMP_DIR="$(mktemp -d)"

    export LAUNCH_COMPONENT="${TEST_TEMP_DIR}"

    # Set up minimal required environment variables for sourcing scripts
    export ZWE_java_home="${TEST_TEMP_DIR}/java"
    mkdir -p "${ZWE_java_home}/bin"

    # Create a mock java binary that returns version info
    cat > "${ZWE_java_home}/bin/java" << 'MOCK_JAVAP'
#!/bin/sh
FULL_COMMAND=$(printf '%q ' "$0" "$@")

echo "Java command executed: $FULL_COMMAND"
MOCK_JAVAP
    chmod +x "${ZWE_java_home}/bin/java"

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
    unset ATTLS_SERVER_ENABLED
    unset ATTLS_CLIENT_ENABLED
    unset CMMN_LB
    unset JVM_SECURITY_PROPERTIES_OVERRIDE
    unset LIBRARY_PATH
    unset ZWE_components_discovery_port
    unset ZWE_components_gateway_debug
    unset ZWE_configs_certificate_keystore_file
    unset ZWE_configs_certificate_keystore_type
    unset ZWE_configs_certificate_truststore_file
    unset ZWE_configs_debug
    unset ZWE_configs_jvm_Xss
    unset ZWE_configs_jvm_Xmn
    unset ZWE_configs_jvm_XX_UseG1GC
    unset ZWE_configs_jvm_XX_MaxGCPauseMillis
    unset ZWE_configs_jvm_Dmy_custom_property
    unset ZWE_configs_jvm_Denable_feature
    unset ZWE_configs_logging_debug
    unset ZWE_configs_logging_debug_gateway
    unset ZWE_configs_logging_debug_authentication
    unset ZWE_configs_logging_debug_discovery
    unset ZWE_configs_logging_debug_caching
    unset ZWE_configs_logging_debug_catalog
    unset ZWE_configs_logging_debug_wiretap
    unset ZWE_configs_logging_toFile_enabled
    unset ZWE_configs_logging_toFile_debugOnly
    unset ZWE_configs_spring_profiles_active
    unset ZWE_DISCOVERY_SERVICES_LIST
    unset ZWE_haInstance_hostname
    unset ZWE_java_home
    unset ZWE_zowe_certificate_keystore_type
    unset ZWE_zowe_verifyCertificates
    unset ZWE_zowe_network_server_tls_attls
    unset ZWE_zowe_network_client_tls_attls
}

################################################################################
# Tests for start.sh
################################################################################

@test "apiml-start: When apiml.debug=true, then debug profile is active and logging to file enabled" {
    export ZWE_configs_debug=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.debug=true, then feature level debug is overridden" {
    export ZWE_configs_debug=true
    export ZWE_configs_logging_debug_gateway=true
    export ZWE_configs_logging_debug_authentication=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.debug=false, then feature level debug is enabled" {
    export ZWE_configs_logging_debug_gateway=true
    export ZWE_configs_logging_debug_authentication=true
    export ZWE_configs_logging_debug_discovery=true
    export ZWE_configs_logging_debug_caching=true
    export ZWE_configs_logging_debug_catalog=true
    export ZWE_configs_logging_debug_wiretap=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-gateway,debug-discovery,debug-authentication,debug-caching,debug-catalog,debug-wiretap" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.debug is unset, then feature level debug is enabled" {
    export ZWE_configs_logging_debug_gateway=true
    export ZWE_configs_logging_debug_authentication=true
    export ZWE_configs_logging_debug_discovery=true
    export ZWE_configs_logging_debug_caching=true
    export ZWE_configs_logging_debug_catalog=true
    export ZWE_configs_logging_debug_wiretap=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-gateway,debug-discovery,debug-authentication,debug-caching,debug-catalog,debug-wiretap" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.debug.gateway=true, then gateway debug is enabled" {
    export ZWE_configs_logging_debug_gateway=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-gateway" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.debug.discovery=true, then discovery debug is enabled" {
    export ZWE_configs_logging_debug_discovery=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-discovery" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.debug.authentication=true, then authentication debug is enabled" {
    export ZWE_configs_logging_debug_authentication=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-authentication" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.debug.caching=true, then caching debug is enabled" {
    export ZWE_configs_logging_debug_caching=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-caching" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.debug.catalog=true, then catalog debug is enabled" {
    export ZWE_configs_logging_debug_catalog=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-catalog" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.debug combination is enabled, then the features level debug are enabled" {
    export ZWE_configs_logging_debug_gateway=false
    export ZWE_configs_logging_debug_authentication=true
    export ZWE_configs_logging_debug_discovery=false
    export ZWE_configs_logging_debug_caching=true
    export ZWE_configs_logging_debug_catalog=false
    export ZWE_configs_logging_debug_wiretap=false
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-authentication,debug-caching" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.toFile.enabled=true, then the property is set" {
    export ZWE_configs_logging_toFile_enabled=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: When apiml.logging.logging.toFile.debugOnly=true, then the property is set" {
    export ZWE_configs_logging_toFile_debugOnly=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${logToFile}" = "false" ]
    [ "${onlyDebugToFile}" = "true" ]
}

@test "apiml-start: Default configuration: info, no file" {
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info" ]
    [ "${logToFile}" = "false" ]
    [ "${onlyDebugToFile}" = "false" ]
}

@test "apiml-start: Common configuration: debug enabled with logging debug to file only" {
    export ZWE_configs_debug=true
    export ZWE_configs_logging_toFile_debugOnly=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "true" ]
}

@test "apiml-start: Common configuration: gateway and authentication debug with logging debug to file only" {
    export ZWE_configs_logging_debug_gateway=true
    export ZWE_configs_logging_debug_authentication=true
    export ZWE_configs_logging_toFile_debugOnly=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${profiles}" = "info,debug-common,debug-gateway,debug-authentication" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "true" ]
}

@test "apiml-start: When apiml.debug not set and single logger level is overridden, then property is set" {
    export ZWE_configs_logging_loggerLevels=org.zowe.apiml.security.HttpsFactory=TRACE
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')
    loggersLevel=$(printf '%s\n' "$output" | tr ' ' '\n' | grep -- '-Dlogging.level' | tr '\n' ' ')

    [ "${profiles}" = "info" ]
    [ "${logToFile}" = "false" ]
    [ "${onlyDebugToFile}" = "false" ]
    [ "${loggersLevel}" = "-Dlogging.level.org.zowe.apiml.security.HttpsFactory=TRACE " ]
}

@test "apiml-start: When apiml.debug not set and multiple logger level is overridden, then property is set" {
    export ZWE_configs_logging_loggerLevels=org.zowe.apiml.security.HttpsFactory=TRACE,org.zowe.apiml.security.HttpsFactory=ERROR,org.apache.http=INFO
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')
    loggersLevel=$(printf '%s\n' "$output" | tr ' ' '\n' | grep -- '-Dlogging.level' | tr '\n' ' ')

    [ "${profiles}" = "info" ]
    [ "${logToFile}" = "false" ]
    [ "${onlyDebugToFile}" = "false" ]
    [ "${loggersLevel}" = "-Dlogging.level.org.zowe.apiml.security.HttpsFactory=TRACE -Dlogging.level.org.zowe.apiml.security.HttpsFactory=ERROR -Dlogging.level.org.apache.http=INFO " ]
}

@test "apiml-start: When apiml.debug=true and multiple logger level is overridden, then property is set" {
    export ZWE_configs_debug=true
    export ZWE_configs_logging_loggerLevels=org.zowe.apiml.security.HttpsFactory=TRACE,org.zowe.apiml.security.HttpsFactory=ERROR,org.apache.http=INFO
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    profiles=$(printf '%s\n' "$output" | sed -n 's/.*-Dspring\.profiles\.active=\([^ "]*\).*/\1/p' | sed 's/\\,/,/g')
    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')
    loggersLevel=$(printf '%s\n' "$output" | tr ' ' '\n' | grep -- '-Dlogging.level' | tr '\n' ' ')

    [ "${profiles}" = "info,debug" ]
    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]
    [ "${loggersLevel}" = "-Dlogging.level.org.zowe.apiml.security.HttpsFactory=TRACE -Dlogging.level.org.zowe.apiml.security.HttpsFactory=ERROR -Dlogging.level.org.apache.http=INFO " ]
}
