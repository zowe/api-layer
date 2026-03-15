# #4058 — Fix using Authentication object for changing password

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4058
**Labels:** technical excellence, Priority: Low | **Created:** 2025-04-08 | **State:** open

---

## Description

`LoginFilter.doAuth()` constructs a `UsernamePasswordAuthenticationToken` with the entire `LoginRequest` object as the credentials:

```java
new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest)
```

This couples all `AuthenticationProvider` implementations to the APIML-internal `LoginRequest` class — providers that need to access `newPassword` (for expiry-triggered password changes) must cast `authentication.getCredentials()` to `LoginRequest`. This violates the Spring Security contract, where `credentials` is expected to be the raw password or a credential type that does not carry request-level metadata.

---

## Acceptance Criteria

- `UsernamePasswordAuthenticationToken` is constructed with a minimal `LoginCredentials` object as credentials, not the full `LoginRequest`.
- `LoginCredentials` contains only `password` (char[]) and `newPassword` (char[]) — no request-metadata fields.
- All `AuthenticationProvider` implementations that extract credentials are updated to cast to `LoginCredentials` instead of `LoginRequest`.
- Zeroing `loginRequest.evictSensitiveData()` in the `finally` block also zeroes the arrays held in the `LoginCredentials` object (shared array references, not copies).
- After authentication completes, the credentials field in the `Authentication` object contains zeroed arrays or is `null`.
- No new `LoginRequest` casts appear anywhere in the `AuthenticationProvider` chain.

---

## Technical Solution

### Files to change

- New: `apiml-security-common/src/main/java/org/zowe/apiml/security/common/login/LoginCredentials.java`
- `apiml-security-common/src/main/java/org/zowe/apiml/security/common/login/LoginFilter.java`
- All `AuthenticationProvider` implementations that cast `authentication.getCredentials()` to `LoginRequest` (search entire codebase for `.getCredentials()` cast sites)

### Changes

**New `LoginCredentials`**

```java
// Use a record to avoid accidental mutation — but hold mutable char[] by shared reference
public record LoginCredentials(char[] password, char[] newPassword) {
    // Note: char[] arrays are mutable; zeroing the LoginRequest arrays
    // also zeroes these via the shared reference.
}
```

**`LoginFilter.doAuth()`**

```java
UsernamePasswordAuthenticationToken authentication =
    new UsernamePasswordAuthenticationToken(
        loginRequest.getUsername(),
        new LoginCredentials(loginRequest.getPassword(), loginRequest.getNewPassword())
    );
```

The `finally` block `loginRequest.evictSensitiveData()` already zeroes `loginRequest.getPassword()`. Because `LoginCredentials` holds the same `char[]` reference (not a copy), the zeroing propagates automatically.

**`AuthenticationProvider` updates**

Replace:
```java
LoginRequest credentials = (LoginRequest) authentication.getCredentials();
char[] password = credentials.getPassword();
char[] newPassword = credentials.getNewPassword();
```

With:
```java
LoginCredentials credentials = (LoginCredentials) authentication.getCredentials();
char[] password = credentials.password();
char[] newPassword = credentials.newPassword();
```

### Tests

**New `LoginCredentialsTest`:**
- `givenSharedCharArray_whenZeroed_thenCredentialsPasswordAlsoZeroed()` — create `LoginCredentials` with a `char[]`, zero the array, assert `credentials.password()` contains only `\0` characters (confirming shared reference, not a copy).

**Update `LoginFilterTest`** (or create if missing):
- `givenLoginRequestWithNewPassword_whenDoAuth_thenCredentialsContainNewPassword()` — capture the `Authentication` passed to `authManager.authenticate()` and assert `((LoginCredentials) auth.getCredentials()).newPassword()` is non-null.
- `givenAuthenticationComplete_whenCredentialsInspected_thenPasswordArrayIsZeroed()` — after `doAuth()` returns, assert the `char[]` held in credentials contains only `\0`.

**Update affected `AuthenticationProvider` tests:**
For each provider, change mock credential setup from `LoginRequest` to `LoginCredentials` and assert correct password extraction.
