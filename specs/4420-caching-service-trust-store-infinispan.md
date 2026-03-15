# #4420 — Caching service is not handling trust store well (in case of Infinispan mode)

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4420
**Labels:** bug, Priority: High, High Availability | **Created:** 2025-12-12 | **State:** open

---

## Description

`caching-service/src/main/resources/infinispan.xml` configures JGroups `SSL_KEY_EXCHANGE` with placeholders for keystore and truststore properties (`${infinispan.ssl.trustStore}`, `${infinispan.ssl.trustStoreType}`, `${infinispan.ssl.trustStorePassword}`). However, these `infinispan.ssl.*` properties are never populated in `application.yml` — there is no mapping from the standard APIML `server.ssl.trust-store*` properties to the `infinispan.ssl.*` namespace. The placeholders resolve to empty strings at startup, resulting in JGroups attempting to configure SSL without a valid truststore and failing silently or falling back to an insecure configuration.

This prevents TLS-secured Infinispan cluster formation in multi-node HA deployments.

---

## Acceptance Criteria

- When `caching.storage.mode=infinispan` and standard APIML SSL properties (`server.ssl.key-store`, `server.ssl.trust-store`, etc.) are configured, the JGroups `SSL_KEY_EXCHANGE` element receives the correct keystore and truststore paths.
- The caching-service starts without SSL errors in Infinispan mode when valid keystores are configured.
- A startup validation bean rejects startup with a clear error message if `caching.storage.mode=infinispan` is set but `server.ssl.trust-store` is blank (unless the `attlsServer` profile is active).
- Two caching-service nodes can form an Infinispan cluster over TLS using the same keystore/truststore configuration as the rest of the APIML services.

---

## Technical Solution

### Files to change

- `caching-service/src/main/resources/application.yml` — add `infinispan.ssl.*` property mappings
- `caching-service/src/main/resources/infinispan.xml` — verify attribute names match JGroups documentation
- New: `caching-service/src/main/java/org/zowe/apiml/caching/config/InfinispanSslValidator.java` — startup validator

### Changes

**`application.yml` — property mapping**

```yaml
infinispan:
  ssl:
    keyStore: ${server.ssl.key-store:}
    keyStoreType: ${server.ssl.key-store-type:PKCS12}
    keyStorePassword: ${server.ssl.key-store-password:}
    trustStore: ${server.ssl.trust-store:}
    trustStoreType: ${server.ssl.trust-store-type:PKCS12}
    trustStorePassword: ${server.ssl.trust-store-password:}
```

Use the kebab-case form (`server.ssl.key-store`) consistent with Spring Boot's relaxed binding. Verify property key capitalisation matches the actual keys used in `application.yml` (`trustStore` vs `trust-store`).

**Startup validator**

```java
@Component
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanSslValidator implements InitializingBean {

    @Value("${infinispan.ssl.trustStore:}")
    private String trustStore;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Override
    public void afterPropertiesSet() {
        boolean atTls = activeProfiles.contains("attlsServer");
        if (!atTls && (trustStore == null || trustStore.isBlank())) {
            throw new IllegalStateException(
                "infinispan.ssl.trustStore must be configured when " +
                "caching.storage.mode=infinispan and AT-TLS is not active");
        }
    }
}
```

### Tests

**New `InfinispanConfigurationTest`** (`@SpringBootTest` with Infinispan profile):
- Start the caching-service with `caching.storage.mode=infinispan` and mock SSL properties pointing to test keystores. Assert `EmbeddedCacheManager` bean is created without throwing `IllegalArgumentException` about empty truststore.

**New `InfinispanSslValidatorTest`:**
- `givenInfinispanModeAndNoTrustStore_whenStartup_thenThrowIllegalStateException()`.
- `givenInfinispanModeAndTrustStoreConfigured_whenStartup_thenNoException()`.
- `givenInfinispanModeAndAtTlsProfile_whenNoTrustStore_thenNoException()` — AT-TLS handles TLS externally, so truststore is not required.

**Integration test (two-node cluster):**
Start two caching-service instances in Infinispan mode with TLS and assert that `GET /application/health` on both nodes reports `clusterSize: 2` (requires the health endpoint from #4427).
