/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.corsTests;

import io.restassured.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@AcceptanceTest
@ActiveProfiles({"GatewayCorsEnabledTest", "test"})
@TestPropertySource(properties = {
    "apiml.service.corsDefaultAllowedOrigins=https://foo.bar.org",
    "apiml.service.corsEnabled=true"
})
@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class GatewayCorsEnabledTestWithProvidedDefault extends AcceptanceTestWithTwoServices {

    @Test
    // The CORS headers are properly set on the request
    void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() throws Exception {
        // Preflight request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + "/gateway/version")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin","https://foo.bar.org")
            .header("Access-Control-Allow-Methods", "GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS")
            .header("Access-Control-Allow-Headers", "origin, x-requested-with");

        // Actual request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .get(basePath + "/gateway/version")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", "https://foo.bar.org");
    }

    @Test
    void givenCorsOriginIsNotAllowed_whenPreFlightRequestArrives_thenCorsHeadersAreNotSet() throws Exception {
        // Origin with other than foo.bar.org should be rejected when cors is enabled
    }

    private void mockValid200HttpResponseWithAddedCors() throws IOException {
        mockValid200HttpResponseWithHeaders(new org.apache.http.Header[]{
            new BasicHeader("Access-Control-Allow-Origin", "test"),
            new BasicHeader("Access-Control-Allow-Methods", "RANDOM"),
            new BasicHeader("Access-Control-Allow-Headers", "origin,x-test"),
            new BasicHeader("Access-Control-Allow-Credentials", "true"),
        });
        applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
        discoveryClient.createRefreshCacheEvent();
    }

    @Test
    // There is request to the southbound server for the request
    // The CORS header is properly set.
    void givenCorsIsAllowedForSpecificService_whenSimpleRequestArrives_thenCorsHeadersAreSetAndOnlyTheOnesByGateway() throws Exception {
        // There is request to the southbound server and the CORS headers are properly set on the response
        mockValid200HttpResponseWithAddedCors();

        // Preflight request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is("https://foo.bar.org"));

        // The actual request is passed to the southbound service
        verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    // There is no request to the southbound server for preflight
    // There is request to the southbound server for the second request
    void givenCorsIsAllowedForSpecificService_whenTheServiceIsSet_thenCorsHeadersAreSetAndOnlyTheOnesByGateway() throws Exception {
        mockValid200HttpResponseWithAddedCors();

        // Preflight request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + serviceWithCustomConfiguration.getPath())
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is("https://foo.bar.org"))
            .header("Access-Control-Allow-Methods", is("GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS"))
            .header("Access-Control-Allow-Headers", is("origin, x-requested-with"));

        // The preflight request isn't passed to the southbound service
        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));

        // Actual request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .post(basePath + serviceWithCustomConfiguration.getPath())
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is("https://foo.bar.org"));

        // The actual request is passed to the southbound service
        verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    void givenCorsIsEnabled_whenRequestWithOriginComes_thenOriginIsntPassedToSouthbound() throws Exception {
        // There is request to the southbound server and the CORS headers are properly set on the response
        mockValid200HttpResponseWithAddedCors();

        // Simple request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is("https://foo.bar.org"));

        // The actual request is passed to the southbound service
        verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(mockClient, times(1)).execute(captor.capture());

        HttpUriRequest toVerify = captor.getValue();
        org.apache.http.Header[] originHeaders = toVerify.getHeaders("Origin");
        assertThat(originHeaders, arrayWithSize(0));
    }

    @Nested
    @AcceptanceTest
    @ActiveProfiles({"GatewayCorsEnabledTestWithDefaults", "test"})
    @TestPropertySource(properties = {
        "apiml.service.corsEnabled=true"
    })
    class GatewayCorsEnabledTestWithDefaults {
        // Gateway uses a default list of origins, does not accept any

    }

    @Nested
    @AcceptanceTest
    @ActiveProfiles({"GatewayCorsEnabledTestWithDefaults", "test"})
    @TestPropertySource(properties = {
        "apiml.service.corsEnabled=false"
    })
    class GatewayCorsDisabled {
        // Gateway does not interfere, all headers are passed to the southbound service

    }

}
