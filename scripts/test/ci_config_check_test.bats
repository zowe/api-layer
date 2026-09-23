#!/usr/bin/env bats

################################################################################
# BATS tests for scripts/ci/check-ci-config.py
#
# The checker encodes CI configuration invariants that were violated in this
# repository, so the tests below assert BOTH directions: the real tree passes,
# and each individual regression makes it fail.
#
# To run these tests:
#   bats scripts/test/ci_config_check_test.bats
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

setup() {
    BATS_TEST_DIRNAME="$(cd "$(dirname "$BATS_TEST_FILENAME")" && pwd)"
    PROJECT_ROOT="$(cd "${BATS_TEST_DIRNAME}/../.." && pwd)"
    CHECKER="${PROJECT_ROOT}/scripts/ci/check-ci-config.py"
    TEST_TEMP_DIR="$(mktemp -d)"
    FIXTURE_ROOT="${TEST_TEMP_DIR}/repo"
    mkdir -p "${FIXTURE_ROOT}/.github/workflows" "${FIXTURE_ROOT}/.github/actions/teardown"
    write_valid_fixture
}

teardown() {
    rm -rf "${TEST_TEMP_DIR}"
}

# A minimal tree that satisfies every invariant. Each test breaks exactly one thing.
write_valid_fixture() {
    cat > "${FIXTURE_ROOT}/.github/workflows/integration-tests.yml" << 'YAML'
name: integration tests
on:
    workflow_dispatch:
env:
    ORG_GRADLE_PROJECT_artifactory_user: ${{ secrets.ARTIFACTORY_USERNAME }}
    ORG_GRADLE_PROJECT_artifactory_password: ${{ secrets.ARTIFACTORY_PASSWORD }}
    GRADLE_REMOTE_CACHE_PUSH: "false"
jobs:
    CITests:
        runs-on: ubuntu-latest
        steps:
            -   uses: ./.github/actions/setup
                with:
                    setupNode: 'false'
            -   name: Run the startup check
                run: ./gradlew runStartUpCheck
YAML
    cat > "${FIXTURE_ROOT}/.github/workflows/service-registration.yml" << 'YAML'
name: service registration
on:
    workflow_dispatch:
env:
    ORG_GRADLE_PROJECT_artifactory_user: ${{ secrets.ARTIFACTORY_USERNAME }}
    ORG_GRADLE_PROJECT_artifactory_password: ${{ secrets.ARTIFACTORY_PASSWORD }}
jobs:
    Register:
        runs-on: ubuntu-latest
        steps:
            -   uses: ./.github/actions/setup
                with:
                    setupNode: 'true'
            -   name: Build
                run: ./gradlew clean build -x test
YAML
    cat > "${FIXTURE_ROOT}/.github/actions/teardown/action.yml" << 'YAML'
name: Tear down action
runs:
    using: "composite"
    steps:
        -   name: Cleanup Gradle Cache
            shell: bash
            run: |
                rm -f ~/.gradle/caches/modules-2/modules-2.lock
                rm -f ~/.gradle/caches/build-cache-1/build-cache-1.lock
YAML
}

@test "the checker passes on the real repository tree" {
    run python3 "${CHECKER}" --root "${PROJECT_ROOT}"
    [ "$status" -eq 0 ]
    [[ "$output" == *"All CI configuration invariants hold"* ]]
}

@test "the checker fails when a setup call site omits setupNode" {
    python3 - "${FIXTURE_ROOT}" << 'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]) / ".github/workflows/integration-tests.yml"
p.write_text(p.read_text().replace("                with:\n                    setupNode: 'false'\n", ""))
PY
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 1 ]
    [[ "$output" == *"without an explicit setupNode input"* ]]
}

@test "the checker fails when a workflow running Gradle has no build-cache credentials" {
    python3 - "${FIXTURE_ROOT}" << 'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]) / ".github/workflows/integration-tests.yml"
text = p.read_text()
text = text.replace("    ORG_GRADLE_PROJECT_artifactory_user: ${{ secrets.ARTIFACTORY_USERNAME }}\n", "")
text = text.replace("    ORG_GRADLE_PROJECT_artifactory_password: ${{ secrets.ARTIFACTORY_PASSWORD }}\n", "")
p.write_text(text)
PY
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 1 ]
    [[ "$output" == *"does not set ORG_GRADLE_PROJECT_artifactory_user"* ]]
}

@test "the checker fails when a job enables the remote cache push unconditionally" {
    python3 - "${FIXTURE_ROOT}" << 'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]) / ".github/workflows/integration-tests.yml"
text = p.read_text()
text = text.replace(
    "    CITests:\n        runs-on: ubuntu-latest\n",
    "    CITests:\n        runs-on: ubuntu-latest\n        env:\n            GRADLE_REMOTE_CACHE_PUSH: \"true\"\n",
)
p.write_text(text)
PY
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 1 ]
    [[ "$output" == *"must be gated on github.event_name"* ]]
}

@test "the checker fails when a job gates the push on the event name" {
    python3 - "${FIXTURE_ROOT}" << 'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]) / ".github/workflows/integration-tests.yml"
text = p.read_text()
text = text.replace(
    "    CITests:\n        runs-on: ubuntu-latest\n",
    "    CITests:\n        runs-on: ubuntu-latest\n        env:\n            GRADLE_REMOTE_CACHE_PUSH: ${{ github.event_name == 'push' }}\n",
)
p.write_text(text)
PY
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 0 ]
}

@test "the checker fails when teardown removes the task-output cache ungated" {
    cat > "${FIXTURE_ROOT}/.github/actions/teardown/action.yml" << 'YAML'
name: Tear down action
runs:
    using: "composite"
    steps:
        -   name: Cleanup Gradle Cache
            shell: bash
            run: |
                rm -rf ~/.gradle/caches/build-cache-1
YAML
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 1 ]
    [[ "$output" == *"without being gated on the cleanBuildCache input"* ]]
}

@test "the checker accepts a task-output removal gated on cleanBuildCache" {
    cat > "${FIXTURE_ROOT}/.github/actions/teardown/action.yml" << 'YAML'
name: Tear down action
runs:
    using: "composite"
    steps:
        -   name: Remove Gradle task-output cache
            if: inputs.cleanBuildCache == 'true'
            shell: bash
            run: |
                rm -rf ~/.gradle/caches/build-cache-1
YAML
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 0 ]
}

@test "the checker fails when a workflow invokes the disabled integration-tests:test task" {
    python3 - "${FIXTURE_ROOT}" << 'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]) / ".github/workflows/integration-tests.yml"
p.write_text(p.read_text().replace("./gradlew runStartUpCheck", "./gradlew :integration-tests:test"))
PY
    run python3 "${CHECKER}" --root "${FIXTURE_ROOT}"
    [ "$status" -eq 1 ]
    [[ "$output" == *"disabled :integration-tests:test task"* ]]
}

@test "the checker reports a fatal error when there are no workflows" {
    mkdir -p "${TEST_TEMP_DIR}/empty/.github/workflows"
    run python3 "${CHECKER}" --root "${TEST_TEMP_DIR}/empty"
    [ "$status" -eq 2 ]
    [[ "$output" == *"no workflows found"* ]]
}
