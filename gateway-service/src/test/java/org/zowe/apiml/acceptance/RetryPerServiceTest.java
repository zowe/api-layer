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

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@AcceptanceTest
@Disabled
public class RetryPerServiceTest extends AcceptanceTestWithTwoServices {

    @Test
    void givenRetryOnAllOperationsIsDisabled_whenGetReturnsUnavailable_thenRetry() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockUnavailableHttpResponseWithEntity(503);
        when().get(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_SERVICE_UNAVAILABLE));
        verify(mockClient, times(6)).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    void givenRetryOnAllOperationsIsDisabled_whenRequestReturnsUnauthorized_thenDontRetry() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockUnavailableHttpResponseWithEntity(401);
        when().get(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_UNAUTHORIZED));
        verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        when().post(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_UNAUTHORIZED));
        verify(mockClient, times(2)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        when().put(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_UNAUTHORIZED));
        verify(mockClient, times(3)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        when().delete(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_UNAUTHORIZED));
        verify(mockClient, times(4)).execute(ArgumentMatchers.any(HttpUriRequest.class));
        when().patch(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_UNAUTHORIZED));
        verify(mockClient, times(5)).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    void givenRetryOnAllOperationsIsDisabled_whenPostReturnsUnavailable_thenDontRetry() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockUnavailableHttpResponseWithEntity(503);
        when().post(basePath + serviceWithDefaultConfiguration.getPath()).then().statusCode(is(SC_SERVICE_UNAVAILABLE));
        verify(mockClient, times(1)).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    void givenRetryOnAllOperationsIsEnabled_whenPostReturnsUnavailable_thenRetry() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
        mockUnavailableHttpResponseWithEntity(503);
        discoveryClient.createRefreshCacheEvent();
        when().post(basePath + serviceWithCustomConfiguration.getPath()).then().statusCode(is(SC_SERVICE_UNAVAILABLE));
        verify(mockClient, times(6)).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }
}
