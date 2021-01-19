/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util;

import io.restassured.RestAssured;
import io.restassured.path.xml.XmlPath;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

public class DiscoveryRequests {
    private DiscoveryServiceConfiguration discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
    private final String scheme = discoveryServiceConfiguration.getScheme();
    private final String host = discoveryServiceConfiguration.getHost();
    private final int port = discoveryServiceConfiguration.getPort();

    public void getApplications() throws URISyntaxException {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        XmlPath result = given()
        .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
        .then()
            .statusCode(is(HttpStatus.SC_OK)).extract().body().xmlPath();

        System.out.println(result);
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
