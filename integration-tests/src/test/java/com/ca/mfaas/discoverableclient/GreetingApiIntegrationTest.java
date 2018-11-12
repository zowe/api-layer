/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discoverableclient;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import com.ca.mfaas.utils.http.HttpRequestUtils;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.RestAssured;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class GreetingApiIntegrationTest {
    private static final String GREETING_ENDPOINT = "/api/v1/discoverableclient/greeting";

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
    public void shouldCallDiscoverableServiceApi() throws Exception {
        given()
            .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, GREETING_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .body("", is("%2fmy%2fvariable%2fpath"));


        // When
        final HttpResponse response = HttpRequestUtils.getResponse("/api/v1/discoverableclient/greeting", SC_OK);
        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        String content = jsonContext.read("$.content");

        // Then
        assertThat(content, equalTo("Hello, world!"));
    }
}
