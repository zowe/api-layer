# High Availability Issues — Analysis and Proposed Solutions

Analysis of all open issues labeled **High Availability** in the
[zowe/api-layer](https://github.com/zowe/api-layer/issues) repository.
Each section covers: current behaviour, root cause, and a concrete implementation proposal
with exact file paths and line numbers.

---

## Table of Contents

| # | Issue | Complexity |
|---|-------|-----------|
| [#4429](#4429--lpar-affinity) | LPAR affinity | Medium |
| [#4423](#4423--storage-in-caching-service-by-caller-dn-fragility) | Storage in Caching service by caller | Medium |
| [#4422](#4422--calling-caching-service-from-gateway-self-routing-no-retry) | Calling Caching service from Gateway | Medium |
| [#4420](#4420--caching-service-trust-store-in-infinispan-mode) | Caching service trust store (Infinispan) | Small |
| [#4409](#4409--listen-on-single-ip-address-dvipa-support) | Listen on single IP address (DVIPA) | Large |
| [#4405](#4405--group-instances) | Group instances | Large |
| [#4400](#4400--infinispan-ha-configuration-complexity) | Infinispan HA configuration complexity | Medium |
| [#4398](#4398--remove-vsam-support) | Remove VSAM support | Small |
| [#4378](#4378--enabler-prefers-local-discovery-service-url) | Enabler prefers local DS URL | Small |
| [#4193](#4193--token-redistribution-on-restart) | Token redistribution on restart | Medium |
| [#15](#15--active-health-checks-for-statically-defined-api-services) | Active health checks for static services | Large |

---

## #4429 — LPAR Affinity

**Goal:** The Gateway should prefer service instances on the same LPAR (or host) and only
fall back to remote instances when local ones are unavailable or saturated.

### Current Behaviour

`DeterministicLoadBalancer` extends `SameInstancePreferenceServiceInstanceListSupplier`
(`DeterministicLoadBalancer.java:55`). The load-balancer pipeline in
`CustomLoadBalancerConfiguration.java` is:

```
ServiceInstanceListSupplier.builder().withDiscoveryClient()
    → DeterministicLoadBalancer (JWT sticky sessions)
```

`DeterministicLoadBalancer.chooseOne()` at line 240 uses `stream.findAny()` — a completely
arbitrary pick from the available instance list. Although the metadata allow-list in
`gateway-service/src/main/resources/application.yml` (line referencing
`zos.sysname,zos.system,zos.sysplex,zos.lpar`) includes LPAR-identifying keys, these values
are **collected** from registered services but never consulted during instance selection.

Spring Cloud LoadBalancer's built-in `ZonePreferenceServiceInstanceListSupplier` is not
used — there is no `.withZonePreference()` or similar call anywhere in the gateway.

**Relevant code:**
- `gateway-service/.../loadbalancer/DeterministicLoadBalancer.java:55,235–243` — extends `SameInstancePreferenceServiceInstanceListSupplier`; `chooseOne()` picks arbitrarily with `findAny()`
- `gateway-service/.../loadbalancer/CustomLoadBalancerConfiguration.java:36–41` — single `withDiscoveryClient()` step, no locality filter
- `gateway-service/.../loadbalancer/DeterministicRoutingListSupplierBuilder.java:25–32` — builder only exposes `withStickySessionRouting()`, no zone step

### Root Cause

1. No LPAR-identity metadata is written into Eureka by the enabler or the gateway.
2. The load balancer supplier chain has no "filter by locality" step.
3. `findAny()` makes no guarantee about ordering; the instance returned depends on the
   underlying Eureka list order, not on proximity.

### Proposed Solution

**Step 1 — Populate locality metadata at registration time.**

Add a configurable `apiml.zone` metadata key to the gateway's Eureka registration
(`gateway-service/src/main/resources/application.yml`) and to the onboarding enabler
(`EurekaInstanceConfigCreator.createMetadata()`):

```yaml
# gateway-service/src/main/resources/application.yml
eureka:
  instance:
    metadata-map:
      apiml.zone: ${apiml.service.zone:${apiml.service.hostname}}
```

The `apiml.service.zone` property defaults to the service's own hostname when not set,
so services on the same host are automatically in the same zone without any configuration
change. On z/OS, operators can set it to the LPAR name or sysplex identifier.

**Step 2 — Insert a locality-aware supplier step in the load balancer chain.**

Add a new `LocalityPreferenceServiceInstanceListSupplier` that:
1. Reads `apiml.zone` from the requesting gateway's own Eureka metadata (injected once
   at construction time from `EurekaClient.getApplicationInfoManager().getInfo().getMetadata()`).
2. Partitions the instance list into "local" (same zone) and "remote" (different zone).
3. Returns local instances when any are available; returns the full list only when the local
   set is empty — i.e., never blocks routing.

The supplier overrides `get(Request request)` (matching the actual signature used in
`DeterministicLoadBalancer.java:83`), not the no-arg `get()`, because the load balancer
chain already passes a `Request`:

```java
// New file: gateway-service/.../loadbalancer/LocalityPreferenceServiceInstanceListSupplier.java
public class LocalityPreferenceServiceInstanceListSupplier
        extends DelegatingServiceInstanceListSupplier {

    private final String localZone;

    public LocalityPreferenceServiceInstanceListSupplier(
            ServiceInstanceListSupplier delegate, String localZone) {
        super(delegate);
        this.localZone = localZone;
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return getDelegate().get(request).map(instances -> {
            if (localZone == null || localZone.isBlank()) return instances;
            List<ServiceInstance> local = instances.stream()
                .filter(i -> localZone.equals(i.getMetadata().get("apiml.zone")))
                .toList();
            return local.isEmpty() ? instances : local;
        });
    }
}
```

**Step 3 — Add `withLocalityPreference()` to `DeterministicRoutingListSupplierBuilder` and
wire it into `CustomLoadBalancerConfiguration.java`.**

`DeterministicRoutingListSupplierBuilder` currently has only one method,
`withStickySessionRouting()`. Add:

```java
// DeterministicRoutingListSupplierBuilder.java
public DeterministicRoutingListSupplierBuilder withLocalityPreference(String localZone) {
    builder.with((context, delegate) ->
        new LocalityPreferenceServiceInstanceListSupplier(delegate, localZone));
    return this;
}
```

In `CustomLoadBalancerConfiguration.java`, inject the local zone and insert the step
**before** sticky-session routing so locality filtering applies to the candidate pool first:

```java
// CustomLoadBalancerConfiguration.java
@Bean
public ServiceInstanceListSupplier stickySessionServiceInstanceListSupplier(
        ConfigurableApplicationContext ctx,
        LoadBalancerCache cache,
        EurekaClient eurekaClient,
        int expirationTime) {
    String localZone = Optional.ofNullable(eurekaClient.getApplicationInfoManager()
        .getInfo().getMetadata().get("apiml.zone")).orElse("");

    return new DeterministicRoutingListSupplierBuilder(
        ServiceInstanceListSupplier.builder().withDiscoveryClient()
    ).withLocalityPreference(localZone)         // new step
     .withStickySessionRouting(cache, expirationTime, Clock.systemUTC())
     .build(ctx);
}
```

Note: `withHealthChecks()` is **not** a method exposed by the current
`ServiceInstanceListSupplierBuilder` wrapper in this codebase. Do not add it; the
`HealthCheckServiceInstanceListSupplier` from Spring Cloud LoadBalancer requires a
separate `HealthCheckProperties` bean and has overhead unsuitable for hot-path routing.

**Step 4 — Handle fallback transparently.**

If all local instances fail, `NettyRoutingFilterApiml` raises `ServiceNotAccessibleException`
→ 503. The existing `Retry` filter in `RoutingConfig.java` (configured with
`retries: 5, statuses: SERVICE_UNAVAILABLE`) triggers a new load-balancer call. On retry,
the load balancer is called again and will return the same local instances again unless they
are marked DOWN in Eureka.

To force fallback to remote instances on retry, set a flag in exchange attributes on failure
that the supplier reads:

```java
// NettyRoutingFilterApiml.java — in the isConnectionRefused() error path:
exchange.getAttributes().put("apiml.lb.localityFailed", true);
```

```java
// LocalityPreferenceServiceInstanceListSupplier.get(Request request):
boolean localityFailed = (request.getContext() instanceof RequestDataContext ctx)
    && Boolean.TRUE.equals(ctx.getClientRequest().getAttributes().get("apiml.lb.localityFailed"));
if (localityFailed) return instances;   // widen to all instances on retry
```

**Configuration surface:** One optional property `apiml.service.zone`, defaulting to
`apiml.service.hostname`. No mandatory change for existing deployments.

---

## #4423 — Storage in Caching Service by Caller (DN Fragility)

**Goal:** The Caching service should reliably identify which service is writing/reading
PAT data without depending on a forgeable HTTP header, and without coupling the storage
namespace to the certificate's Subject DN.

### Current Behaviour

`CachingController.getCertificateServiceId()` (lines 299–304) resolves the caller's
storage namespace by two paths:

```java
// CachingController.java:299–304
private Optional<String> getCertificateServiceId(ServerHttpRequest request) {
    if (applicationInfo != null && applicationInfo.isModulith()) {
        return extractFromSslInfo(request);       // reads actual TLS peer cert — secure
    } else {
        return getHeader(request, "X-Certificate-DistinguishedName");  // trusts HTTP header
    }
}
```

In non-modulith mode, the DN comes from an HTTP header injected by the gateway's
`X509FilterFactory`. Any caller who can reach the caching service endpoint can inject
any DN and write into another service's storage bucket.

Additionally, if a service rotates its certificate to one with a different Subject DN,
all its stored PAT records become inaccessible immediately, because the new cert produces
a different header value.

**Relevant code:**
- `caching-service/.../api/CachingController.java:299–304` — non-modulith path trusts HTTP header
- `caching-service/src/main/resources/application.yml:76` — declares `X-Certificate-DistinguishedName` as an auth header
- `gateway-service/.../filters/X509FilterFactory.java:37–38` — injects `X-Certificate-CommonName` (`COMMON_NAME`) and `X-Certificate-DistinguishedName` (`DISTINGUISHED_NAME`) from the real TLS certificate

### Root Cause

1. In non-modulith mode, the caching service trusts an HTTP header over the TLS session,
   allowing forgery of caller identity.
2. The full Subject DN is unstable across certificate rotations and is unnecessarily verbose
   as a storage key.

### Proposed Solution

**Switch from Subject DN to Common Name (CN) as the storage namespace key.**

`X509FilterFactory` (line 38, `COMMON_NAME = "X-Certificate-CommonName"`) already injects
the certificate CN into every forwarded request. For APIML-onboarded services, the CN is
the `serviceId` — stable across certificate rotations as long as the CN does not change,
which is the standard practice.

**Step 1 — Update `CachingController.getCertificateServiceId()` to use CN.**

```java
// CachingController.java — replace getCertificateServiceId()
private Optional<String> getCertificateServiceId(ServerHttpRequest request) {
    if (applicationInfo != null && applicationInfo.isModulith()) {
        return extractFromSslInfo(request);   // secure path unchanged
    } else {
        // Use CN instead of full DN: stable across cert rotation, still gateway-injected
        return getHeader(request, "X-Certificate-CommonName");
    }
}
```

**Step 2 — Update the caching service's authentication header list.**

In `caching-service/src/main/resources/application.yml:76`, remove the DN header:

```yaml
authentication:
    scheme: x509
    headers: X-Certificate-Public,X-Certificate-CommonName   # remove X-Certificate-DistinguishedName
```

**Step 3 — Migrate existing stored records.**

Existing PAT records stored under a full DN key will not be found by the new CN-based
lookup. Add a one-time migration step in the caching service startup (or document it in
release notes) that re-keys any records matching the old DN pattern:
- Old key format: `CN=...,OU=...,O=...` (a DN string)
- New key format: `<serviceId>` (the CN value only)

If in-place migration is not feasible, document the breaking change and the requirement
to re-issue PATs after upgrading.

**Step 4 — Strengthen the caching service with mandatory client-certificate authentication.**

The caching service's `application.yml` already specifies `authentication.scheme: x509`.
Ensure the `server.ssl.clientAuth` property is set to `need` (not `want`) in the caching
service's TLS configuration so that connections without a valid client certificate are
rejected at the TLS handshake level — before the header is even read. This prevents
unauthenticated callers from forging any CN header.

> **Note on the header remaining forgeable:** Switching from DN to CN does not eliminate
> the fundamental forgery risk — the CN header can still be forged by a caller who bypasses
> the gateway. The definitive fix for non-modulith mode is to either (a) require mutual TLS
> at the caching service's server port so the CN is read from the TLS session directly
> (matching the modulith secure path), or (b) restrict the caching service's firewall to
> only accept connections from the gateway's IP addresses.

---

## #4422 — Calling Caching Service from Gateway (Self-Routing, No Retry)

**Goal:** The Gateway and ZAAS should call the Caching service directly (via Eureka
discovery), prefer local instances, and retry on transient failures.

### Current Behaviour

`CachingServiceClientRest.java` (gateway, lines 56–58) constructs the caching service URL
by routing through the gateway itself:

```java
// CachingServiceClientRest.java:56–58
this.cachingBalancerUrl = String.format("%s://%s/%s",
    gatewayClient.getGatewayConfigProperties().getScheme(),
    gatewayClient.getGatewayConfigProperties().getHostname(),  // gateway's own address
    CACHING_API_PATH);
```

The `CachingServiceClientRest` implementation is fully reactive (returns `Mono<Void>` /
`Mono<ApiKeyValue>`) — it already uses `WebClient`, not `RestTemplate`. There is no
`.retryWhen()` on any of the four operations (create/update/read/delete). A transient
network error or caching service restart immediately propagates as an error.

`zaas-service/.../cache/CachingServiceClient.java` (lines 63–68) also routes via the
gateway's own hostname and uses blocking `RestTemplate.exchange()` with no retry.

**Relevant code:**
- `gateway-service/.../caching/CachingServiceClientRest.java:34–35,48–59,61–141` — reactive WebClient, no retry, self-routing URL
- `zaas-service/.../cache/CachingServiceClient.java:40–43,63–68,78–84` — blocking RestTemplate, no retry, self-routing URL

### Root Cause

1. `GatewayClient.getGatewayConfigProperties().getHostname()` returns the gateway's own
   external hostname, routing caching calls through the gateway's own load-balancer.
2. Neither client discovers `cachingservice` instances directly from Eureka.
3. No retry or circuit-breaker on any caching operation.

### Proposed Solution

**Step 1 — Replace self-routing URL with direct Eureka instance discovery in
`CachingServiceClientRest.java`.**

The existing `WebClient` injection (`@Qualifier("webClientClientCert")`) should be kept
since the caching service requires mutual TLS. Replace the `GatewayClient`-based URL
construction with a `ReactiveDiscoveryClient` lookup:

```java
// CachingServiceClientRest.java — revised constructor and URL resolution
@Component
@Slf4j
@ConditionalOnMissingBean(name = "modulithConfig")
@RequiredArgsConstructor
public class CachingServiceClientRest implements CachingServiceClient {

    @Value("${apiml.cachingServiceClient.apiPath:/cachingservice/api/v1/cache}")
    private String CACHING_API_PATH;

    private final @Qualifier("webClientClientCert") WebClient webClient;
    private final ReactiveDiscoveryClient discoveryClient;
    // Remove: GatewayClient gatewayClient

    private Mono<String> getCachingServiceBaseUrl() {
        return discoveryClient.getInstances("cachingservice")
            .collectList()
            .flatMap(instances -> {
                if (instances.isEmpty()) {
                    return Mono.error(new ServiceNotAccessibleException(
                        "No cachingservice instances found in registry"));
                }
                // Prefer the instance whose host matches this gateway's own hostname
                // to reduce cross-LPAR calls (coordinates with #4429 solution)
                String myHost = InetAddress.getLocalHost().getHostName();
                ServiceInstance chosen = instances.stream()
                    .filter(i -> myHost.equalsIgnoreCase(i.getHost()))
                    .findFirst()
                    .orElse(instances.get(0));  // fall back to first available
                return Mono.just(chosen.getUri().toString());
            });
    }

    public Mono<Void> create(ApiKeyValue keyValue) {
        return getCachingServiceBaseUrl()
            .flatMap(base -> webClient.post()
                .uri(base + CACHING_API_PATH)
                .bodyValue(keyValue)
                .headers(c -> c.addAll(defaultHeaders))
                .exchangeToMono(handler -> handler.statusCode().is2xxSuccessful()
                    ? empty()
                    : error(new CachingServiceClientException(
                        handler.statusCode().value(),
                        "Unable to create caching key " + keyValue.getKey()))))
            .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                .filter(t -> !(t instanceof CachingServiceClientException)));
    }
    // Repeat the retryWhen pattern for update(), read(), delete()
}
```

**Step 2 — Apply the same change to `CachingServiceClient.java` in ZAAS.**

`CachingServiceClient` uses blocking `RestTemplate`. Replace `getGatewayAddress()` with
direct Eureka lookup using the already-available `EurekaClient`:

```java
// CachingServiceClient.java — replace getGatewayAddress()
private String getCachingServiceBaseUrl() {
    Application app = eurekaClient.getApplication("cachingservice");
    if (app == null || app.getInstances().isEmpty()) {
        throw new CachingServiceClientException(503,
            "No cachingservice instances found in Eureka registry");
    }
    // Select the locally-preferred instance if available
    String myHost = applicationInfoManager.getInfo().getHostName();
    return app.getInstances().stream()
        .filter(i -> myHost.equalsIgnoreCase(i.getHostName()))
        .findFirst()
        .map(EurekaUtils::getUrl)
        .orElseGet(() -> EurekaUtils.getUrl(app.getInstances().get(0)));
}
```

**Step 3 — Add retry to the blocking ZAAS caching client.**

Wrap `RestTemplate.exchange()` calls in a simple retry loop with exponential backoff.
Only retry on connectivity errors (`ResourceAccessException`), not on 4xx/5xx responses
from the caching service:

```java
private <T> ResponseEntity<T> executeWithRetry(Supplier<ResponseEntity<T>> operation) {
    int maxAttempts = 3;
    for (int i = 0; i < maxAttempts; i++) {
        try {
            return operation.get();
        } catch (ResourceAccessException e) {
            if (i == maxAttempts - 1) throw e;
            try {
                Thread.sleep(100L * (1L << i));  // 100ms, 200ms backoff
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
    }
    throw new IllegalStateException("unreachable");
}
```

**Step 4 — Keep the `@ConditionalOnMissingBean(name = "modulithConfig")` guard** on
`CachingServiceClientRest` so modulith mode continues using `CachingServiceClientApi`
(in-process, no HTTP calls needed).

---

## #4420 — Caching Service Trust Store in Infinispan Mode

**Goal:** Infinispan's JGroups key-exchange endpoint must correctly use the truststore so
instances can authenticate each other's TLS certificates when using PKCS12 keystores.

### Current Behaviour

The `infinispan.xml` `SSL_KEY_EXCHANGE` element **already has** the truststore attributes
added (lines 39–41):

```xml
<SSL_KEY_EXCHANGE keystore_name="${infinispan.ssl.keyStore}"
                  keystore_type="${infinispan.ssl.keyStoreType}"
                  keystore_password="${infinispan.ssl.keyStorePassword}"
                  truststore_name="${infinispan.ssl.trustStore}"
                  truststore_type="${infinispan.ssl.trustStoreType}"
                  truststore_password="${infinispan.ssl.trustStorePassword}"
                  .../>
```

However, `InfinispanConfig.cacheManager()` at lines 183–185 sets the JVM system properties
incorrectly:

```java
// InfinispanConfig.java:179–185
System.setProperty("infinispan.ssl.keyStoreType", keyStoreType);
System.setProperty("infinispan.ssl.keyStore", keyStore);
System.setProperty("infinispan.ssl.keyStorePassword", keyStorePass);

System.setProperty("infinispan.ssl.trustStoreType", keyStoreType);   // <-- wrong: uses keyStoreType
System.setProperty("infinispan.ssl.trustStore", keyStore);            // <-- wrong: uses keyStore path
System.setProperty("infinispan.ssl.trustStorePassword", keyStorePass); // <-- wrong: uses keyStorePass
```

Lines 183–185 copy the **keystore** values into the truststore system properties, not the
actual truststore values. The correct `trustStore`, `trustStoreType`, and `trustStorePass`
fields are injected at lines 77–84 but are never used for the Infinispan system properties.

Additionally, `SSL_KEY_EXCHANGE` has `require_client_authentication="false"` (line 46).
This means peer certificates are not verified even if the truststore is correctly set.

**Relevant code:**
- `caching-service/src/main/resources/infinispan.xml:36–47` — SSL_KEY_EXCHANGE element (truststore attributes present in XML ✓ but `require_client_authentication="false"` ✗)
- `caching-service/.../infinispan/config/InfinispanConfig.java:77–84` — `trustStore`, `trustStoreType`, `trustStorePass` correctly injected from config
- `caching-service/.../infinispan/config/InfinispanConfig.java:183–185` — system properties incorrectly set from keystore values, not truststore values

### Root Cause

Two bugs:
1. `System.setProperty("infinispan.ssl.trustStore", ...)` is populated with the keystore
   path and password instead of the truststore path and password.
2. `require_client_authentication="false"` disables peer certificate verification entirely,
   meaning the truststore configuration has no effect even when correctly set.

### Proposed Solution

**Fix 1 — Use the correct truststore values in the system properties.**

In `InfinispanConfig.cacheManager()`, replace lines 183–185:

```java
// Before (incorrect — copies keystore values into truststore properties):
System.setProperty("infinispan.ssl.trustStoreType", keyStoreType);
System.setProperty("infinispan.ssl.trustStore", keyStore);
System.setProperty("infinispan.ssl.trustStorePassword", keyStorePass);

// After (correct — uses the injected truststore fields):
System.setProperty("infinispan.ssl.trustStoreType", trustStoreType);
System.setProperty("infinispan.ssl.trustStore", trustStore);
System.setProperty("infinispan.ssl.trustStorePassword", trustStorePass);
```

**Fix 2 — Enable peer certificate verification.**

In `infinispan.xml`, change `require_client_authentication="false"` to `"true"`:

```xml
<SSL_KEY_EXCHANGE ...
                  require_client_authentication="true"
/>
```

This ensures that during the JGroups key-exchange handshake each node verifies the peer's
certificate against the configured truststore, preventing rogue nodes from joining the cluster.

**Note on keyrings (z/OS ICSF):** With keyrings, the keystore and truststore reference the
same SAF keyring path. After Fix 1, both keystore and truststore system properties will
correctly point to the same keyring path. No additional change is needed for keyring mode.

---

## #4409 — Listen on Single IP Address (DVIPA Support)

**Goal:** Allow multiple API ML instances to share a single virtual IP address (DVIPA)
for their REST APIs while using LPAR-local addresses for internal cluster communication
(JGroups, Eureka peer-to-peer replication).

### Current Behaviour

All services bind `server.address` to a single IP, and that same address is registered in
Eureka as the service's hostname and IP. There is no concept of a "virtual IP" distinct
from the "bind address."

Spring Boot's `server.address` controls both:
1. Which NIC the HTTP server binds to.
2. What is registered in Eureka (via `EurekaInstanceConfigCreator.java:40–56` and
   `ModulithConfig.java:180–197`).

Eureka requires unique `instanceId` values per instance. Currently:
```
instanceId = ${apiml.service.hostname}:${apiml.service.id}:${apiml.service.port}
```
If two LPAR nodes share the same DVIPA hostname, their `instanceId` values collide and
Eureka will treat them as the same instance, causing one to deregister the other.

**Relevant code:**
- `gateway-service/src/main/resources/application.yml:4–16` — `eureka.instance.hostname` and `instanceId` both from `apiml.service.hostname`
- `apiml/src/main/java/org/zowe/apiml/ModulithConfig.java:117–124,180–197` — single `hostname` and `ipAddress` for both binding and registration
- `onboarding-enabler-java/.../EurekaInstanceConfigCreator.java:40–56` — no virtual IP concept; `serviceIpAddress` is the only IP field in `ApiMediationServiceConfig`

### Root Cause

The configuration model conflates "listening address" with "registration address." DVIPA
requires:
- Listening on the DVIPA (shared virtual IP, identical across LPARs on the same sysplex).
- Registering in Eureka with the DVIPA so clients can reach any node via the shared address.
- Unique `instanceId` per LPAR to prevent Eureka registration collisions.
- JGroups (Infinispan) using the LPAR-local IP for cluster communication, not the DVIPA.

### Proposed Solution

**Step 1 — Decouple binding address from registration address.**

Introduce two optional properties:
- `apiml.service.listenAddress` — the IP the HTTP server binds to (DVIPA in DVIPA mode).
  Defaults to `${apiml.service.hostname}` when not set (no change for existing deployments).
- `apiml.service.lparHostname` — this node's LPAR-local hostname, used only as the
  `instanceId` discriminator. Defaults to `${apiml.service.hostname}` when not set.

Update `gateway-service/src/main/resources/application.yml`:

```yaml
server:
  address: ${apiml.service.listenAddress:${apiml.service.hostname}}

eureka:
  instance:
    hostname: ${apiml.service.hostname}          # shared DVIPA DNS name
    ip-address: ${apiml.service.hostname}
    # Instance ID uses the LPAR-local hostname to ensure uniqueness
    instanceId: ${apiml.service.lparHostname:${apiml.service.hostname}}:${apiml.service.id}:${apiml.service.port}
```

Apply the same pattern to `discovery-service`, `zaas-service`, `api-catalog-services`,
and `caching-service` application YAML files.

**Step 2 — Update `ModulithConfig.java` to read the two addresses separately.**

In `ModulithConfig.java` around line 186, separate the IP registered in Eureka from the
bind address:

```java
// ModulithConfig.java — InstanceInfo construction
String registrationHostname = environment.getProperty("apiml.service.hostname", "localhost");
String lparHostname = environment.getProperty("apiml.service.lparHostname", registrationHostname);

InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder()
    ...
    .setHostName(registrationHostname)
    .setIPAddr(registrationHostname)
    .setInstanceId(lparHostname + ":" + serviceId + ":" + port)
    ...;
```

`server.address` binds the HTTP server to `apiml.service.listenAddress` (already handled
by Spring Boot's standard `server.address` property — no additional code needed there).

**Step 3 — Separate JGroups bind address from the DVIPA.**

`InfinispanConfig.java` reads `jgroups.bind.address` from a `@Value` at line 89–90. In
DVIPA mode, JGroups must bind to the LPAR-local address (not the DVIPA) so that
inter-node cluster communication stays on the LPAR LAN fabric.

Change the default in `caching-service/src/main/resources/application.yml` to:

```yaml
jgroups:
  bind:
    address: ${apiml.service.lparHostname:${apiml.service.hostname}}
```

This requires no code change — it is a configuration-only update.

**Step 4 — Verify Eureka peer replication uses LPAR-local addresses.**

When the Discovery service registers itself in the peer list
(`eureka.client.serviceUrl.defaultZone`), each DS instance's URL must point to its
LPAR-local address (not the shared DVIPA) so Eureka peer replication flows over the direct
LPAR link. Operators must configure each DS instance's `serviceUrl` list accordingly —
document this requirement.

**Impact:** Zero mandatory change for non-DVIPA deployments (all new properties default to
`apiml.service.hostname`). DVIPA users set `apiml.service.listenAddress` and
`apiml.service.lparHostname` in their Zowe configuration.

---

## #4405 — Group Instances

**Goal:** Allow service instances to be grouped by metadata (e.g., by department, environment
tier, or customer segment) so clients can target a specific group via a request header or
URL prefix.

### Current Behaviour

The only metadata-based filtering in the load balancer is the boolean
`apiml.lb.type=authentication` check in `DeterministicLoadBalancer.lbTypeIsAuthentication()`
(line 260–267). No group concept exists.

`EurekaInstanceConfigCreator.createMetadata()` (lines 93–137) does not register any group
field. Although `flattenMetadata(config.getCustomMetadata())` at line 129 allows arbitrary
custom metadata, no routing component reads it.

**Relevant code:**
- `gateway-service/.../loadbalancer/DeterministicLoadBalancer.java:260–267`
- `gateway-service/.../loadbalancer/CustomLoadBalancerConfiguration.java:36–41`
- `onboarding-enabler-java/.../EurekaInstanceConfigCreator.java:93–137`
- `onboarding-enabler-java/.../config/ApiMediationServiceConfig.java` — no `group` field currently

### Root Cause

No group metadata key is defined in the APIML model, no group-aware supplier step exists in
the load balancer chain, and no URL routing convention exists to convey a group selection.

### Proposed Solution

**Step 1 — Define a standard group metadata key in the onboarding model.**

In `ApiMediationServiceConfig.java`, add:
```java
private String group;   // optional; null means "ungrouped" (no routing restriction)
```

In `EurekaInstanceConfigCreator.createMetadata()`, register it when present:
```java
if (StringUtils.isNotBlank(config.getGroup())) {
    metadata.put("apiml.group", config.getGroup());
}
```

**Step 2 — Add a `GroupFilterServiceInstanceListSupplier`.**

The supplier reads the requested group from the `X-APIML-Group` request header and filters
the instance list. When no group is requested, all instances are returned (no change in
behaviour). When a group is requested but no matching instances exist, the full list is
returned as a safe fallback (no outage if a group is misconfigured):

```java
public class GroupFilterServiceInstanceListSupplier
        extends DelegatingServiceInstanceListSupplier {

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        String requestedGroup = extractGroupFromRequest(request);
        return getDelegate().get(request).map(instances -> {
            if (requestedGroup == null || requestedGroup.isBlank()) {
                return instances;
            }
            List<ServiceInstance> grouped = instances.stream()
                .filter(i -> requestedGroup.equals(i.getMetadata().get("apiml.group")))
                .toList();
            return grouped.isEmpty() ? instances : grouped;  // fall back to all on no match
        });
    }

    private String extractGroupFromRequest(Request request) {
        if (request.getContext() instanceof RequestDataContext ctx) {
            return ctx.getClientRequest().getHeaders().getFirst("X-APIML-Group");
        }
        return null;
    }
}
```

**Step 3 — Add `withGroupFilter()` to `DeterministicRoutingListSupplierBuilder` and wire it.**

```java
// DeterministicRoutingListSupplierBuilder.java
public DeterministicRoutingListSupplierBuilder withGroupFilter() {
    builder.with((context, delegate) -> new GroupFilterServiceInstanceListSupplier(delegate));
    return this;
}
```

In `CustomLoadBalancerConfiguration.java`, insert the group filter **before** locality
preference and sticky-session routing so group selection happens first:

```java
return new DeterministicRoutingListSupplierBuilder(
    ServiceInstanceListSupplier.builder().withDiscoveryClient()
).withGroupFilter()              // applied first: narrow by group
 .withLocalityPreference(zone)   // then prefer local zone
 .withStickySessionRouting(cache, expirationTime, Clock.systemUTC())
 .build(ctx);
```

**Step 4 — Security consideration for the `X-APIML-Group` header.**

The `X-APIML-Group` header comes from the client and must be considered untrusted. Because
the filter's fallback returns all instances when the requested group does not exist, a
forged group name degrades to default routing — it does not allow access to unauthorized
instances. However, to prevent clients from gaining access to a group they should not reach,
the gateway should validate that the authenticated user is permitted to request the given
group. Add an optional allowlist check:

```yaml
apiml:
  gateway:
    groupRouting:
      enforceAuthorization: false   # default: any authenticated client can request any group
```

When `enforceAuthorization: true`, the gateway validates the requested group against an
SAF resource class rule (e.g., `APIML.GROUP.<groupName>`) before forwarding.

**Step 5 — URL-based group routing (optional extension).**

As an alternative to the header approach, add a `GroupRoutingFilter` that matches the
URL prefix `/group/{groupName}/{serviceId}/...`, extracts the group, sets the
`X-APIML-Group` header on the internal exchange, and rewrites the path to
`/{serviceId}/...`. This allows group selection via URL without requiring clients to
manage custom headers:

```
/group/production/myservice/api/v1/endpoint
    → X-APIML-Group: production
    → routes to myservice instances tagged apiml.group=production
```

**Backward compatibility:** Services without a `group` metadata key are returned for all
requests that do not specify a group. Fully additive — existing deployments are unaffected.

---

## #4400 — Infinispan HA Configuration Complexity

**Goal:** Reduce the number of mandatory configuration parameters for Infinispan clustering,
auto-computing values from existing Zowe/APIML configuration where possible.

### Current Behaviour

At minimum, the following parameters must be provided manually for a multi-node Infinispan
cluster (`InfinispanConfig.java:65–102`):

| Property | Current default | Notes |
|----------|----------------|-------|
| `caching.storage.infinispan.initialHosts` | `localhost[7600]` | Must enumerate all peers as `host[port]` — main pain point |
| `jgroups.bind.port` | `7600` | Must not conflict if multiple instances on same host |
| `jgroups.bind.address` | (injected from `@Value`) | Must match the correct network interface |
| `jgroups.keyExchange.port` | `7601` | Must not conflict |
| SSL keystore/truststore | Inherited from `server.ssl.*` | Auto-injected — no extra work needed |

The biggest pain point is `initialHosts`: it is a static list that must be manually
maintained whenever the cluster topology changes.

**Relevant code:**
- `caching-service/.../infinispan/config/InfinispanConfig.java:65–102,173–185`
- `caching-service/src/main/resources/infinispan.xml:23–25` — `TCPPING initial_hosts="${jgroups.tcpping.initial_hosts}"` (no default, required)
- `caching-service/src/main/resources/application.yml:25` — default `initialHosts: localhost[7600]`

### Root Cause

JGroups `TCPPING` requires a static list of initial contact nodes. No dynamic discovery
protocol (e.g., `DNS_PING`, `FILE_PING`) is configured. The static approach requires
operators to know all cluster members upfront and update the list when topology changes.

### Proposed Solution

**Step 1 — Auto-populate `initialHosts` from the Eureka registry.**

In `InfinispanConfig.cacheManager()`, auto-compute `initialHosts` when its value is the
sentinel string `"auto"`:

```java
// InfinispanConfig.java — new method
private String computeInitialHosts() {
    if (StringUtils.isNotBlank(initialHosts) && !"auto".equalsIgnoreCase(initialHosts)) {
        return initialHosts;   // respect explicit override
    }
    Application app = eurekaClient.getApplication("cachingservice");
    if (app == null || app.getInstances().isEmpty()) {
        log.warn("No cachingservice instances found in Eureka; using single-node mode (localhost[{}])", port);
        return "localhost[" + port + "]";
    }
    return app.getInstances().stream()
        .map(i -> i.getHostName() + "[" + port + "]")
        .collect(Collectors.joining(","));
}
```

Inject `EurekaClient` into `InfinispanConfig` (it is already a Spring-managed bean in
both modulith and microservices deployments). Set the default in `application.yml`:

```yaml
caching:
  storage:
    infinispan:
      initialHosts: auto   # use Eureka-based peer discovery by default
```

> **Limitation:** Eureka-based initial peer discovery is a **bootstrap-only** mechanism.
> JGroups computes `initial_hosts` once at startup. If new caching service instances join
> the cluster after startup, they will not automatically be discovered via this mechanism —
> they need to be in Eureka and the cluster must be restarted, or they must use JGroups'
> own join protocol (`MERGE3`, `GMS`) to merge into the running cluster. This is acceptable
> for most production topologies where the cluster size is known at deployment time.

**Step 2 — Auto-derive `jgroups.bind.address` from the APIML service hostname.**

Remove the separate `@Value("${jgroups.bind.address}")` injection and derive from
`apiml.service.hostname` (already injected for other purposes):

```java
// InfinispanConfig.java — remove @Value("${jgroups.bind.address}") field
// In cacheManager():
System.setProperty("jgroups.bind.address", hostname);  // reuse @Value("${apiml.service.hostname}")
```

Update `infinispan.xml` to use the same property name as a fallback:

```xml
<TCP bind_addr="${jgroups.bind.address,jgroups.tcp.address:SITE_LOCAL}"
     ...
```

(This line already exists in `infinispan.xml:10`; no XML change needed.)

**Step 3 — Auto-derive `jgroups.keyExchange.port` from `jgroups.bind.port + 1`.**

```java
// InfinispanConfig.java — in cacheManager():
int jgroupsPort = Integer.parseInt(port);
int resolvedKeyExchangePort = keyExchangePort.isBlank()
    ? jgroupsPort + 1
    : Integer.parseInt(keyExchangePort);
System.setProperty("jgroups.keyExchange.port", String.valueOf(resolvedKeyExchangePort));
```

**Step 4 — Update `start.sh` to default `initialHosts` to `auto`.**

In `caching-service-package/src/main/resources/bin/start.sh`, replace the block that
requires `ZWE_haInstance_hostname` and port-list construction with:

```sh
ZWE_configs_storage_infinispan_initialHosts="${ZWE_configs_storage_infinispan_initialHosts:-auto}"
```

This lets the Java code perform Eureka-based discovery unless the operator explicitly
provides a static list.

**Resulting mandatory parameters for HA (after changes):**
- `caching.storage.mode=infinispan`
- `jgroups.bind.port` (required only if the default 7600 conflicts)

All other parameters become auto-computed or optional. Documentation surface shrinks from
~10 required parameters to 1 mandatory + 1 situational.

---

## #4398 — Remove VSAM Support

**Goal:** Remove the deprecated VSAM storage backend from the Caching service before V4.

### Current Behaviour

VSAM is activated by `caching.storage.mode=vsam`. The backend in `VsamStorage.java` throws
`INCOMPATIBLE_STORAGE_METHOD` for 5 of the 9 `Storage` interface methods (lines 99–111,
213–219), making it incompatible with PAT and token-list operations introduced in V3.

`VsamConfiguration.java:35` emits a runtime deprecation log warning on startup. There is
no `@Deprecated` Java annotation on any VSAM class and no compile-time warning.

**Files to delete (22 total):**
- 11 source files in `caching-service/src/main/java/.../caching/service/vsam/`:
  `VsamStorage.java`, `VsamConfig.java`, `VsamConfiguration.java`, `VsamFile.java`,
  `VsamRecord.java`, `VsamKey.java`, `VsamInitializer.java`, `VsamFileProducer.java`,
  `ZFileProducer.java`, `RemoveOldestStrategy.java`, `EvictionStrategyProducer.java`
- 9 test files in `caching-service/src/test/java/.../caching/service/vsam/`
- 1 message key in `caching-service/src/main/resources/caching-log-messages.yml`
- VSAM variable blocks in `caching-service-package/src/main/resources/bin/start.sh`

### Proposed Solution

**Removal plan (single PR):**

1. **Delete all 22 VSAM source and test files.**

2. **Add an early-startup guard** so deployments still using `caching.storage.mode=vsam`
   fail fast with a clear error instead of silently misbehaving. The most reliable place
   is in the `StorageProvider` bean or the `CachingService` main class:

   ```java
   @Value("${caching.storage.mode:inMemory}")
   private String storageMode;

   @PostConstruct
   void checkDeprecatedStorage() {
       if ("vsam".equalsIgnoreCase(storageMode)) {
           throw new IllegalStateException(
               "VSAM storage (caching.storage.mode=vsam) was removed in this version. " +
               "Migrate to 'infinispan', 'redis', or 'inMemory'. " +
               "See the V4 migration guide for details.");
       }
   }
   ```

3. **Remove the VSAM branch from `start.sh`** (the `ZWE_configs_storage_vsam_name`
   variable block).

4. **Remove the `org.zowe.apiml.cache.storage.deprecated` message key** from
   `caching-log-messages.yml` — it was only used for the VSAM runtime warning.

5. **Update release notes and migration guide** with:
   - Removal announcement with the version number.
   - Migration path: `inMemory` for single-node; `infinispan` for HA clusters.
   - Note that any data stored in VSAM is not migrated automatically.

**Pre-requisite:** Verify no integration tests are tagged exclusively with `@Tag("vsam")`
that would need to be deleted. A search in `integration-tests/` confirms the only VSAM
references are tagged test suites — those tags and tests can be removed along with the
source.

---

## #4378 — Enabler Prefers Local Discovery Service URL

**Goal:** When multiple Discovery Service URLs are configured, the onboarding enabler should
prefer the instance on the same LPAR/host to reduce latency and avoid cross-LPAR registration
delays.

### Current Behaviour

`EurekaClientConfiguration.getEurekaServerServiceUrls()` (line 50–52) returns the
`discoveryServiceUrls` list exactly as configured, ignoring the zone key parameter `s`:

```java
@Override
public List<String> getEurekaServerServiceUrls(String s) {   // zone key 's' ignored
    return config.getDiscoveryServiceUrls();
}
```

**Important:** `ApiMediationServiceConfig` has no `hostname` or `ipAddress` field. The
service's own network address is stored as `serviceIpAddress` (line 143), which is
populated from the `baseUrl` at construction time via `ApiMediationServiceConfigReader`.
The hostname part of `baseUrl` is the correct value to compare against DS URLs.

**Relevant code:**
- `onboarding-enabler-java/.../config/EurekaClientConfiguration.java:50–52`
- `onboarding-enabler-java/.../config/ApiMediationServiceConfig.java:130,143` — `baseUrl` (String) and `serviceIpAddress` (String, resolved IP)

### Root Cause

The zone parameter `s` passed to `getEurekaServerServiceUrls(String s)` is ignored. There
is no hostname or IP comparison between the registering service's address and the DS URLs.

### Proposed Solution

**Implement hostname-based URL reordering in `EurekaClientConfiguration`.**

Extract the hostname from the service's `baseUrl` and compare it against each DS URL.
URLs whose hostname matches the service's own hostname are moved to the front:

```java
@Override
public List<String> getEurekaServerServiceUrls(String zone) {
    List<String> urls = new ArrayList<>(config.getDiscoveryServiceUrls());

    // Extract local hostname from baseUrl (e.g., "https://lpar1.example.com:7553/eureka")
    String localHostname = extractHostname(config.getBaseUrl());
    String localIp = config.getServiceIpAddress();

    if (localHostname == null && localIp == null) {
        return urls;
    }

    // Stable sort: local URLs first, original order preserved within each group
    urls.sort((a, b) -> {
        boolean aLocal = isLocalUrl(a, localHostname, localIp);
        boolean bLocal = isLocalUrl(b, localHostname, localIp);
        if (aLocal == bLocal) return 0;
        return aLocal ? -1 : 1;
    });

    return urls;
}

private String extractHostname(String url) {
    if (url == null) return null;
    try {
        return new URI(url).getHost();
    } catch (URISyntaxException e) {
        return null;
    }
}

private boolean isLocalUrl(String url, String hostname, String ipAddress) {
    try {
        String urlHost = new URI(url).getHost();
        return (hostname != null && hostname.equalsIgnoreCase(urlHost))
            || (ipAddress != null && ipAddress.equals(urlHost));
    } catch (URISyntaxException e) {
        return false;
    }
}
```

This is a stable sort — if no URL matches the local hostname/IP, the original order is
preserved and there is no behavioural change.

**Zero-risk fallback:** If `baseUrl` is null or the hostname cannot be parsed, the
original list order is returned unchanged.

**Optional — Eureka zone-based preference as a follow-up.**

After the #4429 LPAR affinity work adds zone metadata to DS instances, the
`getEurekaServerServiceUrls(String zone)` zone parameter can be used to select DS URLs
matching the service's own zone. This would leverage Eureka's built-in zone-aware failover
(`eureka.client.preferSameZoneEureka=true`) and is a more principled long-term solution.

---

## #4193 — Token Redistribution on Restart

**Goal:** When a ZAAS instance restarts, it must recover all previously invalidated tokens
from peer instances so that logout sessions are respected across the cluster even after
individual node restarts.

### Current Behaviour

`ZaasStartupListener.notifyStartup()` (lines 55–59) publishes `ZaasServiceAvailableEvent`
but does **not** call any peer instance's `/auth/distribute/{instanceId}` endpoint.

`AuthenticationService.distributeInvalidate(String toInstanceId)` (lines 391–407) exists
and correctly sends all locally cached invalidated tokens to another ZAAS instance. It is
only called when an existing running node wants to _push_ to a new peer that just appeared
in Eureka — never triggered at startup by the newly started instance itself requesting a pull.

The invalidated token cache uses Spring's `@Cacheable` annotation with `CacheManager`
(lines 97–105 in `AuthenticationService.java`), not Infinispan. This means token invalidation
state is **in-process only** and is not shared via the Infinispan cluster — redistribution
via HTTP is the designed mechanism, not automatic replication.

The `/distribute/**` and `/invalidate/**` endpoints are protected by Spring Security's
x509 filter at `NewSecurityConfiguration.java:497–498`. The `SimpleUserDetailService` at
line 498 creates a `UserDetails` for **any** valid certificate in the truststore — no
restriction to APIML-internal certificates.

**Relevant code:**
- `zaas-service/.../ZaasStartupListener.java:55–59` — `notifyStartup()` publishes event but does not request redistribution
- `zaas-service/.../security/service/AuthenticationService.java:97–105,391–407` — `@Cacheable`-backed in-memory cache; `distributeInvalidate()` pushes to peers
- `zaas-service/.../controllers/AuthController.java:100,327–338` — `/distribute/**` endpoint exists
- `zaas-service/.../security/config/NewSecurityConfiguration.java:492–499` — x509 protection with `SimpleUserDetailService` (any trusted cert accepted)

### Root Cause

1. No startup trigger exists to request redistribution from peers.
2. The endpoint security accepts any certificate in the truststore — not just APIML
   internal service certificates.

### Proposed Solution

**Step 1 — Request redistribution on startup.**

In `ZaasStartupListener.notifyStartup()`, after the startup event is published, call the
`/distribute/{myInstanceId}` endpoint on every peer ZAAS instance already in Eureka:

```java
// ZaasStartupListener.java — add to notifyStartup()
void requestTokenRedistribution() {
    String myInstanceId = eurekaClient.getApplicationInfoManager().getInfo().getInstanceId();
    Application zaas = eurekaClient.getApplication(CoreService.ZAAS.getServiceId());
    if (zaas == null) return;

    zaas.getInstances().stream()
        .filter(i -> !myInstanceId.equals(i.getInstanceId()))
        .forEach(peer -> {
            String url = EurekaUtils.getUrl(peer)
                + AuthController.CONTROLLER_PATH + "/distribute/" + myInstanceId;
            try {
                restTemplate.getForObject(url, Void.class);
                log.debug("Requested token redistribution from peer {}", peer.getInstanceId());
            } catch (Exception e) {
                log.warn("Could not request redistribution from peer {}: {}",
                    peer.getInstanceId(), e.getMessage());
            }
        });
}
```

Inject `EurekaClient` and `RestTemplate` into `ZaasStartupListener` via the constructor
(both are available as Spring beans in the ZAAS application context).

**Note on timing:** `requestTokenRedistribution()` is called from `notifyStartup()`, which
may run inside a timer task that fires when z/OSMF becomes available. At that point,
Eureka registration is complete and peers are discoverable.

**Step 2 — Restrict the distribute and invalidate endpoints to APIML-internal certificates.**

Replace the broad `SimpleUserDetailService` with a validator that checks the certificate CN:

```java
// NewSecurityConfiguration.java — inside CertificateProtectedEndpoints
.x509(x509 -> x509
    .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
    .userDetailsService(cn -> {
        Set<String> allowedCns = Set.of("gateway", "zaas", "discovery", "apicatalog", "caching");
        // Also allow a configurable set for custom deployments
        allowedCns = Sets.union(allowedCns, customInternalServiceCns);
        if (!allowedCns.contains(cn.toLowerCase())) {
            throw new UsernameNotFoundException(
                "Certificate CN '" + cn + "' is not authorised for internal APIML endpoints");
        }
        return new User(cn, "", Collections.emptyList());
    }))
```

Make the allowed CN set configurable via:
```yaml
apiml:
  security:
    internalServiceIds: gateway,zaas,discovery,apicatalog,caching
```

**Step 3 — Long-term: Replace in-memory token cache with Infinispan.**

The fundamental issue is that the invalidated-token cache is in-process (`@Cacheable` with
local `CacheManager`). Issue #4172 proposes migrating this to Infinispan, which would
replicate token invalidations automatically without any redistribution protocol. Until that
migration is done, the HTTP-based redistribution mechanism is the correct interim approach.

---

## #15 — Active Health Checks for Statically Defined API Services

**Goal:** The Discovery service should actively poll the `healthCheckUrl` of statically
defined API services and update their Eureka status, so the API Catalog and Gateway
accurately reflect actual service availability.

### Current Behaviour

Static services are registered by `StaticServicesRegistrationService.reloadServices()`
(lines 99–104) via `registry.registerStatically(instanceInfo, false, false)`.
`ApimlInstanceRegistry.registerStatically()` (line 190) uses `Integer.MAX_VALUE / 1000` as
the lease duration (line 192), making static instances permanent — they are never evicted.

`StaticServicesRegistrationService` already maintains a `CopyOnWriteArrayList<InstanceInfo>
staticInstances` (line 46), and `getStaticInstances()` (line 56) is already a public method
that returns this list. The infrastructure for tracking static instances **already exists**.

`ServiceDefinitionProcessor.java` sets the health check URL in `InstanceInfo` at lines 315
and 322 (`builder.setHealthCheckUrls(...)`), combining `instanceBaseUrl` with
`service.getHealthCheckRelativeUrl()`. This URL **is stored in `InstanceInfo`** and
accessible via `instanceInfo.getHealthCheckUrl()`.

`ApimlInstanceRegistry.statusUpdate()` (line 344) calls `super.statusUpdate()` and
publishes a `EurekaStatusUpdateEvent` — it can accept static instance IDs and will update
their status in the Eureka registry.

No code currently reads `instanceInfo.getHealthCheckUrl()` to perform polling.

**Relevant code:**
- `discovery-service/.../staticdef/StaticServicesRegistrationService.java:46,56,99–104` — `staticInstances` list and `getStaticInstances()` already exist
- `discovery-service/.../staticdef/ServiceDefinitionProcessor.java:315,322` — `healthCheckUrl` stored in `InstanceInfo`
- `discovery-service/.../ApimlInstanceRegistry.java:190–207` — `registerStatically()` sets `Integer.MAX_VALUE / 1000` lease (permanent)
- `discovery-service/.../ApimlInstanceRegistry.java:344–348` — `statusUpdate()` works for all instances including static ones

### Root Cause

1. Static instances bypass Eureka's lease-expiry mechanism via a near-infinite lease duration.
2. No code polls `healthCheckUrl` at runtime.
3. There is no status-transition mechanism to move a static instance from `UP` to `DOWN`.

### Proposed Solution

**Step 1 — Implement `StaticServiceHealthCheckService` in the discovery service.**

All the necessary infrastructure already exists. The only missing piece is a scheduled
task that polls and calls `statusUpdate()`:

```java
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "apiml.discovery.staticHealthCheck.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class StaticServiceHealthCheckService {

    @Value("${apiml.discovery.staticHealthCheck.intervalMs:30000}")
    private long intervalMs;

    @Value("${apiml.discovery.staticHealthCheck.timeoutMs:5000}")
    private long timeoutMs;

    private final StaticServicesRegistrationService registrationService;
    private final ApimlInstanceRegistry instanceRegistry;
    private final WebClient webClient;   // inject using HttpConfig.secureHttpClient (see Step 3)

    @Scheduled(fixedDelayString = "${apiml.discovery.staticHealthCheck.intervalMs:30000}")
    public void checkAll() {
        for (InstanceInfo instance : registrationService.getStaticInstances()) {
            String healthUrl = instance.getHealthCheckUrl();
            if (healthUrl == null || healthUrl.isBlank()) continue;   // no URL configured — skip

            boolean healthy = ping(healthUrl);
            InstanceInfo.InstanceStatus desired = healthy
                ? InstanceInfo.InstanceStatus.UP
                : InstanceInfo.InstanceStatus.DOWN;

            if (instance.getStatus() != desired) {
                instanceRegistry.statusUpdate(
                    instance.getAppName(),
                    instance.getInstanceId(),
                    desired,
                    null, false);   // false = not a replication, this is the authoritative update
            }
        }
    }

    private boolean ping(String url) {
        try {
            webClient.get().uri(url)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
            return true;   // 2xx response
        } catch (Exception e) {
            return false;
        }
    }
}
```

**Step 2 — Enable Spring's `@Scheduled` support.**

Add `@EnableScheduling` to the discovery service's main application class or configuration:

```java
// DiscoveryServiceApplication.java or a @Configuration class:
@EnableScheduling
```

**Step 3 — Use the APIML's secure HTTP client for health check calls.**

Inject the secure `WebClient` built from `HttpConfig` so health checks can reach
services that require TLS and will present the APIML client certificate:

```java
// In a @Configuration class in discovery-service:
@Bean
WebClient staticHealthCheckWebClient(HttpConfig httpConfig) {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(
            reactor.netty.http.client.HttpClient.from(
                httpConfig.getSecureHttpClient())))
        .build();
}
```

Inject this bean by qualifier into `StaticServiceHealthCheckService`.

**Step 4 — Handle recovery gracefully.**

When a static service returns to health after being marked `DOWN`, the `checkAll()` method
will detect `desired = UP` and call `statusUpdate(... UP ...)` automatically, restoring
normal routing. No manual intervention needed.

**Step 5 — Expose health check status in API Catalog.**

The API Catalog reads instance status from Eureka. After `statusUpdate()` sets a static
instance to `DOWN`, Eureka's `EurekaStatusUpdateEvent` fires (line 347 of
`ApimlInstanceRegistry.statusUpdate()`). The API Catalog's existing Eureka event listener
will pick this up and update the displayed service status without any additional changes to
the catalog code.

**Configuration surface (new properties):**

```yaml
apiml:
  discovery:
    staticHealthCheck:
      enabled: true         # default true; set false to disable all active polling
      intervalMs: 30000     # ms between poll cycles (default 30 seconds)
      timeoutMs: 5000       # ms per individual health request before it is treated as a failure
```

**Backward compatibility:** Services without a `healthCheckRelativeUrl` in their static
definition YAML are never polled and remain permanently `UP` — identical to current
behaviour. Only services that explicitly declare a health check URL are affected.
The feature can be entirely disabled with `enabled: false`.

---

## Summary of Proposed Changes by Component

| Component | Issues Addressed | Change Type |
|-----------|-----------------|-------------|
| `gateway-service/.../loadbalancer/` | #4429, #4405 | New `LocalityPreferenceServiceInstanceListSupplier` and `GroupFilterServiceInstanceListSupplier`; extend `DeterministicRoutingListSupplierBuilder`; wire in `CustomLoadBalancerConfiguration` |
| `caching-service/.../infinispan/config/InfinispanConfig.java` | #4420, #4400 | Fix lines 183–185 to use correct truststore fields; `computeInitialHosts()` from Eureka; auto-derive bind address and key-exchange port |
| `caching-service/src/main/resources/infinispan.xml` | #4420 | Set `require_client_authentication="true"` |
| `caching-service/.../vsam/` | #4398 | Delete all 22 VSAM files; add startup guard |
| `caching-service/.../api/CachingController.java` | #4423 | Switch from `X-Certificate-DistinguishedName` to `X-Certificate-CommonName` in non-modulith path |
| `gateway-service/.../caching/CachingServiceClientRest.java` | #4422 | Replace `GatewayClient` URL with `ReactiveDiscoveryClient` lookup; add `retryWhen` |
| `zaas-service/.../cache/CachingServiceClient.java` | #4422 | Replace gateway URL with direct Eureka lookup; add retry loop |
| `zaas-service/.../ZaasStartupListener.java` | #4193 | Add `requestTokenRedistribution()` call in `notifyStartup()` |
| `zaas-service/.../config/NewSecurityConfiguration.java` | #4193 | Replace `SimpleUserDetailService` with CN-checking service for distribute/invalidate endpoints |
| `onboarding-enabler-java/.../EurekaClientConfiguration.java` | #4378 | Sort DS URLs by hostname locality using `config.getBaseUrl()` and `config.getServiceIpAddress()` |
| Multiple `application.yml` files | #4409 | Add `apiml.service.listenAddress` and `apiml.service.lparHostname` properties; decouple `instanceId` from shared hostname |
| `apiml/src/main/java/.../ModulithConfig.java` | #4409 | Separate `registrationHostname` from `lparHostname` in `InstanceInfo` construction |
| `discovery-service/.../staticdef/` | #15 | New `StaticServiceHealthCheckService` with `@Scheduled` polling |
| `discovery-service/.../ApimlInstanceRegistry.java` | #15 | No change needed — `statusUpdate()` already supports static instances |
