# #4362 — Weird configuration of idle connection

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4362
**Labels:** bug, Priority: Low, clarification | **Created:** 2025-10-27 | **State:** open

---

## Description

`gateway-service/src/main/resources/application.yml` defines `apiml.connection.idleConnectionTimeoutSeconds: 5`. This value is intended to evict HTTP connections that have been idle for more than 5 seconds from the Reactor Netty connection pool.

However, the background eviction task that sweeps the pool and removes idle connections runs at the Reactor Netty default interval (typically 30–60 seconds), which is much longer than the 5-second idle timeout. This means a connection idle for 5 seconds is not evicted for up to 60 seconds, holding a file descriptor and a network socket unnecessarily.

The eviction sweep interval should equal or be slightly shorter than the idle timeout so connections are cleaned up promptly.

---

## Acceptance Criteria

- The background eviction sweep interval for the Reactor Netty connection pool equals `apiml.connection.idleConnectionTimeoutSeconds`.
- A connection that has been idle for longer than `idleConnectionTimeoutSeconds` is reclaimed within one sweep interval (i.e., within `2 × idleConnectionTimeoutSeconds`).
- The `idleConnectionTimeoutSeconds` default is reviewed: if 5 seconds is too aggressive under realistic load, the default is raised (suggested: 30 seconds) with the eviction interval set to the same value.
- The relationship between `maxIdleTime` and `evictInBackground` is documented in `application.yml`.

---

## Technical Solution

### Files to change

- `gateway-service/src/main/java/org/zowe/apiml/security/common/config/WebClientConfig.java` — confirm and update `ConnectionProvider` builder
- `gateway-service/src/main/resources/application.yml` — update default and add documentation comment

### Changes

First, confirm the exact code path where the Reactor Netty `HttpClient` instances (`httpClientNoCert`, `httpClientClientCert`) are constructed. If a `ConnectionProvider` is not explicitly created, add one:

```java
ConnectionProvider connectionProvider = ConnectionProvider.builder("apiml")
    .maxConnections(500)
    .maxIdleTime(Duration.ofSeconds(idleConnectionTimeoutSeconds))
    .evictInBackground(Duration.ofSeconds(idleConnectionTimeoutSeconds))
    .build();

HttpClient httpClient = HttpClient.create(connectionProvider)
    // ... existing SSL and timeout configuration
```

**`application.yml`** — update default and document:

```yaml
apiml:
  connection:
    # Idle connections are evicted after this many seconds.
    # The background sweep runs at the same interval,
    # so connections are reclaimed within 2× this value.
    # Must be shorter than the upstream load balancer's idle timeout.
    idleConnectionTimeoutSeconds: 30   # raised from 5 — tune based on load profile
    timeout: 60000
    timeToLive: 10000
```

### Tests

**New `WebClientConfigTest`:**
- `givenIdleConnectionTimeoutConfigured_whenBuildHttpClient_thenConnectionProviderHasMatchingMaxIdleTime()` — use reflection or a test-accessible `ConnectionProvider` to assert `maxIdleTime` equals `Duration.ofSeconds(idleConnectionTimeoutSeconds)`.
- `givenIdleConnectionTimeoutConfigured_whenBuildHttpClient_thenEvictInBackgroundMatchesMaxIdleTime()` — assert `evictInBackground` duration equals `maxIdleTime` (regression guard against future configuration drift).

**Integration / metrics test:**
Start the Gateway, make one request to a downstream stub, wait for `2 × idleConnectionTimeoutSeconds`, then check Micrometer metric `reactor.netty.connection.provider.idle.connections` via the Actuator `/application/metrics` endpoint and assert it equals 0.
