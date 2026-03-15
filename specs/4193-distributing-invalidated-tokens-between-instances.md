# #4193 — Distributing invalidated tokens between instances is not working in all cases

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4193
**Labels:** bug, Priority: High, High Availability | **Created:** 2025-07-02 | **State:** open

---

## Description

`AuthenticationService.invalidateJwtToken()` distributes revoked tokens to all currently-registered ZAAS peers at logout time via `DELETE /zaas/api/v1/auth/invalidate/{token}`. A complementary pull mechanism exists via `GET /distribute/{instanceId}` which replays locally cached invalidations to a specific instance, but it is never called automatically.

The gap: if a new ZAAS instance starts *after* tokens have been revoked, it never receives those historical invalidations. Its local `invalidatedJwtTokens` Spring Cache (EhCache, per-instance, ephemeral) is empty. The new instance will accept revoked tokens as valid until the next scheduled distribution or logout event.

---

## Acceptance Criteria

- A ZAAS instance that starts after previous logouts occurred receives all historically revoked tokens from its peers within a bounded time of completing Eureka registration.
- The startup synchronisation is idempotent: if `EurekaInstanceRegisteredEvent` fires multiple times, revocation history is not replayed redundantly.
- If a peer is unreachable during startup sync, the remaining reachable peers are still contacted (resilient iteration).
- The long-term fix (shared Infinispan-backed cache via #4172) supersedes the startup sync mechanism — the startup sync is an interim fix.
- Existing push-distribution behaviour (on logout, push to all current peers) is unchanged.

---

## Technical Solution

### Short-term fix: startup synchronisation

### Files to change

- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/AuthenticationService.java`
- `zaas-service/src/test/java/org/zowe/apiml/zaas/security/service/AuthenticationServiceTest.java`

### Changes

**Add a startup sync listener in `AuthenticationService`:**

```java
private volatile boolean startupSyncDone = false;

@EventListener(EurekaInstanceRegisteredEvent.class)
public void onInstanceRegistered(EurekaInstanceRegisteredEvent event) {
    if (startupSyncDone) return; // idempotent
    startupSyncDone = true;

    String myInstanceId = eurekaClient.getApplicationInfoManager()
        .getInfo().getInstanceId();
    Application zaas = eurekaClient.getApplication(CoreService.ZAAS.getServiceId());
    if (zaas == null) return;

    zaas.getInstances().stream()
        .filter(peer -> !myInstanceId.equals(peer.getInstanceId()))
        .forEach(peer -> {
            try {
                String url = EurekaUtils.getUrl(peer)
                    + AuthController.CONTROLLER_PATH
                    + "/distribute/" + myInstanceId;
                restTemplate.getForObject(url, Void.class);
            } catch (Exception e) {
                log.warn("Failed to sync invalidations from peer {}: {}",
                    peer.getInstanceId(), e.getMessage());
            }
        });
}
```

Note: `EurekaInstanceRegisteredEvent` fires when **this** instance registers, so calling `GET /distribute/{myInstanceId}` on each peer causes them to push their history to the new instance.

### Long-term fix: shared cache (tracked in #4172)

Replace `@Cacheable("invalidatedJwtTokens")` with direct reads from the Caching service (Infinispan-backed). With a shared store, new instances automatically see all historical invalidations without any startup sync.

### Tests

**New `GivenStartupTokenSyncTest` in `AuthenticationServiceTest`:**
- `givenMultiplePeers_whenStartupSyncTriggered_thenCallsDistributeOnAllPeers()` — mock Eureka returning 2 peers + self, assert `restTemplate.getForObject()` called exactly twice with the correct URL.
- `givenNoPeers_whenStartupSyncTriggered_thenNoRestCalls()`.
- `givenPeerCallFails_whenStartupSync_thenContinuesWithRemainingPeers()` — first peer throws `HttpClientErrorException`, assert the second peer is still contacted.
- `givenEventFiredTwice_whenStartupSync_thenSyncOccursOnlyOnce()` — fire `EurekaInstanceRegisteredEvent` twice, assert `restTemplate.getForObject()` is called only during the first event.

**Update `GivenCacheJWTTest`:**
Add an end-to-end scenario: instantiate two `AuthenticationService` beans sharing a mock distributed cache, invalidate a token on the first, trigger startup sync on the second, assert the second rejects the token.
