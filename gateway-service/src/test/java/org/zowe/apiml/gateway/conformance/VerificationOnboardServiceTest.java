/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.io.IOException;


@ExtendWith(MockitoExtension.class)
public class VerificationOnboardServiceTest {

    private static final String DISCOVERY_LOCATION = "https://localhost:10011/eureka/";

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryConfigUri discoveryConfigUri;

    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Nested
    class GivenRegisteredService {
        
        @Test
        void whenServiceId_Registered() throws IOException, java.io.IOException {
            when(discoveryConfigUri.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION});

            
            StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
            CloseableHttpResponseTest closeableHttpResponseTest = new CloseableHttpResponseTest(statusLine);
        
            lenient().when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponseTest);
            Boolean registeredInfo = verificationOnboardService.checkOnboarding("gateway");

            Boolean expectedInfo = closeableHttpResponseTest.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
            assertEquals(expectedInfo, registeredInfo);
        }

        
        @Test
        void whenServiceId_Registered_RetrieveData() throws ClientProtocolException, java.io.IOException {
            final String XML_DATA = "<?xml version=\"1.0\" ?><instance><metadata><apiml.apiInfo.api-v2.swaggerUrl>https://hostname/sampleclient/api-doc</apiml.apiInfo.api-v2.swaggerUrl></metadata></instance>";
            
            when(discoveryConfigUri.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION});
            CloseableHttpResponseTest closeableHttpClientTest = new CloseableHttpResponseTest(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK", XML_DATA);
            when(closeableHttpClient.execute(any())).thenReturn(closeableHttpClientTest);
            String expectedUrl = "https://hostname/sampleclient/api-doc";
            String actualUrl = verificationOnboardService.retrieveMetaData("gateway");
            assertEquals(expectedUrl, actualUrl);
        }
        
    }
}



class CloseableHttpResponseTest extends BasicHttpResponse implements CloseableHttpResponse {

    public CloseableHttpResponseTest(StatusLine statusline, ReasonPhraseCatalog catalog, Locale locale) {
        super(statusline, catalog, locale);
    }

    public CloseableHttpResponseTest(StatusLine statusline) {
        super(statusline);
    }

    public CloseableHttpResponseTest(ProtocolVersion ver, int code, String reason) {
        super(ver, code, reason);
    }

    public CloseableHttpResponseTest(ProtocolVersion ver, int code, String reason, String entityString) {
        super(ver, code, reason);
        setEntity(new StringEntity(entityString, Charset.defaultCharset()));
    }

    @Override
    public void close() throws java.io.IOException {
        
    }

}