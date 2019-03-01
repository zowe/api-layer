/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.gatewayservice;


import com.broadcom.apiml.service.security.login.LoginRequest;
import com.broadcom.apiml.test.integration.utils.config.ConfigReader;
import com.broadcom.apiml.test.integration.utils.config.GatewayServiceConfiguration;
import com.broadcom.apiml.test.integration.utils.config.ZosmfServiceConfiguration;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecurityUtils {
    public final static String ZOSMF_TOKEN = "LtpaToken2";
    private final static String GATEWAY_TOKEN = "apimlAuthenticationToken";
    private final static String GATEWAY_LOGIN_ENDPOINT = "/auth/login";
    private final static String GATEWAY_BASE_PATH = "/api/v1/gateway";
    private final static String ZOSMF_LOGIN_ENDPOINT = "/zosmf/info";

    private static GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static ZosmfServiceConfiguration zosmfServiceConfiguration = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration();

    private static String gatewayScheme = serviceConfiguration.getScheme();
    private static String gatewayHost = serviceConfiguration.getHost();
    private static int gatewayPort = serviceConfiguration.getPort();

    private static String zosmfScheme = zosmfServiceConfiguration.getScheme();
    private static String zosmfHost = zosmfServiceConfiguration.getHost();
    private static int zosmfPort = zosmfServiceConfiguration.getPort();

    //@formatter:off
    public static String gatewayToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        return given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, GATEWAY_BASE_PATH, GATEWAY_LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .cookie(GATEWAY_TOKEN, not(isEmptyString()))
            .extract().cookie(GATEWAY_TOKEN);
    }

    public static String zosmfToken(String username, String password) {
        return given()
            .auth().preemptive().basic(username, password)
            .header("X-CSRF-ZOSMF-HEADER", "zosmf")
            .when()
            .get(String.format("%s://%s:%d%s", zosmfScheme, zosmfHost, zosmfPort, ZOSMF_LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .cookie(ZOSMF_TOKEN, not(isEmptyString()))
            .extract().cookie(ZOSMF_TOKEN);
    }
    //@formatter:on
}
