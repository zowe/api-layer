# Releasing the Onboarding Enablers

The Node.js and Python onboarding enablers are released **independently of the API ML core
and of the Zowe release train**. They are no longer part of the Zowe release artifacts, so
without independent releases the published npm/PyPI packages go stale.

Use the [Automated publish of nodejs and python enabler](../actions/workflows/automated-release-nodejs.yml)
workflow for both. One enabler per run (`enablerType` = `node` or `python`).

## Versioning

- Both enablers track a `3.2.x` line that is **decoupled from the API ML `3.5.x` line**.
  The npm registry is the source of truth for the next Node.js version, PyPI for the next
  Python version. Check the published `latest` before dispatching:
  - `npm view @zowe/apiml-onboarding-enabler-nodejs version`
  - `pip index versions zowe-apiml-onboarding-enabler-python` (or the PyPI project page)
- **Always pass an explicit `version`**. For Node.js this avoids `npm version` computing a
  bump from `package.json` when `package.json` is behind the registry; for Python this
  overrides `setuptools_scm` tag-based version derivation, which would otherwise compute a
  version from the *Zowe* tag space (`v3.5.x` tags) instead of the enabler line.
- The version bump must not create git tags in the `vX.Y.Z` space: the composite action
  passes `--no-git-tag-version` to `npm version`, and the Python release uses the
  octorelease git plugin only to push the version-bump commit.

## Validating a release without side effects

Set `dryRun=true` to exercise the release mechanics end to end with **no** registry upload
and **no** push to the repository:

- Node.js: `npm publish --dry-run` (builds the tarball, checks auth resolution, prints the
  package contents) and the version-bump commit is kept local.
- Python: the wheel/sdist is still built and uploaded as the `wheels` run artifact, but
  octorelease runs with `dry-run` and pushes nothing.

A dry run is the mandatory first step after any change to these release paths, and the
mandatory regression check before a real release if the paths have been idle.

## Sequencing relative to the API ML patch train

The patch train (`automated-release.yml`, Fridays) reads `v3.x.x` HEAD, and a real enabler
release pushes a `[skip ci] Update version` commit to `v3.x.x`. To keep an untested commit
out of the next patch tag:

1. **Do not dispatch a real enabler release between patch freeze and GA.** A `dryRun=true`
   dispatch is safe at any time.
2. Release order: patch train first, enabler release after the patch is cut.
3. After a real enabler release lands its `[skip ci]` commit on `v3.x.x`, manually dispatch
   the CI workflow on `v3.x.x` so the commit is not untested when the next patch train picks
   it up.

All release workflows share the `release-train` concurrency group, so an enabler dispatch
cannot interleave with a running patch train or binary release; it queues instead.

## Checklist for a real release

1. `dryRun=true` dispatch green (on `v3.x.x`), tarball/wheel contents reviewed.
2. Patch train not in flight (check [running workflows](../actions)).
3. Dispatch with explicit `version` (Node.js: latest registry + patch; Python: latest PyPI + patch).
4. After the run: verify the npm dist-tag / PyPI files, and the `[skip ci]` commit on `v3.x.x`.
5. Consume-test the released version in the matching sample app
   (`onboarding-enabler-nodejs-sample-app` / `onboarding-enabler-python-sample-app`).
