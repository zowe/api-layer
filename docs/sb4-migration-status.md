# Spring Boot 4 Migration Status

**Branch:** `feat/sb4-health-tomcat-migration`
**Last updated:** 2026-06-05
**Key versions:** Spring Boot 4.0.2 · Spring Cloud 2025.1.1 · Spring Framework 7.0.7 · Spring Security 7.0.2 · Java 17

---

## Build Status

```
./gradlew compileJava
```

**4 modules fail to compile:**

| Module | Error count |
|--------|-------------|
| `discovery-service` | ~25 errors |
| `caching-service` | 3 errors |
| `zaas-service` | 2 errors |
| `api-catalog-services` | 1 error |

All other modules (`apiml-common`, `apiml-tomcat-common`, `apiml-security-common`, `gateway-service`, `zaas-client`, etc.) compile successfully.

---

## Active Compilation Failures

### 1. Java Version / Jersey 4.0.1 Incompatibility (discovery-service) — BLOCKER

**Root cause:** Jersey 4.0.1 (a transitive dependency pulled in by `spring-cloud-netflix-eureka-server:5.0.1` via `eureka-client-jersey3:2.0.6`) compiles to class file version **65.0 (Java 21)**, but the project targets **Java 17 (class file version 61.0)**. The JVM refuses to load this class.

**Error:**
```
discovery-service/.../RefreshablePeerEurekaNodes.java:232: error: cannot access ExtendedConfig
  class CustomClientConfig extends ClientConfig {
  bad class file: jersey-common-4.0.1.jar(org/glassfish/jersey/ExtendedConfig.class)
  class file has wrong version 65.0, should be 61.0
```

**Dependency chain:**
```
spring-cloud-netflix-eureka-server:5.0.1
  → eureka-client-jersey3:2.0.6
      → jersey-client:3.0.5 → 4.0.1 (version conflict resolution)
      → jersey-common:3.0.5 → 4.0.1
```

**Resolution options:**
- **(a) Upgrade project to Java 21** — aligns with Spring Boot 4's recommended baseline.
- **(b) Force Jersey to 3.x** — add a dependency resolution rule in `discovery-service/build.gradle`:
  ```groovy
  configurations.all {
      resolutionStrategy.force 'org.glassfish.jersey.core:jersey-common:3.1.9'
      resolutionStrategy.force 'org.glassfish.jersey.core:jersey-client:3.1.9'
      resolutionStrategy.force 'org.glassfish.jersey.core:jersey-server:3.1.9'
      resolutionStrategy.force 'org.glassfish.jersey.inject:jersey-hk2:3.1.9'
  }
  ```

---

### 2. `InstanceRegistry.log` Privatized (discovery-service)

**Root cause:** `spring-cloud-netflix-eureka-server:5.0.1` changed the `log` field visibility in `InstanceRegistry` from accessible (protected/package) to `private`. `ApimlInstanceRegistry extends InstanceRegistry` and references `log` directly.

**Affected file:** `discovery-service/src/main/java/org/zowe/apiml/discovery/ApimlInstanceRegistry.java`

**Errors:**
```
ApimlInstanceRegistry.java:119: error: log has private access in InstanceRegistry
ApimlInstanceRegistry.java:119: error: cannot access Log
ApimlInstanceRegistry.java:203: error: log has private access in InstanceRegistry
ApimlInstanceRegistry.java:210: error: log has private access in InstanceRegistry
ApimlInstanceRegistry.java:214: error: log has private access in InstanceRegistry
ApimlInstanceRegistry.java:296: error: log has private access in InstanceRegistry
```

**Fix:** The class already has `@Slf4j`, but is using the inherited `log` field instead of its own. Since `@Slf4j` generates a field named `log`, the parent's private `log` shadows the Lombok one. Rename the Lombok logger via `@Slf4j(topic = "ApimlInstanceRegistry")` and use a local field name, or explicitly declare `private static final Logger log = LoggerFactory.getLogger(ApimlInstanceRegistry.class);` and remove references to the parent's field.

---

### 3. Lombok `@Slf4j` in Lambda / Inner-Class Scopes (discovery-service)

**Root cause:** Several classes use `log.debug/info/warn/error` inside lambdas or anonymous inner classes. Lombok's `@Slf4j` generates a `static` field, but `log` is not in scope inside a local class or a `BeanDefinitionRegistryPostProcessor` lambda.

**Affected files:**
```
discovery-service/.../config/EurekaConfig.java:67,69,137
discovery-service/.../staticdef/StaticServicesRegistrationService.java:75,77,80,97
discovery-service/.../staticdef/ServiceDefinitionProcessor.java:73,84,106,116,136,169,172,179,180,207,266,354,356,357
discovery-service/.../eureka/RefreshablePeerEurekaNodes.java:161
discovery-service/.../config/HttpWebSecurityConfig.java:132 (inner static class)
```

**Errors:** `error: cannot find symbol — variable log`

**Fix:** For lambda bodies (e.g. in `deleteEurekaPeerAwareInstanceRegistry`), capture the logger in a local variable before the lambda:
```java
Logger localLog = log;
return registry -> { localLog.debug("..."); };
```
For the inner static class `EurekaBasicAuthenticationProvider`, add a `private static final Logger log = LoggerFactory.getLogger(EurekaBasicAuthenticationProvider.class);` field to the inner class (or use `@Slf4j` on it).

---

### 4. `EurekaBasicAuthenticationProvider` Constructor Mismatch (discovery-service)

**Root cause:** `EurekaBasicAuthenticationProvider` is annotated `@RequiredArgsConstructor` and has two `final` fields (`String eurekaUserid`, `char[] eurekaPassword`), so Lombok should generate a two-arg constructor. However, the compiler reports `required: no arguments`, meaning Lombok is not processing the inner `static` class correctly in this context.

**Error:**
```
HttpWebSecurityConfig.java:70: error: constructor EurekaBasicAuthenticationProvider cannot be applied
  auth.authenticationProvider(new EurekaBasicAuthenticationProvider(eurekaUserid, eurekaPassword));
  required: no arguments
  found: String,char[]
```

**Fix:** Explicitly write the constructor instead of relying on Lombok for this inner static class:
```java
EurekaBasicAuthenticationProvider(String eurekaUserid, char[] eurekaPassword) {
    this.eurekaUserid = eurekaUserid;
    this.eurekaPassword = eurekaPassword;
}
```

---

### 5. `serverProperties.getError()` Removed (discovery-service)

**Root cause:** In Spring Boot 4, `ServerProperties` no longer has a `getError()` method. `ErrorProperties` has been separated out and `BasicErrorController` now takes it differently.

**Affected file:** `discovery-service/src/main/java/org/zowe/apiml/discovery/config/DiscoveryErrorController.java:36`

**Error:**
```
super(errorAttributes, serverProperties.getError(), errorViewResolvers);
error: cannot find symbol — method getError()
```

**Fix:** Inject `ErrorProperties` directly and pass it to the super constructor:
```java
public DiscoveryErrorController(ErrorAttributes errorAttributes,
        ErrorProperties errorProperties,
        List<ErrorViewResolver> errorViewResolvers) {
    super(errorAttributes, errorProperties, errorViewResolvers);
}
```
Wire it in configuration or rely on auto-injection using `@Autowired` + an `ErrorProperties` bean.

---

### 6. `EurekaConfig` Symbol Conflicts (discovery-service)

**Root cause:** Several `cannot find symbol` errors in `EurekaConfig.java` (lines 67, 69, 137) are the same `log`-in-lambda issue as item 3. Additionally, `RefreshablePeerEurekaNodes.java:161` has the same `log` issue in a local anonymous class.

No additional fix beyond item 3 above.

---

### 7. `ErrorPage` Package Moved (zaas-service, api-catalog-services)

**Root cause:** In Spring Boot 4, `ErrorPage` moved from `org.springframework.boot.web.server.ErrorPage` to **`org.springframework.boot.web.error.ErrorPage`**.

**Affected files:**
- `zaas-service/src/main/java/org/zowe/apiml/zaas/error/custom/CustomErrorStatusHandlingBean.java:14`
- `api-catalog-services/src/main/java/org/zowe/apiml/apicatalog/controllers/handlers/CustomErrorStatusHandlingBean.java:14`

**Error:** `error: cannot find symbol — class ErrorPage in package org.springframework.boot.web.server`

**Fix:** Update import in both files:
```java
// Before
import org.springframework.boot.web.server.ErrorPage;
// After
import org.springframework.boot.web.error.ErrorPage;
```

---

### 8. `AntPathRequestMatcher` Removed in Spring Security 7.0.2 (zaas-service)

**Root cause:** Spring Security 7.0.2 (bundled with Spring Boot 4) removed `AntPathRequestMatcher`. The replacement is `PathPatternRequestMatcher` (for pattern-based matching) or `OrRequestMatcher` for combining matchers.

**Affected file:** `zaas-service/src/main/java/org/zowe/apiml/zaas/security/config/NewSecurityConfiguration.java:41,151`

**Error:** `error: cannot find symbol — class AntPathRequestMatcher`

**Usage:**
```java
.logoutRequestMatcher(new AntPathRequestMatcher(...))
```

**Fix:** Replace with `PathPatternRequestMatcher.withDefaults()` or the factory method:
```java
import org.springframework.security.web.util.matcher.PathPatternRequestMatcher;
// ...
.logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(...))
```
Or use the static factory:
```java
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.*;
.logoutRequestMatcher(pathPattern("/auth/logout"))
```

---

### 9. `CachesEndpoint` Missing Dependency (caching-service)

**Root cause:** In Spring Boot 4, `CachesEndpoint` was extracted from `spring-boot-actuator` into the separate `spring-boot-cache` module. The class is at the same package path (`org.springframework.boot.cache.actuate.endpoint.CachesEndpoint`) but requires the `spring-boot-starter-cache` dependency.

**Affected file:** `caching-service/src/main/java/org/zowe/apiml/caching/health/ApimlCachesEndpoint.java:14`

**Error:**
```
error: package org.springframework.boot.cache.actuate.endpoint does not exist
```

**Fix:** Add dependency to `caching-service/build.gradle`:
```groovy
implementation libs.spring_boot_starter_cache
```
The library alias already exists in `gradle/versions.gradle`.

---

## Minor / Non-Blocking Issues

### `javax.servlet` String Literals (zaas-service tests)

Three test assertions use the string constant `"javax.servlet.request.X509Certificate"`. In Jakarta EE this attribute key is `"jakarta.servlet.request.X509Certificate"`.

**File:** `zaas-service/src/test/java/org/zowe/apiml/zaas/security/service/schema/source/X509AuthSourceServiceTest.java` (lines 102, 157, 206)

**Fix:** Update string literals if the production code has been migrated; leave if the attribute name is intentionally backwards-compatible.

### `javax.servlet` JavaDoc references (apiml-tomcat-common)

**File:** `apiml-tomcat-common/src/main/java/org/zowe/apiml/gzip/GZipResponseUtils.java` (lines 49, 51, 71)

These are JavaDoc `{@link javax.servlet...}` references. They break `javadoc -Xdoclint` but do not affect compilation.

**Fix:** Update to `jakarta.servlet` references.

### `spring.factories` for `EnvironmentPostProcessor`

All services register `ServerAddressPropertiesUpdater` as an `EnvironmentPostProcessor` via `META-INF/spring.factories`. In Spring Boot 3+, the preferred approach is a dedicated file:

```
META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
```

`spring.factories` still works in Spring Boot 4 for this use case, but it is the legacy mechanism. This is not a compilation error.

### Shadow Plugin Deprecation Warning

```
The legacy Shadow plugin (id 'com.github.johnrengelman.shadow') is deprecated.
Please use the Gradle Shadow plugin instead (id = 'com.gradleup.shadow')
```

Affects `onboarding-enabler-micronaut-sample-app`. Not a blocker.

### Gradle Wrapper Version

The `gradle-wrapper.properties` specifies **8.14.5**, while recent commits claim a move to Gradle 9.0. Update:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-bin.zip
```

---

## Summary Table

| # | Module | File | Issue | Severity |
|---|--------|------|-------|----------|
| 1 | `discovery-service` | transitive deps | Jersey 4.0.1 requires Java 21; project targets Java 17 | **BLOCKER** |
| 2 | `discovery-service` | `ApimlInstanceRegistry.java` | `InstanceRegistry.log` now private in Spring Cloud Netflix 5.0.1 | High |
| 3 | `discovery-service` | `EurekaConfig.java`, `StaticServicesRegistrationService.java`, `ServiceDefinitionProcessor.java`, `RefreshablePeerEurekaNodes.java` | Lombok `@Slf4j` `log` not accessible inside lambda/inner class scopes | High |
| 4 | `discovery-service` | `HttpWebSecurityConfig.java` | `@RequiredArgsConstructor` not generating constructor for inner static class | High |
| 5 | `discovery-service` | `DiscoveryErrorController.java` | `ServerProperties.getError()` removed in SB4 | High |
| 6 | `zaas-service` | `CustomErrorStatusHandlingBean.java` | `ErrorPage` moved to `org.springframework.boot.web.error` | Medium |
| 7 | `api-catalog-services` | `CustomErrorStatusHandlingBean.java` | Same as #6 | Medium |
| 8 | `zaas-service` | `NewSecurityConfiguration.java` | `AntPathRequestMatcher` removed in Spring Security 7.0.2 | Medium |
| 9 | `caching-service` | `ApimlCachesEndpoint.java` | `CachesEndpoint` moved to `spring-boot-cache` module; missing `spring-boot-starter-cache` dep | Medium |
| 10 | `zaas-service` tests | `X509AuthSourceServiceTest.java` | `javax.servlet.request.X509Certificate` string literal (should be jakarta) | Low |
| 11 | `apiml-tomcat-common` | `GZipResponseUtils.java` | JavaDoc references `javax.servlet` | Low |
| 12 | All services | `spring.factories` | Legacy mechanism for `EnvironmentPostProcessor` | Low |
| 13 | Micronaut sample | `build.gradle` | Deprecated Shadow plugin ID | Low |
| 14 | Root | `gradle-wrapper.properties` | Wrapper still at 8.14.5, not 9.0 | Low |

---

## What Is Already Working

The following migration work is complete and compiles successfully:

- **Health indicators** — All services migrated from `org.springframework.boot.actuate.health.*` to `org.springframework.boot.health.contributor.*` (`AbstractHealthIndicator`, `Health`, `Status`)
- **Tomcat embedding** — Updated to `org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory` across all services
- **`javax` → `jakarta`** — Jakarta EE annotation imports updated (only standard Java `javax.net.ssl`, `javax.naming`, `javax.management` remain, correctly)
- **Spring Security** — All services use `SecurityFilterChain` bean pattern; no `WebSecurityConfigurerAdapter` remaining
- **WebSocket proxy** — Updated to Spring Framework 7 API (`execute()`, `containsHeader()`)
- **`HttpHeaders` API** — Fixed throughout: no longer inherits from `Map`; `forEach()` replaces `entrySet().stream()` patterns
- **Jackson** — `spring-boot-jackson2` dependency added to `apiml-tomcat-common`
- **Spring Framework version** — Pinned to `7.0.7` (the `7.0` BOM does not exist on Maven Central)
- **Modules compiling clean:** `apiml-common`, `apiml-tomcat-common`, `apiml-security-common`, `gateway-service`, `zaas-client`, `common-service-core`, `onboarding-enabler-spring`, `onboarding-enabler-java`, `apiml-extension-loader`, `security-service-client-spring`, `discoverable-client`, and all sample apps
