#!/usr/bin/env sh

set -ex

./gradle/bootstrap/bootstrap_gradlew.sh
AUTH="-Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD"

case $RELEASE_TYPE in
   "SNAPSHOT_RELEASE")
   echo "Make SNAPSHOT release"
   ./gradlew publishAllVersions $AUTH
   git archive --format tar.gz -9 --output api-layer.tar.gz HEAD~1
   ;;
   "PATCH_RELEASE")
   echo "Make PATCH release"
   ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=patch $AUTH
   git archive --format tar.gz -9 --output api-layer.tar.gz HEAD~1
   ;;
   "MINOR_RELEASE")
   echo "Make MINOR release"
   ./gradlew --continue release -Prelease.useAutomaticVersion=true -Prelease.scope=minor $AUTH
  ## ./gradlew --continue publishEnabler '-Penabler=v1' $AUTH
   git archive --format tar.gz -9 --output api-layer.tar.gz HEAD~1
   ;;
   "MAJOR_RELEASE")
   echo "Make MAJOR release"
   ./gradlew release -Prelease.useAutomaticVersion=true -Prelease.scope=major $AUTH
   git archive --format tar.gz -9 --output api-layer.tar.gz HEAD~1
   ;;
   "SPECIFIC_RELEASE")
   echo "Make specific release"
   ./gradlew --continue release -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=$RELEASE_VERSION -Prelease.newVersion=$NEW_VERSION $AUTH
    echo 'Published minor'
   git archive --format tar.gz -9 --output api-layer.tar.gz "v$RELEASE_VERSION"
    echo 'DADA Published minor'
esac

echo "End of publish and release"
