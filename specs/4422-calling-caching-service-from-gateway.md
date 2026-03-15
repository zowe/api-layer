# #4422 — Calling Caching service from Gateway using micro-services

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4422
**Labels:** bug, Priority: Low, High Availability | **Created:** 2025-12-12 | **State:** open

---

## Description

The Gateway calls the Caching service via `lb://cachingservice`, routing through Spring Cloud load balancer. In a multi-Zowe-instance HA topology (where each Zowe instance has its own Gateway and Caching service), this causes the Gateway to route to any registered Caching service instance, including those belonging to different Zowe instances.

The result is cross-instance state sharing for load-balancer sticky-session records and other per-instance cached data. A user's session affinity set on Zowe instance A may be silently stored in Zowe instance B's Caching service, leading to routing inconsistencies.

---

## Acceptance Criteria

- In a multi-Zowe-instance deployment, the Gateway routes Caching service calls to the Caching service instance belonging to the same Zowe instance (identified by matching `apiml.zoweId` metadata).
- If no local Caching service instance is available, the Gateway falls back to any available instance and emits a `WARN` log indicating cross-instance routing.
- The instance-aware supplier is applied only to the `cachingservice` service ID — other services continue to use the standard load balancer.
- The change is transparent to the Caching service; no changes are required on the Caching service side.

---

## Technical Solution

### Files to change

- New: `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/ZoweInstanceAwareServiceInstanceListSupplier.java`
- New: `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/CachingServiceLoadBalancerConfiguration.java`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/GatewayApplication.java` (or equivalent) — add `@LoadBalancerClient(name = "cachingservice", configuration = CachingServiceLoadBalancerConfiguration.class)`

### Changes

**`ZoweInstanceAwareServiceInstanceListSupplier`**

```java
public class ZoweInstanceAwareServiceInstanceListSupplier
        implements ServiceInstanceListSupplier {

    private final ServiceInstanceListSupplier delegate;
    private final String localZoweId;

    @Override
    public Flux<List<ServiceInstance>> get() {
        return delegate.get().map(instances -> {
            List<ServiceInstance> local = instances.stream()
                .filter(i -> localZoweId.equals(
                    i.getMetadata().get("apiml.zoweId")))
                .collect(toList());
            if (local.isEmpty()) {
                log.warn("No local caching service instance found for zoweId={}; " +
                    "falling back to cross-instance routing", localZoweId);
                return instances;
            }
            return local;
        });
    }
}
```

**`CachingServiceLoadBalancerConfiguration`** — applies only to `cachingservice`:

```java
@Configuration
public class CachingServiceLoadBalancerConfiguration {
    @Bean
    public ServiceInstanceListSupplier cachingServiceInstanceListSupplier(
            ConfigurableApplicationContext context,
            @Value("${apiml.zoweId:}") String localZoweId) {
        return new ZoweInstanceAwareServiceInstanceListSupplier(
            ServiceInstanceListSupplier.builder()
                .withDiscoveryClient()
                .build(context),
            localZoweId);
    }
}
```

### Tests

**New `ZoweInstanceAwareServiceInstanceListSupplierTest`:**
- `givenLocalInstanceExists_thenReturnOnlyLocalInstance()` — two instances with different `apiml.zoweId`, local `zoweId=A`, assert only the `zoweId=A` instance is returned.
- `givenNoLocalInstanceExists_thenFallbackToAll_andLogWarn()` — no instance matches → assert all instances returned and a `WARN` log is captured.
- `givenSingleInstance_thenAlwaysReturnIt()` — trivial single-instance case.
- `givenLocalZoweIdNotConfigured_thenReturnAllInstances()` — `apiml.zoweId` is empty/blank → no filtering, return all.
