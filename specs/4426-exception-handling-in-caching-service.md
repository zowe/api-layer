# #4426 — Exception handling in Caching Service

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4426
**Labels:** bug, Priority: High | **Created:** 2025-12-12 | **State:** open

---

## Description

Every endpoint in `CachingController` duplicates the same pattern: a `try/catch(StorageException)` block calling a private `exceptionToResponse()` helper. There is no `@ControllerAdvice` / `@RestControllerAdvice`, meaning:

- Exception handling logic is repeated across all 11 endpoints.
- Any exception that is not a `StorageException` (e.g., an unexpected `NullPointerException` from the storage layer) produces an unformatted Spring default error response without the APIML message structure.
- The exception subclasses used within the storage layer (`KeyNotFoundException`, etc.) are not formally defined — all errors are represented as `StorageException` with only an `HttpStatus` field to distinguish them.

---

## Acceptance Criteria

- All `StorageException` subclasses thrown from the storage layer are handled in a single `@RestControllerAdvice` and produce a structured APIML JSON error body with the correct HTTP status code.
- Unexpected exceptions (anything not a `StorageException`) return HTTP 500 with a generic APIML error body — the internal exception message is logged but not exposed in the response.
- Spring's built-in exceptions (e.g., `MethodArgumentNotValidException` for invalid request bodies) continue to return 400 and are not swallowed by the generic `Exception` handler.
- All existing `CachingControllerTest` test cases pass without modification after the refactoring.
- Per-endpoint `try/catch` blocks in `CachingController` are removed; each method delegates directly to the storage layer.
- Exceptions from within `Mono.fromCallable()` (reactive pipeline) are correctly surfaced to the advice rather than being silently swallowed.

---

## Technical Solution

### Files to change

- New: `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingExceptionHandler.java`
- `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java` — remove try/catch blocks and `exceptionToResponse()` helper
- Optional: new exception subclasses in `common-service-core` (`KeyNotFoundException`, `KeyAlreadyExistsException`, `StorageUnavailableException`)

### Changes

**New `CachingExceptionHandler`**

```java
@RestControllerAdvice
public class CachingExceptionHandler {

    @Autowired
    private MessageService messageService;

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Object> handleStorageException(StorageException ex) {
        ApiMessage message = messageService.createMessage(ex.getKey(), (Object[]) ex.getParameters());
        return ResponseEntity.status(ex.getStatus()).body(message.mapToApiMessage());
    }

    @ExceptionHandler(Exception.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ResponseEntity<Object> handleGeneral(Exception ex) {
        log.error("Unexpected error in caching service", ex);
        ApiMessage message = messageService.createMessage(
            "org.zowe.apiml.common.internalServerError");
        return ResponseEntity.internalServerError().body(message.mapToApiMessage());
    }
}
```

The generic `Exception` handler must be ordered at `LOWEST_PRECEDENCE` so Spring's built-in handlers for `MethodArgumentNotValidException`, `MethodNotAllowedException`, etc. take priority.

**`CachingController` — remove boilerplate**

Each endpoint becomes a direct delegation:
```java
@GetMapping("/{key}")
public Mono<ResponseEntity<KeyValue>> getValue(@PathVariable String key,
                                               ServerWebExchange exchange) {
    String serviceId = getServiceId(exchange);
    return Mono.fromCallable(() -> storage.read(serviceId, key))
               .map(value -> ResponseEntity.ok(value));
    // StorageException propagates via Mono.error() and is caught by the advice
}
```

Confirm that `Mono.fromCallable()` surfaces checked exceptions correctly — if storage throws a `StorageException`, it must arrive as `Mono.error(storageException)`, not be swallowed.

**Exception subclasses (optional but recommended)**

```java
public class KeyNotFoundException extends StorageException {
    public KeyNotFoundException(String key) {
        super("org.zowe.apiml.caching.keyNotFound", HttpStatus.NOT_FOUND, key);
    }
}

public class KeyAlreadyExistsException extends StorageException {
    public KeyAlreadyExistsException(String key) {
        super("org.zowe.apiml.caching.keyAlreadyExists", HttpStatus.CONFLICT, key);
    }
}

public class StorageUnavailableException extends StorageException {
    public StorageUnavailableException(String detail) {
        super("org.zowe.apiml.caching.storageUnavailable", HttpStatus.SERVICE_UNAVAILABLE, detail);
    }
}
```

### Tests

**Acceptance criterion for refactoring:** All existing `CachingControllerTest` tests (614 lines) must pass without modification.

**New `CachingExceptionHandlerTest`** (using `@WebMvcTest` or `WebTestClient`):
- `givenStorageExceptionWith404Status_whenHandled_thenReturns404WithApimlBody()`.
- `givenStorageExceptionWith409Status_whenHandled_thenReturns409WithApimlBody()`.
- `givenUnexpectedException_whenHandled_thenReturns500WithGenericBody()` — assert the response body does not contain the internal exception's message.
- `givenMethodArgumentNotValidException_whenHandled_thenReturns400NotSwallowed()` — confirm Spring's default 400 handler runs before the generic catch-all.
