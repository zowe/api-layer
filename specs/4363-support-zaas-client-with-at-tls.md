# #4363 — Support ZAAS client with AT-TLS

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4363
**Labels:** enhancement, Priority: High | **Created:** 2025-10-27 | **State:** open

---

## Description

The ZAAS client (`ZaasClientImpl`) creates HTTP connections in two modes:
- `httpOnly=true` — plain HTTP, no TLS (intended for non-secure environments)
- Default — mTLS using `ZaasHttpsClientProvider` with a configured keystore

With AT-TLS (Application Transparent Transport Layer Security on z/OS), the TLS handshake is handled by the z/OS Communication Server at the network layer. The application uses plain HTTP sockets, but the wire traffic is encrypted. The AT-TLS policy attaches the application's certificate to the socket transparently.

The current `httpOnly=true` mode is semantically incorrect for AT-TLS: it implies the endpoint is not secured, while AT-TLS does provide security. Additionally, `httpOnly=true` does not set the correct `http://` scheme for URL construction in all cases. There is no dedicated AT-TLS mode, making configuration ambiguous.

---

## Acceptance Criteria

- `ConfigProperties` exposes an `atTls` boolean flag.
- When `atTls=true`, the ZAAS client uses plain HTTP sockets (`http://` scheme) without configuring any Java-level SSL context — TLS is handled externally by AT-TLS.
- When `atTls=true`, a configured `keyStorePath` is silently ignored (not required) and no `ZaasConfigurationException` is thrown for a missing keystore.
- When `atTls=true`, `httpClient` and `httpClientWithoutCert` are the same instance (no distinction — AT-TLS handles certificate selection via policy).
- URL construction uses `http://` scheme when `atTls=true`, regardless of other configuration.
- Existing `httpOnly=true` and full-TLS modes are unchanged.
- The AT-TLS mode is documented in the ZAAS client README with configuration examples.

---

## Technical Solution

### Files to change

- `zaas-client/src/main/java/org/zowe/apiml/zaasclient/config/ConfigProperties.java` — add `atTls` field
- `zaas-client/src/main/java/org/zowe/apiml/zaasclient/service/internal/ZaasClientImpl.java` — add AT-TLS constructor branch
- `zaas-client/src/main/java/org/zowe/apiml/zaasclient/service/internal/ZaasHttpClientProvider.java` — reuse for AT-TLS (no new class needed)

### Changes

**`ConfigProperties`** — add field:

```java
@Builder.Default
private boolean atTls = false;
```

**`ZaasClientImpl` constructor** — add branch before `httpOnly`:

```java
if (configProperties.isAtTls()) {
    // AT-TLS: plain HTTP sockets, no Java-level SSL context
    CloseableHttpClient plainClient = new ZaasHttpClientProvider().getHttpClient();
    httpClient = plainClient;
    httpClientWithoutCert = plainClient;
} else if (configProperties.isHttpOnly()) {
    ...
} else {
    ...
}
```

**URL scheme in `ConfigProperties.getZaasUrl()`** — ensure `http://` scheme is used when `atTls=true`:

```java
public String getZaasUrl() {
    String scheme = (isHttpOnly() || isAtTls()) ? "http" : "https";
    return scheme + "://" + apimlHost + ":" + apimlPort + apimlBaseUrl;
}
```

Do not create a separate `ZaasAtTlsClientProvider` class — `ZaasHttpClientProvider` already produces the correct plain-HTTP client.

### Tests

**New `ZaasClientImplAtTlsTest`:**
- `givenAtTlsTrue_whenConstructed_thenUsesPlainHttpClient()` — assert `httpClient == httpClientWithoutCert` and no `SSLContext` is configured.
- `givenAtTlsTrue_andKeyStoreProvided_whenConstructed_thenNoException()` — confirm that providing a `keyStorePath` does not cause `ZaasConfigurationException` when `atTls=true`.
- `givenAtTlsTrue_whenGetZaasUrl_thenUsesHttpScheme()` — assert the URL begins with `http://`.
- `givenAtTlsTrue_whenLogin_thenRequestSentToHttpEndpoint()` — use WireMock to stub `POST http://host:port/.../auth/login` and assert a successful login response.

**Update `ConfigPropertiesTest`:**
- `givenAtTlsFlag_whenBuilt_thenIsAtTlsReturnsTrue()`.
- `givenDefaultConfig_whenBuilt_thenAtTlsIsFalse()`.
