# Review Round 2

Critical analysis of the final state of all 26 local fix branches after both the
original fix commit(s) and any subsequent review-fix commits. Each entry records
what the combined change actually does, and where a senior engineer would still push
back.

Severity scale: **BLOCKER** (must fix before merge), **MAJOR** (functionally incorrect
or dangerously incomplete), **MINOR** (style, correctness nit, or polish).

---

## fix/2991-bom-missing-dependencies

**What the branch does:** Adds `from components.javaPlatform` to the `mavenJava`
publication so the BOM POM actually serialises its `<dependencyManagement>` block.
Adds a `validateBom` Gradle task that generates the POM and greps for the tag,
hooked into `check`.

**Issues:**

1. **MINOR — `$buildDir` is deprecated in modern Gradle.** The task uses
   `file("$buildDir/publications/mavenJava/pom-default.xml")`. Since Gradle 8.x,
   `buildDir` is deprecated in favour of `layout.buildDirectory`. Any project
   already receiving deprecation warnings will see one more.

   **Fix:** `file(layout.buildDirectory.file("publications/mavenJava/pom-default.xml"))`

2. **MINOR — The validation is text-based, not structural.** `content.contains('<dependencyManagement>')`
   would also pass if the tag appears in an XML comment or in a schema reference
   attribute. A minimal XPath or SAX check would be more robust, though for this
   specific regression the string check is sufficient in practice.

3. **MINOR — `check.dependsOn validateBom` is added after the `publishing` block.**
   In Groovy Gradle scripts this works by dynamic resolution, but it is unusual
   ordering that can confuse readers and static-analysis tools. Placing it after
   the task registration and before `publishing` would be cleaner.

---

## fix/3007-auth-failure-header-leak

**What the branch does:** Two changes: (a) in `AuthExceptionHandler.handleNoMainframeIdentity()`
replaces `ex.getMessage()` (which contained the raw URI) with a resolved message-service
string, preventing header injection; (b) in `AbstractAuthSchemeFactory` replaces the
raw `CertificateEncodingException` message with a new message-service key
`org.zowe.apiml.security.common.invalidCertificate` (ZWEAG112), declared in
`gateway-log-messages.yml`.

**Issues:**

1. **MAJOR — The new message key `org.zowe.apiml.security.common.invalidCertificate`
   is registered in `gateway-log-messages.yml` but the code that uses it lives in
   `AbstractAuthSchemeFactory`, which is in `apiml-security-common`.** If
   `apiml-security-common` is ever loaded in a context that does *not* have
   the gateway messages on the classpath (e.g. ZAAS, or a standalone enabler), the
   `messageService.createMessage(...)` call will throw `MessageNotFoundException` at
   runtime. The key should be registered in `common-log-messages.yml` (which is
   always on the classpath wherever the security-common library is used), or the
   message lookup should be done in the gateway layer only.

2. **MINOR — ZWEAG112 is a new number.** The existing gap between ZWEAG101 and the
   next used number should be checked to confirm no other branch or in-flight PR
   already assigned ZWEAG112. If there is a collision the API Catalog's message
   uniqueness checks will fail at startup.

3. **MINOR — The `handleNoMainframeIdentity` change silently drops the OIDC token
   from the context of the log message.** The old code embedded `ex.getMessage()`
   which (while leaky) did include diagnostic information for operators reading logs.
   The new approach puts a generic message into the header. Some teams relied on the
   header value for quick triage. Consider logging the original message at DEBUG level
   before building the sanitised header value (a pattern already used in the same
   method for the `log.debug(MESSAGE_FORMAT, ...)` call above it).

---

## fix/3097-saf-error-detail-in-response

**What the branch does:** Replaces `ex.getMessage()` (full SAF error detail) with
`ex.getPlatformError().shortErrorName` in both `AuthExceptionHandler` and
`ApimlExceptionHandler`, with a null guard that falls back to `"UNKNOWN"`.

**Issues:**

1. **MAJOR — `ApimlExceptionHandler.handleZosAuthenticationException()` wraps
   `ex.getPlatformError()` in `Optional.ofNullable(...).map(e -> e.errorMessage)`
   but then passes `shortName` (already computed outside the Optional) as the format
   argument.** If `ex.getPlatformError()` is null the `Optional.map` returns empty
   and `setBodyResponse` is called with a null message key. The null check on
   `shortErrorName` is correct, but the method will still throw a NullPointerException
   on `ex.getPlatformError().responseCode` (the very next line in `ApimlExceptionHandler`
   calls `ex.getPlatformError().responseCode.value()`). This path is rare, but the
   pre-existing null-safety of the Optional suggests the author was aware `getPlatformError()`
   could be null — that possibility is not fully handled after the refactor.

2. **MINOR — The two handlers are nearly identical but live in different modules
   (`apiml-security-common` and the `apiml` monolith).** The null-guard logic for
   `shortErrorName` is copy-pasted between them. If a third call site is added
   later, this pattern needs to be copied again. A small static helper in
   `ZosAuthenticationException` itself (e.g., `getSafeShortName()`) would centralise
   the logic.

3. **MINOR — The fallback string `"UNKNOWN"` appears in the HTTP response body.**
   On z/OS a PlatformPwdErrno whose `shortErrorName` is null is unusual, but if it
   does happen users see a literal `"UNKNOWN"` in the error response, which is
   unhelpful and could also trigger monitoring alerts keyed on that string.

---

## fix/3131-zaas-shutdown-on-jwt-failure

**What the branch does:** Injects `ConfigurableApplicationContext` into `JwtSecurity`;
replaces `System.exit(1)` with `new Thread(applicationContext::close, "zaas-shutdown").start()`.
The `@VisibleForTesting` constructor passes `null` for the context.

**Issues:**

1. **BLOCKER — Both shutdown sites guard with `if (applicationContext != null)` but
   the `@VisibleForTesting` constructor deliberately passes `null`.** This means that
   in every unit test that exercises the fatal-error code paths, the shutdown does
   *not* execute. Tests therefore never exercise the new shutdown behaviour at all.
   A regression that broke `applicationContext.close()` (e.g., a wrong thread, a
   deadlock) would not be caught by any test.

   **Fix:** Pass a mock `ConfigurableApplicationContext` in the `@VisibleForTesting`
   constructor and add a test that asserts `close()` is invoked on the expected paths.

2. **MAJOR — The timer-thread path still calls `System.exit(1)` was removed but
   replaced by nothing when `applicationContext` is null.** In testing the failure
   paths are now silent no-ops. Even in production, if somehow `applicationContext`
   ends up null (e.g., a race during construction), the fatal JWT misconfiguration
   will be silently swallowed — ZAAS will stay running with a broken JWT producer,
   accepting requests it cannot properly authorise.

3. **MINOR — Two consecutive `apimlLog.log(...)` calls immediately before the
   shutdown in the timer path.** The second message (`zosmfInstanceNotFound`) is
   semantically wrong: the timer fires because z/OSMF was *never* found within the
   timeout window, not because an instance "disappeared". This pre-existed, but the
   refactor touched this code and the misleading log was not corrected.

---

## fix/3841-missing-reason-action-error-codes

**What the branch does:** Adds `reason` and `action` fields to ZWEAO404, ZWEAO405,
ZWEAO415. Adds `CommonLogMessagesTest` using SnakeYAML to assert all three entries
have non-blank values.

**Issues:**

1. **MINOR — `@BeforeAll` throws a checked `Exception`.** JUnit 5 allows `@BeforeAll`
   to declare `throws Exception`, but if `loadYaml()` fails (e.g., the resource is
   not found), all three tests fail with an unintelligible `@BeforeAll` exception
   rather than a specific assertion failure. The `assertNotNull(in, ...)` check
   inside the method is correct, but a JUnit `Assumptions.assumeTrue` or a
   `@BeforeAll` that stores the failure and re-throws from each test would give
   better diagnostics.

2. **MINOR — The test uses raw `Map<String, Object>` with unchecked casts and
   `@SuppressWarnings("unchecked")`.** If the YAML schema ever changes (e.g.,
   `common` becomes a `Map` instead of a `List`), the `ClassCastException` at
   runtime gives no indication of which assertion failed. An AssertJ or Hamcrest
   approach with typed matchers would give better failure messages.

3. **MINOR — The test only checks the three specific message numbers, not the
   invariant "all messages in this file have reason and action".** A future commit
   could add a new message without reason/action and the test would not catch it.
   Consider a parameterised test that asserts the invariant for all entries.

---

## fix/3883-infinispan-port-collision

**What the branch does:** Adds a `private checkPortAvailable()` helper that binds a
`ServerSocket` to detect in-use ports early; adds `System.getProperty("user.name")`
as an extra path segment in the persistence directory. Tests updated.

**Issues:**

1. **MAJOR — `checkPortAvailable()` calls `Integer.parseInt(port)` but `port` is
   a `@Value`-injected `String` that could be blank, null, or non-numeric** if the
   configuration property is missing or malformed. `Integer.parseInt("")` throws
   `NumberFormatException`, which surfaces as a `BeanCreationException` with a
   confusing stack trace. A validation that catches `NumberFormatException` and
   surfaces a clear message is missing.

   The same applies to `keyExchangePort`.

2. **MINOR — `log.error(msg)` is called immediately before `throw new
   IllegalStateException(msg, e)`.** Spring will also log a `BeanCreationException`
   wrapping this, so the port-conflict message appears twice (once from `log.error`,
   once embedded in Spring's startup failure trace). This is not wrong, but it means
   operators searching logs will find two entries and may be confused about which one
   is authoritative.

3. **MINOR — The test helper `System.getProperty("user.name", "default")` is
   repeated in all four test cases** rather than extracted into a `@BeforeAll`
   constant. If the property key changes, four places need updating.

4. **MINOR — Workspace path now includes `user.name`, which can contain characters
   that are invalid in filesystem paths on some platforms** (spaces, slashes on
   z/OS). `Paths.get()` does not sanitise path components. A username like
   `admin user` would create a directory with a space, which may break downstream
   tooling that assumes no spaces in Zowe paths.

---

## fix/3976-pat-scope-vs-zaas-unavailable

**What the branch does:** Splits the combined `@ExceptionHandler({TokenNotValidException.class,
AuthSchemeException.class})` into two separate handlers; only `TokenNotValidException`
adds the `X-Zowe-Auth-Error-Code: TOKEN_NOT_VALID` header. Adds constants
`AUTH_ERROR_CODE_HEADER` and `AUTH_ERROR_CODE_TOKEN_NOT_VALID` in `ApimlConstants`.
Gateway's `ZaasSchemeTransformRest` uses those constants to detect and propagate the
header.

**Issues:**

1. **MAJOR — The error message produced by `handleAuthSchemeException()` is identical
   to `handleTokenNotValidException()`: both return `org.zowe.apiml.common.unauthorized`.**
   If an `AuthSchemeException` indicates a ZAAS *configuration* problem (as stated in
   the comment), the response should probably be a 503 (Service Unavailable), not a
   401 (Unauthorised). Returning 401 for a server-side configuration fault misleads
   callers into thinking their credentials are the problem.

2. **MINOR — The error message embedded in the `TokenNotValidException` thrown by the
   gateway** is `"Token rejected by ZAAS: TOKEN_NOT_VALID"`. This string echoes the
   constant value back to the caller in the exception message, which may eventually
   surface in an error response body. It is a code smell: the message should describe
   *what happened*, not *what value was in a header*. Prefer
   `"Token was rejected by ZAAS as invalid or lacking required scope."`.

3. **MINOR — No test was added** to verify that the gateway correctly propagates 401
   when the header is present, and correctly strips credentials when it is absent.
   The critical invariant (header present → 401 to caller; header absent → forward
   with stripped credentials) is untested.

---

## fix/4143-attls-cert-read-retry

**What the branch does:** Adds a 3-attempt retry loop with 50ms `Thread.sleep` between
attempts around the AT-TLS context read. Adds a comment documenting that
`Thread.sleep` is safe under Tomcat.

**Issues:**

1. **MAJOR — The `unsecureError` return inside the retry loop short-circuits on the
   *first* successful read that returns `StatConn != SECURE`.** This is correct
   behaviour, but a transient fault followed by recovery could produce:
   - Attempt 1: `IoctlCallException` → retry
   - Attempt 2: Success, but `StatConn != SECURE` → return `unsecureError`

   The unsecure path is not retried. If the `StatConn` value is itself transiently
   wrong during HAFT failover (which is the stated motivation for the retry), a
   connection that would eventually be secure is prematurely rejected. The retry
   should apply to the `StatConn` check as well, or at least be documented as a
   known limitation.

2. **MINOR — The comment claiming Tomcat safety relies on `RequestFacade` being
   present.** The code handles `RequestFacade` and `HttpServletRequestWrapper`, with
   an `else log.error(...)` branch for other types. In the `else` branch there is no
   retry logic problem, but the comment implies *all* paths are Tomcat-safe when the
   else branch proves the code can run with non-Tomcat request types.

3. **MINOR — `UnsatisfiedLinkError` is a JVM `Error`, not an `Exception`.** Catching
   it in the retry loop is a code smell: `Error` subclasses typically indicate a JVM
   state from which recovery is impossible (e.g., a missing native library). Retrying
   after `UnsatisfiedLinkError` will always fail and wastes 100ms (2 × 50ms) before
   returning 500. This was a pre-existing issue but the review commit left it in place.

---

## fix/4149-zosmf-check-debounce

**What the branch does:** Adds a `volatile long lastZosmfCheckAt` debounce field to
`ZosmfListener`; guards with `Math.max(startupCheckIntervalSeconds * 1000L, DEFAULT_DEBOUNCE_MS)`
where `DEFAULT_DEBOUNCE_MS = 15_000L` to handle the `@Value`-not-yet-populated case.

**Issues:**

1. **MAJOR — The debounce check-and-update is not atomic.** `lastZosmfCheckAt` is
   `volatile` which guarantees visibility, but two threads can both read
   `now - lastZosmfCheckAt >= minIntervalMs` as `true` before either writes the
   new value, causing two HTTP calls to z/OSMF to fire concurrently. While this is
   benign in practice (z/OSMF handles the extra call), the debounce contract is
   violated. An `AtomicLong` with `compareAndSet` would give a true single-fire
   guarantee:
   ```java
   private final AtomicLong lastZosmfCheckAt = new AtomicLong(0);
   // ...
   long prev = lastZosmfCheckAt.get();
   if (now - prev < minIntervalMs) return;
   if (!lastZosmfCheckAt.compareAndSet(prev, now)) return; // another thread won
   ```

2. **MINOR — `DEFAULT_DEBOUNCE_MS` duplicates the default value of the
   `@Value("${apiml.startupCheckInterval:15}")` property (15 s).** If an operator
   changes the property to a shorter value (e.g., 5 s), `Math.max` will silently
   ignore their configured value until it exceeds 15 s. The constant should either
   be a true minimum floor (e.g., 1 s) or the comment should explicitly warn that
   the effective minimum is 15 s regardless of configuration.

---

## fix/4159-auth-query-swagger-summary

**What the branch does:** Updates the `summary` field for `/zaas/api/v1/auth/query`
to include "retrieve the associated user information". The `operationId` rename to
`queryUsingGET` is reverted, keeping `validateUsingGET`.

**Issues:**

1. **MINOR — The `operationId` revert is correct, but the revert commit message says
   "revert operationId rename to avoid breaking SDK code generation"** without
   noting *which* code generators or clients are at risk. If this is a public API,
   adding a comment in the JSON near the operationId or a note in the release changelog
   would help future maintainers understand *why* the operationId must not be changed.

2. **MINOR — No test guards the summary text** against future accidental revert.
   A simple JSON-parsing test asserting the summary contains "user information" would
   provide a regression net.

---

## fix/4163-x509-cert-not-mapped-message

**What the branch does:** Adds `CertificateNotMappedException extends AuthenticationException`
with `serialVersionUID = 1L`; `X509AuthenticationProvider` throws it instead of
returning null; `AuthExceptionHandler` maps it to ZWEAG172; message added to
`zaas-log-messages.yml`.

**Issues:**

1. **MAJOR — `handleCertificateNotMapped` calls `writeErrorResponse` with
   `"org.zowe.apiml.security.common.certificateNotMapped"` as the message key**, but
   that key is registered in `zaas-log-messages.yml`. `AuthExceptionHandler` lives in
   `apiml-security-common`, which is used by multiple services. If any service other
   than ZAAS loads `AuthExceptionHandler` and encounters a `CertificateNotMappedException`,
   the message lookup will throw `MessageNotFoundException` at runtime because
   `zaas-log-messages.yml` is not on their classpath.

   The message key should be registered in `common-log-messages.yml` or in
   `apiml-security-common`'s own message resource.

2. **MINOR — The exception message passed to `CertificateNotMappedException` is the
   hardcoded string `"Certificate could not be mapped to a mainframe identity."`**
   This string is used only internally (logged at DEBUG level in the handler) and
   never surfaced to users, so it is not a security concern. But it duplicates the
   meaning of the ZWEAG172 message text. If the copy in the YAML is updated, the
   hardcoded Java string becomes stale.

---

## fix/4193-token-redistribution-on-startup

**What the branch does:** `ZaasStartupListener` gains `EurekaClient` and `RestTemplate`
final fields; on `notifyStartup()`, calls `requestTokenRedistribution()` wrapped in
`CompletableFuture.runAsync()`.

**Issues:**

1. **MAJOR — `CompletableFuture.runAsync()` uses the common fork-join pool by default.**
   In a Zowe deployment with many startup events or a constrained JVM, the FJP threads
   may be saturated. More critically, any uncaught exception inside the runAsync lambda
   is silently swallowed by `CompletableFuture` (the future is completed exceptionally
   but nobody calls `.get()` or `.exceptionally(...)`). If `requestTokenRedistribution`
   throws an unexpected unchecked exception, there is no log entry at a meaningful level.

   **Fix:** Add an `.exceptionally()` handler:
   ```java
   CompletableFuture.runAsync(this::requestTokenRedistribution)
       .exceptionally(t -> { log.error("Token redistribution failed", t); return null; });
   ```

2. **MAJOR — `requestTokenRedistribution()` is package-private**, making it testable,
   but there is no test verifying it is called on startup or that it correctly filters
   out the current instance. A test calling `notifyStartup()` with a mocked `EurekaClient`
   and `RestTemplate` would catch regressions.

3. **MINOR — `EurekaUtils.getUrl(peer)` returns the non-secure URL if the peer is
   registered with HTTP.** For HTTPS-only deployments this call silently fails with a
   connection-refused or SSL exception, which is caught and logged as a warning. This
   is documented as a known limitation in the review notes but not in the code.
   A `log.debug("Using URL {} for peer {}", url, peer.getInstanceId())` before the
   call would aid diagnosis.

---

## fix/4273-passticket-error-userid-applid

**What the branch does:** Adds `userId`/`applId` fields to `IRRPassTicketGenerationException`
with new preferred constructors and backward-compat old constructors. Updates the
message template and the `ZaasExceptionHandler` to log and render both fields.

**Issues:**

1. **MAJOR — The backward-compatible constructors set `userId = null` and `applId = null`.**
   Any call site that still uses the old two-arg or three-arg constructors (all z/OS
   native implementations) will produce `"<not available>"` in both the log and the
   HTTP response body. The review noted this and called for a grep of all throw sites,
   but no evidence of that audit exists in the commit. The `IRRPassTicketGenerationException`
   constructors in `common-service-core` are used by z/OS-specific modules that were
   not changed, so in production the `userId`/`applId` fields will be `null` for most
   real failures.

2. **MINOR — The message template changed from one `%s` to three `%s` parameters.**
   Any code that creates an `IRRPassTicketGenerationException` and passes it to
   `messageService.createMessage(key, ex.getErrorCode().getMessage())` without also
   passing `userId` and `applId` will produce a malformed message (missing arguments
   for the extra placeholders). The `handlePassTicketException` handler was updated,
   but if any other handler catches `IRRPassTicketGenerationException` and formats it
   the old way, it will throw `MissingFormatArgumentException` at runtime.

3. **MINOR — `ex.getErrorCode().getMessage()` is passed as the third argument** but
   `getErrorCode()` itself is never null-checked. If `IRRPassTicketGenerationException`
   is constructed via the `ErrorCode` constructor and somehow `errorCode` is null
   (which should not happen but is not enforced), the handler throws NPE.

---

## fix/4282-oas31-schema-styles

**What the branch does:** Adds CSS rules for `section.models.oas3`,
`.json-schema-2020-12-accordion`, and `.json-schema-2020-12__title`. Adds a comment
documenting the class-name assumptions and the source (swagger-ui v5 source tree).

**Issues:**

1. **MINOR — The comment says the class names were "verified against swagger-ui v5
   source (packages/swagger-ui/src/core/presets/oas3)"** but does not record the
   exact swagger-ui version number in the comment. When the swagger-ui dependency is
   upgraded and the CSS stops working, there is no baseline version to diff against.

2. **MINOR — No snapshot or visual regression test was added.** The review round 1
   noted this; it was not addressed. The CSS change could silently break on the next
   npm update.

---

## fix/4283-authorize-button-swagger

**What the branch does:** Removes the fragile `nth-child` CSS rule; adds
`supportedSubmitMethods: []` to both `setState` calls in `SwaggerUIApiml.jsx`. Adds
a comment explaining that this also disables "Try it out".

**Issues:**

1. **MAJOR — The decision to permanently disable "Try it out" for all APIs in the
   catalog is a significant UX regression** that is only documented in a code comment.
   Developers using the catalog to explore and test APIs lose the ability to make test
   requests from the UI entirely. This is a product decision that should be reflected
   in release notes and ideally surfaced as a configurable option, not a hardcoded
   constant.

2. **MINOR — Both `setState` blocks receive the identical comment** (copy-pasted
   seven lines of text). The second block is the `url`-based rendering path and already
   has `responseInterceptor` and other properties. A shared object literal or a helper
   function for the common props would reduce duplication and the risk of the two paths
   diverging in the future.

3. **MINOR — No unit test** was added to assert `supportedSubmitMethods: []` is present
   in the rendered props.

---

## fix/4286-zweag510-include-status-code

**What the branch does:** Adds `%s` to the ZWEAG510 message text; passes
`ex.getStatusCode().value()` as the format argument in `GatewayExceptionHandler`.
Adds a YAML comment noting the breaking format change.

**Issues:**

1. **MAJOR — `setBodyResponse` now receives the status code as both the HTTP status
   argument and the message format argument.** The signature is
   `setBodyResponse(exchange, statusCode, messageKey, messageArg...)`. If a future
   refactor of `setBodyResponse` changes the meaning of the extra arguments (e.g.,
   adds a different parameter before `messageArg`), the status code will silently land
   in the wrong position. This is fragile. Naming the intent with a local variable
   would help:
   ```java
   int httpStatus = ex.getStatusCode().value();
   return setBodyResponse(exchange, httpStatus, "org.zowe.apiml.gateway.responseStatusError", httpStatus);
   ```
   (The current code already does this implicitly; the variable name `ex.getStatusCode().value()`
   called twice makes the intent opaque.)

2. **MINOR — The YAML comment uses `\d+` in a comment describing a log pattern.**
   The actual message text contains `%s`, which formats to an unpadded decimal integer.
   The comment should say `[0-9]+` or just describe it in prose, since `\d+` implies
   a regex which readers may confuse with the actual log format.

---

## fix/4340-extensions-attls-environment

**What the branch does:** Creates `scannerContext` explicitly, calls
`scannerContext.setEnvironment(event.getApplicationContext().getEnvironment())` before
constructing the `ClassPathBeanDefinitionScanner`, so `@ConditionalOnProperty`
annotations are evaluated against the correct environment.

**Issues:**

1. **MINOR — `scannerContext` is an `AnnotationConfigApplicationContext` that is
   created but never refreshed or closed.** `AnnotationConfigApplicationContext`
   implements `Closeable`. While it is used only as a `BeanDefinitionRegistry` (the
   scanner writes definitions into it but the context is never started), Spring's
   implementation does register some internal lifecycle hooks. Not closing it could
   cause minor resource leaks (shutdown hooks, thread-local state). Wrapping in a
   try-with-resources or calling `scannerContext.close()` after the scan would be
   defensive:
   ```java
   try (var scannerContext = new AnnotationConfigApplicationContext()) {
       scannerContext.setEnvironment(...);
       var scanner = new ClassPathBeanDefinitionScanner(scannerContext);
       ...
   }
   ```

2. **MINOR — No test was added** to verify that AT-TLS conditional beans are correctly
   included or excluded when the environment property is set or absent.

---

## fix/4350-serviceid-validator-consistency

**What the branch does:** Replaces the local `Pattern` with `EurekaUtils.SERVICE_ID_PATTERN`;
switches from `.find()` to `.matches()`; updates the error message to document the
actual constraints (2–63 chars, lowercase alphanumeric + hyphen).

**Issues:**

1. **MAJOR — The `import java.util.regex.Pattern;` was removed** but
   `EurekaUtils.SERVICE_ID_PATTERN` is of type `java.util.regex.Pattern`. The field
   declaration `private static final Pattern p = EurekaUtils.SERVICE_ID_PATTERN;`
   now requires the FQCN `java.util.regex.Pattern` to compile without the import.
   The diff confirms the import was removed. If checkstyle or the IDE import
   organiser re-runs, it may re-add the import automatically; but the compile will
   fail without it or the FQCN. Running `./gradlew :api-catalog-services:compileJava`
   would catch this. The review-fix commit message says "restore Pattern import" —
   but the actual diff does not show the import being restored. The import must be
   verified present in the compiled file.

   *Confirmed from diff:* the import is not in the diff. If `EurekaUtils.SERVICE_ID_PATTERN`
   returns `java.util.regex.Pattern` and the import was removed, the build should fail.
   Either the import was silently kept (not shown in diff) or the code does not compile.

2. **MINOR — The field is still named `p`** — a single-letter non-descriptive name.
   Now that the pattern is borrowed from a shared utility, renaming to `SERVICE_ID_PATTERN`
   or even removing the local field and calling `EurekaUtils.SERVICE_ID_PATTERN.matcher(...)` 
   directly would be cleaner.

---

## fix/4362-idle-connection-eviction-interval

**What the branch does:** Computes `evictionIntervalMs = Math.max(idleConnTimeoutSeconds * 1000L, 1000L)`
and passes it to `Timer.schedule()` instead of the hardcoded `30000`.

**Issues:**

1. **MINOR — The floor of 1 second (`1000L`) is the timer-crash guard, not a
   sensible operational floor.** If `idleConnTimeoutSeconds` is set to 1 (by an
   operator who really wants 1-second eviction), the timer fires every 1 second,
   which is correct but may cause unexpected CPU overhead. The comment says "Enforce a
   floor of 1 second" but does not say this is the operational minimum. Documenting
   a recommended minimum (e.g., 5 s) in the `@Value` annotation default or in the
   `application.yml` documentation would help operators.

2. **MINOR — The eviction timer fires at `evictionIntervalMs` but closes idle
   connections that have been idle for `idleConnTimeoutSeconds` seconds** (the
   `closeIdle(Timeout.ofSeconds(idleConnTimeoutSeconds))` call is unchanged). So
   the initial delay and period for the timer now equal `idleConnTimeoutSeconds * 1000`,
   meaning the *first* eviction run happens after `idleConnTimeoutSeconds` seconds,
   not immediately. The old code used an initial delay of 30 seconds regardless of
   the timeout. The behaviour change (first eviction fires later for long timeout
   values) is not documented.

---

## fix/4418-remove-catalog-tile-wizard

**What the branch does:** Removes catalog tile sections from both wizard JSX files;
removes `CATALOG_*` constants from `EurekaInstanceConfigCreator`; removes the
"no tile config" warning from `EurekaInstanceConfigValidator`; replaces the old
behavior with a `log.warn` when tile config is present.

**Issues:**

1. **MINOR — The wizard snapshot tests, if any, were not mentioned in the commit.**
   Removing a wizard section almost certainly breaks existing UI snapshot tests.
   The review noted this but no evidence of snapshot updates exists in the diff.

2. **MINOR — The `EurekaInstanceConfigValidator` no longer warns when `catalog` is
   null**, but `EurekaInstanceConfigCreator` warns when it is non-null. The asymmetry
   could confuse operators: previously they were told to add tile config; now they
   are told to remove it (if present) but not notified if it is absent. This is
   semantically correct for the new behaviour but the transition could be confusing
   for teams upgrading from v2 who still have the config and were previously warned
   to add it.

---

## fix/4421-salt-init-atomic

**What the branch does:** Uses a dedicated `SALT_LOCK` object; double-checked
locking with differentiated exception handling inside the sync block
(propagates real I/O errors, ignores not-found); wraps the conflict fallback
read with `SecureTokenInitializationException`.

**Note:** This branch includes the Base64 migration code from `fix/4451` as its
first commit (shared base). The atomic fix is the second commit. Review covers both.

**Issues:**

1. **MAJOR — The migration path (legacy salt detection) in `getSalt()` calls
   `cachingServiceClient.delete("salt")` inside a `catch (Exception ignored)` block,
   then calls `storeSalt(newSalt)` which does a `create`.** If `delete` fails
   silently (network timeout), `create` will throw a conflict exception. This conflict
   is not caught in `getSalt()` — it propagates as an uncaught `CachingServiceClientException`,
   causing the PAT request to fail with a 500. The fix in `fix/4451` uses `update`
   to avoid this; the `fix/4421` branch still has the `delete`+`create` pattern in
   `getSalt()`. The two branches solve the same problem differently and will conflict
   on merge.

2. **MAJOR — `initializeSalt()` returns `Base64.getEncoder().encodeToString(newSalt)`
   after storing via `storeSalt()`.** But `getSalt()` calls `initializeSalt()` and then
   decodes the returned value. If `initializeSalt()` falls into the outer
   (non-synchronized) catch and generates a new salt, it returns the Base64 string.
   If it falls into the synchronized block and the conflict catch reads the peer's
   salt, it returns whatever the peer stored (which should also be Base64 if the peer
   is on the same code version). However, if the peer is running the *old* code without
   Base64, the conflict-path read returns a raw-byte string, which `getSalt()` will
   fail to Base64-decode and re-enter the migration path — an infinite migration loop.
   The mixed-version upgrade scenario is not handled.

3. **MINOR — `SecureTokenInitializationException` is thrown by wrapping
   `CachingServiceClientException`** in the conflict fallback path. Callers of
   `getSalt()` only declare `throws CachingServiceClientException`. If
   `SecureTokenInitializationException` is not a subtype of `CachingServiceClientException`,
   this will not compile (or if it is unchecked, callers may not handle it). The class
   hierarchy of `SecureTokenInitializationException` should be verified.

---

## fix/4451-salt-base64-encoding

**What the branch does:** `storeSalt()` Base64-encodes before writing; `getSalt()`
Base64-decodes after reading; migration path uses `update` (not `delete`+`create`)
to atomically overwrite the legacy raw-byte salt; logs a `warn` on migration.

**Issues:**

1. **MAJOR — The migration path catches `CachingServiceClientException` from `update`
   and falls back to `create`.** This is the correct improvement over the original
   `delete`+`create`, but the `create` fallback can itself throw a conflict exception
   if two nodes simultaneously hit the migration path (both read a non-Base64 value,
   both fail to update, both try to create). The second `create` is not caught: it
   propagates as an unhandled `CachingServiceClientException`. The correct fallback
   after a failed `create` (conflict) should be to `read` again.

2. **MAJOR — `initializeSalt()` still has a race:** after the outer try-catch returns
   early (key not found, null cause), two threads could both call `storeSalt()` and
   the second would get a conflict. The review noted that `fix/4421` solves this with
   a lock. `fix/4451` does not include the lock and therefore still has the concurrent
   creation race for new installations.

3. **MINOR — The migration warning says "All previously issued Personal Access Tokens
   are now invalid."** This is correct but alarmist for operators who may not know
   what PATs are. Linking to documentation or clarifying "users will need to re-issue
   Personal Access Tokens" would be more actionable.

---

## fix/4478-origin-header-removal

**What the branch does:** Adds an exact-match split check (`Arrays.stream(...split(","))
.anyMatch("Origin"::equalsIgnoreCase)`) before conditionally adding a
`RemoveRequestHeader` filter for the `Origin` header. Also adds an `isBlank()` guard
on the loop that builds filters from `ignoredHeadersWhenCorsEnabled`.

**Issues:**

1. **MINOR — The `ignoredHeadersWhenCorsEnabled.split(",")` call is executed twice:**
   once in the `originAlreadyCovered` stream and once in the `for` loop below.
   Extracting the split result to a local `String[]` would be slightly more efficient
   and make it obvious that both iterations use the same input:
   ```java
   String[] ignoredHeaders = ignoredHeadersWhenCorsEnabled.split(",");
   ```

2. **MINOR — Neither the `originAlreadyCovered` check nor the `isBlank()` guard has
   a unit test.** The existing tests presumably exercise the header filter list, but
   edge cases such as an empty string, a string with only commas, or
   `"X-Forwarded-Origin,Origin"` are not tested.

3. **MINOR — The unconditional `Origin` removal is added as a `RemoveRequestHeader`
   filter at application startup**, which means it applies to *all* routes globally.
   If a downstream service legitimately needs the `Origin` header (e.g., a CORS-
   compliant service), there is no per-route override mechanism. This was noted in
   COMMIT_REVIEW.md as an accepted intentional restriction, but there is no
   code-level comment explaining that per-route override is not supported.

---

## fix/4510-deterministic-lb-early-exit

**What the branch does:** Adds an early exit in `get()` for services where no instance
has `apiml.lb.type=authentication`, using `stream().noneMatch(this::lbTypeIsAuthentication)`.
Updates `shouldIgnore()` with the same pattern. Marks formerly unnecessary Mockito
stubs as `lenient()` with explanatory comments.

**Issues:**

1. **MAJOR — The early-exit path handles the `X-InstanceId` header by catching
   `ResponseStatusException` from `checkInstanceIdHeader()` and wrapping it in a new
   `ResponseStatusException` with the hardcoded message `"Service instance not found
   for the provided instance ID"`.** This drops the original exception's message and
   HTTP status. If `checkInstanceIdHeader` throws with a different status (e.g., 403),
   the re-wrapped exception always uses 404. This is a silent behaviour change.

2. **MINOR — `noneMatch` scans all instances on every request** for services that do
   not use auth-LB. For a service with many instances (e.g., 50+), this is an O(N)
   scan that could be cached. The metadata for a given service is stable between
   Eureka refreshes; caching the result keyed by `serviceId` with a short TTL would
   eliminate the per-request scan.

3. **MINOR — `shouldIgnore()` remains package-private** even after the refactor. It
   is now used only from within the class (the call site in `filterInstances()` was
   removed). It should be either `private` or removed if it is only used in tests.

---

## fix/4511-sticky-session-failover

**What the branch does:** Adds `LB_SELECTED_USER` and `LB_SELECTED_SERVICE` constants;
`DeterministicLoadBalancer` stores them in `ctx.getClientRequest().getAttributes()`;
`NettyRoutingFilterApiml` injects `LoadBalancerCache`, reads those attributes from
`exchange.getAttribute(...)`, and calls `lbCache.delete()` on `ConnectException`;
adds null-check for `lbCache`; depth-limits the cause-chain walk.

**Issues:**

1. **MAJOR — `DeterministicLoadBalancer` stores the attributes in
   `ctx.getClientRequest().getAttributes()` (the `RequestData` attributes map) and
   `NettyRoutingFilterApiml` reads them from `exchange.getAttribute(...)`.** The
   investigation in COMMIT_REVIEW.md Round 1 concluded these are the same map because
   `ReactiveLoadBalancerClientFilter` constructs `RequestData` with
   `exchange.getAttributes()` (same reference). This is correct for the current
   Spring Cloud Gateway version (4.3.3), where `RequestData(ServerHttpRequest request,
   Map<String, Object> attributes)` stores the map by reference. However, this is an
   implementation detail of a third-party library that is not part of the public API
   contract. Any upgrade to Spring Cloud Gateway that changes this constructor to copy
   the map would silently break the failover path with no test catching it. A comment
   documenting this dependency and the version it was verified against is the minimum
   mitigation.

2. **MINOR — The attributes are only stored in the `else` branch of the
   `user != null && !user.isEmpty()` check.** If the user principal is empty or
   null, the attributes are never set, and `exchange.getAttribute(LB_SELECTED_USER)`
   returns null, so `lbCache.delete()` is never called. This is correct, but it means
   that for unauthenticated requests that still use auth-LB type services, failover
   cache invalidation is silently skipped. A comment to this effect would help future
   maintainers.

3. **MINOR — `isConnectionRefused()` is duplicated** between `fix/4511` and `fix/4512`.
   Both branches add an identical private method to `NettyRoutingFilterApiml`. On
   merge these will conflict, and manual resolution risks subtle differences
   (e.g., the depth limit is in both but the comment differs). The method should
   live in one place (it already does in `fix/4511`, which supersedes `fix/4512`
   for this change).

---

## fix/4512-connect-exception-cause-chain

**What the branch does:** Replaces `e.getCause() instanceof ConnectException` with
`isConnectionRefused(e)` which walks the full cause chain up to depth 20.

**Issues:**

1. **MAJOR — This branch's `isConnectionRefused()` will be duplicated when `fix/4511`
   is also merged**, since both add the same method to `NettyRoutingFilterApiml`. The
   two branches address overlapping concerns and should be merged or one should be
   rebased on top of the other before either is merged to main.

2. **MINOR — No test for a doubly-wrapped `ConnectException`** was added (the review
   round 1 called for one). The fix is applied but is unverified.

3. **MINOR — The depth limit of 20 is a magic number** without justification in a
   named constant. A constant `private static final int MAX_CAUSE_DEPTH = 20;` with a
   brief comment ("real exception chains rarely exceed 5; 20 is a generous safety ceiling")
   would make the intent clear.

---

## Summary Table

| Branch | New Severity | Key Remaining Issues |
|--------|-------------|----------------------|
| fix/2991-bom-missing-dependencies | MINOR | `$buildDir` deprecated; text-based POM check |
| fix/3007-auth-failure-header-leak | **MAJOR** | Message key in wrong YAML file (runtime MNFE in non-gateway services) |
| fix/3097-saf-error-detail-in-response | **MAJOR** | NPE risk when `getPlatformError()` is null in ApimlExceptionHandler |
| fix/3131-zaas-shutdown-on-jwt-failure | **BLOCKER** | Null context means shutdown path never tested; silent no-op on failure |
| fix/3841-missing-reason-action-error-codes | MINOR | Test too narrow; `@BeforeAll` diagnostics poor |
| fix/3883-infinispan-port-collision | **MAJOR** | `parseInt` on blank/invalid config throws NFE; path has no sanitisation |
| fix/3976-pat-scope-vs-zaas-unavailable | **MAJOR** | `AuthSchemeException` returns 401 (should be 503); no test for gateway path |
| fix/4143-attls-cert-read-retry | **MAJOR** | `StatConn != SECURE` not retried; `UnsatisfiedLinkError` retried pointlessly |
| fix/4149-zosmf-check-debounce | **MAJOR** | Non-atomic debounce allows concurrent z/OSMF calls; constant overrides config |
| fix/4159-auth-query-swagger-summary | MINOR | No regression test for summary text |
| fix/4163-x509-cert-not-mapped-message | **MAJOR** | Message key in zaas-only YAML but handler lives in shared library |
| fix/4193-token-redistribution-on-startup | **MAJOR** | Async exceptions silently swallowed; no test; HTTPS scheme not guaranteed |
| fix/4273-passticket-error-userid-applid | **MAJOR** | Native throw sites still use null-producing constructors; message format breakage risk |
| fix/4282-oas31-schema-styles | MINOR | swagger-ui version not pinned in comment; no visual test |
| fix/4283-authorize-button-swagger | **MAJOR** | "Try it out" disabled globally with no configuration override |
| fix/4286-zweag510-include-status-code | MINOR | Fragile dual-use of status code as both HTTP status and format arg |
| fix/4340-extensions-attls-environment | MINOR | `AnnotationConfigApplicationContext` not closed; no test |
| fix/4350-serviceid-validator-consistency | **MAJOR** | `import java.util.regex.Pattern` may be missing (verify compilation) |
| fix/4362-idle-connection-eviction-interval | MINOR | Initial delay change undocumented; 1 s floor may cause CPU overhead |
| fix/4418-remove-catalog-tile-wizard | MINOR | Snapshot tests not mentioned; upgrade transition asymmetry |
| fix/4421-salt-init-atomic | **MAJOR** | Migration delete+create race still present; mixed-version infinite loop risk |
| fix/4451-salt-base64-encoding | **MAJOR** | Create fallback not retried on conflict; no JVM-level lock against concurrent creation |
| fix/4478-origin-header-removal | MINOR | `split()` called twice; no edge-case tests |
| fix/4510-deterministic-lb-early-exit | **MAJOR** | Wrapped ResponseStatusException silently changes HTTP status |
| fix/4511-sticky-session-failover | **MAJOR** | Spring Cloud Gateway internal API relied on without pinning; `isConnectionRefused` duplicate |
| fix/4512-connect-exception-cause-chain | **MAJOR** | Duplicate method conflict with fix/4511; depth constant not named |
