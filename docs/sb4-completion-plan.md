# Spring Boot 4 upgrade — completion plan (assessment-based)

Branch `feat/sb4-upgrade` → `v3.x.x`. 28 commits, 178 files. Baseline for comparison: `origin/v3.x.x`.

## Assessed state (measured, not assumed)

**Compiles green.** All modules. CI `BuildAndTest (17|21|25)` reaches the test phase on all three JDKs —
no `invalid source release` — so the Java 17 bytecode-target fix holds across the matrix.

**Unit tests: 21/25 modules green.** Failing, with CI-verified counts:

- gateway-service — 700 tests / **23 fail**
- api-catalog-services — 185 / **13 fail**
- caching-service — 327 / **11 local, 10 CI** (3 local-only failures are a `cs_CZ` locale artifact)
- apiml — 451 / **4 fail**

**Test inventory vs baseline: 653 test files on HEAD vs 655 on `origin/v3.x.x`.** The 2 missing files are
`apiml-common/.../compatibility/ApimlDiscoveryCompositeHealthContributorTest.java` and
`ApimlHealthCheckHandlerTest.java`, deleted by `129cef5d7` together with the production classes they test.
Assessment: the deleted classes were **self-documented temporary shims** ("NOTE: This should be removed when
the APIML upgrades to Spring Cloud 3.x") and their upstream replacements
(`DiscoveryCompositeHealthContributor` in spring-cloud-commons 5.0.3, `EurekaHealthCheckHandler` in
spring-cloud-netflix-eureka-client 5.0.2) are now on the classpath — verified present in the resolved jars.
The deleted tests were themselves copies of upstream Spring Cloud tests. **Requires explicit approval before
this counts as settled** (see Questions for the owner).

**Coverage: no threshold is configured anywhere in the repo.** `gradle/coverage.gradle` has no
`jacocoTestCoverageVerification` and no `violationRules`; CI only *publishes* coverage
(`./gradlew coverage sonar`) in `integration-tests.yml`. There is therefore no existing gate to weaken, and
no pre-migration number to regress against. A >90% line threshold is a **new** gate. Baseline coverage must
be measured on `origin/v3.x.x` first to know what is being improved.

**No test is `@Disabled`/`@Ignore`/excluded** — the 2 file deletions above are the only inventory delta, and
no tests were renamed out of discovery.

**Integration tests: 190 files in `integration-tests/src/test` on both HEAD and `origin/v3.x.x`** — the
inventory is intact. They run only in CI (container/JIB based).

## Work plan

1. Establish the pre-migration coverage baseline by running `./gradlew coverage` on a clean `v3.x.x`
   worktree, so the 90% target has a reference point.
2. Fix the 4 failing modules to green, root causes in priority order:
   a. api-catalog `TokenControllerTest` PKIX truststore failure (11 of 13 failures, single cause).
   b. gateway x-forwarded-header propagation (14 of 23), then the 5 WebSocket handshakes.
   c. the conditional-on-bean startup issue (8 tests) — **needs an owner decision first**, because the gate
      may be intentional and the test may encode the wrong expectation.
   d. `HttpHeaders`→`MultiValueMap` casts (4), `apiml` `threadLocal` NPE (3), caching JSON/Redis (6).
3. Only then: measure real coverage, add behaviour-focused tests for uncovered migration-critical paths,
   and add a `jacocoTestCoverageVerification` gate at the measured threshold.
4. Run the CI-equivalent command chain (`clean build`, checkstyle, jib packaging) at the end.
5. Fully green remote CI before calling anything complete.

Steps 1 and 2a are the immediate next actions.
