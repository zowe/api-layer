/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.element.Node;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoveryServiceConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.LOCATION;

/**
 * This is an integration test class for PageRedirectionFilter.java
 * It tests PageRedirectionFilter with service staticclient which is defined staticclient.yml
 * <ol>
 * The test scenario is:
 * <li>Send POST request to PageRedirectionController, the request body contains a service url</li>
 * <li>When PageRedirectionController gets the POST request, it takes out the url from request body, sets Location
 * Response header to the url, and returns status code 307</li>
 * <li>The response should then be filtered by PageRedirectionFilter</li>
 * <li>Checks the Location in response header, verify that it has been tranformed to gateway url</li>
 * </ol>
 */
class PageRedirectionTest {
    private final String EUREKA_APP = "/eureka/apps";
    private final String SERVICE_ID = "staticclient";
    private final String BASE_URL = "/discoverableclient";
    private final String API_POSTFIX = "/api/v1";

    private String gatewayScheme;
    private String gatewayHost;
    private int gatewayPort;
    private String dcScheme;
    private String dcHost;
    private int dcPort;
    private String requestUrl;

    public PageRedirectionTest() throws URISyntaxException {
        initGatewayProperties();
        initDiscoverableClientProperties();
    }

    /**
     * Test api instance of staticclient in new path format of /{serviceId}/{typeOfService}/{version}
     */
    @Test
    @TestsNotMeantForZowe
    void apiRouteOfDiscoverableClient() {
        String apiRelativeUrl = "/api/v1";
        String location = String.format("%s://%s:%d%s%s%s", dcScheme, dcHost, dcPort, BASE_URL, apiRelativeUrl, "/greeting");
        String transformedLocation = String.format("%s://%s:%d%s%s%s", gatewayScheme, gatewayHost, gatewayPort, "/" + SERVICE_ID, API_POSTFIX, "/greeting");

        RedirectLocation redirectLocation = new RedirectLocation(location);

        given()
            .contentType(JSON)
            .body(redirectLocation)
            .when()
            .post(requestUrl)
            .then()
            .statusCode(is(HttpStatus.TEMPORARY_REDIRECT.value()))
            .header(LOCATION, transformedLocation);
    }

    /**
     * Test ws instance of staticclient in new path format of /{serviceId}/{typeOfService}/{version}
     */
    @Test
    @TestsNotMeantForZowe
    void wsRouteOfDiscoverableClient() {
        String wsRelativeUrl = "/ws";
        String location = String.format("%s://%s:%d%s%s", dcScheme, dcHost, dcPort, BASE_URL, wsRelativeUrl);
        String wsPostfix = "/ws/v1";
        String transformedLocation = String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, "/" + SERVICE_ID, wsPostfix);

        RedirectLocation redirectLocation = new RedirectLocation(location);

        given()
            .contentType(JSON)
            .body(redirectLocation)
            .when()
            .post(requestUrl)
            .then()
            .statusCode(is(HttpStatus.TEMPORARY_REDIRECT.value()))
            .header(LOCATION, transformedLocation);
    }

    /**
     * Test ui instance of staticclient in new path format of /{serviceId}/{typeOfService}/{version}
     */
    @Test
    @TestsNotMeantForZowe
    void uiRouteOfDiscoverableClient() {
        String location = String.format("%s://%s:%d%s", dcScheme, dcHost, dcPort, BASE_URL);
        String uiPostfix = "/ui/v1";
        String transformedLocation = String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, "/" + SERVICE_ID, uiPostfix);

        RedirectLocation redirectLocation = new RedirectLocation(location);

        given()
            .contentType(JSON)
            .body(redirectLocation)
            .when()
            .post(requestUrl)
            .then()
            .statusCode(is(HttpStatus.TEMPORARY_REDIRECT.value()))
            .header(LOCATION, transformedLocation);
    }

    /**
     * Initiate gateway properties, such as host, port, scheme
     */
    private void initGatewayProperties() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        gatewayScheme = gatewayServiceConfiguration.getScheme();
        gatewayHost = gatewayServiceConfiguration.getHost();
        gatewayPort = gatewayServiceConfiguration.getPort();

        RestAssured.port = gatewayPort;
        RestAssured.useRelaxedHTTPSValidation();

        requestUrl = String.format("%s://%s:%d%s%s%s", gatewayScheme, gatewayHost, gatewayPort, API_POSTFIX, "/" + SERVICE_ID, "/redirect");
    }

    /**
     * Initiate Discoverable Client properties, such as host, port, scheme
     *
     * @throws URISyntaxException
     */
    private void initDiscoverableClientProperties() throws URISyntaxException {
        DiscoveryServiceConfiguration discoveryServiceConfiguration = ConfigReader.environmentConfiguration().getDiscoveryServiceConfiguration();
        final String scheme = discoveryServiceConfiguration.getScheme();
        final String username = discoveryServiceConfiguration.getUser();
        final String password = discoveryServiceConfiguration.getPassword();
        final String host = discoveryServiceConfiguration.getHost();
        final int port = discoveryServiceConfiguration.getPort();
        URI uri = new URIBuilder().setScheme(scheme).setHost(host).setPort(port).setPath(EUREKA_APP).build();

        RestAssured.config = RestAssured.config().sslConfig(SecurityUtils.getConfiguredSslConfig());
        String xml =
            given()
                .auth().basic(username, password)
                .when()
                .get(uri)
                .then()
                .statusCode(is(200))
                .extract().body().asString();

        Node staticclientNode = XmlPath.from(xml).get("applications.application.find {it.name == 'STATICCLIENT'}");
        Node instanceNode = staticclientNode.children().get("instance");
        dcHost = instanceNode.children().get("hostName").toString();
        Node securePortNode = instanceNode.children().get("securePort");
        if (securePortNode.getAttribute("enabled").equalsIgnoreCase("true")) {
            dcScheme = "https";
            dcPort = Integer.parseInt(securePortNode.value());
        } else {
            dcScheme = "http";
            dcPort = Integer.parseInt(instanceNode.children().get("port").toString());
        }
    }


    static class RedirectLocation {

        private String location;

        RedirectLocation(String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
