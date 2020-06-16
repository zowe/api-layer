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

import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.server.LocalServerPort;
import org.zowe.apiml.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;

import java.io.IOException;

import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@AcceptanceTest
public class TimeoutPerServiceTest {
    private String basePath;

    @Autowired @Qualifier("mockProxy")
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

    @Test
    void givenDefaultConfiguration_whenRequestIsCreated_thenTheTimeoutsAreTakenFromDefaultConfig() throws IOException {
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication("/serviceid2/test");

        when()
            .get(basePath + "serviceid2/test")
            .then()
            .statusCode(is(SC_OK));

        assertConfigurationTimeouts(30000);
    }

    @Test
    void givenOverwriteOfConfiguration_whenRequestIsCreated_thenTheTimeoutIsOverriden() throws IOException { // No overwrite happened here
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication("/serviceid1/test");

        discoveryClient.createRefreshCacheEvent();
        when()
            .get(basePath + "serviceid1/test")
            .then()
            .statusCode(is(SC_OK));

        assertConfigurationTimeouts(5000);
    }

    @Test
    void givenServiceWithOverwritenTimeoutAndAnotherWithout_whenOverwritingConfigurationForOneService_thenTheOtherServicesKeepDefault() throws IOException {
        mockValid200HttpResponse();

        // Properly set configuration for service with custom Timeout configuration
        applicationRegistry.setCurrentApplication("/serviceid1/test");
        discoveryClient.createRefreshCacheEvent();
        // Tell the infrastructure to work with the request for serviceid
        applicationRegistry.setCurrentApplication("/serviceid2/test");

        when()
            .get(basePath + "serviceid2/test")
            .then()
            .statusCode(is(SC_OK));

        assertConfigurationTimeouts(30000);
    }

    private void assertConfigurationTimeouts(int timeout) throws IOException {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(mockClient, times(1)).execute(captor.capture());

        HttpUriRequest toVerify = captor.getValue();
        RequestConfig configuration = ((HttpRequestBase) toVerify).getConfig();

        assertThat(configuration.getConnectTimeout(), is(timeout));
        assertThat(configuration.getSocketTimeout(), is(timeout));
    }

    private void mockValid200HttpResponse() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, ""));
        Mockito.when(response.getAllHeaders()).thenReturn(new Header[]{});
        Mockito.when(mockClient.execute(any())).thenReturn(response);
    }
}
