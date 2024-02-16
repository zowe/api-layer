/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.schemes;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.requests.GatewayRequests;
import org.zowe.apiml.util.requests.JsonResponse;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.SAF_IDT_REQUEST;

@DiscoverableClientDependentTest
@NotForMainframeTest
@InfinispanStorageTest
public class SafIdtSchemeTest {
    private final GatewayRequests gateway = new GatewayRequests();

    static Set<String> scopes = new HashSet<>();
    static String jwt;
    static String pat;

    static {
        scopes.add("dcsafidt");
        jwt = gatewayToken();
        pat = personalAccessToken(scopes);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> accessTokens() {

        return Stream.of(
            Arguments.of(pat, COOKIE_NAME),
            Arguments.of(pat, PAT_COOKIE_AUTH_NAME),
            Arguments.of(jwt, COOKIE_NAME)
        );
    }

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }


    @Nested
    class WhenUsingSafIdtAuthenticationScheme {
        @Nested
        class ResultContainsSafIdtInHeader {
            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.SafIdtSchemeTest#accessTokens")
            void givenJwtInCookie(String jwt, String cookieName) {
                System.out.println(System.currentTimeMillis());
                JsonResponse response = gateway.authenticatedRoute(SAF_IDT_REQUEST, jwt, cookieName);
                System.out.println(System.currentTimeMillis());

                Map<String, String> headers = response.getJson().read("headers");

                boolean safTokenIsPresent = headers.containsKey("x-saf-token");
                assertThat(safTokenIsPresent, is(true));
            }
        }
    }
}
