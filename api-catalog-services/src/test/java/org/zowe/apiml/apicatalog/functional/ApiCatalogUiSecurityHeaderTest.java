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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;

@DirtiesContext
@AutoConfigureWebTestClient
class ApiCatalogUiSecurityHeaderTest extends ApiCatalogFunctionalTest {

    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String SAMEORIGIN = "SAMEORIGIN";
    private static final String DEFAULT_SRC_SELF = "default-src 'self'";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    @LocalServerPort
    private int port;

    @Override
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void shouldReturnContentSecurityPolicyHeaderForUiIndex() {
        given()
        .when()
            .get("/apicatalog/ui/v1/index.html")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .header(CONTENT_SECURITY_POLICY, Matchers.containsString(DEFAULT_SRC_SELF))
            .header(X_FRAME_OPTIONS, Matchers.equalTo(SAMEORIGIN))
            .header(X_CONTENT_TYPE_OPTIONS, Matchers.equalTo("nosniff"));
    }

    @Test
    void shouldReturnContentSecurityPolicyHeaderForUiRootPath() {
        given()
            .when()
            .get("/apicatalog/ui/v1/")
            .then()
            .header(CONTENT_SECURITY_POLICY, Matchers.containsString(DEFAULT_SRC_SELF))
            .header(X_FRAME_OPTIONS, Matchers.equalTo(SAMEORIGIN))
            .header(X_CONTENT_TYPE_OPTIONS, Matchers.equalTo("nosniff"));
    }
}
