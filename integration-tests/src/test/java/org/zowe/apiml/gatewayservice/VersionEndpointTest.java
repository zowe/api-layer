/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;

@TestsNotMeantForZowe
public class VersionEndpointTest {

    private String requestString;

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @BeforeEach
    public void setUp() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String scheme = serviceConfiguration.getScheme();
        String host = serviceConfiguration.getHost();
        int port = serviceConfiguration.getPort();
        requestString = String.format("%s://%s:%s%s", scheme, host, port, "/api/v1/gateway/version");
    }

    @Test
    public void shouldCallVersionEndpointAndReceivedVersion() {
        given()
            .when()
            .get(requestString)
            .then()
            .body(containsString("apiml"))
            .statusCode(is(SC_OK));
    }
}
