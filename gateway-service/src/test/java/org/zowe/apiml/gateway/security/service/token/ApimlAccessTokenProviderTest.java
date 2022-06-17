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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.cache.CachingServiceClient;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApimlAccessTokenProviderTest {

    CachingServiceClient cachingServiceClient;
    AuthenticationService as;

    @BeforeEach
    void setup() throws CachingServiceClientException {
        cachingServiceClient = mock(CachingServiceClient.class);
        as = mock(AuthenticationService.class);
        when(cachingServiceClient.read("salt")).thenReturn(new CachingServiceClient.KeyValue("salt", new String(ApimlAccessTokenProvider.generateSalt())));
    }

    @Test
    void invalidateToken() throws Exception {
        String token = "token";

        Date issued = new Date(System.currentTimeMillis());
        when(as.parseJwtToken(token)).thenReturn(new QueryResponse(null, "user", issued, issued, null));
        ApimlAccessTokenProvider accessTokenProvider = new ApimlAccessTokenProvider(cachingServiceClient, as);
        accessTokenProvider.invalidateToken(token);
        verify(cachingServiceClient, times(1)).appendList(any());

    }

    @Test
    void givenSameToken_returnInvalidated() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE2NTQ1MzAwMDUsImV4cCI6MTY1NDU1ODgwNSwiaXNzIjoiQVBJTUwiLCJqdGkiOiIwYTllNzAyMS1jYzY2LTQzMDMtYTc4YS0wZGQwMWM3MjYyZjkifQ.HNfmAzw_bsKVrft5a527LaF9zsBMkfZK5I95mRmdftmRtI9dQNEFQR4Eg10FiBP53asixz6vmereJGKV04uSZIJzAKOpRk-NlGrZ06UZ3cTCBaLmB1l2HYnrAGkWJ8gCaAAOxRN2Dy4LIa_2UrtT-87DfU1T0OblgUdqfgf1_WKw0JIl6uMjdsJrSKdP61GeacFuaGQGxxZBRR7r9D5mxdVLQaHAjzjK89ZqZuQP04jV1BR-0OnFNA84XsQdWG61dYbWDMDkjPcp-nFK65w5X6GLO0BKFHWn4vSIQMKLEb6A9j7ym9N7pAXdt-eXCdLRiHHGQDjYcNSh_zRHtXwwkA";
        ApimlAccessTokenProvider accessTokenProvider = new ApimlAccessTokenProvider(cachingServiceClient, as);
        String tokenHash = accessTokenProvider.getHash(token);

        ApimlAccessTokenProvider.AccessTokenContainer invalidateToken = new ApimlAccessTokenProvider.AccessTokenContainer(null, tokenHash, null, null, null, null);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String s = mapper.writeValueAsString(invalidateToken);
        Map<String, String> map = new HashMap<>();
        map.put(tokenHash, s);
        when(cachingServiceClient.readInvalidatedTokens()).thenReturn(map);
        assertTrue(accessTokenProvider.isInvalidated(token));
    }

    @Test
    void givenDifferentToken_returnNotInvalidated() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE2NTQ1MzAwMDUsImV4cCI6MTY1NDU1ODgwNSwiaXNzIjoiQVBJTUwiLCJqdGkiOiIwYTllNzAyMS1jYzY2LTQzMDMtYTc4YS0wZGQwMWM3MjYyZjkifQ.HNfmAzw_bsKVrft5a527LaF9zsBMkfZK5I95mRmdftmRtI9dQNEFQR4Eg10FiBP53asixz6vmereJGKV04uSZIJzAKOpRk-NlGrZ06UZ3cTCBaLmB1l2HYnrAGkWJ8gCaAAOxRN2Dy4LIa_2UrtT-87DfU1T0OblgUdqfgf1_WKw0JIl6uMjdsJrSKdP61GeacFuaGQGxxZBRR7r9D5mxdVLQaHAjzjK89ZqZuQP04jV1BR-0OnFNA84XsQdWG61dYbWDMDkjPcp-nFK65w5X6GLO0BKFHWn4vSIQMKLEb6A9j7ym9N7pAXdt-eXCdLRiHHGQDjYcNSh_zRHtXwwkA";
        String differentToken = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE2NTQ1MzAwMDUsImV4cCI6MTY1NDU1ODgwNSwiaXNzIjoiQVBJTUwiLCJqdGkiOiIwYTllNzAyMS1jYzY2LTQzMDMtYTc4YS0wZGQwMWM3MjYyZjkifQ.HNfmAzw_bsKVrft5a527LaF9zsBMkfZK5I95mRmdftmRtI9dQNEFQR4Eg10FiBP53asixz6vmereJGKV04uSZIJzAKOpRk-NlGrZ06UZ3cTCBaLmB1l2HYnrAGkWJ8gCaAAOxRN2Dy4LIa_2UrtT-87DfU1T0OblgUdqfgf1_WKw0JIl6uMjdsJrSKdP61GeacFuaGQGxxZBRR7r9D5mxdVLQaHAjzjK89ZqZuQP04jV1BR-0OnFNA84XsQdWG61dYbWDMDkjPcp-nFK65w5X6GLO0BKFHWn4vSIQMKLEb6A9j7ym9N7pAXdt-eXCdLRiHHGQDjYcNSh_zRHtXwwkdA";
        ApimlAccessTokenProvider accessTokenProvider = new ApimlAccessTokenProvider(cachingServiceClient, as);
        String tokenHash = accessTokenProvider.getHash(token);

        ApimlAccessTokenProvider.AccessTokenContainer invalidateToken = new ApimlAccessTokenProvider.AccessTokenContainer(null, tokenHash, null, null, null, null);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String s = mapper.writeValueAsString(invalidateToken);
        Map<String, String> map = new HashMap<>();
        map.put(tokenHash, s);
        when(cachingServiceClient.readInvalidatedTokens()).thenReturn(map);

        assertFalse(accessTokenProvider.isInvalidated(differentToken));
    }

    @Test
    void givenUserAndValidExpirationTest_thenTokenIsCreated() {
        ApimlAccessTokenProvider accessTokenProvider = new ApimlAccessTokenProvider(cachingServiceClient, as);
        when(as.createLongLivedJwtToken("user", 55)).thenReturn("token");
        String token = accessTokenProvider.getToken("user", 55);
        assertNotNull(token);
        assertEquals("token", token);
    }

}
