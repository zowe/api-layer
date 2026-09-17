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
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.cache.PatRevocationStore;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.security.common.error.AccessTokenTooManyScopesException;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.zaas.cache.CachingServiceClient;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.zaas.security.service.token.ApimlAccessTokenProvider.CUTOVER_EPOCH_KEY;
import static org.zowe.apiml.zaas.security.service.token.ApimlAccessTokenProvider.INVALID_SCOPES_KEY;
import static org.zowe.apiml.zaas.security.service.token.ApimlAccessTokenProvider.INVALID_TOKENS_KEY;
import static org.zowe.apiml.zaas.security.service.token.ApimlAccessTokenProvider.INVALID_USERS_KEY;
import static org.zowe.apiml.zaas.security.service.token.ApimlAccessTokenProvider.SALT_KEY;

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
    void setup() throws CachingServiceClientException, SecureTokenInitializationException {
        cachingServiceClient = mock(CachingServiceClient.class);
        as = mock(AuthenticationService.class);
        when(cachingServiceClient.read(SALT_KEY)).thenReturn(new CachingServiceClient.KeyValue(SALT_KEY, new String(ApimlAccessTokenProvider.generateSalt())));
        // a cutover long in the past, so the routing branch is inert and the tests below exercise the
        // per-item lookup unless they say otherwise
        when(cachingServiceClient.read(CUTOVER_EPOCH_KEY)).thenReturn(new CachingServiceClient.KeyValue(CUTOVER_EPOCH_KEY, "1000"));
        when(cachingServiceClient.supportsMapItemQuery()).thenReturn(true);
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

    /**
     * Answers a point lookup out of the given maps the way the storage does: only keys that were asked for
     * and actually exist come back, and a map with nothing found is omitted entirely. Stubbing this rather
     * than a fixed return value means the tests also cover which keys the provider asks for.
     */
    private void givenStore(Map<String, Map<String, String>> maps) {
        when(cachingServiceClient.getMapItems(any())).thenAnswer(invocation -> {
            Map<String, Collection<String>> query = invocation.getArgument(0);
            Map<String, Map<String, String>> result = new HashMap<>();
            query.forEach((mapKey, keys) -> {
                Map<String, String> stored = maps.get(mapKey);
                if (stored == null) {
                    return;
                }
                Map<String, String> found = new HashMap<>();
                keys.forEach(key -> {
                    if (stored.containsKey(key)) {
                        found.put(key, stored.get(key));
                    }
                });
                if (!found.isEmpty()) {
                    result.put(mapKey, found);
                }
            });
            return result;
        });
    }

    private String tokenRecord(String tokenHash, LocalDateTime expiresAt) throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return mapper.writeValueAsString(new AccessTokenContainer(null, tokenHash, null, expiresAt, null, null));
    }

    @Test
    void invalidateToken() throws Exception {
        String token = "token";

        Date issued = new Date(System.currentTimeMillis());
        when(as.parseJwtWithSignature(token)).thenReturn(new QueryResponse(null, "user", issued, issued, "issuer", Collections.emptyList(), null));
        accessTokenProvider.invalidateToken(token);
        verify(cachingServiceClient, times(1)).appendList(anyString(), any());

    }

    /**
     * The lifespan travels with the entry, computed on this side, because the token expiry is authoritative
     * here and comparing it against the caching service's own clock is wrong across time zones.
     */
    @Test
    void givenToken_whenInvalidating_thenTheEntryCarriesItsLifespan() throws Exception {
        String token = "token";
        Date issued = new Date();
        Date expires = new Date(System.currentTimeMillis() + Duration.ofDays(10).toMillis());
        when(as.parseJwtWithSignature(token)).thenReturn(new QueryResponse(null, "user", issued, expires, "issuer", Collections.emptyList(), null));

        accessTokenProvider.invalidateToken(token);

        ArgumentCaptor<CachingServiceClient.KeyValue> captor = ArgumentCaptor.forClass(CachingServiceClient.KeyValue.class);
        verify(cachingServiceClient).appendList(eq(INVALID_TOKENS_KEY), captor.capture());
        assertNotNull(captor.getValue().getTtlSeconds());
        assertTrue(captor.getValue().getTtlSeconds() > Duration.ofDays(9).toSeconds());
        assertTrue(captor.getValue().getTtlSeconds() <= Duration.ofDays(10).toSeconds());
    }

    @Test
    void invalidateAllUserTokens() {
        String userId = "user";
        long timestamp = System.currentTimeMillis();

        accessTokenProvider.invalidateAllTokensForUser(userId, timestamp);

        ArgumentCaptor<CachingServiceClient.KeyValue> captor = ArgumentCaptor.forClass(CachingServiceClient.KeyValue.class);
        verify(cachingServiceClient, times(1)).appendList(eq(INVALID_USERS_KEY), captor.capture());
        assertNotNull(captor.getValue().getTtlSeconds());
        assertTrue(captor.getValue().getTtlSeconds() > Duration.ofDays(PatRevocationStore.RULE_RETENTION_DAYS - 1).toSeconds());
    }

    @Test
    void invalidateAllServiceTokens() {
        String serviceId = "service";
        long timestamp = System.currentTimeMillis();

        accessTokenProvider.invalidateAllTokensForService(serviceId, timestamp);
        verify(cachingServiceClient, times(1)).appendList(eq(INVALID_SCOPES_KEY), any());

    }

    @Test
    void givenSameToken_returnInvalidated() throws Exception {
        String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
        givenStore(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, null))));

        assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        verify(cachingServiceClient, never()).readAllMaps();
        verify(cachingServiceClient, never()).readAllLegacyMaps();
    }

    /**
     * The query has to carry the token hash, the user hash and one entry per scope - and nothing else. If it
     * asked for whole maps, the point of the lookup would be gone.
     */
    @Test
    void givenToken_whenValidating_thenOnlyTheRelevantKeysAreRequested() {
        when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(queryResponseTokenWithScopes);
        givenStore(Map.of());

        accessTokenProvider.isInvalidated(SCOPED_TOKEN);

        ArgumentCaptor<Map<String, Collection<String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cachingServiceClient).getMapItems(captor.capture());
        Map<String, Collection<String>> query = captor.getValue();
        assertEquals(1, query.get(INVALID_TOKENS_KEY).size());
        assertEquals(1, query.get(INVALID_USERS_KEY).size());
        assertEquals(2, query.get(INVALID_SCOPES_KEY).size());
    }

    /**
     * A token issued with more scopes than one lookup may carry is chunked rather than rejected. This is what
     * keeps the issuance cap advisory: a token already issued above it - by a previous release, say, which no
     * check added now can undo - must keep authenticating rather than become permanently unusable.
     */
    @Nested
    class GivenMoreScopesThanOneLookupCanCarry {

        private static final int BATCH_KEYS = 6; // so four scope hashes fit in one lookup

        private QueryResponse manyScopes(List<String> scopes) {
            return new QueryResponse(null, "user", issuedDate, new Date(), "issuer", scopes, QueryResponse.Source.ZOWE_PAT);
        }

        private List<String> scopeNames(int count) {
            return IntStream.range(0, count).mapToObj(i -> "service" + i).toList();
        }

        @BeforeEach
        void narrowTheBatch() {
            ReflectionTestUtils.setField(accessTokenProvider, "revocationLookupBatchKeys", BATCH_KEYS);
        }

        @Test
        void thenEveryScopeIsAskedAboutAcrossSeveralLookups() {
            List<String> scopes = scopeNames(10);
            when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(manyScopes(scopes));
            givenStore(Map.of());

            assertFalse(accessTokenProvider.isInvalidated(SCOPED_TOKEN));

            ArgumentCaptor<Map<String, Collection<String>>> captor = ArgumentCaptor.forClass(Map.class);
            verify(cachingServiceClient, times(3)).getMapItems(captor.capture());

            Set<String> askedFor = new HashSet<>();
            for (Map<String, Collection<String>> query : captor.getAllValues()) {
                assertTrue(query.get(INVALID_SCOPES_KEY).size() <= BATCH_KEYS - 2, "a lookup exceeded the key limit");
                // every batch carries the token and user hashes too, so each is a complete answer on its own
                assertEquals(1, query.get(INVALID_TOKENS_KEY).size());
                assertEquals(1, query.get(INVALID_USERS_KEY).size());
                askedFor.addAll(query.get(INVALID_SCOPES_KEY));
            }
            byte[] salt = accessTokenProvider.getSalt();
            Set<String> expected = scopes.stream()
                .map(scope -> ApimlAccessTokenProvider.getSecurePassword(scope, salt))
                .collect(Collectors.toSet());
            assertEquals(expected, askedFor);
        }

        @Test
        void givenARuleMatchingAScopeInALaterBatch_thenItIsStillFound() {
            List<String> scopes = scopeNames(10);
            when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(manyScopes(scopes));
            byte[] salt = accessTokenProvider.getSalt();
            String lastScopeHash = ApimlAccessTokenProvider.getSecurePassword(scopes.get(9), salt);
            givenStore(Map.of(INVALID_SCOPES_KEY, Map.of(lastScopeHash, String.valueOf(System.currentTimeMillis()))));

            assertTrue(accessTokenProvider.isInvalidated(SCOPED_TOKEN));
        }

        @Test
        void givenARuleMatchingTheFirstBatch_thenTheRemainingLookupsAreSkipped() {
            List<String> scopes = scopeNames(10);
            when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(manyScopes(scopes));
            byte[] salt = accessTokenProvider.getSalt();
            String firstScopeHash = ApimlAccessTokenProvider.getSecurePassword(scopes.get(0), salt);
            givenStore(Map.of(INVALID_SCOPES_KEY, Map.of(firstScopeHash, String.valueOf(System.currentTimeMillis()))));

            assertTrue(accessTokenProvider.isInvalidated(SCOPED_TOKEN));

            verify(cachingServiceClient, times(1)).getMapItems(any());
        }

        @Test
        void givenScopesThatFitInOneLookup_thenOnlyOneIsMade() {
            when(as.parseJwtWithSignature(SCOPED_TOKEN)).thenReturn(manyScopes(scopeNames(4)));
            givenStore(Map.of());

            accessTokenProvider.isInvalidated(SCOPED_TOKEN);

            verify(cachingServiceClient, times(1)).getMapItems(any());
        }
    }

    @Test
    void givenSaltNotAlreadyInCache_thenGenerateAndStoreNew() throws CachingServiceClientException {
        when(cachingServiceClient.read(SALT_KEY)).thenThrow(new CachingServiceClientException(""));
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
        givenStore(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, null))));

        assertFalse(accessTokenProvider.isInvalidated(differentToken));
    }

    @Test
    void givenTokenWithUserIdMatchingRule_returnInvalidated() {
        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
        Map<String, Map<String, String>> store = new HashMap<>();
        givenStore(store);
        doAnswer(answer -> {
            var mapkey = (String) answer.getArgument(0);
            var keyValue = (CachingServiceClient.KeyValue) answer.getArgument(1);
            store.computeIfAbsent(mapkey, key -> new HashMap<>()).put(keyValue.getKey(), keyValue.getValue());
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
        givenStore(Map.of(INVALID_SCOPES_KEY, Map.of(serviceId, String.valueOf(System.currentTimeMillis()))));

        assertTrue(accessTokenProvider.isInvalidated(SCOPED_TOKEN));
    }

    @Test
    void givenTokenWithoutUserId_thenDoNotFailAndDoNotAskForAUserRule() {
        when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES))
            .thenReturn(new QueryResponse(null, null, issuedDate, new Date(), "issuer", Collections.emptyList(), null));
        givenStore(Map.of());

        assertFalse(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));

        ArgumentCaptor<Map<String, Collection<String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cachingServiceClient).getMapItems(captor.capture());
        assertFalse(captor.getValue().containsKey(INVALID_USERS_KEY));
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
        verify(cachingServiceClient, times(1)).appendList(eq(INVALID_USERS_KEY), any());
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

    /**
     * A revoked-token record that has since expired must not shadow a rule that still matches: the token
     * check has to answer "no opinion" rather than "not revoked". This is the regression guard for returning
     * an empty Optional instead of Optional.of(false).
     */
    @Nested
    class WhenTheTokenRecordIsNoLongerUsable {

        @Test
        void givenAnExpiredRecordAndAMatchingUserRule_thenStillInvalidated() throws Exception {
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            String userHash = accessTokenProvider.getHash("USER");
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
            givenStore(Map.of(
                INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, LocalDateTime.now().minusDays(1))),
                INVALID_USERS_KEY, Map.of(userHash, Long.toString(System.currentTimeMillis()))
            ));

            assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        }

        @Test
        void givenAnExpiredRecordAndNoRule_thenNotInvalidated() throws Exception {
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
            givenStore(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, LocalDateTime.now().minusDays(1)))));

            assertFalse(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        }

        @Test
        void givenARecordWithNoExpiry_thenStillInvalidated() throws Exception {
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
            givenStore(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, null))));

            assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        }

        /**
         * The key's presence under this exact token hash already proves the token was revoked. An
         * unparseable value is weaker evidence than a missing one, so it must not fail more open.
         */
        @Test
        void givenAnUnparseableRecord_thenStillInvalidated() {
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
            givenStore(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, "not json at all")));

            assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        }
    }

    @Nested
    class WhenTheCachingServiceIsTooOld {

        /**
         * A caching service that predates the point lookup still serves the whole-map read, and it still
         * holds the layout the token would have been revoked into. Falling back to it is slow; rejecting
         * every personal access token fleet-wide would be an outage.
         */
        @Test
        void thenValidationFallsBackToTheWholeMapReadRatherThanFailing() throws Exception {
            when(cachingServiceClient.supportsMapItemQuery()).thenReturn(false);
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
            when(cachingServiceClient.readAllMaps())
                .thenReturn(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, null))));

            assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
            verify(cachingServiceClient, never()).getMapItems(any());
        }

        @Test
        void givenNothingRevoked_thenTheTokenIsStillAccepted() {
            when(cachingServiceClient.supportsMapItemQuery()).thenReturn(false);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(queryResponseWithoutScopes);
            when(cachingServiceClient.readAllMaps()).thenReturn(Map.of());

            assertFalse(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        }
    }

    @Nested
    class WhenRoutingByCutoverEpoch {

        private QueryResponse tokenCreatedAt(long creation) {
            return new QueryResponse(null, "user", new Date(creation), new Date(creation + Duration.ofDays(1).toMillis()),
                "issuer", Collections.emptyList(), QueryResponse.Source.ZOWE_PAT);
        }

        @Test
        void givenAConfiguredEpoch_thenItWinsOverTheStoredValueAndOverMinting() {
            ReflectionTestUtils.setField(accessTokenProvider, "configuredCutoverEpoch", 42L);

            assertEquals(42L, accessTokenProvider.getCutoverEpoch());
            verify(cachingServiceClient, never()).read(CUTOVER_EPOCH_KEY);
            verify(cachingServiceClient, never()).create(any());
        }

        @Test
        void givenAStoredEpoch_thenNothingIsMinted() {
            assertEquals(1000L, accessTokenProvider.getCutoverEpoch());
            verify(cachingServiceClient, never()).create(any());
        }

        @Test
        void givenNoEpochAnywhere_thenOneIsMintedAndTheEventIsCatalogued() {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(accessTokenProvider, "apimlLog", apimlLog);
            when(cachingServiceClient.read(CUTOVER_EPOCH_KEY)).thenThrow(new CachingServiceClientException("not found"));

            long before = System.currentTimeMillis();
            long minted = accessTokenProvider.getCutoverEpoch();

            assertTrue(minted >= before);
            verify(cachingServiceClient).create(argThat(kv -> CUTOVER_EPOCH_KEY.equals(kv.getKey())));
            verify(apimlLog).log(eq("org.zowe.apiml.zaas.pat.cutoverEpochMinted"), any());
        }

        /**
         * Two nodes hitting an absent epoch at once must converge on one value rather than each minting
         * their own, exactly as the salt does - create maps to putIfAbsent and a collision comes back as 409.
         */
        @Test
        void givenTwoNodesMintingAtOnce_thenTheyConvergeOnOneValue() {
            Map<String, String> store = new HashMap<>();
            CachingServiceClient client = mock(CachingServiceClient.class);
            when(client.read(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                if (!store.containsKey(key)) {
                    throw new CachingServiceClientException("no record");
                }
                return new CachingServiceClient.KeyValue(key, store.get(key));
            });
            doAnswer(invocation -> {
                CachingServiceClient.KeyValue kv = invocation.getArgument(0);
                if (store.putIfAbsent(kv.getKey(), kv.getValue()) != null) {
                    CachingServiceClientException collision = mock(CachingServiceClientException.class);
                    when(collision.isKeyCollision()).thenReturn(true);
                    throw collision;
                }
                return null;
            }).when(client).create(any());

            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            var nodeA = new ApimlAccessTokenProvider(client, as, mapper);
            var nodeB = new ApimlAccessTokenProvider(client, as, mapper);

            long epochA = nodeA.getCutoverEpoch();
            long epochB = nodeB.getCutoverEpoch();

            assertEquals(epochA, epochB);
            assertEquals(1, store.size());
        }

        /**
         * A gateway 404 for a caching service that is not registered yet is indistinguishable from a missing
         * key. putIfAbsent semantics are what stop that from silently replacing a good value.
         */
        @Test
        void givenASpuriousAbsence_thenTheStoredValueIsNotOverwritten() {
            when(cachingServiceClient.read(CUTOVER_EPOCH_KEY))
                .thenThrow(new CachingServiceClientException("looks absent"))
                .thenReturn(new CachingServiceClient.KeyValue(CUTOVER_EPOCH_KEY, "777"));
            CachingServiceClientException collision = mock(CachingServiceClientException.class);
            when(collision.isKeyCollision()).thenReturn(true);
            doThrow(collision).when(cachingServiceClient).create(argThat(kv -> CUTOVER_EPOCH_KEY.equals(kv.getKey())));

            assertEquals(777L, accessTokenProvider.getCutoverEpoch());
        }

        @Test
        void givenAnUnresolvableEpoch_thenTheLegacyStoreIsStillConsulted() {
            when(cachingServiceClient.read(CUTOVER_EPOCH_KEY)).thenThrow(new CachingServiceClientException("boom"));
            doThrow(new CachingServiceClientException("boom")).when(cachingServiceClient).create(argThat(kv -> CUTOVER_EPOCH_KEY.equals(kv.getKey())));

            assertTrue(accessTokenProvider.shouldConsultLegacyStore(tokenCreatedAt(System.currentTimeMillis())));
        }

        /**
         * A store that cannot answer must not add two round trips to every request for as long as it stays
         * that way; routing to the pre-cutover path in the meantime is only slower, not wrong.
         */
        @Test
        void givenResolutionFails_thenItIsNotRetriedOnEveryRequest() {
            when(cachingServiceClient.read(CUTOVER_EPOCH_KEY)).thenThrow(new CachingServiceClientException("boom"));
            doThrow(new CachingServiceClientException("boom")).when(cachingServiceClient).create(argThat(kv -> CUTOVER_EPOCH_KEY.equals(kv.getKey())));

            for (int i = 0; i < 5; i++) {
                accessTokenProvider.getCutoverEpoch();
            }

            verify(cachingServiceClient, times(1)).read(CUTOVER_EPOCH_KEY);
        }

        @Test
        void givenResolutionSucceeds_thenTheStoreIsNotReadAgain() {
            assertEquals(1000L, accessTokenProvider.getCutoverEpoch());
            assertEquals(1000L, accessTokenProvider.getCutoverEpoch());

            verify(cachingServiceClient, times(1)).read(CUTOVER_EPOCH_KEY);
        }

        @Test
        void givenATokenOlderThanTheEpoch_thenBothStoresAreConsulted() throws Exception {
            long epoch = System.currentTimeMillis();
            ReflectionTestUtils.setField(accessTokenProvider, "configuredCutoverEpoch", epoch);
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(tokenCreatedAt(epoch - Duration.ofDays(1).toMillis()));
            givenStore(Map.of());
            when(cachingServiceClient.readAllLegacyMaps())
                .thenReturn(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, null))));

            assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
            verify(cachingServiceClient).readAllLegacyMaps();
        }

        @Test
        void givenAPreCutoverTokenRevokedAfterTheUpgrade_thenTheNewStoreStillAnswers() throws Exception {
            long epoch = System.currentTimeMillis();
            ReflectionTestUtils.setField(accessTokenProvider, "configuredCutoverEpoch", epoch);
            String tokenHash = accessTokenProvider.getHash(TOKEN_WITHOUT_SCOPES);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(tokenCreatedAt(epoch - Duration.ofDays(1).toMillis()));
            when(cachingServiceClient.readAllLegacyMaps()).thenReturn(Map.of());
            givenStore(Map.of(INVALID_TOKENS_KEY, Map.of(tokenHash, tokenRecord(tokenHash, null))));

            assertTrue(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
        }

        @Test
        void givenATokenAfterTheEpoch_thenTheLegacyStoreIsNeverRead() {
            long epoch = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
            ReflectionTestUtils.setField(accessTokenProvider, "configuredCutoverEpoch", epoch);
            when(as.parseJwtWithSignature(TOKEN_WITHOUT_SCOPES)).thenReturn(tokenCreatedAt(System.currentTimeMillis()));
            givenStore(Map.of());

            assertFalse(accessTokenProvider.isInvalidated(TOKEN_WITHOUT_SCOPES));
            verify(cachingServiceClient, never()).readAllLegacyMaps();
        }

        /**
         * A token minted moments after the cutover on a node whose clock runs slightly behind must not lose
         * its pre-cutover revocations; that is the whole point of adding the allowance to the comparison
         * rather than baking it into the stored value.
         */
        @Test
        void givenATokenWithinTheSkewAllowance_thenTheLegacyStoreIsStillConsulted() {
            long epoch = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
            ReflectionTestUtils.setField(accessTokenProvider, "configuredCutoverEpoch", epoch);
            ReflectionTestUtils.setField(accessTokenProvider, "cutoverSkewAllowanceSeconds", 300L);

            assertTrue(accessTokenProvider.shouldConsultLegacyStore(tokenCreatedAt(epoch + Duration.ofSeconds(120).toMillis())));
            assertFalse(accessTokenProvider.shouldConsultLegacyStore(tokenCreatedAt(epoch + Duration.ofSeconds(600).toMillis())));
        }

        /**
         * The branch has to go inert on its own, so that deleting it later is a refactor rather than a
         * behaviour change.
         */
        @Test
        void givenTheSunsetHasPassed_thenTheBranchIsNotTakenEvenForAnAncientToken() {
            long epoch = System.currentTimeMillis() - ApimlAccessTokenProvider.LEGACY_SUNSET.toMillis() - 1000;
            ReflectionTestUtils.setField(accessTokenProvider, "configuredCutoverEpoch", epoch);

            assertFalse(accessTokenProvider.shouldConsultLegacyStore(tokenCreatedAt(epoch - Duration.ofDays(30).toMillis())));
        }
    }

    @Nested
    class WhenCappingScopes {

        @Test
        void givenMoreScopesThanTheLimit_thenIssuanceIsRejectedNamingTheLimit() {
            Set<String> scopes = IntStream.rangeClosed(0, PatRevocationStore.DEFAULT_MAX_SCOPES_PER_TOKEN)
                .mapToObj(i -> "service" + i)
                .collect(Collectors.toSet());

            var exception = assertThrows(AccessTokenTooManyScopesException.class,
                () -> accessTokenProvider.getToken("user", 10, scopes));

            assertEquals(PatRevocationStore.DEFAULT_MAX_SCOPES_PER_TOKEN, exception.getLimit());
            verify(as, never()).createLongLivedJwtToken(any(), anyInt(), any());
        }

        @Test
        void givenExactlyTheLimit_thenIssuanceSucceeds() {
            Set<String> scopes = IntStream.range(0, PatRevocationStore.DEFAULT_MAX_SCOPES_PER_TOKEN)
                .mapToObj(i -> "service" + i)
                .collect(Collectors.toSet());
            when(as.createLongLivedJwtToken(eq("user"), anyInt(), any())).thenReturn("token");

            assertEquals("token", accessTokenProvider.getToken("user", 10, scopes));
        }

        /**
         * A validation looks up one key per scope plus the token and user hashes, so the lookup limit has to
         * leave room for both - otherwise a legitimately-issued token could never be validated.
         */
        @Test
        void thenTheLookupLimitLeavesRoomForTheTokenAndUserHashes() {
            assertTrue(PatRevocationStore.DEFAULT_MAX_QUERY_KEYS >= PatRevocationStore.DEFAULT_MAX_SCOPES_PER_TOKEN + 2);
        }
    }

    @Nested
    class WhenCallingEviction {
        @Test
        void thenEvictNonRelevantTokensAndRules() {
            accessTokenProvider.evictNonRelevantTokensAndRules();
            verify(cachingServiceClient, times(1)).evictTokens(INVALID_TOKENS_KEY);
            verify(cachingServiceClient, times(1)).evictRules(INVALID_USERS_KEY);
            verify(cachingServiceClient, times(1)).evictRules(INVALID_SCOPES_KEY);
        }

        /**
         * The first of the three throwing used to abort the other two, so a single bad map silently skipped
         * the rest of the cleanup.
         */
        @Test
        void givenTheFirstEvictionFails_thenTheOthersStillRunAndTheFailureIsReported() {
            doThrow(new CachingServiceClientException("boom")).when(cachingServiceClient).evictTokens(INVALID_TOKENS_KEY);

            assertThrows(CachingServiceClientException.class, () -> accessTokenProvider.evictNonRelevantTokensAndRules());

            verify(cachingServiceClient, times(1)).evictRules(INVALID_USERS_KEY);
            verify(cachingServiceClient, times(1)).evictRules(INVALID_SCOPES_KEY);
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
    class SaltMemoization {

        @Test
        void givenRepeatedCallsWithinTheRefreshWindow_thenTheStoreIsReadOnce() {
            byte[] first = accessTokenProvider.getSalt();
            byte[] second = accessTokenProvider.getSalt();

            assertArrayEquals(first, second);
            verify(cachingServiceClient, times(1)).read(SALT_KEY);
        }

        @Test
        void givenTheRefreshWindowHasPassed_thenTheStoreIsReadAgain() {
            accessTokenProvider.getSalt();
            expireTheMemo();

            accessTokenProvider.getSalt();

            verify(cachingServiceClient, times(2)).read(SALT_KEY);
        }

        /**
         * A failed refresh must not lose the salt that is known to work, and must not be memoized either -
         * otherwise a transient store outage would freeze a stale answer for the whole refresh interval.
         */
        @Test
        void givenARefreshFailure_thenTheLastKnownSaltIsServedAndTheNextCallRetries() {
            byte[] original = accessTokenProvider.getSalt();
            expireTheMemo();
            doThrow(new CachingServiceClientException("timeout", new IOException())).when(cachingServiceClient).read(SALT_KEY);

            assertArrayEquals(original, accessTokenProvider.getSalt());
            verify(cachingServiceClient, times(2)).read(SALT_KEY);

            assertArrayEquals(original, accessTokenProvider.getSalt());
            verify(cachingServiceClient, times(3)).read(SALT_KEY);
        }

        @Test
        void thenTheCallerCannotMutateTheMemoizedSalt() {
            byte[] salt = accessTokenProvider.getSalt();
            Arrays.fill(salt, (byte) 0);

            assertFalse(Arrays.equals(salt, accessTokenProvider.getSalt()));
        }

        /**
         * A regenerated salt is not a self-heal: nothing hashed under the previous one can ever match again,
         * so every existing revocation has silently stopped being enforced.
         */
        @Test
        void givenTheStoreHasNoSalt_thenTheRegenerationIsCatalogued() {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(accessTokenProvider, "apimlLog", apimlLog);
            when(cachingServiceClient.read(SALT_KEY)).thenThrow(new CachingServiceClientException("no record"));

            accessTokenProvider.getSalt();

            verify(apimlLog).log("org.zowe.apiml.zaas.pat.saltRegenerated");
        }

        @Test
        void givenAnExistingSalt_thenNoRegenerationIsReported() {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(accessTokenProvider, "apimlLog", apimlLog);

            accessTokenProvider.getSalt();

            verify(apimlLog, never()).log("org.zowe.apiml.zaas.pat.saltRegenerated");
        }

        /**
         * A first start after enabling personal access tokens leaves the salt absent too, and it is not an
         * incident. The cutover epoch tells the two apart: it is written independently and never removed, so
         * an empty store means "new" while a store holding the epoch but no salt means "lost".
         */
        @Test
        void givenAnEmptyStore_thenTheFirstSaltIsNotReportedAsAnIncident() {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(accessTokenProvider, "apimlLog", apimlLog);
            when(cachingServiceClient.read(SALT_KEY)).thenThrow(new CachingServiceClientException("no record"));
            when(cachingServiceClient.read(CUTOVER_EPOCH_KEY)).thenThrow(new CachingServiceClientException("no record"));

            accessTokenProvider.getSalt();

            verify(apimlLog, never()).log("org.zowe.apiml.zaas.pat.saltRegenerated");
        }

        private void expireTheMemo() {
            ReflectionTestUtils.setField(accessTokenProvider, "saltReadAt",
                System.currentTimeMillis() - ApimlAccessTokenProvider.SALT_REFRESH_INTERVAL_MILLIS - 1);
        }
    }

    @Nested
    class SaltInitialization {

        @Test
        void givenUnexpectedError_whenReadSalt_thenThrowIt() {
            Exception unexpectedError = new CachingServiceClientException("unexpected error", new IOException("e.g. timeout"));
            doThrow(unexpectedError).when(cachingServiceClient).read(SALT_KEY);
            Exception thrownException = assertThrows(CachingServiceClientException.class, accessTokenProvider::initializeSalt);
            assertSame(unexpectedError, thrownException);
        }

        @Test
        void givenNoSaltInCache_whenInitializing_thenCreateNewOne() {
            Exception noRecordException = new CachingServiceClientException("no record");
            doThrow(noRecordException).when(cachingServiceClient).read(SALT_KEY);
            String salt = accessTokenProvider.initializeSalt();
            assertTrue(StringUtils.isNotBlank(salt));
            verify(cachingServiceClient, times(1)).create(any());
        }

        @Test
        void testInitializeSalt_WhenOldFormat_ShouldMigrateToBase64() {
            String oldRawSalt = "legacy_raw_salt_€_!";
            String expectedBase64 = Base64.getEncoder().encodeToString(oldRawSalt.getBytes());
            CachingServiceClient.KeyValue mockKeyValue = new CachingServiceClient.KeyValue(SALT_KEY, oldRawSalt);

            when(cachingServiceClient.read(SALT_KEY)).thenReturn(mockKeyValue);
            String salt = accessTokenProvider.initializeSalt();
            assertEquals(expectedBase64, salt);

            ArgumentCaptor<CachingServiceClient.KeyValue> argumentCaptor = ArgumentCaptor.forClass(CachingServiceClient.KeyValue.class);
            verify(cachingServiceClient, times(1)).update(argumentCaptor.capture());

            assertEquals(SALT_KEY, argumentCaptor.getValue().getKey());
            assertEquals(expectedBase64, argumentCaptor.getValue().getValue());
        }

        @Test
        void testInitializeSalt_WhenAlreadyBase64_ShouldNotMigrate() {
            byte[] originalBytes = "1234567890abcdef".getBytes();
            String validBase64Salt = Base64.getEncoder().encodeToString(originalBytes);

            CachingServiceClient.KeyValue mockKeyValue = new CachingServiceClient.KeyValue(SALT_KEY, validBase64Salt);
            when(cachingServiceClient.read(SALT_KEY)).thenReturn(mockKeyValue);
            String salt = accessTokenProvider.initializeSalt();

            assertEquals(validBase64Salt, salt);

            verify(cachingServiceClient, never()).update(any());
        }

        @Test
        void givenKeyCollision_whenStoreSalt_thenLogWarnAndThrowException() throws CachingServiceClientException {
            when(cachingServiceClient.read(SALT_KEY)).thenThrow(new CachingServiceClientException("Salt not found"));

            CachingServiceClientException mockCollisionException = mock(CachingServiceClientException.class);
            when(mockCollisionException.isKeyCollision()).thenReturn(true);

            doThrow(mockCollisionException).when(cachingServiceClient).create(any(CachingServiceClient.KeyValue.class));

            assertThrows(CachingServiceClientException.class, () -> accessTokenProvider.initializeSalt());

            verify(cachingServiceClient, times(1)).create(any(CachingServiceClient.KeyValue.class));
        }

        @Test
        void givenGenericCacheError_whenStoreSalt_thenLogErrorAndThrowException() throws CachingServiceClientException {
            when(cachingServiceClient.read(SALT_KEY)).thenThrow(new CachingServiceClientException("Salt not found"));

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
            when(cachingServiceClient.read(SALT_KEY)).thenReturn(new CachingServiceClient.KeyValue(SALT_KEY, ""));

            String resultSalt = accessTokenProvider.initializeSalt();

            assertNotNull(resultSalt);
            assertFalse(resultSalt.isEmpty());
            verify(cachingServiceClient, times(1)).create(any());
        }

    }

}
