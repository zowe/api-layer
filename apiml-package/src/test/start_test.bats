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
#!/bin/bash
# bash required to safely capture arguments
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

# Capture the actual arguments supplied to Java without inheriting local configuration.
capture_mapping_arguments() {
    cat > "${ZWE_java_home}/bin/java" << 'MOCK_JAVA'
#!/bin/sh
printf '%s\n' "$@" > "${JAVA_ARGUMENTS_FILE}"
MOCK_JAVA
    cd "${PROJECT_ROOT}"
    run env -i PATH="${PATH}" \
        JAVA_ARGUMENTS_FILE="${TEST_TEMP_DIR}/java-arguments" \
        LAUNCH_COMPONENT="${TEST_TEMP_DIR}" \
        ZWE_java_home="${ZWE_java_home}" \
        "$@" "${APIML_DIR}/start.sh"
    [ "$status" -eq 0 ]
    [ -f "${TEST_TEMP_DIR}/java-arguments" ]
}

assert_mapping_argument() {
    grep -Fxq -- "-D$1=$2" "${TEST_TEMP_DIR}/java-arguments"
}

catalog_settings() {
    local prefix="$1"
    CATALOG_SETTINGS=(
        "${prefix}_customStyle_backgroundColor=#003366"
        "${prefix}_customStyle_docLink=https://example.org/docs"
        "${prefix}_customStyle_fontFamily=Arial"
        "${prefix}_customStyle_headerColor=#112233"
        "${prefix}_customStyle_logo=/brand/logo.svg"
        "${prefix}_customStyle_textColor=#445566"
        "${prefix}_customStyle_titlesColor=#778899"
        "${prefix}_hide_serviceInfo=true"
    )
}

assert_catalog_settings() {
    assert_mapping_argument apiml.catalog.customStyle.backgroundColor '#003366'
    assert_mapping_argument apiml.catalog.customStyle.docLink https://example.org/docs
    assert_mapping_argument apiml.catalog.customStyle.fontFamily Arial
    assert_mapping_argument apiml.catalog.customStyle.headerColor '#112233'
    assert_mapping_argument apiml.catalog.customStyle.logo /brand/logo.svg
    assert_mapping_argument apiml.catalog.customStyle.textColor '#445566'
    assert_mapping_argument apiml.catalog.customStyle.titlesColor '#778899'
    assert_mapping_argument apiml.catalog.hide.serviceInfo true
}

@test "apiml-start: Catalog mappings use the api-catalog component settings" {
    catalog_settings ZWE_components_api_catalog_apiml_catalog
    capture_mapping_arguments "${CATALOG_SETTINGS[@]}"
    assert_catalog_settings
}

@test "apiml-start: Catalog mappings fall back to APIML settings" {
    catalog_settings ZWE_configs_apiml_catalog
    capture_mapping_arguments "${CATALOG_SETTINGS[@]}"
    assert_catalog_settings
}

@test "apiml-start: Catalog component mappings override APIML settings including explicit false" {
    catalog_settings ZWE_components_api_catalog_apiml_catalog
    capture_mapping_arguments "${CATALOG_SETTINGS[@]}" \
        ZWE_configs_apiml_catalog_customStyle_logo=/fallback/logo.svg \
        ZWE_configs_apiml_catalog_hide_serviceInfo=true \
        ZWE_components_api_catalog_apiml_catalog_hide_serviceInfo=false
    assert_mapping_argument apiml.catalog.customStyle.logo /brand/logo.svg
    assert_mapping_argument apiml.catalog.hide.serviceInfo false
}

@test "apiml-start: registry mappings use Gateway component settings" {
    capture_mapping_arguments \
        ZWE_components_gateway_apiml_gateway_registry_maxSimultaneousRequests=73 \
        ZWE_components_gateway_apiml_gateway_registry_refreshIntervalMs=9876 \
        ZWE_components_gateway_apiml_gateway_registry_metadataKeyAllowList=audit.key,example.key
    assert_mapping_argument apiml.gateway.maxSimultaneousRequests 73
    assert_mapping_argument apiml.gateway.refresh-interval-ms 9876
    assert_mapping_argument apiml.gateway.registry.metadata-key-allow-list audit.key,example.key
}

@test "apiml-start: registry mappings fall back to APIML settings" {
    capture_mapping_arguments \
        ZWE_configs_apiml_gateway_registry_maxSimultaneousRequests=51 \
        ZWE_configs_apiml_gateway_registry_refreshIntervalMs=8765 \
        ZWE_configs_apiml_gateway_registry_metadataKeyAllowList=apiml.key
    assert_mapping_argument apiml.gateway.maxSimultaneousRequests 51
    assert_mapping_argument apiml.gateway.refresh-interval-ms 8765
    assert_mapping_argument apiml.gateway.registry.metadata-key-allow-list apiml.key
}

@test "apiml-start: Gateway registry mappings take precedence over APIML settings" {
    capture_mapping_arguments \
        ZWE_components_gateway_apiml_gateway_registry_maxSimultaneousRequests=73 \
        ZWE_components_gateway_apiml_gateway_registry_refreshIntervalMs=9876 \
        ZWE_components_gateway_apiml_gateway_registry_metadataKeyAllowList=gateway.key \
        ZWE_configs_apiml_gateway_registry_maxSimultaneousRequests=51 \
        ZWE_configs_apiml_gateway_registry_refreshIntervalMs=8765 \
        ZWE_configs_apiml_gateway_registry_metadataKeyAllowList=apiml.key
    assert_mapping_argument apiml.gateway.maxSimultaneousRequests 73
    assert_mapping_argument apiml.gateway.refresh-interval-ms 9876
    assert_mapping_argument apiml.gateway.registry.metadata-key-allow-list gateway.key
}

@test "apiml-start: Catalog and registry mappings retain defaults when unset" {
    capture_mapping_arguments
    for setting in backgroundColor docLink fontFamily headerColor logo textColor titlesColor; do
        assert_mapping_argument "apiml.catalog.customStyle.${setting}" ''
    done
    assert_mapping_argument apiml.catalog.hide.serviceInfo false
    assert_mapping_argument apiml.gateway.maxSimultaneousRequests 20
    assert_mapping_argument apiml.gateway.refresh-interval-ms 30000
    assert_mapping_argument apiml.gateway.registry.metadata-key-allow-list ''
}

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

@test "apiml-start: When apiml.logging.logging.toFile.enabled=true, then the property is set and no message about debugOnly" {
    export ZWE_configs_logging_toFile_enabled=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "false" ]

    [[ ! "$output" =~ "APIML logging debug only to file was enabled, but logging to file disabled. The option is ignored." ]]
}

@test "apiml-start: When When apiml.logging.logging.toFile.enabled=false, then property is set and no message about debugOnly" {
    export ZWE_configs_logging_toFile_enabled=false

    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${logToFile}" = "false" ]
    [ "${onlyDebugToFile}" = "false" ]

    [[ ! "$output" =~ "APIML logging debug only to file was enabled, but logging to file disabled. The option is ignored." ]]
}

@test "apiml-start: When When apiml.logging.logging.toFile.debugOnly=true, then print a message and disable both" {
    export ZWE_configs_logging_toFile_debugOnly=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${logToFile}" = "false" ]
    [ "${onlyDebugToFile}" = "false" ]

    [[ "$output" =~ "APIML logging debug only to file was enabled, but logging to file disabled. The option is ignored." ]]
}

@test "apiml-start: When apiml logging to file is enabled and debugOnly enabled, then no message about debugOnly" {
    export ZWE_configs_logging_toFile_enabled=true
    export ZWE_configs_logging_toFile_debugOnly=true
    run "${APIML_DIR}/start.sh"

    [ "$status" -eq 0 ]

    logToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.enabled=\([^ "]*\).*/\1/p')
    onlyDebugToFile=$(printf '%s\n' "$output" | sed -n 's/.*-Dapiml\.logging\.toFile\.debugOnly=\([^ "]*\).*/\1/p')

    [ "${logToFile}" = "true" ]
    [ "${onlyDebugToFile}" = "true" ]

    [[ ! "$output" =~ "APIML logging debug only to file was enabled, but logging to file disabled. The option is ignored." ]]
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
