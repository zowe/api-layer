# #4172 — Using only one cache library in the project

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4172
**Labels:** technical excellence, Priority: High | **Created:** 2025-06-18 | **State:** open

---

## Description

APIML uses two cache libraries simultaneously:

- **EhCache** — for `invalidatedJwtTokens` and `validationJwtToken` caches in zaas-service and gateway-service. Configured via `ehcache.xml`. Local to each instance (not shared), which means revoked tokens are not visible across ZAAS instances — a root cause of issue #4193.
- **Infinispan** (via Caching service) — for PAT, load-balancer sticky sessions, and other distributed state.

The dual-library approach increases maintenance burden, caused XML ordering issues (#3267), and prevents HA operation of JWT revocation. The goal is to consolidate on a single library.

---

## Acceptance Criteria

- The `invalidatedJwtTokens` cache is backed by the shared Caching service (Infinispan) rather than per-instance EhCache. A token invalidated on one ZAAS instance is immediately visible to all other instances.
- The `validationJwtToken` cache (read-through cache of signature verification results) remains local but is backed by Caffeine instead of EhCache — lighter weight and without XML configuration.
- `ehcache.xml` is removed from the codebase.
- The EhCache dependency is removed from all module `build.gradle` files.
- The migration is done in phases behind a feature flag (`apiml.security.useDistributedTokenCache`) to allow safe rollout.
- The known `@Cacheable` self-proxy bug (documented in `AuthenticationService` Javadoc) is eliminated by replacing annotations with explicit caching service calls.

---

## Technical Solution

### Files to change

- `zaas-service/build.gradle` — remove EhCache, add Caffeine
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/AuthenticationService.java` — replace `@Cacheable("invalidatedJwtTokens")` with explicit Caching service calls
- `apiml-security-common/src/main/resources/ehcache.xml` — delete
- `zaas-service/src/main/resources/application.yml` — add `apiml.security.useDistributedTokenCache: false` (Phase 1 default)

### Migration plan

**Phase 1:** Add a `@ConditionalOnProperty` on the cache implementation beans:
- `apiml.security.useDistributedTokenCache=false` (default) → EhCache (existing behaviour)
- `apiml.security.useDistributedTokenCache=true` → Caching service calls (new behaviour)

**Phase 2:** Flip the default to `true`. Announce in release notes.

**Phase 3:** Remove the feature flag and EhCache code entirely.

### Changes (Phase 1)

```java
// In AuthenticationService — replace @Cacheable("invalidatedJwtTokens") with:
public Boolean invalidateJwtToken(String jwtToken, boolean distribute) {
    if (useDistributedCache) {
        cachingServiceClient.create(new KeyValue(
            "invalidated." + jwtToken, String.valueOf(System.currentTimeMillis())));
    } else {
        // legacy EhCache path (existing @Cacheable annotation)
    }
    // ... rest of the existing logic
}

public boolean isInvalidated(String jwtToken) {
    if (useDistributedCache) {
        try {
            cachingServiceClient.read("invalidated." + jwtToken);
            return true;
        } catch (CachingServiceClientException e) {
            if (e.getCause() != null) throw e;
            return false;
        }
    }
    // legacy path
    return meAsProxy.isInvalidatedLegacy(jwtToken);
}
```

### Tests

**Update `GivenCacheJWTTest` in `AuthenticationServiceTest`:**
With `useDistributedTokenCache=true`, mock the caching service client, assert:
- `invalidateJwtToken()` calls `cachingServiceClient.create()` with a key of the form `"invalidated.{token}"`.
- A subsequent `isInvalidated()` call reads from the caching service (not from EhCache).

**New `GivenCacheJWTTestDistributed`:**
Two `AuthenticationService` instances sharing a mock caching service client — invalidate on one, assert `isInvalidated()` returns `true` on the other.

**Phase 1 feature flag tests:**
- `givenFeatureFlagFalse_whenInvalidate_thenUsesEhCacheAnnotation()`.
- `givenFeatureFlagTrue_whenInvalidate_thenCallsCachingService()`.

**Phase 3 cleanup test:**
After EhCache removal, assert `ehcache.xml` is absent from the test classpath.
