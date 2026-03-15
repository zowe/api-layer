# #4511 — Sticky session and HA

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4511
**Labels:** bug, new | **Created:** 2026-03-12 | **State:** open

---

## Description

`DeterministicLoadBalancer` implements sticky-session routing by selecting exactly one service instance (the cached preferred instance) and returning it as a singleton list. If that instance is unhealthy but still present in the Eureka registry, every request is routed exclusively to the dead instance and all requests fail until Eureka deregisters it.

The Gateway's retry filter (`RoutingConfig`) is configured to retry on HTTP 503 up to 5 times, but because the load balancer returns only the failed instance, each retry hits the same dead instance. The retry mechanism cannot help because it re-invokes the load balancer, which again returns the same preferred-but-dead instance (the cache still points to it).

---

## Acceptance Criteria

- When the sticky-session preferred instance is reachable, requests are always routed to it (sticky behaviour unchanged).
- When the sticky-session preferred instance is unreachable (connection refused or timeout), the Gateway automatically falls back to another available instance within the same retry cycle — without waiting for Eureka deregistration.
- On successful fallback to a different instance, the load balancer cache is updated so subsequent requests stick to the new working instance.
- The existing `X-InstanceId` header override behaviour is unchanged.
- Services without `apiml.lb.type=authentication` metadata are unaffected.

---

## Technical Solution

### Files to change

- `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/DeterministicLoadBalancer.java`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/config/NettyRoutingFilterApiml.java` — clear cache on `ConnectException`
- Optionally a new post-routing `GatewayFilter` to update the cache on successful retry

### Changes

**1. Return a prioritised list instead of a singleton in `DeterministicLoadBalancer.filterInstances()`**

```java
// Replace chooseOne(instanceId, user, serviceInstances) singleton return with:
private List<ServiceInstance> buildOrderedList(String preferredInstanceId,
                                               List<ServiceInstance> all) {
    List<ServiceInstance> ordered = new ArrayList<>(all.size());
    all.stream()
       .filter(i -> preferredInstanceId.equals(i.getInstanceId()))
       .findFirst()
       .ifPresent(ordered::add);
    all.stream()
       .filter(i -> !preferredInstanceId.equals(i.getInstanceId()))
       .forEach(ordered::add);
    return ordered;
}
```

The cache `store()` call (which records the chosen instance) should still record the first element of this list (the preferred instance), so the preference is preserved for future requests.

**2. Clear the cache on connection failure in `NettyRoutingFilterApiml`**

When a `ConnectException` / `ConnectTimeoutException` is caught (see also #4512), extract the preferred `instanceId` from the exchange attribute and call `LoadBalancerCache.delete(user, serviceId)` before throwing `ServiceNotAccessibleException`. This ensures the next retry (via `RetryGatewayFilter`) re-invokes the load balancer without the dead instance at the top of the list.

**3. Confirm the correct attribute name**

Verify the Spring Cloud Gateway attribute that carries the resolved instance (currently believed to be `LoadBalancerUriTools.ORIGINAL_INSTANCE_ATTR`) before implementing the post-routing cache-update filter.

### Tests

**Update existing tests in `DeterministicLoadBalancerTest`:**
- `whenInstanceExists_thenUpdateList()` (cookie and header variants): change assertion from `assertEquals(1, chosenInstances.size())` to `assertEquals(2, chosenInstances.size())`, and add `assertEquals("instance1", chosenInstances.get(0).getInstanceId())`, `assertEquals("instance2", chosenInstances.get(1).getInstanceId())`.
- `whenInstanceDoesNotExist_thenUpdatePreference()`: verify the returned list has size 2 and the first element is the fallback instance.

**New tests in `DeterministicLoadBalancerTest`:**
- `whenPreferredInstanceIsUnavailable_thenCacheIsCleared_andNextCallReturnsUnordered()`: simulate cache pointing to `"instance1"`, trigger a `ConnectException` from routing, assert `lbCache.delete("USER", "service")` is called, then assert the next invocation of `loadBalancer.get(request)` returns both instances without a fixed first element.
- `whenCacheCleared_thenNextRequestPicksAnyInstance()`: after a `delete()`, confirm the load balancer stores a fresh preference from the available instances.
