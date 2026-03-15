# #3878 — When southbound service is down the Gateway returns 500 instead of 503

**GitHub Issue:** https://github.com/zowe/api-layer/issues/3878
**Labels:** bug, Priority: High, V3 | **Created:** 2024-11-05 | **State:** open

---

## Description

When a registered service is down but still present in the Eureka registry, the Gateway should return HTTP **503** (Service Unavailable). Instead, it returns HTTP **500** (Internal Server Error) for some connection failure types.

`NettyRoutingFilterApiml.filter()` catches `ConnectException` (TCP connection refused) and wraps it into `ServiceNotAccessibleException`, which `GatewayExceptionHandler` correctly maps to 503. However, Netty's own `ConnectTimeoutException` (which extends `RuntimeException`, not `ConnectException`) is not caught and falls through to the generic `Exception` handler, returning 500.

This issue overlaps significantly with #4512 (exception classification). Both should be addressed in the same PR.

---

## Acceptance Criteria

- All TCP-level connection failures to downstream services — including `ConnectException`, `ConnectTimeoutException`, `NoRouteToHostException`, and their Netty wrappers — result in HTTP **503** with an APIML-structured JSON body.
- TLS failures (certificate errors, handshake failures) result in HTTP **500** with a distinct TLS error message, not 503.
- The existing acceptance test `allInstancesAreUnavailable()` continues to pass.
- A new unit test covers `ConnectTimeoutException` specifically.
- The response body for 503 is never empty — it always contains the APIML `messages` JSON array.

---

## Technical Solution

### Files to change

- `gateway-service/src/main/java/org/zowe/apiml/gateway/config/NettyRoutingFilterApiml.java`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/controllers/GatewayExceptionHandler.java`

This fix should be implemented together with #4512, which provides the full exception classification logic including the recursive cause-unwrapping helper.

### Changes

**Extend `isServiceUnavailable()` in `NettyRoutingFilterApiml`** (see #4512 for the full implementation):

```java
private static boolean isServiceUnavailable(Throwable e) {
    Throwable cause = e;
    while (cause != null) {
        if (cause instanceof ConnectException           // TCP refused
                || cause instanceof ConnectTimeoutException   // Netty timeout
                || cause instanceof NoRouteToHostException) { // no route
            return true;
        }
        // Do NOT catch ChannelException broadly — it includes SSL errors
        cause = cause.getCause();
    }
    return false;
}
```

### Tests

**New tests in `NettyRoutingFilterApimlTest`:**
- `givenConnectTimeoutException_whenFilter_thenProducesServiceNotAccessibleException()` — mock `super.filter()` returning `Mono.error(new ConnectTimeoutException("connect timed out"))`, assert the result is `ServiceNotAccessibleException` (which maps to 503).
- `givenConnectExceptionNestedInClosedChannelException_whenFilter_thenProduces503()` — wrap `ConnectException` inside `ClosedChannelException`, assert 503.

**Update `allInstancesAreUnavailable()` acceptance test:**
Add assertion that the response body is non-empty and is valid JSON containing a `messages` array, not just an HTTP 503 status code.
