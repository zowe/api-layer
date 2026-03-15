# #4510 — Unnecessary call of caching service during routing

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4510
**Labels:** bug, new | **Created:** 2026-03-12 | **State:** open

---

## Description

`DeterministicLoadBalancer.get()` calls `cache.retrieve(user, serviceId)` whenever a user principal can be extracted from the request token. This happens regardless of whether the target service has sticky-session routing enabled (`apiml.lb.type=authentication` metadata). The guard (`shouldIgnore()`) that short-circuits sticky-session logic runs only *after* the cache has already been fetched.

`LoadBalancerCache.cachingServiceAvailability()` is called inside every `retrieve()` and `store()`, issuing an `eurekaClient.getApplication("cachingservice")` call. This means every authenticated request to every service — including those that have no sticky-session requirement — generates two unnecessary remote calls: one to Eureka (to check caching service availability) and one to the caching service itself.

Impacts:
- Increased latency for all authenticated requests to non-sticky services.
- Unnecessary coupling: if the Caching service is down, requests to non-sticky services fail even though the caching service is not needed.

---

## Acceptance Criteria

- For services where `apiml.lb.type` is **not** `authentication`, `LoadBalancerCache.retrieve()` is never called, regardless of whether the request carries an authentication token.
- For services where `apiml.lb.type=authentication` is set but the request carries no token (unauthenticated), `LoadBalancerCache.retrieve()` is still not called.
- When the `X-InstanceId` header is present, `LoadBalancerCache.retrieve()` is not called regardless of metadata or token presence.
- If the Caching service is down, requests to non-sticky services succeed normally.
- Sticky-session behaviour for services with `apiml.lb.type=authentication` is unchanged.

---

## Technical Solution

### Files to change

- `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/DeterministicLoadBalancer.java`

### Changes

Reorder the evaluation in `DeterministicLoadBalancer.get()` so metadata is checked first:

```java
@Override
public Flux<List<ServiceInstance>> get(Request request) {
    String serviceId = getServiceId();
    if (serviceId == null) return Flux.empty();

    return delegate.get(request)
        .flatMap(serviceInstances -> {
            // 1. Check X-InstanceId header first — no token parsing, no cache
            String instanceIdHeader = getInstanceIdFromHeader(request.getContext());
            if (instanceIdHeader != null) {
                return Flux.just(checkInstanceIdHeader(instanceIdHeader, serviceInstances));
            }
            // 2. Check sticky-session metadata — no cache call if not sticky
            if (!isStickySessionEnabled(serviceInstances)) {
                return Flux.just(serviceInstances);
            }
            // 3. Only now extract the principal and call the cache
            return getSub(request.getContext())
                .filter(user -> !user.isEmpty())
                .flatMap(user -> cache.retrieve(user, serviceId)
                    .onErrorResume(t -> Mono.just(LoadBalancerCacheRecord.NONE))
                    .flatMapMany(record -> filterInstances(user, serviceId, record, serviceInstances, request.getContext())))
                .switchIfEmpty(Flux.just(serviceInstances));
        });
}
```

Where `isStickySessionEnabled(serviceInstances)` reads `apiml.lb.type` from the metadata of the first instance in the list (all instances of a service share the same metadata).

### Tests

**Update existing tests in `DeterministicLoadBalancerTest`:**
- `whenServiceDoesNotHaveMetadata_thenUseDefaultList()`: add `verify(lbCache, never()).retrieve(anyString(), anyString())`.
- `whenServiceDoesNotUseSticky_thenUseDefaultList()`: add `verify(lbCache, never()).retrieve(anyString(), anyString())`.

**New tests in `DeterministicLoadBalancerTest`:**
- `whenStickyDisabled_andCachingServiceThrows_thenRequestSucceeds()`: configure `lbCache.retrieve()` to throw `CachingServiceClientException`, set service metadata to non-sticky, assert the full instance list is returned without error.
- `whenInstanceIdHeaderPresent_thenCacheNotCalled()`: set `X-InstanceId` header to a valid instance, provide a valid auth token, assert `lbCache.retrieve()` is never called and the specified instance is returned.
- `whenStickyEnabled_andNoToken_thenCacheNotCalled()`: set `apiml.lb.type=authentication` but provide no token in the request, assert `lbCache.retrieve()` is never called and the full instance list is returned.
