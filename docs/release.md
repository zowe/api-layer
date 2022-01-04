# Releasing API Mediation Layer

## Guidelines

- Master build creates a new [_snapshot_](https://stackoverflow.com/questions/5901378/what-exactly-is-a-maven-snapshot-and-why-do-we-need-it) that is deployed to the [libs-snapshot repository in Zowe Artifactory](https://zowe.jfrog.io/zowe/libs-snapshot/org/zowe/apiml/sdk/ ) automatically.
- Patch releases are made weekly every Friday. A minor release is made when a new Zowe version goes GA.
- Artifacts are deployed to the [lib-release repository](https://zowe.jfrog.io/zowe/libs-release/org/zowe/apiml/sdk/).
- The API ML follows [semantic versioning](https://semver.org/).
- The API ML is a part of the Zowe PAX file that is packaged by builds of the [zowe-install-packaging](https://github.com/zowe/zowe-install-packaging/) repository.
- When a new release is available for a release candidate, the [zowe-install-packaging/manifest.json.template file on the RC branch](https://github.com/zowe/zowe-install-packaging/blob/rc/manifest.json.template) needs to be updated so the `org.zowe.apiml.sdk.*` components have the proper version.
  - [Zowe Release Process](https://github.com/zowe/community/blob/master/Technical-Steering-Committee/release.md)
- If you need to publish a PR version of the API ML, use the [specific release action](https://github.com/zowe/api-layer/actions/workflows/specific-release.yml) and use the current snapshot version as new version and the currently released version with an added PR as the current version {VERSION}-PR-{PR_NUMBER} and run from the branch for the PR you want to release. Example:
  - Current Version: 1.22.1-PR-1475
  - New Version: 1.22.2-SNAPSHOT
  - Branch: apiml/GH1456/specific_release
  
## GitHub actions

- Snapshot release - https://github.com/zowe/api-layer/actions/workflows/snapshot-release.yml
- PR Snapshot release - https://github.com/zowe/api-layer/actions/workflows/pull-request-snapshot-release.yml
- Automated release (patch, minor, major) - https://github.com/zowe/api-layer/actions/workflows/release.yml
- Specific release - https://github.com/zowe/api-layer/actions/workflows/specific-release.yml

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

### Release artifacts with custom version

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

Command to display commit messages for specified number of releases:

`conventional-changelog -r <number_of_releases>`

More information about the tool:

`https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-cli`
