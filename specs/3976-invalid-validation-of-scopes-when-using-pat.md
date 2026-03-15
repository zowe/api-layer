# #3976 — Invalid validation of scopes when using PAT (with ZSS)

**GitHub Issue:** https://github.com/zowe/api-layer/issues/3976
**Labels:** bug, Priority: High | **Created:** 2025-02-03 | **State:** open

---

## Description

PAT (Personal Access Token) tokens include a `scopes` claim listing the service IDs for which the token is valid. Services that validate tokens locally — by downloading the APIML public key via `GET /zaas/api/v1/auth/keys/public` and verifying the JWT signature — do not check the `scopes` claim. They only verify the signature and expiry.

ZSS is a known example. By accepting a PAT that is scoped to a different service, ZSS (and other services doing local validation) allow broader access than intended. A PAT issued for `service-a` is accepted by `service-b` if `service-b` does local validation.

Note: the fix for `GET /keys/public` (issue #4175) is a prerequisite — services must receive the correct APIML public key before they can validate PAT signatures.

---

## Acceptance Criteria

- A `PATValidator` utility class is provided in `apiml-security-common` that encapsulates both RSA signature verification and `scopes` claim checking.
- A PAT token without a `scopes` claim is always rejected, even if the signature is valid.
- A PAT token with `scopes` that does not include the validating service's own `serviceId` is rejected.
- `PATValidator` is documented as the required validation mechanism for any service performing local JWT validation.
- A cross-repo issue is filed in `zowe/zss` to adopt `PATValidator` or implement equivalent scope checking.
- `PATAuthSourceService.isValid()` in zaas-service continues to work correctly (it already calls `isValidForScopes()` — this should be confirmed and tested).

---

## Technical Solution

### Files to change

- New: `apiml-security-common/src/main/java/org/zowe/apiml/security/common/token/PATValidator.java`
- New: `apiml-security-common/src/test/java/org/zowe/apiml/security/common/token/PATValidatorTest.java`
- `zaas-client/src/main/java/org/zowe/apiml/zaasclient/service/internal/ZaasClientImpl.java` — document that PAT validation must include scope checking
- `docs/` — update security architecture docs

### Changes

**New `PATValidator`**

```java
public class PATValidator {

    private final PublicKey signingKey;
    private final String localServiceId;

    /**
     * Validates a PAT JWT token for use by this service.
     * Checks: signature, expiry, scopes claim.
     *
     * @throws TokenNotValidException if the token is invalid for any reason.
     */
    public void validate(String jwtToken) {
        // 1. Verify RSA signature using signingKey
        JWTClaimsSet claims = verifySignature(jwtToken); // throws if invalid

        // 2. Check expiry
        if (claims.getExpirationTime().before(new Date())) {
            throw new TokenNotValidException("Token has expired");
        }

        // 3. Require scopes claim
        List<String> scopes = claims.getStringListClaim("scopes");
        if (scopes == null || scopes.isEmpty()) {
            throw new TokenNotValidException("PAT token must contain a scopes claim");
        }

        // 4. Check that this service's ID is in the scopes
        if (!scopes.contains(localServiceId.toLowerCase())) {
            throw new TokenNotValidException(
                "PAT token is not valid for service: " + localServiceId);
        }
    }
}
```

### Tests

**New `PATValidatorTest`:**
- `givenTokenWithMatchingScope_whenValidate_thenNoException()`.
- `givenTokenWithNoMatchingScope_whenValidate_thenThrowTokenNotValidException()`.
- `givenTokenWithNoScopesClaim_whenValidate_thenThrowTokenNotValidException()`.
- `givenExpiredToken_whenValidate_thenThrowTokenNotValidException()`.
- `givenTamperedSignature_whenValidate_thenThrowException()` — modify one character of the JWT signature and assert validation fails.

**Confirm existing `PATAuthSourceServiceTest`:**
- `givenValidScopeButInvalidatedToken_whenIsValid_thenReturnFalse()`.
- `givenInvalidScopeButNotInvalidated_whenIsValid_thenReturnFalse()`.
- `givenBothValidScopeAndNotInvalidated_whenIsValid_thenReturnTrue()`.
