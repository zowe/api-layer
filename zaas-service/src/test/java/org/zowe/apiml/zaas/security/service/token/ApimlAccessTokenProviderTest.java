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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.zaas.cache.CachingServiceClient;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApimlAccessTokenProviderTest {

    CachingServiceClient cachingServiceClient;
    AuthenticationService as;
    ApimlAccessTokenProvider accessTokenProvider;

    private static String SCOPED_TOKEN;
    private static String TOKEN_WITHOUT_SCOPES;
    Date issuedDate = new Date(System.currentTimeMillis() - 100000L);
    QueryResponse queryResponseTokenWithScopes = new QueryResponse(null, "user", issuedDate, new Date(), "issuer", Arrays.asList("gateway", "discovery"), QueryResponse.Source.ZOWE_PAT);
    QueryResponse queryResponseWithoutScopes = new QueryResponse(null, "user", issuedDate, new Date(), "issuer", Collections.emptyList(), QueryResponse.Source.ZOWE_PAT);

    @BeforeEach
    void setup() throws CachingServiceClientException,SecureTokenInitializationException {
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
    void givenSaltResolved_whenGetSaltCalledAgain_thenReuseCachedValueWithoutRereading() throws CachingServiceClientException {
        byte[] first = accessTokenProvider.getSalt();
        byte[] second = accessTokenProvider.getSalt();

        assertArrayEquals(first, second);
        // the salt is resolved once and memoized, so the caching service is not hit again
        verify(cachingServiceClient, times(1)).read("salt");
    }

    @Test
    void givenSaltIsInvalid_thenThrowException() throws RuntimeException {

        try (MockedStatic<ApimlAccessTokenProvider> apimlAccessTokenProviderMock = Mockito.mockStatic(ApimlAccessTokenProvider.class)) {
            apimlAccessTokenProviderMock.when(() -> ApimlAccessTokenProvider.generateSalt()).thenThrow(new SecureTokenInitializationException(new Throwable("cause")));
            assertThrows(SecureTokenInitializationException.class, () ->  ApimlAccessTokenProvider.generateSalt());
        }
    }

    @Test
    void givenNominalCase_thenReturnSaltSuccessfully() throws CachingServiceClientException {

        try (MockedStatic<ApimlAccessTokenProvider> mock = Mockito.mockStatic(ApimlAccessTokenProvider.class)) {
            byte[] expectedSalt = new byte[24];
            mock.when(ApimlAccessTokenProvider::generateSalt).thenReturn(expectedSalt);

            byte[] actualSalt = ApimlAccessTokenProvider.generateSalt();
            assertNotNull(actualSalt);
            assertEquals(expectedSalt.length, actualSalt.length);
        }
    }
    @Test
    void given_whenSecureRandomThrowsNoSuchAlgorithmException_thenThrowSecureTokenInitializationException()  {

        try (MockedStatic<SecureRandom> mockedSecureRandom = Mockito.mockStatic(SecureRandom.class)) {
            mockedSecureRandom.when(SecureRandom::getInstanceStrong).thenThrow(new NoSuchAlgorithmException());

            assertThrows(SecureTokenInitializationException.class, () -> ApimlAccessTokenProvider.generateSalt());
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
        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
        Map<String, Map<String, String>> cacheMap = new HashMap<>();
        when(cachingServiceClient.readAllMaps()).thenReturn(cacheMap);
        doAnswer(answer -> {
            var mapkey = (String) answer.getArgument(0);
            var keyValue = (CachingServiceClient.KeyValue) answer.getArgument(1);
            cacheMap.computeIfAbsent(mapkey, key -> new HashMap<>()).put(keyValue.getKey(), keyValue.getValue());
            return null;
        }).when(cachingServiceClient).appendList(any(), any());
        accessTokenProvider.invalidateAllTokensForUser("User", System.currentTimeMillis());
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
        when(as.createLongLivedJwtToken("user", 55, scopes)).thenReturn("token");
        String token = accessTokenProvider.getToken("user", 55, scopes);
        assertNotNull(token);
        assertEquals("token", token);
    }

    @Test
    void givenScopedToken_whenScopeIsListed_thenReturnValid() {
        when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(queryResponseTokenWithScopes);
        assertTrue(accessTokenProvider.isValidForScopes(SCOPED_TOKEN, "gateway"));
    }

    @Test
    void givenNoTimestamp_thenUserSystemTimeToInvalidateAllTokensForUser() {
        String userId = "user";
        accessTokenProvider.invalidateAllTokensForUser(userId, 0);
        verify(cachingServiceClient, times(1)).appendList(eq(ApimlAccessTokenProvider.INVALID_USERS_KEY), any());
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

    @Nested
    class SaltInitialization {

        @Test
        void givenUnexpectedError_whenReadSalt_thenThrowIt() {
            Exception unexpectedError = new CachingServiceClientException("unexpected error", new IOException("e.g. timeout"));
            doThrow(unexpectedError).when(cachingServiceClient).read("salt");
            Exception thrownException = assertThrows(CachingServiceClientException.class, accessTokenProvider::initializeSalt);
            assertSame(unexpectedError, thrownException);
        }

        @Test
        void givenNoSaltInCache_whenInitializing_thenCreateNewOne() {
            Exception noRecordException = new CachingServiceClientException("no record");
            doThrow(noRecordException).when(cachingServiceClient).read("salt");
            String salt = accessTokenProvider.initializeSalt();
            assertTrue(StringUtils.isNotBlank(salt));
            verify(cachingServiceClient, times(1)).create(any());
        }

        @Test
        void testInitializeSalt_WhenOldFormat_ShouldMigrateToBase64() {
            String oldRawSalt = "legacy_raw_salt_€_!";
            String expectedBase64 = Base64.getEncoder().encodeToString(oldRawSalt.getBytes());
            CachingServiceClient.KeyValue mockKeyValue = new CachingServiceClient.KeyValue("salt", oldRawSalt);

            when(cachingServiceClient.read("salt")).thenReturn(mockKeyValue);
            String salt = accessTokenProvider.initializeSalt();
            assertEquals(expectedBase64, salt);

            ArgumentCaptor<CachingServiceClient.KeyValue> argumentCaptor = ArgumentCaptor.forClass(CachingServiceClient.KeyValue.class);
            verify(cachingServiceClient, times(1)).update(argumentCaptor.capture());

            assertEquals("salt", argumentCaptor.getValue().getKey());
            assertEquals(expectedBase64, argumentCaptor.getValue().getValue());
        }

        @Test
        void testInitializeSalt_WhenAlreadyBase64_ShouldNotMigrate() {
            byte[] originalBytes = "1234567890abcdef".getBytes();
            String validBase64Salt = Base64.getEncoder().encodeToString(originalBytes);

            CachingServiceClient.KeyValue mockKeyValue = new CachingServiceClient.KeyValue("salt", validBase64Salt);
            when(cachingServiceClient.read("salt")).thenReturn(mockKeyValue);
            String salt = accessTokenProvider.initializeSalt();

            assertEquals(validBase64Salt, salt);

            verify(cachingServiceClient, never()).update(any());
        }

        @Test
        void givenKeyCollision_whenStoreSalt_thenAdoptExistingSharedSalt() throws CachingServiceClientException {
            String winningSalt = Base64.getEncoder().encodeToString(ApimlAccessTokenProvider.generateSalt());
            // the initial lookup misses, and the re-read after the collision returns the salt stored by the winner
            when(cachingServiceClient.read("salt"))
                .thenThrow(new CachingServiceClientException("Salt not found"))
                .thenReturn(new CachingServiceClient.KeyValue("salt", winningSalt));

            CachingServiceClientException mockCollisionException = mock(CachingServiceClientException.class);
            when(mockCollisionException.isKeyCollision()).thenReturn(true);
            doThrow(mockCollisionException).when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            String salt = accessTokenProvider.initializeSalt();

            assertEquals(winningSalt, salt);
            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
            verify(cachingServiceClient, times(2)).read("salt");
        }

        @Test
        void givenKeyCollisionButSaltStillMissingOnReread_whenStoreSalt_thenThrowException() throws CachingServiceClientException {
            when(cachingServiceClient.read("salt")).thenThrow(new CachingServiceClientException("Salt not found"));

            CachingServiceClientException mockCollisionException = mock(CachingServiceClientException.class);
            when(mockCollisionException.isKeyCollision()).thenReturn(true);
            doThrow(mockCollisionException).when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            assertThrows(CachingServiceClientException.class, () -> accessTokenProvider.initializeSalt());

            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
        }

        @Test
        void givenStorageKeyCollision_whenStoreSalt_thenAdoptExistingSharedSalt() throws CachingServiceClientException {
            String winningSalt = Base64.getEncoder().encodeToString(ApimlAccessTokenProvider.generateSalt());
            // the embedded LocalCachingClient surfaces cache errors as StorageException, not CachingServiceClientException:
            // the initial lookup misses, and the re-read after the collision returns the salt stored by the winner
            when(cachingServiceClient.read("salt"))
                .thenThrow(new StorageException("org.zowe.apiml.cache.keyNotInCache", HttpStatus.NOT_FOUND))
                .thenReturn(new CachingServiceClient.KeyValue("salt", winningSalt));
            doThrow(new StorageException("org.zowe.apiml.cache.keyCollision", HttpStatus.CONFLICT))
                .when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            String salt = accessTokenProvider.initializeSalt();

            assertEquals(winningSalt, salt);
            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
            verify(cachingServiceClient, times(2)).read("salt");
        }

        @Test
        void givenGenericStorageError_whenStoreSalt_thenThrowException() {
            when(cachingServiceClient.read("salt")).thenThrow(new StorageException("org.zowe.apiml.cache.keyNotInCache", HttpStatus.NOT_FOUND));
            doThrow(new StorageException("org.zowe.apiml.cache.insufficientStorage", HttpStatus.INSUFFICIENT_STORAGE))
                .when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            assertThrows(StorageException.class, () -> accessTokenProvider.initializeSalt());

            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
        }

        @Test
        void givenGenericCacheError_whenStoreSalt_thenLogErrorAndThrowException() throws CachingServiceClientException {
            when(cachingServiceClient.read("salt")).thenThrow(new CachingServiceClientException("Salt not found"));

            CachingServiceClientException mockGenericException = mock(CachingServiceClientException.class);
            when(mockGenericException.isKeyCollision()).thenReturn(false);

            doThrow(mockGenericException).when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            assertThrows(CachingServiceClientException.class, () -> accessTokenProvider.initializeSalt());

            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
        }

        @Test
        void givenForcedNullInitialization_whenGetSalt_thenReturnEmptyByteArray() throws CachingServiceClientException {
            ApimlAccessTokenProvider providerSpy = spy(accessTokenProvider);

            doReturn(null).when(providerSpy).initializeSalt();
            byte[] actualBytes = providerSpy.getSalt();

            assertNotNull(actualBytes);
            assertEquals(0, actualBytes.length, "The byte array must be empty when saltStr is null");
        }

        @Test
        void givenNullOrEmptySaltInCache_whenInitializing_thenFallbackToGenerateNewSalt() throws CachingServiceClientException {
            when(cachingServiceClient.read("salt")).thenReturn(new CachingServiceClient.KeyValue("salt", ""));

            String resultSalt = accessTokenProvider.initializeSalt();

            assertNotNull(resultSalt);
            assertFalse(resultSalt.isEmpty());
            verify(cachingServiceClient, times(1)).create(any());
        }

    }

    /**
     * Focused coverage for the salt cache introduced to keep {@link ApimlAccessTokenProvider#getSalt()} off the
     * caching service on every request: it must resolve the salt exactly once (even under concurrent access),
     * hand every caller the same value, never leak the cached array, and recover from a cold-start collision by
     * adopting the salt another instance won the race to store.
     */
    @Nested
    class SaltCachingAndConcurrency {

        private String freshBase64Salt() {
            return Base64.getEncoder().encodeToString(ApimlAccessTokenProvider.generateSalt());
        }

        @Test
        void givenNoSaltInCache_whenGetSalt_thenNewSaltIsCreatedAndReturnedValueMatchesStored() throws CachingServiceClientException {
            // no salt exists yet (a bare "not found" without a cause is swallowed by initializeSalt)
            when(cachingServiceClient.read("salt")).thenThrow(new CachingServiceClientException("no record"));

            byte[] salt = accessTokenProvider.getSalt();

            // exactly one salt is created and it is what callers receive: the persisted value decodes to the returned bytes
            ArgumentCaptor<CachingServiceClient.KeyValue> captor = ArgumentCaptor.forClass(CachingServiceClient.KeyValue.class);
            verify(cachingServiceClient, times(1)).create(captor.capture());
            assertEquals("salt", captor.getValue().getKey());
            assertArrayEquals(Base64.getDecoder().decode(captor.getValue().getValue()), salt);
            assertEquals(16, salt.length);
        }

        @Test
        void givenManyConcurrentCallers_whenGetSalt_thenSaltResolvedOnceAndAllSeeSameValue() throws Exception {
            String base64Salt = freshBase64Salt();
            AtomicInteger reads = new AtomicInteger();
            when(cachingServiceClient.read("salt")).thenAnswer(invocation -> {
                reads.incrementAndGet();
                return new CachingServiceClient.KeyValue("salt", base64Salt);
            });

            int threadCount = 32;
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            // a barrier releases every thread at the same instant, so they genuinely race into getSalt()
            CyclicBarrier startGate = new CyclicBarrier(threadCount);
            List<Future<byte[]>> results = new ArrayList<>();
            try {
                for (int i = 0; i < threadCount; i++) {
                    results.add(pool.submit(() -> {
                        startGate.await();
                        return accessTokenProvider.getSalt();
                    }));
                }

                byte[] expected = Base64.getDecoder().decode(base64Salt);
                for (Future<byte[]> result : results) {
                    assertArrayEquals(expected, result.get(10, TimeUnit.SECONDS));
                }
            } finally {
                pool.shutdownNow();
            }

            // double-checked locking must ensure the salt is resolved once regardless of how many callers race
            assertEquals(1, reads.get());
            verify(cachingServiceClient, times(1)).read("salt");
            verify(cachingServiceClient, never()).create(any());
        }

        @Test
        void givenResolvedSalt_whenCallerMutatesReturnedArray_thenCachedSaltIsNotCorrupted() throws CachingServiceClientException {
            String base64Salt = freshBase64Salt();
            when(cachingServiceClient.read("salt")).thenReturn(new CachingServiceClient.KeyValue("salt", base64Salt));

            byte[] first = accessTokenProvider.getSalt();
            Arrays.fill(first, (byte) 0); // a caller tampering with its copy must not poison the shared cache

            byte[] second = accessTokenProvider.getSalt();

            assertArrayEquals(Base64.getDecoder().decode(base64Salt), second);
            // the salt is memoized, so the caching service is read only on the first resolution
            verify(cachingServiceClient, times(1)).read("salt");
        }

        @Test
        void givenCollisionDuringInit_whenGetSalt_thenAdoptSharedSaltAndMemoize() throws CachingServiceClientException {
            String winningSalt = freshBase64Salt();
            // first read misses, the create loses the race with a 409, and the re-read returns the winner's salt
            when(cachingServiceClient.read("salt"))
                .thenThrow(new CachingServiceClientException("Salt not found"))
                .thenReturn(new CachingServiceClient.KeyValue("salt", winningSalt));
            CachingServiceClientException collision = mock(CachingServiceClientException.class);
            when(collision.isKeyCollision()).thenReturn(true);
            doThrow(collision).when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            byte[] first = accessTokenProvider.getSalt();
            byte[] second = accessTokenProvider.getSalt();

            // the adopted shared salt is returned and then memoized (no further reads on the second call)
            assertArrayEquals(Base64.getDecoder().decode(winningSalt), first);
            assertArrayEquals(first, second);
            verify(cachingServiceClient, times(2)).read("salt");
            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
        }

        @Test
        void givenTransientInitFailure_whenGetSaltCalledAgain_thenRetriesUntilResolvedAndThenMemoizes() throws CachingServiceClientException {
            ApimlAccessTokenProvider providerSpy = spy(accessTokenProvider);
            byte[] realSalt = ApimlAccessTokenProvider.generateSalt();
            String base64Salt = Base64.getEncoder().encodeToString(realSalt);
            // a transient failure yields no salt, a later attempt succeeds
            doReturn("").doReturn(base64Salt).when(providerSpy).initializeSalt();

            byte[] firstAttempt = providerSpy.getSalt();
            assertEquals(0, firstAttempt.length, "a transient failure must not be memoized as an empty salt");

            byte[] secondAttempt = providerSpy.getSalt();
            byte[] thirdAttempt = providerSpy.getSalt();

            assertArrayEquals(realSalt, secondAttempt);
            assertArrayEquals(realSalt, thirdAttempt);
            // initialization is retried after the empty result, then memoized once resolved: 2 inits for 3 calls
            verify(providerSpy, times(2)).initializeSalt();
        }

    }

}
