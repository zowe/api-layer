# Testing Resource Budgets

This document defines the operating modes, resource budgets and evidence schema for the api-layer
resource-reduction programme: the work that makes local feedback and CI qualification fit inside a
bounded, measured resource envelope without losing coverage.

## 1. Scope

What this document governs:

- The three operating modes (local focused, CI full qualification, CI fast PR lane) and what each one
  is allowed to skip.
- The resource budget profiles that Gradle invocations, test JVMs and service containers are expected
  to run inside, and how those values are ratified.
- The evidence schema and collector used to measure a run, and the rules for reading the numbers.
- The rollout and rollback conditions for tightening any budget.

What this document does not govern:

- Production launch defaults. Nothing here is a recommendation for deployed API ML services; see
  [local-configuration.md](local-configuration.md) for local runtime settings.
- The mandatory per-PR test inventory. That is owned by the workflows and can only change after the
  equivalence evidence described in section 7 exists.
- Scheduling policy (runner sizing, fleet concurrency, queue priorities). Savings here come from
  build/setup/cache reuse and bounded execution first; scheduling changes are out of scope.

Implementation status at the time of writing: the first PR contains resource instrumentation (the
collector described in section 4), the build-cache credential/cache-path fix, Java-only setup for
Java-only jobs and workflow concurrency cancellation. It contains **no test-selection change**. The
local-small profile, bundled test inputs, report-only coverage, topology consolidation and
change-aware selection are **not yet implemented**; they are planned in later PRs (section 7).

## 2. Operating modes

### Mode A - local focused mode (default developer feedback)

- Named scenario/test selection instead of a full run. No root `clean build`.
- Reuse compiled outputs and locally built images; do not rebuild what is already up to date.
- Generic routing, authentication and catalog checks run on the three-service modulith topology
  (`apiml` + `discoverable-client` + `mock-services`).
- Cache, provider and proxy work selects a dedicated topology with the real dependencies. Never run
  an Infinispan test against in-memory storage, and never present a non-HA run as an HA test.
- One environment at a time by default, with a unique run id, network, ports and volumes so a
  leftover environment cannot be mistaken for a live one.
- Live integration tests always execute against a live environment. Their results are never cached
  as if they were unit tests.

**Provisional local acceptance target:** a 4 vCPU / 8 GiB Linux host, with the aggregate measured
peak for the run's process group at or below 6 GiB, measured in the warm scenario (cached inputs).
This target is provisional: it must be ratified after measurement. If measurement shows the target is
not reachable, the correct response is to report the dominant consumers, not to silently raise the
number.

### Mode B - CI full qualification

- All existing deployment, JDK, security, HA/chaos, provider, UI and enabler coverage stays
  represented. Mode B is not a coverage reduction.
- Savings come from build/setup/cache reuse and bounded execution, before scheduling policy.
- Images are built once for the exact tested SHA and consumed by the jobs that need them.
- Service state is never reused across jobs.

### Mode C - CI fast PR lane

- Introduced only after equivalence/shadow evidence exists (section 7).
- Core smoke contract in standalone and modulith modes, all relevant unit tests, and affected
  integration scenarios.
- Shared-infrastructure changes automatically expand to the full suite.
- Full qualification stays mandatory for dependency/platform upgrades (for example Spring Boot 4),
  security or shared routing changes, release qualification, and during selection rollout.
- A full-suite manual override and periodic full runs are required. Nightly runs alone are not
  sufficient protection for high-risk merge changes.

## 3. Resource budget profiles

Every value below is a **starting hypothesis - to be ratified by measurement**. These are benchmark
inputs for the collector, not established safe values, and not production settings. A budget becomes
a limit only after at least three comparable full-baseline runs (Mode B) and focused cold/warm local
runs have been collected and reviewed.

| Profile | Gradle heap (`org.gradle.jvmargs`) | Unit-test JVM heap (`maxHeapSize`) | Integration-test JVM heap | Service containers | `org.gradle.workers.max` | Parallelism (`maxParallelForks`) |
|---|---|---|---|---|---|---|
| `local-focused-small` | 1024m | 512m-768m | 512m | 3 (apiml, discoverable-client, mock-services) | 2, `--no-parallel` | 1 |
| `local-focused-dedicated` (cache/provider/proxy) | 1024m | 512m-768m | 512m, with measured module-specific overrides | topology-specific, 4-6 typical | 2, `--no-parallel` | 1 |
| `ci-fast-pr` | 2048m | 2g | to be measured | 3 | to be measured | 1 |
| `ci-full-qualification` | 2048m (current value) | 2g (current value) | to be measured | as declared per job | to be measured | 1 (current value) |
| `ha-chaos-qualification` | 2048m | 2g | to be measured | 14 (largest declared topology) | to be measured | 1 |

The starting service envelope to test for the three-service local profile, per the same
"starting hypothesis" rule:

| Service | Container memory | JVM heap |
|---|---|---|
| `apiml` (modulith) | 1536m | 768m |
| `discoverable-client` | 768m | 384m |
| `mock-services` | 768m | 384m |

A Spring-context-heavy suite that fails or thrashes inside these values gets a measured
module-specific override rather than a global increase. Under-sizing is never hidden by raising a
test timeout.

Notes on the hypotheses:

- The ordinary `test` task already gets a 2 GiB heap from the root `build.gradle`, and
  `maxParallelForks = 1` is set for all `Test` tasks. The 2 GiB setting lives inside `test {}` and
  does **not** reach the custom integration `Test` tasks, which is why those need explicit budgets.
- `gradle.properties` sets `org.gradle.jvmargs=-Xmx2048m`, `org.gradle.parallel=true` and
  `org.gradle.caching=true`, and sets no `org.gradle.workers.max`. `org.gradle.parallel=true` with
  no worker cap is why one fork per task does not bound a run: several modules can be testing at
  once. The worker cap and `--no-parallel` in the table are hypotheses to be validated, not current
  settings.
- The container and host budgets must include the remaining build/test processes. Bounding Gradle
  does not bound services that are launched outside it, and vice versa.
- CPU budgets must be compatible with IBM Semeru/OpenJ9 and the actual platform; HotSpot-only
  tuning flags must not be pasted into shared defaults. Verify how the chosen JVM recognises cgroups
  before relying on its ergonomics.
- `integration-tests:test` is disabled (`test.enabled = false`) and must never be used as a success
  proxy for anything in this table.
- Service-container counts are per environment, not per job: the integration workflow declares 268
  service-container instances summed over expanded jobs, and that number is not simultaneous demand.

## 4. Evidence schema and collection

### 4.1 Collector CLI

`scripts/ci/collect-resources.sh` is the measurement instrument. It is opt-in per run and is wired
into the shared setup (start) and teardown (stop) actions so that every job is covered without
editing each job.

```shell
scripts/ci/collect-resources.sh start --label <name> [--out-dir <dir>] [--interval <s>]
scripts/ci/collect-resources.sh mark <phase>
scripts/ci/collect-resources.sh stop [--label <name>] [--out-dir <dir>] [--log <file>]
scripts/ci/collect-resources.sh summarize --log <file> [--out <path>]
scripts/ci/collect-resources.sh selftest
```

- `start` begins sampling for a labelled run. `--interval` sets the sample period in seconds.
- `mark <phase>` labels the boundary of a phase (for example `gradle-configuration`,
  `service-startup`, `integration-tests`).
- `stop` ends sampling and writes the JSON evidence file; `--log <file>` also ingests the job log.
- `summarize --log <file>` derives everything from a Gradle log alone, for runs where sampling was
  not enabled. It cannot produce cgroup or RSS values, so it reports the log-derived fields only.
- `selftest` verifies the collector itself against known fixtures.

It samples the runner and Docker cgroups plus a simultaneous per-tick RSS sum of build/test
processes. Where `cgroup memory.peak` is available it is used directly; otherwise the value is
labelled a lower bound.

### 4.2 Enabling collection in CI

Collection is off unless requested. Enable it per run with the `workflow_dispatch` input
`collect_resources`, which sets `CI_RESOURCE_COLLECTION=true` for the run. Existing timing data
should be reused where it already exists; new resource metrics are collected only during already
needed runs or runs that are explicitly approved.

### 4.3 JSON evidence schema

`resources.json` uses schema version 1. This is the exact shape emitted by
`collect-resources.sh`; the collector is the reference, and this section describes it.

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-09-23T16:31:57Z",
  "run": {"id": "1234567890", "attempt": "1", "workflow": "Integration tests using JIB containers",
          "job": "CITests", "event": "push", "ref": "v3.x.x", "sha": "1bab2a0f1...", "label": "CITests"},
  "runner": {"os": "Linux", "arch": "x86_64", "cpuCount": 4, "memTotalBytes": 16777216000,
             "memAvailableBytes": 3181973504},
  "jdk": {"version": "17.0.20", "vendor": "IBM Corporation", "home": "/opt/hostedtoolcache/java/17"},
  "environment": {"topology": "docker", "cacheState": "warm", "dockerContainers": 10},
  "durationSeconds": {"total": 412.6, "setup": 50.0, "phasesSource": "log", "compile": null,
                      "readiness": 163.0, "test": 71.0, "report": 1.0},
  "memoryBytes": {"peakSampled": 6583308288, "peakSource": "cgroup.memory.peak",
                  "peakIsLowerBound": false, "cgroupPeak": 6583308288,
                  "containerPeakSum": 1809213028, "sampledSumPeak": 3507736576},
  "cpu": {"usageUsec": 15988018633, "userUsec": 15063529681, "systemUsec": 924488952,
          "nrThrottled": null, "throttledUsec": null},
  "disk": {"workspaceBytes": 27185152, "gradleCacheBytes": 2957905920},
  "oom": {"events": 0, "evidence": ["cgroup memory.events oom+oom_kill=0"]},
  "gradleCache": {"fromCacheTasks": 0, "failedReads": 1, "disabledDuringBuild": true,
                  "dependencyDownloads": 0},
  "tests": {"selected": null, "discovered": null, "executed": 412, "skipped": 3, "failed": 0,
            "result": "success"},
  "samples": {"count": 82, "intervalSeconds": 5, "path": "samples.jsonl", "truncated": false}
}
```

Field by field:

| Field | Meaning |
|---|---|
| `schemaVersion` | Integer version of this schema. Consumers must reject unknown versions. |
| `generatedAt` | RFC 3339 UTC timestamp at which the record was written. |
| `run.id`, `run.attempt` | Workflow run id and attempt; `unknown` outside CI. |
| `run.workflow`, `run.job`, `run.event`, `run.ref`, `run.sha` | The run identity. Two records are only comparable when these and `jdk.version` and `runner.cpuCount` match. |
| `run.label` | The `--label` the run was started with. |
| `runner.os`, `runner.arch` | Platform identity of the machine that produced the numbers. |
| `runner.cpuCount` | Logical CPUs visible to the run; without it a memory peak is not comparable. |
| `runner.memTotalBytes`, `runner.memAvailableBytes` | Memory at the start of collection. |
| `jdk.version`, `jdk.vendor`, `jdk.home` | The JDK used; `null` when it could not be determined, never guessed. |
| `environment.topology` | The `ENV_CONFIG` value (for example `docker-modulith`), or `unknown`. |
| `environment.cacheState` | `cold`, `warm` or `unknown`; `unknown` is honest, not a default to `warm`. |
| `environment.dockerContainers` | Containers running when collection started, including dynamically created Redis instances. |
| `durationSeconds.total` | Sampled window: `start` to `stop`. |
| `durationSeconds.setup` | Duration of the setup phase, when it was marked. |
| `durationSeconds.phasesSource` | `log` when the per-phase values came from log step markers, `marks` when they came from `mark`, `unavailable` otherwise. |
| `durationSeconds.compile`, `.readiness`, `.test`, `.report` | Per-phase durations. Phases may overlap in reality; do not add them and expect `total`. |
| `memoryBytes.peakSampled` | The peak to quote. `cgroupPeak` when the kernel tracked it, otherwise `sampledSumPeak`. |
| `memoryBytes.peakSource` | `cgroup.memory.peak`, `sampled-sum` or `unavailable`. |
| `memoryBytes.peakIsLowerBound` | `true` unless the value came from `memory.peak`. |
| `memoryBytes.cgroupPeak` | Highest value read from the run's cgroup `memory.peak`, when available. |
| `memoryBytes.containerPeakSum` | Highest simultaneous sum across Docker containers. |
| `memoryBytes.sampledSumPeak` | Highest simultaneous per-tick RSS sum over the tracked processes. Never a sum of per-process lifetime peaks. |
| `cpu.usageUsec`, `.userUsec`, `.systemUsec` | From cgroup `cpu.stat`. |
| `cpu.nrThrottled`, `.throttledUsec` | Throttling evidence; `null` when the cgroup did not expose it. |
| `disk.workspaceBytes`, `disk.gradleCacheBytes` | Measured on the host when the record is written. In `summarize` mode these describe the machine running the collector, not the run being summarized. |
| `oom.events` | OOM kill count from cgroup `memory.events`, when readable. |
| `oom.evidence` | Short strings supporting the count, including log evidence such as `OutOfMemoryError`. |
| `gradleCache.fromCacheTasks` | Tasks served `FROM-CACHE`. |
| `gradleCache.failedReads` | `Could not load entry` occurrences, reported separately from dependency downloads. |
| `gradleCache.disabledDuringBuild` | `true` when Gradle logged that it disabled the remote cache mid-build. This must be `false` on a credentialed build. |
| `gradleCache.dependencyDownloads` | `Download http(s)://` lines; dependency resolution, not task output reuse. |
| `tests.selected` | Tests requested by `--tests` or tag filters, when determinable. |
| `tests.discovered`, `.executed`, `.skipped`, `.failed` | Test counts derived from the log. |
| `tests.result` | `success`, `failure` or `unknown`; derived from the Gradle build result. |
| `samples.count`, `samples.intervalSeconds` | Sampling ticks and period. A low count weakens every sampled peak. |
| `samples.path` | The `samples.jsonl` file holding the raw ticks. |
| `samples.truncated` | `true` when the sample cap was reached and the oldest ticks were dropped. |

CSV export header, one row per run (`resources.csv`):

```csv
label,sha,workflow,job,jdk,cpuCount,memTotalBytes,durationTotalSeconds,peakMemoryBytes,peakSource,cpuUsageUsec,nrThrottled,gradleFromCache,gradleFailedReads,gradleCacheDisabled,testsExecuted,testsFailed,result
```

### 4.4 Java-only jobs and the Node opt-in

The shared setup action gains an input `setupNode` (values `'true'`/`'false'`, default `'true'` for
backward compatibility) and every call site states it explicitly. Java-only consumers set `'false'`,
which skips Node setup, the global npm install and all six npm/`node_modules` cache restores.

Evidence required for a Java-only job: `./gradlew <task> --dry-run` for every integration test task
used in CI shows a task graph with zero npm/node tasks (the `gradle.npm_tasks` field in section 4.3),
and the job consumes images published once by `PublishJibContainers` instead of building them.

Node stays enabled for `PublishJibContainers` (builds the Node and Python sample apps), `E2EUITests`
and `E2EUITestsModulith` (`npm run cy:e2e`), `CITestsNodeJsAndPythonServices` (its task graph does
contain an npm/node task), `PublishResults` (the coverage task graph contains npm tasks), and the
service-registration `BuildAndTest` matrix (root `clean build`).

## 5. How to read the numbers

- **A sampled peak is not a peak.** The collector sees the process table at each tick. A short-lived
  allocation between two ticks is invisible, so a sampled peak is a lower bound on the true peak.
  Report it as such.
- **A sampled RSS sum is a lower bound for a second reason:** it misses kernel memory (page cache,
  socket buffers) and any process that starts and exits inside one interval.
- **Never add per-process lifetime RSS peaks.** Each process's peak occurs at a different time;
  adding them produces a number that no machine ever had to supply. Use the simultaneous per-tick
  sum, or better, the cgroup value.
- **Prefer `cgroup memory.peak`.** It is tracked by the kernel continuously and includes page cache
  and children, so it is the closest available answer to "how much memory did this run need". Fall
  back to sampled `memory.current` only when `memory.peak` is unavailable, and label the result a
  lower bound.
- **A fleet cap trades wall time for peak demand.** Running fewer jobs at once lowers peak host
  memory and raises elapsed feedback time. It does not reduce total work: aggregate job-minutes stay
  roughly constant. Do not report a fleet cap as a saving in aggregate job-time.
- **Report savings as separate axes.** Peak host memory, aggregate job-time, elapsed feedback time,
  storage/network volume and flake rate are five different numbers. Never add them into a single
  percentage, and never sum the phases: their effects overlap (a shorter build also shortens setup,
  and a warmer cache changes both).

### Measured baseline

The reference point for the hypotheses above, verified against revision
`1bab2a0f1e71cea722d03239192ed2bf06f90e8a` and run `35860743885`:

| Measurement | Value |
|---|---|
| Integration workflow declared jobs | 33 (41 after matrix expansion) |
| Declared service-container instances, summed over expanded jobs | 268 (not simultaneous) |
| Service containers with an explicit `--memory` limit | 3 |
| Services declared by `CITests` | 10 |
| Services declared by `CITestsModulith` | 3 (apiml, discoverable-client, mock-services) |
| Largest declared topology | 14 services (`CITestsHA_caching-chaotic`) |
| Successful integration run | 214.87 aggregate job-minutes, 22.25 minutes elapsed |
| Shared setup action alone | 39.83 aggregate job-minutes (a cost, not a savings estimate) |
| Build/registration workflow | 52.58 aggregate job-minutes |
| Combined, excluding other workflows | 267.45 job-minutes |
| Standard integration job log | about 35 MB |

`CITests` and `CITestsModulith` are not semantically identical, so their service counts are not
interchangeable when budgeting. The 39.83 minutes in the shared setup action is a measured cost of the
current setup, not an estimate of what fixing it will save.

## 6. Budget rules for services

- **Heap is only part of RSS.** A service container's resident memory also includes direct buffers,
  metaspace and class storage, native libraries (TLS, JVM internals), thread stacks and, in
  instrumented runs, the coverage agent. A container budget must be set against measured RSS, not
  against the configured heap, and a coverage-instrumented job must not be compared with the same job
  without the agent as if they were the same run.
- **Do not assume package `start.sh` heap defaults apply to Jib startup.** The package start scripts
  default to `-Xms32m -Xmx512m` via `ZWE_configs_heap_init`/`ZWE_configs_heap_max`, but Jib-started
  containers do not go through those scripts. The effective heap in a Jib-launched service is
  whatever the image entrypoint and JVM arguments set, and must be verified per topology.
- **Test-only limits must never become production launch defaults.** A `--memory` limit or a lowered
  heap introduced to fit a test topology is a test constraint. It must not be copied into packaging
  or deployment manifests.
- **The HA topology needs its own budget.** Fourteen services with their own JVMs, TLS material and
  coverage agents cannot be squeezed into the small local envelope. Give it a separate profile and
  measure it separately.

## 7. Rollout and rollback

Rollout order (later PRs; only step 0 is in the first PR):

0. **First PR (implemented now):** resource instrumentation, the build-cache credential/cache-path
   fix, Java-only setup (no Node/npm in Java-only jobs) and workflow concurrency cancellation. No
   test-selection change.
1. **PR 2:** opt-in `local-small` profile plus an isolated three-service wrapper with explicit
   budgets and cleanup.
2. **PR 3:** cache-backed/bundled test inputs, report-only coverage and non-Java build deduplication.
3. **PR 4:** scenario manifest, minimal topologies and carefully isolated suite grouping.
4. **PR 5:** optional, validated change-aware selection with a capped fleet.

Requirements for any step that changes what runs:

- The profile is behind a reversible flag. Turning the flag off restores the previous behaviour
  without a revert.
- Shadow comparison before enabling: full-suite test identities, outcomes and coverage are compared
  against the current pipeline for the same revision, and the differences are explained.
- Resource-sensitive cases are rerun under the proposed limits: TLS, HA, chaos and WebSocket cases
  specifically, since they are the ones most likely to break first under a tighter budget.
- Stop the rollout if failures become less detectable (a scenario stops failing when it should fail)
  or if flakiness rises. Both are rollback triggers, not tuning opportunities.

Rollback: revert the profile flag to its previous value and keep the measurement instrumentation on,
so the failing configuration is measured rather than guessed at.

## 8. Open decisions for maintainers

- **Confirm the local hardware target.** The 4 vCPU / 8 GiB host and the 6 GiB aggregate peak are a
  provisional assumption. Confirm them, or supply the real target, before the local profile is
  ratified.
- **Choose the priority when goals conflict.** Local memory footprint, total CI work and fleet
  concurrency pull against each other. Recommended order: local safety and duplicate-work reduction
  first, fleet concurrency last.
- **Confirm the trusted cache producers.** The mechanism is in place: `GRADLE_REMOTE_CACHE_PUSH` is
  read by `settings.gradle`, is `"false"` at workflow level, and is overridden to
  `${{ github.event_name == 'push' }}` on `PublishJibContainers` (integration workflow) and on
  `BuildAndTest` and `Register` (service-registration workflow). Confirm that producer list, and
  confirm that pull requests stay read-only against the remote build cache.
- **Decide whether to keep the local task-output cache persisted.** `setup` caches
  `~/.gradle/caches/build-cache-1` under a stable key by default. This is the change with the least
  evidence behind it: the directory is 1.3 GiB on a developer machine after many runs, and a CI job's
  copy is smaller but not measured. The collector's first three runs decide it, and
  `cacheBuildOutputs: 'false'` at a call site restores the previous behaviour without a revert.
- **Decide the fork access policy.** Fork PRs run without cache credentials and without privileged
  execution; never `pull_request_target` for untrusted code. Confirm that this is acceptable for
  contributor throughput.
- **Approve mandatory per-PR scenario changes only after equivalence/shadow evidence exists.** Until
  then the full suite stays mandatory for high-risk changes, and a full-suite manual override must
  remain available.
- **Make the CI configuration check a required status check, or leave it advisory.** Adding
  `.github/workflows/ci-config-checks.yml` to the required checks is what turns the invariants into a
  gate. It deliberately has no `paths` filter so that it can never be left "Expected" on a filtered
  pull request. Branch protection on `v3.x.x` currently requires `CITests`, `CITestsRegistration` and
  `BuildAndTest (17)`, `(21)`, `(25)`; adding a check is a branch-protection change, not a repository
  change, and was deliberately not made here.

Two decisions are already made and are not open:

- **Build-cache credentials.** The two covered workflows make credentials available to every Gradle
  invocation through Gradle's `ORG_GRADLE_PROJECT_artifactory_user` /
  `ORG_GRADLE_PROJECT_artifactory_password` convention, so no command line has to repeat them and no
  invocation can be missed. `settings.gradle` registers the remote `HttpBuildCache` only when both
  are present and non-placeholder; otherwise it prints one line
  (`Remote build cache: disabled - no usable credentials provided, using the local cache only`) and
  does not register the remote cache at all, so a missing credential degrades gracefully instead of
  producing an HTTP 403 that disables the remote cache mid-build. Fork PRs receive empty secrets, so
  the remote cache disables itself cleanly there. Push is a strict boolean, read from
  `GRADLE_REMOTE_CACHE_PUSH` or `-Pgradle.cache.push=true`, granted only on push events for the
  producer jobs named above; pull requests read only. The check reports, without failing, the eight
  other workflows that run Gradle without credentials and therefore cannot read the shared cache:
  `automated-release.yml`, `binary-snapshot-release.yml`, `binary-specific-release.yml`,
  `branch-snapshot-release.yml`, `image-publish-branch.yml`, `image-snapshot-release.yml`,
  `image-specific-release.yml`, `infinispan-jgroup-stability-test.yml`.
- **Local task-output cache persistence.** The teardown action deletes only `build-cache-1/*.lock` and
  `build-cache-1/gc.properties`, mirroring what is already done for `modules-2`; it no longer deletes
  the content-addressed `build-cache-1` entries, which is what made the cache useless (the cache
  post-step runs after the job's steps, so the directory was saved empty on every run). Gradle
  dependency caches and the local task-output cache are two separate `actions/cache` steps over
  non-overlapping paths. The dependency cache covers `modules-2`, `jars-*`, `journal-1`,
  `*/transforms`, `*/generated-gradle-jars`, `*/groovy-dsl`, `*/javaCompile` and `wrapper`; the
  task-output cache covers `build-cache-1` only. Both keys include `build.gradle`, `settings.gradle`,
  `gradle.properties`, `gradle/**/*.gradle`, `gradle/**/*.properties` and the per-module build logic;
  the task-output key additionally includes the JDK version, because compiled outputs are toolchain
  specific, and is deliberately stable rather than per-run, because a per-run key would save a fresh
  copy of this directory from every job of every run and exhaust the repository cache quota. The same
  `~/.npm` directory is never cached by several overlapping cache steps.

## 9. Enforcement

`scripts/ci/check-ci-config.py`, run by `.github/workflows/ci-config-checks.yml` with its bats suite
`scripts/test/ci_config_check_test.bats`, fails on a regression of any invariant above: a setup call
site without an explicit `setupNode`, a covered workflow that runs Gradle without build-cache
credentials, an unconditional remote-cache push, an ungated removal of `build-cache-1`, or a workflow
that invokes the disabled `:integration-tests:test` task. On the `v3.x.x` baseline it reports 38
violations; on the tree that fixes them it reports none.

`actionlint` is not a clean gate in this repository: the existing workflows produce 86 pre-existing
`SC2086` info-level findings. The checker above is therefore targeted rather than a general linter.
