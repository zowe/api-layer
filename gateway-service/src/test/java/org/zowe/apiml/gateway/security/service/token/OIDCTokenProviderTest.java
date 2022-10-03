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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;
import org.zowe.apiml.gateway.security.service.schema.OIDCAuthException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OIDCTokenProviderTest {

    private OIDCTokenProvider oidcTokenProvider;
    private RestTemplate restTemplate;
    private ResponseEntity response;

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

    @BeforeEach
    void setup() throws CachingServiceClientException {
        restTemplate = mock(RestTemplate.class);
        response = mock(ResponseEntity.class);
        oidcTokenProvider = new OIDCTokenProvider(restTemplate);
        ReflectionTestUtils.setField(oidcTokenProvider, "isEnabled", true);
    }

    @Nested
    class GivenTokenForValidation {
        @Test
        void whenRequestIsSuccessful_thenReturnValid() {
            doReturn(HttpStatus.OK).when(response).getStatusCode();
            doReturn(BODY).when(response).getBody();
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), (Class<?>) any())).thenReturn(response);
            assertTrue(oidcTokenProvider.isValid("token"));
        }

        @Test
        void whenRequestIsNotSuccessful_thenReturnInvalid() {
            doReturn(HttpStatus.OK).when(response).getStatusCode();
            doReturn(NOT_VALID_BODY).when(response).getBody();
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), (Class<?>) any())).thenReturn(response);
            assertFalse(oidcTokenProvider.isValid("token"));
        }

        @Test
        void whenThrowException_thenReturnInvalid() {
            doReturn(HttpStatus.OK).when(response).getStatusCode();
            doReturn(NOT_VALID_BODY).when(response).getBody();
            HttpClientErrorException exception =
                HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "statusText", new HttpHeaders(), new byte[]{}, null);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), (Class<?>) any())).thenThrow(exception);
            assertFalse(oidcTokenProvider.isValid("token"));
        }

        @Test
        void whenNotValidJson_thenReturnInvalid() {
            doReturn(HttpStatus.OK).when(response).getStatusCode();
            doReturn("{notValid}").when(response).getBody();
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), (Class<?>) any())).thenReturn(response);
            assertFalse(oidcTokenProvider.isValid("token"));
        }

        @Test
        void whenResponseStatusIsNotOk_thenReturnInvalid() {
            doReturn(HttpStatus.UNAUTHORIZED).when(response).getStatusCode();
            doReturn(BODY).when(response).getBody();
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), (Class<?>) any())).thenReturn(response);
            assertFalse(oidcTokenProvider.isValid("token"));
        }

        @Test
        void whenTokenIsNull_ThenThrowException() {
            assertThrows(OIDCAuthException.class, () -> oidcTokenProvider.isValid(null));
        }

        @Test
        void whenTokenIsEmpty_ThenThrowException() {
            assertThrows(OIDCAuthException.class, () -> oidcTokenProvider.isValid(""));
        }

        @Test
        void whenProviderDisabled_ThenThrowException() {
            ReflectionTestUtils.setField(oidcTokenProvider, "isEnabled", false);
            assertThrows(OIDCAuthException.class, () -> oidcTokenProvider.isValid(null));
        }
    }

}
