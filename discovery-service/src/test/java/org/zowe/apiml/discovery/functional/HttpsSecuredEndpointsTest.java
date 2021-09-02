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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;

@ActiveProfiles("https")
public class HttpsSecuredEndpointsTest extends DiscoveryFunctionalTest {

    @Override
    protected String getProtocol() {
        return "https";
    }

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;
    @Value("${server.ssl.keyStore}")
    private String keystore;
    private String clientKeystore = "../keystore/client_cert/client-certs.p12";

    @Override
    @BeforeEach
    void setUp() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(
            keystorePassword,
            clientKeystore,
            keystore);
        SslContext.prepareSslAuthentication(configurer);
    }

    @Test
    void testEurekaEndpoints_whenProvidedCertificate() throws Exception {
        given().config(SslContext.clientCertApiml)
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    void testDiscoveryEndpoints_whenProvidedCertification() throws Exception {
        given().config(SslContext.clientCertApiml)
            .when()
            .get(getDiscoveryUriWithPath("/discovery/api/v1/staticApi"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    void givenTLS_whenProvidedNothing() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_FORBIDDEN))
            .header(HttpHeaders.WWW_AUTHENTICATE, nullValue());
    }

    @Test
    void givenTLS_whenProvidedBasicAuthentication() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .auth().basic("username", "password")
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_FORBIDDEN));
    }

    // /application health,info endpoints
    @Test
    void testApplicationInfoEndpoints_whenProvidedNothing() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/application/info"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    void testApplicationHealthEndpoints_whenProvidedNothing() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath("/application/health"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    // /application endpoints
    @ParameterizedTest(name = "givenTLS_testApplicationBeansEndpoints_Get {index} {0} ")
    @ValueSource(strings = {"/application/beans", "/discovery/api/v1/staticApi", "/"})
    void givenTLS_testApplicationBeansEndpoints_Get(String path) throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        given()
            .when()
            .get(getDiscoveryUriWithPath(path))
            .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, containsString(DISCOVERY_REALM));
    }

    @Test
    void verifyHttpHeadersOnEureka() throws Exception {
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
