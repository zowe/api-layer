# #4512 — Detection of failed services during routing

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4512
**Labels:** bug, new | **Created:** 2026-03-12 | **State:** open

---

## Description

`NettyRoutingFilterApiml` wraps only `ConnectException` into `ServiceNotAccessibleException`, which `GatewayExceptionHandler` maps to HTTP 503. All other connection-related exceptions — including TLS/certificate failures (`SSLHandshakeException`, PKIX errors wrapped inside `SSLException`) and Netty-specific timeout exceptions (`ConnectTimeoutException`) — are re-thrown as-is and hit the generic `Exception` catch-all handler, returning HTTP 500. This makes it impossible for callers and operators to distinguish between a genuinely unreachable service (503) and a misconfigured TLS connection (which should also be 500 but with a meaningful TLS-specific message).

Additionally, the debug log in `handleServiceNotAccessibleException` only logs `ex.getMessage()`, discarding the full cause chain and making diagnosis harder.

---

## Acceptance Criteria

- When a downstream service is unreachable due to a TCP connection failure (`ConnectException`, `ConnectTimeoutException`, `NoRouteToHostException`), the Gateway returns HTTP **503** with an APIML-structured JSON error body.
- When a downstream service fails due to a TLS error (`SSLHandshakeException`, PKIX path failure), the Gateway returns HTTP **500** with a structured JSON body that includes a specific TLS error message key — distinct from the generic 500 catch-all body.
- The `handleServiceNotAccessibleException` log statement includes the full exception cause chain at `DEBUG` level.
- The `ServiceTlsException` handler logs at `WARN` level so TLS misconfigurations toward downstream services are visible to operators without requiring `DEBUG` logging.
- A `ConnectException` nested several levels deep inside Netty wrapper exceptions (e.g., `ClosedChannelException` → `ConnectException`) is still classified as 503, not 500.
- All existing behaviour for non-connection exceptions (e.g., `AuthenticationException` → 401) is unchanged.

---

## Technical Solution

### Files to change

- `gateway-service/src/main/java/org/zowe/apiml/gateway/config/NettyRoutingFilterApiml.java`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/controllers/GatewayExceptionHandler.java`
- New: `gateway-service/src/main/java/org/zowe/apiml/gateway/exceptions/ServiceTlsException.java`

### Changes

**1. Recursive exception classification in `NettyRoutingFilterApiml.filter()`**

Replace the single `ConnectException` check with a `classifyException()` helper that walks the full cause chain:

```java
private static boolean isServiceUnavailable(Throwable e) {
    Throwable cause = e;
    while (cause != null) {
        if (cause instanceof ConnectException
                || cause instanceof ConnectTimeoutException
                || cause instanceof NoRouteToHostException) {
            return true;
        }
        cause = cause.getCause();
    }
    return false;
}

private static boolean isTlsFailure(Throwable e) {
    Throwable cause = e;
    while (cause != null) {
        if (cause instanceof SSLHandshakeException
                || (cause instanceof SSLException && !(cause instanceof SSLHandshakeException))) {
            return true;
        }
        cause = cause.getCause();
    }
    return false;
}

@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return super.filter(exchange, chain).onErrorResume(e -> {
        if (isServiceUnavailable(e)) {
            var uri = exchange.getRequest().getURI();
            return Mono.error(new ServiceNotAccessibleException(
                String.format("Service is not available at %s://%s:%d",
                    uri.getScheme(), uri.getHost(), uri.getPort()), e));
        }
        if (isTlsFailure(e)) {
            return Mono.error(new ServiceTlsException(
                "TLS handshake failed connecting to downstream service: " + e.getMessage(), e));
        }
        return Mono.error(e);
    });
}
```

Do **not** catch `io.netty.channel.ChannelException` broadly — it is the base class of SSL channel errors and is too wide.

**2. New `ServiceTlsException`**

```java
public class ServiceTlsException extends RuntimeException {
    public ServiceTlsException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**3. New handler in `GatewayExceptionHandler`**

```java
@ExceptionHandler(ServiceTlsException.class)
public Mono<Void> handleServiceTlsException(ServerWebExchange exchange, ServiceTlsException ex) {
    log.warn("TLS failure routing request {}: {}", exchange.getRequest().getURI(), ex.getMessage(), ex);
    return setBodyResponse(exchange, SC_INTERNAL_SERVER_ERROR,
        "org.zowe.apiml.gateway.tlsError", exchange.getRequest().getPath());
}
```

**4. Improve the existing 503 log**

```java
// Before:
log.debug("A service is not available at the moment to finish request {}: {}",
    exchange.getRequest().getURI(), ex.getMessage());

// After:
log.debug("A service is not available at the moment to finish request {}: {}",
    exchange.getRequest().getURI(), ex.getMessage(), ex);
```

### Tests

**`NettyRoutingFilterApimlTest` — new `@Nested class GivenExceptionClassification`:**
- `givenConnectException_whenFilter_thenProducesServiceNotAccessibleException()` — mock `super.filter()` returning `Mono.error(new ConnectException())`, assert result is `ServiceNotAccessibleException`.
- `givenConnectExceptionWrappedInRuntimeException_whenFilter_thenStillProduces503()` — wrap `ConnectException` inside `new RuntimeException(new ConnectException())`, assert `ServiceNotAccessibleException` (recursive unwrap).
- `givenConnectTimeoutException_whenFilter_thenProducesServiceNotAccessibleException()` — Netty's `ConnectTimeoutException` (extends `RuntimeException`), assert 503 path.
- `givenSslHandshakeException_whenFilter_thenProducesServiceTlsException()` — assert `ServiceTlsException` is produced, not `ServiceNotAccessibleException`.
- `givenGenericRuntimeException_whenFilter_thenPropagatesUnchanged()` — unrecognised exception passes through.

**`GatewayExceptionHandlerTest` — additions:**
- `givenServiceTlsException_whenHandled_thenReturns500WithTlsMessageKey()` — assert HTTP 500 and JSON body with `org.zowe.apiml.gateway.tlsError` message key, distinct from the generic 500 body.
- `givenServiceNotAccessibleException_whenHandled_thenDebugLogIncludesStackTrace()` — use a Logback `ListAppender` to capture log events and assert the event includes a `Throwable`.
