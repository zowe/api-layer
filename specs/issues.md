# API Mediation Layer - Open Issues

> Source: https://github.com/zowe/api-layer/issues  
> Last updated: 2026-03-14  
> Total open issues: ~64 (excludes PRs)

---

## Quick Stats

| Category | Count |
|----------|-------|
| Bugs - Priority Critical | 1 |
| Bugs - Priority High | 11 |
| Bugs - Priority Medium | 3 |
| Bugs - Priority Low | 2 |
| Bugs - New/Unworked | 3 |
| Enhancements - Priority High | 26 |
| Enhancements - Priority Medium | 13 |
| Technical Excellence | 9 |
| Dependencies | 1 |

---

## Bugs - Priority Critical

| # | Title | Size | Assignees | Created |
|---|-------|------|-----------|---------|
| [#4424](https://github.com/zowe/api-layer/issues/4424) | Performance of PAT validation depends on amount of stored records | size/M | — | 2025-12-12 |

**#4424** – The validation of PAT is based on multiple queries that load all revocation information. Performance degrades as storage grows. The code loads the whole bucket of data from the caching service — the problem is the request grows heavier with stored records over time.

---

## Bugs - Priority High

| # | Title | Size | Assignees | Created |
|---|-------|------|-----------|---------|
| [#4478](https://github.com/zowe/api-layer/issues/4478) | IZUG846W: An HTTP request for a z/OSMF REST service was received from a remote site | size/S | achmelo | 2026-01-29 |
| [#4451](https://github.com/zowe/api-layer/issues/4451) | Salt in caching service is represented as String even it contains binary data | size/M | — | 2026-01-09 |
| [#4444](https://github.com/zowe/api-layer/issues/4444) | /gateway/api/v1/auth/ticket does not accept OIDC token | size/S | — | 2026-01-02 |
| [#4426](https://github.com/zowe/api-layer/issues/4426) | Exception handling in Caching Service | size/M | — | 2025-12-12 |
| [#4420](https://github.com/zowe/api-layer/issues/4420) | Caching service is not handling trust store well (Infinispan mode) | size/S | — | 2025-12-12 |
| [#4362](https://github.com/zowe/api-layer/issues/4362) | Weird configuration of idle connection | — | — | 2025-10-27 |
| [#3461](https://github.com/zowe/api-layer/issues/3461) | JSON schemas miss some configuration parameters | size/M | — | 2024-03-12 |
| [#3097](https://github.com/zowe/api-layer/issues/3097) | Login endpoint provides too much information when authentication fails with SAF provider | size/S | — | 2023-09-21 |
| [#3007](https://github.com/zowe/api-layer/issues/3007) | Fix 401 responses | size/XL | — | 2023-07-31 |
| [#2991](https://github.com/zowe/api-layer/issues/2991) | API ML BOM | size/M | — | 2023-07-19 |
| [#2902](https://github.com/zowe/api-layer/issues/2902) | Invalid log message and response on missing credentials | size/M | — | 2023-05-04 |

**#4478** – z/OSMF API Jobs returns 403 IZUG846W when listing jobs via Zowe v3.3. HTTP request for z/OSMF REST service received from a remote site.

**#4451** – The Caching service generates a 16-byte random salt to create a hash for storing data. The salt is stored as String, which is incorrect for binary data and can cause issues.

**#4444** – OIDC tokens configured in Zowe v3.3 work for `/auth/oidc-token/validate` but the `/auth/ticket` endpoint does not accept them.

**#4426** – Almost all exceptions in the Caching service use `StorageException` regardless of meaning. This class should be split to represent state vs. wrapped exceptions. Proper HTTP error codes are not returned.

**#4420** – JGroups in the Caching service does not handle the trustStore well. Configuration issue in `infinispan.xml` for the key exchange endpoint.

**#4362** – Weird configuration of idle connection in the gateway.

**#3461** – `caching-schema.json` is missing many configuration parameters. All configuration parameters (at least those in `start.sh`) need to be described in JSON schemas.

**#3097** – Login endpoint returns overly specific error message when authentication fails with SAF provider, revealing whether the user exists or not.

**#3007** – The APIML returns 401 with a description in `X-Zowe-Auth-Failure` header. This is a potential security risk as it helps attackers understand the configuration.

**#2991** – API ML build produces a BOM that does not contain dependencies.

**#2902** – Code always writes a message about status 400 even when a different code is returned. Multiple messages for 401 could help an attacker determine login flow.

---

## Bugs - Priority Medium

| # | Title | Size | Assignees | Created |
|---|-------|------|-----------|---------|
| [#4418](https://github.com/zowe/api-layer/issues/4418) | Catalog tile is required for registration but never used | size/S | — | 2025-12-12 |
| [#3226](https://github.com/zowe/api-layer/issues/3226) | Fix response message when user revoked using x509 auth | size/S | taban03 | 2023-12-01 |
| [#3131](https://github.com/zowe/api-layer/issues/3131) | The Gateway says that it is shutting down but nothing happened | size/M | — | 2023-10-10 |

**#4418** – API Catalog removed usage of tiles in v3, but service registration still requires tile information. Onboarding wizard has a whole section for tiles, making the process confusing.

**#3226** – When using client cert authentication with a revoked user, identity mapping from SAF succeeds, passticket generates successfully, but z/OSMF is unable to authenticate the user. Unclear response message.

**#3131** – After removing `System.exit` calls in PR #3068, the Gateway writes a shutdown message but doesn't actually stop. Confusing behavior for operators.

---

## Bugs - Priority Low

| # | Title | Size | Assignees | Created |
|---|-------|------|-----------|---------|
| [#4422](https://github.com/zowe/api-layer/issues/4422) | Calling Caching service from Gateway using micro-services | size/M | — | 2025-12-12 |
| [#4421](https://github.com/zowe/api-layer/issues/4421) | Loading/initializing of salt is not an atomic operation | size/M | — | 2025-12-12 |

**#4422** – The Gateway calls the Caching service to store revoked tokens via Gateway itself, routing to a Caching service instance. This doesn't respect Zowe instance grouping and can lead to wrong instance selection.

**#4421** – Two potential race conditions: if clusters are out of sync, multiple salts could be generated; if two or more threads initialize salt simultaneously, one must fail. Should be solved with atomic/synchronized operations.

---

## Bugs - New (Unworked)

> These bugs have the `new` label indicating they have not yet been worked on.

| # | Title | Size | Assignees | Created |
|---|-------|------|-----------|---------|
| [#4512](https://github.com/zowe/api-layer/issues/4512) | Detection of failed services during routing | — | — | 2026-03-12 |
| [#4511](https://github.com/zowe/api-layer/issues/4511) | Sticky session and HA | — | — | 2026-03-12 |
| [#4510](https://github.com/zowe/api-layer/issues/4510) | Unnecessary call of caching service during routing | — | — | 2026-03-12 |

**#4512** – The `NettyRoutingFilterApiml` condition for detecting failed services is too general and could match other communication issues. The `GatewayExceptionHandler` ignores the error cause. Goals: better debug messages; return 503 only when service is truly unreachable (not for cert errors like PKIX).

**#4511** – `DeterministicLoadBalancer` always selects the same instance. When an instance goes down but is still in Eureka, all requests fail until deregistration. The correct behavior should try the sticky instance first, then fall back to any available instance.

**#4510** – `DeterministicLoadBalancer` always calls the Caching service when a request contains a token (user is detectable), even when the cached value is not needed. This causes unnecessary I/O and failures when Caching service is down.

---

## Enhancements - Priority High

| # | Title | Size | Milestone | Assignees | Created |
|---|-------|------|-----------|-----------|---------|
| [#4489](https://github.com/zowe/api-layer/issues/4489) | Cherry-pick startup changes and fixes from v3.x.x | size/XS | — | — | 2026-02-20 |
| [#4431](https://github.com/zowe/api-layer/issues/4431) | Alternative authentication for onboarding | size/M | — | — | 2025-12-15 |
| [#4429](https://github.com/zowe/api-layer/issues/4429) | LPAR affinity | size/M | — | — | 2025-12-15 |
| [#4411](https://github.com/zowe/api-layer/issues/4411) | Sample JCL to enable passtickets for zOSMF | size/S | — | — | 2025-12-02 |
| [#4409](https://github.com/zowe/api-layer/issues/4409) | Listen on single IP address | size/M | 3.6 | — | 2025-12-01 |
| [#4405](https://github.com/zowe/api-layer/issues/4405) | Group instances | size/M | 3.6 | — | 2025-11-27 |
| [#4365](https://github.com/zowe/api-layer/issues/4365) | Service-Initiated Pass-ticket Generation for AI Agent Scenarios | — | — | — | 2025-10-28 |
| [#4363](https://github.com/zowe/api-layer/issues/4363) | Support ZAAS client with AT-TLS | size/S | — | — | 2025-10-27 |
| [#3865](https://github.com/zowe/api-layer/issues/3865) | Allow javaArgs for all Zowe Java processes | size/M | — | — | 2024-10-03 |
| [#3747](https://github.com/zowe/api-layer/issues/3747) | Starting API ML on Already Used Port Should Give a Specific Message | size/S | — | — | 2024-09-06 |
| [#3652](https://github.com/zowe/api-layer/issues/3652) | Default route path replacement is different in v3 and v2 | size/M | — | — | 2024-07-17 |
| [#3589](https://github.com/zowe/api-layer/issues/3589) | Support multiple OIDC PKIs in the Gateway | size/S | — | — | 2024-06-05 |
| [#3575](https://github.com/zowe/api-layer/issues/3575) | Better way of setting default java properties | size/M | — | — | 2024-05-30 |
| [#3528](https://github.com/zowe/api-layer/issues/3528) | Update or remove jgroups cluster identification message | size/S | — | — | 2024-05-02 |
| [#3327](https://github.com/zowe/api-layer/issues/3327) | caching-service config dependency when unused | size/M | — | — | 2024-02-27 |
| [#3243](https://github.com/zowe/api-layer/issues/3243) | What information we return on unsuccessful attempt to login | size/L | 4.0 | — | 2023-12-18 |
| [#3188](https://github.com/zowe/api-layer/issues/3188) | Authorization capabilities of the API Mediation Layer | size/L | 3.6 | balhar-jakub, JirkaAichler | 2023-11-08 |
| [#3129](https://github.com/zowe/api-layer/issues/3129) | Multiple services/instances of one product within infrastructure | size/XL | — | balhar-jakub | 2023-10-09 |
| [#3085](https://github.com/zowe/api-layer/issues/3085) | ZAAS provides meaningful message when onboarding failed because of expired certificate | size/L | — | — | 2023-09-18 |
| [#3037](https://github.com/zowe/api-layer/issues/3037) | Support upgrade Zowe in HA by loadbalancers | size/M | — | — | 2023-08-16 |
| [#3007](https://github.com/zowe/api-layer/issues/3007) | Fix 401 responses | size/XL | — | — | 2023-07-31 |
| [#2976](https://github.com/zowe/api-layer/issues/2976) | Certificate and password management for System Administrators | size/XL | — | balhar-jakub | 2023-07-10 |
| [#2865](https://github.com/zowe/api-layer/issues/2865) | Support for Open Telemetry | size/L | 3.5 | balhar-jakub | 2023-04-12 |
| [#2830](https://github.com/zowe/api-layer/issues/2830) | JDK serialization filtering | size/S | — | — | 2023-03-13 |
| [#2557](https://github.com/zowe/api-layer/issues/2557) | Improve onboarding of internal APIs | size/L | — | — | 2022-08-19 |
| [#2315](https://github.com/zowe/api-layer/issues/2315) | Easier debugging of swagger doc rendering | size/M | — | — | 2022-04-22 |
| [#2296](https://github.com/zowe/api-layer/issues/2296) | Use zowe.yaml as the source for the configuration | size/M | — | — | 2022-04-13 |

**#4489** – Startup check improvements and fixes were made on v3 branch (PR #4463). These should be carried over to v2 as well.

**#4431** – API ML requires x509 authentication for service onboarding, adding complexity. Provide an alternative authentication method for service registration.

**#4429** – Gateway currently load-balances across all registered instances. Customers want to prioritize local (same LPAR) instances in the load balancer for performance/locality.

**#4411** – Default authentication provider changed to SAF. For z/OSMF REST APIs to continue working, API ML generates a static definition with `httpBasicPassticket`. A sample JCL for enabling passtickets for z/OSMF is needed.

**#4409** – Allow REST APIs to listen on specific IP addresses rather than all interfaces. Enables multiple API ML instances to share the same host+port for hot-standby HA.

**#4405** – Allow service instances to be grouped via registration metadata. Enables instance grouping in HA for specific purposes (e.g., tenant isolation).

**#4365** – AI Agent scenario requires a service to programmatically request a passticket on behalf of a specified user to facilitate subsequent service access.

**#4363** – When AT-TLS is used between a service and the API Gateway, client certificate signing cannot be controlled. Need support for the ZAAS client in AT-TLS mode.

**#3865** – Currently only the Message Service JVM has a `javaArgs` yaml field. All Java processes should support this for tools like OMEGAMON for JVM.

**#3747** – When API ML starts on an already-used port, the error messages (`Unable to start embedded Tomcat`, `EDC5111I Permission denied`) do not clearly indicate the port is in use. Detect and issue a specific message.

**#3652** – In Spring Cloud Gateway (v3), trailing slash behavior in routing changed from v2. Need consistent behavior or clear documentation.

**#3589** – `oidc.jwks.uri` currently supports only one URL. Convert to a list to support multiple OIDC providers/PKIs.

**#3575** – Some libraries depend on Java properties like `javax.net.ssl.keyStore` set programmatically at startup. This can produce unexpected behavior. Need a better configuration approach.

**#3528** – The jgroups cluster node ID is read from system calls. If the hostname doesn't match the configured one in `/etc/hosts`, it causes confusion even when the feature works. The message should be updated or removed.

**#3327** – `zwe config install` fails with VSAM configuration errors even when `components.caching-service.enabled: false`. Remove the configuration dependency when caching service is disabled.

**#3243** – The squad has inconsistent understanding of what information is safe to return on failed login attempts. Needs alignment and implementation.

**#3188** – As a system administrator, allow limiting access to specific services using SAF tools. Full RBAC/authorization capabilities via SAF.

**#3129** – Service providers with multiple customers running different service versions cannot get a meaningful view of APIs in the Catalog. Need support for multi-tenant/multi-instance product grouping.

**#3085** – When a service's certificate expires, the error from the gateway is `EDC51I Broken pipe`, which provides no actionable guidance. The message should clearly say the certificate has expired.

**#3037** – Improve API Gateway and Spring Cloud Gateway to support zero-downtime Zowe upgrades in HA mode via load balancers.

**#3007** – Security risk: 401 responses include the `X-Zowe-Auth-Failure` header with debug information that helps attackers understand the gateway configuration.

**#2976** – Provide a secure vault-like service for storing client certificates and passwords needed for accessing other services, similar to HashiCorp Vault.

**#2865** – All requests through API ML should produce OpenTelemetry-compatible telemetry data (spans, metrics, logs).

**#2830** – Enable JDK serialization filtering (JEP 290, Java 9+) via global configuration to mitigate deserialization vulnerabilities.

**#2557** – Internal APIs should be visible in the API Catalog and benefit from SSO and visualization features.

**#2315** – Improve server-side logging and UI error information to make swagger rendering failures easier to debug, without requiring full debug mode.

**#2296** – Standardize all API ML component configuration to derive from `zowe.yaml`, removing reliance on additional environment variables.

---

## Enhancements - Priority Medium

| # | Title | Size | Milestone | Assignees | Created |
|---|-------|------|-----------|-----------|---------|
| [#4490](https://github.com/zowe/api-layer/issues/4490) | Flakiness when creating a cluster on Infinispan | size/L | — | — | 2026-02-20 |
| [#4484](https://github.com/zowe/api-layer/issues/4484) | Investigate obtaining of Address Space ID for OpenTelemetry | size/M | — | — | 2026-02-09 |
| [#4473](https://github.com/zowe/api-layer/issues/4473) | Stored certificates and keys in the repository | size/S | — | — | 2026-01-27 |
| [#4445](https://github.com/zowe/api-layer/issues/4445) | PKCS#12 support in Python Enabler | size/S | — | — | 2026-01-06 |
| [#4427](https://github.com/zowe/api-layer/issues/4427) | Caching service health endpoint | size/L | — | — | 2025-12-12 |
| [#4425](https://github.com/zowe/api-layer/issues/4425) | Binding service on a specific subnet | size/S | — | — | 2025-12-12 |
| [#4423](https://github.com/zowe/api-layer/issues/4423) | Storage in Caching service by caller | size/L | — | — | 2025-12-12 |
| [#4406](https://github.com/zowe/api-layer/issues/4406) | Reload registration metadata during runtime | size/S | — | — | 2025-11-27 |
| [#4400](https://github.com/zowe/api-layer/issues/4400) | Infinispan HA configuration | — | 3.6 | — | 2025-11-25 |
| [#4378](https://github.com/zowe/api-layer/issues/4378) | Enabler can choose the preferred DS URL | size/S | 3.6 | — | 2025-11-05 |
| [#3299](https://github.com/zowe/api-layer/issues/3299) | Fix homepage URL at API Catalog | size/M | — | — | 2024-01-30 |
| [#3121](https://github.com/zowe/api-layer/issues/3121) | Make API Catalog tiles unique | size/M | — | balhar-jakub | 2023-10-03 |
| [#1977](https://github.com/zowe/api-layer/issues/1977) | RBAC (or other way of segmentation) of access to APIML services | size/M | — | — | 2022-01-14 |

**#4490** – The `LazyCacheManager` retries to start `DefaultCacheManager` on failure. The first and second retry attempts always fail with errors before succeeding. The root cause needs investigation and fixing.

**#4484** – Investigate whether the Address Space ID (ASID) available via `ps -Afo xasid,args` can be obtained from Java (via Jzos or z/OS SDK) to enrich OpenTelemetry data.

**#4473** – The `/keystore` directory in the repo contains keys and certificates. Even for testing only, consider: not storing keys in plain text; not providing passwords in the codebase; using alternative approaches for test certificates.

**#4445** – Enhance the Python Enabler to read keystores in `.p12`/`.pfx` format, similar to what was done for the Node.js Enabler in PR #4430. Must remain backward compatible with old configuration format.

**#4427** – The health endpoint should expose new checks: whether jgroups is listening, list of connected cluster nodes, and node count. Decide whether JGroup state should influence overall service health status.

**#4425** – Allow each service to configure which network interface it binds to. Currently services bind to `0.0.0.0` (all interfaces). This applies to v3; v2 behavior may differ.

**#4423** – The current implementation of PAT revocation storage via HTTP between GW and Caching service uses DN-based routing, which is fragile. Consider a direct storage approach by the calling service.

**#4406** – Updating Eureka metadata currently requires a service restart. Allow reading configuration files on demand during runtime so the Eureka client can update registered metadata without restarts.

**#4400** – Infinispan cluster configuration for HA requires too many settings, increasing installation complexity. Reduce to minimal required configuration parameters.

**#4378** – The onboarding enabler always uses the first URL in the Discovery Service list. Add preference logic so the enabler can prefer local instances of Discovery Service.

**#3299** – API Catalog displays the service's homepage URL as provided by the service. In HA with DVIPA, this URL may be wrong. The Catalog should resolve or update the homepage URL based on actual gateway address.

**#3121** – Multiple services with the same display name but different service IDs are indistinguishable in API Catalog. Show service ID or other disambiguating information in the tile.

**#1977** – Provide RBAC or another segmentation mechanism to limit which users can access which APIML services. SAF-based segmentation preferred.

---

## Technical Excellence

| # | Title | Size | Milestone | Assignees | Created |
|---|-------|------|-----------|-----------|---------|
| [#4447](https://github.com/zowe/api-layer/issues/4447) | Improve integration tests performance and fix the remote Gradle cache | size/M | — | — | 2026-01-06 |
| [#4398](https://github.com/zowe/api-layer/issues/4398) | Remove support for VSAM | — | 4.0 | — | 2025-11-25 |
| [#3484](https://github.com/zowe/api-layer/issues/3484) | Migrate to Material UI v5 | — | — | — | 2024-03-25 |
| [#3223](https://github.com/zowe/api-layer/issues/3223) | Speed up and make the pipeline more stable | size/L | — | balhar-jakub | 2023-11-29 |
| [#3049](https://github.com/zowe/api-layer/issues/3049) | Separate npm build of API catalog from gradle | size/M | — | achmelo | 2023-08-28 |
| [#3041](https://github.com/zowe/api-layer/issues/3041) | Expiration of passtickets | — | — | — | 2023-08-21 |
| [#2942](https://github.com/zowe/api-layer/issues/2942) | Improve Spring configuration | size/M | — | — | 2023-06-02 |
| [#2941](https://github.com/zowe/api-layer/issues/2941) | Reduce using `System.exit` | size/S | — | — | 2023-06-01 |
| [#2144](https://github.com/zowe/api-layer/issues/2144) | Migrate UI unit tests from Enzyme to RTL | size/L | — | — | 2022-03-02 |

**#4447** – CI forces full rebuilds even for targeted test subsets. Remote Gradle build cache is unreliable. Fix cache access and optimize test execution to reduce CI time.

**#4398** – VSAM is deprecated in V3. Prepare for full removal in V4. Milestone: 4.0.

**#3484** – Material UI v4 is unmaintained since September 2021. Migrate to MUI v5.

**#3223** – Reduce build time in GitHub Actions. Make the pipeline more stable to reduce the need for manual reruns of failed workflows.

**#3049** – Taking npm projects out of the Gradle build and publishing them separately would significantly reduce overall build time.

**#3041** – Passtickets have a new value each second. The source code has expiration data that removes caching. Correct implementation should reduce parallel actions for generating the same passticket.

**#2942** – Replace deprecated Spring configurations: `spring.config.useLegacyProcessing`, `spring.mvc.favicon.enabled`, etc.

**#2941** – About 10 `System.exit` calls exist in the codebase. Replace with `SpringApplication.exit` at minimum.

**#2144** – Enzyme has no official adapter for React 17+. Migrate all UI unit tests to React Testing Library (RTL).

---

## Dependencies

| # | Title | Assignees | Created |
|---|-------|-----------|---------|
| [#3709](https://github.com/zowe/api-layer/issues/3709) | Dependency Dashboard | zowe-robot | 2024-08-28 |

**#3709** – Renovate bot dependency dashboard. Tracks all pending dependency updates and repository issues detected by Renovate.

---

## Good First Issues

> Issues labeled `good first issue` suitable for new contributors.

| # | Title | Size | Created |
|---|-------|------|---------|
| [#3299](https://github.com/zowe/api-layer/issues/3299) | Fix homepage URL at API Catalog | size/M | 2024-01-30 |

---

## Help Wanted

> Issues labeled `help-wanted` open for community contribution.

| # | Title | Size | Created |
|---|-------|------|---------|
| [#3865](https://github.com/zowe/api-layer/issues/3865) | Allow javaArgs for all Zowe Java processes | size/M | 2024-10-03 |
| [#3747](https://github.com/zowe/api-layer/issues/3747) | Starting API ML on Already Used Port Should Give a Specific Message | size/S | 2024-09-06 |
| [#3652](https://github.com/zowe/api-layer/issues/3652) | Default route path replacement is different in v3 and v2 | size/M | 2024-07-17 |
| [#3589](https://github.com/zowe/api-layer/issues/3589) | Support multiple OIDC PKIs in the Gateway | size/S | 2024-06-05 |
| [#3575](https://github.com/zowe/api-layer/issues/3575) | Better way of setting default java properties | size/M | 2024-05-30 |
| [#3528](https://github.com/zowe/api-layer/issues/3528) | Update or remove jgroups cluster identification message | size/S | 2024-05-02 |
| [#3327](https://github.com/zowe/api-layer/issues/3327) | caching-service config dependency when unused | size/M | 2024-02-27 |
| [#3129](https://github.com/zowe/api-layer/issues/3129) | Multiple services/instances of one product within infrastructure | size/XL | 2023-10-09 |
| [#3085](https://github.com/zowe/api-layer/issues/3085) | ZAAS provides meaningful message when onboarding failed because of expired certificate | size/L | 2023-09-18 |
| [#3037](https://github.com/zowe/api-layer/issues/3037) | Support upgrade Zowe in HA by loadbalancers | size/M | 2023-08-16 |
| [#2976](https://github.com/zowe/api-layer/issues/2976) | Certificate and password management for System Administrators | size/XL | 2023-07-10 |
| [#2830](https://github.com/zowe/api-layer/issues/2830) | JDK serialization filtering | size/S | 2023-03-13 |
| [#2557](https://github.com/zowe/api-layer/issues/2557) | Improve onboarding of internal APIs | size/L | 2022-08-19 |
| [#2315](https://github.com/zowe/api-layer/issues/2315) | Easier debugging of swagger doc rendering | size/M | 2022-04-22 |
| [#2296](https://github.com/zowe/api-layer/issues/2296) | Use zowe.yaml as the source for the configuration | size/M | 2022-04-13 |

---

## Milestone Tracking

### Milestone: 3.5
| # | Title | Type |
|---|-------|------|
| [#2865](https://github.com/zowe/api-layer/issues/2865) | Support for Open Telemetry | Enhancement |

### Milestone: 3.6
| # | Title | Type |
|---|-------|------|
| [#4409](https://github.com/zowe/api-layer/issues/4409) | Listen on single IP address | Enhancement |
| [#4405](https://github.com/zowe/api-layer/issues/4405) | Group instances | Enhancement |
| [#4400](https://github.com/zowe/api-layer/issues/4400) | Infinispan HA configuration | Enhancement |
| [#4378](https://github.com/zowe/api-layer/issues/4378) | Enabler can choose the preferred DS URL | Enhancement |
| [#3188](https://github.com/zowe/api-layer/issues/3188) | Authorization capabilities of the API Mediation Layer | Enhancement |

### Milestone: 4.0
| # | Title | Type |
|---|-------|------|
| [#4398](https://github.com/zowe/api-layer/issues/4398) | Remove support for VSAM | Tech Excellence |
| [#3243](https://github.com/zowe/api-layer/issues/3243) | What information we return on unsuccessful attempt to login | Enhancement |

---

## High Availability Related

> Issues tagged with `High Availability` label.

| # | Title | Priority | Created |
|---|-------|----------|---------|
| [#4429](https://github.com/zowe/api-layer/issues/4429) | LPAR affinity | High | 2025-12-15 |
| [#4423](https://github.com/zowe/api-layer/issues/4423) | Storage in Caching service by caller | Medium | 2025-12-12 |
| [#4422](https://github.com/zowe/api-layer/issues/4422) | Calling Caching service from Gateway using micro-services | Low (bug) | 2025-12-12 |
| [#4420](https://github.com/zowe/api-layer/issues/4420) | Caching service is not handling trust store well (Infinispan mode) | High (bug) | 2025-12-12 |
| [#4409](https://github.com/zowe/api-layer/issues/4409) | Listen on single IP address | High | 2025-12-01 |
| [#4405](https://github.com/zowe/api-layer/issues/4405) | Group instances | High | 2025-11-27 |
| [#4400](https://github.com/zowe/api-layer/issues/4400) | Infinispan HA configuration | Medium | 2025-11-25 |
| [#4398](https://github.com/zowe/api-layer/issues/4398) | Remove support for VSAM | Tech Excellence | 2025-11-25 |
| [#4378](https://github.com/zowe/api-layer/issues/4378) | Enabler can choose the preferred DS URL | Medium | 2025-11-05 |
