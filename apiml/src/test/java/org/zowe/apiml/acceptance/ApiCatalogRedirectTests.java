/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_PERMANENT_REDIRECT;

@TestInstance(Lifecycle.PER_CLASS)
class ApiCatalogRedirectTests {

    @AcceptanceTest
    @Nested
    class GivenCatalogReactiveController {

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;

        @Test
        void whenCatalogApi_thenRedirect() {
            given()
                .config(RestAssuredConfig.config()
                    .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                    .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/api/v1"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/apicatalog/api/v1/");
        }

        @Test
        void whenCatalogApiIndex_thenRedirect() {
            given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/api/v1/"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/apicatalog/api/v1/index.html");
        }

        @Test
        void whenCatalogUi_thenRedirect() {
            given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/ui/v1"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/apicatalog/ui/v1/");
        }

        @Test
        void whenCatalogUiIndex_thenRedirect() {
            given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/ui/v1/"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/apicatalog/ui/v1/index.html");
        }

        @Test
        void whenCatalogLogin_thenRedirect() {
            given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .post(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/api/v1/auth/login"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/login");
        }

        @Test
        void whenCatalogLogout_thenRedirect() {
            given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .post(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/api/v1/auth/logout"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/logout");
        }

        @Test
        void whenCatalogQuery_thenRedirect() {
            given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(false)))
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "https", "apicatalog/api/v1/auth/query"))
            .then()
                .statusCode(SC_PERMANENT_REDIRECT)
                .and()
                .header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/query");
        }

    }

    private String getGatewayUrlWithPath(String hostname, int port, String scheme, String path) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, path);
    }

}
