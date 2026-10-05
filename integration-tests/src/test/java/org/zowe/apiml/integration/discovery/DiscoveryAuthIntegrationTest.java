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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoveryServiceTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.requests.Endpoints.*;

/**
 * This test suite must be run with HTTPS profile
 * Verifies integration of Discovery service with ZAAS
 */
@DiscoveryServiceTest
class DiscoveryAuthIntegrationTest implements TestWithStartedInstances {

    private DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private final static String COOKIE = "apimlAuthenticationToken";
    private String scheme;
    private String username;
    private String password;
    private String host;
    private int port;

    @BeforeEach
    void setUp() {
        discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        scheme = discoveryServiceConfiguration.getScheme();
        username = ConfigReader.environmentConfiguration().getCredentials().getUser();
        password = ConfigReader.environmentConfiguration().getCredentials().getPassword();
        host = discoveryServiceConfiguration.getHost();
        port = discoveryServiceConfiguration.getPort();
    }

    @ParameterizedTest(name = "testApplicationInfoEndpoints_Cookie {index} {0} ")
    @ValueSource(strings = {DISCOVERY_STATIC_API})
    void testApplicationInfoEndpoints_Cookie(String path) throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        var jwtToken = SecurityUtils.gatewayToken(username, password);

        //@formatter:off
        given()
            .cookie(COOKIE, jwtToken)
        .when()
            .get(getDiscoveryUriWithPath(path))
        .then()
            .statusCode(is(HttpStatus.SC_OK));
        //@formatter:on
    }

    /**
     * The Discovery Service root path answered with the Eureka server's dashboard, and this suite asserted it here as
     * one more endpoint that a cookie token opens. That page came from Eureka and is gone with it - this change
     * removes the dashboard controller, and the {@code spring-cloud-starter-netflix-eureka-server} dependency that
     * used to serve {@code /} is no longer on the classpath, so nothing maps the path any more.
     * <p>
     * The case is kept and asserted against the new answer rather than dropped: a bare {@code GET /} on the Discovery
     * Service is not served, and that is now the contract. Deleting the case outright would have hidden a deliberate
     * change in behaviour behind a missing test.
     */
    @Test
    void givenTheEurekaDashboardIsGone_whenTheRootPathIsRequested_thenItIsNotServed() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        var jwtToken = SecurityUtils.gatewayToken(username, password);

        //@formatter:off
        given()
            .cookie(COOKIE, jwtToken)
        .when()
            .get(getDiscoveryUriWithPath("/"))
        .then()
            .statusCode(is(HttpStatus.SC_NOT_FOUND));
        //@formatter:on
    }

    private URI getDiscoveryUriWithPath(String path) throws Exception {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }

}
