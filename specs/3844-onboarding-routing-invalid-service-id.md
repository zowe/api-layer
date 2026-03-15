# #3844 — Onboarding and routing to a service with invalid serviceId

**GitHub Issue:** https://github.com/zowe/api-layer/issues/3844
**Labels:** bug, Priority: High, V3 | **Created:** 2024-10-10 | **State:** open

---

## Description

Registering a service with a non-conformant `serviceId` — one containing characters invalid in a URI hostname, such as `_` (underscore) — causes the Gateway to return HTTP **500** when routing to that service. The load balancer constructs a `lb://service_id` URI, which Java's `URI` parser rejects because underscore is not permitted in hostnames per RFC 952/1123.

`EurekaUtils.validateServiceId()` already enforces the pattern `^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$` (RFC 952/1123 compliant), but `ApimlInstanceRegistry.validateInstanceInfo()` only logs a warning on violation — it never blocks registration. The validation is silent, and the 500 error occurs much later during routing.

---

## Acceptance Criteria

- A service with a non-conformant `serviceId` (e.g., containing `_`, starting with a digit, or exceeding 63 characters) is rejected at registration time by the Discovery service with HTTP **400** and a descriptive error body identifying the invalid `serviceId`.
- The Gateway never receives a routing request for a service with an invalid `serviceId`, eliminating the 500 error.
- A backward-compatibility flag `apiml.discovery.strictServiceIdValidation` (default: `false`) allows a transition period where violations produce a warning log instead of rejection. The default must be flipped to `true` in the next major release.
- The enabler validates `serviceId` at application startup and logs a clear error (or fails startup) before attempting to register.
- `EurekaUtils.validateServiceId()` throws `InvalidServiceIdException` (not just logs) on pattern mismatch — the current implementation must be confirmed.

---

## Technical Solution

### Files to change

- `discovery-service/src/main/java/org/zowe/apiml/discovery/registry/ApimlInstanceRegistry.java`
- `common-service-core/src/main/java/org/zowe/apiml/util/EurekaUtils.java` — confirm exception is thrown
- `spring-enabler/src/main/java/...` — add startup validation
- `discovery-service/src/main/resources/application.yml` — add `apiml.discovery.strictServiceIdValidation: false`

### Changes

**`ApimlInstanceRegistry.validateInstanceInfo()`** — enforce strictly when flag is enabled:

```java
@Value("${apiml.discovery.strictServiceIdValidation:false}")
private boolean strictValidation;

private void enforceServiceIdValidation(String serviceId) {
    try {
        EurekaUtils.validateServiceId(serviceId);
    } catch (InvalidServiceIdException e) {
        if (strictValidation) {
            throw new MetadataValidationException(
                "Invalid serviceId [" + serviceId + "]: " + e.getMessage() +
                ". See APIML conformance requirements.");
        } else {
            log.warn("Non-conformant serviceId [{}]: {}. " +
                "This will be rejected in a future release.", serviceId, e.getMessage());
        }
    }
}
```

`MetadataValidationException` is already handled in the discovery service's exception handler and results in HTTP 400.

**Spring Enabler startup validation:**

```java
@EventListener(ApplicationStartedEvent.class)
public void validateOnStartup() {
    try {
        EurekaUtils.validateServiceId(serviceId);
    } catch (InvalidServiceIdException e) {
        log.error("Invalid serviceId [{}]: {}. " +
            "This service may fail to route correctly through the Gateway.", serviceId, e.getMessage());
        // Optionally throw to abort startup
    }
}
```

### Tests

**Update `ApimlInstanceRegistryTest`:**
- `givenInvalidServiceIdWithUnderscore_andStrictValidation_whenRegister_thenReturn400()` — set `strictServiceIdValidation=true`, attempt registration with `serviceId=service_test`, assert `MetadataValidationException` is thrown.
- `givenInvalidServiceIdWithUnderscore_andPermissiveValidation_whenRegister_thenLogWarnAndContinue()` — set flag to `false`, assert registration succeeds but a `WARN` log is emitted.

**`EurekaUtilsTest` additions:**
- `givenServiceIdWithUnderscore_whenValidate_thenThrowInvalidServiceIdException()`.
- `givenServiceIdStartingWithHyphen_whenValidate_thenThrowInvalidServiceIdException()`.
- `givenValidServiceId_whenValidate_thenNoException()`.

**Integration test:**
Register a service with `serviceId=service_test` against the Discovery service with `strictServiceIdValidation=true`. Assert the response is HTTP 400 with a body describing the invalid `serviceId`.
