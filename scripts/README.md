scripts
=======

The `scripts` directory contains the following scripts used during the build process:

- **`classify_changes.py`** - Repository changes classifier

    This script is used by the Jenkins build to skip some stages when there are specific changes. 

- **`post_actions.py`** - Changes the label in Zowe pull request

    After each internal pipeline runs, this script executes and updates labels in GitHub to reflect the outcome of integration testing.

- **`publish_and_release.sh`** - Release script for Gradle API Mediation Layer release task

    This script is run by Jenkins `api-layer-release` job to start the Gradle release task.
