#!/usr/bin/env bats

################################################################################
# BATS tests for ZAAS start.sh
# Run from the repository root: bats zaas-package/src/test/start_test.bats
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

setup() {
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../../.." && pwd)"
    ZAAS_START="${PROJECT_ROOT}/zaas-package/src/main/resources/bin/start.sh"
    TEST_TEMP_DIR="$(mktemp -d)"
    mkdir -p "${TEST_TEMP_DIR}/java/bin"
    cat > "${TEST_TEMP_DIR}/java/bin/java" << 'MOCK_JAVA'
#!/bin/sh
printf '%s\n' "$@"
MOCK_JAVA
    chmod +x "${TEST_TEMP_DIR}/java/bin/java"
    cd "${PROJECT_ROOT}"
}

teardown() {
    rm -rf "${TEST_TEMP_DIR}"
}

assert_cookie_name() {
    expected_cookie_name=$1
    shift
    run env -i PATH="${PATH}" \
        LAUNCH_COMPONENT="${TEST_TEMP_DIR}" \
        ZWE_java_home="${TEST_TEMP_DIR}/java" \
        ZWE_zowe_cookieIdentifier="instance-A" \
        "$@" "${ZAAS_START}"

    [ "$status" -eq 0 ]
    cookie_argument=$(printf '%s\n' "$output" | sed -n '/^-Dapiml\.security\.auth\.cookieProperties\.cookieName=/p')
    [ "$cookie_argument" = "-Dapiml.security.auth.cookieProperties.cookieName=${expected_cookie_name}" ]
}

@test "zaas-start: defaults to the shared cookie name" {
    assert_cookie_name apimlAuthenticationToken
}

@test "zaas-start: inherits Gateway's unique cookie setting" {
    assert_cookie_name apimlAuthenticationToken.instance-A \
        ZWE_components_gateway_apiml_security_auth_uniqueCookie=true
}

@test "zaas-start: inherits Gateway's disabled unique cookie setting" {
    assert_cookie_name apimlAuthenticationToken \
        ZWE_components_gateway_apiml_security_auth_uniqueCookie=false
}

@test "zaas-start: explicit ZAAS false overrides Gateway true" {
    assert_cookie_name apimlAuthenticationToken \
        ZWE_components_gateway_apiml_security_auth_uniqueCookie=true \
        ZWE_configs_apiml_security_auth_uniqueCookie=false
}

@test "zaas-start: explicit ZAAS true overrides Gateway false" {
    assert_cookie_name apimlAuthenticationToken.instance-A \
        ZWE_components_gateway_apiml_security_auth_uniqueCookie=false \
        ZWE_configs_apiml_security_auth_uniqueCookie=true
}

@test "zaas-start: empty ZAAS setting inherits Gateway true" {
    assert_cookie_name apimlAuthenticationToken.instance-A \
        ZWE_components_gateway_apiml_security_auth_uniqueCookie=true \
        ZWE_configs_apiml_security_auth_uniqueCookie=
}
