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
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.*;

/**
 * Basic set of logout related tests that needs to pass against every valid authentication provider.
 */
@GeneralAuthenticationTest
class LogoutTest implements TestWithStartedInstances {

    protected final static String QUERY_ENDPOINT = "/gateway/api/v1/auth/query";

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
    void givenValidCredentials_whenUserLogsOut_thenUsedTokenIsLoggedOut(String logoutUrl) {
        // make login
        String jwt = SecurityUtils.gatewayToken();

        // check if it is logged in
        assertIfLogged(jwt, true);

        SecurityUtils.logoutOnGateway(logoutUrl, jwt);

        // check if it is logged out
        assertIfLogged(jwt, false);
    }
}
