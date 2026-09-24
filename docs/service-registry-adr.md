# ADR: In-sourcing the service registry and freezing the Discovery wire contract

**Status:** accepted, implemented on `feature/apiml-registry-clean-room` (PR #4950, target `v3.x.x`)
**Supersedes:** the Netflix Eureka server and Spring Cloud Netflix client used by the Discovery Service

## Context

The Discovery Service has always been Netflix Eureka: `spring-cloud-starter-netflix-eureka-server` in the server,
`spring-cloud-starter-netflix-eureka-client` in every service that registers with it, and `ApimlInstanceRegistry`
subclassing Eureka's own registry to apply API ML's deviations from stock behaviour.

Two forces made that untenable:

1. **Maintenance.** Eureka is in maintenance mode and its server does not track Spring Boot releases. The next
   Spring Boot upgrade would be blocked on a dependency nobody is updating.
2. **It is a public contract.** Discovery is a *public API*. External services outside Zowe register with it and
   query it through the five onboarding enablers (Java, Spring, Micronaut, Node.js, Python). Whatever replaces
   Eureka on the inside has to keep answering the same HTTP surface and the same JSON/XML payloads on the
   outside, because an enabler that cannot register is a customer that cannot onboard.

Two years of accumulated workarounds came with it: `ApimlInstanceRegistry` (351 lines) reaching into Eureka's
private state through reflection, `EurekaConfig` (184 lines) removing a bean definition to displace the
auto-configured registry, `RefreshablePeerEurekaNodes` (319 lines) reimplementing peer replication, and
`EurekaRestController` (406 lines) marshalling a reactive `ServerWebExchange` into a JAX-RS `UriInfo` so that
Netflix's servlet-shaped resources could be called from the modulith.

## Decision

**In-source the registry and freeze the wire protocol.**

- `apiml-registry` — the registry core: model, codec, in-memory registry, replication, interceptors. No Spring.
- `apiml-registry-client` — a passive client: register, renew, cancel, fetch, cache.
- `apiml-registry-client-spring` — the Spring integration: discovery client, lifecycle, autoconfiguration.
- The Discovery Service serves the Eureka HTTP surface from `RegistryController`/`RegistryEndpoints`.
- Eureka's *protocol* is reproduced exactly; Eureka's *code* is not depended on at runtime.

## The frozen wire contract

**Captured from:** Netflix Eureka **2.0.6** with Spring Cloud Netflix **4.3.3** — the versions
`gradle/versions.gradle` pinned before this change, and the last state in which the Eureka-based service ran.
The capture is checked in as `apiml-registry/src/test/resources/wire-contract/`, produced by running the
Eureka-based server and recording its responses (the `http-contract.json` file pins status codes and
representation rules; the `*.json`/`*.xml` and `*.mini.json` fixtures pin payload shape, field order and the
compact-vs-full form). The capture tests still drive **Eureka's own codecs** to prove our encoder matches, which
is why `eureka-core` and `eureka-client-jersey3` remain as **test-scope** dependencies of `discovery-service`.

### In the contract

Reads: `GET /eureka/apps`, `/apps/delta`, `/apps/{app}`, `/apps/{app}/{id}`, `/instances/{id}`, `/vips/{vip}`,
`/svips/{svip}`.
Writes: `POST /apps/{app}`, `PUT /apps/{app}/{id}`, `DELETE /apps/{app}/{id}`,
`PUT|DELETE /apps/{app}/{id}/status`, `PUT /apps/{app}/{id}/metadata`.
Status: `GET /status`, `GET /lastn`.
Peer replication: `POST /peerreplication/batch`.
Representation: `Accept` and `X-Eureka-Accept` select JSON (full or compact) or XML.

### Deliberately excluded

| Excluded | Reason |
|---|---|
| The Freemarker dashboard (`/eureka/`) | An HTML view rendered from a template that no external consumer or enabler has ever parsed. Replaced by a JSON status document at `/eureka/status` (the internal decision recorded in the code as **D4**), which carries the same facts an operator needs: instances, effective status, time since last renewal, and whether self-preservation has suspended eviction. |
| `/eureka/status` as HTML | Same decision as above. `/status` and `/lastn` are the only two paths deliberately outside the frozen contract, precisely so that this representation can change. Worth noting: on a locally-run Eureka-based Discovery Service this page answered **HTTP 500** (a `NullPointerException` out of the Freemarker view), so nothing that worked before stops working. |
| `GET /eureka/peerreplication` | The read side of replication. No client, enabler or test consults it; peer state is driven entirely through `POST /peerreplication/batch`, which **is** in the contract. Implementing a read endpoint with no consumers would freeze a shape nobody asked for. |

### Why external consumers are unaffected

The surface, the status codes, the content types, the JSON/XML selection headers and the payload field order are
all unchanged and pinned by the contract fixtures. A registrant cannot tell the difference: it registers with the
same request against the same path and receives the same `204` with no body; it renews and receives the same
`200`, or the same `404` that tells it to re-register. The enablers are, for this release, unchanged - they keep
using Netflix's client, which is exactly why the protocol had to be reproduced rather than improved.

## Policy for `eureka.*` configuration property names

**Kept unchanged.** `RegistryConfiguration` reads `eureka.server.*` and `eureka.instance.*` under their original
names, and the new client reads `eureka.client.*`. This is a deliberate compatibility choice, not an oversight:

- The cutover is hard — there is no runtime toggle — so an existing deployment has to keep behaving as it does
  today without anyone editing `zowe.yaml`.
- `config/docker/*.yml`, the `*-package/` `start.sh` scripts and every customer's `zowe.yaml` carry `eureka.*`
  blocks. Renaming them would turn a code change into a configuration migration for no functional gain.
- Renaming to `apiml.registry.*` is a **separate, documented migration**, not part of this change. When it
  happens both names must be read for at least one release, with `eureka.*` deprecated and warned about.

The consequence to be aware of: a property named `eureka.*` that no longer has any effect because the component
it configured is gone. The audit below names each one.

## Modules that keep the Netflix client, and why

`grep -rn "com.netflix" --include='*.java' */src/main` does not return only comments. Every remaining user is
named here.

| Module | Netflix usage | Why it stays |
|---|---|---|
| `onboarding-enabler-java` | Real: `EurekaClient`, `InstanceInfo`, `ApplicationInfoManager`, transport factories | The enablers are the client surface. They speak Eureka's protocol and are the reason the protocol is frozen. Migrating them is Phase 5 and is a coordinated change with external consumers, not a side effect of this one. |
| `onboarding-enabler-spring` | Real (via the Java enabler), plus `implementation libs.eureka.jersey.client` | Same as above; it wraps the Java enabler. |
| `caching-service` | Real: `CachingHealthIndicator` imports `com.netflix.discovery.shared.Application` and reads `apiMediationClient.getEurekaClient()`; declares `implementation libs.eureka.jersey.client` | The Caching Service onboards through `onboarding-enabler-spring`, so it *is* a Netflix client until Phase 5. Its health indicator asks that client whether the Gateway is present. Removing the dependency means moving the service off the enabler, which is Phase 5. |
| `discoverable-client` | Real: `ApiMediationClientService` builds its own `ApiMediationClientImpl`, and `ClientEurekaRegistryVersionEndpoint` reads the version out of `apiMediationClient.getEurekaClient().getApplications().getAppsHashCode()` | It is the integration tests' registration probe — its whole purpose is to register and un-register through the Java enabler without disturbing the other services. Its version endpoint is one of the Phase 5 items (see below). |
| `apiml-utility` | A string literal, not a dependency: `ServiceStartupEventHandler` names `com.netflix.discovery.DiscoveryClient` and `...RedirectingEurekaHttpClient` to lower their log level after startup | **Named exception to the grep criterion.** Those logger names exist only while a Netflix client is running, and the suppression is still live for `caching-service` and `discoverable-client`, which use this shared handler. Where Eureka is gone it is a no-op. It is deliberately not deleted, because deleting it from a shared class would re-enable the noisy startup logging in the two components that still have the client. |
| `discovery-service` | Test scope only: `libs.eureka.core`, `libs.eureka.jersey.client`, `libs.netflix.servo` | Deliberate. The wire-contract capture tests drive Eureka's own codecs to prove our encoder still matches it. Not on the runtime classpath — that is the point of this work. |
| `common-service-core`, `apiml-registry-client-spring` | Test scope only: `libs.spring.cloud.starter.eureka.client` | Used as a *reference implementation* in tests, not as the runtime client. |

Dependency and alias clean-up done as part of this change: `spring_cloud_starter_eureka_server` was removed from
`gradle/versions.gradle` — nothing referenced it any more. The aliases that remain
(`eureka_core`, `eureka_jersey_client`, `netflix_servo`, `spring_cloud_starter_eureka_client`) all still have
live test-scope or enabler users, listed above.

`gradle/coverage.gradle` and `gradle/license.gradle` include the three new modules so their code is measured and
licensed like everything else.

## Version semantics: knowingly half-migrated

`/application/eurekaversion` still reports Eureka's `appsHashCode` **UP-count**, not the registry's own monotonic
`Applications.version()`. This is deliberate and temporary.

The integration startup check compares the value *between* APIML instances to decide whether their registries
have converged, and the Caching Service, the discoverable-client and `apiml-common`'s
`EurekaRegistryVersionEndpoint` all still derive it from a Eureka client's hash code. Switching the Discovery
Service to a monotonic counter while they report an instance count would make the comparison meaningless — and
the failure mode is jobs hanging for three minutes and then failing with no explanation, not a test failure
anybody can read.

The semantics are pinned by tests on **both** sides: `RegistryVersionEndpointTest` in `discovery-service` and
`RegistryClientVersionEndpointTest` in `apiml-registry-client-spring`. **Phase 5 is where both sides switch in
one commit.**

## Phase plan

The commit messages on this branch refer to phases; this is the canonical list.

| Phase | Content | State |
|---|---|---|
| 0 | Capture the Eureka wire contract from the running service; decide the compatibility surface | done |
| 1 | `apiml-registry`: model, codec, in-memory registry, policy | done |
| 2 | `apiml-registry-client` + `-spring`: register, renew, fetch, cache | done |
| 3 | Cut the Discovery Service over; cut the Gateway, ZAAS and API Catalog off the Netflix client | done |
| 4 | Modulith cutover, packaging-facing configuration, documentation | this change |
| 5 | Move the enablers and the remaining clients (`caching-service`, `discoverable-client`) onto the registry client; switch `/application/eurekaversion` to the real version; rename `eureka.*` properties | **not started** |

**Note on the numbering:** some javadoc on this branch refers to the same client-migration work as "Phase 7"
(for example in `RegistryInstanceProperties`, on introducing an `apiml.registry.instance.*` alias). The table
above is the numbering the commit messages and the PR body use; the javadoc is out of step and should be
reconciled with it. It is a documentation inconsistency, not a disagreement about the plan.

## Consequences

- The registry is now ours to simplify: no bean-definition surgery, no reflection into private state, no
  static `EurekaServerContextHolder`.
- `Applications.version()` exists and is monotonic, which the Eureka-based implementation never had.
- Peer replication is a batch POST and a `PeerReplicator`, not the 17-class acceptor/executor/traffic-shaper trio.
- The enabler migration is now the long pole, and it is a multi-release, externally coordinated piece of work.
- **Any behaviour that depended on Eureka's client publishing Spring Cloud's `HeartbeatEvent` had to be found and
  re-established by hand.** Two components listened for it: `GatewayInstanceInitializer` (re-resolves the
  Gateway's address) and `RouteRefreshListener` (rebuilds the modulith's routes). The registry client publishes
  it on every refresh, and the Discovery Service's own `SpringRegistryEventBridge` publishes it whenever its
  registry content changes. Missing this is not a compile error and not a unit-test failure — it surfaces as
  `/application/**` answering `401` with a *bad credentials* body on the Discovery Service, because the Gateway
  login can never be attempted and Spring Security falls through to the next authentication provider.
