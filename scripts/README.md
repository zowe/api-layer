scripts
=======

The `scripts` directory contains useful scripts that can be used by developer or are used by the build on Jenkins.

`classify_changes.py` - Repository changes classifier
  - It used by Jenkins build to skip some stages in case of a specific changes 

`post_actions.py` - Changes the label in Zowe pull request

`publish_and_release.sh` - Release script for Gradle API Mediation Layer release
