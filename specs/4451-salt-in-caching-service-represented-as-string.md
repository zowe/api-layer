# #4451 — Salt in caching service is represented as String even it contains binary data

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4451
**Labels:** bug, Priority: High | **Created:** 2026-01-09 | **State:** open

---

## Description

`ApimlAccessTokenProvider` (`zaas-service`) generates a 16-byte cryptographically random salt to create a SHA-512 hash for storing PAT-related data. The salt is stored in the Caching service as a plain `String` using `new String(byte[])` and retrieved via `String.getBytes()`.

`new String(byte[])` interprets the bytes using the platform default charset (typically UTF-8). If any byte value falls in a range that represents a control character or forms part of a multi-byte sequence, the resulting string is shorter than 16 characters. When the string is later converted back to bytes with `getBytes()`, the original byte array is not recovered — the salt is corrupted. This silently weakens the security of all PAT hashes derived from that salt.

This also creates a compatibility risk between instances running on JVMs with different default charsets.

---

## Acceptance Criteria

- The salt is stored in the Caching service as a Base64-encoded string and decoded back to exactly 16 bytes on retrieval, regardless of byte content.
- A salt containing control characters (bytes 0x00–0x1F) survives a store-and-retrieve round-trip as an identical 16-byte array.
- On first startup after the fix, if the stored salt is not valid Base64 or decodes to a length other than 16 bytes, a new salt is generated and a `WARN` log message is emitted stating that existing PAT hashes are invalidated.
- `getSalt()` returns a `byte[]` without any intermediate `String` representation.
- The `@BeforeEach` in `ApimlAccessTokenProviderTest` uses a Base64-encoded salt string in its mock setup.

---

## Technical Solution

### Files to change

- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java`
- `zaas-service/src/test/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProviderTest.java`

### Changes

**`storeSalt(byte[] salt)`**

```java
private void storeSalt(byte[] salt) throws CachingServiceClientException {
    cachingServiceClient.create(new CachingServiceClient.KeyValue(
        "salt", Base64.getEncoder().encodeToString(salt)));
}
```

**`initializeSalt()`**

```java
String initializeSalt() throws CachingServiceClientException {
    try {
        CachingServiceClient.KeyValue keyValue = cachingServiceClient.read("salt");
        String stored = keyValue.getValue();
        // Migration: validate that the stored value is proper Base64 of 16 bytes
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(stored);
        } catch (IllegalArgumentException e) {
            decoded = null;
        }
        if (decoded == null || decoded.length != 16) {
            log.warn("Stored salt is invalid or in legacy format; generating a new salt. " +
                     "All existing PAT hashes are now invalid — users must re-authenticate.");
            byte[] newSalt = generateSalt();
            storeSalt(newSalt);
            return Base64.getEncoder().encodeToString(newSalt);
        }
        return stored;
    } catch (CachingServiceClientException e) {
        if (e.getCause() != null) throw e; // network/timeout — propagate
        // 404 not found — generate new salt
        byte[] newSalt = generateSalt();
        storeSalt(newSalt);
        return Base64.getEncoder().encodeToString(newSalt);
    }
}
```

**`getSalt()`**

```java
public byte[] getSalt() throws CachingServiceClientException {
    return Base64.getDecoder().decode(initializeSalt());
}
```

### Tests

**Update `ApimlAccessTokenProviderTest.@BeforeEach`:**
```java
when(cachingServiceClient.read("salt")).thenReturn(
    new CachingServiceClient.KeyValue("salt",
        Base64.getEncoder().encodeToString(ApimlAccessTokenProvider.generateSalt())));
```

**Update `givenNoSaltInCache_whenInitializing_thenCreateNewOne()`:**
Add verification that `cachingServiceClient.create()` is called with a value that is valid Base64 and decodes to exactly 16 bytes:
```java
verify(cachingServiceClient).create(argThat(kv ->
    Base64.getDecoder().decode(kv.getValue()).length == 16));
```

**New test `givenSaltWithControlBytes_whenStored_thenRoundTripIsLossless()`:**
```java
byte[] original = new byte[16];
for (int i = 0; i < 6; i++) original[i] = (byte) i; // control chars 0x00–0x05
// mock create to capture the stored value, mock read to return it
// assert Arrays.equals(original, getSalt())
```

**New test `givenLegacySaltFormat_whenInitializing_thenRegeneratesAndLogsWarn()`:**
Mock `cachingServiceClient.read("salt")` to return a raw-byte string that is not valid Base64 (e.g., `"\u0001\u0002"`). Assert a new salt is generated and stored, and a `WARN`-level log message is emitted (use Logback `ListAppender`).

**New test `givenBase64SaltInCache_whenGetSalt_thenReturns16Bytes()`:**
End-to-end: call `getSalt()` with a properly encoded mock, assert the returned array has length 16.
