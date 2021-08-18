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
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.Credentials;
import org.zowe.apiml.util.config.DiscoverableClientConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;

@Slf4j
public class DiscoverableClientRequests {

    private static final DiscoverableClientConfiguration discoverableClientConfiguration =  ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration();
    private static final Credentials credentials = ConfigReader.environmentConfiguration().getCredentials();

    private final Requests requests;
    private final String scheme;
    private final String host;
    private final int port;
    private final String instance;

    public DiscoverableClientRequests(String host) {
        this(discoverableClientConfiguration.getScheme(), host, discoverableClientConfiguration.getPort(), new Requests());
    }
    public DiscoverableClientRequests(String scheme, String host, int port, Requests requests) {
        this.requests = requests;
        this.scheme = scheme;
        this.host = host;
        this.port = port;

        instance = String.format("%s://%s:%s", scheme, host, port);

        log.info("Created discoverable client requests for: {}", instance);
    }

    public boolean isUp() {
        try {
            log.info("DiscoverableClientRequests#isUp Instance: {}", instance);

            ReadContext healthResponse = requests.getJson(getDiscoverableClientUriWithPath("/discoverableclient" + Endpoints.HEALTH));
            String health = healthResponse.read("$.status");

            return health.equals("UP");
        } catch (Exception e) {
            log.info("DiscoverableClientRequests#isUP", e);

            return false;
        }
    }

    public void shutdown() {
        log.info("DiscoverableClientRequests#shutdown Instance: {}", instance);

        try {
            given()
                .contentType(JSON)
                .auth().basic(credentials.getUser(), credentials.getPassword())
                .when()
                .post(getDiscoverableClientUriWithPath(Endpoints.SHUTDOWN))
                .then()
                .statusCode(is(SC_OK));
        } catch (Exception e) {
            log.info("DiscoverableClientRequests#shutdown", e);
        }
    }

    private URI getDiscoverableClientUriWithPath(String path) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(scheme)
            .setHost(host)
            .setPort(port)
            .setPath(path)
            .build();
    }

}
