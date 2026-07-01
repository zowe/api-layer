/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.corsTests;

import io.restassured.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;
import com.sun.net.httpserver.Headers;


import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class GatewayCorsTest {

    @Nested
    @MicroservicesAcceptanceTest
    @ActiveProfiles({"GatewayCorsEnabledWithProvidedDefaultTest"})
    @TestPropertySource(properties = {
        "apiml.service.corsDefaultAllowedOrigins=https://foo.bar.org",
        "apiml.service.corsEnabled=true"
    })
    class GatewayCorsEnabledWithProvidedDefaultTest extends AcceptanceTestWithMockServices {

        private MockService mockService;

        @BeforeEach
        void setUp() {
            var responseHeaders = new Headers();
            responseHeaders.add("Access-Control-Allow-Origin", "test");
            responseHeaders.add("Access-Control-Allow-Methods", "RANDOM");
            responseHeaders.add("Access-Control-Allow-Headers", "origin,x-test");
            responseHeaders.add("Access-Control-Allow-Credentials", "true");

            mockService = mockService("servicewithcors")
                .addEndpoint("/servicewithcors/fullheaders")
                .headers(responseHeaders)
            .and()
            .scope(Scope.TEST)
            .start();
        }

        @Test
        // The CORS headers are properly set on the request
        void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() {
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
            // Preflight request with disallowed origin
            given()
                .header(new Header("Origin", "https://malicious.example.com"))
                .header(new Header("Access-Control-Request-Method", "POST"))
                .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
            .when()
                .options(basePath + "/servicewithcors/api/v1/fullheaders")
            .then()
                .statusCode(is(SC_FORBIDDEN))
                .header("Access-Control-Allow-Origin", is((String) null));
        }

        @Test
        // There is request to the southbound server for the request
        // The CORS header is properly set.
        void givenCorsIsAllowedForSpecificService_whenSimpleRequestArrives_thenCorsHeadersAreSetAndOnlyTheOnesByGateway() throws Exception {
            // There is request to the southbound server and the CORS headers are properly set on the response
            // Preflight request
            given()
                .header(new Header("Origin", "https://foo.bar.org"))
            .when()
                .get(basePath /*+ serviceWithCustomConfiguration.getPath()*/)
            .then()
                .statusCode(is(SC_OK))
                .header("Access-Control-Allow-Origin", is("https://foo.bar.org"));

            // The actual request is passed to the southbound service
            // verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class)); USE MOCK SERVICE ASSERTION?
        }

        @Test
        // There is no request to the southbound server for preflight
        // There is request to the southbound server for the second request
        void givenCorsIsAllowedForSpecificService_whenTheServiceIsSet_thenCorsHeadersAreSetAndOnlyTheOnesByGateway() throws Exception {
            // mockValid200HttpResponseWithAddedCors();

            // Preflight request
            given()
                .header(new Header("Origin", "https://foo.bar.org"))
                .header(new Header("Access-Control-Request-Method", "POST"))
                .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
            .when()
                .options(basePath /*+ serviceWithCustomConfiguration.getPath()*/)
            .then()
                .statusCode(is(SC_OK))
                .header("Access-Control-Allow-Origin", is("https://foo.bar.org"))
                .header("Access-Control-Allow-Methods", is("GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS"))
                .header("Access-Control-Allow-Headers", is("origin, x-requested-with"));

            // The preflight request isn't passed to the southbound service
            // verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));

            // Actual request
            given()
                .header(new Header("Origin", "https://foo.bar.org"))
            .when()
                .post(basePath /*+ serviceWithCustomConfiguration.getPath()*/)
            .then()
                .statusCode(is(SC_OK))
                .header("Access-Control-Allow-Origin", is("https://foo.bar.org"));

            // The actual request is passed to the southbound service
            // verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

        @Test
        void givenCorsIsEnabled_whenRequestWithOriginComes_thenOriginIsntPassedToSouthbound() throws Exception {
            // There is request to the southbound server and the CORS headers are properly set on the response
            // mockValid200HttpResponseWithAddedCors();

            // Simple request
            given()
                .header(new Header("Origin", "https://foo.bar.org"))
            .when()
                .get(basePath /*+ serviceWithCustomConfiguration.getPath()*/)
            .then()
                .statusCode(is(SC_OK))
                .header("Access-Control-Allow-Origin", is("https://foo.bar.org"));

            // The actual request is passed to the southbound service
            // verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));

            var captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            // verify(mockClient, times(1)).execute(captor.capture());

            HttpUriRequest toVerify = captor.getValue();
            org.apache.http.Header[] originHeaders = toVerify.getHeaders("Origin");
            assertThat(originHeaders, arrayWithSize(0));
        }

    }

    @Nested
    @MicroservicesAcceptanceTest
    @ActiveProfiles({"GatewayCorsEnabledWithDefaultsTest"})
    @TestPropertySource(properties = {
        "apiml.service.corsEnabled=true"
    })
    class GatewayCorsEnabledWithDefaultsTest extends AcceptanceTestWithMockServices {
        // Gateway uses a default list of origins, does not accept any (*)

        @Test
        void givenCorsIsEnabledWithDefaults_whenPreflightRequestComes_thenPreflightIsRejected() throws Exception {
            // Preflight request with origin that should be rejected by default CORS policy
            given()
                .log().all()
                .header(new Header("Origin", "https://foo.bar.org"))
                .header(new Header("Access-Control-Request-Method", "POST"))
                .header(new Header("Access-Control-Request-Headers", "Content-Type"))
            .when()
                .options(basePath /*+ serviceWithCustomConfiguration.getPath()*/)
            .then()
                .log().all()
                .statusCode(is(SC_FORBIDDEN));

            // No request should be passed to the southbound service for preflight
            // verify(mockClient, times(0)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

        @Test
        void givenCorsIsEnabledWithDefaults_whenPreflightRequestWithLocalhostOriginComes_thenPreflightIsAccepted() throws Exception {
            // Preflight request with localhost origin that should be accepted by default CORS policy
            given()
                .log().all()
                .header(new Header("Origin", "https://localhost:" + port))
                .header(new Header("Access-Control-Request-Method", "POST"))
                .header(new Header("Access-Control-Request-Headers", "Content-Type"))
            .when()
                .options(basePath /*+ serviceWithCustomConfiguration.getPath()*/)
            .then()
            .log().all()
                .statusCode(is(SC_OK));

            // No request should be passed to the southbound service for preflight
            // verify(mockClient, times(0)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

    }

}

