# Releasing API Mediation Layer

## Properties

- `zowe.deploy.username` and `zowe.deploy.password` - credentials to [Zowe Artifactory](https://gizaartifactory.jfrog.io/)

You can set them:

- on command-line: `-Pzowe.deploy.username=$USERNAME... -Pzowe.deploy.password=$PASSWORD`
- in `~/.gradle/gradle.properties`

**Warning!** Do not commit them to the Git repository. They are secret

They are stored in Jenkins Credentials: https://wash.zowe.org:8443/credentials/store/system/domain/_/credential/GizaArtifactory/


## Release SNAPSHOT artifacts

```shell
./gradlew publishAllVersions
```

## Release final artifacts

```shell
./gradlew release -Prelease.useAutomaticVersion=true # new patch
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=patch # new patch
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=minor # new minor
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=major # new major
```

## Release artifacts with custom version

```shell
./gradlew release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=0.0.0 -Prelease.newVersion=1.1.0-SNAPSHOT
```

## Jenkins release job

http://plape03-u114063:8080/job/MFaaS/job/MFaaS-publish-and-release/
