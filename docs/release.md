# Releasing API Mediation Layer

## Guidelines

- Master build creates a new [_snapshot_](https://stackoverflow.com/questions/5901378/what-exactly-is-a-maven-snapshot-and-why-do-we-need-it) that is deployed to the [libs-snapshot repository in Zowe Artifactory](https://zowe.jfrog.io/zowe/libs-snapshot/org/zowe/apiml/sdk/ ) automatically.
- Patch releases are made weekly every Friday. A minor release is made when a new Zowe version goes GA.
- Artifacts are deployed to the [lib-release repository](https://zowe.jfrog.io/zowe/libs-release/org/zowe/apiml/sdk/).
- The API ML follows [semantic versioning](https://semver.org/).
- The API ML is a part of the Zowe PAX file that is packaged by builds of the [zowe-install-packaging](https://github.com/zowe/zowe-install-packaging/) repository.
- When a new release is available for a release candidate, the [zowe-install-packaging/manifest.json.template file on the RC branch](https://github.com/zowe/zowe-install-packaging/blob/rc/manifest.json.template) needs to be updated so the `org.zowe.apiml.sdk.*` components have the proper version.
  - [Zowe Release Process](https://github.com/zowe/community/blob/master/Technical-Steering-Committee/release.md)
- If you need to publish a PR version of the API ML, use the [PR snapshot workflow](https://github.com/zowe/api-layer/actions/workflows/pull-request-snapshot-release.yml) and use the current snapshot version as new version and the currently released version with an added PR as the current version {VERSION}-PR-{PR_NUMBER} and run from the branch for the PR you want to release. Example:
  - Pull request: PR-123
  - Branch: apiml/GH123/my_branch
- If you need to publish images of the API ML for testing purposes, use the [create image workflow](https://github.com/zowe/api-layer/actions/workflows/create-image-without-release.yml)
  - Service: all
  - Branch: my-branch-for-testing

## Releasing API ML for a Zowe Release

When there is a new minor release of Zowe, there should be a new patch release of the API ML from the `master` branch and a new minor snapshot version created.
This is done in two steps:
 
1. Release the binaries with the [binary specific release workflow](https://github.com/zowe/api-layer/actions/workflows/binary-specific-release.yml).
    * `release_version` is the version that will be released. This should be a new patch version. For example, if `master` is currently on version 1.27.1, `release_version` would be 1.27.2.
    * `new_version` should be a `SNAPSHOT` version with a new minor version. For example, if `master` is currently on version 1.27.1, `new_version` would be 1.28.0-SNAPSHOT.
      If 1.28.0-SNAPSHOT already exists due to previous releases, the patch version should be incremented, in this example to 1.28.1-SNAPSHOT.

2. Release the images with the [image specific release workflow](https://github.com/zowe/api-layer/actions/workflows/image-specific-release.yml).
    * `release_version` is the version that will be released. This should be the same value as used in step `i`.

After this release is finished the new version must be added to the [release candidate manifest](https://github.com/zowe/zowe-install-packaging/blob/rc/manifest.json.template).

The following sections of the manifest need to have their version tag updated to the newly released version of the API ML (the value used in `release_version`):
* `binaryDependencies.org.zowe.apiml.sdk.*` components
* API ML components under `imageDependencies`:
  * `api-catalog`
  * `caching`
  * `discovery`
  * `gateway`
* `api-layer` repository under `sourceDependencies`
  
## GitHub actions

For some releases there are two workflows, one to release binaries and one to release images. This is because Marist is used to build the images and is unstable,
causing failures that shouldn't impact the binary release.

- **Snapshot release** - runs on push to `v3.x.x` or `v2.x.x` to create a snapshot
  - https://github.com/zowe/api-layer/actions/workflows/binary-snapshot-release.yml
  - https://github.com/zowe/api-layer/actions/workflows/image-snapshot-release.yml
- **Automated release** (patch, minor, major) - runs every Friday to create a patch release, or manually to create a patch, minor, or major release
  - https://github.com/zowe/api-layer/actions/workflows/automated-release.yml
- **Specific release** - manually run to create a new release from the target branch the workflow is run from 
  - https://github.com/zowe/api-layer/actions/workflows/binary-specific-release.yml
  - https://github.com/zowe/api-layer/actions/workflows/image-specific-release.yml

There are also workflows that publish test builds.

- **PR Snapshot publish** - publish a new binary version from a pull request
  - https://github.com/zowe/api-layer/actions/workflows/pull-request-snapshot-release.yml
- **Publish image** - publish new images from a target branch
    - https://github.com/zowe/api-layer/actions/workflows/create-image-without-release.yml

## Commands

The commands below are listed as a reference. Use GitHub Actions to execute them. The following commands are relevant only for Java artifacts. 

### Release SNAPSHOT artifacts

```shell
./gradlew publishAllVersions
```

### Release final artifacts

```shell
./gradlew release -Prelease.useAutomaticVersion=true # new patch
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=patch # new patch
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=minor # new minor
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=major # new major
```

### Release artifacts with a custom version

```shell
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=0.0.0 -Prelease.newVersion=1.1.0-SNAPSHOT
```

## Properties

- `zowe.deploy.username` and `zowe.deploy.password` - credentials to [Zowe Artifactory](https://zowe.jfrog.io/)

You can set properties in two ways:

- on the command-line: `-Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD`
- in `~/.gradle/gradle.properties`

**Warning!** Do not commit property changes to the Git repository. This is confidential information.

Properties are stored in GitHub Secrets.

## Update changelog

You can get commit messages with the tool:

`npm install -g conventional-changelog-cli`

Command to display commit messages for a specified number of releases:

`conventional-changelog -r <number_of_releases>`

More information about the tool:

`https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-cli`
