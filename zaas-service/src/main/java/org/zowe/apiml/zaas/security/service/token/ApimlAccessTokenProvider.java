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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.cache.PatRevocationStore;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {

    static final String INVALID_TOKENS_KEY = PatRevocationStore.INVALID_TOKENS_KEY;
    static final String INVALID_USERS_KEY = PatRevocationStore.INVALID_USERS_KEY;
    static final String INVALID_SCOPES_KEY = PatRevocationStore.INVALID_SCOPES_KEY;
    static final String SALT_KEY = "salt";
    static final String CUTOVER_EPOCH_KEY = "patCutoverEpoch";

    private static final int SALT_LENGTH = 16;

    /** Longest life a personal access token can be issued with, and therefore the longest a revocation of one matters. */
    static final int MAX_TOKEN_VALIDITY_DAYS = 90;

    /**
     * How long a memoized salt is served before it is read again. The salt never changes in normal operation,
     * but {@code initializeSalt} creates one when the store has none, so a permanent memo would leave nodes
     * hashing with divergent salts indefinitely after a store wipe. A refresh interval bounds that divergence
     * while still taking essentially every round trip off the request path.
     */
    static final long SALT_REFRESH_INTERVAL_MILLIS = 5L * 60 * 1000;

    /** How long to wait before reading the salt again after a failed attempt. */
    static final long SALT_RETRY_INTERVAL_MILLIS = 60L * 1000;

    /**
     * How long after the cutover the pre-cutover store is still consulted. Set beyond the 90-day token
     * lifetime on purpose: overshooting costs only a later deletion, whereas undershooting would stop
     * enforcing revocations that are still live. The branch is unreachable before then anyway -
     * {@code parseJwtWithSignature} rejects an expired token before any store is touched.
     */
    static final Duration LEGACY_SUNSET = Duration.ofDays(120);

    private static final long EPOCH_UNRESOLVED = -1L;
    private static final long LEGACY_ROUTE_LOG_INTERVAL_MILLIS = 30L * 60 * 1000;

    /**
     * How long to wait before trying to resolve the epoch again after a failure. Without this, a store that
     * cannot answer would add two round trips to every single request for as long as it stayed that way.
     */
    private static final long EPOCH_RESOLVE_RETRY_MILLIS = 60L * 1000;

    private final CachingClient cachingServiceClient;
    private final AuthenticationService authenticationService;
    @Qualifier("oidcJwkMapper")
    private final ObjectMapper objectMapper;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Pins the cutover for the whole fleet. Zero means "not configured": the value is then read from the
     * store, and minted there if absent. Setting it explicitly makes the value deterministic, identical on
     * every node, and immune to anything that happens to the cache.
     */
    @Value("${apiml.security.personalAccessToken.cutoverEpoch:0}")
    private long configuredCutoverEpoch;

    /**
     * Added to the cutover when deciding whether a token predates it, rather than baked into the stored
     * value, so the stored epoch stays audit-meaningful while the tolerance stays tunable. Keep it small:
     * a high threshold is free in correctness terms but sends post-cutover tokens down the slow path exactly
     * while the pre-cutover store is at its largest.
     */
    @Value("${apiml.security.personalAccessToken.cutoverSkewAllowanceSeconds:300}")
    private long cutoverSkewAllowanceSeconds = 300;

    /**
     * Cap on scopes at issuance. Advisory rather than load-bearing: validation chunks its lookups
     * ({@link #batchScopes}), so a token issued above this cap - including one issued by a previous release,
     * which no check here can undo - still authenticates.
     */
    @Value("${apiml.security.personalAccessToken.maxScopes:#{T(org.zowe.apiml.cache.PatRevocationStore).DEFAULT_MAX_SCOPES_PER_TOKEN}}")
    private int maxScopes = PatRevocationStore.DEFAULT_MAX_SCOPES_PER_TOKEN;

    /**
     * Most keys one revocation lookup may ask for. Matches the caching service's own limit, which is what
     * bounds the batch size; a value above it would make the caching service reject the lookup.
     */
    @Value("${apiml.security.personalAccessToken.revocationLookupBatchKeys:#{T(org.zowe.apiml.cache.PatRevocationStore).DEFAULT_MAX_QUERY_KEYS}}")
    private int revocationLookupBatchKeys = PatRevocationStore.DEFAULT_MAX_QUERY_KEYS;

    private final AtomicLong cutoverEpoch = new AtomicLong(EPOCH_UNRESOLVED);
    private final AtomicLong cutoverEpochAttemptedAt = new AtomicLong();
    private final AtomicLong legacyRouteLoggedAt = new AtomicLong();

    private volatile byte[] memoizedSalt;
    private volatile long saltReadAt;
    private final AtomicLong saltAttemptedAt = new AtomicLong();
    private volatile RuntimeException lastSaltFailure;

    // -------------------------------------------------------------------------------------------------
    // revocation
    // -------------------------------------------------------------------------------------------------

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
            timestamp = System.currentTimeMillis();
        }
        log.debug("hashedUserId {}, timestamp {}", hashedUserId, timestamp);
        cachingServiceClient.appendList(INVALID_USERS_KEY,
            new CachingServiceClient.KeyValue(hashedUserId, Long.toString(timestamp), ruleTtlSeconds(timestamp)));
    }

    public void invalidateAllTokensForService(String serviceId, long timestamp) throws CachingServiceClientException {
        apimlLog.log(MessageType.DEBUG, "Invalidating all PATs for service: {}", serviceId);
        String hashedServiceId = getHash(serviceId);
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        log.debug("serviceIdHash {}, timestamp {}", hashedServiceId, timestamp);
        cachingServiceClient.appendList(INVALID_SCOPES_KEY,
            new CachingServiceClient.KeyValue(hashedServiceId, Long.toString(timestamp), ruleTtlSeconds(timestamp)));
    }

    /**
     * The lifespan is computed here rather than in the caching service because this side holds the
     * authoritative expiration, on the clock of the node that issued the token. The stored
     * {@code AccessTokenContainer.expiresAt} is a zone-less {@code LocalDateTime}, so comparing it against
     * the caching service's own clock is already wrong whenever the two run in different time zones.
     *
     * @return seconds until the moment, which may be zero or negative for something already past - the
     *         caching service turns that into a removal rather than into an entry that never expires.
     */
    private Long secondsUntil(Date moment) {
        if (moment == null) {
            return null;
        }
        return Duration.ofMillis(moment.getTime() - System.currentTimeMillis()).toSeconds();
    }

    private Long ruleTtlSeconds(long ruleTimestamp) {
        return secondsUntil(new Date(ruleTimestamp + Duration.ofDays(PatRevocationStore.RULE_RETENTION_DAYS).toMillis()));
    }

    // -------------------------------------------------------------------------------------------------
    // validation
    // -------------------------------------------------------------------------------------------------

    public boolean isInvalidated(String token) throws CachingServiceClientException {
        byte[] salt = getSalt();
        QueryResponse parsedToken = authenticationService.parseJwtWithSignature(token);
        String hashedToken = getHash(token, salt);
        String hashedUserId = hashUserId(parsedToken, salt);
        List<String> hashedServiceIds = hashScopes(parsedToken, salt);

        if (!cachingServiceClient.supportsMapItemQuery()) {
            // the caching service predates the point lookup, so its whole-map read still returns the layout
            // this token would have been revoked into; slower, but correct
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
     * <p>
     * Chunking rather than rejecting is what keeps {@link #maxScopes} advisory: a token already issued with
     * more scopes than the cap - which no check added now can undo - still authenticates, at the cost of an
     * extra round trip. Each batch carries the token and user hashes too, so it is a complete answer on its
     * own and the first match short-circuits the rest. In the overwhelmingly common case there is exactly one
     * batch and this costs nothing.
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

    /**
     * An <em>absent</em> record means "not revoked". A transport or storage failure is a different thing
     * entirely and must not be confused with it: those propagate, and {@code PATAuthSourceService.isValid}
     * turns them into "not valid".
     */
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

    /**
     * Returns empty - "no opinion" - rather than {@code false} for an expired record, so that an expired
     * entry cannot shadow a still-valid user or scope rule that would otherwise have matched.
     */
    private Optional<Boolean> checkInvalidToken(Map<String, String> invalidTokens, String tokenId) {
        if (invalidTokens == null || !invalidTokens.containsKey(tokenId)) {
            return Optional.empty();
        }
        String s = invalidTokens.get(tokenId);
        try {
            AccessTokenContainer c = objectMapper.readValue(s, AccessTokenContainer.class);
            if (c == null) {
                return Optional.of(true);
            }
            if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(LocalDateTime.now())) {
                // the token is dead of old age anyway, so this changes no security outcome - it only stops a
                // stale record from being load-bearing
                return Optional.empty();
            }
            // a record with no expiry predates the field being populated, and stays revoked
            return Optional.of(true);
        } catch (JsonProcessingException e) {
            // the key's presence under this exact token hash already proves the token was revoked; an
            // unparseable value is weaker evidence than a missing one, so it must not fail more open
            log.error("Not able to parse invalidToken json value.", e);
            return Optional.of(true);
        }
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

    // -------------------------------------------------------------------------------------------------
    // cutover
    // -------------------------------------------------------------------------------------------------

    /**
     * A token issued after the cutover has only ever been able to be revoked into the per-item store, so it
     * is answered from there alone. An older one additionally consults the pre-cutover store, exactly as
     * before - which is what makes the upgrade invalidate nothing.
     * <p>
     * Every way the epoch can be wrong therefore costs latency rather than availability: too high, or lost
     * and re-minted, or divergent across nodes, and the affected tokens merely take the slower path. The one
     * case that is not benign is an epoch that is too <em>low</em>, which would drop pre-cutover revocations
     * - that is what the skew allowance is for.
     */
    boolean shouldConsultLegacyStore(QueryResponse parsedToken) {
        Date creation = parsedToken.getCreation();
        if (creation == null) {
            return true;
        }
        long epoch = getCutoverEpoch();
        if (epoch == EPOCH_UNRESOLVED) {
            // correct, merely slow - and loud, because a rate that never decays is the one way this could
            // quietly become permanent
            return true;
        }
        if (System.currentTimeMillis() >= epoch + LEGACY_SUNSET.toMillis()) {
            return false;
        }
        return creation.getTime() <= epoch + Duration.ofSeconds(cutoverSkewAllowanceSeconds).toMillis();
    }

    long getCutoverEpoch() {
        long current = cutoverEpoch.get();
        if (current != EPOCH_UNRESOLVED) {
            return current;
        }
        long attemptedAt = cutoverEpochAttemptedAt.get();
        long now = System.currentTimeMillis();
        if (attemptedAt != 0 && now - attemptedAt < EPOCH_RESOLVE_RETRY_MILLIS) {
            return EPOCH_UNRESOLVED;
        }
        if (!cutoverEpochAttemptedAt.compareAndSet(attemptedAt, now)) {
            // another thread is resolving it right now; routing to the legacy path meanwhile is only slower
            return EPOCH_UNRESOLVED;
        }
        long resolved = resolveCutoverEpoch();
        if (resolved != EPOCH_UNRESOLVED) {
            cutoverEpoch.compareAndSet(EPOCH_UNRESOLVED, resolved);
        }
        return resolved;
    }

    private long resolveCutoverEpoch() {
        if (configuredCutoverEpoch > 0) {
            logCutoverEpoch("configuration", configuredCutoverEpoch);
            return configuredCutoverEpoch;
        }

        Long stored = readCutoverEpoch();
        if (stored != null) {
            logCutoverEpoch("store", stored);
            return stored;
        }

        long minted = System.currentTimeMillis();
        try {
            // create maps to putIfAbsent and a collision comes back as 409, so a spurious "absent" - which a
            // gateway 404 for a not-yet-registered caching service looks exactly like - cannot overwrite a
            // good value
            cachingServiceClient.create(new CachingServiceClient.KeyValue(CUTOVER_EPOCH_KEY, Long.toString(minted)));
            apimlLog.log("org.zowe.apiml.zaas.pat.cutoverEpochMinted", minted);
            logCutoverEpoch("minted", minted);
            return minted;
        } catch (CachingServiceClientException e) {
            if (e.isKeyCollision()) {
                Long concurrent = readCutoverEpoch();
                if (concurrent != null) {
                    logCutoverEpoch("store", concurrent);
                    return concurrent;
                }
            }
            log.debug("Cannot resolve the personal access token cutover epoch", e);
        } catch (RuntimeException e) {
            log.debug("Cannot resolve the personal access token cutover epoch", e);
        }
        return EPOCH_UNRESOLVED;
    }

    private Long readCutoverEpoch() {
        try {
            CachingServiceClient.KeyValue keyValue = cachingServiceClient.read(CUTOVER_EPOCH_KEY);
            if (keyValue == null || keyValue.getValue() == null) {
                return null;
            }
            return Long.parseLong(keyValue.getValue().trim());
        } catch (NumberFormatException e) {
            log.warn("The stored personal access token cutover epoch is not a number, ignoring it", e);
        } catch (CachingServiceClientException | StorageException e) {
            log.debug("Cannot read the personal access token cutover epoch", e);
        }
        return null;
    }

    private void logCutoverEpoch(String source, long epoch) {
        log.info("Personal access token cutover epoch resolved to {} from {}; this node's clock reads {}",
            epoch, source, System.currentTimeMillis());
    }

    /**
     * Throttled, because the rate of this message is the signal: it should decay to zero as pre-cutover
     * tokens expire, and a rate that does not decay means the epoch is failing to resolve.
     */
    private void logLegacyRoute() {
        long last = legacyRouteLoggedAt.get();
        long now = System.currentTimeMillis();
        if (now - last < LEGACY_ROUTE_LOG_INTERVAL_MILLIS || !legacyRouteLoggedAt.compareAndSet(last, now)) {
            return;
        }
        long epoch = cutoverEpoch.get();
        apimlLog.log("org.zowe.apiml.zaas.pat.legacyStoreConsulted", epoch == EPOCH_UNRESOLVED ? "unresolved" : epoch);
    }

    // -------------------------------------------------------------------------------------------------
    // maintenance
    // -------------------------------------------------------------------------------------------------

    /**
     * With native expiration this is a safety net, not the primary mechanism, and it is reachable only from
     * the admin endpoints. Each of the three is attempted regardless of what the others did, so one failing
     * store does not silently skip the other two.
     */
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

    // -------------------------------------------------------------------------------------------------
    // issuance
    // -------------------------------------------------------------------------------------------------

    public String getToken(String username, int expirationTime, Set<String> scopes) {
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

    // -------------------------------------------------------------------------------------------------
    // salt
    // -------------------------------------------------------------------------------------------------

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
                    // the salt is stored in the old way, transform to base64 value
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
            String newSalt = Base64.getEncoder().encodeToString(generateSalt());
            storeSalt(newSalt);
            if (storeHadOtherPatState) {
                // Not a self-heal: SHA-512(salt+value) under the new salt can never match anything hashed
                // under the old one, so every revocation that predates this moment has just stopped being
                // enforced.
                apimlLog.log("org.zowe.apiml.zaas.pat.saltRegenerated");
            } else {
                // Nothing else of ours is in the store either, so this is a first start rather than a loss.
                // Saying otherwise would greet every new adopter with an incident.
                log.info("A hashing salt for personal access tokens was created; the caching service held none, " +
                    "which is expected on the first start after enabling personal access tokens");
            }
            localSalt = newSalt;
        }

        return localSalt;
    }

    /**
     * Whether the store holds the cutover epoch, which is written independently of the salt and never
     * removed. It is the cheapest available way to tell "first ever start" from "the store lost the salt":
     * both leave the salt absent, but only the second leaves this behind.
     */
    private boolean hasStoredCutoverEpoch() {
        return readCutoverEpoch() != null;
    }

    /**
     * Memoized here rather than inside {@code initializeSalt}, which is exercised directly for its
     * create-and-migrate side effects and has to reach the store on each call. A Spring {@code @Cacheable}
     * would not work either: {@code getHash(String)} calls this on the same bean, and a self-invocation
     * bypasses the proxy - so the annotation would silently do nothing on exactly the path that matters.
     * <p>
     * The memo expires rather than lasting forever, because {@code initializeSalt} creates a salt when the
     * store has none: after a store wipe a permanent memo would leave nodes hashing with divergent salts
     * indefinitely, where a refresh interval bounds the divergence. The array is copied on the way out
     * because it is handed straight to {@code MessageDigest.update} by callers.
     * <p>
     * Once a salt is known, no caller ever waits for the store. A due refresh is made by the one caller that
     * claims it, while everyone else carries on with the last known salt - which is safe, because the refresh
     * interval already accepts that much divergence. A failed refresh is retried only after
     * {@link #SALT_RETRY_INTERVAL_MILLIS}, so a store that is down costs one read per interval rather than one
     * per request.
     */
    public byte[] getSalt() throws CachingServiceClientException {
        byte[] current = memoizedSalt;
        if (current == null) {
            return loadFirstSalt().clone();
        }
        long now = System.currentTimeMillis();
        if (now - saltReadAt >= SALT_REFRESH_INTERVAL_MILLIS && claimSaltAttempt(now)) {
            refreshSalt();
            current = memoizedSalt;
        }
        return current.clone();
    }

    private boolean claimSaltAttempt(long now) {
        long attemptedAt = saltAttemptedAt.get();
        return now - attemptedAt >= SALT_RETRY_INTERVAL_MILLIS && saltAttemptedAt.compareAndSet(attemptedAt, now);
    }

    /**
     * Neither an empty result nor a failure is ever memoized. On an error the previous salt keeps being
     * served - which stays fail closed, because the store lookup that follows is going to fail too and
     * {@code PATAuthSourceService.isValid} denies on the exception.
     */
    private void refreshSalt() {
        try {
            byte[] decoded = decodeSalt(initializeSalt());
            if (decoded.length > 0) {
                memoizedSalt = decoded;
                saltReadAt = System.currentTimeMillis();
            }
        } catch (RuntimeException e) {
            log.warn("Cannot refresh the personal access token hashing salt, keeping the last known one", e);
        }
    }

    /**
     * With no salt known yet there is nothing to hash with, so callers have to wait here - but only for an
     * attempt in progress. After a failure, callers fail at once until the retry interval has passed,
     * rather than each queuing up for a read of their own against a store that is not answering.
     */
    private synchronized byte[] loadFirstSalt() {
        if (memoizedSalt != null) {
            return memoizedSalt;
        }
        long now = System.currentTimeMillis();
        RuntimeException previousFailure = lastSaltFailure;
        if (previousFailure != null && now - saltAttemptedAt.get() < SALT_RETRY_INTERVAL_MILLIS) {
            throw new CachingServiceClientException("The personal access token hashing salt is not available yet, " +
                "the last attempt to read it failed: " + previousFailure.getMessage(), previousFailure);
        }
        saltAttemptedAt.set(now);
        try {
            byte[] decoded = decodeSalt(initializeSalt());
            lastSaltFailure = null;
            if (decoded.length > 0) {
                memoizedSalt = decoded;
                saltReadAt = System.currentTimeMillis();
            }
            return decoded;
        } catch (RuntimeException e) {
            lastSaltFailure = e;
            throw e;
        }
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
                log.warn("Salt initialization encountered a 409 Conflict. Verify your configuration (property 'jgroups.tcpping.initial_hosts') and using caching service '/application/health' endpoint verify that your clustering/JGroups cluster members are properly joined.");
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
