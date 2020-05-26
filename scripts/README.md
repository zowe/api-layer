scripts
=======

The `scripts` directory contains scripts used during build process.

`classify_changes.py` - Repository changes classifier

  - It used by the Jenkins build to skip some stages in case of a specific changes 

`post_actions.py` - Changes the label in Zowe pull request

  - After each internal pipeline run this script executes and updates labels in GitHub to reflect the outcome of integration testing.

`publish_and_release.sh` - Release script for Gradle API Mediation Layer release task

  - This script is run by Jenkins `api-layer-release` job to kick off the Gradle release task
