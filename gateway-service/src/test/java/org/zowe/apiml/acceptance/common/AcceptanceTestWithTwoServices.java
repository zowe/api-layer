/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.common;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;
import org.zowe.apiml.acceptance.netflix.MetadataBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@AcceptanceTest
public class AcceptanceTestWithTwoServices extends AcceptanceTestWithBasePath {

    @Autowired
    @Qualifier("mockProxy")
    protected CloseableHttpClient mockClient;
    @Autowired
    protected ApimlDiscoveryClientStub discoveryClient;
    @Autowired
    protected ApplicationRegistry applicationRegistry;
    @Autowired
    protected RestTemplate restTemplateWithoutKeystore;
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

    protected void mockValid200HttpResponse() throws IOException {
        mockValid200HttpResponseWithHeaders(new Header[]{});
    }

    protected void mockValid200HttpResponseWithAddedCors() throws IOException {
        mockValid200HttpResponseWithHeaders(new Header[]{
            new BasicHeader("Access-Control-Allow-Origin", "test"),
            new BasicHeader("Access-Control-Allow-Methods", "RANDOM"),
            new BasicHeader("Access-Control-Allow-Headers", "origin,x-test"),
            new BasicHeader("Access-Control-Allow-Credentials", "true"),
        });
    }


    protected void mockValid200HttpResponseWithHeaders(org.apache.http.Header[] headers) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, ""));
        Mockito.when(response.getAllHeaders()).thenReturn(headers);
        Mockito.when(response.getEntity()).thenReturn(new HttpEntityImpl("Hello worlds!".getBytes()));
        Mockito.when(mockClient.execute(any())).thenReturn(response);
    }

    protected void mockUnavailableHttpResponseWithEntity(int statusCode) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), statusCode, "fake_reason"));
        Mockito.when(response.getAllHeaders()).thenReturn(new Header[]{});
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("{foo}".getBytes()));
        Mockito.when(response.getLocale()).thenReturn(Locale.US);
        Mockito.when(mockClient.execute(any())).thenReturn(response);
    }
}

class HttpEntityImpl implements HttpEntity {

    byte[] bytes;

    public HttpEntityImpl(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public long getContentLength() {
        return bytes.length;
    }

    @Override
    public Header getContentType() {
        return null;
    }

    @Override
    public Header getContentEncoding() {
        return null;
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        InputStream is = new ByteArrayInputStream(bytes);
        return is;
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {

    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public void consumeContent() throws IOException {

    }
}
