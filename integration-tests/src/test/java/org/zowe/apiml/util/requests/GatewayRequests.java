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
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.Credentials;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.*;

@Slf4j
public class GatewayRequests {
    private static final GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static final Credentials credentials = ConfigReader.environmentConfiguration().getCredentials();
    private static final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();

    private final Requests requests = new Requests();
    private final String scheme;
    private final String host;
    private final int port;
    private final String instance;

    public GatewayRequests() {
        this(gatewayServiceConfiguration.getScheme(), gatewayServiceConfiguration.getHost(), gatewayServiceConfiguration.getExternalPort());
    }

    public GatewayRequests(String scheme, String host, int port) {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        this.scheme = scheme;
        this.host = host;
        this.port = port;

        instance = String.format("%s://%s:%s", scheme, host, port);
        log.info("Created gateway requests for: {}", instance);
    }

    public void shutdown() {
        log.info("GatewayRequests#shutdown Instance: {}", instance);

        try {
            given()
                .contentType(JSON)
                .auth().basic(credentials.getUser(), new String(credentials.getPassword()))
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
            log.info("GatewayRequests#isUp Instance: {}", instance);

            ReadContext healthResponse = requests.getJson(getGatewayUriWithPath(Endpoints.HEALTH));
            String health = healthResponse.read("$.status");

            return health.equals("UP");
        } catch (Exception e) {
            log.info("GatewayRequests#isUP", e);

            return false;
        }
    }

    public String login() {
        log.info("GatewayRequest#login Default credentials");

        return gatewayToken();
    }

    public String refresh(String token) {
        try {
            log.info("GatewayRequests#refresh Token to be refreshed: {}", token);

            return given()
                .cookie(COOKIE_NAME, token)
            .when()
                .post(getGatewayUriWithPath(authConfigurationProperties.getGatewayRefreshEndpoint()))
            .then()
                .statusCode(is(SC_NO_CONTENT))
                .cookie(GATEWAY_TOKEN_COOKIE_NAME, not(isEmptyString()))
                .extract().cookie(GATEWAY_TOKEN_COOKIE_NAME);
        } catch (URISyntaxException e) {
            log.info("GatewayRequests#refresh", e);

            throw new RuntimeException("Incorrect URI");
        }
    }

    public JsonResponse route(String path) {
        try {
            log.info("GatewayRequests#route - {} Instance: {}", path, instance);

            return requests.getJsonResponse(getGatewayUriWithPath(path));
        } catch (URISyntaxException e) {
            log.info("GatewayRequests#route - {}", path, e);

            throw new RuntimeException("Incorrect URI");
        }
    }

    public JsonResponse authenticatedRoute(String path, String jwt, String cookieName) {
        try {
            log.info("GatewayRequests#authenticatedRoute - {} Instance: {}", path, instance);


            log.info("GatewayRequests#authenticatedRoute - {} Token: {}", path, jwt);

            return requests.getJsonResponse(getGatewayUriWithPath(path), jwt, cookieName);
        } catch (URISyntaxException e) {
            log.info("GatewayRequests#route - {}", path, e);

            throw new RuntimeException("Incorrect URI");
        }
    }

    public URI getGatewayUriWithPath(String path) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }
}
