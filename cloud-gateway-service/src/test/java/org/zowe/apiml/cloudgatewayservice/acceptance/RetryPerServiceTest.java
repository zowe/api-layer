/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@AcceptanceTest
class RetryPerServiceTest extends AcceptanceTestWithTwoServices {

    @Nested
    class GivenRetryOnAllOperationsIsDisabled {
        @Test
        void whenGetReturnsUnavailable_thenRetry() throws Exception {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockUnavailableHttpResponseWithEntity(503);
            given()
                .header("X-Request-Id", "serviceid2localhost")
            .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
            .then().statusCode(is(SC_SERVICE_UNAVAILABLE));
            verify(mockClient, times(6)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

        @Test
        void whenRequestReturnsUnauthorized_thenDontRetry() throws Exception {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockUnavailableHttpResponseWithEntity(401);
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .post(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            verify(mockClient, times(2)).execute(ArgumentMatchers.any(HttpUriRequest.class));
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .put(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            verify(mockClient, times(3)).execute(ArgumentMatchers.any(HttpUriRequest.class));
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .delete(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            verify(mockClient, times(4)).execute(ArgumentMatchers.any(HttpUriRequest.class));
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .patch(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_UNAUTHORIZED));
            verify(mockClient, times(5)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

        @Test
        void whenPostReturnsUnavailable_thenDontRetry() throws Exception {
            applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            mockUnavailableHttpResponseWithEntity(503);
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .post(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_SERVICE_UNAVAILABLE));
            verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }

        @Test
        void whenPostReturnsUnavailable_thenRetry() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
        mockUnavailableHttpResponseWithEntity(503);
        discoveryClient.createRefreshCacheEvent();
        given()
            .header("X-Request-Id", "serviceid2localhost")
            .when()
            .post(basePath + serviceWithDefaultConfiguration.getPath())
            .then().statusCode(is(SC_SERVICE_UNAVAILABLE));
        verify(mockClient, times(6)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        }
    }
}
