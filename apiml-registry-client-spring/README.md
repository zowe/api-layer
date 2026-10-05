# apiml-registry-client-spring

Spring integration for `apiml-registry-client`: the autoconfiguration, the lifecycle that drives registration and
renewal, Spring Cloud `DiscoveryClient` implementations, the registration properties, and the
`/application/eurekaversion` endpoint that the integration startup check reads.

This is what the Gateway, ZAAS and the API Catalog use instead of `spring-cloud-starter-netflix-eureka-client`.
The Discovery Service and the modulith do not: they *are* the registry, and use `apiml-registry` directly.

## Responsibility

| Type | Content |
|---|---|
| `RegistryClientAutoConfiguration` | The beans, and the conditions under which they exist |
| `RegistryClientLifecycle` | The one thread: registers, renews, refreshes, unregisters on shutdown |
| `RegistryDiscoveryClient`, `CachedRegistryReactiveDiscoveryClient` | Spring Cloud's blocking and reactive `DiscoveryClient` views |
| `RegistryFetchProperties` | `eureka.client.*` |
| `RegistryInstanceProperties`, `RegistryInstanceDefaults` | `eureka.instance.*`, and the defaults Netflix filled in invisibly |
| `RegistryClientVersionEndpoint` | `/application/eurekaversion`, backed by this service's registry view |
| `HealthStatusSource` | Maps the application's aggregate health onto the registered status |
| `RegistryCacheRefreshedEvent` | This module's own "the view changed" event |

`RegistryClientLifecycle` also publishes Spring Cloud's **`HeartbeatEvent`** on every refresh. That is not
decoration: `GatewayInstanceInitializer` re-resolves the Gateway's address on it and `RouteRefreshListener`
rebuilds the modulith's routes, both of which used to be driven by Eureka's client. Dropping the event makes the
Gateway unresolvable — which on the Discovery Service shows up as `/application/**` answering `401` with a *bad
credentials* body — and compiles, and passes every unit test.

## Wire-contract guarantees

The client does not define the format; `apiml-registry`'s `RegistryCodec` does, so the client and server cannot
drift. What this module guarantees is the *behaviour* around it:

- Registration is attempted at the end of startup (last phase) and withdrawn at the first phase of shutdown, so
  a service is only advertised once it can serve, and is withdrawn before its dependencies are torn down.
- The lease is renewed at `eureka.instance.leaseRenewalIntervalInSeconds` — the interval the registry is told to
  expect — **not** at `instanceInfoReplicationIntervalSeconds`, which is how often the client checks whether its
  own instance info changed. Using the latter leaves an instance registered, evicted, and unseen for a full
  replication cycle.
- A 404 on renewal means "re-register", and is retried on the next heartbeat rather than treated as fatal.
- The registered status follows the application's aggregate health, and is only pushed when it *changes*: an
  unconditional status update on every heartbeat would make each renewal a write the peers have to replicate.
- `/application/eurekaversion` reports the Eureka-compatible UP-count, not a version — see the ADR. It backs off
  automatically for the three components that serve this endpoint from their own registry (the Discovery
  Service, the Caching Service, the discoverable client); two endpoints with the same id stop the application
  from starting, and the integration tests' `liteLibJarAll` classpath puts this autoconfiguration in front of
  those services whether they declare the module or not.

## Configuration

The `eureka.*` prefixes are kept deliberately, so an existing `zowe.yaml` needs no edit. Renaming them is a
separate, documented migration — see the ADR's policy section.

`eureka.client.*` (`RegistryFetchProperties`), defaults in brackets:

| Property | Meaning |
|---|---|
| `enabled` [`true`] | Turns the whole client off: no registration, no fetching, no background work |
| `registerWithEureka` [`true`] | Whether this service registers itself |
| `fetchRegistry` [`true`] | Whether it reads the registry |
| `registryFetchIntervalSeconds` [`30`] | Refresh cadence |
| `instanceInfoReplicationIntervalSeconds` [`30`] | How often the client re-checks whether its own instance info changed |
| `initialInstanceInfoReplicationIntervalSeconds` [`40`] | First replication delay |
| `eurekaServerConnectTimeoutSeconds` [`5`], `eurekaServerReadTimeoutSeconds` [`8`] | Transport timeouts |
| `shouldUnregisterOnShutdown` [`true`] | Cancel the registration on shutdown — synchronously, so peers learn immediately instead of waiting out a lease |
| `serviceUrl.defaultZone` | **Required** when the client registers or fetches; the service fails to start without it, by design |
| `healthcheck.enabled` | Feeds the application's aggregate health into the registered status |

`eureka.instance.*` (`RegistryInstanceProperties`) carries the registration itself: `appname` (defaulted from
`spring.application.name`), `hostname` and `ipAddress` (resolved from the network interfaces when unset),
`vipAddress`/`secureVipAddress`, `leaseRenewalIntervalInSeconds`, `leaseExpirationDurationInSeconds`,
`securePortEnabled`, status/home/health URLs and `metadata`.

Two properties are deliberately **not** read, because Spring Cloud never bound them either:
`eureka.instance.port` (only `non-secure-port` exists — the API Catalog's `application.yml` sets the former to
this day and it has always been ignored) and `eureka.instance.dataCenterInfo`.

## Tests

`./gradlew :apiml-registry-client-spring:test`. `RegistryDiscoveryClientAutoConfigurationTest` covers the
endpoint back-off; `RegistryInstancePropertiesContractTest` pins the property names and defaults that existing
configuration depends on.
