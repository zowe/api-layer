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

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

public class AllowEncodedSlashIntegrationTest {
    private static final String GREETING_PATH = "/ui/v1/discoverableclient/greeting/with%2fslash";

    private GatewayServiceConfiguration serviceConfiguration;
    private String scheme;
    private String host;
    private int port;

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
    }

    @Test
    public void shouldGetGreetingWithEncodedSlash() {
        given()
        .urlEncodingEnabled(false)
        .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, GREETING_PATH))
        .then()
            .statusCode(is(SC_OK))
            .body("content", is("Hello, with/slash!"));
    }
}
