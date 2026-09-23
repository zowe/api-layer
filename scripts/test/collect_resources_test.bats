#!/usr/bin/env bats

################################################################################
# BATS tests for scripts/ci/collect-resources.sh
#
# To run these tests:
#   1. Install BATS: brew install bats-core (macOS) or apt install bats (Linux)
#   2. Run: bats scripts/test/collect_resources_test.bats
#
# The suite needs no network and no docker: docker-dependent assertions are
# skipped with a message when the docker CLI is missing.
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

# Setup function runs before each test
setup() {
    BATS_TEST_DIRNAME="$(cd "$(dirname "$BATS_TEST_FILENAME")" && pwd)"
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../.." && pwd)"
    SCRIPTS_DIR="${PROJECT_ROOT}/scripts"
    COLLECT="${SCRIPTS_DIR}/ci/collect-resources.sh"

    TEST_TEMP_DIR="$(mktemp -d)"
    OUT="${TEST_TEMP_DIR}/out"

    # keep every docker call short so the suite stays fast
    export CI_RESOURCE_DOCKER_TIMEOUT=5
}

# Teardown function runs after each test
teardown() {
    # a cancelled test can leave the detached sampler behind; stop it by pid
    local state="${OUT}/state.json" pid=""
    if [ -f "$state" ] && command -v jq >/dev/null 2>&1; then
        pid="$(jq -r '.samplerPid // empty' "$state" 2>/dev/null)"
    fi
    case "${pid:-}" in
        '' | *[!0-9]*) : ;;
        *) kill -TERM "$pid" 2>/dev/null || true ;;
    esac
    if [ -n "${TEST_TEMP_DIR:-}" ] && [ -d "${TEST_TEMP_DIR}" ]; then
        rm -rf "${TEST_TEMP_DIR}"
    fi
}

# jq -e expression asserting every required key of the schemaVersion 1 document
assert_schema() {
    local json="$1" expr
    expr='
      .schemaVersion == 1
      and (.generatedAt | type == "string")
      and (.run | (.id | type == "string")
             and (.attempt | type == "string")
             and (.workflow | type == "string")
             and (.job | type == "string")
             and (.event | type == "string")
             and (.ref | type == "string")
             and (.sha | type == "string")
             and (.label | type == "string"))
      and (.runner | (.os | type == "string")
             and (.arch | type == "string")
             and ((.cpuCount | type == "number") or .cpuCount == null)
             and ((.memTotalBytes | type == "number") or .memTotalBytes == null)
             and ((.memAvailableBytes | type == "number") or .memAvailableBytes == null))
      and (.jdk | has("version") and has("vendor") and has("home"))
      and (.environment | (.topology | type == "string")
             and (.cacheState | test("^(cold|warm|unknown)$"))
             and ((.dockerContainers | type == "number") or .dockerContainers == null))
      and (.durationSeconds | ((.total | type == "number") or .total == null)
             and ((.setup | type == "number") or .setup == null)
             and (.phasesSource | test("^(log|marks|unavailable)$"))
             and ((.compile | type == "number") or .compile == null)
             and ((.readiness | type == "number") or .readiness == null)
             and ((.test | type == "number") or .test == null)
             and ((.report | type == "number") or .report == null))
      and (.memoryBytes | ((.peakSampled | type == "number") or .peakSampled == null)
             and (.peakSource | test("^(cgroup.memory.peak|sampled-sum|unavailable)$"))
             and (.peakIsLowerBound | type == "boolean")
             and ((.cgroupPeak | type == "number") or .cgroupPeak == null)
             and ((.containerPeakSum | type == "number") or .containerPeakSum == null)
             and ((.sampledSumPeak | type == "number") or .sampledSumPeak == null))
      and (.cpu | ((.usageUsec | type == "number") or .usageUsec == null)
             and ((.userUsec | type == "number") or .userUsec == null)
             and ((.systemUsec | type == "number") or .systemUsec == null)
             and ((.nrThrottled | type == "number") or .nrThrottled == null)
             and ((.throttledUsec | type == "number") or .throttledUsec == null))
      and (.disk | ((.workspaceBytes | type == "number") or .workspaceBytes == null)
             and ((.gradleCacheBytes | type == "number") or .gradleCacheBytes == null))
      and (.oom | ((.events | type == "number") or .events == null)
             and (.evidence | type == "array"))
      and (.gradleCache | ((.fromCacheTasks | type == "number") or .fromCacheTasks == null)
             and ((.failedReads | type == "number") or .failedReads == null)
             and ((.disabledDuringBuild | type == "boolean") or .disabledDuringBuild == null)
             and ((.dependencyDownloads | type == "number") or .dependencyDownloads == null))
      and (.tests | ((.selected | type == "number") or .selected == null)
             and ((.discovered | type == "number") or .discovered == null)
             and ((.executed | type == "number") or .executed == null)
             and ((.skipped | type == "number") or .skipped == null)
             and ((.failed | type == "number") or .failed == null)
             and (.result | test("^(success|failure|unknown)$")))
      and (.samples | ((.count | type == "number") or .count == null)
             and ((.intervalSeconds | type == "number") or .intervalSeconds == null)
             and ((.path | type == "string") or .path == null))
    '
    run jq -e "$expr" "$json"
    [ "$status" -eq 0 ] || {
        echo "schema assertion failed for $json:"
        echo "$output"
        jq . "$json" || true
        return 1
    }
}

################################################################################
# CLI surface
################################################################################

@test "collect-resources: --help exits 0 and lists every subcommand" {
    run "$COLLECT" --help

    [ "$status" -eq 0 ]
    [[ "$output" == *"start --label"* ]]
    [[ "$output" == *"mark <phase-name>"* ]]
    [[ "$output" == *"stop ["* ]]
    [[ "$output" == *"summarize --log"* ]]
    [[ "$output" == *"selftest"* ]]
}

@test "collect-resources: unknown command exits non-zero" {
    run "$COLLECT" not-a-command

    [ "$status" -ne 0 ]
}

################################################################################
# start / stop
################################################################################

@test "collect-resources: start prints the out-dir and records state plus samples" {
    run "$COLLECT" start --label unit-test --out-dir "$OUT" --interval 1

    [ "$status" -eq 0 ]
    [ "$output" = "$OUT" ]
    [ -f "${OUT}/state.json" ]
    [ -f "${OUT}/samples.jsonl" ]
    [ -s "${OUT}/state.json" ]

    run jq -e '.label == "unit-test" and (.samplerPid | type == "number") and .interval == 1' "${OUT}/state.json"
    [ "$status" -eq 0 ]

    sleep 4
    "$COLLECT" stop --label unit-test --out-dir "$OUT"

    # the sampler was really running: at least one sample line landed
    [ "$(wc -l < "${OUT}/samples.jsonl")" -ge 1 ]
    run jq -e 'has("timestamp") and has("memCurrentBytes") and has("rssSumBytes") and has("containerSumBytes") and (.cpu | has("usageUsec"))' "${OUT}/samples.jsonl"
    [ "$status" -eq 0 ]
}

@test "collect-resources: start+stop produce a schema-valid resources.json, csv and summary" {
    run "$COLLECT" start --label schema-test --out-dir "$OUT" --interval 1
    [ "$status" -eq 0 ]

    sleep 4
    run "$COLLECT" stop --label schema-test --out-dir "$OUT"
    [ "$status" -eq 0 ]
    [ -f "${OUT}/resources.json" ]

    assert_schema "${OUT}/resources.json"

    run jq -e '.run.label == "schema-test" and (.durationSeconds.total | type == "number")' "${OUT}/resources.json"
    [ "$status" -eq 0 ]

    # bounded artifact: resources.json stays small
    [ "$(wc -c < "${OUT}/resources.json")" -lt 20480 ]

    # csv: exact header, one row, 18 fields
    run head -n 1 "${OUT}/resources.csv"
    [ "$status" -eq 0 ]
    [ "$output" = "label,sha,workflow,job,jdk,cpuCount,memTotalBytes,durationTotalSeconds,peakMemoryBytes,peakSource,cpuUsageUsec,nrThrottled,gradleFromCache,gradleFailedReads,gradleCacheDisabled,testsExecuted,testsFailed,result" ]

    run awk -F',' 'END { print NR }' "${OUT}/resources.csv"
    [ "$output" -eq 2 ]

    # csv is append-safe: a second stop adds a row instead of rewriting the header
    "$COLLECT" stop --label schema-test-2 --out-dir "$OUT" 2>/dev/null
    run awk -F',' 'END { print NR }' "${OUT}/resources.csv"
    [ "$output" -eq 3 ]
    run head -n 1 "${OUT}/resources.csv"
    [ "$output" = "label,sha,workflow,job,jdk,cpuCount,memTotalBytes,durationTotalSeconds,peakMemoryBytes,peakSource,cpuUsageUsec,nrThrottled,gradleFromCache,gradleFailedReads,gradleCacheDisabled,testsExecuted,testsFailed,result" ]
}

@test "collect-resources: stop tolerates a completely missing state and samples" {
    mkdir -p "$OUT"

    run "$COLLECT" stop --out-dir "$OUT"
    local stop_output="$output"

    [ "$status" -eq 0 ]
    [ -f "${OUT}/resources.json" ]
    assert_schema "${OUT}/resources.json"

    # every optional metric is null, nothing is fabricated
    run jq -e '
        .samples.count == 0
        and .samples.path == null
        and .memoryBytes.sampledSumPeak == null
        and .memoryBytes.containerPeakSum == null
        and .memoryBytes.cgroupPeak == null
        and .memoryBytes.peakSampled == null
        and .memoryBytes.peakSource == "unavailable"
        and .memoryBytes.peakIsLowerBound == true
        and .durationSeconds.total == null
        and .durationSeconds.phasesSource == "unavailable"
        and .gradleCache.fromCacheTasks == null
        and .gradleCache.disabledDuringBuild == null
        and .tests.executed == null
        and .tests.result == "unknown"
    ' "${OUT}/resources.json"
    [ "$status" -eq 0 ] || {
        echo "$output"
        jq . "${OUT}/resources.json"
        return 1
    }

    # the missing metrics were reported one warning per metric, on stop's own output
    [[ "$stop_output" == *"warning"* ]]
}

@test "collect-resources: stop tolerates samples deleted mid-run (cancellation path)" {
    run "$COLLECT" start --label cancelled --out-dir "$OUT" --interval 1
    [ "$status" -eq 0 ]

    sleep 1
    rm -f "${OUT}/samples.jsonl"

    run "$COLLECT" stop --out-dir "$OUT"

    [ "$status" -eq 0 ]
    assert_schema "${OUT}/resources.json"
    run jq -e '.samples.count == 0 and .run.label == "cancelled"' "${OUT}/resources.json"
    [ "$status" -eq 0 ]
}

@test "collect-resources: samples.jsonl is capped and truncation is recorded" {
    CI_RESOURCE_SAMPLE_CAP=1 run "$COLLECT" start --label capped --out-dir "$OUT" --interval 1
    [ "$status" -eq 0 ]

    sleep 8
    CI_RESOURCE_SAMPLE_CAP=1 "$COLLECT" stop --out-dir "$OUT" 2>/dev/null

    run jq -e '.samples.count <= 1 and .samples.truncated == true' "${OUT}/resources.json"
    [ "$status" -eq 0 ] || {
        echo "$output"
        jq '.samples' "${OUT}/resources.json"
        return 1
    }
    [ "$(wc -l < "${OUT}/samples.jsonl")" -le 1 ]
}

################################################################################
# summarize (log-derived facts and phase durations)
################################################################################

write_gradle_log_fixture() {
    cat > "$1" << 'GRADLE_LOG'
2026-09-23T18:00:00.0000000Z ##[group]Run ./gradlew build compileJava -Partifactory_password=swordfish-9f8e7d6c -Dsonar.token=tok-abcdef123456 -Dnote=swordfish-9f8e7d6c
2026-09-23T18:00:30.0000000Z Task :apiml-common:compileJava FROM-CACHE
2026-09-23T18:00:31.0000000Z Could not load entry 6b1c1f2e from remote build cache: 403 Forbidden
2026-09-23T18:00:32.0000000Z The remote build cache was disabled during the build due to errors
2026-09-23T18:00:33.0000000Z Download https://artifactory.example.com/libs/foo-1.0.jar
2026-09-23T18:00:40.0000000Z Test com.example.FooTest PASSED
2026-09-23T18:00:41.0000000Z Test com.example.BarTest PASSED
2026-09-23T18:00:42.0000000Z Test com.example.BazTest PASSED
2026-09-23T18:00:50.0000000Z BUILD SUCCESSFUL in 50s
2026-09-23T18:01:00.0000000Z ##[group]Run ./gradlew jacocoTestReport coverage
2026-09-23T18:01:20.0000000Z BUILD SUCCESSFUL
GRADLE_LOG
}

@test "collect-resources: summarize derives gradle cache, test and phase facts from a log" {
    local log="${TEST_TEMP_DIR}/gradle.log"
    write_gradle_log_fixture "$log"

    run "$COLLECT" summarize --log "$log" --out "${OUT}/resources.json" --label log-test
    [ "$status" -eq 0 ]
    [ -f "${OUT}/resources.json" ]
    assert_schema "${OUT}/resources.json"

    run jq -e '
        .run.label == "log-test"
        and .gradleCache.fromCacheTasks == 1
        and .gradleCache.failedReads == 1
        and .gradleCache.disabledDuringBuild == true
        and .gradleCache.dependencyDownloads == 1
        and .tests.executed == 3
        and .tests.failed == 0
        and .tests.skipped == 0
        and .tests.result == "success"
        and .durationSeconds.phasesSource == "log"
        and .durationSeconds.compile == 60
        and .durationSeconds.report == 20
    ' "${OUT}/resources.json"
    [ "$status" -eq 0 ] || {
        echo "$output"
        jq . "${OUT}/resources.json"
        return 1
    }
}

@test "collect-resources: summarize reports BUILD FAILED as result failure" {
    local log="${TEST_TEMP_DIR}/failed.log"
    printf '%s\n' \
        "2026-09-23T18:00:00.0000000Z ##[group]Run ./gradlew runIntegrationTests" \
        "2026-09-23T18:00:10.0000000Z Test com.example.FooTest FAILED" \
        "2026-09-23T18:00:30.0000000Z BUILD FAILED in 30s" > "$log"

    run "$COLLECT" summarize --log "$log" --out "${OUT}/resources.json" --label failed
    [ "$status" -eq 0 ]

    run jq -e '.tests.failed == 1 and .tests.executed == 1 and .tests.result == "failure" and .durationSeconds.test == 30' "${OUT}/resources.json"
    [ "$status" -eq 0 ] || {
        echo "$output"
        jq . "${OUT}/resources.json"
        return 1
    }
}

################################################################################
# redaction
################################################################################

@test "collect-resources: secrets are redacted from every output file" {
    local log="${TEST_TEMP_DIR}/secret.log"
    local summary="${TEST_TEMP_DIR}/step-summary.md"
    export FAKE_ARTIFACTORY_PASSWORD="swordfish-9f8e7d6c"
    export FAKE_SONAR_TOKEN="tok-abcdef123456"
    export GITHUB_STEP_SUMMARY="$summary"

    write_gradle_log_fixture "$log"

    run "$COLLECT" start --label redact --out-dir "$OUT" --interval 1
    [ "$status" -eq 0 ]

    sleep 2
    run "$COLLECT" stop --label redact --out-dir "$OUT" --log "$log"
    [ "$status" -eq 0 ]

    # the summary exists and really did carry the step command (so redaction mattered)
    [ -f "$summary" ]
    run grep -c 'Partifactory_password' "$summary"
    [ "$status" -eq 0 ]
    [ "$output" -ge 1 ]

    # ... but no secret value survived anywhere
    run grep -rl "swordfish-9f8e7d6c" "$OUT" "$summary"
    [ "$status" -ne 0 ]
    run grep -rl "tok-abcdef123456" "$OUT" "$summary"
    [ "$status" -ne 0 ]

    # the pattern layer replaced the values with ***
    run grep -c '\*\*\*' "$summary"
    [ "$status" -eq 0 ]
    [ "$output" -ge 1 ]

    # the JSON stays parseable after redaction
    run jq -e . "${OUT}/resources.json"
    [ "$status" -eq 0 ]
}

@test "collect-resources: redaction does not corrupt non-secret values" {
    local summary="${TEST_TEMP_DIR}/step-summary.md"
    export GITHUB_STEP_SUMMARY="$summary"
    # a var whose name matches (?i)key but whose value is a boolean must not be scrubbed
    export FAKE_BUILD_KEY="true"

    mkdir -p "$OUT"
    run "$COLLECT" stop --out-dir "$OUT"
    [ "$status" -eq 0 ]

    run jq -e '.memoryBytes.peakIsLowerBound == true and .gradleCache.disabledDuringBuild == null' "${OUT}/resources.json"
    [ "$status" -eq 0 ] || {
        echo "$output"
        jq . "${OUT}/resources.json"
        return 1
    }
}

################################################################################
# selftest and docker-dependent sampling
################################################################################

@test "collect-resources: selftest observes synthetic short-lived processes" {
    run "$COLLECT" selftest --out-dir "${TEST_TEMP_DIR}/self"

    [ "$status" -eq 0 ]
    [[ "$output" == *"observed ticks="* ]]
    [[ "$output" == *"selftest: OK"* ]]

    # the numbers printed are the real observed ones
    run jq -s 'map(.rssSumBytes // 0) | max' "${TEST_TEMP_DIR}/self/samples.jsonl"
    [ "$status" -eq 0 ]
    [ "$output" -gt 0 ]
}

@test "collect-resources: container memory is sampled when docker is available" {
    if ! command -v docker >/dev/null 2>&1; then
        skip "docker CLI not available"
    fi
    if ! timeout 5 docker version --format '{{.Server.Version}}' >/dev/null 2>&1; then
        skip "docker daemon not reachable"
    fi

    run "$COLLECT" start --label docker-test --out-dir "$OUT" --interval 1
    [ "$status" -eq 0 ]

    sleep 5
    "$COLLECT" stop --label docker-test --out-dir "$OUT" 2>/dev/null

    # a single `docker stats --no-stream` call per tick fills containers[] and the sum
    run jq -e 'has("containers") and has("containerSumBytes") and has("containerCount")' "${OUT}/samples.jsonl"
    [ "$status" -eq 0 ]

    run jq -e '(.environment.dockerContainers | type == "number") and ((.memoryBytes.containerPeakSum | type == "number") or .memoryBytes.containerPeakSum == null)' "${OUT}/resources.json"
    [ "$status" -eq 0 ] || {
        echo "$output"
        jq . "${OUT}/resources.json"
        return 1
    }
}
