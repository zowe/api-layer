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
import org.apache.http.client.utils.URIBuilder;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.Credentials;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@Slf4j
public class GatewayRequests {
    private static final GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static final Credentials credentials = ConfigReader.environmentConfiguration().getCredentials();

    private final Requests requests;
    private final String scheme;
    private final String host;
    private final int port;

    public GatewayRequests(String host, String port) {
        this(gatewayServiceConfiguration.getScheme(), host, Integer.parseInt(port), new Requests());
    }

    public GatewayRequests(String scheme, String host, int port, Requests requests) {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        this.requests = requests;
        this.scheme = scheme;
        this.host = host;
        this.port = port;

        log.info("Created gateway requests for: {}{}:{}", scheme, host, port);
    }

    public void shutdown() {
        log.info("GatewayRequests#shutdown");

        try {
            given()
                .contentType(JSON)
                .auth().basic(credentials.getUser(), credentials.getPassword())
            .when()
                .post(getGatewayUriWithPath(Endpoints.SHUTDOWN))
            .then()
                .statusCode(is(SC_OK));
        } catch (Exception e) {
            log.info("GatewayRequests#shutdown", e);
        }
    }

    public boolean isUp() {
        try {
            log.info("GatewayRequests#isUp");

            ReadContext healthResponse = requests.getJson(getGatewayUriWithPath(Endpoints.HEALTH));
            String health = healthResponse.read("$.status");

            return health.equals("UP");
        } catch (Exception e) {
            log.info("GatewayRequests#isUP", e);

            return false;
        }
    }

    public JsonResponse route(String path) {
        try {
            log.info("GatewayRequests#route - {}", path);

            return requests.getJsonResponse(getGatewayUriWithPath(path));
        } catch (URISyntaxException e) {
            log.info("GatewayRequests#route - {}", path, e);

            throw new RuntimeException("Incorrect URI");
        }
    }

    private URI getGatewayUriWithPath(String path) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }
}
