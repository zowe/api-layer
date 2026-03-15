# #4421 — Loading/initializing of salt is not an atomic operation

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4421
**Labels:** bug, Priority: Low | **Created:** 2025-12-12 | **State:** open

---

## Description

`ApimlAccessTokenProvider.initializeSalt()` reads the salt from the Caching service. If the key is absent, it generates a new 16-byte salt and stores it. This read-then-write is not atomic: if two ZAAS instances start simultaneously and both find no salt, both generate different salts and store them. Whichever write completes last wins, and the other instance continues using a different salt. The two instances will then produce different hashes for the same token, causing cross-instance validation failures.

This issue should be fixed together with #4451 (salt encoding) since both touch the same code paths.

---

## Acceptance Criteria

- When two ZAAS instances initialise the salt concurrently and neither finds an existing value, exactly one salt is written to the Caching service and both instances end up using the same salt.
- If a race is detected (a concurrent write is rejected with HTTP 409 Conflict), the losing instance reads the winner's salt and uses it.
- A transient network error during the `read("salt")` call (distinguished from a 404 "not found" by the presence of a cause on `CachingServiceClientException`) propagates as an exception rather than triggering salt generation.
- The fix is delivered in the same commit as #4451 (Base64 encoding), using Base64 throughout.

---

## Technical Solution

### Files to change

- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java`
- `zaas-service/src/test/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProviderTest.java`

### Changes

**Updated `initializeSalt()` with CAS pattern**

```java
String initializeSalt() throws CachingServiceClientException {
    // 1. Fast path — salt already exists
    try {
        CachingServiceClient.KeyValue kv = cachingServiceClient.read("salt");
        return validateAndReturn(kv.getValue());
    } catch (CachingServiceClientException readEx) {
        if (readEx.getCause() != null) throw readEx; // network error — propagate

        // 2. Not found — try to create atomically
        byte[] newSalt = generateSalt();
        String encoded = Base64.getEncoder().encodeToString(newSalt);
        try {
            cachingServiceClient.create(new CachingServiceClient.KeyValue("salt", encoded));
            return encoded; // won the race
        } catch (CachingServiceClientException createEx) {
            if (createEx.getCause() != null) throw createEx; // unexpected error

            // 3. 409 Conflict — another instance won; read the winner's value
            CachingServiceClient.KeyValue winner = cachingServiceClient.read("salt");
            return validateAndReturn(winner.getValue());
        }
    }
}

private String validateAndReturn(String stored) {
    try {
        byte[] decoded = Base64.getDecoder().decode(stored);
        if (decoded.length == 16) return stored;
    } catch (IllegalArgumentException ignored) {}
    // Legacy or invalid format — regenerate
    log.warn("Invalid salt format; regenerating. Existing PAT hashes are invalidated.");
    byte[] newSalt = generateSalt();
    String encoded = Base64.getEncoder().encodeToString(newSalt);
    try { cachingServiceClient.update(new CachingServiceClient.KeyValue("salt", encoded)); }
    catch (CachingServiceClientException ignored) {}
    return encoded;
}
```

### Tests

**New tests in `SaltInitialization` nested class of `ApimlAccessTokenProviderTest`:**

- `givenRaceConditionOnCreate_whenAnotherInstanceWins_thenReadWinnerSalt()`:
  1. Mock `cachingServiceClient.read("salt")` first call → throw no-cause exception (not found).
  2. Mock `cachingServiceClient.create(any())` → throw no-cause exception (409 conflict).
  3. Mock `cachingServiceClient.read("salt")` second call → return a valid Base64 salt.
  4. Assert the returned salt matches the winner's value and `create()` is called exactly once.

- `givenNetworkErrorOnFirstRead_whenInitializing_thenPropagateException()`:
  Mock `cachingServiceClient.read("salt")` → throw `CachingServiceClientException` with an `IOException` cause. Assert the exception propagates and `create()` is never called.

- `givenNetworkErrorOnCreateAfterMiss_whenInitializing_thenPropagateException()`:
  Mock read → 404 (no cause), mock create → network error (with cause). Assert the exception propagates.

**Update existing `givenNoSaltInCache_whenInitializing_thenCreateNewOne()`:**
Confirm `create()` is called exactly once and the value is valid Base64 decoding to 16 bytes.
