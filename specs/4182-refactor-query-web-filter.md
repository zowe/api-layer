# #4182 — Refactor of `org.zowe.apiml.filter.QueryWebFilter`

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4182
**Labels:** technical excellence, Priority: Medium | **Created:** 2025-06-23 | **State:** open

---

## Description

`QueryWebFilter` is a reactive `WebFilter` that performs four distinct responsibilities in a single class:
1. HTTP method validation (reject non-matching methods immediately)
2. Optional X.509 client certificate verification (check Spring Security context for `X509AuthenticationToken`)
3. Token extraction from cookie or `Authorization` header
4. Token authentication via `ReactiveAuthenticationManager`

The coupling of these concerns makes the class difficult to test in isolation, hard to read, and fragile to modify. It was introduced as a side effect of PR #4108 (refactoring servlets to controllers) and lacks a comprehensive unit test.

---

## Acceptance Criteria

- `QueryWebFilter` is decomposed so that each concern can be tested independently.
- `attemptAuthentication()` is accessible (at minimum package-private) to enable direct unit testing without invoking the full Spring Security context.
- Alternatively, the class is split into two or three focused components, each with its own unit test class.
- All existing security behaviour of the filter chain is preserved: correct HTTP method gating, X.509 requirement enforcement, token extraction, and authentication.
- A comprehensive `QueryWebFilterTest` (or tests for the replacement components) covers all success and failure branches.

---

## Technical Solution

### Files to change

- `apiml/src/main/java/org/zowe/apiml/filter/QueryWebFilter.java` — refactor or decompose
- New/updated: `apiml/src/test/java/org/zowe/apiml/filter/QueryWebFilterTest.java`

### Changes

**Option A (minimal): make `attemptAuthentication()` package-private**

This is the lowest-risk change. The `filter()` method logic stays intact; only the access modifier changes so tests can call `attemptAuthentication()` directly with a mocked `ServerWebExchange`.

**Option B (recommended): extract token extraction to a utility**

Move `httpUtils.getTokenFromRequest()` logic into a dedicated `TokenExtractor` utility class that can be unit-tested without a reactive chain:

```java
public class TokenExtractor {
    public Mono<String> extract(ServerWebExchange exchange) {
        return extractFromCookie(exchange)
            .switchIfEmpty(extractFromHeader(exchange));
    }
}
```

Keep `QueryWebFilter` as a single class but with `TokenExtractor` injected, making all three sub-steps independently mockable.

**Do not rename** any class to `TokenAuthenticationFilter` — this conflicts with the existing `TokenAuthentication` class in the same package.

### Tests

**New `QueryWebFilterTest`** covering:
- `givenWrongHttpMethod_whenFilter_thenFailureHandlerCalledWithMethodNotSupported()`.
- `givenCertificateRequired_andNoCertInSecurityContext_whenFilter_thenFailureHandlerCalledWith401()`.
- `givenCertificateRequired_andCertPresent_andValidToken_whenFilter_thenChainContinues()`.
- `givenCertificateNotRequired_andValidToken_whenFilter_thenChainContinues()`.
- `givenCertificateNotRequired_andNoToken_whenFilter_thenFailureHandlerCalledWith401()`.
- `givenCertificateNotRequired_andInvalidToken_whenFilter_thenFailureHandlerCalledWith401()`.

Use `MockServerWebExchange` and mock the `ReactiveAuthenticationManager` to avoid spinning up a full Spring context.

**New `TokenExtractorTest`** (if Option B is chosen):
- `givenTokenInCookie_thenReturnTokenValue()`.
- `givenTokenInAuthorizationHeader_thenReturnTokenValue()`.
- `givenTokenInBoth_thenPreferCookie()`.
- `givenNoToken_thenReturnEmpty()`.
