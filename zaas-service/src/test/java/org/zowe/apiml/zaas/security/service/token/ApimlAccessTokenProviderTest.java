/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.zowe.apiml.zaas.cache.CachingServiceClient;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApimlAccessTokenProviderTest {

    CachingServiceClient cachingServiceClient;
    AuthenticationService as;
    ApimlAccessTokenProvider accessTokenProvider;

    private static String SCOPED_TOKEN;
    private static String TOKEN_WITHOUT_SCOPES;
    Date issued = new Date(System.currentTimeMillis() - 100000L);
    QueryResponse queryResponseTokenWithScopes = new QueryResponse(null, "user", issued, new Date(), "issuer", Arrays.asList("gateway", "discovery"), QueryResponse.Source.ZOWE_PAT);
    QueryResponse queryResponseWithoutScopes = new QueryResponse(null, "user", issued, new Date(), "issuer", Collections.emptyList(), QueryResponse.Source.ZOWE_PAT);

    @BeforeEach
    void setup() throws CachingServiceClientException {
        cachingServiceClient = mock(CachingServiceClient.class);
        as = mock(AuthenticationService.class);
        when(cachingServiceClient.read("salt")).thenReturn(new CachingServiceClient.KeyValue("salt", new String(ApimlAccessTokenProvider.generateSalt())));
        accessTokenProvider = new ApimlAccessTokenProvider(cachingServiceClient, as, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @BeforeAll
    static void initTokens() {
        HashSet<String> scopes = new HashSet<>();
        scopes.add("gateway");
        scopes.add("api-catalog");
        Map<String, Object> scopesClaim = new HashMap<>();
        scopesClaim.put("scopes", scopes);
        SCOPED_TOKEN = createTestToken("user", scopesClaim);
        TOKEN_WITHOUT_SCOPES = createTestToken("user", null);
    }

    @Test
    void invalidateToken() throws Exception {
        String token = "token";

        Date issued = new Date(System.currentTimeMillis());
        when(as.parseJwtWithSignature(token)).thenReturn(new QueryResponse(null, "user", issued, issued, "issuer", Collections.emptyList(), null));
        accessTokenProvider.invalidateToken(token);
        verify(cachingServiceClient, times(1)).appendList(anyString(), any());

    }

    @Test
    void invalidateAllUserTokens() {
        String userId = "user";
        int timestamp = 1234;

        accessTokenProvider.invalidateAllTokensForUser(userId, timestamp);
        verify(cachingServiceClient, times(1)).appendList(eq(ApimlAccessTokenProvider.INVALID_USERS_KEY), any());

    }

    @Test
    void invalidateAllServiceTokens() {
        String serviceId = "service";
        int timestamp = 1234;

        accessTokenProvider.invalidateAllTokensForService(serviceId, timestamp);
        verify(cachingServiceClient, times(1)).appendList(eq(ApimlAccessTokenProvider.INVALID_SCOPES_KEY), any());

    }

    @Test
    void givenSameToken_returnInvalidated() throws Exception {
        String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);

        AccessTokenContainer invalidateToken = new AccessTokenContainer(null, tokenHash, null, null, null, null);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String s = mapper.writeValueAsString(invalidateToken);
        Map<String, String> invalidTokens = new HashMap<>();
        invalidTokens.put(tokenHash, s);
        Map<String, Map<String, String>> cacheMap = new HashMap<>();
        cacheMap.put(ApimlAccessTokenProvider.INVALID_TOKENS_KEY, invalidTokens);
        when(cachingServiceClient.readAllMaps()).thenReturn(cacheMap);
        assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
    }

    @Test
    void givenSaltNotAlreadyInCache_thenGenerateAndStoreNew() throws CachingServiceClientException {
        when(cachingServiceClient.read("salt")).thenThrow(new CachingServiceClientException(""));
        doNothing().when(cachingServiceClient).create(any());
        byte[] salt = accessTokenProvider.getSalt();
        assertNotNull(salt);
    }

    @Test
    void givenSaltIsInvalid_thenThrowException() throws RuntimeException {

        try (MockedStatic<ApimlAccessTokenProvider> apimlAccessTokenProviderMock = Mockito.mockStatic(ApimlAccessTokenProvider.class)) {
            apimlAccessTokenProviderMock.when(() -> ApimlAccessTokenProvider.generateSalt()).thenThrow(new SecureTokenInitializationException(new Throwable("cause")));
            assertThrows(SecureTokenInitializationException.class, () ->  ApimlAccessTokenProvider.generateSalt());
        }
    }

    @Test
    void givenDifferentToken_returnNotInvalidated() throws Exception {
        String differentToken = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE2NTQ1MzAwMDUsImV4cCI6MTY1NDU1ODgwNSwiaXNzIjoiQVBJTUwiLCJqdGkiOiIwYTllNzAyMS1jYzY2LTQzMDMtYTc4YS0wZGQwMWM3MjYyZjkifQ.HNfmAzw_bsKVrft5a527LaF9zsBMkfZK5I95mRmdftmRtI9dQNEFQR4Eg10FiBP53asixz6vmereJGKV04uSZIJzAKOpRk-NlGrZ06UZ3cTCBaLmB1l2HYnrAGkWJ8gCaAAOxRN2Dy4LIa_2UrtT-87DfU1T0OblgUdqfgf1_WKw0JIl6uMjdsJrSKdP61GeacFuaGQGxxZBRR7r9D5mxdVLQaHAjzjK89ZqZuQP04jV1BR-0OnFNA84XsQdWG61dYbWDMDkjPcp-nFK65w5X6GLO0BKFHWn4vSIQMKLEb6A9j7ym9N7pAXdt-eXCdLRiHHGQDjYcNSh_zRHtXwwkdA";
        when(as.parseJwtWithSignature(differentToken)).thenReturn(queryResponseWithoutScopes);
        String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);

        AccessTokenContainer invalidateToken = new AccessTokenContainer(null, tokenHash, null, null, null, null);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String s = mapper.writeValueAsString(invalidateToken);
        Map<String, String> invalidTokens = new HashMap<>();
        invalidTokens.put(tokenHash, s);
        Map<String, Map<String, String>> cacheMap = new HashMap<>();
        cacheMap.put(ApimlAccessTokenProvider.INVALID_TOKENS_KEY, invalidTokens);
        when(cachingServiceClient.readAllMaps()).thenReturn(cacheMap);

        assertFalse(accessTokenProvider.isInvalidated(differentToken));
    }

    @Test
    void givenTokenWithUserIdMatchingRule_returnInvalidated() {
        String userId = accessTokenProvider.getHash("user");

        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
        Map<String, String> invalidUsers = new HashMap<>();
        invalidUsers.put(userId, String.valueOf(System.currentTimeMillis()));
        Map<String, Map<String, String>> cacheMap = new HashMap<>();
        cacheMap.put(ApimlAccessTokenProvider.INVALID_USERS_KEY, invalidUsers);
        when(cachingServiceClient.readAllMaps()).thenReturn(cacheMap);
        assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
    }
    @Test
    void givenTokenWithScopeMatchingRule_returnInvalidated() {
        String serviceId = accessTokenProvider.getHash("service");
        Date issued = new Date(System.currentTimeMillis() - 100000L);
        when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(new QueryResponse(null, "user", issued, issued, "issuer", Collections.singletonList("service"), null));
        Map<String, String> invalidScopes = new HashMap<>();
        invalidScopes.put(serviceId, String.valueOf(System.currentTimeMillis()));
        Map<String, Map<String, String>> cacheMap = new HashMap<>();
        cacheMap.put(ApimlAccessTokenProvider.INVALID_SCOPES_KEY, invalidScopes);
        when(cachingServiceClient.readAllMaps()).thenReturn(cacheMap);
        assertTrue(accessTokenProvider.isInvalidated(SCOPED_TOKEN));
    }


    @Test
    void givenUserAndValidExpirationTest_thenTokenIsCreated() {
        Set<String> scopes = new HashSet<>();
        scopes.add("Service1");
        scopes.add("Service2");
        int expiration = 55;
        when(as.createLongLivedJwtToken("user", expiration, scopes)).thenReturn("token");
        String token = accessTokenProvider.getToken("user", expiration, scopes);
        assertNotEquals(expiration,90);
        assertNotNull(token);
        assertEquals("token", token);
    }

    @Test
    void givenScopedToken_whenScopeIsListed_thenReturnValid() {
        when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(queryResponseTokenWithScopes);
        assertTrue(accessTokenProvider.isValidForScopes(SCOPED_TOKEN, "gateway"));
    }

    static Stream<String> invalidScopes() {
        return Stream.of("invalidService", "", null);
    }

    @ParameterizedTest
    @MethodSource("invalidScopes")
    void givenScopedToken_whenScopeIsNotListed_thenReturnInvalid(String scope) {
        when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(queryResponseTokenWithScopes);
        assertFalse(accessTokenProvider.isValidForScopes(SCOPED_TOKEN, scope));
    }

    @ParameterizedTest
    @MethodSource("invalidScopes")
    void givenTokenWithoutScopes_thenReturnInvalid(String scope) {
        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
        assertFalse(accessTokenProvider.isValidForScopes(TOKEN_WITHOUT_SCOPES, scope));
    }

    @Nested
    class WhenCallingEviction {
        @Test
        void thenEvictNonRelevantTokensAndRules() {
            accessTokenProvider.evictNonRelevantTokensAndRules();
            verify(cachingServiceClient, times(1)).evictTokens(ApimlAccessTokenProvider.INVALID_TOKENS_KEY);
            verify(cachingServiceClient, times(1)).evictRules(ApimlAccessTokenProvider.INVALID_USERS_KEY);
            verify(cachingServiceClient, times(1)).evictRules(ApimlAccessTokenProvider.INVALID_SCOPES_KEY);
        }
    }

    static String createTestToken(String username, Map<String, Object> claims) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 10_000L))
            .setIssuer(QueryResponse.Source.ZOWE_PAT.value)
            .setId(UUID.randomUUID().toString())
            .addClaims(claims).compact();
    }

}
