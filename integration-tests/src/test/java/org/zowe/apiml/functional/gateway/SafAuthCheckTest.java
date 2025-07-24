/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.SAFAuthResourceCheckTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.requests.Endpoints.SAF_AUTH_CHECK;

@SAFAuthResourceCheckTest
class SafAuthCheckTest {

    private static final String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();

    @BeforeAll
    static void setupSsl() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @BeforeEach
    void setUp() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private final SafCheckRequest request = new SafCheckRequest(
        "ZOWE",
        "APIML.SERVICES",
        "READ"
    );

    @Nested
    class GivenClientCertAuthentication {

        @Test
        void returnReturn204() {
            given()
                .config(SslContext.clientCertUser)
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post(HttpRequestUtils.getUriFromGateway(SAF_AUTH_CHECK))
            .then()
                .statusCode(SC_NO_CONTENT);
        }
    }

    @Nested
    class GivenBearerAuthentication {

        @Test
        void return204() {
            String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("Authorization", "Bearer " + token)
            .when()
                .post(HttpRequestUtils.getUriFromGateway(SAF_AUTH_CHECK))
            .then()
                .statusCode(is(SC_NO_CONTENT));
        }
    }

    @Nested
    class GivenInvalidCredentials {

        @Test
        void return401() {
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("Authorization", "Bearer invalidToken")
            .when()
                .post(HttpRequestUtils.getUriFromGateway(SAF_AUTH_CHECK))
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
        }
    }

    @Data
    @AllArgsConstructor
    static class SafCheckRequest {
        private final String resourceClass;
        private final String resourceName;
        private final String accessLevel;
    }
}


