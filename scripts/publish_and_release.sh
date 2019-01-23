#!/usr/bin/env sh

set -ex

RELEASE_TYPE=$1
AUTH="-Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD"

case $RELEASE_TYPE in
   "SNAPSHOT_RELEASE")
   echo "Make SNAPSHOT release"
   ./gradlew publishAllVersions $AUTH
   ;;
   "PATCH_RELEASE")
   echo "Make PATCH release"
   ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=patch $AUTH
   ;;
   "MINOR_RELEASE")
   echo "Make MINOR release"
   ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=minor $AUTH
   ;;
   "MAJOR_RELEASE")
   echo "Make MAJOR release"
   ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=major $AUTH
   ;;
   "SPECIFIC_RELEASE")
   echo "Make specific release"
   ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=$RELEASE_VERSION -Prelease.newVersion=$NEW_VERSION $AUTH
esac

echo "End of publish and release"
