# Fix Plans for Open Bugs (Ordered by Simplicity)

Each plan references the exact files, line numbers, and the precise change required.

## Cross-cutting notes

- **#14 before #15:** The salt Base64 fix (#4451, plan 14) must be applied before or together with the atomicity fix (#4421, plan 15), since plan 15's code returns a Base64-encoded string that plan 14's `getSalt()` must decode.
- **#7 before #20:** The ConnectException chain-walk fix (#4512, plan 7) should be applied before the sticky-session failover (#4511, plan 20), since plan 20's error-path cache invalidation depends on `ServiceNotAccessibleException` being reliably raised.
- **Message numbers:** Plans 13 and 22 introduce new ZWEAG message codes. Confirmed next free numbers: **ZWEAG171** (plan 13, `certificateNotMapped`) and **ZWEAG172** (plan 22, `revokedUser`). Verify against the full message registry before committing.
- **`generateFailed` YAML location:** The message key `org.zowe.apiml.security.ticket.generateFailed` lives in `common-service-core/src/main/resources/core-log-messages.yml`, not in `zaas-log-messages.yml`. Plan 12 updates that file.

---

---

## 1. #3841 — Missing `reason` and `action` in ZWEAO404 / ZWEAO405 / ZWEAO415

**File:** `apiml-common/src/main/resources/common-log-messages.yml:61–74`

**Root cause:** Three message entries have only `key`, `number`, `type`, and `text` fields. All other error codes in the same file (e.g. ZWEAO402 at line 54, ZWEAO503 at line 84) include `reason` and `action` fields.

**Change:** Add `reason` and `action` fields to all three entries, following the style of the surrounding entries.

```yaml
# ZWEAO404 (line 61)
- key: org.zowe.apiml.common.notFound
  number: ZWEAO404
  type: ERROR
  text: "The service can not find the requested resource."
  reason: "The requested endpoint or resource does not exist on the target service."
  action: "Verify the endpoint path is correct and that the service exposes the requested resource."

# ZWEAO405 (line 66)
- key: org.zowe.apiml.common.methodNotAllowed
  number: ZWEAO405
  type: ERROR
  text: "The request method has been disabled and cannot be used for the requested resource."
  reason: "The HTTP method used in the request is not supported for this endpoint."
  action: "Check the API documentation to determine which HTTP methods are supported for this endpoint."

# ZWEAO415 (line 71)
- key: org.zowe.apiml.common.unsupportedMediaType
  number: ZWEAO415
  type: ERROR
  text: "The media format of the requested data is not supported by the service, so the service has rejected the request."
  reason: "The Content-Type header in the request specifies a media type that the endpoint does not accept."
  action: "Set the Content-Type header to a media type supported by the endpoint, typically application/json."
```

**Tests:** No existing unit tests assert on the presence of `reason`/`action`. Add a test in `apiml-common` that loads the YAML and asserts all three message keys have non-blank `reason` and `action` fields.

---

## 2. #4159 — `/auth/query` Swagger summary says "Validate" only

**File:** `zaas-service/src/main/resources/zaas-api-doc.json:54,56`

**Root cause:** The `summary` field (shown as the headline in Swagger UI) reads `"Validate the authentication token."` The description body was already expanded to mention retrieval, but the headline still misleads users. The `operationId` is also `"validateUsingGET"`.

**Change:** Update the two fields on lines 54 and 56:

```json
"summary": "Validate the authentication token and retrieve the associated user information.",
"operationId": "queryUsingGET",
```

**Tests:** No automated test covers the exact summary string. Add a test that parses `zaas-api-doc.json` and asserts the `/zaas/api/v1/auth/query` `summary` field contains the word "retrieve" and that the `operationId` is `"queryUsingGET"`. This guards against future regressions.

---

## 3. #4286 — ZWEAG510E message does not include the status code

**Files:**
- `gateway-service/src/main/resources/gateway-log-messages.yml:104`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/controllers/GatewayExceptionHandler.java:169`

**Root cause:** The message text is a static string with no format parameter. `handleStatusError()` passes no arguments to `setBodyResponse()`.

**Change 1** — Add `%s` placeholder to the message text in the YAML:
```yaml
text: "Request to the resource ended with unexpected status code %s."
```

**Change 2** — Pass the status code as the format argument in `GatewayExceptionHandler.java:169`:
```java
// Before:
return setBodyResponse(exchange, ex.getStatusCode().value(), "org.zowe.apiml.gateway.responseStatusError");
// After:
return setBodyResponse(exchange, ex.getStatusCode().value(), "org.zowe.apiml.gateway.responseStatusError", ex.getStatusCode().value());
```

**Tests:** Add a test in `GatewayExceptionHandlerTest` that fires a `ResponseStatusException` with a known status and asserts the response body contains the status code number.

---

## 4. #2991 — API ML BOM missing dependencies

**File:** `platform/build.gradle:48–61`

**Root cause:** The `mavenJava` publication block contains only `pom { licenses { ... } }`, `groupId`, `version`, and `artifactId`. It is missing the `from components.javaPlatform` statement, which is the Gradle instruction that serializes the `java-platform` constraints block (lines 18–31) into a Maven `<dependencyManagement>` section.

**Change:** Add `from components.javaPlatform` inside the `mavenJava(MavenPublication)` block:

```groovy
publications {
    mavenJava(MavenPublication) {
        from components.javaPlatform   // <-- add this line
        pom {
            licenses {
                license {
                    name = 'Eclipse Public License, v2.0'
                    url = 'https://www.eclipse.org/legal/epl-2.0/'
                }
            }
        }
        groupId 'org.zowe.apiml'
        version rootProject.version
        artifactId "${project.artifactId}"
    }
}
```

**Tests:** Run `./gradlew :platform:generatePomFileForMavenJavaPublication` and inspect the generated POM at `platform/build/publications/mavenJava/pom-default.xml` to confirm a `<dependencyManagement>` section is present with the expected dependencies.

---

## 5. #4362 — Idle connection eviction interval larger than idle timeout

**File:** `apiml-common/src/main/java/org/zowe/apiml/product/web/HttpConfig.java:213–219`

**Root cause:** The `connectionManagerTimer.schedule()` call hardcodes `30000` ms (30 s) as both the initial delay and the period. The idle timeout is `idleConnTimeoutSeconds` (default 5 s). The eviction task should fire at an interval no larger than the idle timeout itself.

**Change:** Replace the hardcoded `30000, 30000` with a computed value based on `idleConnTimeoutSeconds`:

```java
// Before:
this.connectionManagerTimer.schedule(new TimerTask() {
    @Override
    public void run() {
        connectionManager.closeExpired();
        connectionManager.closeIdle(Timeout.ofSeconds(idleConnTimeoutSeconds));
    }
}, 30000, 30000);

// After:
long evictionIntervalMs = idleConnTimeoutSeconds * 1000L;
this.connectionManagerTimer.schedule(new TimerTask() {
    @Override
    public void run() {
        connectionManager.closeExpired();
        connectionManager.closeIdle(Timeout.ofSeconds(idleConnTimeoutSeconds));
    }
}, evictionIntervalMs, evictionIntervalMs);
```

**Tests:** Add a test in `HttpConfigTest` that verifies `getConnectionManager()` schedules the timer with a period equal to `idleConnTimeoutSeconds * 1000`. Inject a mock `Timer` or spy to capture the scheduled interval.

---

## 6. #4283 — Authorize button hidden by fragile CSS selector

**Files:**
- `api-catalog-ui/frontend/src/components/Swagger/SwaggerUIApiml.jsx:146–153,160–174`
- `api-catalog-ui/frontend/src/components/Swagger/Swagger.css:19–22`

**Root cause:** The button is hidden using an `nth-child` CSS selector that depends on the internal SwaggerUI DOM structure. The correct approach is to pass `supportedSubmitMethods: []` to the SwaggerUI component, which disables the "Try it out" and authorization submission at the API level, preventing the button from being rendered at all.

**Change 1** — Add `supportedSubmitMethods: []` to both `swaggerProps` objects in `SwaggerUIApiml.jsx`:

```jsx
// In the first setState (line ~146):
swaggerProps: {
    dom_id: '#swaggerContainer',
    spec: swagger,
    presets: [SwaggerUi.presets.apis],
    requestSnippetsEnabled: true,
    supportedSubmitMethods: [],   // <-- add this
    plugins: [this.customPlugins, AdvancedFilterPlugin, CustomizedSnippedGenerator(codeSnippets)],
    filter: true,
},

// In the second setState (line ~160):
swaggerProps: {
    dom_id: '#swaggerContainer',
    url,
    presets: [SwaggerUi.presets.apis],
    requestSnippetsEnabled: true,
    supportedSubmitMethods: [],   // <-- add this
    plugins: [this.customPlugins, AdvancedFilterPlugin, CustomizedSnippedGenerator(codeSnippets)],
    responseInterceptor: ...,
},
```

**Change 2** — Remove the fragile CSS rule in `Swagger.css:19–22`:
```css
/* Remove these lines: */
/*comment this to see the authorization button */
#swaggerContainer > div > div:nth-child(2) > div.scheme-container > section > div.auth-wrapper > button {
    display: none;
}
```

**Tests:** Add a React unit test in `SwaggerUIApiml.test.js` that asserts `swaggerProps` contains `supportedSubmitMethods: []`. Also add a Cypress/integration test that loads a service in the catalog and asserts the Authorize button is not present in the DOM.

---

## 7. #4512 — ConnectException check only looks one level deep

**File:** `gateway-service/src/main/java/org/zowe/apiml/gateway/config/NettyRoutingFilterApiml.java:86–92`

**Root cause:** The condition `e.getCause() instanceof ConnectException` only checks exactly one level in the exception cause chain. Netty sometimes wraps exceptions multiple levels deep (e.g. `IOException` → `ConnectException` → root cause). A doubly-wrapped `ConnectException` falls through to `Mono.error(e)`, which becomes an unhandled 500.

**Change:** Replace the single-level check with a root-cause walk using Apache Commons `ExceptionUtils` (already on the classpath via Spring) or a manual loop:

```java
// Before:
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return super.filter(exchange, chain).onErrorResume(e -> {
        if (e.getCause() instanceof ConnectException) {
            var uri = exchange.getRequest().getURI();
            return Mono.error(new ServiceNotAccessibleException(..., e));
        }
        return Mono.error(e);
    });
}

// After:
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return super.filter(exchange, chain).onErrorResume(e -> {
        if (isConnectionRefused(e)) {
            var uri = exchange.getRequest().getURI();
            return Mono.error(new ServiceNotAccessibleException(
                String.format("Service is not available at %s://%s:%d", uri.getScheme(), uri.getHost(), uri.getPort()), e));
        }
        return Mono.error(e);
    });
}

private boolean isConnectionRefused(Throwable t) {
    Throwable current = t;
    while (current != null) {
        if (current instanceof ConnectException) return true;
        current = current.getCause();
    }
    return false;
}
```

**Tests:** Extend `NettyRoutingFilterApimlTest` with a test case where `ConnectException` is nested two levels deep (wrapped in an `IOException`) and assert the response status is `SC_SERVICE_UNAVAILABLE` (503).

---

## 8. #3131 — ZAAS timeout path logs but does not shut down

**File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/JwtSecurity.java:267–284,319–326`

**Root cause:** Two separate issues:
1. The one-minute timer task at line 275 logs an error when z/OSMF is not available, but does nothing to stop the service.
2. Line 325 still has `System.exit(1)` (marked `// TODO remove`) in the async listener path for the case where JWT validation against z/OSMF fails.

**Change 1** — Inject `ConfigurableApplicationContext` into `JwtSecurity` and call `context.close()` instead of `System.exit(1)` on line 325, and also call it in the timeout path:

```java
// Add field (Lombok @RequiredArgsConstructor or manual injection):
private final ConfigurableApplicationContext applicationContext;

// Replace System.exit(1) at line 325:
// Before:
System.exit(1); // TODO remove
// After:
apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", StringUtils.join(events, "\n"));
applicationContext.close();

// Replace the timer task body at line 274–280:
// Before:
if (!zosmfListener.isZosmfReady()) {
    synchronized (events) {
        apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", StringUtils.join(events, "\n"));
    }
    apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", zosmfServiceId);
}
// After:
if (!zosmfListener.isZosmfReady()) {
    synchronized (events) {
        apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", StringUtils.join(events, "\n"));
    }
    apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", zosmfServiceId);
    applicationContext.close();
}
```

**Change 2** — Remove the duplicate `apimlLog.log(...)` call that now precedes `applicationContext.close()` in the listener path (line 322–323 is already logging; don't double-log).

**Tests:** Extend `JwtSecurityTest` with a test that mocks `providers.isZosmfAvailableAndOnline()` to always return `false`, fires the timer, and asserts `applicationContext.close()` was called. Also test the listener path where `validateInitializationAgainstZosmf()` throws `HttpsConfigError`.

---

## 9. #4350 — Inconsistent serviceId validators (uppercase allowed in static API)

**File:** `api-catalog-services/src/main/java/org/zowe/apiml/apicatalog/staticapi/StaticDefinitionGenerator.java:38`

**Root cause:** `StaticDefinitionGenerator` uses pattern `^[A-Za-z][A-Za-z0-9-]*$` (line 38) which allows uppercase letters and has no maximum length. The discovery service uses `EurekaUtils.SERVICE_ID_PATTERN` = `^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$` (lowercase only, max 63 chars, min 2 chars). A serviceId accepted by the static API endpoint is silently rejected by discovery.

`EurekaUtils.SERVICE_ID_PATTERN` is a `Pattern` constant (confirmed at `common-service-core/src/main/java/org/zowe/apiml/util/EurekaUtils.java:35`), so this is a direct replacement.

**Change:** Replace the pattern on line 38 and reuse the existing constant:

```java
// Before (line 38):
private static final Pattern p = Pattern.compile("^[A-Za-z][A-Za-z0-9-]*$");

// After:
private static final Pattern p = EurekaUtils.SERVICE_ID_PATTERN;
```

Add the import for `EurekaUtils` if not already present. Also update the error message returned by `getInvalidResponse()` to describe the actual constraint (lowercase letters and digits, hyphens allowed in the middle, 2–63 characters).

**Tests:** Extend `StaticDefinitionGeneratorTest` to assert that:
- `"MyService"` (uppercase) is now rejected.
- `"my-service"` (valid lowercase with hyphen) is accepted.
- `"a"` (single char — invalid per EurekaUtils min-2-char rule) is rejected.
- `"my"` (exactly 2 chars — valid lower bound) is accepted.

---

## 10. #3097 — Login returns full SAF errno detail in response body

**Files:**
- `apiml-security-common/src/main/java/org/zowe/apiml/security/common/error/AuthExceptionHandler.java:167–170`
- `apiml/src/main/java/org/zowe/apiml/controller/ApimlExceptionHandler.java:119–122`

**Root cause:** `handleZosAuthenticationException()` passes `ex.getMessage()` as a format argument, which contains the full SAF errno name and explanation (e.g. `"ESRCH: The identity that was specified is not defined to the security product"`). This detail is included verbatim in the JSON response body.

**Critical note on message keys:** The `PlatformPwdErrno.errorMessage` field holds a YAML message key (e.g. `"org.zowe.apiml.security.platform.errno.ESRCH"`). Those keys in `security-common-log-messages.yml` (lines 35–86) all have `text: "The platform returned error: %s"` — they each take exactly one `%s` argument. That argument is currently `ex.getMessage()` (the full SAF explanation). The fix must either supply a generic replacement argument or add parameter-free variants of those message keys.

**Change 1** — In `AuthExceptionHandler.java:167–170`, log the detail at DEBUG and pass a generic placeholder instead of `ex.getMessage()`:

```java
// Before:
private void handleZosAuthenticationException(BiConsumer<ApiMessageView, HttpStatus> function, ZosAuthenticationException ex) {
    final ApiMessageView message = messageService.createMessage(ex.getPlatformError().errorMessage, ex.getMessage()).mapToView();
    final HttpStatus status = ex.getPlatformError().responseCode;
    function.accept(message, status);
}

// After:
private void handleZosAuthenticationException(BiConsumer<ApiMessageView, HttpStatus> function, ZosAuthenticationException ex) {
    log.debug("Authentication failed with platform error: {}", ex.getMessage());
    // Pass the short error name only (e.g. "ESRCH"), not the full explanation
    final ApiMessageView message = messageService.createMessage(
        ex.getPlatformError().errorMessage, ex.getPlatformError().shortErrorName).mapToView();
    final HttpStatus status = ex.getPlatformError().responseCode;
    function.accept(message, status);
}
```

Using `ex.getPlatformError().shortErrorName` (e.g. `"ESRCH"`) is a pragmatic middle ground: it tells the operator which category of error occurred without embedding the full free-text explanation that confirms the user does or does not exist. If even the short name is considered too revealing, replace it with a fixed string like `"authentication error"`.

**Change 2** — Apply the same fix in `ApimlExceptionHandler.java:119–122`, which currently passes `ex.getMessage()` as the argument to the same message key template:

```java
// Before (line 121):
return setBodyResponse(exchange, ex.getPlatformError().responseCode.value(),
    Optional.ofNullable(ex.getPlatformError()).map(e -> e.errorMessage).orElse(null), ex.getMessage());
// After:
return setBodyResponse(exchange, ex.getPlatformError().responseCode.value(),
    Optional.ofNullable(ex.getPlatformError()).map(e -> e.errorMessage).orElse(null),
    ex.getPlatformError().shortErrorName);
```

**Tests:** Add a test that throws a `ZosAuthenticationException` with `PlatformPwdErrno.ESRCH` and asserts the response body does NOT contain the explanation text `"not defined to the security product"`, while still containing the short name `"ESRCH"` or a generic phrase.

---

## 11. #3007 — Specific errors still leak into `X-Zowe-Auth-Failure` header

**Files:**
- `gateway-service/src/main/java/org/zowe/apiml/gateway/filters/AbstractAuthSchemeFactory.java:182–183`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/filters/AbstractAuthSchemeFactory.java:254–256`
- Any callers of `updateHeadersForError()` or `cleanHeadersOnAuthFail()` that pass `e.getMessage()` directly

**Root cause:** Three places pass raw exception message text into the `X-Zowe-Auth-Failure` response header:
1. Line 183: `"Invalid client certificate in request. Error message: " + e.getMessage()` on `CertificateEncodingException`.
2. Lines 254–256: `updateHeadersForError(exchange, errorMessage)` — callers pass arbitrary strings.
3. `AuthExceptionHandler.handleNoMainframeIdentity()` passes `ex.getMessage()` directly (line 207).

**Change 1** — On line 183, replace the raw message with a generic fixed string:
```java
// Before:
exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER,
    "Invalid client certificate in request. Error message: " + e.getMessage());

// After:
exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER,
    messageService.createMessage("org.zowe.apiml.security.common.invalidCertificate").mapToLogMessage());
```
Add message key `org.zowe.apiml.security.common.invalidCertificate` with text `"The client certificate in the request is invalid."` to the appropriate message YAML (e.g. `zaas-log-messages.yml` or `security-common-log-messages.yml`).

**Change 2** — In `AuthExceptionHandler.handleNoMainframeIdentity()` (line 207), replace `ex.getMessage()` with a generic message:
```java
// Before:
addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, ex.getMessage());
// After:
addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER,
    this.messageService.createMessage("org.zowe.apiml.security.common.identityMappingFailed").mapToLogMessage());
```

**Change 3** — Audit all direct callers of `updateHeadersForError()` and `cleanHeadersOnAuthFail()` in the gateway to ensure none pass `e.getMessage()` or other internal detail strings. Replace any that do with a message-service-resolved generic string.

**Tests:** For each changed site, add a test that triggers the relevant exception path and asserts the `X-Zowe-Auth-Failure` response header value does not contain raw internal error text (e.g. does not match `".*Error message:.*"` or `".*ESRCH.*"`).

---

## 12. #4273 — PassTicket error message doesn't include userId and appName

**Files:**
- `common-service-core/src/main/java/org/zowe/apiml/passticket/IRRPassTicketGenerationException.java`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/zaas/ZaasExceptionHandler.java:55–64`
- `zaas-service/src/main/resources/zaas-log-messages.yml` (message key `org.zowe.apiml.security.ticket.generateFailed`)
- Throw sites in passticket generation code (search for `new IRRPassTicketGenerationException(`)

**Root cause:** `IRRPassTicketGenerationException` stores only `safRc`, `racfRc`, `racfRsn`. Neither `userId` nor `applicationName` is captured. The message template `"The generation of the PassTicket failed. Reason: %s"` thus cannot include them.

**Change 1** — Add `userId` and `applId` fields to `IRRPassTicketGenerationException`:
```java
public class IRRPassTicketGenerationException extends AbstractIRRPassTicketException {

    private final String userId;
    private final String applId;

    public IRRPassTicketGenerationException(int safRc, int racfRc, int racfRsn, String userId, String applId) {
        super(safRc, racfRc, racfRsn);
        this.userId = userId;
        this.applId = applId;
    }

    public IRRPassTicketGenerationException(ErrorCode errorCode, String userId, String applId) {
        super(errorCode);
        this.userId = userId;
        this.applId = applId;
    }

    // keep backward-compat constructors delegating with nulls
    public IRRPassTicketGenerationException(int safRc, int racfRc, int racfRsn) {
        this(safRc, racfRc, racfRsn, null, null);
    }

    @Override
    public String getMessage() {
        return getMessage("Error on generation of PassTicket:");
    }

    public String getUserId()  { return userId; }
    public String getApplId()  { return applId; }
}
```

**Change 2** — Update all throw sites in the passticket generation code to pass the `userId` and `applId` that are already available at that point.

**Change 3** — Update the message template in `core-log-messages.yml` (the correct file — confirmed at `common-service-core/src/main/resources/core-log-messages.yml:55–61`; the key `org.zowe.apiml.security.ticket.generateFailed` lives there, not in `zaas-log-messages.yml`):
```yaml
text: "The generation of the PassTicket failed for user '%s' and application '%s'. Reason: %s"
```

**Change 4** — Update `ZaasExceptionHandler.handlePassTicketException()` (line 58) to pass the new arguments:
```java
ApiMessageView messageView = messageService.createMessage(
    "org.zowe.apiml.security.ticket.generateFailed",
    ex.getUserId() != null ? ex.getUserId() : "unknown",
    ex.getApplId() != null ? ex.getApplId() : "unknown",
    ex.getErrorCode().getMessage()
).mapToView();
```

**Change 5** — Update all throw sites (confirmed in `PassTicketService.java` and test files). The `IRRPassTicket.generate(userId, applId)` interface already receives both parameters at the call site, so updating those throw sites to `new IRRPassTicketGenerationException(safRc, racfRc, racfRsn, userId, applId)` is straightforward.

**Tests:**
- Update `ZaasExceptionHandlerTest` to assert the response body contains the userId and applId strings.
- Update existing tests in `IRRPassTicketGenerationExceptionTest`, `TokenCreationServiceTest`, and `SchemeControllerTest` that construct `new IRRPassTicketGenerationException(8, 8, 8)` to use the new three-arg constructor (backward-compat constructor remains, so no forced migration — but add coverage for the new five-arg form).

---

## 13. #4163 — ZWEAG121E triggered for certificate-not-mapped case (wrong message)

**Files:**
- `apiml-security-common/src/main/java/org/zowe/apiml/security/common/login/ShouldBeAlreadyAuthenticatedFilter.java`
- `apiml-security-common/src/main/java/org/zowe/apiml/security/common/error/AuthExceptionHandler.java:72–116`
- A message YAML file (e.g. `zaas-log-messages.yml` or `security-common-log-messages.yml`)

**Root cause:** When x509 certificate-to-user mapping returns `null`, the security chain falls through to `ShouldBeAlreadyAuthenticatedFilter`, which throws `AuthenticationCredentialsNotFoundException`. This maps to ZWEAG121E ("Authorization header is missing") — entirely wrong for a certificate scenario.

**Exact location confirmed:** `X509AuthenticationProvider.authenticate()` at `zaas-service/src/main/java/org/zowe/apiml/zaas/security/login/x509/X509AuthenticationProvider.java:63–65`. When `getUserid()` returns `null` (i.e. `x509AuthenticationMapper.mapToMainframeUserId()` returns `null`), the method returns `null` at line 65, which flows to `ShouldBeAlreadyAuthenticatedFilter` and triggers ZWEAG121E. The fix must be applied here.

**Change 1** — Instead of returning `null` when mapping fails, throw a dedicated `CertificateNotMappedException`:
```java
// Before (X509AuthenticationProvider.java:62–65):
String username = getUserid(authentication);
if (username == null) {
    log.debug("Mapping user to certificate was not successful.");
    return null;
}

// After:
String username = getUserid(authentication);
if (username == null) {
    log.debug("Mapping user to certificate was not successful.");
    throw new CertificateNotMappedException("Certificate could not be mapped to a mainframe identity.");
}
```

**Change 2** — Create `CertificateNotMappedException` extending `AuthenticationException` in `apiml-security-common`:
```java
public class CertificateNotMappedException extends AuthenticationException {
    public CertificateNotMappedException(String msg) { super(msg); }
}
```

**Change 3** — Add a handler entry in `AuthExceptionHandler.exceptionHandlers` map:
```java
entry(CertificateNotMappedException.class,
    (ex, ctx) -> handleCertificateNotMapped(ctx.requestUri, ctx.function, ex)),
```

**Change 4** — Implement `handleCertificateNotMapped()` using a new message key:
```java
private void handleCertificateNotMapped(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, CertificateNotMappedException ex) {
    log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
    writeErrorResponse("org.zowe.apiml.security.common.certificateNotMapped", HttpStatus.UNAUTHORIZED, function, requestUri);
}
```

**Change 5** — Add the message key to `zaas-log-messages.yml`. Use number **ZWEAG171** — numbers ZWEAG160–ZWEAG162 and ZWEAG169–ZWEAG170 are already taken (confirmed in `zaas-service/src/main/resources/zaas-log-messages.yml`), and ZWEAG167 is taken in gateway; ZWEAG171 is the next free slot in the ZAAS range:
```yaml
- key: org.zowe.apiml.security.common.certificateNotMapped
  number: ZWEAG171
  type: ERROR
  text: "The provided certificate could not be mapped to a mainframe user identity."
  reason: "No identity mapping exists for the supplied certificate in the configured security product."
  action: "Contact your security administrator to map the certificate to a valid mainframe user identity."
```

**Tests:** Add a test that simulates an x509 login with a certificate that has no mainframe mapping and asserts the response uses the new message code, not ZWEAG121E.

---

## 14. #4451 — Salt stored as raw `String` instead of Base64

**File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java:157–201`

**Root cause:** `storeSalt(byte[])` at line 199–200 converts 16 raw bytes to a `String` using `new String(salt)` (platform default charset). Binary bytes that map to control characters produce a shorter string, so the round-trip `getSalt()` → `initializeSalt().getBytes()` does not recover the original 16 bytes.

**Change:** Encode with Base64 when storing and decode with Base64 when reading:

```java
// storeSalt (line 199):
private void storeSalt(byte[] salt) throws CachingServiceClientException {
    cachingServiceClient.create(new CachingServiceClient.KeyValue("salt",
        Base64.getEncoder().encodeToString(salt)));
}

// initializeSalt (line 171):
// Replace: localSalt = new String(newSalt);
// With:    localSalt = Base64.getEncoder().encodeToString(newSalt);

// getSalt (line 195–196):
public byte[] getSalt() throws CachingServiceClientException {
    return Base64.getDecoder().decode(initializeSalt());
}
```

**Migration concern:** Existing stored salts (in raw-String form) will be unreadable by the new Base64 decoder. Add a migration guard in `getSalt()`: attempt Base64 decode; if it throws `IllegalArgumentException`, the stored value is a legacy raw string. In that case, generate a new salt, overwrite it in Base64, and return the new salt. Overwriting is safe because all ZAAS instances that were using the old salt will roll their own new salt in the same way during their next call — there is a brief window where different instances use different salts, causing tokens signed before the migration to fail validation. This is acceptable since the alternative (permanently broken salt) is worse. Document this one-time migration event in the release notes.

**Tests:**
- Update `ApimlAccessTokenProviderTest` to assert `getSalt()` returns exactly 16 bytes.
- Add a test that stores a salt containing all byte values 0–255 and verifies the round-trip is lossless.
- Add a test for the migration path where the stored value is a legacy non-Base64 string.

---

## 15. #4421 — Salt initialization is not atomic

**File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java:157–175`

**Root cause:** `initializeSalt()` performs a read-then-create sequence with no locking. Concurrent threads or simultaneous cluster startups can each generate independent salts, causing token validation inconsistencies.

**Note:** This fix depends on #4451 (Base64 encoding) being applied first, since the return value must be consistent. Apply #14 before #15 in any implementation sequence, or apply both in the same commit.

**Change:** Add a `synchronized` block around the generate+store path, and handle the "already exists" conflict from the caching service (a 409-like `StorageException`) by re-reading the value. The `synchronized` on `ApimlAccessTokenProvider.class` guards against races between threads on the same JVM instance; the try-on-conflict-read-again path guards against races between cluster nodes:

```java
String initializeSalt() throws CachingServiceClientException, SecureTokenInitializationException {
    try {
        CachingServiceClient.KeyValue keyValue = cachingServiceClient.read("salt");
        return keyValue.getValue();
    } catch (CachingServiceClientException | StorageException e) {
        log.debug("Cannot read salt, attempting to initialize.", e);
        if (e.getCause() != null) {
            throw e;  // real I/O error, propagate
        }
    }

    // Salt does not exist yet — synchronize to avoid race between threads on this instance
    synchronized (ApimlAccessTokenProvider.class) {
        // Re-check after acquiring lock (another thread may have stored it)
        try {
            CachingServiceClient.KeyValue keyValue = cachingServiceClient.read("salt");
            return keyValue.getValue();
        } catch (CachingServiceClientException | StorageException ignored) {
            // still not present, proceed to create
        }

        byte[] newSalt = generateSalt();
        try {
            storeSalt(newSalt);
        } catch (CachingServiceClientException conflict) {
            // Another cluster node stored salt first — read theirs
            log.debug("Salt already stored by a peer, reading existing salt.");
            return cachingServiceClient.read("salt").getValue();
        }
        return Base64.getEncoder().encodeToString(newSalt);  // consistent with #4451 fix
    }
}
```

**Tests:**
- Add a multi-threaded test that calls `initializeSalt()` concurrently from 10 threads and asserts all threads return the same salt value.
- Add a test that simulates the caching service returning a conflict exception on `create` and asserts the method falls back to reading the existing value.

---

## 16. #4418 — Catalog tile still shown and populated in registration

**Files:**
- `api-catalog-ui/frontend/src/components/Wizard/configs/wizard_non_java_categories.jsx:73–76`
- `api-catalog-ui/frontend/src/components/Wizard/configs/wizard_base_categories.jsx:224,245`
- `onboarding-enabler-java/src/main/java/org/zowe/apiml/eurekaservice/client/util/EurekaInstanceConfigCreator.java:113–122`
- `onboarding-enabler-java/src/main/java/org/zowe/apiml/eurekaservice/client/util/EurekaInstanceConfigValidator.java:52–54`

**Root cause:** API Catalog v3 removed tile-based grouping, but (a) the wizard UI still shows a "Catalog" / tile section, and (b) `EurekaInstanceConfigCreator` still writes tile metadata into registration, and (c) the validator still warns when tile is missing.

**Change 1** — Remove the tile/catalog section from both wizard config files. Delete the object entries that define tile-related form fields (`id`, `title`, `description`, `version` under the catalog grouping).

**Change 2** — Remove the tile metadata writes from `EurekaInstanceConfigCreator.java:113–122`. Delete or comment out the block that sets `apiml.catalog.tile.id`, `apiml.catalog.tile.title`, etc. on the instance metadata map.

**Change 3** — Remove the tile validation warning from `EurekaInstanceConfigValidator.java:52–54`.

**Change 4** — Keep the `Catalog` and `Tile` model classes for backward compatibility with existing service registrations that may still send tile metadata, but stop generating or requiring them.

**Tests:**
- Update `EurekaInstanceConfigCreatorTest` to assert that tile metadata is no longer set on the instance.
- Update `EurekaInstanceConfigValidatorTest` to assert no warning is emitted when `catalog` is null.
- Update wizard snapshot/unit tests to reflect the removed tile section.

---

## 17. #4282 — API Catalog shows OAS 3.1 schemas with wrong styles

**Files:**
- `api-catalog-ui/frontend/src/components/Swagger/Swagger.css`
- `api-catalog-ui/frontend/src/components/Swagger/_swagger.scss`

**Root cause:** swagger-ui v5 renders OAS 3.1 schema sections using a different DOM structure (class names and nesting) than OAS 2.0/3.0. The existing CSS overrides in `Swagger.css` target the old DOM structure and do not match OAS 3.1's rendered output.

**Investigation step (required before writing CSS):** Render an OAS 3.1 spec (e.g. the Gateway's own swagger) in swagger-ui v5 (currently v5.31.0) in a browser. Inspect the DOM of the Schemas section to identify the actual class names used by v5 for OAS 3.1 (look for `.models`, `.model-container`, `.schemas-wrapper`, or similar).

**Change:** Add new CSS rules in `_swagger.scss` that correctly style the OAS 3.1 schema section. The exact selectors depend on the DOM inspection above, but the general approach is:

```scss
// In _swagger.scss — add after existing model styles:
// OAS 3.1 schema section rendered by swagger-ui v5
.swagger-ui .schemas-wrapper,
.swagger-ui section.models.oas3 {
  // apply same background, border, padding as the existing .swagger-ui section.models rules
  border: 1px solid rgba(59, 65, 81, 0.3);
  border-radius: 4px;
  margin: 0 0 20px;

  h4 {
    // match existing heading styles
  }
}
```

**Tests:** Add a Cypress test that opens the Gateway API documentation page (which uses OAS 3.1) and asserts the Schemas section has non-zero dimensions and is visually non-broken (e.g. has correct background color, no overlapping elements). A snapshot test of the rendered Schemas section would also serve as a regression guard.

---

## 18. #4478 — `Origin` header forwarded to z/OSMF when CORS is disabled

**File:** `gateway-service/src/main/java/org/zowe/apiml/gateway/config/RoutingConfig.java:26–27,58–65`

**Root cause:** The `RemoveRequestHeader` filter definitions at lines 58–65 iterate `ignoredHeadersWhenCorsEnabled`. This property has a default value in `application.yml:108` that already includes `Origin` in its list — **but only when CORS is enabled** (`corsEnabled: true`, also the default). When a deployment explicitly sets `corsEnabled: false`, the property is overridden to an empty string, so the `Origin` header is no longer stripped and flows through to z/OSMF, which rejects it with IZUG846W.

**Change:** Add an unconditional `RemoveRequestHeader` filter for `Origin` regardless of CORS setting. The safest place is inside `commonNoRetryFilters()`, before the CORS-conditional loop:

```java
@Bean
public List<FilterDefinition> commonNoRetryFilters() {
    List<FilterDefinition> filters = new ArrayList<>();

    // ... existing filters (acceptForwardedCert, encodedSlashes, secureHeaders, circuitBreaker) ...

    // Always strip Origin to prevent z/OSMF remote-site rejections (IZUG846W)
    FilterDefinition removeOrigin = new FilterDefinition();
    removeOrigin.setName("RemoveRequestHeader");
    removeOrigin.setArgs(Map.of("name", "Origin"));
    filters.add(removeOrigin);

    // Conditionally strip CORS-related headers when CORS is enabled
    for (String headerName : ignoredHeadersWhenCorsEnabled.split(",")) {
        if (!headerName.isBlank()) {
            FilterDefinition removeHeaders = new FilterDefinition();
            removeHeaders.setName("RemoveRequestHeader");
            removeHeaders.setArgs(Map.of("name", headerName));
            filters.add(removeHeaders);
        }
    }

    return filters;
}
```

**Note:** If `Origin` is already included in `ignoredHeadersWhenCorsEnabled` for CORS-enabled deployments, add a guard to avoid adding a duplicate filter definition.

**Tests:** Add a test in `RoutingConfigTest` that instantiates the config with `corsEnabled=false` (empty `ignoredHeadersWhenCorsEnabled`) and asserts the resulting `commonNoRetryFilters` list contains a `RemoveRequestHeader` filter for `"Origin"`.

---

## 19. #4510 — Unnecessary caching-service call in `DeterministicLoadBalancer`

**File:** `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/DeterministicLoadBalancer.java:82–105`

**Root cause:** `get()` calls `cache.retrieve(user, serviceId)` (line 98) unconditionally for all authenticated requests, before `filterInstances()` → `shouldIgnore()` → `lbTypeIsAuthentication()` checks whether this service uses authentication-based load balancing at all. The `shouldIgnore()` check depends on knowing the service instances' metadata, which is already available from `delegate.get(request)` at line 89.

**Change:** Reorder the reactive chain so that the `lbTypeIsAuthentication` check runs first, and `cache.retrieve()` is only called when the service is actually configured for authentication-based LB:

```java
@Override
public Flux<List<ServiceInstance>> get(Request request) {
    String serviceId = getServiceId();
    if (serviceId == null) return Flux.empty();

    AtomicReference<String> principal = new AtomicReference<>();
    return delegate.get(request)
        .flatMap(serviceInstances -> {
            // Short-circuit early: if this service doesn't use auth-based LB, skip cache entirely
            if (serviceInstances.isEmpty() || !lbTypeIsAuthentication(serviceInstances.get(0))) {
                return just(serviceInstances);
            }

            return getSub(request.getContext())
                .switchIfEmpty(Mono.just(""))
                .flatMap(user -> {
                    if (user == null || user.isEmpty()) {
                        log.debug("No authentication on request, not filtering: {}", serviceId);
                        return empty();
                    } else {
                        principal.set(user);
                        return cache.retrieve(user, serviceId).onErrorResume(t -> Mono.empty());
                    }
                })
                .switchIfEmpty(Mono.just(LoadBalancerCacheRecord.NONE))
                .flatMapMany(cacheRecord -> filterInstances(principal.get(), serviceId, cacheRecord, serviceInstances, request.getContext()));
        })
        .doOnError(e -> log.debug("Error in determining service instances", e));
}
```

**Important:** The `shouldIgnore()` call at line 164 of `filterInstances()` also handles the `X-InstanceId` header path (lines 165–170): when the service does not use authentication-based LB, it still checks for an explicit instance pinning header. The proposed early-exit short-circuit at the top of `get()` must replicate that path — returning `just(serviceInstances)` unfiltered is correct only if there is no `X-InstanceId` header. Check `getInstanceId(requestContext)` and, if a header is present, delegate to `checkInstanceIdHeader()` even for non-auth-LB services. Update the short-circuit as:

```java
if (serviceInstances.isEmpty() || !lbTypeIsAuthentication(serviceInstances.get(0))) {
    // Still honour explicit instance pinning via X-InstanceId header
    var instanceId = getInstanceId(request.getContext());
    if (instanceId != null) {
        return just(checkInstanceIdHeader(instanceId, serviceInstances));
    }
    return just(serviceInstances);
}
```

The `shouldIgnore()` call inside `filterInstances()` can then be removed since its job is now done at the top of `get()`.

**Tests:**
- Add a test with a service whose metadata does NOT have `apiml.lb.type=authentication` and assert that `cache.retrieve()` is never called.
- Confirm existing tests for the authentication-LB path still pass.

---

## 20. #4511 — Sticky session: requests stuck routing to dead instance

**Files:**
- `gateway-service/src/main/java/org/zowe/apiml/gateway/loadbalancer/DeterministicLoadBalancer.java:235–243`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/config/NettyRoutingFilterApiml.java` (for the downstream retry hook)

**Root cause:** `chooseOne(instanceId, user, serviceInstances)` at line 240 falls back to `serviceInstances.get(0)` only after Eureka has evicted the dead instance (up to 90 s). During that window, `serviceInstances` still contains the dead instance, the filter matches it, and all requests for that user go to the dead endpoint.

**Change 1** — After selecting the preferred instance in `chooseOne()`, do not immediately cache and return. Instead, store the selection in the exchange attributes and let the downstream error path (in `NettyRoutingFilterApiml`) invalidate the cached preference on `ServiceNotAccessibleException`, then retry with a different instance:

```java
// In chooseOne(), add the instance to exchange attributes for downstream inspection:
// (Pass exchange or a callback through the reactive chain — or use a thread-local/Mono context key)
```

**Change 2** — A simpler alternative that avoids touching the routing pipeline: in `filterInstances()`, after selecting the cached instance, add a reactive health pre-check. If `serviceInstances` contains the preferred instance but it has been marked as `DOWN` in its Eureka status, skip it and call `chooseOne(user, serviceInstances)` (pick a new one) and update the cache:

```java
} else if (isNotBlank(cacheRecord.getInstanceId())) {
    boolean preferredStillUp = serviceInstances.stream()
        .anyMatch(i -> cacheRecord.getInstanceId().equals(i.getInstanceId())
                    && InstanceStatus.UP.name().equals(i.getMetadata().get("status")));
    if (preferredStillUp) {
        result = chooseOne(cacheRecord.getInstanceId(), user, serviceInstances);
    } else {
        // Preferred instance is down; evict cache and pick a new one
        result = cache.delete(user, serviceId).thenMany(chooseOne(user, serviceInstances));
    }
}
```

**Note on Eureka metadata:** Spring Cloud's `EurekaServiceInstance.getMetadata()` returns the Eureka instance metadata map, which does not directly expose the instance's `InstanceStatus` (UP/DOWN/OUT_OF_SERVICE). The `serviceInstances` list supplied by the delegate already excludes instances that Eureka has marked as DOWN — so the simpler check is: if the cached `instanceId` is not found anywhere in `serviceInstances`, the instance has been evicted, and the fallback `serviceInstances.get(0)` is already safe. The bug window is when the dead instance is still in the `UP` list but unresponsive. For that window, a Eureka-status check is insufficient; the only reliable fix is the post-routing error path described in Change 1 (invalidate cache on `ServiceNotAccessibleException`).

**Tests:**
- Add a test where the `serviceInstances` list contains the preferred instance with a DOWN status and assert a different instance is selected and the cache is updated.
- Add a test where the preferred instance is not in `serviceInstances` at all (already evicted) and assert fallback selection.

---

## 21. #4149 — Discovery debug mode floods ZAAS with `CacheRefreshedEvent`

**File:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/JwtSecurity.java:303–331`

**Root cause:** `zosmfRegisteredListener.onEvent()` calls `providers.isZosmfAvailableAndOnline()` — an HTTP call — on every single `CacheRefreshedEvent`. When the Discovery service is in debug mode, these events are emitted many times per second, exhausting threads or causing timeout cascades that prevent ZAAS from completing startup.

**Change:** Add a debounce inside the listener: record the last time a z/OSMF reachability check was performed, and skip repeated calls within a minimum interval (e.g. the value of `apiml.startupCheckInterval`, default 15 s):

```java
private volatile long lastZosmfCheckAt = 0;
private static final long MIN_CHECK_INTERVAL_MS = 5_000; // 5 seconds minimum between checks

private final EurekaEventListener zosmfRegisteredListener = new EurekaEventListener() {
    @Override
    public void onEvent(EurekaEvent event) {
        if (!(event instanceof CacheRefreshedEvent)) return;

        long now = System.currentTimeMillis();
        if (now - lastZosmfCheckAt < MIN_CHECK_INTERVAL_MS) {
            return;  // debounce: skip this event
        }
        lastZosmfCheckAt = now;

        events.add("Discovery Service Cache was updated.");
        log.debug("Trying to reach the z/OSMF instance {}.", zosmfServiceId);

        if (providers.isZosmfAvailableAndOnline()) {
            // ... existing success path unchanged ...
        } else {
            events.add("z/OSMF instance " + zosmfServiceId + " is not available and online yet.");
        }
    }
};
```

Make `MIN_CHECK_INTERVAL_MS` configurable. The existing property `apiml.startupCheckInterval` (default 15, in seconds) already controls the `ZaasStartupListener` polling interval and is the natural value to reuse here: `MIN_CHECK_INTERVAL_MS = startupCheckInterval * 1000L`. Inject it via `@Value("${apiml.startupCheckInterval:15}")` (already present in `ZaasStartupListener`) and expose it to `JwtSecurity` through a constructor parameter or shared configuration property.

**Tests:** Add a test that fires `CacheRefreshedEvent` 100 times in rapid succession and asserts `providers.isZosmfAvailableAndOnline()` was called at most `ceil(test_duration / MIN_CHECK_INTERVAL_MS)` times.

---

## 22. #3226 — Confusing error message for x509 + revoked user

**Files:**
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/zosmf/ZosmfService.java:360,375`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/filters/AbstractAuthSchemeFactory.java` (generic error message path)
- A message YAML

**Root cause:** When z/OSMF returns 401 due to a revoked user, the error propagates back as a generic "invalid authentication" message. The two `// TODO` comments at lines 360 and 375 in `ZosmfService.java` confirm this is a known unresolved case.

**Change 1** — In `ZosmfService.java`, inspect the z/OSMF 401 response body for the SAF error indicators (e.g. the returnCode `8` with a reason code that indicates a revoked user). When detected, throw a specific exception (e.g. `RevokedUserException`) instead of a generic `TokenNotValidException`:

```java
// Where z/OSMF 401 is currently handled (around lines 360, 375):
if (httpStatus == 401) {
    if (isRevokedUserError(responseBody)) {
        throw new RevokedUserException("Authentication rejected: the user account has been revoked.");
    }
    throw new TokenNotValidException("Token is not valid.");
}
```

**Change 2** — Create `RevokedUserException extends AuthenticationException`.

**Change 3** — Add a handler in `AuthExceptionHandler` / `ZaasExceptionHandler` that maps `RevokedUserException` to a dedicated message key. Note: a message for revoked access already exists — `org.zowe.apiml.security.platform.errno.EMVSSAFEXTRERR` (ZWEAT414) covers "username access has been revoked" for BPX4PWD-based password login. However, the x509 path goes through `ZosmfService` and does not produce a `ZosAuthenticationException`, so that key is never reached. Add a new key in `zaas-log-messages.yml` using the next free number (ZWEAG171 is proposed for #13; use **ZWEAG172** here):
```yaml
- key: org.zowe.apiml.security.common.revokedUser
  number: ZWEAG172
  type: ERROR
  text: "Authentication failed. The user account is revoked."
  reason: "The security product (SAF) has revoked the user account associated with the provided certificate."
  action: "Contact your security administrator to reinstate the user account."
```

**Tests:** Add a test that mocks z/OSMF to return a 401 with a revoked-user response body and asserts the API ML error response uses the new message code.

---

## 23. #4193 — Invalidated tokens not redistributed to restarted instances

**Files:**
- `zaas-service/src/main/java/org/zowe/apiml/zaas/ZaasStartupListener.java:37–59`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/AuthenticationService.java:391–407`

**Root cause:** When a ZAAS instance restarts, it starts with an empty in-memory token invalidation cache. The `distributeInvalidate(toInstanceId)` endpoint exists but is never called at startup by the restarting instance itself.

**Change:** After startup is complete (i.e., after `notifyStartup()` is called in `ZaasStartupListener`), retrieve the list of all other ZAAS instances from Eureka and call `/auth/distribute/{myInstanceId}` on each peer to ask them to push their invalidated token lists to the newly started instance:

```java
public void notifyStartup() {
    handler.onServiceStartup("ZAAS", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    publisher.publishEvent(new ZaasServiceAvailableEvent(providers.isZosfmUsed() ? "zosmf" : "saf"));
    requestTokenRedistribution();  // <-- add this
}

private void requestTokenRedistribution() {
    String myInstanceId = eurekaClient.getApplicationInfoManager().getInfo().getInstanceId();
    Application zaas = eurekaClient.getApplication(CoreService.ZAAS.getServiceId());
    if (zaas == null) return;

    zaas.getInstances().stream()
        .filter(i -> !myInstanceId.equals(i.getInstanceId()))
        .forEach(peer -> {
            String url = EurekaUtils.getUrl(peer)
                + AuthController.CONTROLLER_PATH + "/distribute/" + myInstanceId;
            try {
                restTemplate.getForObject(url, Void.class);
            } catch (Exception e) {
                log.warn("Could not request token redistribution from peer {}: {}", peer.getInstanceId(), e.getMessage());
            }
        });
}
```

Inject `EurekaClient` and `RestTemplate` into `ZaasStartupListener` (both are available as Spring beans).

**Tests:**
- Add a test that mocks `EurekaClient.getApplication()` returning two peer instances and verifies `restTemplate.getForObject()` is called once per peer with the correct URL.
- Add a test for the case where there are no peers and assert no HTTP calls are made.

---

## 24. #3976 — ZAAS 401 (invalid PAT scope) not distinguished from ZAAS unavailable

**Files:**
- `zaas-service/src/main/java/org/zowe/apiml/zaas/zaas/ZaasExceptionHandler.java` (or the relevant ZAAS scheme endpoint)
- `gateway-service/src/main/java/org/zowe/apiml/gateway/filters/ZaasSchemeTransformRest.java:83–91`
- `gateway-service/src/main/java/org/zowe/apiml/gateway/filters/AbstractAuthSchemeFactory.java`

**Root cause:** In `ZaasSchemeTransformRest.requestWithHa()` (line 84–85), a ZAAS `SC_UNAUTHORIZED` response is handled as `new AuthorizationResponse<>(headers, null)` — an empty response. Both "invalid PAT scope" (a valid ZAAS 401) and ZAAS being down (which surfaces as a different error) end up in the same `processResponse()` path that strips credentials and forwards the request. The caller never receives a direct 401.

**Change 1** — Add a custom response header (e.g. `X-Zowe-Auth-Error-Code`) to the ZAAS 401 response that distinguishes "invalid token/scope" from other 401 causes. In the ZAAS `ZaasExceptionHandler`, set this header:
```java
// When returning 401 for invalid PAT scope:
response.addHeader("X-Zowe-Auth-Error-Code", "TOKEN_INVALID_SCOPE");
```

**Change 2** — In `ZaasSchemeTransformRest.requestWithHa()` (line 84–85), inspect the `X-Zowe-Auth-Error-Code` header on the 401 response. If it indicates a token problem (not a ZAAS infrastructure issue), propagate the 401 directly to the caller rather than returning an empty `AuthorizationResponse`:

```java
case SC_UNAUTHORIZED -> {
    String errorCode = clientResp.headers().header("X-Zowe-Auth-Error-Code").stream().findFirst().orElse("");
    if ("TOKEN_INVALID_SCOPE".equals(errorCode) || "TOKEN_NOT_VALID".equals(errorCode)) {
        // Token is definitively invalid — return 401 directly to the client
        yield Mono.error(new TokenNotValidException("Token rejected by ZAAS: " + errorCode));
    }
    // Unknown 401 — fall back to stripping credentials (existing behaviour)
    yield Mono.just(new AbstractAuthSchemeFactory.AuthorizationResponse<>(clientResp.headers(), null));
}
```

**Change 3** — Handle `TokenNotValidException` in the `GatewayExceptionHandler` to return HTTP 401 with a clear message (this likely already exists via `AbstractAuthSchemeFactory`'s error path).

**Tests:**
- Add a test in `ZaasSchemeTransformRestTest` where the mock ZAAS returns 401 with `X-Zowe-Auth-Error-Code: TOKEN_INVALID_SCOPE` and assert a `TokenNotValidException` is thrown.
- Add a test where the mock ZAAS returns 401 without the header and assert the existing stripped-credentials fallback is used.

---

## 25. #4143 — AT-TLS certificate read fails during HAFT failover (HTTP 500)

**File:** `apiml-tomcat-common/src/main/java/org/zowe/apiml/filter/AttlsHttpHandler.java:125–155`

**Root cause:** `updateCertificate()` is called on every request. During a HAFT failover, the AT-TLS context is briefly in a transient state, causing `CertificateFactory.generateCertificate()` to throw `CertificateException`. This is caught at line 148–151 and returns HTTP 500. There is no retry, no cache, and no backoff.

**Change:** Add a short retry loop (2–3 attempts, 50 ms apart) around the AT-TLS context read in the `postProcessAfterInitialization` lambda. If all retries fail, fall back to processing the request without the certificate (which will fail authentication) rather than returning a hard 500 — or keep the 500 but log it as a transient rather than a permanent error:

```java
// In the HttpHandler lambda (around line 130):
int maxAttempts = 3;
Exception lastException = null;
for (int attempt = 0; attempt < maxAttempts; attempt++) {
    try {
        var attlsContext = InboundAttls.get();
        if (attlsContext.getStatConn() != StatConn.SECURE) {
            return unsecureError(request, response);
        }
        var nativeRequest = ((AbstractServerHttpRequest) request).getNativeRequest();
        if (nativeRequest instanceof RequestFacade facade) {
            facade.setAttribute("attls", attlsContext);
            request = updateCertificate(request, facade, attlsContext.getCertificate());
        } else if (nativeRequest instanceof HttpServletRequestWrapper applicationRequest) {
            applicationRequest.setAttribute("attls", attlsContext);
            request = updateCertificate(request, applicationRequest, attlsContext.getCertificate());
        }
        lastException = null;
        break;  // success
    } catch (CertificateException | IoctlCallException | UnknownEnumValueException
             | ContextIsNotInitializedException | UnsatisfiedLinkError e) {
        lastException = e;
        if (attempt < maxAttempts - 1) {
            try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }
}
if (lastException != null) {
    log.warn("Cannot verify AT-TLS status after {} attempts, returning 500: {}", maxAttempts, lastException.getMessage());
    return internalError(request, response);
}
return httpHandler.handle(request, response);
```

**Tests:** Add a test that mocks `InboundAttls.get()` to throw `CertificateException` on the first two calls and succeed on the third, and assert the request is processed successfully. Also add a test that fails all attempts and asserts HTTP 500.

---

## 26. #4426 — Exception handling in Caching Service (no `@ControllerAdvice`)

**Files:**
- `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java`
- `caching-service/src/main/java/org/zowe/apiml/caching/service/` (all storage backends)
- New file: `caching-service/src/main/java/org/zowe/apiml/caching/exceptions/CachingServiceControllerAdvice.java`

**Root cause:** All storage errors use `StorageException` regardless of cause. The controller handles them inline with `exceptionToResponse()` and `handleInternalError()`, making it hard to reason about which HTTP status each scenario maps to.

**Change 1** — Introduce typed exception subclasses in the `caching-service/src/main/java/org/zowe/apiml/caching/exceptions/` package:
```
KeyNotFoundException extends StorageException       (404)
KeyAlreadyExistsException extends StorageException  (409)
StorageLimitException extends StorageException      (507)
```

**Change 2** — Update all storage backends (`InMemoryStorage`, `VsamStorage`, `InfinispanStorage`, `RedisStorage`) to throw the typed exceptions instead of the base `StorageException` where appropriate (e.g. throw `KeyNotFoundException` when a key is not found).

**Change 3** — Create a `@ControllerAdvice` class `CachingServiceControllerAdvice`:
```java
@ControllerAdvice
@RequiredArgsConstructor
public class CachingServiceControllerAdvice {

    @ExceptionHandler(KeyNotFoundException.class)
    public ResponseEntity<ApiMessageView> handleKeyNotFound(KeyNotFoundException ex) { ... /* 404 */ }

    @ExceptionHandler(KeyAlreadyExistsException.class)
    public ResponseEntity<ApiMessageView> handleKeyExists(KeyAlreadyExistsException ex) { ... /* 409 */ }

    @ExceptionHandler(StorageLimitException.class)
    public ResponseEntity<ApiMessageView> handleStorageLimit(StorageLimitException ex) { ... /* 507 */ }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiMessageView> handleStorage(StorageException ex) { ... /* 500 fallback */ }
}
```

**Change 4** — Remove the inline `exceptionToResponse()` and `handleInternalError()` methods from `CachingController` and let the `@ControllerAdvice` handle all exception-to-response mapping.

**Tests:** Add tests in `CachingControllerTest` for each exception type asserting the correct HTTP status and message key. Update existing tests that relied on the inline exception handling.

---

## 27. #4424 — PAT validation loads entire caching-service dataset per call

**Files:**
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/token/ApimlAccessTokenProvider.java:83–111`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/cache/CachingServiceClient.java`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/cache/LocalCachingClient.java` (modulith path)
- `caching-service/src/main/java/org/zowe/apiml/caching/api/CachingController.java` (endpoint verification)

**Root cause:** `isInvalidated()` calls `cachingServiceClient.readAllMaps()` which fetches all three maps (`invalidTokens`, `invalidUsers`, `invalidScopes`) in their entirety. This grows linearly with the number of stored records.

**Change:** Add targeted single-key lookup methods to `CachingServiceClient` and use them in `isInvalidated()`:

```java
// CachingServiceClient — add:
public Optional<String> readFromMap(String mapKey, String recordKey) throws CachingServiceClientException {
    // GET /cachingservice/api/v1/cache-list/{mapKey}/{recordKey}
    // Return Optional.empty() on 404, Optional.of(value) on 200
}
```

**Update `isInvalidated()` in `ApimlAccessTokenProvider`:**
```java
public boolean isInvalidated(String token) throws CachingServiceClientException {
    byte[] salt = getSalt();
    QueryResponse parsedToken = authenticationService.parseJwtWithSignature(token);
    String hashedToken   = getHash(token, salt);
    String hashedUserId  = getHash(parsedToken.getUserId().trim().toUpperCase(), salt);

    // 1. Check if the token itself is invalidated
    if (cachingServiceClient.readFromMap(INVALID_TOKENS_KEY, hashedToken).isPresent()) {
        return true;
    }
    // 2. Check if the user is invalidated
    Optional<String> userRule = cachingServiceClient.readFromMap(INVALID_USERS_KEY, hashedUserId);
    if (userRule.isPresent() && isRuleApplicable(userRule.get(), parsedToken)) {
        return true;
    }
    // 3. Check if any scope is invalidated
    for (String scope : parsedToken.getScopes()) {
        String hashedScope = getHash(scope, salt);
        Optional<String> scopeRule = cachingServiceClient.readFromMap(INVALID_SCOPES_KEY, hashedScope);
        if (scopeRule.isPresent() && isRuleApplicable(scopeRule.get(), parsedToken)) {
            return true;
        }
    }
    return false;
}
```

Verify that the caching service REST API already supports `GET /cache-list/{mapKey}/{recordKey}` returning a single record, or add the endpoint if it does not exist.

**Tests:**
- Add a test asserting `readAllMaps()` is no longer called during `isInvalidated()`.
- Add tests for each of the three lookup paths (token invalidated, user invalidated, scope invalidated) using mock responses.
- Benchmark test (or microbenchmark): assert the number of HTTP calls to the caching service scales with the number of scopes on the token, not with total stored records.

---

## 28. #4444 — `/auth/ticket` endpoint does not accept OIDC tokens

**Files:**
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/query/QueryFilter.java:85–93`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/config/NewSecurityConfiguration.java:397–431`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/ticket/SuccessfulTicketHandler.java`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/security/service/schema/source/OIDCAuthSourceService.java`

**Root cause:** `QueryFilter.attemptAuthentication()` hard-codes `TokenAuthentication.Type.JWT` (line 88). The OIDC token validation path is never attempted, so OIDC tokens are always rejected with "Token is not valid."

**Change 1** — Modify `QueryFilter.attemptAuthentication()` to try JWT first, then fall back to OIDC:

```java
String token = authenticationService.getJwtTokenFromRequest(request)
    .orElseThrow(() -> new TokenNotProvidedException("Authorization token not provided."));

// Try JWT first
Authentication result = null;
try {
    result = this.getAuthenticationManager().authenticate(
        new TokenAuthentication(token, TokenAuthentication.Type.JWT));
} catch (AuthenticationException jwtEx) {
    log.debug("JWT validation failed, trying OIDC: {}", jwtEx.getMessage());
}

// If JWT failed, try OIDC
if (result == null || !result.isAuthenticated()) {
    result = this.getAuthenticationManager().authenticate(
        new TokenAuthentication(token, TokenAuthentication.Type.OIDC));
}

if (result.isAuthenticated()) {
    return result;
}
throw new TokenNotValidException("Token is not authenticated by JWT or OIDC.");
```

**Change 2** — Ensure the `AuthenticationManager` used by `QueryFilter` has an `OIDCAuthenticationProvider` registered (check `NewSecurityConfiguration.java`). If not, register it on the filter's manager.

**Change 3** — In `SuccessfulTicketHandler`, verify that it handles an OIDC-backed `Authentication` principal correctly: it must be able to extract the mainframe userId from an OIDC-validated principal to generate the passticket. Confirm that `NoMainframeIdentityException` is correctly propagated if no RACF identity mapping exists for the OIDC user.

**Tests:**
- Add an integration test that sends an OIDC token to `/auth/ticket` with a valid RACF identity mapping and asserts a passticket is returned.
- Add a test that sends an OIDC token with no RACF mapping and asserts a clear error (not "Token is not valid").
- Add a test that sends a JWT token (existing behaviour) and asserts it still works.

---

## 29. #4318 — Gateway returns 400/500 for IPv6 host headers

**Files:**
- `gateway-service/src/main/java/org/zowe/apiml/gateway/config/WebSecurity.java:525–539`
- New file: `gateway-service/src/main/java/org/zowe/apiml/gateway/filter/IPv6HostHeaderNormalizationFilter.java`

**Root cause:** Spring's `StrictServerWebExchangeFirewall` and Netty's URI parser both reject or misparse IPv6 addresses in the `Host` header because they contain colons, which conflict with the `host:port` separator convention. There is no pre-processing filter to normalize IPv6 host headers.

**Change 1** — Add a high-priority `WebFilter` that normalizes the `Host` header before the firewall evaluates it. IPv6 addresses in `Host` must be enclosed in square brackets per RFC 2732 (e.g. `[::1]:7554`). The filter detects a bare IPv6 address (no brackets) and adds them:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IPv6HostHeaderNormalizationFilter implements WebFilter {

    private static final Pattern BARE_IPV6 = Pattern.compile(
        "^([0-9a-fA-F:]+)(:(\\d+))?$"  // IPv6 without brackets, optionally with :port
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String host = exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST);
        if (host != null) {
            Matcher m = BARE_IPV6.matcher(host);
            if (m.matches() && host.contains(":") && !host.startsWith("[")) {
                String ip   = m.group(1);
                String port = m.group(3);
                String normalized = port != null ? "[" + ip + "]:" + port : "[" + ip + "]";
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(HttpHeaders.HOST, normalized)
                    .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            }
        }
        return chain.filter(exchange);
    }
}
```

**Change 2** — Allow bracket-enclosed IPv6 host values in `StrictServerWebExchangeFirewall` in `WebSecurity.java`:
```java
// In the StrictServerWebExchangeFirewall configuration bean:
firewall.setAllowedHostnames(hostname ->
    hostname.matches(".*") // or a more targeted allowance for [::1] format
);
```

**Tests:**
- Add a unit test for `IPv6HostHeaderNormalizationFilter` with bare IPv6 inputs (`::1`, `::1:7554`, `2001:db8::1:7554`) and assert the `Host` header is normalized to `[::1]:7554` form.
- Add an integration test that fires a request with `Host: ::1:7554` to the gateway and asserts a non-400/non-500 response.

---

## 30. #4422 — Gateway self-routes to caching service, no retry (distributed mode)

**Files:**
- `gateway-service/src/main/java/org/zowe/apiml/gateway/caching/CachingServiceClientRest.java`
- `zaas-service/src/main/java/org/zowe/apiml/zaas/cache/CachingServiceClient.java`

**Root cause:** Both clients construct the caching service URL by routing through the gateway's own hostname (`gatewayClient.getGatewayConfigProperties().getHostname()`). This means every caching call transits through the gateway itself, ignoring topology and adding latency. There is also no retry logic for transient caching service failures.

**Change 1 — Direct Eureka-based routing:** Replace the self-routing approach with a direct Eureka lookup of the caching service and load-balance across available instances:

```java
// In CachingServiceClientRest constructor or a factory method:
// Instead of: this.cachingBalancerUrl = "https://<gateway>/cachingservice/api/v1/..."
// Use Eureka to find caching-service instances directly:
List<ServiceInstance> instances = discoveryClient.getInstances("cachingservice");
// Select one (round-robin), build direct URL:
String directUrl = instances.get(0).getUri() + "/api/v1/...";
```

Inject `ReactiveDiscoveryClient` (already available in the gateway Spring context).

**Change 2 — Retry:** Wrap REST calls in a retry block with 3 attempts and exponential backoff:
```java
webClient.get().uri(directUrl)
    .retrieve()
    .bodyToMono(...)
    .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
        .filter(t -> t instanceof WebClientResponseException.ServiceUnavailable
                  || t instanceof ConnectException));
```

**Change 3** — For the ZAAS `CachingServiceClient`, apply the same direct-Eureka-routing change. Since ZAAS already has access to a `DiscoveryClient`, use it to discover caching service instances directly rather than routing through the gateway.

**Tests:**
- Add a test that mocks `DiscoveryClient.getInstances("cachingservice")` returning two instances and asserts the client uses one of their direct URIs (not the gateway hostname).
- Add a test that mocks the first caching service instance as unavailable and asserts the retry picks the second instance.

---

## 31. #4340 — Extensions don't inherit AT-TLS context from parent Spring context

**File:** `apiml-extension-loader/src/main/java/org/zowe/apiml/extension/ExtensionsLoader.java`

**Exact code confirmed:** `ExtensionsLoader.onApplicationEvent()` at line 38 constructs `new ClassPathBeanDefinitionScanner(new AnnotationConfigApplicationContext())` — a throwaway isolated context used only as a registry for scanning. The scanned bean definitions are then individually registered into the real application context (`registry`, which is the `ApplicationContextInitializedEvent`'s context) at lines 48–52. The isolated `AnnotationConfigApplicationContext` is never refreshed or used as a live context — it is purely a scanning vehicle.

This means the fix is **not** `setParent()` on a child context. The scanned bean definitions are registered directly into the main context, so AT-TLS beans are already available there. The actual problem is that the `ClassPathBeanDefinitionScanner` uses a temporary context with an uninitialized `Environment`, so extension classes annotated with `@ConditionalOnProperty` for AT-TLS properties (e.g. `@ConditionalOnProperty("server.attlsClient.enabled")`) are not evaluated correctly during scanning.

**Change 1** — Pass the real application context's `Environment` to the scanner's backing context before scanning:
```java
AnnotationConfigApplicationContext scannerContext = new AnnotationConfigApplicationContext();
scannerContext.setEnvironment(event.getApplicationContext().getEnvironment());
ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(scannerContext);
```

**Change 2** — Verify that any AT-TLS-specific beans in extensions are annotated with the appropriate `@ConditionalOnProperty` so that they are only registered when AT-TLS is active. This is an extension author responsibility, but add documentation in the extension loader README.

**Note:** Since bean definitions (not instances) are registered into the main context at `ApplicationContextInitializedEvent` time (before the context is refreshed), `@Conditional` annotations are evaluated during the main context's `refresh()`, not during scanning. The environment must be correct at scan time only for the scanner's internal filtering — passing the real environment fixes this.

**Tests:**
- Add a test that creates a mock parent context with AT-TLS-related properties set (`server.attlsClient.enabled=true`) and asserts the loaded extension context's `Environment` contains those properties.
- Add a test that loads a sample extension bean that injects a bean from the parent context and asserts it resolves correctly.

---

## 32. #4511 + #4512 combined — Full HA routing integration test plan

Items 7 (#4512) and 20 (#4511) must be applied together. The sticky-session failover in #4511 requires that `ServiceNotAccessibleException` is reliably raised for all "service down" signals, which depends on the cause-chain fix in #4512. Applying one without the other leaves a gap.

**Implementation sequence:**
1. Apply fix 7 (ConnectException cause chain walk in `NettyRoutingFilterApiml`).
2. In `NettyRoutingFilterApiml.filter()`, after wrapping the error in `ServiceNotAccessibleException`, also publish a Spring `ApplicationEvent` or use a `LoadBalancerLifecycle` hook to notify the load balancer that the selected instance failed. Alternatively, store the failed instance ID in the `ServerWebExchange` attributes so `DeterministicLoadBalancer` can react.
3. Apply fix 20: in `filterInstances()`, after selecting the cached instance, register an error callback: if the downstream routing produces `ServiceNotAccessibleException`, invalidate the cache (`cache.delete(user, serviceId)`) and re-select. This can be done by chaining a `.doOnError(ServiceNotAccessibleException.class, e -> cache.delete(...))` in the reactive pipeline after the route is selected.

**Integration test:** Use `WireMock` or a mock service to simulate instance A going down mid-session. Assert:
- Request 1 routes to instance A (pinned).
- Instance A is brought down (WireMock returns `ConnectionRefused`).
- Request 2 within the same Eureka refresh window routes to instance B (failover).
- The sticky-session cache is updated to instance B.
- Request 3 routes to instance B (new pin).

---

## 33. #3883 — Infinispan port collision and workspace data isolation

**File:** `caching-service/src/main/java/org/zowe/apiml/caching/service/infinispan/config/InfinispanConfig.java`

**Root cause (1):** No port availability check before JGroups starts; port conflicts are only detected at bind time as an exception.
**Root cause (2):** The persistence path uses `{workspaceDirectory}/caching-service/{haInstanceId}`. If two users share the same `workspaceDirectory` and the same `haInstanceId`, their Infinispan nodes will share stored data.

**Change 1 — Port pre-flight check:** Before calling `cacheManager()` and configuring JGroups, probe whether the configured ports are available:

```java
private void checkPortAvailable(int port, String portName) {
    try (ServerSocket ignored = new ServerSocket(port)) {
        // Port is available
    } catch (IOException e) {
        throw new IllegalStateException(
            String.format("Infinispan %s port %d is already in use. Configure a different port via " +
                "ZWE_configs_storage_infinispan_jgroups_port or ZWE_configs_storage_infinispan_jgroups_keyExchange_port.", portName, port), e);
    }
}

// Call before setting system properties:
checkPortAvailable(Integer.parseInt(jgroupsPort), "JGroups");
checkPortAvailable(Integer.parseInt(keyExchangePort), "key exchange");
```

**Change 2 — Workspace isolation:** Strengthen the persistence path by appending a hash of a unique system-level identifier (e.g. the OS user running the process, or a UUID written to the workspace on first startup):

```java
private String getRootFolder() {
    String base = workspaceDirectory + "/caching-service/" + haInstanceId;
    // Append a per-user discriminator to prevent shared-filesystem collisions
    String userDiscriminator = System.getProperty("user.name", "default");
    return base + "/" + userDiscriminator;
}
```

Alternatively, document that `haInstanceId` must be globally unique across all Zowe instances on the system, and validate this on startup.

**Tests:**
- Add a test that mocks a port-in-use scenario (bind a `ServerSocket` on the port first) and asserts `IllegalStateException` is thrown with a meaningful message during `InfinispanConfig` initialization.
- Add a test that verifies `getRootFolder()` incorporates the `haInstanceId` and the user discriminator in the path.
