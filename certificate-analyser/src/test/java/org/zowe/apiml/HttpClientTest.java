/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpClientTest {

    @Test
    void testExecuteCall() throws IOException {
        SSLContext mockSslContext = mock(SSLContext.class);
        SSLSocketFactory mockSocketFactory = mock(SSLSocketFactory.class);
        when(mockSslContext.getSocketFactory()).thenReturn(mockSocketFactory);

        HttpsURLConnection mockConnection = mock(HttpsURLConnection.class);
        when(mockConnection.getResponseCode()).thenReturn(200);

        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return mockConnection;
            }
        };

        URL url = new URL("https", "localhost", 443, "/", handler);

        HttpClient httpClient = new HttpClient(mockSslContext);
        int responseCode = httpClient.executeCall(url);

        assertEquals(200, responseCode);
        verify(mockConnection).setRequestMethod("GET");
        verify(mockConnection).setConnectTimeout(5000);
        verify(mockConnection).setReadTimeout(5000);
        verify(mockConnection).disconnect();
    }
}
