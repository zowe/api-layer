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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * End-to-end acceptance tests for {@code apiml.security.enableStrictUrlValidation} in the (Zuul) Gateway. These
 * assert the deterministic, security-relevant behavior that is observable end to end:
 * <ul>
 *     <li>{@link WhenStrictValidationEnabled} (the default) - a request containing one of the controlled encoded
 *     characters is rejected, whether the target is a routed service or a Gateway-internal endpoint;</li>
 *     <li>{@link WhenStrictValidationDisabled} - Gateway-internal endpoints remain strictly validated.</li>
 * </ul>
 * Rejection is asserted as HTTP status 400.
 */
@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class StrictUrlValidationTest {

    /**
     * Every special character checked by strict validation.
     */
    private static final List<Named<String>> SPECIAL_CHARACTERS = Arrays.asList(
        Named.of("encoded slash", "encoded%2Fslash"),
        Named.of("encoded double slash", "encoded%2F%2Fslash"),
        Named.of("backslash", "encoded%5Cbackslash"),
        Named.of("encoded percent", "encoded%25percent"),
        Named.of("encoded period", "encoded%2Eperiod"),
        Named.of("semicolon", "path;matrix")
    );

    private static final List<String> INTERNAL_ENDPOINTS = Arrays.asList("/gateway", "/application", "/images", "/api-doc");

    static Stream<Arguments> specialCharacters() {
        return SPECIAL_CHARACTERS.stream().map(Arguments::arguments);
    }

    static Stream<Arguments> internalEndpointsWithSpecialCharacters() {
        return INTERNAL_ENDPOINTS.stream().flatMap(endpoint ->
            SPECIAL_CHARACTERS.stream().map(character -> arguments(endpoint, character)));
    }

    @Nested
    @AcceptanceTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "apiml.security.enableStrictUrlValidation=true")
    class WhenStrictValidationEnabled extends AcceptanceTestWithTwoServices {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#specialCharacters")
        void whenRoutedRequestContainsSpecialCharacter_thenRejected(String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath() + "/" + pathSuffix)
            .then()
                .statusCode(is(SC_BAD_REQUEST));
        }

        @ParameterizedTest(name = "{0}/[{1}]")
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#internalEndpointsWithSpecialCharacters")
        void whenInternalEndpointRequestContainsSpecialCharacter_thenRejected(String internalPath, String pathSuffix) {
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
    @ActiveProfiles("test")
    @TestPropertySource(properties = "apiml.security.enableStrictUrlValidation=false")
    class WhenStrictValidationDisabled extends AcceptanceTestWithTwoServices {

        @BeforeEach
        void routeToService() throws IOException {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();
            discoveryClient.createRefreshCacheEvent();
        }

        @ParameterizedTest(name = "[{0}]")
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#specialCharacters")
        void whenRoutedRequestContainsSpecialCharacter_thenForwardedToService(String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath() + "/" + pathSuffix)
            .then()
                .log().all()
                .statusCode(is(SC_OK));
        }

        @ParameterizedTest(name = "{0}/[{1}]")
        @MethodSource("org.zowe.apiml.acceptance.StrictUrlValidationTest#internalEndpointsWithSpecialCharacters")
        void whenInternalEndpointRequestContainsSpecialCharacter_thenStillRejected(String internalPath, String pathSuffix) {
            given()
                .urlEncodingEnabled(false)
            .when()
                .get(basePath + internalPath + "/" + pathSuffix)
            .then()
                .log().all()
                .statusCode(is(SC_BAD_REQUEST));
        }

    }

}
