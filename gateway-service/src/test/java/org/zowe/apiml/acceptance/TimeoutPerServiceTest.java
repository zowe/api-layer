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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;

import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@AcceptanceTest
@Disabled
public class TimeoutPerServiceTest extends AcceptanceTestWithTwoServices {
    private int SECOND = 1000;

    @Test
    void givenDefaultConfiguration_whenRequestIsCreated_thenTheTimeoutsAreTakenFromDefaultConfig() throws IOException {
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

        when()
            .get(basePath + serviceWithDefaultConfiguration.getPath())
            .then()
            .statusCode(is(SC_OK));

        assertConfigurationTimeouts(30 * SECOND);
    }

    @Test
    void givenOverwriteOfConfiguration_whenRequestIsCreated_thenTheTimeoutIsOverriden() throws IOException { // No overwrite happened here
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());

        discoveryClient.createRefreshCacheEvent();
        when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .statusCode(is(SC_OK));

        assertConfigurationTimeouts(5 * SECOND);
    }

    @Test
    void givenServiceWithOverwritenTimeoutAndAnotherWithout_whenOverwritingConfigurationForOneService_thenTheOtherServicesKeepDefault() throws IOException {
        mockValid200HttpResponse();

        // Properly set configuration for service with custom Timeout configuration
        applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
        discoveryClient.createRefreshCacheEvent();
        // Tell the infrastructure to work with the request for serviceid
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());

        when()
            .get(basePath + serviceWithDefaultConfiguration.getPath())
            .then()
            .statusCode(is(SC_OK));

        assertConfigurationTimeouts(30 * SECOND);
    }

    private void assertConfigurationTimeouts(int timeout) throws IOException {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(mockClient, times(1)).execute(captor.capture());

        HttpUriRequest toVerify = captor.getValue();
        RequestConfig configuration = ((HttpRequestBase) toVerify).getConfig();

        assertThat(configuration.getConnectTimeout(), is(timeout));
        assertThat(configuration.getSocketTimeout(), is(timeout));
    }
}
