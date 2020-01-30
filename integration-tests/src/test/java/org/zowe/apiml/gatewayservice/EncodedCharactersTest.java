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

import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
@Ignore
public class EncodedCharactersTest {


    private String scheme;
    private String host;
    private int port;

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Before
    public void setUp() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
    }

    @Test
    public void shouldCallDiscoverableServiceWithEncodedCharacterAndAllow() {
        final String encodedURI = "/api/v1/discoverableclient/wor%2fld/greeting";

        given()
            .urlEncodingEnabled(false)
            .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, encodedURI))
            .then()
            .body("content", is("Hello, wor/ld!"))
            .statusCode(is(SC_OK));
    }

    @Test
    public void shouldCallCatalogServiceWithEncodedCharacterAndReject() {
        final String encodedURI = "/api/v1/apicatalog/gf%2fd/testcall";

        given()
            .contentType(ContentType.JSON)
            .urlEncodingEnabled(false)
            .when()
            .get(String.format("%s://%s:%s%s", scheme, host, port, encodedURI))
            .then()
            .statusCode(is(SC_BAD_REQUEST));
    }
}
