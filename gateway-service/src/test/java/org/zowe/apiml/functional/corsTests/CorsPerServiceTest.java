/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.corsTests;

import io.restassured.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.functional.common.AcceptanceTest;
import org.zowe.apiml.functional.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

/*
 * Verify default state
 *    - With pre-flight request
 *       The No Access Control Allow Origin header
 *    - Without pre-flight request
 *       The header for Access Control Allow Origin isn't present
 *  Verify changed state
 *    - With pre-flight request
 *      - Verify that the downstream headers for CORS are ignored
 *    - Without pre-flight request
 *      - Verify that the downstream headers for CORS are ignored
 *
 *  The case for downstream headers will be?
 *     With disabled by default not much.
 *     Is there case when disabled cors would mean that we will have to change it?
 *     When allowed the headers are also irelevant as the headers adds no behavior
 *  What can the headers do?
 *  If the pre-flight request comes and we
 */
@AcceptanceTest
@ActiveProfiles("test")
@Disabled
class CorsPerServiceTest extends AcceptanceTestWithTwoServices {
    @Nested
    class GivenCorsIsDelegatedToGatewayButServiceDoesntAllowCors {

        @Test
        // Verify the header to allow CORS isn't set
        // Verify there was no call to southbound service
        void whenPreflightRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();
            discoveryClient.createRefreshCacheEvent();

            given()
                .header(new Header("Origin", "https://foo.bar.org"))
                .header(new Header("Access-Control-Request-Method", "POST"))
                .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
            .when()
                .options(basePath + serviceWithDefaultConfiguration.getPath())
            .then()
                .statusCode(is(SC_FORBIDDEN))
                .header("Access-Control-Allow-Origin", is(nullValue()));

            verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

        @Test
        // Verify the header to allow CORS isn't set
        // Verify there was no call to southbound service
        void whenSimpleCorsRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockValid200HttpResponse();
            discoveryClient.createRefreshCacheEvent();

            given()
                .header(new Header("Origin", "https://foo.bar.org"))
                .header(new Header("Access-Control-Request-Method", "POST"))
                .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
            .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
            .then()
                .statusCode(is(SC_FORBIDDEN))
                .header("Access-Control-Allow-Origin", is(nullValue()));

            verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }
    }

    @Nested
    class GivenCorsIsAllowedForSpecificService {

        @Test
        // There is no request to the southbound server for preflight
        // There is request to the southbound server for the second request
        void whenPreFlightRequestArrives_thenCorsHeadersAreSet() throws Exception {
            mockValid200HttpResponse();
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            discoveryClient.createRefreshCacheEvent();
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
                .header("Access-Control-Allow-Methods", is("GET,HEAD,POST,DELETE,PUT,OPTIONS"))
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
        // There is request to the southbound server for the request
        // The CORS header is properly set.
        void whenSimpleRequestArrives_thenCorsHeadersAreSet() throws Exception {
            // There is request to the southbound server and the CORS headers are properly set on the response
            mockValid200HttpResponse();
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            discoveryClient.createRefreshCacheEvent();

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

    }
}
