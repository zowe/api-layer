/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.requests;

import com.jayway.jsonpath.ReadContext;
import io.restassured.RestAssured;
import org.apache.http.client.utils.URIBuilder;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.Credentials;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * This class is built with the expectation that there is one discovery service to communicate with.
 */
public class DiscoveryRequests {
    private static final DiscoveryServiceConfiguration discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
    private static final Credentials credentials = ConfigReader.environmentConfiguration().getCredentials();

    private final Requests requests;
    private final String scheme;
    private final String host;
    private final int port;

    public DiscoveryRequests(String host) {
        this(discoveryServiceConfiguration.getScheme(), host, discoveryServiceConfiguration.getPort(), new Requests());
    }

    public DiscoveryRequests(String scheme, String host, int port, Requests requests) {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        this.requests = requests;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    public boolean isApplicationRegistered(String appName) {
        try {
            ReadContext result = getJsonEurekaApps();

            return result.read("$.applications.application.app[?(@.name=" + appName + ")].instances.length()");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUp() {
        try {
            ReadContext healthResponse = requests.getJson(getDiscoveryUriWithPath("/application/health"));
            String health = healthResponse.read("$.status");

            return health.equals("UP");
        } catch (Exception e) {
            return false;
        }
    }

    public ReadContext getJsonEurekaApps() throws URISyntaxException {
        return requests.getJson(getDiscoveryUriWithPath("/eureka/apps"));
    }

    public int getAmountOfRegisteredInstancesForService(String appName) {
        try {
            ReadContext result = getJsonEurekaApps();

            return result.read("$.applications.application.app[?(@.name=" + appName + ")].instances.length()");
        } catch (Exception e) {

            return 0;
        }
    }

    public void shutdown() {
        String SHUTDOWN = "/application/shutdown";

        try {
            given()
                .contentType(JSON)
                .auth().basic(credentials.getUser(), credentials.getPassword())
            .when()
                .post(getDiscoveryUriWithPath(SHUTDOWN))
            .then()
                .statusCode(is(SC_OK));
        } catch (Exception e) {
            // Log
            e.printStackTrace();
        }
    }

    private URI getDiscoveryUriWithPath(String path) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }
}
