# Release new version

With each release of the Zowe new version of this container needs to be published. The prerequisity to release is a full Docker instalation on the machine running the comands.

1) Based on the OS open the PowerShell or Bash 
2) Change working directory to api-layer/docker/development
3) docker build --no-cache -t jbalhar/api-layer-development:{versionOfZowe} .
4) docker push jbalhar/api-layer-development:{versionOfZowe}
