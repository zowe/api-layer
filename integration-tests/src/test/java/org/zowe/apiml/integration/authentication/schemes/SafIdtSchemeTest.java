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
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;

@DiscoverableClientDependentTest
@NotForMainframeTest
public class SafIdtSchemeTest {
    private final static String REQUEST_INFO_ENDPOINT = "/api/v1/dcsafidt/request";

    private final static URI requestUrl = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);

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
                String jwt = gatewayToken();

                JsonPath response = given()
                    .cookie(COOKIE_NAME, jwt)
                .when()
                    .get(requestUrl)
                .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath();

                boolean safTokenIsPresent = response.getMap("headers").containsKey("x-saf-token");
                assertThat(safTokenIsPresent, is(true));
            }
        }
    }
}
