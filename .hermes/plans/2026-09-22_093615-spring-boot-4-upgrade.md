# Spring Boot 4 Upgrade — Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Move Zowe API Mediation Layer from Spring Boot 3.5 to Spring Boot 4.1.x on Java 21, so the product runs on a supported framework line before the 3.5 OSS window closes.

**Architecture:** Finish the stalled `feat/sb4-health-tomcat-migration` branch (SB 4.0.2, June 2026), then retarget it to Spring Boot 4.1.1 and Java 21. The branch is ~90% complete — the modulith and the gateway already compile; the remaining work is concentrated in `discovery-service` (Eureka/Spring Cloud Netflix 5.x API breakage) plus three one-line API moves in the microservice-only modules.

**Tech Stack:** Java 21 (Zulu, SDKMAN), Gradle 9.7.1, Spring Boot 4.1.1, Spring Framework 7.0.9, Spring Cloud 2025.1.x / Netflix 5.0.2, Spring Modulith 2.1.1, Infinispan 16.2.x.

**Status snapshot (verified 2026-09-22):**

| Fact | Value | Source |
|---|---|---|
| Current v3.x.x baseline | SB 3.5.16, Framework 6.2.19, Spring Cloud Netflix 4.3.3, Gradle 9.7.1 | `gradle/versions.gradle`, `gradle-wrapper.properties` @ `origin/v3.x.x` |
| SB4 branch tip | `1958edbe3` "Additional Fixes", 2026-06-05 | `origin/feat/sb4-health-tomcat-migration` |
| Branch versions | SB 4.0.2, Framework 7.0.7, Cloud 2025.1.1 | branch `gradle/versions.gradle` + `docs/sb4-migration-status.md` |
| Java in build | **17** (`sourceCompatibility = JavaVersion.VERSION_17`) | `build.gradle:63-64,151-152` |
| Java installed locally | **17.0.20** (Semeru) only — no 21 on this machine | `sdkman/candidates/java` |
| SB 3.5 OSS EOL | **30 June 2026** (already passed); enterprise support to 2032 | spring.io support policy / endoflife.date |
| Recommended target | SB **4.1.1** (20 Aug 2026), Java 17+ baseline, Spring Framework 7.0.8+ | versionlog 4.1, HeroDevs, release highlights |
| SB 4.1.0 release date | 10 Jun 2026 | versionlog |
| SB 4.1 EOL | OSS Jun 2027, enterprise Jun 2028 | versionlog |
| Available in Zowe Artifactory | SB 4.0.0–4.0.8, **4.1.0, 4.1.1**; Framework **7.0.9**; Netflix **5.0.2**; Commons/CB/Gateway **5.0.3**; `spring-cloud-dependencies` **2025.1.3**; Modulith **2.1.1**; Infinispan `spring-boot4-starter-embedded` 16.2.3; Tomcat 11.0.26 | `curl` against `zowe.jfrog.io/zowe/libs-release` |
| Rebase target | **not yet rebased** — the branch predates Gradle 9.0 (wrapper 8.14.5 on the branch vs 9.7.1 on v3.x.x) | branch `gradle-wrapper.properties` vs v3.x.x |
| Stale-line check | both `ErrorPage` imports and the `AntPathRequestMatcher` usage are still unmigrated on v3.x.x (`CustomErrorStatusHandlingBean.java:14` ×2, `NewSecurityConfiguration.java:41,152`) — June fixes live only on the branch | `grep` @ `hermes/gh4858` (v3.x.x-derived) |

**Key scope decision baked into this plan:** do the migration **modulith-first**. The five standalone services (`gateway-service`, `zaas-service`, `discovery-service`, `caching-service`, `api-catalog-services`) are deleted in V4 (target Sep 2027). Migrating them costs days; migrating the modulith costs hours. The plan defines a shippable "modulith-only SB4" milestone, then treats the standalone modules as a separate, explicitly-scoped phase — and recommends deleting them instead of fixing them.

---

## Phase 0 — Decisions to confirm before writing code

These are cheap to answer now and expensive to reverse. Confirm before Task 1.

1. **Java 21 vs Java 17 — this is the one decision that gates the whole plan, so decide it first.** SB4 supports Java 17, but the dependency chain does not cooperate. **Measured on the artifacts we would actually use:** `eureka-client-jersey3:2.0.6` declares `jersey-client:3.0.5`, and Gradle resolves that to **jersey-common 4.0.1**. I unpacked the class file: `jersey-common:4.0.1` → `ExtendedConfig.class` is **class file version 61 (Java 17)**; the newer `jersey-common:4.0.2` is **version 65 (Java 21)** — and Jersey 4.0 is the line that drops Java 17. So today's failure is *not* a Java-version blocker; it is Gradle resolving to a Jersey version nobody in the chain asked for, while the declared version is 3.0.5.
   - Option **(a)** — move the build to Java 21. Removes the resolution hazard permanently, because any future Jersey 4.0.2+ (pulled transitively at any time) loads fine. Matches the modulith's ~1 GB production heap assumptions and the enablers, which already support 21.
   - Option **(b)** — stay on Java 17 and force Jersey to the newest 3.x (**3.1.12**, verified present; class file version **55**, safe). Cheaper, but it is a pin we must remember to keep, and transitive drift toward 4.0.2 will keep re-introducing "bad class file version" failures.
   **Recommendation: (a) Java 21.** Option (b) is not wrong, but it turns a one-time build change into a standing resolution rule — and Jersey 4.0 is exactly where Eureka's client chain is headed. Note the modulith itself needs no Jersey either way; this chain is only reachable through the Eureka client.
2. **Target line: 4.0.8 or 4.1.1.** **Recommendation: 4.1.1.** It is the current stable branch, its OSS window runs to Jun 2027 (4.0's is shorter), and it is built on Framework 7.0.8+. The only 4.1-specific hazard is that removals of APIs deprecated in 4.0 now bite — which is the same code we are already migrating.
3. **Fate of the standalone services in this cycle.** Fix them (Tasks 12–15) or delete them as the V4 runtime contract (pre-3.5.0, in this cycle)? **Recommendation: fix minimally now (they are 2–6 line fixes each), delete in V4** — deleting them is a product decision that needs release-notes and install-packaging work, not a framework upgrade.
4. **Jackson 2 vs Jackson 3.** SB4 defaults to Jackson 3; Jersey 4.0 does not support it. The branch already added the `spring-boot-jackson2` shim for `apiml-tomcat-common`. Confirm we keep **Jackson 2 via shim** for this upgrade (Jackson 2 artifacts are deprecated for removal in 4.3) rather than doing a Jackson 3 migration in the same window.

---

## Phase 1 — Rebase the SB4 branch onto current v3.x.x

The branch was cut from v3.x.x in early June 2026 and is 15+ weeks stale (v3.x.x is at `3.5.23-SNAPSHOT` with Gradle 9.7.1 and SB 3.5.16). Rebasing first keeps the eventual diff reviewable.

### Task 1: Create a fresh SB4 working branch from current v3.x.x

**Objective:** Land the branch work on a current base without losing the June fixes.

**Steps:**

```bash
git fetch --all --tags
git checkout -b feat/sb4-upgrade v3.x.x          # or origin/v3.x.x
git merge --no-commit --no-ff origin/feat/sb4-health-tomcat-migration
# resolve conflicts; the high-churn files are gradle/versions.gradle and
# build.gradle files — take the SB4 side for Spring deps, v3.x.x side for
# everything unrelated to Spring.
```

**Verification:** `git diff --stat v3.x.x` shows only Spring/Java-related files plus `docs/sb4-migration-status.md`. Commit as `chore: rebase SB4 migration onto v3.x.x (3.5.23-SNAPSHOT)`.

**Rollback:** the branch is new; `git branch -D feat/sb4-upgrade` returns to status quo. The old `feat/sb4-health-tomcat-migration` is untouched.

---

## Phase 2 — Toolchain: Java 21 + target versions

### Task 2: Move the build to Java 21

**Objective:** Remove the Jersey/Java-17 class-file blocker at the root.

**Files:**
- Modify: `build.gradle:63-64` and `build.gradle:151-152` — replace `JavaVersion.VERSION_17` with `JavaVersion.VERSION_21`
- Modify: `gradle.properties` — no change needed
- Verify: `gradle/wrapper/gradle-wrapper.properties` already at 9.7.1 (OK for SB4)

**Steps:**

1. `export JAVA_HOME=/home/balda/.sdkman/candidates/java/21.0.x-zulu && export PATH=$JAVA_HOME/bin:$PATH` (install via `sdk install java 21.0.x-zulu` if absent — **note:** this workstation currently has only 17.0.20).
2. Change the four `JavaVersion` references to `VERSION_21`.
3. Also update every `-source/-target` or `options.release` if present in `gradle/*.gradle`; search with `grep -rn "VERSION_17\|release = \|targetCompatibility" --include=*.gradle`.
4. Update CI images and `gradle/jib.gradle` base image JRE to 21.

**Verification:**

```bash
./gradlew :apiml-common:compileJava :apiml:compileJava --no-daemon
```
Expected: BUILD SUCCESSFUL, no "bad class file version" errors.

**Commit:** `build: move API ML to Java 21 for Spring Boot 4`

### Task 3: Pin the SB4 dependency set to the 4.1 line

**Objective:** Update `gradle/versions.gradle` to the versions verified present in Zowe Artifactory.

**File:** `gradle/versions.gradle`

```groovy
version('springBoot', '4.1.1')
version('springBootGraphQl', '4.1.1')
version('springFramework', '7.0.9')
version('springCloud', '2025.1.3')          // newest 2025.1.x on Zowe Artifactory
version('springCloudNetflix', '5.0.2')
version('springCloudCommons', '5.0.3')
version('springCloudCB', '5.0.3')
version('springCloudGateway', '5.0.3')      // separate release line from Netflix — confirmed distinct
version('modulith', '2.1.1')
version('tomcat', '11.0.26')
version('infinispan', '16.2.3')             // + switch artifact to spring-boot4-starter-embedded
```

**Notes:**
- `springCloudGateway` and `springCloudNetflix` are separate lines in this catalog — resolve each independently against Artifactory (`curl http://.../spring-cloud-gateway-server-webflux/maven-metadata.xml`) rather than assuming they move together.
- Keep the existing `springFramework` explicit pin: the branch already learned that `7.0` is not a resolvable Maven version.
- **If Phase 0 decision 1 lands on option (b) — stay on Java 17 —** add a forced resolution in `discovery-service/build.gradle` (and anywhere the Eureka client is on the runtime classpath, i.e. the modulith too, in case its embedded server pulls the same chain):
  ```groovy
  configurations.all {
      resolutionStrategy.force(
          'org.glassfish.jersey.core:jersey-common:3.1.12',
          'org.glassfish.jersey.core:jersey-client:3.1.12',
          'org.glassfish.jersey.core:jersey-server:3.1.12',
          'org.glassfish.jersey.inject:jersey-hk2:3.1.12'
      )
  }
  ```
  (The June scratch note suggested 3.1.9; **3.1.12** is the newest 3.x and the one I verified is class-file-safe at Java 17.)
- `spring-boot-jackson2` shim stays (Phase 5 decision).

**Verification:**

```bash
./gradlew dependencyInsight --dependency spring-boot --configuration runtimeClasspath -p apiml
./gradlew :apiml:dependencies > /tmp/deps-apiml.txt && grep -E "spring-boot|spring-core|spring-cloud|infinispan|eureka" /tmp/deps-apiml.txt | sort -u
```
Expected: everything resolves, no dynamic-version warnings, no mixed 3.5.x artifacts anywhere in the graph.

**Commit:** `build: target Spring Boot 4.1.1 / Framework 7.0.9 / Modulith 2.1.1`

### Task 4: Resolve the Infinispan starter rename

**Objective:** Infinispan's Spring Boot 3 starter (`infinispan-spring-boot3-starter-embedded`) will not work against SB4's autoconfiguration contract.

**Verified:** Infinispan publishes a sibling starter, `infinispan-spring-boot4-starter-embedded`, at **16.2.3** — the same version the SB4 branch pins — and its POM depends on `spring-boot-cache` from the modularised SB4 layout. This is a drop-in artifact swap at the same version, and it is already available in Zowe Artifactory. Confirm the corresponding property names for cache configuration in the new starter's metadata before changing YAML (Task 11 covers the sweep).

**Files:**
- Modify: `gradle/versions.gradle:312` (the `infinispan` bundle)
- Modify: `gradle/versions.gradle` library alias — change artifact to `infinispan-spring-boot4-starter-embedded`
- Verify consumers: `apiml/build.gradle:93`, `caching-service/build.gradle:71`

**Verification:** `./gradlew :caching-service:dependencies | grep infinispan` shows only `...spring-boot4-starter-embedded`. Then run the cache-behaviour test set (Task 11) — the unbounded-cache finding means we must confirm bounds survive the starter swap.

**Commit:** `build: use Infinispan Spring Boot 4 starter`

---

## Phase 3 — The blocker: discovery-service / Eureka on Spring Cloud Netflix 5.0.x

All five items in the June scratch notes (`docs/sb4-migration-status.md`, items 1–6) live here. The Eureka 2.0.6 + Netflix 5.0.2 + Framework 7 combination is the highest-risk unknown in the whole upgrade. **Timebox this phase: if Task 8 cannot be made green within 3 working days, escalate to the V4 fork-and-in-source decision early rather than fighting it.**

### Task 5: Capture the real error set before changing anything

**Objective:** The June notes were made against SB 4.0.2 with Java 17. Re-derive the failures against SB 4.1.1 + Java 21.

```bash
./gradlew :discovery-service:compileJava --continue 2>&1 | tee /tmp/disc-errors.txt
grep -c "error:" /tmp/disc-errors.txt
```

Record the count in the plan's status section. Do not fix from the June list alone — some of it is already stale (the branch's later commits resolved `ErrorPage`, `getError()`, and `CachesEndpoint`).

### Task 6: Fix `InstanceRegistry.log` privatization in `ApimlInstanceRegistry`

**Objective:** Spring Cloud Netflix 5.0.x made `InstanceRegistry.log` private, breaking the subclass's inherited-log usage.

**File:** `discovery-service/src/main/java/org/zowe/apiml/discovery/ApimlInstanceRegistry.java` (~6 call sites around lines 119, 203, 210, 214, 296)

**Approach:** delete reliance on the inherited field; add

```java
private static final Logger log = LoggerFactory.getLogger(ApimlInstanceRegistry.class);
```

and, because Lombok `@Slf4j` on the class would collide with the inherited name, either drop `@Slf4j` from this class or rename via `@Slf4j(topic = "ApimlInstanceRegistry")` and use the generated name.

**Verification:** `./gradlew :discovery-service:compileJava` — the `log has private access` errors disappear; other errors remain (expected, Tasks 7–8).

### Task 7: Fix logger visibility in lambdas and inner classes

**Objective:** Lombok `@Slf4j` generates a `static` field that is not in scope inside lambdas/anonymous classes in several discovery classes.

**Files:**
- `discovery-service/.../config/EurekaConfig.java` (67, 69, 137)
- `discovery-service/.../staticdef/StaticServicesRegistrationService.java` (75, 77, 80, 97)
- `discovery-service/.../staticdef/ServiceDefinitionProcessor.java` (many)
- `discovery-service/.../eureka/RefreshablePeerEurekaNodes.java` (161)
- `discovery-service/.../config/HttpWebSecurityConfig.java` (132, inner static class)

**Approach:** for lambda bodies, hoist the logger into a local before the lambda:

```java
Logger localLog = log;
return registry -> { localLog.debug("..."); };
```

For inner static classes (`EurekaBasicAuthenticationProvider`), declare an explicit `private static final Logger log = ...` on the inner class.

**Note for `RefreshablePeerEurekaNodes`:** this class also does `MethodHandles` reflection into Eureka internals for peer replication. Expect signature drift between Netflix 4.3.3 and 5.0.x — treat any reflection failure as a Task 8 escalation, not a local patch.

**Verification:** `./gradlew :discovery-service:compileJava`; then run `:discovery-service:test --tests "*RefreshablePeerEurekaNodesTest*"`.

### Task 8: Fix the `EurekaBasicAuthenticationProvider` constructor

**Objective:** `@RequiredArgsConstructor` is not generating the expected 2-arg constructor for the inner static class.

**File:** `discovery-service/.../config/HttpWebSecurityConfig.java:70`

**Approach:** write the constructor explicitly:

```java
EurekaBasicAuthenticationProvider(String eurekaUserid, char[] eurekaPassword) {
    this.eurekaUserid = eurekaUserid;
    this.eurekaPassword = eurekaPassword;
}
```

**Verification:** `./gradlew :discovery-service:compileJava` → BUILD SUCCESSFUL. This is the checkpoint for the timebox in this phase.

### Task 9: Validate the Eureka runtime contract, not just compilation

**Objective:** Compilation is necessary but not sufficient — Discovery is a public API and its REST surface must not change.

**Steps:**
1. Start Discovery standalone: `./gradlew :discovery-service:bootRun` (or `:discovery-service:run`).
2. Register a fake instance: `POST /eureka/apps/TESTAPP` with a minimal `InstanceInfo` JSON body.
3. Heartbeat it: `PUT /eureka/apps/TESTAPP/{instanceId}` — expect 200.
4. Query: `GET /eureka/apps`, `GET /eureka/apps/TESTAPP`, `GET /eureka/apps/delta` — expect the registered instance in the normalised Eureka XML/JSON shape.
5. Peer replication: start two instances with each other in `eureka.client.serviceUrl`, confirm `POST /eureka/peerreplication/batch/` is exercised and the second registry converges.
6. Static definitions: point at `discovery-service/src/main/resources/static definitions` YAML and confirm `reloadServices()` still registers them with never-expiring leases.

**Verification:** the same requests against a SB 3.5 discovery-service produce byte-comparable normalised output (modulo timestamps). Any shape difference is a **breaking change to a public contract** and must be raised before merge.

**Commit:** `fix: migrate discovery-service to Spring Cloud Netflix 5.0.x APIs`

---

## Phase 4 — Modulith correctness

### Task 10: Migrate the modulith's embedded Eureka wiring

**Objective:** The modulith embeds an Eureka server and does internal peer replication. Netflix 5.0.x changed internals this code depends on.

**Files:**
- `apiml/src/main/java/org/zowe/apiml/EurekaConfiguration.java`
- `apiml/src/main/java/org/zowe/apiml/ModulithConfig.java` (static `InstanceInfo` registration, `peerAwareHeartbeat`, `peerReplicate`)
- `apiml/src/main/java/org/zowe/apiml/RouteRefreshListener.java`
- `apiml/src/main/java/org/zowe/apiml/ApimlApplication.java`

**Steps:**
1. `./gradlew :apiml:compileJava` and record remaining errors.
2. Fix the `@Slf4j`/lambda logger patterns here too (same class of failure as Task 7).
3. Run `RefreshablePeerEurekaNodesTest` and the modulith test slice.

**Verification:** `./gradlew :apiml:test` passes; app boots with `otel.sdk.disabled=true` and `/application/health` returns UP; `/eureka/apps` on the embedded server lists gateway/discovery/caching/catalog instances.

### Task 11: Config-property migration sweep

**Objective:** SB4 renamed/removed configuration properties. Silent property drift is the failure mode that reaches production.

**Steps:**
1. Use SB 4.1's configuration changelog (`docs.spring.io/spring-boot/docs/4.1.1`) plus the generated metadata: unzip `spring-boot-autoconfigure`'s `spring-configuration-metadata.json` from the resolved jar and diff the property names against every `application.yml` in the repo.
2. `grep -rn "^\( *\)[a-z]" --include=application.yml --include=application-*.yml .` and check each renamed key by hand — the count is small, so do it exhaustively rather than by sampling.
3. Pay special attention to: health (`actuate.health` → `health.contributor`), Tomcat (`web.embedded.tomcat` → `tomcat`), cache endpoint packages, `management.*`, and Eureka client paths.
4. Replace the legacy `META-INF/spring.factories` `EnvironmentPostProcessor` registration with `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` for `ServerAddressPropertiesUpdater` (flagged as non-blocking in June; it should land in the same release as the rest of the migration).

**Verification:** boot each service with the migrated YAML and assert on the actuator `/configprops` + `/env` output that no property is unbound; add a test that fails if any property in `application.yml` is absent from the SB4 metadata (catches future renames).

**Commit:** `fix: migrate configuration properties to Spring Boot 4 names`

---

## Phase 5 — Microservice-only modules (or delete them)

Each of these is a 2–6 line fix. None of them are deleted until V4, so they must compile.

### Task 12: `api-catalog-services` — `ErrorPage` package move

`org.springframework.boot.web.server.ErrorPage` → `org.springframework.boot.web.error.ErrorPage` in `api-catalog-services/.../controllers/handlers/CustomErrorStatusHandlingBean.java:14`. (Already fixed on the June branch — verify it survived the rebase.)

### Task 13: `zaas-service` — same `ErrorPage` move + `AntPathRequestMatcher` removal

- `zaas-service/.../zaas/error/custom/CustomErrorStatusHandlingBean.java:14` (package move)
- `zaas-service/.../zaas/security/config/NewSecurityConfiguration.java:41,152` — `AntPathRequestMatcher` removed in Spring Security 7; replace `logoutRequestMatcher(new AntPathRequestMatcher(...))` with `PathPatternRequestMatcher.withDefaults().matcher(...)`.
- Test literals: `zaas-service/.../X509AuthSourceServiceTest.java:102,157,206` use `"javax.servlet.request.X509Certificate"` — verify whether the attribute name is intentionally backwards-compatible, and if not, switch to `jakarta.servlet.request.X509Certificate`.

**Verification:** `./gradlew :zaas-service:test` — the security-chain tests are the ones that will catch a logout-matcher regression.

### Task 14: `caching-service` — `CachesEndpoint` relocation

`CachesEndpoint` moved into the modularised `spring-boot-cache` jar while keeping its package name. `caching-service` does not declare `spring-boot-starter-cache` — add it to `caching-service/build.gradle` (the alias `libs.spring_boot_starter_cache` already exists).

**Verification:** `./gradlew :caching-service:test`. Then confirm the actuator cache endpoint still lists the Infinispan caches and that the unbounded-cache findings from the SB3 line are unchanged by the SB4 starter swap.

### Task 15: JavaDoc and legacy-mechanism cleanup (non-blocking, land anyway)

- `apiml-tomcat-common/.../gzip/GZipResponseUtils.java:49,51,71` — `{@link javax.servlet...}` → `jakarta.servlet`
- `onboarding-enabler-micronaut-sample-app/build.gradle` — legacy Shadow plugin id `com.github.johnrengelman.shadow` → `com.gradleup.shadow`

**Commit per module:** `fix: <module> compiles against Spring Boot 4.1`

---

## Phase 6 — Verification

### Task 16: Full build green

```bash
export JAVA_HOME=/home/balda/.sdkman/candidates/java/21.0.x-zulu
export PATH=$JAVA_HOME/bin:$PATH
./gradlew clean build --continue 2>&1 | tee /tmp/sb4-full-build.txt
grep -E "FAILED|error:" /tmp/sb4-full-build.txt
```
Expected: zero failures across all 41 modules. Attach `sb4-full-build.txt` to the PR.

### Task 17: Integration + z/OS test pass

1. `./gradlew :integration-tests:test` on Linux with `mock-services` — gateway routing, all six auth schemes, CORS/CSRF behaviour, WebSocket proxy.
2. Run the z/OS USS suite (AT-TLS, SAF IDT, Passticket, X.509/mTLS, OIDC) on a real LPAR. **The container image is not a valid substitute** — SAF keyrings, OIDC, and Passticket do not work in containers, which is exactly the surface a framework major bump threatens.
3. Modulith HA: two JVMs, verify Eureka peer replication and JWT key sharing.

**Verification evidence required:** test result XML/summary attached to the PR, plus the z/OS run ID.

### Task 18: CVE and dependency review

1. `./gradlew dependencyCheckAnalyze` (or the configured equivalent) on the SB4 graph.
2. Confirm the past deadline did not leave us exposed while 3.5 was already EOL: list security advisories fixed in 3.5.17+ / 4.0.3+ / 4.1.1 that affect the 3.5.16 baseline, and confirm each is resolved by the SB4 branch.
3. Note explicitly that `eureka-core:2.0.6` is unchanged between 3.5 and the SB4 branch — no accidental Eureka upgrade happened.

### Task 19: Enabler compatibility

**Objective:** external services onboard through five enablers. A framework bump in API ML must not break them.

- Confirm `onboarding-enabler-spring` and `security-service-client-spring` built from the SB4 branch still work for a consumer on SB 3.5 (the common case — customers are not all on 4.x).
- Confirm the enabler does not drag SB4 into a consumer's classpath. Check the published POM of the branch artifacts for Spring version ranges.
- Smoke-test `onboarding-enabler-java`, `-micronaut`, `-nodejs`, `-python` sample apps against the SB4 modulith.

**Verification:** run the sample apps from `samples/` against the SB4 modulith and capture registration + routing proof.

### Task 20: Migration guide + release notes

- Rewrite the branch's `docs/sb4-migration-status.md` into a **user-facing** migration note: what a Zowe customer must change in `zowe.yaml` (any renamed properties), minimum Java version, and the fact that the JSON writer is Jackson 2-backed for this release.
- Add a CHANGELOG entry for the release train that carries SB4.
- State the Jackson 2 → Jackson 3 intent and timeline (Jackson 2 removal lands in SB 4.3.0 per the SB 4.1 deprecated list) so it is a planned follow-up, not a surprise.

---

## Files likely to change

**Build / toolchain**
- `gradle/versions.gradle` (versions + `infinispan` alias + bundle at line 312)
- `build.gradle` (Java 21, lines 63-64, 151-152)
- `gradle/wrapper/gradle-wrapper.properties` (already 9.7.1 — verify only)
- `gradle/jib.gradle`, CI image definitions (JRE 21)
- `caching-service/build.gradle` (`spring-boot-starter-cache`), `discovery-service/build.gradle` (possible Jersey pin if Java 21 is rejected)
- `onboarding-enabler-micronaut-sample-app/build.gradle` (Shadow plugin id)
- `platform/build.gradle`, `security-service-client-spring/build.gradle`, `zaas-service/build.gradle`, `mock-services/build.gradle`

**Discovery (the hard part)**
- `discovery-service/.../ApimlInstanceRegistry.java`
- `discovery-service/.../config/EurekaConfig.java`
- `discovery-service/.../config/HttpWebSecurityConfig.java`
- `discovery-service/.../config/DiscoveryErrorController.java`
- `discovery-service/.../staticdef/StaticServicesRegistrationService.java`
- `discovery-service/.../staticdef/ServiceDefinitionProcessor.java`
- `discovery-service/.../eureka/RefreshablePeerEurekaNodes.java`

**Modulith**
- `apiml/src/main/java/org/zowe/apiml/{ApimlApplication,EurekaConfiguration,ModulithConfig,RouteRefreshListener}.java`
- `apiml/src/test/.../RefreshablePeerEurekaNodesTest.java`

**Servlets**
- `zaas-service/.../CustomErrorStatusHandlingBean.java`, `.../config/NewSecurityConfiguration.java`
- `api-catalog-services/.../controllers/handlers/CustomErrorStatusHandlingBean.java`
- `caching-service/.../health/ApimlCachesEndpoint.java`
- `apiml-tomcat-common/.../gzip/GZipResponseUtils.java`

**Docs**
- `docs/sb4-migration-status.md` → user-facing migration note
- CHANGELOG

---

## Risks, tradeoffs, open questions

1. **Eureka + Spring Framework 7 is unproven.** Netflix Eureka 2.0.6 is unchanged across the upgrade and in maintenance mode. The June notes show Spring Cloud Netflix 5.0.1 breaking subclassing into `InstanceRegistry`, and the modulith does `MethodHandles` reflection into Eureka internals. **Mitigation:** Task 9's runtime contract test, and a hard 3-day timebox on Phase 3 — if breached, pull the V4 fork-and-in-source Eureka work forward instead of extending the fight. Doing that fork early is cheaper than discovering it during V4.
2. **The 3.5 OSS window is already closed** (30 Jun 2026), not upcoming as the June ticket assumed. This changes the risk profile: the exposure is live, not scheduled. That argues for landing the modulith slice on the 4.1 line and cutting a release, rather than holding the whole upgrade for the microservice modules.
3. **Java 21 vs 17 is a real fork in the road.** Staying on 17 means maintaining Jersey resolution pins and fighting class-file mismatches in any future dependency that ships Java 21 bytecode. Moving to 21 touches every CI image, build script, and JIB base image — but the enablers and the modulith already assume it.
4. **Jackson 2 shim is technical debt with a deadline.** `spring-boot-jackson2` exists because Jersey 4.0 cannot use Jackson 3. Jackson 2 is deprecated for removal in SB 4.3, which is the next release train after 4.2 (Nov 2026). If we do not plan Jackson 3 now, we do this work again in six months. **Open question:** is there any appetite to migrate the Jersey-based path (`discovery-service` peer replication, static definitions) off Jersey entirely, since the modulith-only V4 removes the biggest Jersey consumer?
5. **Duplicate code paths multiply the work.** Gateway routing and auth must work in both microservice and modulith modes; the SB4 diff also touches both. This is pre-existing debt (#1 in the architecture notes) that this upgrade amplifies — another argument for the modulith-first milestone.
6. **Test surface fragility.** Integration tests are known-flaky around the SAF provider and modulith modes. A flaky CI run after a framework major bump is ambiguous evidence, so budget for stabilising the flaky suites first or running them twice.
7. **Container deployment must not be the acceptance vehicle.** Containerised API ML already breaks mTLS, OIDC, Passticket, AT-TLS, and ICSF. A green container build proves nothing about the z/OS paths that SB4 is most likely to disturb.
8. **Open questions for the squad:** (a) does the release train that carries SB4 need to preserve microservices mode at all, or can we ship modulith-only and delete the standalone modules in the same release? (b) Who owns the z/OS USS acceptance run and when is an LPAR available? (c) Is enterprise support for 3.5 being purchased as a bridge, or is the intent to have zero window without OSS support?

---

## Suggested sequencing

```
Week 1   Tasks 1-4    rebase, Java 21, version pins        (modulith + all libs compile)
Week 1-2 Tasks 5-9    discovery-service / Eureka          (TIMEBOXED, highest risk)
Week 2   Tasks 10-11  modulith wiring + config properties  ← shippable modulith-only milestone
Week 3   Tasks 12-15  microservice modules                 (or delete them)
Week 3-4 Tasks 16-20  full build, z/OS IT, CVEs, enablers, docs
```

The milestone worth defending is after Task 11: **the modulith runs on Spring Boot 4.1 on a supported framework, and the microservice modules are a separate, explicitly-scoped tail.** Shipping that first is the difference between "we are on an EOL framework for another two months" and "we are not."
