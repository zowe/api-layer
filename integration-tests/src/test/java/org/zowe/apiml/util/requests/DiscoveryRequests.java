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
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
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
@Slf4j
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

        log.info("Created discovery requests for: {}://{}:{}", scheme, host, port);
    }

    public boolean isApplicationRegistered(String appName) {
        try {
            log.info("DiscoveryRequests#isApplicationRegistered: {}", appName);

            int amountOfRegistered = getAmountOfRegisteredInstancesForService(appName);
            return amountOfRegistered > 0;
        } catch (Exception e) {
            log.info("DiscoveryRequests#isApplicationRegistered: {}", appName, e);
            return false;
        }
    }

    public boolean isUp() {
        try {
            log.info("DiscoveryRequests#isUp");

            ReadContext healthResponse = requests.getJson(getDiscoveryUriWithPath(Endpoints.HEALTH));
            String health = healthResponse.read("$.status");

            return health.equals("UP");
        } catch (Exception e) {
            log.info("DiscoveryRequests#isUp", e);

            return false;
        }
    }

    public ReadContext getJsonEurekaApps() throws URISyntaxException {
        return requests.getJson(getDiscoveryUriWithPath(Endpoints.APPLICATIONS));
    }

    public int getAmountOfRegisteredInstancesForService(String appName) {
        try {
            log.info("DiscoveryRequests#getAmountOfRegisteredInstancesForService: {}", appName);

            ReadContext result = getJsonEurekaApps();

            JSONArray amount = result.read("$.applications.application[?(@.name=~ /" + appName + "/)].instance.length()");
            return (int) amount.get(0);
        } catch (Exception e) {
            log.info("DiscoveryRequests#getAmountOfRegisteredInstancesForService", e);

            return 0;
        }
    }

    public void shutdown() {
        try {
            log.info("DiscoveryRequests#shutdown");

            given()
                .contentType(JSON)
                .auth().basic(credentials.getUser(), credentials.getPassword())
            .when()
                .post(getDiscoveryUriWithPath(Endpoints.SHUTDOWN))
            .then()
                .statusCode(is(SC_OK));
        } catch (Exception e) {
            log.info("DiscoveryRequests#shutdown", e);
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
