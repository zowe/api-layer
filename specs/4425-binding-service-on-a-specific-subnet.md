# #4425 — Binding service on a specific subnet

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4425
**Labels:** enhancement, Priority: Medium | **Created:** 2025-12-12 | **State:** open

---

## Description

Spring Boot's `server.address` property accepts a single `InetAddress`, allowing a service to bind to one specific network interface or `0.0.0.0` (all interfaces). Services that need to listen on more than one specific interface — but not all — had no supported configuration mechanism and were forced to bind to `0.0.0.0`.

This was implemented in PR #4457 (merged 2026-01-27) via `MultiAddressWebServerFactoryCustomizer`. This spec documents the delivered feature and the test coverage required to validate it.

---

## Acceptance Criteria

- A service can be configured with a comma-separated list of IP addresses in `server.address` (e.g., `server.address=127.0.0.1,192.168.1.10`).
- The service binds a separate server connector to each listed address.
- A single address (the existing behaviour) continues to work identically.
- An empty or unset `server.address` leaves the default Tomcat/Netty binding behaviour unchanged (binds to all interfaces).
- A malformed IP address in the list produces a clear `IllegalArgumentException` at startup, not a `NullPointerException`.
- The feature is available to all APIML services that include `apiml-common` on their classpath.

---

## Technical Solution

### Files changed (PR #4457)

- `apiml-common/src/main/java/org/zowe/apiml/web/MultiAddressWebServerFactoryCustomizer.java` — new customizer bean
- Per-service `application.yml` files — document `server.address` as accepting a comma-separated list

### Remaining work: test coverage

The implementation was delivered but the following tests need to be verified or added.

### Tests

**New `MultiAddressWebServerFactoryCustomizerTest`:**
- `givenSingleAddress_whenCustomize_thenOneConnectorCreated()` — mock `ConfigurableReactiveWebServerFactory` (or `TomcatServletWebServerFactory`), call `customize()` with `server.address=127.0.0.1`, assert exactly one address-specific connector is configured.
- `givenCommaSeparatedAddresses_whenCustomize_thenOneConnectorPerAddress()` — `server.address=127.0.0.1,127.0.0.2`, assert two connectors with distinct `address` settings.
- `givenEmptyAddress_whenCustomize_thenDefaultBehaviourUnchanged()` — no `server.address` set → the customizer makes no changes to the factory.
- `givenInvalidAddress_whenCustomize_thenThrowsIllegalArgumentException()` — `server.address=not-an-ip` → assert `IllegalArgumentException` with a message identifying the bad value.

**Integration smoke test:**
Start the service with `server.address=127.0.0.1,127.0.0.2` (in a `@SpringBootTest` with `webEnvironment = RANDOM_PORT`) and assert that both addresses respond to a health check request, while a request to a different address (e.g., `127.0.0.3`) is refused.
