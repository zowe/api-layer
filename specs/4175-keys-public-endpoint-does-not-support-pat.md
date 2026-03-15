# #4175 — Endpoint `/zaas/api/v1/auth/keys/public` does not support PAT

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4175
**Labels:** enhancement, Priority: Low | **Created:** 2025-06-19 | **State:** open

---

## Description

`GET /zaas/api/v1/auth/keys/public` returns the current JWT signing key as a PEM-encoded string. Its implementation (`AuthController.getPublicKeyUsedForSigning()`) calls `getCurrentKey()`, which returns either the ZAAS signing key or the z/OSMF signing key — never both.

When z/OSMF is the authentication provider and PAT (Personal Access Token) is also enabled, two signing keys are active:
- PAT tokens are signed by the APIML key.
- Standard JWT tokens are signed by z/OSMF.

Services that download the public key via this endpoint receive only one key and cannot validate both token types. The existing `/keys/public/all` and `/keys/public/current` (JWK Set format) endpoints already return all keys, but this single-PEM endpoint does not.

---

## Acceptance Criteria

- `GET /keys/public` is deprecated in the OpenAPI specification with a note directing consumers to `/keys/public/current` (JWK Set format).
- Every response from `GET /keys/public` includes a `Deprecation: true` header and a `Link: </zaas/api/v1/auth/keys/public/current>; rel="successor-version"` header (per RFC 8594).
- When the APIML key is active (PAT-enabled deployment), the endpoint returns the APIML public key as PEM — not the z/OSMF key — because PAT tokens are always signed by APIML.
- The endpoint continues to return 503 (not 500) when the key is not yet initialised (coordinated with fix in #4176).
- `/keys/public/current` continues to return all active keys as a JWK Set and is the recommended replacement.

---

## Technical Solution

### Files to change

- `zaas-service/src/main/java/org/zowe/apiml/zaas/controllers/AuthController.java` — add deprecation headers and fix key selection
- `zaas-service/src/main/resources/apiml-api-info.yaml` — mark endpoint as `deprecated: true`

### Changes

**Add deprecation headers to every response from `getPublicKeyUsedForSigning()`:**

```java
private static final String DEPRECATION_HEADER = "Deprecation";
private static final String LINK_HEADER = "Link";
private static final String SUCCESSOR_LINK =
    "</zaas/api/v1/auth/keys/public/current>; rel=\"successor-version\"";

@GetMapping(path = PUBLIC_KEYS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<Object> getPublicKeyUsedForSigning() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(DEPRECATION_HEADER, "true");
    headers.set(LINK_HEADER, SUCCESSOR_LINK);

    List<JsonWebKey> keys = getCurrentKey();
    if (keys.isEmpty()) {
        headers.set("Retry-After", String.valueOf(retryAfterSeconds));
        return new ResponseEntity<>(..., headers, HttpStatus.SERVICE_UNAVAILABLE);
    }
    // Return APIML key as PEM when PAT is active; z/OSMF key otherwise
    JsonWebKey keyToReturn = selectKeyForPem(keys);
    return new ResponseEntity<>(getPublicKeyAsPem(keyToReturn.getPublicKey()),
        headers, HttpStatus.OK);
}

private JsonWebKey selectKeyForPem(List<JsonWebKey> keys) {
    // Prefer APIML key when multiple keys are present (PAT + z/OSMF scenario)
    return keys.stream()
        .filter(k -> "APIML".equals(k.getKeyId()))
        .findFirst()
        .orElse(keys.get(0));
}
```

### Tests

**New tests in `AuthControllerTest`:**
- `givenEndpointCalled_thenDeprecationHeaderPresent()` — assert every response includes `Deprecation: true` and a `Link` header pointing to `/keys/public/current`.
- `givenBothApimlAndZosmfKeysAvailable_whenGetPublicKey_thenReturnApimlKey()` — mock both keys available, assert the APIML key is returned as PEM.
- `givenOnlyZosmfKeyAvailable_whenGetPublicKey_thenReturnZosmfKey()` — only z/OSMF active (no PAT), assert z/OSMF key is returned.
- `givenKeysPublicCurrentEndpoint_whenBothKeysAvailable_thenReturnBothInJwkSet()` — confirm the replacement endpoint returns a JWK Set with both keys.
