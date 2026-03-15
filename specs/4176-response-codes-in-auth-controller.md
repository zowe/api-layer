# #4176 — Response codes in AuthController (review of the code)

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4176
**Labels:** technical excellence, Priority: Medium | **Created:** 2025-06-19 | **State:** open

---

## Description

`AuthController` in zaas-service has several response code inconsistencies that violate RFC 9110 semantics or are misleading to clients:

- `POST /oidc-token/validate` returns **401** when OIDC is not configured — this is a server-side configuration issue, not a client authentication failure.
- `GET /keys/public` returns **500** when the JWT signing key is not yet initialised — this is a transient startup state, not a permanent server error.
- `DELETE /access-token/revoke` returns **401** when the token is already revoked — revocation is an idempotent operation and should return 204 on repeat calls.

---

## Acceptance Criteria

- `POST /oidc-token/validate` returns **503** (Service Unavailable) when OIDC is not configured, with an error body explaining that the OIDC provider is not available.
- `GET /keys/public` returns **503** with a `Retry-After` header (value from `apiml.startup.retryAfterSeconds`, default 10) when the JWT signing key is not yet initialised. Returns 200 once the key is available.
- `DELETE /access-token/revoke` returns **204** regardless of whether the token was already revoked — the operation is idempotent.
- `DELETE /access-token/revoke` also returns **204** for a token that was never issued (unknown token), not 401.
- The OpenAPI specification (`zaas-service/src/main/resources/apiml-api-info.yaml`) is updated to reflect the corrected response codes.
- All `AuthController`-related tests are updated to assert the new response codes.

---

## Technical Solution

### Files to change

- `zaas-service/src/main/java/org/zowe/apiml/zaas/controllers/AuthController.java`
- `zaas-service/src/main/resources/apiml-api-info.yaml`
- `zaas-service/src/test/java/org/zowe/apiml/zaas/controllers/AuthControllerTest.java`

### Changes

**`validateOIDCToken()` — 401 → 503:**

```java
if (oidcProvider == null || !oidcProvider.isConfigured()) {
    return new ResponseEntity<>(
        messageService.createMessage("org.zowe.apiml.zaas.oidc.notConfigured").mapToApiMessage(),
        HttpStatus.SERVICE_UNAVAILABLE);
}
```

**`getPublicKeyUsedForSigning()` — 500 → 503 + `Retry-After`:**

```java
if (publicKeys.isEmpty()) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Retry-After", String.valueOf(retryAfterSeconds));
    return new ResponseEntity<>(
        messageService.createMessage("org.zowe.apiml.zaas.keys.notReady").mapToApiMessage(),
        headers,
        HttpStatus.SERVICE_UNAVAILABLE);
}
```

Add `@Value("${apiml.startup.retryAfterSeconds:10}") int retryAfterSeconds` field.

**`revokeAccessToken()` — 401 → 204 for already-revoked tokens:**

```java
// Remove the pre-check that returns 401 if token is already invalidated.
// Simply revoke unconditionally and return 204.
tokenProvider.invalidateToken(token);
return new ResponseEntity<>(HttpStatus.NO_CONTENT);
```

### Tests

**New or updated tests in `AuthControllerTest`:**
- `givenOidcNotConfigured_whenValidateOidcToken_thenReturn503()` — assert 503 and error body with `org.zowe.apiml.zaas.oidc.notConfigured`.
- `givenJwtNotInitialized_whenGetPublicKey_thenReturn503WithRetryAfterHeader()` — assert 503, `Retry-After` header present with a numeric value.
- `givenTokenAlreadyRevoked_whenRevokeAgain_thenReturn204()` — assert 204 on second revoke.
- `givenUnknownToken_whenRevoke_thenReturn204()` — assert 204 when token was never issued.
- `givenOidcConfigured_whenValidateOidcToken_thenReturn204()` — existing happy path unchanged.
