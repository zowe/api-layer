#!/usr/bin/env python3
"""Check the CI configuration invariants that the resource-reduction work depends on.

Each check below encodes a defect that was actually observed in this repository, so
that it cannot come back unnoticed. Run it from the repository root:

    python3 scripts/ci/check-ci-config.py [--root DIR]

See docs/testing-resource-budgets.md for the reasoning behind each invariant.

SPDX-License-Identifier: EPL-2.0
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - the CI runner always has PyYAML
    print("FATAL: PyYAML is required (pip install pyyaml)", file=sys.stderr)
    sys.exit(2)

# Workflows whose Gradle invocations are covered by the build-cache credential
# invariant. Extend this list deliberately, one workflow at a time.
CREDENTIAL_COVERED_WORKFLOWS = [
    "integration-tests.yml",
    "service-registration.yml",
]

# The remote build cache is only usable with credentials. Without them Gradle answers
# 403 and then disables the cache for the whole build, so every cacheable task becomes
# a miss. These are the env names that make credentials available to Gradle.
CACHE_CREDENTIAL_ENV = (
    "ORG_GRADLE_PROJECT_artifactory_user",
    "ORG_GRADLE_PROJECT_artifactory_password",
)

SETUP_ACTION = "./.github/actions/setup"
PUSH_ENV = "GRADLE_REMOTE_CACHE_PUSH"

failures: list[str] = []
notes: list[str] = []


def fail(check: str, message: str) -> None:
    failures.append(f"{check}: {message}")


def load_workflows(root: Path) -> dict[str, dict]:
    """Load every workflow, tolerating the YAML 1.1 `on` key quirk."""
    workflows = {}
    for path in sorted((root / ".github" / "workflows").glob("*.yml")):
        with path.open() as handle:
            doc = yaml.safe_load(handle)
        if isinstance(doc, dict):
            workflows[path.name] = doc
    return workflows


def check_setup_inputs(workflows: dict[str, dict]) -> None:
    """Every setup call site must state setupNode explicitly.

    The shared setup action installs Node, installs npm globally and restores six
    frontend caches. A Java-only job that inherits the default pays for all of it.
    """
    check = "setupNode-explicit"
    found = 0
    bad = 0
    for name in CREDENTIAL_COVERED_WORKFLOWS:
        doc = workflows.get(name)
        if doc is None:
            fail(check, f"{name} not found")
            bad += 1
            continue
        for job_name, job in (doc.get("jobs") or {}).items():
            for step in job.get("steps") or []:
                if str(step.get("uses", "")).endswith(SETUP_ACTION):
                    found += 1
                    if "setupNode" not in (step.get("with") or {}):
                        fail(check, f"{name}:{job_name} calls {SETUP_ACTION} without an explicit setupNode input")
                        bad += 1
                    elif str(step["with"]["setupNode"]) not in ("true", "false"):
                        fail(check, f"{name}:{job_name} has setupNode={step['with']['setupNode']!r}, expected 'true' or 'false'")
                        bad += 1
    if found == 0:
        fail(check, "no setup action call sites found - the check would pass vacuously")
    elif bad == 0:
        notes.append(f"{check}: {found} setup call sites all declare setupNode")


def check_cache_credentials(workflows: dict[str, dict]) -> None:
    """Covered workflows must make build-cache credentials available to every job."""
    check = "build-cache-credentials"
    for name in CREDENTIAL_COVERED_WORKFLOWS:
        doc = workflows.get(name)
        if doc is None:
            fail(check, f"{name} not found")
            continue
        if "gradlew" not in (root_text(name)):
            notes.append(f"{check}: {name} has no Gradle invocation, skipped")
            continue
        env = doc.get("env") or {}
        missing = [key for key in CACHE_CREDENTIAL_ENV if key not in env]
        if missing:
            fail(
                check,
                f"{name} runs Gradle but its workflow-level env does not set "
                f"{', '.join(missing)} - the remote cache would answer 403 and be disabled "
                f"for the whole build",
            )
        else:
            notes.append(f"{check}: {name} exposes build-cache credentials to every job")


def check_push_policy(workflows: dict[str, dict]) -> None:
    """No workflow may write to the remote build cache on a pull_request event."""
    check = "remote-cache-push-policy"
    literal_true = re.compile(rf"^{PUSH_ENV}:\s*[\"']?(true|1|yes)[\"']?\s*$", re.IGNORECASE)
    for name, doc in workflows.items():
        env = doc.get("env") or {}
        value = str(env.get(PUSH_ENV, "false"))
        if literal_true.match(f"{PUSH_ENV}: {value}"):
            fail(check, f"{name} enables {PUSH_ENV} unconditionally at workflow level")
        for job_name, job in (doc.get("jobs") or {}).items():
            job_env = job.get("env") or {}
            if PUSH_ENV in job_env:
                job_value = str(job_env[PUSH_ENV])
                if not re.search(r"github\.event_name", job_value) and job_value.lower() != "false":
                    fail(
                        check,
                        f"{name}:{job_name} sets {PUSH_ENV}={job_value!r} - it must be gated on "
                        f"github.event_name so pull request code never writes the shared cache",
                    )
    # A literal push=true in any workflow bypasses the event gate entirely.
    literal_push = False
    for name in workflows:
        for line in root_text(name).splitlines():
            if re.search(r"gradle\.cache\.push\s*=\s*true", line, re.IGNORECASE):
                literal_push = True
                fail(check, f"{name} sets gradle.cache.push=true on a command line ({line.strip()[:80]})")
    if not literal_push:
        notes.append(f"{check}: push is event-gated everywhere, never unconditional")


def check_teardown_keeps_task_outputs(root: Path) -> None:
    """The teardown action must not wipe the content-addressed task-output cache.

    Deleting ~/.gradle/caches/build-cache-1 unconditionally made the local task-output
    cache useless: it was saved empty on every run, so eligible compile tasks never came
    back FROM-CACHE. A deliberate, opt-in removal gated on the cleanBuildCache input is
    fine; an ungated one is not.
    """
    check = "teardown-keeps-task-outputs"
    path = root / ".github" / "actions" / "teardown" / "action.yml"
    if not path.exists():
        fail(check, f"{path} not found")
        return
    with path.open() as handle:
        doc = yaml.safe_load(handle)
    ungated = []
    for step in ((doc.get("runs") or {}).get("steps") or []):
        script = str(step.get("run") or "")
        for line in script.splitlines():
            stripped = line.strip()
            if stripped.startswith("#"):
                continue
            if re.search(r"rm\s+-rf?\s+.*build-cache-1(?![\w./-])", stripped):
                condition = str(step.get("if") or "")
                if "cleanBuildCache" not in condition:
                    ungated.append(stripped)
    if ungated:
        fail(
            check,
            "teardown deletes the whole build-cache-1 directory without being gated on the "
            f"cleanBuildCache input ({'; '.join(ungated)}); only its lock and gc files may be "
            "removed, otherwise the task-output cache is saved empty",
        )
    else:
        notes.append(f"{check}: build-cache-1 entries are preserved, only lock/gc files are removed")


def check_disabled_test_task(workflows: dict[str, dict]) -> None:
    """No workflow may use the disabled :integration-tests:test task as a success proxy."""
    check = "no-disabled-test-task"
    for name in workflows:
        for line in root_text(name).splitlines():
            if ":integration-tests:test" in line and not line.strip().startswith("#"):
                fail(
                    check,
                    f"{name} invokes the disabled :integration-tests:test task "
                    f"(integration-tests/build.gradle sets test.enabled = false)",
                )
    notes.append(f"{check}: no workflow relies on the disabled integration-tests:test task")


TEXT_CACHE: dict[str, str] = {}


def root_text(name: str) -> str:
    return TEXT_CACHE[name]


def advisory_gradle_credentials(workflows: dict[str, dict]) -> None:
    """Report (without failing) other workflows that run Gradle without credentials.

    The settings.gradle change means a missing credential no longer disables the remote
    cache mid-build, so these workflows are not broken - they simply cannot read the
    shared cache. They are listed so the remaining work stays visible instead of being
    forgotten.
    """
    uncovered = []
    for name, doc in workflows.items():
        if name in CREDENTIAL_COVERED_WORKFLOWS:
            continue
        if "gradlew" not in root_text(name):
            continue
        env = doc.get("env") or {}
        if any(key not in env for key in CACHE_CREDENTIAL_ENV):
            uncovered.append(name)
    if uncovered:
        notes.append(
            "advisory: these workflows run Gradle without build-cache credentials and "
            f"cannot read the shared cache: {', '.join(sorted(uncovered))}"
        )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", help="repository root (default: current directory)")
    args = parser.parse_args()
    global root
    root = Path(args.root).resolve()

    workflows = load_workflows(root)
    if not workflows:
        print(f"FATAL: no workflows found under {root}/.github/workflows", file=sys.stderr)
        return 2
    for name in workflows:
        TEXT_CACHE[name] = (root / ".github" / "workflows" / name).read_text()

    check_setup_inputs(workflows)
    check_cache_credentials(workflows)
    check_push_policy(workflows)
    check_teardown_keeps_task_outputs(root)
    check_disabled_test_task(workflows)
    advisory_gradle_credentials(workflows)

    for note in notes:
        print(f"  ok   {note}")
    for failure in failures:
        print(f"  FAIL {failure}")
    if failures:
        print(f"\n{len(failures)} CI configuration invariant(s) violated")
        return 1
    print("\nAll CI configuration invariants hold")
    return 0


if __name__ == "__main__":
    sys.exit(main())
