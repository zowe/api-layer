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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.requests.GatewayRequests;
import org.zowe.apiml.util.requests.JsonResponse;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@DiscoverableClientDependentTest
@NotForMainframeTest
public class SafIdtSchemeTest {
    private final GatewayRequests gateway = new GatewayRequests();

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class WhenUsingSafIdtAuthenticationScheme {
        @Nested
        class ResultContainsSafIdtInHeader {
            @Test
            void givenJwtInCookie() {
                JsonResponse response = gateway.authenticatedRoute("/api/v1/dcsafidt/request");
                Map<String, String> headers = response.getJson().read("headers");

                boolean safTokenIsPresent = headers.containsKey("x-saf-token");
                assertThat(safTokenIsPresent, is(true));
            }
        }
    }
}
