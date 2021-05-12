/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication.providers;

import io.restassured.RestAssured;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Basic set of logout related tests that needs to pass against every valid authentication provider.
 */
abstract class LogoutTest implements TestWithStartedInstances {

    protected final static String QUERY_ENDPOINT = "/gateway/api/v1/auth/query";
    protected final static String COOKIE_NAME = "apimlAuthenticationToken";

    protected static String[] logoutUrlsSource() {
        return new String[]{SecurityUtils.getGatewayLogoutUrl(), SecurityUtils.getGatewayLogoutUrlOldPath()};
    }

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @ParameterizedTest
    @MethodSource("logoutUrlsSource")
    void testLogout(String logoutUrl) {
        // make login
        String jwt = generateToken();

        // check if it is logged in
        assertIfLogged(jwt, true);

        logout(logoutUrl, jwt);

        // check if it is logged out
        assertIfLogged(jwt, false);
    }

    protected void assertIfLogged(String jwt, boolean logged) {
        final HttpStatus status = logged ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
        .when()
            .get(HttpRequestUtils.getUriFromGateway(QUERY_ENDPOINT))
        .then()
            .statusCode(status.value());
    }

    protected String generateToken() {
        return SecurityUtils.gatewayToken();
    }

    protected void logout(String url, String jwtToken) {
        SecurityUtils.logoutOnGateway(url, jwtToken);
    }

    protected void assertLogout(String url, String jwtToken, int expectedStatusCode) {
        given()
            .cookie(COOKIE_NAME, jwtToken)
        .when()
            .post(url)
        .then()
            .statusCode(is(expectedStatusCode));
    }
}
