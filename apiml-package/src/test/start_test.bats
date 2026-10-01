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
    LAUNCH_COMPONENT="${PROJECT_ROOT}/apiml-package"
#    # Create a temporary directory for test artifacts
#    TEST_TEMP_DIR="$(mktemp -d)"
#
#    # Set up minimal required environment variables for sourcing scripts
#    export JAVA_HOME="${TEST_TEMP_DIR}/java"
#    mkdir -p "${JAVA_HOME}/bin"

    # Create a mock java binary that always return success
    java() {
        return 0
      }
    export -f java

##    export ZWE_zowe_workspaceDirectory="${TEST_TEMP_DIR}/workspace"
##    export ZWE_zowe_runtimeDirectory="${TEST_TEMP_DIR}/runtime"
##    export ZWE_zowe_logDirectory="${TEST_TEMP_DIR}/logs"
##    export ZWE_STATIC_DEFINITIONS_DIR="${TEST_TEMP_DIR}/static-defs"
##    export ZWE_zowe_certificate_keystore_file="${TEST_TEMP_DIR}/keystore.p12"
##    export ZWE_zowe_certificate_keystore_password="password"
##    export ZWE_zowe_certificate_keystore_alias="localhost"
##    export ZWE_zowe_certificate_truststore_file="${TEST_TEMP_DIR}/truststore.p12"
##    export ZWE_zowe_certificate_truststore_password="password"
##    export ZWE_zowe_externalDomains_0="localhost"
##    export ZWE_zowe_externalPort="7554"
##    export ZWE_zowe_job_prefix="ZWE"
##
##    # Create necessary directories
##    mkdir -p "${ZWE_zowe_workspaceDirectory}"
##    mkdir -p "${ZWE_zowe_runtimeDirectory}"
##    mkdir -p "${ZWE_zowe_logDirectory}"
}
#
## Teardown function runs after each test
#teardown() {
#    # Clean up temporary directory
#    if [ -n "${TEST_TEMP_DIR}" ] && [ -d "${TEST_TEMP_DIR}" ]; then
#        rm -rf "${TEST_TEMP_DIR}"
#    fi
#
#    # Unset test environment variables
#    unset ZWE_configs_jvm_Xss
#    unset ZWE_configs_jvm_Xmn
#    unset ZWE_configs_jvm_XX_UseG1GC
#    unset ZWE_configs_jvm_XX_MaxGCPauseMillis
#    unset ZWE_configs_jvm_Dmy_custom_property
#    unset ZWE_configs_jvm_Denable_feature
#    unset ZWE_configs_spring_profiles_active
#    unset ZWE_configs_debug
#    unset ZWE_components_gateway_debug
#    unset ZWE_zowe_verifyCertificates
#    unset ZWE_zowe_network_server_tls_attls
#    unset ZWE_zowe_network_client_tls_attls
#    unset ATTLS_SERVER_ENABLED
#    unset ATTLS_CLIENT_ENABLED
#    unset CMMN_LB
#    unset LIBRARY_PATH
#    unset JVM_SECURITY_PROPERTIES_OVERRIDE
#    unset ZWE_configs_logging_config
#    unset ZWE_java_home
#    unset ZWE_haInstance_hostname
#    unset ZWE_components_discovery_port
#    unset ZWE_DISCOVERY_SERVICES_LIST
#    unset ZWE_configs_certificate_keystore_type
#    unset ZWE_zowe_certificate_keystore_type
#    unset ZWE_configs_certificate_keystore_file
#    unset ZWE_configs_certificate_truststore_file
#}

################################################################################
# Tests for start.sh
################################################################################

@test "start: sets APIML CODE" {
    . "${APIML_DIR}/start.sh"

    [ "$APIML_CODE" = "AG" ]
}
