/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.functional;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.zowe.apiml.apicatalog.ApiCatalogApplication;

import static io.restassured.RestAssured.given;



@SpringBootTest(
    classes = ApiCatalogApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"apiml.health.protected=false"}
)
@ContextConfiguration
public class ApiCatalogProtectedEndpointTest  {

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    protected String getCatalogUriWithPath(String path) {
        return getCatalogUriWithPath("https", path);
    }

    protected String getCatalogUriWithPath(String scheme, String path) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, path);
    }

    @Test
    void requestFailsWith401() {
        given()
            .when()
            .get(getCatalogUriWithPath("apicatalog/application/health"))
            .then()
            .statusCode(HttpStatus.SC_OK);
    }
}
