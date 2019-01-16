/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;



import com.ca.mfaas.security.login.LoginRequest;
import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecurityUtils {
    private final static String TOKEN = "apimlAuthenticationToken";
    private final static String LOGIN_ENDPOINT = "/auth/login";

    private static GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static String scheme = serviceConfiguration.getScheme();
    private static String host = serviceConfiguration.getHost();
    private static int port = serviceConfiguration.getPort();
    private static String basePath = "/api/v1/gateway";

    public static String gatewayToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        String token = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .cookie(TOKEN, not(isEmptyString()))
            .extract().cookie(TOKEN);

        return token;
    }
}
