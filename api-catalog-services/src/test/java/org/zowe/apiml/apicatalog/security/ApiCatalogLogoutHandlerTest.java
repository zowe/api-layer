/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiCatalogLogoutHandlerTest {

    @Mock private AuthConfigurationProperties authConfigurationProperties;
    @Mock private CloseableHttpClient httpClient;

    private ApiCatalogLogoutHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new ApiCatalogLogoutHandler(httpClient, authConfigurationProperties, "https", "localhost:10010");
        when(authConfigurationProperties.getGatewayLogoutEndpoint()).thenReturn("/gateway/api/v1/auth/logout");
    }

    @Nested
    class GivenLogoutHandler {

        @Mock private CloseableHttpResponse logoutResponse;
        @Mock private StatusLine status;

        private MockHttpServletRequest request;
        private MockHttpServletResponse response;

        @BeforeEach
        void setUp() throws ClientProtocolException, IOException {
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            lenient().when(httpClient.execute(any())).thenReturn(logoutResponse);
            lenient().when(logoutResponse.getStatusLine()).thenReturn(status);
        }

        private void requestWithAuthentication() {
            request = new MockHttpServletRequest();
            request.addHeader("cookie", "apimlAuthenticationToken=token");
            request.addHeader("authorization", "Bearer token");
        }

        @Test
        void success_WithCredentials() throws ClientProtocolException, IOException {
            requestWithAuthentication();

            when(status.getStatusCode()).thenReturn(204);

            assertDoesNotThrow(() -> handler.logout(request, response, null));

            verify(httpClient, times(1)).execute(argThat(
                postRequest -> {
                    assertEquals("https://localhost:10010/gateway/api/v1/auth/logout", postRequest.getURI().toString());
                    assertTrue(postRequest.getHeaders(HttpHeaders.COOKIE).length > 0);
                    assertTrue(postRequest.getHeaders(HttpHeaders.AUTHORIZATION).length > 0);
                    assertEquals("Cookie: apimlAuthenticationToken=token", postRequest.getHeaders(HttpHeaders.COOKIE)[0].toString());
                    assertEquals("Authorization: Bearer token", postRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].toString());
                    return true;
                }
            ));
        }

        @Test
        void whenInternalProtocolHttp_thenUnsecureAttls() throws ClientProtocolException, IOException {
            ReflectionTestUtils.setField(handler, "internalProtocol", "http");
            requestWithAuthentication();

            when(status.getStatusCode()).thenReturn(204);
            assertDoesNotThrow(() -> handler.logout(request, response, null));

            verify(httpClient, times(1)).execute(argThat(
                postRequest -> {
                    assertEquals("http://localhost:10010/gateway/api/v1/auth/logout", postRequest.getURI().toString());
                    assertTrue(postRequest.getHeaders(HttpHeaders.COOKIE).length > 0);
                    assertTrue(postRequest.getHeaders(HttpHeaders.AUTHORIZATION).length > 0);
                    assertEquals("Cookie: apimlAuthenticationToken=token", postRequest.getHeaders(HttpHeaders.COOKIE)[0].toString());
                    assertEquals("Authorization: Bearer token", postRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].toString());
                    return true;
                }
            ));
        }

        @Test
        void whenNotSuccessfulReturnCode_thenDoesNotThrow() {
            requestWithAuthentication();

            when(status.getStatusCode()).thenReturn(500);
            assertDoesNotThrow(() -> handler.logout(request, response, null));
        }

        @Test
        void whenClientError_thenDoesNotThrow() throws ClientProtocolException, IOException {
            doThrow(new ClientProtocolException()).when(httpClient).execute(any());
            assertDoesNotThrow(() -> handler.logout(request, response, null));
        }

    }

}
