# Gateway Image

## General Information

This image can be used to start the Gateway Service.

It includes 2 Linux Distributions:

- Ubuntu
- Red Hat UBI

Each image supports both `amd64` and `s390x` CPU architectures.

## Usage

Image `zowe-docker-release.jfrog.io/ompzowe/gateway-service:latest` should be able to run with minimal environment variables:

- `KEYSTORE`: path to keystore.
- `KEY_ALIAS`: certificate alias stored in keystore.
- `KEYSTORE_PASSWORD`: password of your keystore and truststore.
- `TRUSTSTORE`: path to truststore.
- `LAUNCH_COMPONENT`: set to `bin`.
- `CMMN_LB`: set to `apiml-common-lib/bin/api-layer-lite-lib-all.jar`
- `WORKSPACE_DIR`: set to `/component`

Review the [Gateway package start script](../gateway-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/gateway-service:latest
# start container
docker run -it --rm -p 7554:7554 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e KEYSTORE=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e KEY_ALIAS=localhost \
    -e KEYSTORE_PASSWORD=password \
    -e TRUSTSTORE=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e CMMN_LB=apiml-common-lib/bin/api-layer-lite-lib-all.jar \
    -e LAUNCH_COMPONENT=bin \
    -e WORKSPACE_DIR=/component \
    zowe-docker-release.jfrog.io/ompzowe/gateway-service:latest
```
