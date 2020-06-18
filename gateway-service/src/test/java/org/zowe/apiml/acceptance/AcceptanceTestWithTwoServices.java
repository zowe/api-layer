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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zowe.apiml.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@AcceptanceTest
public class AcceptanceTestWithTwoServices extends AcceptanceTestWithBasePath {
    @Autowired
    @Qualifier("mockProxy")
    CloseableHttpClient mockClient;
    @Autowired
    ApimlDiscoveryClientStub discoveryClient;
    @Autowired
    ApplicationRegistry applicationRegistry;

    @BeforeEach
    public void prepareApplications() {
        applicationRegistry.clearApplications();
        applicationRegistry.addApplication("/serviceid2/test", "/serviceid2/**", "serviceid2", false);
        applicationRegistry.addApplication("/serviceid1/test", "/serviceid1/**", "serviceid1",true);
    }

    protected void mockValid200HttpResponse() throws IOException {
        mockValid200HttpResponseWithHeaders(new Header[]{});
    }

    protected void mockValid200HttpResponseWithHeaders(org.apache.http.Header[] headers) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, ""));
        Mockito.when(response.getAllHeaders()).thenReturn(headers);
        Mockito.when(mockClient.execute(any())).thenReturn(response);
    }
}
