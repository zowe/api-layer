/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance;

import io.restassured.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.LocalServerPort;
import org.zowe.apiml.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@AcceptanceTest
public class CorsPerServiceTest {
    private String basePath;

    @Autowired
    @Qualifier("mockProxy")
    CloseableHttpClient mockClient;
    @Autowired
    ApimlDiscoveryClientStub discoveryClient;
    @Autowired
    ApplicationRegistry applicationRegistry;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setBasePath() {
        basePath = String.format("https://localhost:%d/", port);

        applicationRegistry.clearApplications();
        applicationRegistry.addApplication("/serviceid2/test", "/serviceid2/**", "serviceid2", false);
        applicationRegistry.addApplication("/serviceid1/test", "/serviceid1/**", "serviceid1",true);
    }

    // Verify default state
    //   - With pre-flight request
    //      The No Access Control Allow Origin header
    //   - Without pre-flight request
    //      The header for Access Control Allow Origin isn't present
    // Verify changed state
    //   - With pre-flight request
    //     - Verify that the downstream headers for CORS are ignored
    //   - Without pre-flight request
    //     - Verify that the downstream headers for CORS are ignored

    @Test
    // Verify the header to allow CORS isn't set
    // Verify there was no call to southbound service
    void givenDefaultConfiguration_whenPreflightRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
        applicationRegistry.setCurrentApplication("/serviceid2/test");

        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + "serviceid2/test")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is(nullValue()));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    // Verify the header to allow CORS isn't set
    // Verify there was no call to southbound service
    void givenDefaultConfiguration_whenSimpleCorsRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
        applicationRegistry.setCurrentApplication("/serviceid2/test");

        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .get(basePath + "serviceid2/test")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is(nullValue()));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() {
        // There is no request to the southbound server for preflight
        // There is request to the southbound server for the second request
    }

    @Test
    void givenCorsIsAllowedForSpecificService_whenSimpleRequestArrives_thenCorsHeadersAreSet() {
        // There is request to the southbound server and the CORS headers are properly set on the response
    }

    // The case for downstream headers will be?
    //    With disabled by default not much.
    //    Is there case when disabled cors would mean that we will have to change it?
    //    When allowed the headers are also irelevant as the headers adds no behavior
    // What can the headers do?
    // If the pre-flight request comes and we
}
