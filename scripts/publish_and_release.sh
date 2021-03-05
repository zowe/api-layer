#!/usr/bin/env sh

set -ex

./gradle/bootstrap/bootstrap_gradlew.sh
AUTH="-Pzowe.deploy.username=$USERNAME -Pzowe.deploy.password=$PASSWORD -Partifactory_user=$USERNAME -Partifactory_password=$PASSWORD"
DIST_REGISTRY="https://registry.npmjs.org/"

case $RELEASE_TYPE in
   "SPECIFIC_RELEASE")
   echo "Make specific release"
   cd onboarding-enabler-nodejs
   echo "//registry.npmjs.org/:_authToken=$TOKEN" > ~/.npmrc
   echo "registry=$DIST_REGISTRY" >> ~/.npmrc
   #TODO broken with gradle release task, it fails on uncommitted changes in working dir
   #/npm version $RELEASE_VERSION
   #npm publish --access public
   cd ..
   ./gradlew release -x test -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=$RELEASE_VERSION -Prelease.newVersion=$NEW_VERSION $AUTH
   git archive --format tar.gz -9 --output api-layer.tar.gz "v$RELEASE_VERSION"
esac

echo "End of publish and release"
