/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Client for interaction with Caching Service
 * Supports basic CRUD operations
 */
@Slf4j
@SuppressWarnings({"squid:S1192"}) // literals are repeating in debug logs only
public class CachingServiceClient implements CachingClient, InitializingBean {

    private final GatewayClient gatewayClient;
    private final RestTemplate restTemplate;

    /**
     * Used for the revocation lookup and for {@link #read}. With no local caching of the answer, that lookup
     * is on every personal access token request and is this service's sole synchronous dependency in a split
     * deployment, so it gets its own short timeouts rather than the shared client's - a caching service that
     * is merely slow would otherwise tie up a request thread per personal access token user at the same time.
     * The salt and cutover epoch reads are on the same path, so they share it.
     */
    private final RestTemplate lookupRestTemplate;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Value("${apiml.cachingServiceClient.apiPath:/cachingservice/api/v1/cache}")
    private String CACHING_API_PATH;

    @Value("${apiml.cachingServiceClient.list.apiPath:/cachingservice/api/v1/cache-list/}")
    private String CACHING_LIST_API_PATH;

    @Value("${apiml.cachingServiceClient.legacyList.apiPath:/cachingservice/api/v1/cache-list-legacy}")
    private String CACHING_LEGACY_LIST_API_PATH;

    @Value("${apiml.cachingServiceClient.query.apiPath:/cachingservice/api/v1/cache-query}")
    private String CACHING_QUERY_API_PATH;

    /**
     * Minimum caching service version that serves {@code /cache-query}. Named in the operator-facing message
     * so a version-skewed deployment is diagnosable without reading a stack trace.
     */
    static final String MIN_CACHING_SERVICE_VERSION = "3.6.0";

    /**
     * How long a "this caching service is too old" answer is trusted before probing again. Bounded so that
     * upgrading the caching service heals the deployment on its own, and so the catalogued error keeps being
     * emitted rather than scrolling away once.
     */
    private static final long QUERY_SUPPORT_RECHECK_MILLIS = 5L * 60 * 1000;

    /**
     * Map key used for nothing but checking that the caching service is answering at all.
     * {@code GET /cache-list/{mapKey}} has existed in every release and answers 200 for a map that does not
     * exist, so it separates "too old to serve /cache-query" from "no caching service registered" without
     * moving any real payload. It has to be an endpoint under {@code /api/v1}, because that is the only
     * gateway route the caching service declares - {@code /application/info} is not reachable this way.
     */
    private static final String REACHABILITY_PROBE_MAP_KEY = "apimlReachabilityProbe";

    private volatile boolean mapItemQuerySupported = true;
    private final AtomicLong querySupportCheckedAt = new AtomicLong();

    /**
     * A deliberately small circuit breaker over the revocation lookup. Once the caching service has failed
     * this many times in a row, further lookups fail immediately instead of each waiting out a timeout. The
     * answer is still "not valid" either way - {@code PATAuthSourceService.isValid} fails closed - but the
     * request threads come back rather than piling up behind a store that is not answering.
     */
    @Value("${apiml.security.personalAccessToken.revocationLookupFailureThreshold:5}")
    private int lookupFailureThreshold = 5;

    @Value("${apiml.security.personalAccessToken.revocationLookupCircuitOpenMillis:10000}")
    private long lookupCircuitOpenMillis = 10_000;

    private final AtomicInteger consecutiveLookupFailures = new AtomicInteger();
    private final AtomicLong lookupCircuitOpenedAt = new AtomicLong();

    @Value("${apiml.service.http.userId:#{null}}")
    private String cachingServiceUserId;

    @Value("${apiml.service.http.password:#{null}}")
    private String cachingServicePassword;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifyCertificates;

    @Value("${apiml.security.personalAccessToken.enabled:false}")
    private boolean personalAccessTokenEnabled;

    @Getter(AccessLevel.PACKAGE)
    private static final HttpHeaders defaultHeaders = new HttpHeaders();

    static {
        defaultHeaders.add("Content-Type", "application/json");
    }

    public CachingServiceClient(RestTemplate restTemplate, GatewayClient gatewayClient) {
        this(restTemplate, restTemplate, gatewayClient);
    }

    public CachingServiceClient(RestTemplate restTemplate, RestTemplate lookupRestTemplate, GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
        if (restTemplate == null) {
            throw new IllegalStateException("RestTemplate instance cannot be null");
        }
        this.restTemplate = restTemplate;
        this.lookupRestTemplate = lookupRestTemplate == null ? restTemplate : lookupRestTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (!verifyCertificates) {
            if (StringUtils.isEmpty(cachingServiceUserId) || StringUtils.isEmpty(cachingServicePassword)) {
                apimlLog.log("org.zowe.apiml.security.common.auth.missingDefaultCredentials");
            } else {
                String basicToken = "Basic " + Base64.getEncoder().encodeToString((cachingServiceUserId + ":" + cachingServicePassword).getBytes());
                defaultHeaders.add(HttpHeaders.AUTHORIZATION, basicToken);
            }
        }
    }

    /**
     * Finds out once, at startup, whether the caching service on the other end serves point lookups, so that
     * a version-skewed deployment shows up in the log rather than as a stack trace on the first token. A
     * failure here is inconclusive - the caching service may simply not be registered yet - so it leaves the
     * optimistic answer in place and the first real lookup settles it.
     * <p>
     * Skipped entirely when personal access tokens are disabled, which is the default. The revocation lookup
     * is only ever made for a personal access token, so such an installation has nothing to find out - and
     * plenty of them run no caching service at all, where probing would only produce a misleading error.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void probeOnStartup() {
        if (!personalAccessTokenEnabled) {
            log.debug("Personal access tokens are disabled, not probing the caching service for point lookup support");
            return;
        }
        probeMapItemQuery();
    }

    private String getGatewayAddress() {
        ServiceAddress gatewayAddress = gatewayClient.getGatewayConfigProperties();
        if (gatewayAddress.getScheme() == null || gatewayAddress.getHostname() == null) {
            throw new IllegalStateException("zaasProtocolHostPort has to have value in format <protocol>://<host>:<port> and not be null");
        }
        return String.format("%s://%s", gatewayAddress.getScheme(), gatewayAddress.getHostname());
    }

    /**
     * Creates {@link KeyValue} in Caching Service.
     *
     * @param kv {@link KeyValue} to store
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or cache conflict
     */
    public void create(KeyValue kv) throws CachingServiceClientException {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH, HttpMethod.POST, new HttpEntity<>(kv, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to create keyValue: " + kv.toString() + ", caused by: " + e.getMessage(), e);
        }
    }

    public void appendList(String mapKey, KeyValue kv) throws CachingServiceClientException {
        try {
            var url = getGatewayAddress() + CACHING_LIST_API_PATH + mapKey;
            log.debug("append list url: {}", url);
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(kv, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to create keyValue: " + kv.toString() + " in a map under " + mapKey + " key, caused by: " + e.getMessage(), e);
        }
    }

    @Override
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    public Map<String, Map<String, String>> readAllMaps() throws CachingServiceClientException {
        return readAllMaps(CACHING_LIST_API_PATH, "cache list");
    }

    @Override
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    public Map<String, Map<String, String>> readAllLegacyMaps() throws CachingServiceClientException {
        return readAllMaps(CACHING_LEGACY_LIST_API_PATH, "legacy cache list");
    }

    private Map<String, Map<String, String>> readAllMaps(String path, String description) throws CachingServiceClientException {
        try {
            var responseType = new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
            };
            var url = getGatewayAddress() + path;
            log.debug("readAllMaps url: {}", url);
            var response = restTemplate.exchange(url, HttpMethod.GET, null, responseType);
            if (response.getStatusCode().is2xxSuccessful()) {
                if (response.getBody() != null && !response.getBody().isEmpty()) {     //NOSONAR tests return null
                    return response.getBody();
                }
                return Map.of();
            } else {
                throw new CachingServiceClientException("Unable to read all key-value maps from " + description + ", caused by response from caching service is null or has no body");
            }
        } catch (Exception e) {
            throw new CachingServiceClientException("Unable to read all key-value maps from " + description + ", caused by: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Map<String, String>> getMapItems(Map<String, Collection<String>> keysByMapKey) throws CachingServiceClientException {
        if (isLookupCircuitOpen()) {
            throw new CachingServiceClientException(
                "Unable to look up cache items: the caching service has failed " + lookupFailureThreshold +
                    " lookups in a row, so this one was not attempted");
        }
        try {
            Map<String, Map<String, String>> found = lookupMapItems(keysByMapKey);
            consecutiveLookupFailures.set(0);
            return found;
        } catch (HttpStatusCodeException e) {
            boolean versionMismatch = isEndpointMissing(e.getStatusCode()) && markMapItemQueryUnsupportedIfConfirmed();
            if (!versionMismatch) {
                recordLookupFailure();
            }
            throw new CachingServiceClientException("Unable to look up cache items, caused by: " + e.getMessage(), e);
        } catch (Exception e) {
            recordLookupFailure();
            throw new CachingServiceClientException("Unable to look up cache items, caused by: " + e.getMessage(), e);
        }
    }

    private Map<String, Map<String, String>> lookupMapItems(Map<String, Collection<String>> keysByMapKey) {
        var responseType = new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
        };
        var url = getGatewayAddress() + CACHING_QUERY_API_PATH;
        log.debug("getMapItems url: {}", url);
        ResponseEntity<Map<String, Map<String, String>>> response;
        try {
            response = lookupRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(keysByMapKey, defaultHeaders), responseType);
        } catch (ResourceAccessException e) {
            // a read is idempotent, so one retry is safe, and a single dropped connection is the most common
            // reason for this to fail at all. The short timeout is what keeps the retry affordable.
            log.debug("Retrying the revocation lookup once after a transport failure", e);
            response = lookupRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(keysByMapKey, defaultHeaders), responseType);
        }
        return response.getBody() == null ? Map.of() : response.getBody();   //NOSONAR tests return null
    }

    private boolean isLookupCircuitOpen() {
        long openedAt = lookupCircuitOpenedAt.get();
        if (openedAt == 0) {
            return false;
        }
        if (System.currentTimeMillis() - openedAt < lookupCircuitOpenMillis) {
            return true;
        }
        // half open: let one request through to find out whether the store is back
        if (lookupCircuitOpenedAt.compareAndSet(openedAt, 0)) {
            consecutiveLookupFailures.set(0);
        }
        return false;
    }

    private void recordLookupFailure() {
        if (consecutiveLookupFailures.incrementAndGet() >= lookupFailureThreshold) {
            lookupCircuitOpenedAt.compareAndSet(0, System.currentTimeMillis());
        }
    }

    @Override
    public boolean supportsMapItemQuery() {
        if (mapItemQuerySupported) {
            return true;
        }
        long checkedAt = querySupportCheckedAt.get();
        long now = System.currentTimeMillis();
        if (now - checkedAt < QUERY_SUPPORT_RECHECK_MILLIS || !querySupportCheckedAt.compareAndSet(checkedAt, now)) {
            return false;
        }
        return probeMapItemQuery();
    }

    /**
     * Asks the caching service for nothing at all. A 2xx, or any error other than "no such endpoint", means
     * the endpoint is there; a 404 or 405 means the caching service predates it.
     */
    boolean probeMapItemQuery() {
        querySupportCheckedAt.set(System.currentTimeMillis());
        try {
            lookupRestTemplate.exchange(getGatewayAddress() + CACHING_QUERY_API_PATH, HttpMethod.POST,
                new HttpEntity<>(Map.of(), defaultHeaders), String.class);
            markMapItemQuerySupported();
            return true;
        } catch (HttpStatusCodeException e) {
            if (isEndpointMissing(e.getStatusCode())) {
                markMapItemQueryUnsupportedIfConfirmed();
                return mapItemQuerySupported;
            }
            // any other status still proves the endpoint exists
            markMapItemQuerySupported();
            return true;
        } catch (RuntimeException e) {
            // the caching service may simply not be registered yet; assume it is current and find out on use
            log.debug("Could not probe the caching service for point lookup support", e);
            return mapItemQuerySupported;
        }
    }

    private boolean isEndpointMissing(HttpStatusCode status) {
        return HttpStatus.NOT_FOUND.equals(status) || HttpStatus.METHOD_NOT_ALLOWED.equals(status);
    }

    private void markMapItemQuerySupported() {
        if (!mapItemQuerySupported) {
            log.info("The caching service now serves point lookups; personal access token validation is back on the fast path");
        }
        mapItemQuerySupported = true;
    }

    /**
     * A 404 from the query endpoint has two very different causes, and only one of them is worth an
     * operator-facing error: a caching service too old to serve it, versus no caching service registered at
     * the gateway. The second is the ordinary state of a deployment that does not run one, and of every
     * deployment for the moments before registration completes - so it is confirmed against an endpoint that
     * has existed in every release before the version-mismatch error is logged.
     *
     * @return whether this really is a version mismatch, as opposed to an unreachable caching service
     */
    private boolean markMapItemQueryUnsupportedIfConfirmed() {
        if (!isCachingServiceReachable()) {
            log.debug("The point lookup endpoint is missing, but the caching service is not answering at all; " +
                "not treating this as a version mismatch");
            return false;
        }
        mapItemQuerySupported = false;
        // remember when this was learned, so the answer is trusted for the recheck interval rather than
        // re-probed on the very next request
        querySupportCheckedAt.set(System.currentTimeMillis());
        apimlLog.log("org.zowe.apiml.zaas.pat.cachingServiceTooOld", MIN_CACHING_SERVICE_VERSION);
        return true;
    }

    private boolean isCachingServiceReachable() {
        try {
            lookupRestTemplate.exchange(getGatewayAddress() + CACHING_LIST_API_PATH + REACHABILITY_PROBE_MAP_KEY,
                HttpMethod.GET, new HttpEntity<>(defaultHeaders), String.class);
            return true;
        } catch (HttpStatusCodeException e) {
            // any status other than "no such endpoint" came from the caching service itself, which is all
            // this needs to establish - a 400 for a storage mode without map support still proves it is there
            return !isEndpointMissing(e.getStatusCode());
        } catch (RuntimeException e) {
            log.debug("The caching service is not reachable", e);
            return false;
        }
    }

    /**
     * Evict the non-relevant invalidated tokens by deleting the entries in the specified map
     *
     * @param key the map key
     */
    public void evictTokens(String key) {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_LIST_API_PATH + "evict/tokens/" + key, HttpMethod.DELETE, new HttpEntity<>(null, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to delete key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Evict the non-relevant rules by deleting the entries in the specified map
     *
     * @param key the map key
     */
    public void evictRules(String key) {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_LIST_API_PATH + "evict/rules/" + key, HttpMethod.DELETE, new HttpEntity<>(null, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to delete key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }


    /**
     * Reads {@link KeyValue} from Caching Service
     * <p>
     * Uses the short-timeout lookup client: the only reads are of the hashing salt and the cutover epoch,
     * and both are made on the personal access token request path.
     *
     * @param key Key to read
     * @return {@link KeyValue}
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or 404 key not found in cache
     */
    public KeyValue read(String key) throws CachingServiceClientException {
        try {
            ResponseEntity<KeyValue> response = lookupRestTemplate.exchange(getGatewayAddress() + CACHING_API_PATH + "/" + key, HttpMethod.GET, new HttpEntity<KeyValue>(null, defaultHeaders), KeyValue.class);
            if (response.hasBody()) {
                return response.getBody();
            }
        } catch (RestClientException e) {
            if (!(
                (e instanceof HttpStatusCodeException httpStatusCodeException) &&
                (httpStatusCodeException.getStatusCode() == HttpStatus.NOT_FOUND)
            )) {
                throw new CachingServiceClientException("Unable to read key: " + key + ", caused by: " + e.getMessage(), e);
            }
        }

        // record not found
        throw new CachingServiceClientException("Key '" + key + "' was not found in caching service or the response body was empty");
    }

    /**
     * Updates {@link KeyValue} in Caching Service
     *
     * @param kv {@link KeyValue} to update
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or 404 key not found in cache
     */
    public void update(KeyValue kv) throws CachingServiceClientException {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH, HttpMethod.PUT, new HttpEntity<>(kv, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to update keyValue: " + kv.toString() + ", caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes {@link KeyValue} from Caching Service
     *
     * @param key Key to delete
     * @throws CachingServiceClientException when http response from caching is not 2xx, such as connect exception or 404 key not found in cache
     */
    public void delete(String key) throws CachingServiceClientException {
        try {
            restTemplate.exchange(getGatewayAddress() + CACHING_API_PATH + "/" + key, HttpMethod.DELETE, new HttpEntity<KeyValue>(null, defaultHeaders), String.class);
        } catch (RestClientException e) {
            throw new CachingServiceClientException("Unable to delete key: " + key + ", caused by: " + e.getMessage(), e);
        }
    }

    /**
     * Data POJO that represents entry in caching service
     */
    @RequiredArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Data
    public static class KeyValue {
        private final String key;
        private final String value;

        /**
         * Requested lifespan of the entry, in seconds. Computed here rather than in the caching service
         * because this side holds the authoritative expiration of the token, on the clock that issued it -
         * comparing a zone-less timestamp against the caching service's own clock is wrong the moment the two
         * run in different time zones. Left null when there is nothing to derive it from.
         */
        private Long ttlSeconds;

        @JsonCreator
        public KeyValue() {
            key = "";
            value = "";
        }

        public KeyValue(String key, String value, Long ttlSeconds) {
            this.key = key;
            this.value = value;
            this.ttlSeconds = ttlSeconds;
        }
    }

}
