# #4239 — Flaky tests

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4239
**Labels:** technical excellence, stale | **Created:** 2025-07-25 | **State:** open

---

## Description

Several integration tests fail intermittently in CI without any code change. The confirmed flaky test is `GatewayCentralRegistry Test [2]`, which asserts `headers.x-forwarded-proto` equals `https` but fails when the assertion runs before the Gateway has finished setting the header. Other tests are suspected to be flaky due to timing issues, shared mutable state, or environment-specific conditions (network latency to z/OS in integration environments).

Flaky tests erode trust in the test suite: developers start ignoring CI failures, and real regressions can be masked.

---

## Acceptance Criteria

- All tests identified as flaky in this issue pass consistently across 10 consecutive CI runs on the main branch.
- No `Thread.sleep()` calls remain in integration or acceptance tests — all timing-dependent assertions use `Awaitility` with explicit timeouts.
- Tests that share mutable state (Eureka registry, Caching service, Spring context) reset that state in `@AfterEach` or use `@DirtiesContext` where unavoidable.
- No `@Retry` or similar retry annotations are used to mask root causes — if a test requires environmental conditions that cannot be reproduced deterministically, it is quarantined with `@Disabled` and a tracking comment until fixed.
- Each newly identified flaky test is tracked as a sub-issue referencing #4239 before being fixed.

---

## Technical Solution

### Process

1. **Categorise** each flaky test:
   - **Timing-based** — assertion runs before the system reaches the expected state.
   - **State-pollution** — a previous test leaves shared state (Eureka entries, cached data) that affects the next test.
   - **Environment-dependent** — relies on external services (z/OS, network) with variable latency.

2. **Fix timing-based tests** — replace fixed sleeps with `Awaitility`:

   ```java
   // Before:
   Thread.sleep(5000);
   assertThat(response.header("x-forwarded-proto")).isEqualTo("https");

   // After:
   Awaitility.await()
       .atMost(30, SECONDS)
       .pollInterval(500, MILLISECONDS)
       .untilAsserted(() ->
           given().when().get(url)
               .then().body("headers.x-forwarded-proto", is("https")));
   ```

3. **Fix state-pollution tests** — add `@AfterEach` teardown:

   ```java
   @AfterEach
   void clearCachingServiceState() {
       cachingServiceClient.deleteForService(TEST_SERVICE_ID);
   }
   ```

   Reserve `@DirtiesContext` for tests that genuinely pollute global static state (e.g., modifying `System` properties). It restarts the full Spring context and is expensive.

4. **Quarantine** tests that depend on z/OS or external network and cannot be made deterministic:

   ```java
   @Disabled("Flaky due to z/OS network latency — tracked in #4239")
   @Test
   void myZosTest() { ... }
   ```

   Add a `@Tag("flaky")` tag so a separate, non-blocking CI stage can run quarantined tests.

### Files to change

- Each flaky test file identified — replace `Thread.sleep()` with `Awaitility`, add `@AfterEach` teardown where needed.
- `.github/workflows/ci.yml` — add a non-blocking `flaky-tests` stage for `@Tag("flaky")` tests.

### Tests

- **Acceptance criterion**: the `GatewayCentralRegistry Test [2]` (and all other identified flaky tests) must pass in 10 consecutive CI runs after the fix.
- **Regression guard**: add a CI step that counts `Thread.sleep()` calls in `integration-tests/` and fails if the count increases: `grep -r 'Thread.sleep' integration-tests/src/test/ | wc -l`.
- **New flaky-test CI stage** using JUnit `@Tag("flaky")` with a single retry allowed, isolated from the main build gate, so quarantined tests are still exercised without blocking merges.
