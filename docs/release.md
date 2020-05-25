# Releasing API Mediation Layer

## Guidelines

- Master build creates a new [_snapshot_](https://stackoverflow.com/questions/5901378/what-exactly-is-a-maven-snapshot-and-why-do-we-need-it) that is deployed to [libs-snapshot repository in Zowe Artifactory](https://zowe.jfrog.io/zowe/libs-snapshot/org/zowe/apiml/sdk/ ) automatically
- New release is done starting Jenkins job [api-layer-release](https://wash.zowe.org:8443/job/api-layer-release/build?delay=0sec) when the contents of the master is stable and contains new functionality or bugfixes
- Artifacts are deployed to [lib-release repository](https://zowe.jfrog.io/zowe/libs-release/org/zowe/apiml/sdk/)
- The versioning follows [semantic versioning](https://semver.org/):
  - Patch versions are done when no external APIs and user functionality is changed and only fixes are done
    - [zowe-install-packaging/manifest.json.template](https://github.com/zowe/zowe-install-packaging/blob/master/manifest.json.template) includes the latest patch version
  - Minor versions versions are updated when new functionality is delivered, usually after each sprint but the existing functionality is not changed
    - [zowe-install-packaging/manifest.json.template](https://github.com/zowe/zowe-install-packaging/blob/master/manifest.json.template) needs to be updated to select new minor or major version
  - Major version is increased when a backward incompatible change is introduced
  - The APIML is a part of the Zowe PAX file that is packaged by builds of the [zowe-install-packaging](https://github.com/zowe/zowe-install-packaging/) repository
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

You can set them:

- on command-line: `-Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD`
- in `~/.gradle/gradle.properties`

**Warning!** Do not commit them to the Git repository. They are secret

They are stored in Jenkins Credentials: https://wash.zowe.org:8443/credentials/store/system/domain/_/credential/zowe/
