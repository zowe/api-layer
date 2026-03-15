# #4424 — Performance of PAT validation depends on amount of stored records

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4424
**Labels:** bug, Priority: Critical | **Created:** 2025-12-12 | **State:** open

---

## Description

`ApimlAccessTokenProvider.isInvalidated()` calls `cachingServiceClient.readAllMaps()` on every invocation, loading the entire dataset of revoked tokens, revoked users, and revoked scopes from the Caching service in one call. It then iterates all entries to find a match for the token being checked.

As the number of PAT revocations accumulates over time, validation latency grows proportionally — O(n) in the number of stored revocation records. In high-throughput deployments this becomes a critical bottleneck.

Additionally, `getSalt()` is called within the same code path and also reads from the Caching service on every invocation (no in-memory caching of the salt), further compounding the I/O.

---

## Acceptance Criteria

- Per-token revocation lookup is O(1): checking whether a specific token has been revoked requires exactly one Caching service read, regardless of the total number of revoked tokens.
- User-level and scope-level revocation rules are still enforced correctly.
- A network error from the Caching service during a revocation check causes the check to fail-safe (treat as revoked / propagate the error) rather than silently treating the token as valid.
- The `isInvalidated()` method no longer calls `readAllMaps()` for the per-token check.
- Existing tests for user-level and scope-level invalidation remain green.

---

## Technical Solution

### Files to change

- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java`
- `zaas-service/src/test/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProviderTest.java`

### Changes

**Per-token lookup: O(1) via direct key**

Store each revoked token under a direct key derived from its hash:

```java
// In invalidateToken():
String tokenHash = getHash(jwtToken);
String key = INVALID_TOKENS_KEY + "." + tokenHash;
cachingServiceClient.create(new CachingServiceClient.KeyValue(key,
    mapper.writeValueAsString(container)));
```

Check revocation via a single `read()`:

```java
private boolean isTokenDirectlyRevoked(String jwtToken) {
    String tokenHash = getHash(jwtToken);
    String key = INVALID_TOKENS_KEY + "." + tokenHash;
    try {
        cachingServiceClient.read(key); // 200 = revoked
        return true;
    } catch (CachingServiceClientException e) {
        if (e.getCause() != null) {
            // Network or unexpected error — fail safe
            throw e;
        }
        return false; // 404 = not revoked
    }
}
```

Keep the user-level (`INVALID_USERS_KEY`) and scope-level (`INVALID_SCOPES_KEY`) checks using `readAllMaps()` as before — they require timestamp comparison and cannot easily be O(1) without a more significant schema change.

**Updated `isInvalidated()`:**

```java
public boolean isInvalidated(String jwtToken) {
    // Fast O(1) per-token check first
    if (isTokenDirectlyRevoked(jwtToken)) return true;

    // Then check user/scope revocation rules (still uses readAllMaps)
    QueryResponse parsed = authenticationService.parseJwtWithSignature(jwtToken);
    if (parsed == null) return false;
    Map<String, Map<String, String>> allMaps = cachingServiceClient.readAllMaps();
    return checkRule(allMaps, parsed);
}
```

**In-memory salt caching (complementary fix):**

Cache the salt in a `volatile byte[]` field after first load. Clear the cache only on a `CachingServiceClientException` with a cause (indicating a potentially stale value due to service restart), not on 404 (salt missing).

### Tests

**Rewrite in `ApimlAccessTokenProviderTest`:**
- `givenSameToken_returnInvalidated()`: replace `when(cachingServiceClient.readAllMaps())` with `when(cachingServiceClient.read(tokenHashKey)).thenReturn(new KeyValue(...))` → assert `true`.
- `givenDifferentToken_returnNotInvalidated()`: mock `cachingServiceClient.read(hashKey)` throwing `CachingServiceClientException` with no cause (404) → assert `false`.

**New tests:**
- `givenCachingServiceNetworkError_whenCheckingRevocation_thenPropagateException()`: mock `cachingServiceClient.read(key)` throwing `CachingServiceClientException` with an `IOException` cause → assert the exception propagates (fail-safe behaviour).
- `givenTokenNotRevoked_whenCheckRevocation_thenReadAllMapsNotCalled()`: assert `cachingServiceClient.readAllMaps()` is not called when the per-token key is not found and no user/scope rule matches.

**Existing tests to keep unchanged:**
- `givenTokenWithUserIdMatchingRule_returnInvalidated()` — user-level check via `readAllMaps()` must still work.
- `givenTokenWithScopeMatchingRule_returnInvalidated()` — scope-level check via `readAllMaps()` must still work.
