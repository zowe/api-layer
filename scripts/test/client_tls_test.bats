#!/usr/bin/env bats

# SPDX-License-Identifier: EPL-2.0
# Exercise the real launchers with a mock Java executable; no TLS material is loaded.

setup() {
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../.." && pwd)"
    TEST_TEMP_DIR="$(mktemp -d)"
    mkdir -p "${TEST_TEMP_DIR}/java/bin" "${TEST_TEMP_DIR}/workspace" \
        "${TEST_TEMP_DIR}/runtime" "${TEST_TEMP_DIR}/logs"
    cat > "${TEST_TEMP_DIR}/java/bin/java" << 'MOCK_JAVA'
#!/bin/sh
printf '%s\n' "$@" > "${JAVA_ARGUMENTS_FILE}"
MOCK_JAVA
    chmod +x "${TEST_TEMP_DIR}/java/bin/java"
    COMPONENTS=(gateway zaas discovery api-catalog caching-service apiml)
    cd "${PROJECT_ROOT}"
}

teardown() {
    rm -rf "${TEST_TEMP_DIR}"
}

capture_arguments() {
    local component="$1"
    shift
    : > "${TEST_TEMP_DIR}/java-arguments"
    run env -i PATH="${PATH}" \
        JAVA_ARGUMENTS_FILE="${TEST_TEMP_DIR}/java-arguments" \
        LAUNCH_COMPONENT="${TEST_TEMP_DIR}" \
        ZWE_java_home="${TEST_TEMP_DIR}/java" \
        ZWE_zowe_workspaceDirectory="${TEST_TEMP_DIR}/workspace" \
        ZWE_zowe_runtimeDirectory="${TEST_TEMP_DIR}/runtime" \
        ZWE_zowe_logDirectory="${TEST_TEMP_DIR}/logs" \
        ZWE_zowe_certificate_keystore_file="${TEST_TEMP_DIR}/server-keystore.p12" \
        ZWE_zowe_certificate_keystore_alias=server-alias \
        ZWE_zowe_certificate_keystore_password=server-keystore-password \
        ZWE_zowe_certificate_key_password=server-key-password \
        ZWE_zowe_certificate_truststore_file="${TEST_TEMP_DIR}/server-truststore.p12" \
        ZWE_zowe_certificate_truststore_password=server-truststore-password \
        ZWE_zowe_externalDomains_0=localhost \
        ZWE_zowe_externalPort=7554 \
        ZWE_zowe_job_prefix=TEST \
        "$@" /bin/sh "${PROJECT_ROOT}/${component}-package/src/main/resources/bin/start.sh"
    [ "$status" -eq 0 ]
    [ -f "${TEST_TEMP_DIR}/java-arguments" ]
}

assert_property() {
    grep -Fxq -- "-D$1=$2" "${TEST_TEMP_DIR}/java-arguments"
}

assert_client_configuration() {
    assert_property apiml.service.ssl.key-alias "$1"
    assert_property apiml.service.ssl.key-password "$2"
    assert_property apiml.service.ssl.key-store "$3"
    assert_property apiml.service.ssl.key-store-type "$4"
    assert_property apiml.service.ssl.key-store-password "$5"
    assert_property apiml.service.ssl.trust-store "$6"
    assert_property apiml.service.ssl.trust-store-type "$7"
    assert_property apiml.service.ssl.trust-store-password "$8"
}

set_canonical_configuration() {
    CANONICAL_CONFIGURATION=(
        ZWE_configs_apiml_service_ssl_key_alias=client-alias
        "ZWE_configs_apiml_service_ssl_key_password=client key password"
        "ZWE_configs_apiml_service_ssl_key_store=${TEST_TEMP_DIR}/client keystore.jks"
        ZWE_configs_apiml_service_ssl_key_store_type=JKS
        "ZWE_configs_apiml_service_ssl_key_store_password=client keystore password"
        "ZWE_configs_apiml_service_ssl_trust_store=${TEST_TEMP_DIR}/client truststore.jks"
        ZWE_configs_apiml_service_ssl_trust_store_type=JKS
        "ZWE_configs_apiml_service_ssl_trust_store_password=client truststore password"
    )
}

set_legacy_configuration() {
    LEGACY_CONFIGURATION=(
        ZWE_configs_apiml_service_ssl_keystore_alias=legacy-alias
        "ZWE_configs_apiml_service_ssl_key_password=legacy key password"
        "ZWE_configs_apiml_service_ssl_keystore_file=${TEST_TEMP_DIR}/legacy keystore.jks"
        ZWE_configs_apiml_service_ssl_keystore_type=PKCS12
        "ZWE_configs_apiml_service_ssl_keystore_password=legacy keystore password"
        "ZWE_configs_apiml_service_ssl_truststore_file=${TEST_TEMP_DIR}/legacy truststore.jks"
        ZWE_configs_apiml_service_ssl_truststore_type=PKCS12
        "ZWE_configs_apiml_service_ssl_truststore_password=legacy truststore password"
    )
}

@test "client TLS: all launchers honor schema-declared settings without changing server identity" {
    set_canonical_configuration
    for component in "${COMPONENTS[@]}"; do
        capture_arguments "$component" "${CANONICAL_CONFIGURATION[@]}"
        assert_client_configuration client-alias "client key password" \
            "${TEST_TEMP_DIR}/client keystore.jks" JKS "client keystore password" \
            "${TEST_TEMP_DIR}/client truststore.jks" JKS "client truststore password"
        assert_property server.ssl.keyAlias server-alias
        assert_property server.ssl.keyStore "${TEST_TEMP_DIR}/server-keystore.p12"
        assert_property server.ssl.trustStore "${TEST_TEMP_DIR}/server-truststore.p12"
    done
}

@test "client TLS: all launchers retain legacy settings" {
    set_legacy_configuration
    for component in "${COMPONENTS[@]}"; do
        capture_arguments "$component" "${LEGACY_CONFIGURATION[@]}"
        assert_client_configuration legacy-alias "legacy key password" \
            "${TEST_TEMP_DIR}/legacy keystore.jks" PKCS12 "legacy keystore password" \
            "${TEST_TEMP_DIR}/legacy truststore.jks" PKCS12 "legacy truststore password"
    done
}

@test "client TLS: schema-declared settings take precedence over legacy settings" {
    set_canonical_configuration
    set_legacy_configuration
    for component in "${COMPONENTS[@]}"; do
        capture_arguments "$component" "${LEGACY_CONFIGURATION[@]}" "${CANONICAL_CONFIGURATION[@]}"
        assert_client_configuration client-alias "client key password" \
            "${TEST_TEMP_DIR}/client keystore.jks" JKS "client keystore password" \
            "${TEST_TEMP_DIR}/client truststore.jks" JKS "client truststore password"
    done
}

@test "client TLS: all launchers retain certificate fallback when client settings are absent" {
    for component in "${COMPONENTS[@]}"; do
        capture_arguments "$component"
        assert_client_configuration server-alias server-key-password \
            "${TEST_TEMP_DIR}/server-keystore.p12" PKCS12 server-keystore-password \
            "${TEST_TEMP_DIR}/server-truststore.p12" PKCS12 server-truststore-password
    done
}

@test "client TLS: dedicated certificate alias is preferred over server alias" {
    for component in "${COMPONENTS[@]}"; do
        capture_arguments "$component" ZWE_zowe_certificate_keystore_clientCertificateAlias=dedicated-client
        assert_property apiml.service.ssl.key-alias dedicated-client
        assert_property server.ssl.keyAlias server-alias
    done
}

@test "client TLS: empty schema-declared settings preserve legacy fallback" {
    set_legacy_configuration
    for component in "${COMPONENTS[@]}"; do
        capture_arguments "$component" "${LEGACY_CONFIGURATION[@]}" \
            ZWE_configs_apiml_service_ssl_key_alias= \
            ZWE_configs_apiml_service_ssl_key_store= \
            ZWE_configs_apiml_service_ssl_key_store_type= \
            ZWE_configs_apiml_service_ssl_key_store_password= \
            ZWE_configs_apiml_service_ssl_trust_store= \
            ZWE_configs_apiml_service_ssl_trust_store_type= \
            ZWE_configs_apiml_service_ssl_trust_store_password=
        assert_client_configuration legacy-alias "legacy key password" \
            "${TEST_TEMP_DIR}/legacy keystore.jks" PKCS12 "legacy keystore password" \
            "${TEST_TEMP_DIR}/legacy truststore.jks" PKCS12 "legacy truststore password"
    done
}
