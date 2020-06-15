# Releasing API Mediation Layer

## Guidelines

- Master build creates a new [_snapshot_](https://stackoverflow.com/questions/5901378/what-exactly-is-a-maven-snapshot-and-why-do-we-need-it) that is deployed to [libs-snapshot repository in Zowe Artifactory](https://zowe.jfrog.io/zowe/libs-snapshot/org/zowe/apiml/sdk/ ) automatically.
- When the contents of the master are stable and contains new functionality or bugfixes, a new release is created by starting a Jenkins job [api-layer-release](https://wash.zowe.org:8443/job/api-layer-release/build?delay=0sec).
- Artifacts are deployed to [lib-release repository](https://zowe.jfrog.io/zowe/libs-release/org/zowe/apiml/sdk/).
- The versioning follows [semantic versioning](https://semver.org/):
  - Patch versions are created only when fixes are made. Patch versions are not created when chages are made to external APIs and user functionality.
    - [zowe-install-packaging/manifest.json.template](https://github.com/zowe/zowe-install-packaging/blob/master/manifest.json.template) includes the latest patch version.
  - Minor versions versions are updated when new functionality is delivered. Typically, this occurs after each sprint when existing functionality remains the same.
    - [zowe-install-packaging/manifest.json.template](https://github.com/zowe/zowe-install-packaging/blob/master/manifest.json.template) needs to be updated to select a new minor or major version.
  - The major version number increases when a backward incompatible change is introduced.
  - The API ML is a part of the Zowe PAX file that is packaged by builds of the [zowe-install-packaging](https://github.com/zowe/zowe-install-packaging/) repository.
  - [Zowe Release Process](https://github.com/zowe/zlc/blob/master/process/release.md)

## Jenkins release job

https://wash.zowe.org:8443/job/api-layer-release/build?delay=0sec

## Commands

The commands are listed for reference. Use the Jenkins job to execute them.

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

Properties are stored in Jenkins Credentials: https://wash.zowe.org:8443/credentials/store/system/domain/_/credential/zowe/
