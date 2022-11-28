/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.common;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.cloudgatewayservice.acceptance.netflix.ApplicationRegistry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@AcceptanceTest
public class AcceptanceTestWithTwoServices extends AcceptanceTestWithBasePath {

    @Autowired
    @Qualifier("test")
    protected ApimlDiscoveryClientStub discoveryClient;
    @Autowired
    protected ApplicationRegistry applicationRegistry;
    @Mock
    protected HttpEntity httpEntity;

    protected Service serviceWithDefaultConfiguration = new Service("serviceid2", "/serviceid2/**", "serviceid2");
    protected Service serviceWithCustomConfiguration = new Service("serviceid1", "/serviceid1/**", "serviceid1");

    @BeforeEach
    public void prepareApplications() {
        applicationRegistry.clearApplications();
        applicationRegistry.addApplication(serviceWithDefaultConfiguration, MetadataBuilder.defaultInstance(), false);
        applicationRegistry.addApplication(serviceWithCustomConfiguration, MetadataBuilder.customInstance(), false);
    }

    protected void mockUnavailableHttpResponseWithEntity(int statusCode) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), statusCode, "fake_reason"));
        Mockito.when(response.getAllHeaders()).thenReturn(new Header[]{});
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("{foo}".getBytes()));
        Mockito.when(response.getLocale()).thenReturn(Locale.US);
//        Mockito.when(mockClient.execute(any())).thenReturn(response);
    }
}
