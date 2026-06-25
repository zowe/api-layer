/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.discovery;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoveryBasicAuthTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.requests.Endpoints.APPLICATIONS;

/**
 * Verifies that the Discovery Service protects the Eureka registration endpoint with HTTP basic
 * authentication when {@code apiml.security.ssl.verifySslCertificatesOfServices=false}.
 * <p>
 * Requires the discovery service to be started with:
 * <ul>
 *   <li>{@code APIML_SECURITY_SSL_VERIFYSSLCERTIFICATESOFSERVICES=false}</li>
 *   <li>{@code APIML_DISCOVERY_USERID=eureka}</li>
 *   <li>{@code APIML_DISCOVERY_PASSWORD=password}</li>
 * </ul>
 */
@DiscoveryBasicAuthTest
class DiscoveryBasicAuthProtectionTest implements TestWithStartedInstances {

    private static final String EUREKA_USERID = "eureka";
    private static final String EUREKA_PASSWORD = "password";

    private String scheme;
    private String host;
    private int port;

    @BeforeEach
    void setUp() {
        DiscoveryServiceConfiguration config = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        scheme = config.getScheme();
        host = config.getHost();
        port = config.getPort();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenNoCredentials_thenReturnUnauthorized() throws URISyntaxException {
        given()
            .get(eurekaAppsUri())
        .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED));
    }

    @Test
    void givenValidEurekaCredentials_thenReturnOk() throws URISyntaxException {
        given()
            .auth().preemptive().basic(EUREKA_USERID, EUREKA_PASSWORD)
            .get(eurekaAppsUri())
        .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    void givenWrongEurekaCredentials_thenReturnUnauthorized() throws URISyntaxException {
        given()
            .auth().preemptive().basic(EUREKA_USERID, "wrongPassword")
            .get(eurekaAppsUri())
        .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED));
    }

    private URI eurekaAppsUri() throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(APPLICATIONS)
            .build();
    }
}
