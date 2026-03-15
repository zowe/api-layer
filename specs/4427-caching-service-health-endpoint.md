# #4427 — Caching service health endpoint

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4427
**Labels:** enhancement, Priority: Medium | **Created:** 2025-12-12 | **State:** open

---

## Description

The caching-service's `/application/health` endpoint (Spring Boot Actuator) reports `UP` based on application-level checks but has no visibility into JGroups/Infinispan cluster connectivity. A caching-service node can be fully started and passing its Spring health check while being isolated from the rest of the Infinispan cluster — silently causing split-brain where different nodes serve different subsets of data.

Load balancers and orchestrators (Kubernetes, z/OS WLM) cannot detect this condition and will continue routing requests to isolated nodes.

---

## Acceptance Criteria

- `/application/health` reports a component named `infinispan` (or similar) when the storage mode is `infinispan`.
- The component status is `UP` when the number of connected cluster members meets or exceeds `infinispan.cluster.expectedNodes`.
- The component status is `DOWN` when the cluster is below the expected size, with details showing `clusterSize` and `expectedSize`.
- When the storage mode is `inMemory` or `redis`, the health component reports `UP` with a detail indicating the non-clustered mode (e.g., `"mode": "standalone"`).
- The health indicator does not propagate exceptions from `EmbeddedCacheManager` — any internal error results in `DOWN`, not a 500 from the actuator.
- A `WARN` log is emitted when cluster size drops below `expectedNodes`; an `INFO` log is emitted on recovery.
- `infinispan.cluster.expectedNodes` defaults to `1` (standalone) and is documented: for a 2-node HA cluster, operators must set it to `2`.

---

## Technical Solution

### Files to change

- New: `caching-service/src/main/java/org/zowe/apiml/caching/health/InfinispanHealthIndicator.java`
- `caching-service/src/main/resources/application.yml` — add `infinispan.cluster.expectedNodes: 1`

### Changes

**`InfinispanHealthIndicator`**

```java
@Component
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private EmbeddedCacheManager cacheManager;

    @Value("${infinispan.cluster.expectedNodes:1}")
    private int expectedNodes;

    @Override
    public Health health() {
        if (cacheManager == null) {
            return Health.up().withDetail("mode", "standalone").build();
        }
        try {
            int clusterSize = cacheManager.getMembers().size();
            // getMembers() includes the local node, so size >= 1 always
            if (clusterSize < expectedNodes) {
                log.warn("Infinispan cluster size {} is below expected {}",
                    clusterSize, expectedNodes);
                return Health.down()
                    .withDetail("clusterSize", clusterSize)
                    .withDetail("expectedSize", expectedNodes)
                    .build();
            }
            return Health.up().withDetail("clusterSize", clusterSize).build();
        } catch (Exception e) {
            log.error("Failed to determine Infinispan cluster health", e);
            return Health.down(e).build();
        }
    }
}
```

Note: do not call `cacheManager.executor()` — it is unused and unnecessary.

**`application.yml`**

```yaml
infinispan:
  cluster:
    expectedNodes: 1  # Set to cluster size in HA deployments (e.g., 2 for a 2-node cluster)
```

### Tests

**New `InfinispanHealthIndicatorTest`** with `@Nested` classes:

- `GivenInfinispanNotConfigured`: inject `null` `cacheManager` → assert `Health.status == UP` and detail contains `"mode": "standalone"`.
- `GivenClusterSizeAtMinimum`: mock `cacheManager.getMembers()` returning a list of size equal to `expectedNodes` (e.g., 2) → assert `UP` with `clusterSize: 2`.
- `GivenClusterSizeBelowMinimum`: mock `getMembers()` returning size 1 when `expectedNodes=2` → assert `DOWN` with details `clusterSize: 1`, `expectedSize: 2`.
- `GivenClusterSizeExceedsMinimum`: mock size 3 with `expectedNodes=2` → assert `UP`.
- `GivenGetMembersThrows`: mock `getMembers()` throwing `RuntimeException` → assert `DOWN` (exception is caught, not propagated).

**Integration test:**
Start the caching-service with `caching.storage.mode=inMemory` and call `GET /application/health`. Assert the JSON response contains a component with status `UP` and a non-null detail indicating standalone mode.
