#!/usr/bin/env bats

################################################################################
# BATS tests for Gateway start.sh
# Run from the repository root: bats gateway-package/src/test/start_test.bats
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

setup() {
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../../.." && pwd)"
    GATEWAY_START="${PROJECT_ROOT}/gateway-package/src/main/resources/bin/start.sh"
    TEST_TEMP_DIR="$(mktemp -d)"
    mkdir -p "${TEST_TEMP_DIR}/java/bin"
    cat > "${TEST_TEMP_DIR}/java/bin/java" << 'MOCK_JAVA'
#!/bin/sh
printf '%s\n' "$@" > "${JAVA_ARGUMENTS_FILE}"
printf '%s' "${APIML_SECURITY_FORWARDHEADER_TRUSTEDPROXIES:-}" > "${JAVA_ARGUMENTS_FILE}.environment"
MOCK_JAVA
    chmod +x "${TEST_TEMP_DIR}/java/bin/java"
    cd "${PROJECT_ROOT}"
}

teardown() {
    rm -rf "${TEST_TEMP_DIR}"
}

run_trusted_proxy_startup() {
    run env -i PATH="${PATH}" \
        LAUNCH_COMPONENT="${TEST_TEMP_DIR}" \
        ZWE_java_home="${TEST_TEMP_DIR}/java" \
        JAVA_ARGUMENTS_FILE="${TEST_TEMP_DIR}/java-arguments" \
        "$@" "${GATEWAY_START}"
    [ "$status" -eq 0 ]
    trusted_proxy_arguments=$(sed -n '/^-Dapiml\.security\.forwardHeader\.trustedProxies=/p' "${TEST_TEMP_DIR}/java-arguments")
}

@test "gateway-start: trusted proxies regex with spaces is passed as one argument" {
    regex='(?x) ^ (10\.0\.0\.[0-9]+ | proxy[.]example) $'
    run_trusted_proxy_startup "ZWE_configs_apiml_security_forwardHeader_trustedProxies=${regex}"
    [ "${trusted_proxy_arguments}" = "-Dapiml.security.forwardHeader.trustedProxies=${regex}" ]
}

@test "gateway-start: absent trusted proxies do not override application defaults" {
    run_trusted_proxy_startup
    [ -z "${trusted_proxy_arguments}" ]
}

@test "gateway-start: empty trusted proxies do not override application defaults" {
    run_trusted_proxy_startup ZWE_configs_apiml_security_forwardHeader_trustedProxies=
    [ -z "${trusted_proxy_arguments}" ]
}

@test "gateway-start: trusted proxies preserve environment-only configuration" {
    regex='^10\.0\.0\.[0-9]+$'
    run_trusted_proxy_startup "APIML_SECURITY_FORWARDHEADER_TRUSTEDPROXIES=${regex}"
    [ -z "${trusted_proxy_arguments}" ]
    [ "$(cat "${TEST_TEMP_DIR}/java-arguments.environment")" = "${regex}" ]
}

@test "gateway-start: trusted proxies preserve JVM-only configuration" {
    regex='10[.]0[.]0[.][0-9]+'
    run_trusted_proxy_startup "ZWE_configs_jvm_Dapiml_security_forwardHeader_trustedProxies=${regex}"
    [ "${trusted_proxy_arguments}" = "-Dapiml.security.forwardHeader.trustedProxies=${regex}" ]
}
