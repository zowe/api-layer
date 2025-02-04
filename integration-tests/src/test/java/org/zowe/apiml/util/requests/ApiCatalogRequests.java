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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.Matchers;
import org.zowe.apiml.util.config.ApiCatalogServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.Credentials;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;

@Slf4j
public class ApiCatalogRequests {
    private static final ApiCatalogServiceConfiguration apiCatalogServiceConfiguration = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration();
    private static final Credentials credentials = ConfigReader.environmentConfiguration().getCredentials();

    private final String scheme;
    private final String host;
    private final int port;
    private final String instance;

    public ApiCatalogRequests(String host) {
        this(apiCatalogServiceConfiguration.getScheme(), host, apiCatalogServiceConfiguration.getPort());
    }

    public ApiCatalogRequests(String scheme, String host, int port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;

        instance = String.format("%s://%s:%s", scheme, host, port);

        log.info("Created apicatalog requests for: {}", instance);
    }

    public boolean isUp() {
        try {
            log.info("ApiCatalogRequests#isUp Instance: {}", instance);

            given()
                .contentType(JSON)
                .auth()
                .basic(credentials.getUser(), new String(credentials.getPassword()))
                .when()
                .get(getApiCatalogUriWithPath("/apicatalog" + Endpoints.HEALTH))
                .then()
                .statusCode(200)
                .body("status", Matchers.is("UP"));
            return true;
        } catch (AssertionError | URISyntaxException e) {
            log.info("ApiCatalogRequests#isUP", e);

            return false;
        }
    }

    public void shutdown() {
        log.info("ApiCatalogRequests#shutdown Instance: {}", instance);

        try {
            given()
                .contentType(JSON)
                .auth().basic(credentials.getUser(), new String(credentials.getPassword()))
                .when()
                .post(getApiCatalogUriWithPath(Endpoints.SHUTDOWN))
                .then()
                .statusCode(is(SC_OK));
        } catch (Exception e) {
            log.info("ApiCatalogRequests#shutdown", e);
        }
    }

    private URI getApiCatalogUriWithPath(String path) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }

}
