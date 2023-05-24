/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.token;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OIDCTokenProviderTest {

    private OIDCTokenProvider oidcTokenProvider;
    private CloseableHttpClient httpClient;

    private StatusLine responseStatusLine;
    private BasicHttpEntity responseEntity;

    private static final String BODY = "{\n" +
        "    \"active\": true,\n" +
        "    \"scope\": \"scope\",\n" +
        "    \"exp\": 1664538493,\n" +
        "    \"iat\": 1664534893,\n" +
        "    \"sub\": \"sub\",\n" +
        "    \"aud\": \"aud\",\n" +
        "    \"iss\": \"iss\",\n" +
        "    \"jti\": \"jti\",\n" +
        "    \"token_type\": \"Bearer\",\n" +
        "    \"client_id\": \"id\"\n" +
        "}";

    private static final String NOT_VALID_BODY = "{\n" +
        "    \"active\": false\n" +
        "}";

    private static final String TOKEN = "token";

    @BeforeEach
    void setup() throws CachingServiceClientException, IOException {
        httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        responseStatusLine = mock(StatusLine.class);
        responseEntity = new BasicHttpEntity();
        responseEntity.setContent(IOUtils.toInputStream("", StandardCharsets.UTF_8));
        when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getStatusLine()).thenReturn(responseStatusLine);
        when(response.getEntity()).thenReturn(responseEntity);
        when(httpClient.execute(any())).thenReturn(response);
        oidcTokenProvider = new OIDCTokenProvider(httpClient);
        oidcTokenProvider.introspectUrl = "https://acme.com/introspect";
        oidcTokenProvider.clientId = "client_id";
        oidcTokenProvider.clientSecret = "client_secret";
    }

    @Nested
    class GivenTokenForValidation {
        @Test
        void tokenIsActive_thenReturnValid() {
            responseEntity.setContent(IOUtils.toInputStream(BODY, StandardCharsets.UTF_8));
            assertTrue(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void tokenIsExpired_thenReturnInvalid() {
            responseEntity.setContent(IOUtils.toInputStream(NOT_VALID_BODY, StandardCharsets.UTF_8));
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenClientThrowsException_thenReturnInvalid() throws IOException {
            ClientProtocolException exception = new ClientProtocolException("http error");
            when(httpClient.execute(any())).thenThrow(exception);
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenResponseIsNotValidJson_thenReturnInvalid() {
            responseEntity.setContent(IOUtils.toInputStream("{notValid}", StandardCharsets.UTF_8));
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenResponseStatusIsNotOk_thenReturnInvalid() {
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

    }

    @Nested
    class GivenEmptyTokenProvided {
        @Test
        void whenTokenIsNull_thenReturnInvalid() {
            assertFalse(oidcTokenProvider.isValid(null));
        }

        @Test
        void whenTokenIsEmpty_thenReturnInvalid() {
            assertFalse(oidcTokenProvider.isValid(""));
        }
    }
    @Nested
    class GivenInvalidConfiguration {
        @Test
        void whenIntrospectUrlIsNull_thenReturnInvalid() {
            oidcTokenProvider.introspectUrl = null;
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }
        @Test
        void whenIntrospectUrlIsEmpty_thenReturnInvalid() {
            oidcTokenProvider.introspectUrl = "";
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenIntrospectUrlIsInvalid_thenReturnInvalid() {
            oidcTokenProvider.introspectUrl = "{not_an_URL}";
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenClientIdIsNull_thenReturnInvalid() {
            oidcTokenProvider.clientId = null;
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }
        @Test
        void whenClientIdIsEmpty_thenReturnInvalid() {
            oidcTokenProvider.clientId = "";
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenClientSecretIsNull_thenReturnInvalid() {
            oidcTokenProvider.clientSecret = null;
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }
        @Test
        void whenClientSecretIsEmpty_thenReturnInvalid() {
            oidcTokenProvider.clientSecret = "";
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }
    }

}
