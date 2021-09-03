# APIML Images

All images include 2 Linux Distributions:

- Ubuntu
- Red Hat UBI

Each image supports both `amd64` and `s390x` CPU architectures.

## Gateway Service Image

Image `zowe-docker-release.jfrog.io/ompzowe/gateway-service:latest` should be able to run with minimal environment variables:

- `KEYSTORE`: path to keystore.
- `KEY_ALIAS`: certificate alias stored in keystore.
- `KEYSTORE_PASSWORD`: password of your keystore and truststore.
- `TRUSTSTORE`: path to truststore.
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
    -e WORKSPACE_DIR=/component \
    zowe-docker-release.jfrog.io/ompzowe/gateway-service:latest
```

## Discovery Service Image

Image `zowe-docker-release.jfrog.io/ompzowe/discovery-service:latest` should be able to run with minimal environment variables:

- `KEYSTORE`: path to keystore.
- `KEY_ALIAS`: certificate alias stored in keystore.
- `KEYSTORE_PASSWORD`: password of your keystore and truststore.
- `TRUSTSTORE`: path to truststore.
- `CMMN_LB`: set to `apiml-common-lib/bin/api-layer-lite-lib-all.jar`
- `WORKSPACE_DIR`: set to `/component`

Review the [Discovery package start script](../discovery-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/discovery-service:latest
# start container
docker run -it --rm -p 7553:7553 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e KEYSTORE=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e KEY_ALIAS=localhost \
    -e KEYSTORE_PASSWORD=password \
    -e TRUSTSTORE=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e CMMN_LB=apiml-common-lib/bin/api-layer-lite-lib-all.jar \
    -e WORKSPACE_DIR=/component \
    zowe-docker-release.jfrog.io/ompzowe/discovery-service:latest
```

## API Catalog Image

Image `zowe-docker-release.jfrog.io/ompzowe/api-catalog-services:latest` should be able to run with minimal environment variables:

- `KEYSTORE`: path to keystore.
- `KEY_ALIAS`: certificate alias stored in keystore.
- `KEYSTORE_PASSWORD`: password of your keystore and truststore.
- `TRUSTSTORE`: path to truststore.
- `CMMN_LB`: set to `apiml-common-lib/bin/api-layer-lite-lib-all.jar`
- `WORKSPACE_DIR`: set to `/component`
- `GATEAY_HOST`: the host of the API Gateway

Review the [API Catalog package start script](../api-catalog-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/api-catalog-services:latest
# start container
docker run -it --rm -p 7552:7552 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e KEYSTORE=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e KEY_ALIAS=localhost \
    -e KEYSTORE_PASSWORD=password \
    -e TRUSTSTORE=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e CMMN_LB=apiml-common-lib/bin/api-layer-lite-lib-all.jar \
    -e WORKSPACE_DIR=/component \
    -e GATEWAY_HOST=gateway.com \
    zowe-docker-release.jfrog.io/ompzowe/api-catalog-services:latest
```

## Caching Service Image

Image `zowe-docker-release.jfrog.io/ompzowe/caching-service:latest` should be able to run with minimal environment variables:

- `KEYSTORE`: path to keystore.
- `KEY_ALIAS`: certificate alias stored in keystore.
- `KEYSTORE_PASSWORD`: password of your keystore and truststore.
- `TRUSTSTORE`: path to truststore.
- `WORKSPACE_DIR`: set to `/component`

Review the [Caching service package start script](../caching-service-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/caching-service:latest
# start container
docker run -it --rm -p 7554:7554 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e KEYSTORE=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e KEY_ALIAS=localhost \
    -e KEYSTORE_PASSWORD=password \
    -e TRUSTSTORE=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e WORKSPACE_DIR=/component \
    zowe-docker-release.jfrog.io/ompzowe/caching-service:latest
```
