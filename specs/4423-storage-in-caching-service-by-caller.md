# #4423 — Storage in Caching service by caller

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4423
**Labels:** enhancement, Priority: Medium, High Availability | **Created:** 2025-12-12 | **State:** open

---

## Description

The Caching service identifies the calling service by trusting the `X-Certificate-DistinguishedName` header in the request. This header is set by the calling service itself and is not verified against the actual TLS client certificate presented during the mTLS handshake. Any service that can reach the Caching service endpoint can impersonate any other service by setting an arbitrary `X-Certificate-DistinguishedName` value, gaining access to or corrupting another service's cached data.

---

## Acceptance Criteria

- When a client presents a TLS client certificate during the mTLS handshake, the Caching service extracts the caller identity exclusively from the certificate's Distinguished Name — the `X-Certificate-DistinguishedName` header is ignored if the TLS certificate is present.
- If both the TLS certificate DN and the `X-Certificate-DistinguishedName` header are present and they differ, the request is rejected with HTTP 401.
- In modulith mode (in-process), the caller identity is taken from the `Authentication` object in the Spring Security context.
- Requests with no certificate and no header are rejected with HTTP 401.
- The `X-CS-Service-ID` header override continues to work when the certificate DN matches the expected DN for that service.
- Backward compatibility: reverse-proxy deployments that forward the DN in the header continue to work, as long as the header value matches the certificate presented to the proxy.

---

## Technical Solution

### Files to change

- `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java` — update service identity extraction
- New: `caching-service/src/main/java/org/zowe/apiml/caching/security/CertificateServiceIdentityExtractor.java`

### Changes

**New `CertificateServiceIdentityExtractor`**

```java
@Component
public class CertificateServiceIdentityExtractor {

    public String extractServiceId(ServerWebExchange exchange) {
        String certDn = extractCertificateDn(exchange.getRequest().getSslInfo());
        String headerDn = exchange.getRequest()
            .getHeaders().getFirst("X-Certificate-DistinguishedName");

        if (certDn != null && headerDn != null && !certDn.equals(headerDn)) {
            throw new UnauthorizedException("Certificate DN does not match header DN");
        }

        String resolvedDn = certDn != null ? certDn : headerDn;
        if (resolvedDn == null) {
            throw new UnauthorizedException("No certificate or DN header provided");
        }

        String specificServiceId = exchange.getRequest()
            .getHeaders().getFirst("X-CS-Service-ID");
        return specificServiceId != null
            ? resolvedDn + ", SERVICE=" + specificServiceId
            : resolvedDn;
    }

    private String extractCertificateDn(SslInfo sslInfo) {
        if (sslInfo == null || sslInfo.getPeerCertificates() == null
                || sslInfo.getPeerCertificates().length == 0) {
            return null;
        }
        return sslInfo.getPeerCertificates()[0].getSubjectX500Principal().getName();
    }
}
```

Update `CachingController` to delegate service identity resolution to `CertificateServiceIdentityExtractor` rather than reading the header directly.

### Tests

**Update `CachingControllerTest`:**
- `WhenCertificateDNDoesNotMatchHeader`: mock `SslInfo` with DN `"CN=service-a"` and header DN `"CN=service-b"` → assert HTTP 401.
- `WhenNoCertificateAndNoHeader`: no `SslInfo`, no header → assert HTTP 401.
- Update `WhenUseSpecificServiceHeader`: add a case where the certificate DN is present and matches the header → assert 200.

**New `CertificateServiceIdentityExtractorTest`:**
- `givenCertificateDnOnly_thenReturnCertDn()`.
- `givenHeaderDnOnly_thenReturnHeaderDn()`.
- `givenBothMatchingDns_thenReturnCertDn()`.
- `givenBothConflictingDns_thenThrowUnauthorized()`.
- `givenNeitherCertNorHeader_thenThrowUnauthorized()`.
- `givenCertDnAndServiceIdHeader_thenReturnCombinedIdentity()`.
