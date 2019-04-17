/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.helloworldspring;

import com.ca.mfaas.utils.categories.AdditionalLocalTest;
import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;

@Category(AdditionalLocalTest.class)
public class HelloWorldSpringIntegrationTest {
    private static final String APP_INFO = "/api/hellospring/application/info";
    private static final String APP_HEALTH = "/api/hellospring/application/health";
    private static final String API_DOC_PATH = "/api/v1/api-doc/hellospring/";
    private static final String GREETING_PATH = "/api/v1/hellospring/greeting";
    private static final String GREETING_WITH_NAME_PATH = "/api/v1/hellospring/greeting/petr";

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
    //@formatter:off
    public void shouldGetApplicationInfo() {
        given()
        .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, APP_INFO))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void shouldGetHealth() {
        given()
        .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, APP_HEALTH))
        .then()
            .statusCode(is(SC_OK))
            .body("status", is("UP"));
    }

    @Test
    public void shouldGetApiDoc() {
        given()
        .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, API_DOC_PATH))
        .then()
            .statusCode(is(SC_OK))
            .body("info.description", is("REST API for a Spring Application"));
    }

    @Test
    public void shouldGetGreeting() {
        given()
        .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, GREETING_PATH))
        .then()
            .statusCode(is(SC_OK))
            .body("content", is("Hello, World!"));
    }

    @Test
    public void shouldGetGreetingWithName() {
        given()
        .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, GREETING_WITH_NAME_PATH))
        .then()
            .statusCode(is(SC_OK))
            .body("content", is("Hello, petr!"));
    }
    //@formatter:on
}
