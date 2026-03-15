# Commit Review

Critical analysis of the 26 local fix branches. Each entry records the branch, a summary
of what was changed, issues found, and — where the issue requires a code fix — the exact
location and the corrective change needed.

Severity scale: **BLOCKER** (must fix before merge), **MAJOR** (functionally incomplete or
incorrect, should fix), **MINOR** (style, completeness, or polish concern).

---

## fix/3841-missing-reason-action-error-codes

**Change:** Added `reason` and `action` fields to ZWEAO404, ZWEAO405, ZWEAO415 in
`apiml-common/src/main/resources/common-log-messages.yml`.

**Issues:**

1. **MINOR — No test coverage added.** The commit message says "Add a test that loads the
   YAML and asserts all three message keys have non-blank reason and action fields." No test
   was added in this commit. A regression that removes the new fields will go undetected.

   **Fix needed:** Add a unit test in `apiml-common/src/test/java/...` that loads
   `common-log-messages.yml`, finds entries with numbers ZWEAO404, ZWEAO405, ZWEAO415, and
   asserts `reason` and `action` are non-blank strings.

---

## fix/4159-auth-query-swagger-summary

**Change:** Updated the `summary` and `operationId` fields in `zaas-api-doc.json` for
`/zaas/api/v1/auth/query`.

**Issues:**

1. **MAJOR — `operationId` rename is a breaking API change.** The `operationId` was changed
   from `"validateUsingGET"` to `"queryUsingGET"`. Any client that generates code from the
   OpenAPI spec and references this method by its operationId will break (e.g., SDK
   generated classes, test utilities referencing the operation by name). The commit message
   does not call out this breaking change.

   **Fix needed:** Either revert the `operationId` rename (it is not necessary to fix #4159
   — only the `summary` text matters for the display issue), or explicitly document it as a
   breaking change in the release notes. The safest path:

   ```json
   // Keep original operationId, only change summary:
   "operationId": "validateUsingGET",
   "summary": "Validate the authentication token and retrieve the associated user information.",
   ```

2. **MINOR — No test guards the summary text.** A test was recommended but not added.

---

## fix/4286-zweag510-include-status-code

**Change:** Added `%s` placeholder to ZWEAG510 message text; passes `ex.getStatusCode().value()`
as the format argument in `GatewayExceptionHandler.handleStatusError()`.

**Issues:**

1. **MAJOR — The status code is now embedded twice in the response.** The HTTP response
   already carries the status code in the HTTP status line. The ZWEAG510E message body now
   also includes it via `%s`. However, `setBodyResponse()` is called with
   `ex.getStatusCode().value()` as *both* the HTTP status code parameter *and* the message
   format argument. This is intentional for the body, but the resulting error JSON message
   will read:

   ```json
   "messageContent": "Request to the resource ended with unexpected status code 503."
   ```

   While the HTTP response code is also 503. This is redundant but not wrong. The concern
   is readability: if the HTTP layer changes the status code (e.g., a reverse proxy rewrites
   it), the message body and the actual response code will disagree.

   **No immediate code change required** — document the accepted redundancy.

2. **MAJOR — The `%s` placeholder changes the message format contract, potentially breaking
   existing log parsers.** The old text had no format parameters; the new text has one. Any
   monitoring system or log analysis tool that matches on the exact string
   `"Request to the resource ended with unexpected status code."` will no longer match.

   **Fix needed:** Add a `reason` + `action` note in the commit message or release notes
   warning that the message text has changed. No code change — documentation only.

3. **MINOR — Number formatting inconsistency.** `ex.getStatusCode().value()` returns an
   `int`, so `%s` will format it without any padding or padding-specifier. This is correct
   for readability. No change needed.

---

## fix/2991-bom-missing-dependencies

**Change:** Added `from components.javaPlatform` to `platform/build.gradle` so the BOM
publishes its `<dependencyManagement>` section.

**Issues:**

1. **MINOR — No verification step documented or added to CI.** The fix was manually
   verified by running `generatePomFileForMavenJavaPublication` and checking the output
   POM, but no automated test or CI step validates that the BOM POM is non-empty. A future
   refactor could accidentally reintroduce the bug undetected.

   **Fix needed:** Add a Gradle task or test that asserts the generated POM contains
   `<dependencyManagement>`. This can be a simple Gradle `test` task that reads the
   generated POM file and fails if the section is absent.

---

## fix/4362-idle-connection-eviction-interval

**Change:** Changed the hardcoded `30000, 30000` timer schedule arguments to
`evictionIntervalMs, evictionIntervalMs` where `evictionIntervalMs = idleConnTimeoutSeconds * 1000L`.

**Issues:**

1. **BLOCKER — If `idleConnTimeoutSeconds` is zero or negative, the timer fires
   continuously.** `idleConnTimeoutSeconds` is a `@Value`-injected int with no documented
   minimum. If a misconfiguration sets it to `0`, `evictionIntervalMs` becomes `0` and
   `Timer.schedule(task, 0, 0)` throws `IllegalArgumentException: Non-positive period`.
   If it is `-1`, the period is negative and the same exception fires, crashing the context.

   **Fix needed:**
   ```java
   // HttpConfig.java — before the schedule call:
   long evictionIntervalMs = Math.max(idleConnTimeoutSeconds * 1000L, 1000L);
   ```
   This ensures the timer always fires at least once per second even when `idleConnTimeoutSeconds`
   is zero or negative, preventing the `IllegalArgumentException`.

2. **MINOR — When `idleConnTimeoutSeconds` is very small (e.g., 1 s), the eviction timer
   fires every 1 second.** This is correct behaviour per the fix intent but may cause
   unexpected CPU overhead for deployments that set very short idle timeouts for other
   reasons. Document the changed default in upgrade notes.

---

## fix/4283-authorize-button-swagger

**Change:** Added `supportedSubmitMethods: []` to both `swaggerProps` `setState` calls in
`SwaggerUIApiml.jsx`; removed the brittle `nth-child` CSS rule from `Swagger.css`.

**Issues:**

1. **MAJOR — `supportedSubmitMethods: []` disables "Try it out" for all endpoints, not
   just the Authorize button.** The `supportedSubmitMethods` SwaggerUI prop controls which
   HTTP methods the "Try it out" execution feature is enabled for. An empty array disables
   *all* in-browser request execution, not just the Authorize dialog. The commit message
   states "prevents the Authorize button from rendering" but this is not entirely accurate —
   it also removes the "Try it out" button from every endpoint.

   If users rely on "Try it out" for exploring APIs (even though it uses session tokens),
   this is a regression. The commit message does not mention this side effect.

   **Fix needed:** Either:
   - Accept the full "Try it out" disablement as intentional (update commit message/docs), or
   - Instead of `supportedSubmitMethods: []`, explicitly hide only the `.auth-wrapper` button
     via a more targeted approach, preserving "Try it out" functionality.

   If the intent is to keep "Try it out" working, change to:
   ```jsx
   // This disables the Authorize button but keeps Try it out working:
   // Do NOT use supportedSubmitMethods: []
   // Instead, use a custom plugin that removes the AuthBtn component:
   plugins: [this.customPlugins, AdvancedFilterPlugin, CustomizedSnippedGenerator(codeSnippets), DisableAuthorizePlugin],
   ```
   Where `DisableAuthorizePlugin` wraps the `authorizeBtn` component with a no-op render.

2. **MINOR — No frontend unit test added** to assert `supportedSubmitMethods: []` is present
   in the props, as recommended in the original plan.

---

## fix/4512-connect-exception-cause-chain

**Change:** Replaced `e.getCause() instanceof ConnectException` with `isConnectionRefused(e)`
which walks the full cause chain.

**Issues:**

1. **MAJOR — The `isConnectionRefused` method can enter an infinite loop on circular cause
   chains.** If a malicious or buggy exception class has a circular `getCause()` chain
   (e.g., `e.getCause() == e`), the while loop will spin forever, hanging the reactor
   thread.

   **Fix needed:** Add a depth limit or visited-set check:
   ```java
   private boolean isConnectionRefused(Throwable t) {
       Throwable current = t;
       int depth = 0;
       while (current != null && depth++ < 20) {
           if (current instanceof ConnectException) return true;
           current = current.getCause();
       }
       return false;
   }
   ```
   A depth of 20 is generous — real exception chains rarely exceed 5 levels.

2. **MINOR — No test for doubly-wrapped `ConnectException`.** The fix plan called for a
   test with a `ConnectException` nested two levels deep, asserting HTTP 503. No such test
   was added.

---

## fix/3131-zaas-shutdown-on-jwt-failure

**Change:** Injected `ConfigurableApplicationContext` into `JwtSecurity`; replaced
`System.exit(1)` and the no-op timer path with `applicationContext.close()`.

**Issues:**

1. **MAJOR — `applicationContext.close()` is called from a `Timer` thread, which is not a
   Spring-managed thread.** Spring's `ConfigurableApplicationContext.close()` is designed
   to be called from any thread, but it fires `ContextClosedEvent` which may be handled by
   listeners that assume they run on the Spring main thread. More critically, calling
   `close()` inside a `TimerTask.run()` will block the timer thread until the context is
   fully closed. If any shutdown hook waits for the timer thread (e.g., to cancel pending
   tasks), a deadlock can occur.

   **Fix needed:** Use `SpringApplication.exit()` or schedule the close on a separate
   thread to avoid blocking the timer:
   ```java
   if (applicationContext != null) {
       new Thread(() -> applicationContext.close(), "zaas-shutdown").start();
   }
   ```

2. **MAJOR — The `@VisibleForTesting` constructor passes `null` for the context, so the
   shutdown path is silently skipped in tests.** This means the new behavior (graceful
   shutdown) is never exercised in tests — a test can pass with `applicationContext = null`
   even if the shutdown path is completely broken.

   **Fix needed:** Update `JwtSecurityTest` to use a non-null mock `ConfigurableApplicationContext`
   in the `@VisibleForTesting` constructor, and assert that `applicationContext.close()` is
   called on the expected failure paths.

3. **MINOR — Duplicate log call before context close in the timer path.** In the timer
   path, `apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", ...)` is called, then
   `apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", ...)`, then
   `applicationContext.close()`. The two log calls before shutdown are fine, but the second
   log message ("z/OSMF instance not found") is misleading — the timer fires only when
   z/OSMF was *never* found within the timeout window; it is not an "instance not found"
   event mid-run.

---

## fix/4350-serviceid-validator-consistency

**Change:** Replaced the local `Pattern.compile("^[A-Za-z][A-Za-z0-9-]*$")` with
`EurekaUtils.SERVICE_ID_PATTERN`; updated the `serviceIdIsValid` method to use `.matches()`
instead of `.find()`; updated the error message.

**Issues:**

1. **MAJOR — The import `java.util.regex.Pattern` is still present as a fully qualified
   reference** in the field declaration: `private static final java.util.regex.Pattern p =
   EurekaUtils.SERVICE_ID_PATTERN;`. The removed `import java.util.regex.Pattern;` statement
   means the code now uses the FQCN inline. This compiles correctly but is inconsistent
   style — the rest of the file uses standard imports. More importantly, the diff shows
   `import java.util.regex.Pattern` was removed from the import section, so the FQCN inline
   is required. This is an intentional, if unusual, choice.

   **Fix needed:** Either restore the import and use the short form, or leave as FQCN. The
   FQCN form is correct but uncommon. Prefer the import form:
   ```java
   import java.util.regex.Pattern;
   // ...
   private static final Pattern p = EurekaUtils.SERVICE_ID_PATTERN;
   ```

2. **MAJOR — Silent behaviour change for services with serviceIds of length 1.** The old
   pattern (`^[A-Za-z][A-Za-z0-9-]*$`) accepted single-character IDs like `"a"`. The new
   `EurekaUtils.SERVICE_ID_PATTERN` (`^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$`) requires
   *at least 2 characters*. Any existing static API definition with a single-character
   serviceId that was previously accepted will now be rejected with HTTP 400.

   The old validator also had an arbitrary 16-character limit that is now removed (max is
   now 63). This is an improvement, but the minimum length change (from 1 to 2) is
   undocumented in the commit message and could break existing deployments.

   **Fix needed:** Add to the commit message / release notes that single-character serviceIds
   are no longer accepted. No code change required unless backward compatibility is needed.

3. **MINOR — No new test for the now-rejected single-character case.** The plan called for
   a test asserting `"a"` is rejected. No test was added.

---

## fix/3097-saf-error-detail-in-response

**Change:** Replaced `ex.getMessage()` with `ex.getPlatformError().shortErrorName` in both
`AuthExceptionHandler.handleZosAuthenticationException()` and
`ApimlExceptionHandler.handleZosAuthenticationException()`.

**Issues:**

1. **MAJOR — `ex.getPlatformError().shortErrorName` may be null.** `PlatformPwdErrno` is
   an enum; `shortErrorName` is a field on it. If `getPlatformError()` returns a value whose
   `shortErrorName` field is `null` (possible for custom or unknown errno values), then
   `messageService.createMessage(key, null)` is called. Depending on the message template's
   `%s` handling, this may produce the literal string `"null"` in the response body, which
   is confusing.

   **Fix needed:**
   ```java
   String safeShortName = Optional.ofNullable(ex.getPlatformError().shortErrorName)
       .orElse("UNKNOWN");
   final ApiMessageView message = messageService.createMessage(
       ex.getPlatformError().errorMessage, safeShortName).mapToView();
   ```

2. **MINOR — The log message prefix changed from `"Zos Authentication Exception: {}"` to
   `"Authentication failed with platform error: {}"`.** The new wording is better, but it
   changes log grep patterns for monitoring tools. Not a code defect.

---

## fix/3007-auth-failure-header-leak

**Change:** Replaced `ex.getMessage()` in `handleNoMainframeIdentity()` with a resolved
message-service string; replaced the raw `CertificateEncodingException` message in
`AbstractAuthSchemeFactory` with a fixed generic string.

**Issues:**

1. **MAJOR — The `X-Zowe-Auth-Error-Code` header is set on the ZAAS response (in a
   different branch, fix/3976) but the generic message placed in `X-Zowe-Auth-Failure` for
   `CertificateEncodingException` is now a hardcoded string literal** `"The client
   certificate in the request is invalid."` rather than a message-service-resolved string.
   This means the message is not translatable, not governed by the message key system, and
   not easily changed without a code deployment.

   **Fix needed:** Add a message key for the certificate encoding failure and resolve it
   via `messageService`:
   ```java
   // In AbstractAuthSchemeFactory, after catching CertificateEncodingException:
   log.debug("Invalid client certificate in request: {}", e.getMessage());
   exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER,
       messageService.createMessage("org.zowe.apiml.security.common.invalidCertificate").mapToLogMessage());
   ```
   Add the message key `org.zowe.apiml.security.common.invalidCertificate` to the
   appropriate YAML.

2. **MINOR — `handleNoMainframeIdentity` now calls `messageService.createMessage(key,
   requestUri).mapToLogMessage()`.** The `requestUri` is included as a format argument.
   This means the URI (which may contain query parameters or path segments with user-
   controlled content) is embedded in the header value. Query parameters could contain
   characters that are illegal in HTTP header values (e.g., newlines), enabling header
   injection. The header value should be sanitized or the URI should not be included.

   **Fix needed:**
   ```java
   addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER,
       this.messageService.createMessage(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey()).mapToLogMessage());
   // Do not pass requestUri as a format argument into a response header value
   ```

---

## fix/4273-passticket-error-userid-applid

**Change:** Added `userId` and `applId` fields to `IRRPassTicketGenerationException`;
updated the message template in `core-log-messages.yml`; updated `ZaasExceptionHandler`
and `PassTicketService` throw sites.

**Issues:**

1. **MAJOR — Only two of the throw sites in `PassTicketService` were updated.** The diff
   shows two `throw new IRRPassTicketGenerationException(ErrorCode..., userId, applId)` calls
   updated in `PassTicketService.DefaultImplementation.generate()`. However,
   `IRRPassTicketGenerationException` is also thrown in the native z/OS IRR passticket
   generation code path (the real `generate()` implementations in z/OS-specific classes).
   Those throw sites were not changed — they will continue using the two-arg backward-compat
   constructors, so `getUserId()` and `getApplId()` will return `null`, and the error
   message will show `"unknown"` for both fields even though the userId and applId were
   available at the throw site.

   **Fix needed:** Search all throw sites of `IRRPassTicketGenerationException` across the
   codebase and update them to pass `userId` and `applId` where available:
   ```bash
   grep -rn "new IRRPassTicketGenerationException" --include="*.java" /home/balda/api-layer
   ```
   Update each remaining site.

2. **MAJOR — `log.error(ex.getMessage())` in `ZaasExceptionHandler.handlePassTicketException()`
   still logs only the SAF return codes.** The exception message from `getMessage()` returns
   something like `"Error on generation of PassTicket: safRc=8, racfRc=8, racfRsn=16"`.
   The userId/applId are now in the response body but are NOT in the log. Operators reading
   logs still cannot identify which user/application triggered the failure without checking
   the response.

   **Fix needed:**
   ```java
   log.error("PassTicket generation failed for user '{}', application '{}': {}",
       ex.getUserId(), ex.getApplId(), ex.getMessage());
   ```

3. **MINOR — `ZaasExceptionHandler` uses `"unknown"` as a null fallback for `userId` and
   `applId`.** The string `"unknown"` could be a valid userId on some systems. Use a more
   distinctive placeholder or omit the field entirely when null:
   ```java
   ex.getUserId() != null ? ex.getUserId() : "<not available>"
   ```

---

## fix/4163-x509-cert-not-mapped-message

**Change:** Created `CertificateNotMappedException`; updated `X509AuthenticationProvider`
to throw it; added handler in `AuthExceptionHandler`; added ZWEAG172 message to
`zaas-log-messages.yml`.

**Issues:**

1. **MAJOR — `CertificateNotMappedException` is in `apiml-security-common` but
   `X509AuthenticationProvider` is in `zaas-service`.** The `zaas-service` module already
   depends on `apiml-security-common`, so the compile dependency is satisfied. However,
   `CertificateNotMappedException` is placed in `org.zowe.apiml.security.common.error`, a
   package that has many other exceptions. The exception is ZAAS-specific (x509 mapping is
   a ZAAS concern) but lives in the common module. This creates a subtle coupling.

   **No immediate code change required** — the placement is defensible since
   `AuthExceptionHandler` (which handles it) is also in `apiml-security-common`. Document
   the coupling decision.

2. **MAJOR — The Spring Security `AuthenticationException` subtype can be caught by other
   generic handlers.** `CertificateNotMappedException extends AuthenticationException`.
   The `AuthExceptionHandler.exceptionHandlers` map uses the most specific match first. The
   entry for `CertificateNotMappedException` is added *before* `InvalidCertificateException`
   in the map, which is correct for specificity. However, the catch-all `AuthenticationException`
   handler at the end of the map will still handle it if `CertificateNotMappedException` is
   not registered. Since it IS registered, this is fine — but verify no other filter in the
   chain catches `AuthenticationException` before `AuthExceptionHandler` runs and swallows it.

   **Action needed:** Add a unit test that confirms `CertificateNotMappedException` is not
   caught and re-thrown as a different type by an earlier filter in the ZAAS security chain.

3. **MINOR — `CertificateNotMappedException` has no `serialVersionUID`.** It extends
   `AuthenticationException` which is `Serializable`. Adding a `serialVersionUID` prevents
   spurious serialization warnings:
   ```java
   private static final long serialVersionUID = 1L;
   ```

---

## fix/4451-salt-base64-encoding

**Change:** Changed `storeSalt` to Base64-encode the salt; changed `getSalt` to Base64-
decode and added a migration guard for legacy raw-string salts.

**Issues:**

1. **BLOCKER — The `delete("salt")` call in the migration guard is swallowed silently.** In
   `getSalt()`, the migration path does:
   ```java
   try {
       cachingServiceClient.delete("salt");
   } catch (Exception ignored) { }
   storeSalt(newSalt);
   ```
   If `delete` fails (e.g., due to a network error) and then `storeSalt` is called, the
   caching service will try to `create` a key that already exists, which will throw a
   conflict exception. The `storeSalt` method calls `cachingServiceClient.create(...)` —
   there is no `update` fallback. The overall result is that the migration path can fail
   silently, leaving the legacy salt in place, and the next call to `getSalt()` will hit
   the migration path again, potentially generating a different new salt each time — leading
   to HMAC hash divergence between calls.

   **Fix needed:** Use `update` instead of `delete`+`create` in the migration path, or
   catch the conflict from `storeSalt` and fall back to `update`:
   ```java
   // In getSalt() migration path:
   try {
       cachingServiceClient.update(new CachingServiceClient.KeyValue("salt",
           Base64.getEncoder().encodeToString(newSalt)));
   } catch (CachingServiceClientException e) {
       // If update also fails, try create
       cachingServiceClient.create(new CachingServiceClient.KeyValue("salt",
           Base64.getEncoder().encodeToString(newSalt)));
   }
   return newSalt;
   ```

2. **MAJOR — `generateSalt()` is called in two places in `getSalt()`: once in the normal
   `initializeSalt()` path and once in the migration path.** The salt from the migration
   path is returned as a raw `byte[]` directly (`return newSalt;`) rather than being
   re-read as a Base64 string. This means the return types of the two code paths differ:
   `initializeSalt()` returns a Base64 string, but the migration path returns the raw bytes
   and bypasses `initializeSalt()`. This is correct behaviour (the raw bytes are returned
   directly), but the code structure makes it confusing to follow.

   **Fix needed:** Add a comment clarifying the two return paths and that the migration
   path returns raw bytes directly:
   ```java
   // Migration path returns raw bytes directly — no need to decode Base64
   return newSalt;
   ```

3. **MINOR — The breaking migration is documented in the commit message but not in the
   user-visible YAML messages or a dedicated migration guide.** Add a runtime log message
   when the migration fires, so operators know their PATs have been invalidated:
   ```java
   log.warn("PAT salt migrated from legacy format to Base64. " +
       "All previously issued Personal Access Tokens are now invalid.");
   ```

---

## fix/4421-salt-init-atomic

**Change:** Added double-checked locking with `synchronized(ApimlAccessTokenProvider.class)`
and conflict-handling for the cluster-node race case.

**Issues:**

1. **MAJOR — The `synchronized` block catches `CachingServiceClientException` from
   `cachingServiceClient.read("salt")` inside the lock and falls through to salt generation,
   but the outer try-catch already handles real I/O errors by re-throwing them.** The inner
   catch `catch (CachingServiceClientException | StorageException ignored)` inside the sync
   block suppresses *all* exceptions, including I/O failures. This means a timeout while
   checking "still not present?" inside the lock will silently generate a new salt instead
   of propagating the error.

   **Fix needed:** Only suppress "not found" exceptions, propagate real I/O errors:
   ```java
   try {
       CachingServiceClient.KeyValue keyValue = cachingServiceClient.read("salt");
       return keyValue.getValue();
   } catch (StorageException ignored) {
       // key not found — proceed to create
   } catch (CachingServiceClientException e) {
       if (e.getCause() != null) throw e;  // real I/O error
       // else: not found, proceed
   }
   ```

2. **MAJOR — The conflict fallback `cachingServiceClient.read("salt").getValue()` can throw
   if the peer's newly stored salt is immediately evicted or if a second I/O error occurs.**
   The call is made without any error handling. If it throws, the exception propagates out
   of the `synchronized` block, which is correct, but the error message will be confusing
   ("failed to read peer salt after conflict").

   **Fix needed:** Wrap the fallback read with a descriptive exception:
   ```java
   } catch (CachingServiceClientException conflict) {
       log.debug("Salt was stored by a peer node concurrently, reading peer salt.");
       try {
           return cachingServiceClient.read("salt").getValue();
       } catch (CachingServiceClientException e) {
           throw new SecureTokenInitializationException(
               "Salt was stored by a peer but could not be read back", e);
       }
   }
   ```

3. **MINOR — Class-level lock (`synchronized(ApimlAccessTokenProvider.class)`) is very
   broad.** It prevents any other method that synchronizes on the same class from running
   concurrently with salt initialization. A dedicated `static final Object SALT_LOCK = new
   Object()` would be narrower and safer:
   ```java
   private static final Object SALT_LOCK = new Object();
   // ...
   synchronized (SALT_LOCK) { ... }
   ```

---

## fix/4418-remove-catalog-tile-wizard

**Change:** Removed tile metadata from `EurekaInstanceConfigCreator`; removed tile warning
from `EurekaInstanceConfigValidator`; removed tile form sections from both wizard JSX files.

**Issues:**

1. **MAJOR — The constants `CATALOG_ID`, `CATALOG_VERSION`, `CATALOG_TITLE`,
   `CATALOG_DESCRIPTION` are still defined in `EurekaInstanceConfigCreator` but are now
   unused.** The `// fill tile metadata` block was replaced with a comment, but the four
   constants were not deleted. Unused constants are dead code.

   **Fix needed:** Remove the four constant declarations from `EurekaInstanceConfigCreator`:
   ```java
   // Delete these lines:
   private static final String CATALOG_ID = "apiml.catalog.tile.id";
   private static final String CATALOG_VERSION = "apiml.catalog.tile.version";
   private static final String CATALOG_TITLE = "apiml.catalog.tile.title";
   private static final String CATALOG_DESCRIPTION = "apiml.catalog.tile.description";
   ```

2. **MAJOR — Existing services that pass `catalog.tile.*` metadata at registration will
   have it silently ignored.** The `Catalog` model class and the `getCatalog()` accessor
   on `ApiMediationServiceConfig` are retained for backward compatibility, but the
   `EurekaInstanceConfigCreator` now ignores the tile data entirely. There is no warning
   or log message to tell operators that their tile configuration is being dropped.

   **Fix needed:** Add a single `log.warn(...)` in `EurekaInstanceConfigCreator.createMetadata()`
   when `config.getCatalog()` is non-null:
   ```java
   if (config.getCatalog() != null) {
       log.warn("Catalog tile configuration ('apiml.service.catalog.tile') is present " +
           "but is no longer used in API Catalog v3. Remove it from the service configuration.");
   }
   ```

3. **MINOR — No snapshot test update for the wizard UI.** Removing a wizard section likely
   breaks existing snapshot tests. Verify or update those tests.

---

## fix/4282-oas31-schema-styles

**Change:** Added CSS rules targeting `section.models.oas3`, `.json-schema-2020-12-accordion`,
and `.json-schema-2020-12__title` in `Swagger.css`.

**Issues:**

1. **MAJOR — The CSS class names used (`json-schema-2020-12-accordion`,
   `json-schema-2020-12__title`) were inferred from knowledge of swagger-ui v5's DOM
   structure but were not verified by inspecting the actual rendered output in the running
   application.** If swagger-ui v5 uses different class names or nesting than assumed, the
   CSS rules will silently have no effect — the schemas section will remain unstyled and
   the bug will appear unfixed.

   **Fix needed:** Verify the class names are correct by running the API Catalog UI, opening
   a service that uses OAS 3.1 (e.g., the Gateway's own swagger), and inspecting the DOM.
   If the class names differ, update the CSS selectors.

2. **MINOR — The CSS uses `section.models.oas3` as the top-level selector.** This relies on
   swagger-ui always adding the `oas3` class to the section element for OAS 3.1 specs. If
   the swagger-ui version in use does not add this class, none of the new rules apply.
   Verify this with the actual installed version.

---

## fix/4478-origin-header-removal

**Change:** Added unconditional removal of the `Origin` header in `RoutingConfig.java`;
added `isBlank()` guard on the header loop.

**Issues:**

1. **MAJOR — The `ignoredHeadersWhenCorsEnabled.contains("Origin")` check is a simple
   string contains, not a split-and-compare.** If `ignoredHeadersWhenCorsEnabled` is
   `"Access-Control-Origin,Origin,X-Requested-With"`, the check correctly finds `"Origin"`.
   But if it is `"X-Forwarded-Origin,Origin"`, the check also finds `"Origin"` (because
   `"X-Forwarded-Origin".contains("Origin")` is `true` — a substring match). The guard
   would incorrectly skip the unconditional `Origin` removal filter.

   **Fix needed:** Use a split-and-exact-compare check:
   ```java
   boolean originAlreadyCovered = Arrays.stream(ignoredHeadersWhenCorsEnabled.split(","))
       .map(String::trim)
       .anyMatch("Origin"::equalsIgnoreCase);
   if (!originAlreadyCovered) {
       // add the RemoveRequestHeader filter for Origin
   }
   ```

2. **MINOR — The unconditional `Origin` removal means there is no way to forward the
   `Origin` header to downstream services, even if a downstream service legitimately needs
   it.** This is an intentional restriction for z/OSMF compatibility, but it may break
   other services. Document this behavior.

---

## fix/4510-deterministic-lb-early-exit

**Change:** Added an early exit in `DeterministicLoadBalancer.get()` for non-auth-LB
services; removed the `shouldIgnore()` call from `filterInstances()`; updated tests to use
`lenient()` for stubs that are now not invoked.

**Issues:**

1. **MAJOR — The `lbTypeIsAuthentication` check uses `serviceInstances.get(0)` to read
   metadata.** If different instances of the same service have inconsistent
   `apiml.lb.type` metadata (e.g., some instances registered with it, some without), the
   load balancer will make inconsistent routing decisions depending on which instance happens
   to be first in the list. This is a pre-existing issue but the refactor makes it more
   prominent since the entire auth-LB path is now gated on this single check.

   **Fix needed:** Change the check to be "at least one instance has auth-LB type":
   ```java
   if (serviceInstances.isEmpty() || serviceInstances.stream().noneMatch(this::lbTypeIsAuthentication)) {
   ```
   This is more robust but has a performance cost for large instance lists. Document the
   chosen semantics.

2. **MAJOR — `checkInstanceIdHeader` is called with `instanceId` which may be `null` if
   `getInstanceId(request.getContext())` returns `null`.** The original code checked for
   `instanceId != null` before calling `checkInstanceIdHeader`. The new code also checks
   (`if (instanceId != null)`), but the exception path wraps the exception in a new
   `ResponseStatusException` with the same status code and message. This changes the error
   message from whatever `checkInstanceIdHeader` throws to the hardcoded
   `"Service instance not found for the provided instance ID"`, potentially losing the
   original detail. This is intentional but the diff shows it is a behaviour change worth
   documenting.

3. **MINOR — Several test `lenient()` annotations were added but no comment explains why
   a specific stub is lenient.** Future developers may remove the `lenient()` annotation
   thinking it was added by mistake. The comments added in the test file are helpful — keep
   them.

---

## fix/4511-sticky-session-failover

**Change:** Stores `LB_SELECTED_USER` and `LB_SELECTED_SERVICE` in exchange attributes;
`NettyRoutingFilterApiml` reads these on connection failure and calls `lbCache.delete()`.

**Issues:**

1. **BLOCKER — `lbCache` is injected as a constructor parameter but the test uses `null`
   for it.** In `NettyRoutingFilterApimlTest.java:107`:
   ```java
   nettyRoutingFilterApiml = new NettyRoutingFilterApiml(httpClientNoCert, httpClientWithCert, null, null, null);
   ```
   The last `null` is `lbCache`. If any existing test exercises the `isConnectionRefused`
   path (which it does — `UnavailableService` tests check for 503), the code will call
   `lbCache.delete(user, service)` where `lbCache` is `null`, throwing a
   `NullPointerException` and causing the request to fail with 500 instead of 503.

   However, looking at the test more carefully: `LB_SELECTED_USER` and `LB_SELECTED_SERVICE`
   are only set in `DeterministicLoadBalancer` when there is an authenticated user. The
   acceptance test `UnavailableService.allInstancesAreUnavailable()` uses an unauthenticated
   request, so `exchange.getAttribute(LB_SELECTED_USER)` returns `null`. The code path:
   ```java
   Mono<Void> invalidate = (user != null && service != null) ? ... : Mono.empty();
   ```
   will follow `Mono.empty()` and never call `lbCache.delete()`. The NPE is avoided.
   But this is fragile — any future test that adds authentication to the service call AND
   exercises the connection failure path will get a NPE.

   **Fix needed:** Add a null-check guard for `lbCache`:
   ```java
   Mono<Void> invalidate = (lbCache != null && user != null && service != null)
       ? lbCache.delete(user, service).onErrorResume(...)
       : Mono.empty();
   ```

2. **MAJOR — The `LB_SELECTED_USER` and `LB_SELECTED_SERVICE` attributes are stored in the
   `RequestDataContext`'s `clientRequest.getAttributes()` map, not in the `ServerWebExchange`
   attributes.** `NettyRoutingFilterApiml` reads them from `exchange.getAttribute(...)` which
   is the `ServerWebExchange` attributes map. These are two different maps. The attributes
   stored in `RequestDataContext.clientRequest` are Spring Cloud LoadBalancer's internal
   request context, while `exchange.getAttributes()` is the WebFlux exchange context.

   **This means the attributes will never be found by `exchange.getAttribute(...)`** — they
   are stored in one map and read from a different map. The cache invalidation path will
   always take the `Mono.empty()` branch.

   **Fix needed:** Store the attributes in the `ServerWebExchange` directly, not in the
   load-balancer's `RequestDataContext`. The `ServerWebExchange` is accessible from the
   load-balancer context via Spring Cloud Gateway's `ReactiveLoadBalancerClientFilter` which
   stores it at `GATEWAY_LOADBALANCER_RESPONSE_ATTR`. However, the cleaner solution is to
   store the user/service in the exchange in a filter that runs before the load balancer,
   or to use a `LoadBalancerLifecycle` hook which has direct access to both the request
   context and the exchange.

   Alternative — pass the exchange into the load balancer via a `ReactiveLoadBalancerClientFilter`
   subclass that adds the user+service to the exchange attributes *after* instance selection.

3. **MAJOR — The `isConnectionRefused` helper is duplicated** across both the `fix/4512`
   branch and the `fix/4511` branch. This commit reimplements the same method since it is
   based on a separate branch. When both are merged together, there will be a duplicate
   method. Deduplicate before merging.

---

## fix/4149-zosmf-check-debounce

**Change:** Added `volatile long lastZosmfCheckAt` field in `ZosmfListener`; debounces
`CacheRefreshedEvent` handling to at most once per `startupCheckIntervalSeconds`.

**Issues:**

1. **MAJOR — `startupCheckIntervalSeconds` is an outer-class `@Value`-injected field.
   `ZosmfListener` is an inner class that reads it via the enclosing instance reference.**
   `startupCheckIntervalSeconds` is injected with `@Value("${apiml.startupCheckInterval:15}")`
   on the outer `JwtSecurity` class, not on `ZosmfListener`. Spring populates `@Value` fields
   after construction via `AutowiredAnnotationBeanPostProcessor`. At the moment
   `ZosmfListener` is instantiated (inside `JwtSecurity`'s constructor), the `@Value` field
   may not yet be populated (it is `0` until Spring finishes injection).

   If `ZosmfListener.onEvent()` fires before Spring finishes injecting `startupCheckIntervalSeconds`,
   `minIntervalMs = 0 * 1000L = 0`, and the debounce has no effect — every event passes through.

   **Fix needed:** Either use a constant for the default minimum interval, or inject the
   property into `ZosmfListener`'s constructor:
   ```java
   // In JwtSecurity, pass the value to ZosmfListener explicitly:
   this.zosmfListener = new ZosmfListener(eurekaClient, startupCheckIntervalSeconds);
   // But this has the same problem if called during @Autowired constructor before @Value injection.

   // Better: make minIntervalMs a constant:
   private static final long DEFAULT_DEBOUNCE_MS = 15_000L;
   ```
   Or, defer first listener registration until `@PostConstruct`:
   ```java
   @PostConstruct
   public void registerZosmfListener() {
       zosmfListener.register();
   }
   ```
   And set `minIntervalMs` inside `ZosmfListener.register()` from the outer class field.

2. **MINOR — `lastZosmfCheckAt` is `volatile` but the debounce check is not atomic.** Two
   threads can both read `lastZosmfCheckAt < minIntervalMs` as `true` simultaneously
   (before either updates the field) and both proceed to call
   `providers.isZosmfAvailableAndOnline()`. This is a benign data race — at worst two HTTP
   calls are made instead of one — but it is worth documenting.

---

## fix/4193-token-redistribution-on-startup

**Change:** Added `EurekaClient` and `RestTemplate` injection to `ZaasStartupListener`;
added `requestTokenRedistribution()` called from `notifyStartup()`.

**Issues:**

1. **BLOCKER — No `RestTemplate` bean is defined anywhere in ZAAS that can be injected by
   `@RequiredArgsConstructor`.** `ZaasStartupListener` uses Lombok's `@RequiredArgsConstructor`
   which generates a constructor for all `final` fields. `RestTemplate restTemplate` is a
   new final field. Spring will fail to start ZAAS with `NoSuchBeanDefinitionException:
   No qualifying bean of type 'RestTemplate' available` unless a `RestTemplate` bean
   exists in the application context.

   Inspection of the ZAAS codebase shows `CachingServiceClient` also injects a
   `RestTemplate` — meaning one must already exist as a bean somewhere. However, that bean
   may have a `@Qualifier` or be a conditional bean. This needs to be verified.

   **Fix needed:** Verify that an unqualified `RestTemplate` bean exists in the ZAAS Spring
   context. If not, declare one:
   ```java
   @Bean
   public RestTemplate internalRestTemplate(HttpConfig httpConfig) {
       // Use the secure HTTP client from HttpConfig so mutual TLS is used when
       // calling peer ZAAS instances
       return new RestTemplate(new HttpComponentsClientHttpRequestFactory(
           httpConfig.getSecureHttpClient()));
   }
   ```
   This is critical — without it ZAAS will fail to start entirely.

2. **MAJOR — `requestTokenRedistribution()` is called synchronously inside `notifyStartup()`,
   which is called from a timer task.** If any peer ZAAS instance is slow or unreachable,
   `restTemplate.getForObject(url, Void.class)` will block the timer thread for the full
   HTTP timeout duration (default may be several seconds per peer). With N peer instances,
   startup notification is delayed by N × timeout.

   **Fix needed:** Call `requestTokenRedistribution()` asynchronously so it does not delay
   the startup notification:
   ```java
   public void notifyStartup() {
       handler.onServiceStartup("ZAAS", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
       publisher.publishEvent(new ZaasServiceAvailableEvent(...));
       CompletableFuture.runAsync(this::requestTokenRedistribution);  // non-blocking
   }
   ```

3. **MAJOR — The `RestTemplate` call uses the plain HTTP URL from `EurekaUtils.getUrl(peer)`.
   If the peer is running with HTTPS only, the plain HTTP call will fail.** `EurekaUtils.getUrl()`
   returns the URL from Eureka registration, which depends on how the peer is registered
   (secure or non-secure port). On a properly configured HTTPS cluster, `EurekaUtils.getUrl()`
   should return the HTTPS URL, but this is not guaranteed.

   **Fix needed:** Verify and document that `EurekaUtils.getUrl(peer)` returns the correct
   scheme for the deployment, and ensure the `RestTemplate` is configured with a mutual-TLS
   HTTP client (see point 1).

---

## fix/3976-pat-scope-vs-zaas-unavailable

**Change:** ZAAS `ZaasExceptionHandler` adds `X-Zowe-Auth-Error-Code: TOKEN_NOT_VALID`
header on 401; gateway `ZaasSchemeTransformRest` inspects the header.

**Issues:**

1. **MAJOR — The `X-Zowe-Auth-Error-Code` header is added to `TokenNotValidException` AND
   `AuthSchemeException` responses (both handled by the same `@ExceptionHandler`).
   `AuthSchemeException` is not necessarily a token problem** — it is a general authentication
   scheme error that could indicate a configuration issue. Sending `TOKEN_NOT_VALID` for
   `AuthSchemeException` causes the gateway to propagate a 401 to the client when the real
   problem may be a ZAAS configuration error that should produce a 503.

   **Fix needed:** Split the handler for `AuthSchemeException` from `TokenNotValidException`
   and only add the header to the `TokenNotValidException` path:
   ```java
   @ExceptionHandler(value = {TokenNotValidException.class})
   public ResponseEntity<ApiMessageView> handleTokenNotValidException() {
       return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
           .header("X-Zowe-Auth-Error-Code", "TOKEN_NOT_VALID")
           .contentType(MediaType.APPLICATION_JSON)
           .body(messageService.createMessage("org.zowe.apiml.common.unauthorized").mapToView());
   }

   @ExceptionHandler(value = {AuthSchemeException.class})
   public ResponseEntity<ApiMessageView> handleAuthSchemeException() {
       return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
           .contentType(MediaType.APPLICATION_JSON)
           .body(messageService.createMessage("org.zowe.apiml.common.unauthorized").mapToView());
       // No X-Zowe-Auth-Error-Code — gateway falls back to stripped-credentials path
   }
   ```

2. **MINOR — The error code string `"TOKEN_NOT_VALID"` is a raw string literal in both
   ZAAS and the gateway.** If it is ever changed in one place but not the other, the
   protocol silently breaks. Define it as a shared constant:
   ```java
   // In ApimlConstants.java or a new AuthErrorCodes class:
   public static final String AUTH_ERROR_CODE_TOKEN_NOT_VALID = "TOKEN_NOT_VALID";
   ```

---

## fix/4143-attls-cert-read-retry

**Change:** Added a retry loop (3 attempts, 50ms apart) around the AT-TLS context read in
`AttlsHttpHandler`.

**Issues:**

1. **MAJOR — `Thread.sleep(50)` is called inside a reactive HTTP handler lambda.** The
   lambda passed to `(HttpHandler) (request, response) -> { ... }` is executed on a Netty
   event-loop thread (the reactor Netty I/O thread). Blocking an event-loop thread with
   `Thread.sleep` is a major anti-pattern in reactive programming — it can starve other
   requests sharing the same thread, cause cascading slowdowns, and violate the Project
   Reactor "no blocking on event loop" contract.

   **Fix needed:** Replace `Thread.sleep` with a non-blocking delay. The cleanest approach
   is to convert the retry logic to a reactive chain:
   ```java
   return Mono.fromCallable(() -> getAttlsContext(request, response))
       .retryWhen(Retry.backoff(ATTLS_MAX_ATTEMPTS - 1, Duration.ofMillis(ATTLS_RETRY_DELAY_MS))
           .filter(t -> t instanceof CertificateException || t instanceof IoctlCallException || ...))
       .onErrorReturn(internalErrorResult)
       .flatMap(ctx -> httpHandler.handle(ctx.request, response));
   ```
   However, `AttlsHttpHandler` is a servlet-tier `BeanPostProcessor` wrapping a blocking
   `HttpHandler`, not a reactive filter. The event-loop concern depends on whether the
   wrapped handler runs on the Netty event loop or a bounded elastic thread pool.

   If this code runs under Tomcat (blocking servlet), `Thread.sleep` is acceptable.
   If it runs under Netty (reactive), it is not. Given the class name includes "Reactive"
   in its parent chain (`AbstractServerHttpRequest`), this is likely reactive and the sleep
   is problematic.

   **Minimum fix:** Add a comment documenting the assumption:
   ```java
   // NOTE: Thread.sleep here is only safe if this handler runs on a blocking servlet
   // thread (Tomcat). If running on a Netty event loop, replace with reactive delay.
   ```
   And file a follow-up to convert to a reactive retry.

2. **MINOR — The `InterruptedException` catch sets `Thread.currentThread().interrupt()` and
   `break`s out of the retry loop.** After `break`, `lastException` is the last caught
   `CertificateException` (or whatever triggered the retry), and the code falls through to
   `internalError(request, response)`. The interrupted status is restored, but the request
   is returned as a 500. This is acceptable but the interrupt is not propagated up — it will
   be cleared by the response writing logic. Add a comment explaining this tradeoff.

---

## fix/3883-infinispan-port-collision

**Change:** Added `checkPortAvailable()` pre-flight check; added OS username to workspace
path.

**Issues:**

1. **MAJOR — `checkPortAvailable()` binds a `ServerSocket` to verify port availability,
   then immediately releases it.** There is a TOCTOU (time-of-check time-of-use) race
   condition: between `checkPortAvailable()` returning and JGroups actually binding the
   port, another process can grab it. The check provides a clearer error message but does
   not eliminate the race. This is acceptable for a "fail fast with good error message"
   improvement, but the commit message implies it "prevents port collisions" which is
   slightly overstated.

   **Fix needed:** Update the commit message / comments to say "provides an early, clear
   error message when the port is already in use at startup time" rather than implying it
   prevents all port conflicts.

2. **MAJOR — `checkPortAvailable()` is package-private (`void checkPortAvailable(...)`),
   not `private`.** This exposes it beyond the class for no reason — it is an internal
   helper. Change to `private`:
   ```java
   private void checkPortAvailable(int portNumber, String portName) {
   ```

3. **MAJOR — The workspace path isolation using `user.name` can still collide** if two
   users share the same OS username (e.g., both running as `ibmuser` on z/OS). The
   `user.name` property on z/OS typically reflects the RACF user ID, which is usually
   unique, but this is not guaranteed. A more robust discriminator would be a hash of
   the absolute path of the ZAAS certificate, or an explicit `haInstanceId` that operators
   are required to set uniquely per instance.

   No immediate code change required, but document the limitation.

4. **MINOR — The `checkPortAvailable` method throws `IllegalStateException` which is a
   `RuntimeException`.** This will cause the Spring application context to fail to start
   with an `IllegalStateException` wrapped in a `BeanCreationException`. The error will
   appear in the logs but may be hard to read because it is nested inside Spring's
   initialization stack trace. Consider throwing a more descriptive checked or unchecked
   exception, or add a `log.error()` before throwing so the port conflict message appears
   at the top level of the log output.

---

## Summary Table

| Branch | Severity | Issues |
|--------|----------|--------|
| fix/3841-missing-reason-action-error-codes | MINOR | No test coverage |
| fix/4159-auth-query-swagger-summary | MAJOR | `operationId` rename is a breaking change |
| fix/4286-zweag510-include-status-code | MAJOR | Breaking message format change undocumented |
| fix/2991-bom-missing-dependencies | MINOR | No automated BOM validation |
| fix/4362-idle-connection-eviction-interval | **BLOCKER** | Zero/negative `idleConnTimeoutSeconds` crashes timer |
| fix/4283-authorize-button-swagger | MAJOR | `supportedSubmitMethods: []` also removes "Try it out" |
| fix/4512-connect-exception-cause-chain | MAJOR | Potential infinite loop on circular cause chains; no test |
| fix/3131-zaas-shutdown-on-jwt-failure | MAJOR | `close()` called from timer thread risks deadlock; `null` context bypasses all testing |
| fix/4350-serviceid-validator-consistency | MAJOR | Inline FQCN; silent breaking change for 1-char serviceIds |
| fix/3097-saf-error-detail-in-response | MAJOR | `shortErrorName` may be null |
| fix/3007-auth-failure-header-leak | MAJOR | Hardcoded string instead of message key; requestUri in header enables injection |
| fix/4273-passticket-error-userid-applid | MAJOR | Production throw sites not updated; userId/applId missing from log |
| fix/4163-x509-cert-not-mapped-message | MAJOR | Missing `serialVersionUID`; no security chain test |
| fix/4451-salt-base64-encoding | **BLOCKER** | Migration guard delete+create race; can loop generating new salt each call |
| fix/4421-salt-init-atomic | MAJOR | Inner double-check suppresses I/O errors; broad class-level lock |
| fix/4418-remove-catalog-tile-wizard | MAJOR | Unused constants left; no deprecation warning for callers |
| fix/4282-oas31-schema-styles | MAJOR | CSS class names not verified against actual DOM |
| fix/4478-origin-header-removal | MAJOR | `contains("Origin")` is a substring check, not exact match |
| fix/4510-deterministic-lb-early-exit | MAJOR | First-instance metadata check is fragile for mixed-metadata fleets |
| fix/4511-sticky-session-failover | **BLOCKER** | Attributes stored in wrong map — cache invalidation never fires; `lbCache` NPE risk |
| fix/4149-zosmf-check-debounce | MAJOR | `@Value` field not populated when inner class reads it |
| fix/4193-token-redistribution-on-startup | **BLOCKER** | No `RestTemplate` bean; synchronous startup blocks timer thread |
| fix/3976-pat-scope-vs-zaas-unavailable | MAJOR | `TOKEN_NOT_VALID` header added to `AuthSchemeException` (wrong) |
| fix/4143-attls-cert-read-retry | MAJOR | `Thread.sleep` on potentially reactive event-loop thread |
| fix/3883-infinispan-port-collision | MAJOR | `checkPortAvailable` is package-private; TOCTOU race undocumented |
| fix/4340-extensions-attls-environment | — | No issues found |
