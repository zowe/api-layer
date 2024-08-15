/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;

@ActiveProfiles("https")
@TestPropertySource(
    properties = {
        "apiml.health.protected=false"
    }
)
class HttpsSecuredEndpointsTest extends DiscoveryFunctionalTest {

    @Override
    protected String getProtocol() {
        return "https";
    }

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;

    @Value("${server.ssl.keyStore}")
    private String keystore;

    @Override
    @BeforeEach
    void setUp() throws Exception {
        String clientKeystore = "../keystore/client_cert/client-certs.p12";
        SslContextConfigurer configurer = new SslContextConfigurer(
            keystorePassword,
            clientKeystore,
            keystore);
        SslContext.prepareSslAuthentication(configurer);
    }

    @Nested
    class ThenReturnOk {
        @Test
        void eurekaEndpoints_whenProvidedCertificate() {
            given().config(SslContext.clientCertApiml)
                .when()
                .get(getDiscoveryUriWithPath("/eureka/apps"))
                .then()
                .statusCode(is(HttpStatus.SC_OK));
        }

        @Test
        void discoveryEndpoints_whenProvidedCertification() {
            given().config(SslContext.clientCertApiml)
                .when()
                .get(getDiscoveryUriWithPath("/discovery/api/v1/staticApi"))
                .then()
                .statusCode(is(HttpStatus.SC_OK));
        }

        @ParameterizedTest(name = "givenApplicationEndpoints_whenProvidedNothing {index} {0} ")
        @ValueSource(strings = {"/application/info", "/application/health"})
        void givenApplicationEndpoints_whenProvidedNothing(String endpoint) {
            RestAssured.useRelaxedHTTPSValidation();
            given()
                .when()
                .get(getDiscoveryUriWithPath(endpoint))
                .then()
                .statusCode(is(HttpStatus.SC_OK));
        }
    }

    @Nested
    class GivenTLS {
        @BeforeEach
        void setup() {
            RestAssured.useRelaxedHTTPSValidation();
        }

        @Nested
        class ThenReturnForbidden {
            @Test
            void whenProvidedNothing() {
                given()
                    .when()
                    .get(getDiscoveryUriWithPath("/eureka/apps"))
                    .then()
                    .statusCode(is(HttpStatus.SC_FORBIDDEN))
                    .header(HttpHeaders.WWW_AUTHENTICATE, nullValue());
            }

            @Test
            void whenProvidedBasicAuthentication() {
                given()
                    .auth().basic("username", "password")
                    .when()
                    .get(getDiscoveryUriWithPath("/eureka/apps"))
                    .then()
                    .statusCode(is(HttpStatus.SC_FORBIDDEN));
            }
        }

        @ParameterizedTest(name = "whenProvidedNothing_thenReturnUnauthorized {index} {0} ")
        @ValueSource(strings = {"/application/beans", "/discovery/api/v1/staticApi", "/"})
        void whenProvidedNothing_thenReturnUnauthorized(String path) {
            given()
                .when()
                .get(getDiscoveryUriWithPath(path))
                .then()
                .statusCode(is(HttpStatus.SC_UNAUTHORIZED))
                .header(HttpHeaders.WWW_AUTHENTICATE, containsString(DISCOVERY_REALM));
        }
    }

    @Test
    void whenGetApps_thenCorrectHeadersInResponse() {
        RestAssured.useRelaxedHTTPSValidation();
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Content-Type-Options", "nosniff");
        expectedHeaders.put("X-XSS-Protection", "1; mode=block");
        expectedHeaders.put("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        expectedHeaders.put("Pragma", "no-cache");
        expectedHeaders.put("Content-Type", "application/xml");
        expectedHeaders.put("X-Frame-Options", "DENY");

        List<String> forbiddenHeaders = new ArrayList<>();
        forbiddenHeaders.add("Strict-Transport-Security");


        Response response = RestAssured
            .given().config(SslContext.clientCertApiml)
            .get(getDiscoveryUriWithPath("/eureka/apps"));
        Map<String, String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach(h -> responseHeaders.put(h.getName(), h.getValue()));

        expectedHeaders.forEach((key, value) -> assertThat(responseHeaders, hasEntry(key, value)));
        forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
    }

}
