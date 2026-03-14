# Bug Validation Report

Validation of all open `bug`-labeled issues in [zowe/api-layer](https://github.com/zowe/api-layer/issues) against the current codebase (single-service-deployment / modulith mode in `apiml/`).

**Validated:** Fri Mar 13 2026

Legend: **STILL EXISTS** | **FIXED** | **PARTIAL** | **NOT VERIFIABLE** (z/OS-only, no code evidence either way)

---

## STILL EXISTS

### #4512 — Detection of failed services during routing
- **File:** `gateway-service/src/main/java/org/zowe/apiml/gateway/config/NettyRoutingFilterApiml.java:85–93`
- **Detail:** Condition only catches `e.getCause() instanceof ConnectException` (one level deep). Doubly-wrapped `ConnectException`s fall through to 500 instead of 503.

### #4511 — Sticky session and HA
- **File:** `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/DeterministicLoadBalancer.java:235–243`
- **Detail:** No reachability check on the preferred instance; all requests are routed to a dead instance until Eureka eviction (~90s window).

### #4510 — Unnecessary call of caching service during routing
- **File:** `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/DeterministicLoadBalancer.java:98` vs `164`
- **Detail:** `cache.retrieve()` is always called before the `shouldIgnore()`/`lbTypeIsAuthentication` check. Triggers a caching-service HTTP call for every authenticated request regardless of load balancer type.

### #4451 — Salt in caching service represented as String with binary data
- **File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java:171,199–200`
- **Detail:** `new String(salt)` used on raw bytes with no Base64 encoding. Salt can silently truncate if bytes contain control characters.

### #4421 — Loading/initializing of salt is not an atomic operation
- **File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java:157–174`
- **Detail:** No synchronization around the read+generate+store sequence. Multiple threads or cluster nodes can each generate and store different salts concurrently.

### #4444 — `/gateway/api/v1/auth/ticket` does not accept OIDC token
- **File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/query/QueryFilter.java:88`
- **Detail:** Hard-codes `TokenAuthentication.Type.JWT`. OIDC tokens never reach the OIDC auth path on the ticket endpoint.

### #4424 — Performance of PAT validation depends on amount of stored records
- **File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java:90`
- **Detail:** Calls `cachingServiceClient.readAllMaps()` (loads entire dataset) for every PAT validation. Scales linearly with record count.

### #4426 — Exception handling in Caching Service
- **File:** `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java:209,323`
- **Detail:** No `@ControllerAdvice` bean. `StorageException` still used as a general-purpose wrapper for all storage errors, making it hard to control response messages.

### #4422 — Calling Caching service from Gateway using micro-services
- **Files:** `gateway-service/.../caching/CachingServiceClientRest.java:56–58`, `zaas-service/.../cache/CachingServiceClient.java:63–68`
- **Detail:** Distributed mode still self-routes through the Gateway with no retry logic. Only modulith mode is fixed (via `CachingServiceClientApi` with `@ConditionalOnBean`).

### #4418 — Catalog tile is required for registration but never used
- **Files:** `onboarding-enabler-java/.../EurekaInstanceConfigValidator.java:52–54`, `EurekaInstanceConfigCreator.java:113–122`
- **Detail:** Tile still enforced (warning only, not error), wizard still shows tile section, and tile metadata is still written into Eureka registration.

### #4362 — Weird configuration of idle connection
- **File:** `apiml-common/src/main/java/org/zowe/apiml/product/web/HttpConfig.java:104–105,219`
- **Detail:** Idle timeout is 5s but the eviction task runs every 30s. Connections can sit idle up to ~35s before cleanup.

### #4350 — serviceId problems when onboarding static API
- **Files:** `api-catalog-services/.../StaticDefinitionGenerator.java:38`, `common-service-core/.../EurekaUtils.java:35`
- **Detail:** `StaticDefinitionGenerator` allows uppercase letters in serviceId; `EurekaUtils.SERVICE_ID_PATTERN` rejects them. Inconsistent validators produce confusing failures at discovery time.

### #4193 — Distributing invalidated tokens between instances not working in all cases
- **Files:** `zaas-service/.../AuthenticationService.java:391–407`, `zaas-service/.../ZaasStartupListener.java:37–59`
- **Detail:** No startup trigger exists. When a restarted instance comes back online it never requests redistribution from surviving instances.

### #4163 — Message ZWEAG121E is missing from doc and confusing
- **File:** `apiml-security-common/.../login/ShouldBeAlreadyAuthenticatedFilter.java:40`
- **Detail:** An x509 certificate-not-mapped failure triggers ZWEAG121E ("Authorization header is missing"), which is a completely wrong message for this scenario.

### #4126 — Repeating error log when HA peer is down
- **Status note:** Fixed in `ApimlPeerEurekaNode.java:473–493` for network-level errors. However, a secondary path in `ZaasStartupListener` / JWT init can still produce repeated logs when the Discovery debug profile is active (see also #4149).

### #4149 — Discovery service debug mode breaks ZAAS
- **Files:** `zaas-service/.../JwtSecurity.java:267–283`, `discovery-service/src/main/resources/application.yml:156–181`
- **Detail:** Debug profile sets `com.netflix.eureka: DEBUG`, causing high-frequency `CacheRefreshedEvent` floods. ZAAS JWT init still structurally capable of being disrupted by this event storm.

### #4143 — AT-TLS + HAFT: Exception reading certificate upon failover
- **File:** `apiml-tomcat-common/.../filter/AttlsHttpHandler.java:102–123,148–152`
- **Detail:** No retry, backoff, or certificate cache. Transient AT-TLS context failures during a failover window still return HTTP 500 "Exception reading certificate".

### #4340 — Extensions loaded by gateway classloader do not work with AT-TLS
- **File:** `apiml-extension-loader/.../extension/ExtensionsLoader.java:38`
- **Detail:** Extension beans are scanned in an isolated `AnnotationConfigApplicationContext` that does not inherit the AT-TLS SSL context or AT-TLS profile properties from the parent context.

### #4318 — Gateway sensitive to IPv6 host header
- **File:** `gateway-service/.../config/WebSecurity.java:525–539`
- **Detail:** No IPv6 host-header normalization anywhere in the gateway. `StrictServerWebExchangeFirewall` has no IPv6 bracket handling; colon ambiguity in IPv6 addresses still causes 400/500 responses.

### #4286 — Message incomplete or wrong (ZWEAG510E)
- **Files:** `gateway-service/src/main/resources/gateway-log-messages.yml:101–106`, `GatewayExceptionHandler.java:169`
- **Detail:** Message text has no format parameter for the status code. The actual status code is only visible in the HTTP response code, not in the message body.

### #4273 — Improve error message for PassTicket generation
- **File:** `common-service-core/.../passticket/IRRPassTicketGenerationException.java:20–31`
- **Detail:** Neither `userId` nor `applicationName` is stored in or surfaced by the exception. Users only see the SAF/RACF return codes.

### #3976 — Invalid validation of scopes when using PAT (with ZSS)
- **Files:** `gateway-service/.../filters/ZaasSchemeTransformRest.java:85`, `AbstractTokenFilterFactory.java:100–103`
- **Detail:** A ZAAS 401 due to invalid PAT scope and a ZAAS 503 (ZAAS unavailable) both result in the same stripped-credentials path. The Gateway does not distinguish between the two.

### #3841 — Missing reason and action in certain error codes
- **File:** `apiml-common/src/main/resources/common-log-messages.yml:61–74`
- **Detail:** ZWEAO404, ZWEAO405, and ZWEAO415 all have no `reason` or `action` fields. Nearby entries (e.g. ZWEAO402, ZWEAO503) correctly include both.

### #3226 — Fix response message when user revoked using x509 auth
- **File:** `zaas-service/.../service/zosmf/ZosmfService.java:360,375`
- **Detail:** Two active `// TODO` comments referencing issue #2995 confirm this is unresolved. Generic "invalid authentication" message is returned for the revoked-user case.

### #3097 — Login endpoint provides too much information on SAF auth failure
- **Files:** `apiml-security-common/.../error/AuthExceptionHandler.java:167–170`, `apiml/.../ApimlExceptionHandler.java:119–122`
- **Detail:** `ex.getMessage()` with the full SAF errno name and explanation (e.g. `"ESRCH: identity not defined"`) is embedded verbatim in the response body.

### #3007 — Fix 401 responses (X-Zowe-Auth-Failure security risk)
- **Files:** `gateway-service/.../filters/AbstractAuthSchemeFactory.java:183,208–209,255–256`, `X509FilterFactory.java:59,71–72`
- **Detail:** Specific internal error messages are still placed in the `X-Zowe-Auth-Failure` header for certificate and identity-mapping failures, despite being fixed for the generic token error path.

### #3131 — Gateway says it is shutting down but nothing happened
- **File:** `zaas-service/.../security/service/JwtSecurity.java:267–283`
- **Detail:** The z/OSMF availability timeout path only logs an error and does NOT shut down. ZAAS continues running in a broken state after the timeout expires.

### #3883 — Caching service infinispan port collision
- **File:** `caching-service/.../infinispan/config/InfinispanConfig.java:86–93,126–138`
- **Detail:** (1) No port-in-use check before JGroups attempts to bind. (2) Workspace path is not isolated beyond `haInstance_id`; shared workspace directories can lead to data crossover.

### #4282 — API Catalog shows schemas in API Docs with wrong styles
- **Files:** `api-catalog-ui/frontend/src/components/Swagger/SwaggerUIApiml.jsx:147–174`, `Swagger.css`
- **Detail:** No OAS 3.1-aware CSS overrides. swagger-ui v5 renders OAS 3.1 schemas in a different DOM structure than the CSS targets, causing visual inconsistencies.

### #4283 — Block Authorize button in API Catalog
- **File:** `api-catalog-ui/frontend/src/components/Swagger/Swagger.css:19–22`
- **Detail:** Authorize button is hidden via a fragile `nth-child` CSS selector. No proper API-level disabling (`supportedSubmitMethods: []`) is configured in the SwaggerUI component.

### #2991 — API ML BOM missing dependencies
- **File:** `platform/build.gradle`
- **Detail:** The `mavenJava` publication block is missing `from components.javaPlatform`. The generated POM contains no `<dependencyManagement>` section.

### #4159 — Expand description of swagger /auth/query
- **File:** `zaas-service/src/main/resources/zaas-api-doc.json:54`
- **Detail:** The `summary` field still reads "Validate the authentication token." only. The description body was expanded to mention retrieval, but the Swagger UI headline remains misleading.

### #4478 — IZUG846W: z/OSMF rejects request from remote site
- **File:** `gateway-service/.../config/RoutingConfig.java:58–65`
- **Detail:** `Origin` header removal is conditional on `corsEnabled=true`. CORS-disabled deployments still forward the `Origin` header to z/OSMF, which can trigger the rejection.

---

## FIXED

### #4420 — Caching service not handling trust store well (Infinispan mode)
- **File:** `caching-service/src/main/resources/infinispan.xml:36–47`
- **Fix:** `truststore_name`, `truststore_type`, and `truststore_password` are all now present in the `SSL_KEY_EXCHANGE` element.

### #4211 — Remove use of reflection to initialize custom Tomcat connector (modulith)
- **File:** `apiml/src/main/java/org/zowe/apiml/ModulithConfig.java`
- **Fix:** All reflection removed in PR #4457. Replaced with the public `TomcatConnectorCustomizer` API.

### #3878 — Southbound service down → Gateway returns 500 instead of 503
- **Files:** `gateway-service/.../config/NettyRoutingFilterApiml.java:84–94`, `GatewayExceptionHandler.java:172–176`
- **Fix:** `ConnectException` is wrapped into `ServiceNotAccessibleException`, which is mapped to HTTP 503 by the exception handler.

### #3950 — Fix 400 return code on SAF errors (error 8/16/28)
- **File:** `common-service-core/.../passticket/AbstractIRRPassTicketException.java:53–75`
- **Fix:** All error codes, including `ERR_8_16_28`, now map to `SC_INTERNAL_SERVER_ERROR` (500).

### #2902 — Invalid log message and response on missing credentials
- **File:** `apiml-security-common/.../error/AuthExceptionHandler.java:72–281`
- **Fix:** Refactored to a map-based dispatch. Logged status always matches the returned HTTP status code. Multiple 401 messages eliminated.

### #3844 — Onboarding and routing to a service with invalid serviceId
- **Files:** `gateway-service/.../service/routing/RouteDefinitionProducer.java:68–80`, `ValidateAPIController.java:291–304`
- **Fix:** Conformance validator rejects non-`[a-z0-9]` IDs. Router uses `apimlId` to construct the `lb://` URI, avoiding underscore/URI-parsing failures.

### #4042 — AT-TLS server enabled + client disabled has wrong registration
- **Files:** `gateway-service/.../config/RegistryConfig.java:57–70`, `RegistryConfigTest.java:49–53`
- **Fix:** `determineScheme()` correctly returns `https` when server AT-TLS is on and client AT-TLS is off. Covered by a unit test.

### #4319 — AT-TLS enabled → service homepage URLs show http instead of https
- **Files:** `api-catalog-services/.../apicatalog/swagger/ContainerService.java:66–67,131–155`, `TransformService.java:51–108`
- **Fix:** AT-TLS client flag is read and passed into URL transformation to force the `https` scheme on generated homepage URLs.

### #4111 — Wrong message with POST to `/gateway/api/v1/services`
- **Files:** `gateway-service/.../services/ServicesInfoController.java`, `GatewayExceptionHandler.java:136–140`
- **Fix:** Endpoint only registers `@GetMapping`; a POST returns 405 with the accurate `methodNotAllowed` message.

### #4126 — Repeating error log when HA peer is down
- **File:** `apiml-common/.../product/eureka/client/ApimlPeerEurekaNode.java:473–493`
- **Fix:** `logNetworkErrorSample()` rate-limits repeated peer error logs to at most once per 10 seconds.

### #4312 — VSAM property of caching-service never read
- **File:** `apiml-package/src/main/resources/bin/start.sh:176–178`
- **Fix:** Both the `if` condition and the assignment now use the same compound expression `${ZWE_configs_storage_vsam_name:-${ZWE_components_caching_service_storage_vsam_name}}`.

---

## PARTIAL

### #4512 — Detection of failed services during routing *(also listed under STILL EXISTS)*
- The original bug described an overly broad catch. The catch is now narrow (`ConnectException` only), which is an improvement, but it only checks one level of cause depth. Some "service down" signals (e.g. doubly-wrapped `ConnectException`) still produce a 500.

### #4160 — Swagger page not clear about what URL is needed
- **File:** `gateway-service/.../config/SwaggerConfig.java:105–128`
- `updatePaths()` prepends the base path to server URLs, partially addressing the issue. No test enforces that the full base path is always displayed correctly.

### #4180 — z/OSMF incorrect SSO and API base path in Zowe v3.2
- **SSO sub-issue: FIXED** — `Authentication.java:46–59` correctly defaults z/OSMF SSO to `true`.
- **Base path sub-issue: STILL EXISTS** — `TransformService.java:138–161` still generates `/ibmzosmf/api/{api-version}` instead of `/ibmzosmf/`.

### #3461 — JSON schemas miss some configuration parameters
- Most caching-service parameters from `start.sh` are now covered in `caching-service-config.json`.
- Still missing: `zowe.network.server.listenAddresses[0]` and the SSL `protocol` property.

### #3131 — Gateway says it is shutting down but nothing happened
- **`System.exit(1)` still present** in the async z/OSMF listener path (`JwtSecurity.java:325`, marked `// TODO remove`).
- **Timeout path still does not shut down** (`JwtSecurity.java:267–283`) — logs an error and continues running in a broken state.

---

## NOT VERIFIABLE (z/OS-only or environment-specific)

| # | Title | Reason |
|---|-------|--------|
| #4478 | IZUG846W z/OSMF remote site rejection | Requires live z/OSMF and specific network topology to reproduce |
| #4341 | Problem shutting Zowe 3.3 down (SLIP trap) | z/OS-specific SLIP trap; no Java code to inspect |
| #4325 | Java libraries wrong paths (s390) | z/OS `LIBPATH` startup script for s390 architecture |
| #4215 | AT-TLS does not work (general) | Multi-component; requires live z/OS environment |
| #4087 | Weak cipher suites | Configuration-dependent; requires runtime TLS negotiation to verify |
| #4063 | Confused about `components.gateway.apiml.security.zosmf.applid` parameter | Documentation/config naming inconsistency; primarily a docs issue |
| #4055 | z/OSMF incorrect version/URL in API Catalog | Template issue; base path partially confirmed by #4180 analysis |
| #3870 | `verifyCertificates` NONSTRICT/DISABLED not working in containers | Requires containerized environment with specific certificate setup |
| #4314 | Issue with docs.zowe.org authentication page | Docs-only issue; no code to inspect |
| #3944 | NPE with 500 on z/OSMF token with untrusted cert | Requires specific z/OS certificate configuration to reproduce |
| #3901 | ZWEAS123E invalid token type | Requires live Zowe v3 installation with specific auth configuration |
| #3461 | JSON schemas miss config parameters | Partially verifiable (see PARTIAL above); full coverage requires running all start.sh paths |

---

## Summary

| Status | Count |
|--------|-------|
| Still Exists | 33 |
| Fixed | 11 |
| Partial | 5 |
| Not Verifiable | 12 |
| **Total** | **61** |
