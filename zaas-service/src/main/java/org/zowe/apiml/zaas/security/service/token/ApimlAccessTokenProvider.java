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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.cache.PATRevocationStore;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.AccessTokenTooManyScopesException;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.zaas.cache.CachingClient;
import org.zowe.apiml.zaas.cache.CachingServiceClient;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {

    static final String INVALID_TOKENS_KEY = PATRevocationStore.INVALID_TOKENS_KEY;
    static final String INVALID_USERS_KEY = PATRevocationStore.INVALID_USERS_KEY;
    static final String INVALID_SCOPES_KEY = PATRevocationStore.INVALID_SCOPES_KEY;
    static final String SALT_KEY = "salt";
    static final String CUTOVER_EPOCH_KEY = "patCutoverEpoch";

    private static final int SALT_LENGTH = 16;

    /** Longest life a personal access token can be issued with, and therefore the longest a revocation of one matters. */
    static final int MAX_TOKEN_VALIDITY_DAYS = 90;

    /**
     * How long a memoized salt is served before it is read again.
     */
    static final Duration SALT_REFRESH_INTERVAL = Duration.ofMinutes(5);

    /** How long to wait before reading the salt again after a failed attempt. */
    static final Duration SALT_RETRY_INTERVAL = Duration.ofMinutes(1);

    /**
     * How long after the cutover the pre-cutover store is still consulted.
     */
    static final Duration LEGACY_SUNSET = Duration.ofDays(100);

    static final Instant EARLIEST_PLAUSIBLE_CUTOVER_DATE = Instant.parse("2026-09-01T00:00:00Z");

    /** How far ahead of this node's clock a configured cutover may lie, for clocks that disagree. */
    static final Duration CUTOVER_FUTURE_TOLERANCE = Duration.ofDays(1);

    static final Duration MAX_CUTOVER_SKEW_ALLOWANCE = Duration.ofHours(1);

    private static final Duration LEGACY_ROUTE_LOG_INTERVAL = Duration.ofMinutes(30);

    private static final Duration EPOCH_RESOLVE_RETRY = Duration.ofMinutes(1);

    private final CachingClient cachingServiceClient;
    private final AuthenticationService authenticationService;
    @Qualifier("oidcJwkMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("oidcJwtClock")
    private final Clock clock;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Value("${apiml.security.personalAccessToken.cutoverDate:}")
    private String configuredCutoverDate = "";

    @Value("${apiml.security.personalAccessToken.cutoverSkewAllowanceSeconds:300}")
    private long cutoverSkewAllowanceSeconds = 300;

    @Value("${apiml.security.personalAccessToken.maxScopes:#{T(org.zowe.apiml.cache.PATRevocationStore).DEFAULT_MAX_SCOPES_PER_TOKEN}}")
    private int maxScopes = PATRevocationStore.DEFAULT_MAX_SCOPES_PER_TOKEN;

    @Value("${apiml.security.personalAccessToken.revocationLookupBatchKeys:#{T(org.zowe.apiml.cache.PATRevocationStore).DEFAULT_MAX_QUERY_KEYS}}")
    private int revocationLookupBatchKeys = PATRevocationStore.DEFAULT_MAX_QUERY_KEYS;

    private final AtomicReference<Instant> cutoverEpoch = new AtomicReference<>();
    private final AtomicReference<Instant> cutoverEpochAttemptedAt = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<Instant> legacyRouteLoggedAt = new AtomicReference<>(Instant.EPOCH);
    private final AtomicBoolean skewAllowanceRejectionLogged = new AtomicBoolean();

    private volatile byte[] memoizedSalt;
    private volatile Instant saltReadAt = Instant.EPOCH;
    private final AtomicReference<Instant> saltAttemptedAt = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<RuntimeException> lastSaltFailure = new  AtomicReference<>();


    public void invalidateToken(String token) throws CachingServiceClientException, JsonProcessingException {
        apimlLog.log(MessageType.DEBUG, "Invalidating PAT: ...{}", StringUtils.right(token, 15));
        String hashedValue = getHash(token);
        QueryResponse queryResponse = authenticationService.parseJwtWithSignature(token);
        AccessTokenContainer container = new AccessTokenContainer();
        container.setTokenValue(hashedValue);
        container.setIssuedAt(LocalDateTime.ofInstant(queryResponse.getCreation().toInstant(), ZoneId.systemDefault()));
        container.setExpiresAt(LocalDateTime.ofInstant(queryResponse.getExpiration().toInstant(), ZoneId.systemDefault()));

        String json = objectMapper.writeValueAsString(container);
        cachingServiceClient.appendList(INVALID_TOKENS_KEY,
            new CachingServiceClient.KeyValue(hashedValue, json, secondsUntil(queryResponse.getExpiration())));
    }

    public void invalidateAllTokensForUser(String userId, long timestamp) throws CachingServiceClientException {
        apimlLog.log(MessageType.DEBUG, "Invalidating all PATs for user: {}", userId);
        String hashedUserId = getHash(userId.trim().toUpperCase());
        if (timestamp == 0) {
            timestamp = clock.millis();
        }
        log.debug("hashedUserId {}, timestamp {}", hashedUserId, timestamp);
        cachingServiceClient.appendList(INVALID_USERS_KEY,
            new CachingServiceClient.KeyValue(hashedUserId, Long.toString(timestamp), ruleTtlSeconds(timestamp)));
    }

    public void invalidateAllTokensForService(String serviceId, long timestamp) throws CachingServiceClientException {
        apimlLog.log(MessageType.DEBUG, "Invalidating all PATs for service: {}", serviceId);
        String hashedServiceId = getHash(serviceId);
        if (timestamp == 0) {
            timestamp = clock.millis();
        }
        log.debug("serviceIdHash {}, timestamp {}", hashedServiceId, timestamp);
        cachingServiceClient.appendList(INVALID_SCOPES_KEY,
            new CachingServiceClient.KeyValue(hashedServiceId, Long.toString(timestamp), ruleTtlSeconds(timestamp)));
    }

    private Long secondsUntil(Date moment) {
        if (moment == null) {
            return null;
        }
        return secondsUntil(moment.toInstant());
    }

    private long secondsUntil(Instant moment) {
        return Duration.between(clock.instant(), moment).toSeconds();
    }

    private Long ruleTtlSeconds(long ruleTimestamp) {
        return secondsUntil(Instant.ofEpochMilli(ruleTimestamp).plus(Duration.ofDays(PATRevocationStore.RULE_RETENTION_DAYS)));
    }

    public boolean isInvalidated(String token) throws CachingServiceClientException {
        byte[] salt = getSalt();
        QueryResponse parsedToken = authenticationService.parseJwtWithSignature(token);
        String hashedToken = getHash(token, salt);
        String hashedUserId = hashUserId(parsedToken, salt);
        List<String> hashedServiceIds = hashScopes(parsedToken, salt);

        if (!cachingServiceClient.supportsMapItemQuery()) {
            return matches(cachingServiceClient.readAllMaps(), parsedToken, hashedToken, hashedUserId, hashedServiceIds);
        }

        if (shouldConsultLegacyStore(parsedToken)) {
            logLegacyRoute();
            if (matches(cachingServiceClient.readAllLegacyMaps(), parsedToken, hashedToken, hashedUserId, hashedServiceIds)) {
                return true;
            }
        }

        for (List<String> scopeBatch : batchScopes(hashedServiceIds)) {
            if (matches(cachingServiceClient.getMapItems(buildQuery(hashedToken, hashedUserId, scopeBatch)),
                    parsedToken, hashedToken, hashedUserId, scopeBatch)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Splits the scope hashes so that no single lookup can exceed the caching service's key limit.
     */
    private List<List<String>> batchScopes(List<String> hashedServiceIds) {
        int perBatch = Math.max(1, revocationLookupBatchKeys - 2); // the token and user hashes ride along
        if (hashedServiceIds.size() <= perBatch) {
            return List.of(hashedServiceIds);
        }
        List<List<String>> batches = new ArrayList<>();
        for (int from = 0; from < hashedServiceIds.size(); from += perBatch) {
            batches.add(hashedServiceIds.subList(from, Math.min(from + perBatch, hashedServiceIds.size())));
        }
        return batches;
    }

    private Map<String, Collection<String>> buildQuery(String hashedToken, String hashedUserId, List<String> hashedServiceIds) {
        Map<String, Collection<String>> query = new LinkedHashMap<>();
        query.put(INVALID_TOKENS_KEY, List.of(hashedToken));
        if (hashedUserId != null) {
            query.put(INVALID_USERS_KEY, List.of(hashedUserId));
        }
        if (!hashedServiceIds.isEmpty()) {
            query.put(INVALID_SCOPES_KEY, hashedServiceIds);
        }
        return query;
    }

    private boolean matches(Map<String, Map<String, String>> cacheMap, QueryResponse parsedToken,
                            String hashedToken, String hashedUserId, List<String> hashedServiceIds) {
        Map<String, Map<String, String>> maps = cacheMap == null ? Map.of() : cacheMap;

        Optional<Boolean> isInvalidated = checkInvalidToken(maps.get(INVALID_TOKENS_KEY), hashedToken);
        if (isInvalidated.isEmpty() && hashedUserId != null) {
            isInvalidated = checkRule(maps.get(INVALID_USERS_KEY), hashedUserId, parsedToken);
        }
        for (String hashedServiceId : hashedServiceIds) {
            if (isInvalidated.isPresent()) {
                break;
            }
            isInvalidated = checkRule(maps.get(INVALID_SCOPES_KEY), hashedServiceId, parsedToken);
        }
        return isInvalidated.orElse(false);
    }

    private String hashUserId(QueryResponse parsedToken, byte[] salt) {
        String userId = parsedToken.getUserId();
        return userId == null ? null : getHash(userId.trim().toUpperCase(), salt);
    }

    private List<String> hashScopes(QueryResponse parsedToken, byte[] salt) {
        List<String> scopes = parsedToken.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        return scopes.stream().map(scope -> getHash(scope, salt)).toList();
    }

    private Optional<Boolean> checkInvalidToken(Map<String, String> invalidTokens, String tokenId) {
        if (invalidTokens == null || !invalidTokens.containsKey(tokenId)) {
            return Optional.empty();
        }
        return Optional.of(true);
    }

    private Optional<Boolean> checkRule(Map<String, String> tokenRules, String ruleId, QueryResponse parsedToken) {
        if (parsedToken.getCreation() == null) {
            return Optional.empty();
        }
        if (tokenRules != null && !tokenRules.isEmpty() && tokenRules.containsKey(ruleId)) {
            String timestampStr = tokenRules.get(ruleId);
            try {
                long timestamp = Long.parseLong(timestampStr);
                var tokenTime = parsedToken.getCreation().getTime();
                boolean result = tokenTime <= timestamp;
                if (result) {
                    return Optional.of(true);
                }
            } catch (NumberFormatException e) {
                log.error("Not able to convert timestamp value to number.", e);
            }
        }
        return Optional.empty();
    }

    boolean shouldConsultLegacyStore(QueryResponse parsedToken) {
        Date creation = parsedToken.getCreation();
        if (creation == null) {
            return true;
        }
        return getCutoverEpoch()
            .map(cutover -> isPreCutover(creation.toInstant(), cutover))
            .orElse(true);
    }

    private boolean isPreCutover(Instant creation, Instant cutover) {
        if (hasElapsed(cutover, LEGACY_SUNSET, clock.instant())) {
            return false;
        }
        return !creation.isAfter(cutover.plus(skewAllowance()));
    }

    /**
     * The allowance is a margin after the cutover inside which a token still counts as pre-cutover:
     */
    private Duration skewAllowance() {
        long seconds = cutoverSkewAllowanceSeconds;
        long maxSeconds = MAX_CUTOVER_SKEW_ALLOWANCE.toSeconds();
        if (seconds > maxSeconds) {
            logSkewAllowanceRejected(seconds, "it is above the maximum of " + maxSeconds
                + " - check that it is in seconds, not milliseconds; using " + maxSeconds + " instead");
            return MAX_CUTOVER_SKEW_ALLOWANCE;
        }
        return Duration.ofSeconds(seconds);
    }

    private void logSkewAllowanceRejected(long seconds, String problem) {
        if (skewAllowanceRejectionLogged.compareAndSet(false, true)) {
            apimlLog.log("org.zowe.apiml.zaas.pat.cutoverSettingRejected", "cutoverSkewAllowanceSeconds", seconds, problem);
        }
    }

    Optional<Instant> getCutoverEpoch() {
        var current = cutoverEpoch.get();
        if (current != null) {
            return Optional.of(current);
        }
        var attemptedAt = cutoverEpochAttemptedAt.get();
        var now = clock.instant();
        if (isWithin(attemptedAt, EPOCH_RESOLVE_RETRY, now) || !cutoverEpochAttemptedAt.compareAndSet(attemptedAt, now)) {
            return Optional.empty();
        }
        var resolved = resolveCutoverEpoch();
        resolved.ifPresent(r -> cutoverEpoch.compareAndSet(null, r));
        return resolved;
    }

    private Optional<Instant> resolveCutoverEpoch() {
        if (StringUtils.isNotBlank(configuredCutoverDate)) {
            String problem;
            try {
                var configured = OffsetDateTime.parse(configuredCutoverDate.trim()).toInstant();
                problem = validateCutoverEpoch(configured, clock.instant());
                if (problem == null) {
                    logCutoverEpoch("configuration", configured);
                    return Optional.of(configured);
                }
            } catch (DateTimeParseException e) {
                problem = "it is not an ISO-8601 date and time with a zone, such as 2026-10-05T12:00:00Z or 2026-10-05T14:00:00+02:00";
            }
            apimlLog.log("org.zowe.apiml.zaas.pat.cutoverSettingRejected", "cutoverDate", configuredCutoverDate, problem);
        }

        var stored = readCutoverEpoch();
        if (stored != null) {
            logCutoverEpoch("store", stored);
            return Optional.of(stored);
        }

        // millisecond precision, so this node agrees with the ones that read it back
        var minted = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        try {
            cachingServiceClient.create(new CachingServiceClient.KeyValue(CUTOVER_EPOCH_KEY, Long.toString(minted.toEpochMilli())));
            apimlLog.log("org.zowe.apiml.zaas.pat.cutoverEpochMinted", minted);
            logCutoverEpoch("minted", minted);
            return Optional.of(minted);
        } catch (CachingServiceClientException e) {
            if (e.isKeyCollision()) {
                var concurrent = readCutoverEpoch();
                if (concurrent != null) {
                    logCutoverEpoch("store", concurrent);
                    return Optional.of(concurrent);
                }
            }
            log.debug("Cannot resolve the personal access token cutover epoch", e);
        } catch (RuntimeException e) {
            log.debug("Cannot resolve the personal access token cutover epoch", e);
        }
        return Optional.empty();
    }

    static String validateCutoverEpoch(Instant epoch, Instant now) {
        if (epoch.isBefore(EARLIEST_PLAUSIBLE_CUTOVER_DATE)) {
            return "it is earlier than " + EARLIEST_PLAUSIBLE_CUTOVER_DATE
                + ", before this release existed";
        }
        Instant latest = now.plus(CUTOVER_FUTURE_TOLERANCE);
        if (epoch.isAfter(latest)) {
            return "it is more than " + CUTOVER_FUTURE_TOLERANCE.toHours() + " hours in the future ("
                + epoch + ")";
        }
        return null;
    }

    private Instant readCutoverEpoch() {
        try {
            CachingServiceClient.KeyValue keyValue = cachingServiceClient.read(CUTOVER_EPOCH_KEY);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            return Instant.ofEpochMilli(Long.parseLong(keyValue.getValue().trim()));
        } catch (NumberFormatException e) {
            log.warn("The stored personal access token cutover epoch is not a number, ignoring it", e);
        } catch (CachingServiceClientException | StorageException e) {
            log.debug("Cannot read the personal access token cutover epoch", e);
        }
        return null;
    }

    private void logCutoverEpoch(String source, Instant epoch) {
        log.info("Personal access token cutover epoch resolved to {} from {}; this node's clock reads {}",
            epoch, source, clock.instant());
    }

    private void logLegacyRoute() {
        var last = legacyRouteLoggedAt.get();
        var now = clock.instant();
        if (isWithin(last, LEGACY_ROUTE_LOG_INTERVAL, now) || !legacyRouteLoggedAt.compareAndSet(last, now)) {
            return;
        }
        apimlLog.log("org.zowe.apiml.zaas.pat.legacyStoreConsulted",
            Optional.ofNullable(cutoverEpoch.get()).map(Instant::toString).orElse("unresolved"));
    }

    public void evictNonRelevantTokensAndRules() {
        RuntimeException failure = null;
        failure = evictQuietly(() -> cachingServiceClient.evictTokens(INVALID_TOKENS_KEY), INVALID_TOKENS_KEY, failure);
        failure = evictQuietly(() -> cachingServiceClient.evictRules(INVALID_USERS_KEY), INVALID_USERS_KEY, failure);
        failure = evictQuietly(() -> cachingServiceClient.evictRules(INVALID_SCOPES_KEY), INVALID_SCOPES_KEY, failure);
        if (failure != null) {
            throw failure;
        }
    }

    private RuntimeException evictQuietly(Runnable eviction, String mapKey, RuntimeException previousFailure) {
        try {
            eviction.run();
            return previousFailure;
        } catch (RuntimeException e) {
            log.error("Cannot evict non-relevant entries of {}", mapKey, e);
            if (previousFailure == null) {
                return e;
            }
            previousFailure.addSuppressed(e);
            return previousFailure;
        }
    }

    public String issueToken(String username, int expirationTime, Set<String> scopes) {
        if (scopes != null && scopes.size() > maxScopes) {
            throw new AccessTokenTooManyScopesException(
                "A personal access token was requested with " + scopes.size() + " scopes, the limit is " + maxScopes, maxScopes);
        }
        int expiration = Math.min(expirationTime, MAX_TOKEN_VALIDITY_DAYS);
        if (expiration <= 0) {
            expiration = MAX_TOKEN_VALIDITY_DAYS;
        }
        return authenticationService.createLongLivedJwtToken(username, expiration, scopes);
    }

    public boolean isValidForScopes(String jwtToken, String serviceId) {
        if (serviceId != null) {
            QueryResponse parsedToken = authenticationService.parseJwtWithSignature(jwtToken);
            if (parsedToken != null && parsedToken.getScopes() != null) {
                return parsedToken.getScopes().contains(serviceId.toLowerCase());
            }
        }
        return false;
    }

    private String getHash(String token, byte[] salt) throws CachingServiceClientException {
        return getSecurePassword(token, salt);
    }

    public String getHash(String token) throws CachingServiceClientException {
        return getSecurePassword(token, getSalt());
    }

    private boolean isBase64EncodedSalt(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(value).length == SALT_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    String initializeSalt() throws CachingServiceClientException, SecureTokenInitializationException {
        String localSalt = null;
        try {
            CachingServiceClient.KeyValue keyValue = cachingServiceClient.read(SALT_KEY);
            if (keyValue != null && keyValue.getValue() != null) {
                localSalt = keyValue.getValue();
                if (!isBase64EncodedSalt(localSalt)) {
                    localSalt = Base64.getEncoder().encodeToString(localSalt.getBytes());
                    cachingServiceClient.update(new CachingServiceClient.KeyValue(SALT_KEY, localSalt));
                }
            }
        } catch (CachingServiceClientException | StorageException e) {
            log.debug("Cannot read salt.", e);
            if (e.getCause() != null) {
                // it could be because of timeout for example
                throw e;
            }
            // a null value was returned
        }
        if (localSalt == null || localSalt.isEmpty()) {
            boolean storeHadOtherPatState = hasStoredCutoverEpoch();
            var newSalt = getEncodedSalt();
            storeSalt(newSalt);
            if (storeHadOtherPatState) {
                apimlLog.log("org.zowe.apiml.zaas.pat.saltRegenerated");
            } else {
                log.info("A hashing salt for personal access tokens was created; the caching service held none, " +
                    "which is expected on the first start after enabling personal access tokens");
            }
            localSalt = newSalt;
        }

        return localSalt;
    }

    private boolean hasStoredCutoverEpoch() {
        return readCutoverEpoch() != null;
    }

    public byte[] getSalt() throws CachingServiceClientException {
        byte[] current = memoizedSalt;
        if (current == null) {
            return loadFirstSalt().clone();
        }
        var now = clock.instant();
        if (hasElapsed(saltReadAt, SALT_REFRESH_INTERVAL, now) && claimSaltAttempt(now)) {
            refreshSalt();
            current = memoizedSalt;
        }
        return current.clone();
    }

    private static boolean isWithin(Instant since, Duration interval, Instant now) {
        return now.isBefore(since.plus(interval));
    }

    private static boolean hasElapsed(Instant since, Duration interval, Instant now) {
        return !isWithin(since, interval, now);
    }

    private boolean claimSaltAttempt(Instant now) {
        var attemptedAt = saltAttemptedAt.get();
        return hasElapsed(attemptedAt, SALT_RETRY_INTERVAL, now) && saltAttemptedAt.compareAndSet(attemptedAt, now);
    }

    private void refreshSalt() {
        try {
            byte[] decoded = decodeSalt(initializeSalt());
            if (decoded.length > 0) {
                memoizedSalt = decoded;
                saltReadAt = clock.instant();
            }
        } catch (RuntimeException e) {
            log.warn("Cannot refresh the personal access token hashing salt, keeping the last known one", e);
        }
    }

    private synchronized byte[] loadFirstSalt() {
        if (memoizedSalt != null) {
            return memoizedSalt;
        }
        var now = clock.instant();
        RuntimeException previousFailure = lastSaltFailure.get();
        if (previousFailure != null && isWithin(saltAttemptedAt.get(), SALT_RETRY_INTERVAL, now)) {
            throw new CachingServiceClientException("The personal access token hashing salt is not available yet, " +
                "the last attempt to read it failed: " + previousFailure.getMessage(), previousFailure);
        }
        saltAttemptedAt.set(now);
        try {
            byte[] decoded = decodeSalt(initializeSalt());
            lastSaltFailure.set(null);
            if (decoded.length > 0) {
                memoizedSalt = decoded;
                saltReadAt = clock.instant();
            }
            return decoded;
        } catch (RuntimeException e) {
            lastSaltFailure.set(e);
            throw e;
        }
    }

    private String getEncodedSalt() {
       return Base64.getEncoder().encodeToString(generateSalt());
    }

    private byte[] decodeSalt(String saltStr) {
        if (saltStr == null) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(saltStr);
        } catch (IllegalArgumentException e) {
            // fallback to maintain back compatibility with the old raw format
            return saltStr.getBytes();
        }
    }

    private void storeSalt(String salt) throws CachingServiceClientException {
        try {
            cachingServiceClient.create(new CachingServiceClient.KeyValue(SALT_KEY, salt));
        } catch (CachingServiceClientException e) {
            if (e.isKeyCollision()) {
                log.warn("Salt initialization encountered a key collision. Verify your configuration (property 'jgroups.tcpping.initial_hosts') and using caching service '/application/health' endpoint verify that your clustering/JGroups cluster members are properly joined.");
            } else {
                log.error("Failed to store salt due to a cache infrastructure error.", e);
            }
            throw e;
        }
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        try {
            SecureRandom.getInstanceStrong().nextBytes(salt);
            return salt;
        } catch (NoSuchAlgorithmException e) {
            throw new SecureTokenInitializationException(e);
        }
    }

    public static String getSecurePassword(String password, byte[] salt) {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not generate hash", e);
        }
        return generatedPassword;
    }
}
