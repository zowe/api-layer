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
    @ValueSource(strings = {"/discovery/api/v1/staticApi", "/"})
    void testApplicationInfoEndpoints_Cookie(String path) throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        String jwtToken = SecurityUtils.gatewayToken(username, password);
        given()
            .cookie(COOKIE, jwtToken)
            .when()
            .get(getDiscoveryUriWithPath(path))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
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
