/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Modulith acceptance tests validating the {@code apiml.security.enableStrictUrlValidation} option. The two nested
 * classes exercise the two states of the option, differentiated by {@link TestPropertySource}:
 * <ul>
 *     <li>{@link WhenStrictValidationEnabled} - the default, strict URL validation rejects requests containing
 *     any of the encoded characters controlled by the option, whether the target is a routed service or an
 *     internal endpoint.</li>
 *     <li>{@link WhenStrictValidationDisabled} - relaxed URL validation forwards such requests to the target
 *     service for routed traffic, but internal endpoints remain strictly validated.</li>
 * </ul>
 * The parameterized cases cover every character relaxed by the option: encoded slash, encoded double slash,
 * backslash, encoded percent, encoded period, and semicolon.
 */
@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class StrictUrlValidationTest {

    private static final String SERVICE_ID = "serviceid1";

    /**
     * Path suffixes, each containing one of the special characters controlled by
     * {@code apiml.security.enableStrictUrlValidation}.
     */
    private static final List<Named<String>> SPECIAL_CHARACTERS = List.of(
        Named.of("encoded slash", "encoded%2Fslash"),
        Named.of("encoded double slash", "encoded%2F%2Fslash"),
        Named.of("backslash", "encoded%5Cbackslash"),
        Named.of("encoded percent", "encoded%25percent"),
        Named.of("encoded period", "encoded%2Eperiod"),
        Named.of("semicolon", "path;matrix")
    );

    /**
     * Internal base paths, which are strictly validated even when the option is disabled. In the modulith the
     * Gateway also fronts the API Catalog and Caching service, so this set is broader than in a standalone Gateway.
     * Matches {@code BASE_PATHS_MODULITH} in {@code WebSecurity}.
     */
    private static final List<String> INTERNAL_ENDPOINTS = List.of(
        "/gateway", "/application", "/images", "/v3/api-docs", "/apicatalog", "/cachingservice"
    );

    static Stream<Arguments> specialCharacters() {
        return SPECIAL_CHARACTERS.stream().map(Arguments::arguments);
    }

    static Stream<Arguments> internalEndpointsWithSpecialCharacters() {
        return INTERNAL_ENDPOINTS.stream().flatMap(endpoint ->
            SPECIAL_CHARACTERS.stream().map(character -> arguments(endpoint, character)));
    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "apiml.security.enableStrictUrlValidation=true"
    })
    class WhenStrictValidationEnabled extends AcceptanceTestWithMockServices {

        @BeforeAll
        void setUp() {
            mockService(SERVICE_ID).scope(MockService.Scope.CLASS)
                .addEndpoint("/" + SERVICE_ID + "/test")
                .and().start();
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#specialCharacters")
        void whenRoutedRequestContainsSpecialCharacter_thenRejectedWithBadRequest(String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + "/" + SERVICE_ID + "/api/v1/test/" + pathSuffix)
            .then()
                .statusCode(is(SC_BAD_REQUEST));
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#internalEndpointsWithSpecialCharacters")
        void whenInternalEndpointRequestContainsSpecialCharacter_thenRejectedWithBadRequest(String internalPath, String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + internalPath + "/" + pathSuffix)
            .then()
                .statusCode(is(SC_BAD_REQUEST));
        }

    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "apiml.security.enableStrictUrlValidation=false"
    })
    class WhenStrictValidationDisabled extends AcceptanceTestWithMockServices {

        @BeforeAll
        void setUp() {
            mockService(SERVICE_ID).scope(MockService.Scope.CLASS)
                .addEndpoint("/" + SERVICE_ID + "/test")
                .and().start();
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#specialCharacters")
        void whenRoutedRequestContainsSpecialCharacter_thenForwardedToService(String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + "/" + SERVICE_ID + "/api/v1/test/" + pathSuffix)
            .then()
                .statusCode(is(SC_OK));
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#internalEndpointsWithSpecialCharacters")
        void whenInternalEndpointRequestContainsSpecialCharacter_thenStillRejectedWithBadRequest(String internalPath, String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + internalPath + "/" + pathSuffix)
            .then()
                .statusCode(is(SC_BAD_REQUEST));
        }

    }

}
