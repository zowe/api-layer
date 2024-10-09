# APIML Images

All images include 2 Linux Distributions:

- Ubuntu
- Red Hat UBI

Each image supports both `amd64` and `s390x` CPU architectures.

## Gateway Service Image

Image `zowe-docker-release.jfrog.io/ompzowe/gateway-service:3` should be able to run with minimal environment variables:

- `ZWE_configs_certificate_keystore_file`: path to keystore.
- `ZWE_configs_certificate_keystore_alias`: certificate alias stored in keystore.
- `ZWE_configs_certificate_keystore_password`: password of your keystore.
- `ZWE_configs_certificate_truststore_file`: path to truststore.
- `ZWE_configs_certificate_truststore_password`: password of your truststore.
- `CMMN_LB`: set to `apiml-common-lib/bin/api-layer-lite-lib-all.jar`
- `ZWE_zowe_workspaceDirectory`: set to `/component`

Review the [Gateway package start script](../gateway-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/gateway-service:3
# start container
docker run -it --rm -p 7554:7554 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e ZWE_configs_certificate_keystore_file=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e ZWE_configs_certificate_keystore_alias=localhost \
    -e ZWE_configs_certificate_keystore_password=password \
    -e ZWE_configs_certificate_truststore_file=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e ZWE_configs_certificate_truststore_password=password \
    -e CMMN_LB=apiml-common-lib/bin/api-layer-lite-lib-all.jar \
    -e ZWE_zowe_workspaceDirectory=/component \
    zowe-docker-release.jfrog.io/ompzowe/gateway-service:3
```

## Discovery Service Image

Image `zowe-docker-release.jfrog.io/ompzowe/discovery-service:3` should be able to run with minimal environment variables:

- `ZWE_configs_certificate_keystore_file`: path to keystore.
- `ZWE_configs_certificate_keystore_alias`: certificate alias stored in keystore.
- `ZWE_configs_certificate_keystore_password`: password of your keystore.
- `ZWE_configs_certificate_truststore_file`: path to truststore.
- `ZWE_configs_certificate_truststore_password`: password of your truststore.
- `CMMN_LB`: set to `apiml-common-lib/bin/api-layer-lite-lib-all.jar`
- `ZWE_zowe_workspaceDirectory`: set to `/component`

Review the [Discovery package start script](../discovery-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/discovery-service:3
# start container
docker run -it --rm -p 7553:7553 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e ZWE_configs_certificate_keystore_file=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e ZWE_configs_certificate_keystore_alias=localhost \
    -e ZWE_configs_certificate_keystore_password=password \
    -e ZWE_configs_certificate_truststore_file=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e ZWE_configs_certificate_truststore_password=password \
    -e CMMN_LB=apiml-common-lib/bin/api-layer-lite-lib-all.jar \
    -e ZWE_zowe_workspaceDirectory=/component \
    zowe-docker-release.jfrog.io/ompzowe/discovery-service:3
```

## API Catalog Image

Image `zowe-docker-release.jfrog.io/ompzowe/api-catalog-services:3` should be able to run with minimal environment variables:

- `ZWE_configs_certificate_keystore_file`: path to keystore.
- `ZWE_configs_certificate_keystore_alias`: certificate alias stored in keystore.
- `ZWE_configs_certificate_keystore_password`: password of your keystore.
- `ZWE_configs_certificate_truststore_file`: path to truststore.
- `ZWE_configs_certificate_truststore_password`: password of your truststore.
- `CMMN_LB`: set to `apiml-common-lib/bin/api-layer-lite-lib-all.jar`
- `ZWE_zowe_workspaceDirectory`: set to `/component`
- `ZWE_GATEWAY_HOST`: the host of the API Gateway

Review the [API Catalog package start script](../api-catalog-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/api-catalog-services:3
# start container
docker run -it --rm -p 7552:7552 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e ZWE_configs_certificate_keystore_file=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e ZWE_configs_certificate_keystore_alias=localhost \
    -e ZWE_configs_certificate_keystore_password=password \
    -e ZWE_configs_certificate_truststore_file=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e ZWE_configs_certificate_truststore_password=password \
    -e CMMN_LB=apiml-common-lib/bin/api-layer-lite-lib-all.jar \
    -e ZWE_zowe_workspaceDirectory=/component \
    -e ZWE_GATEWAY_HOST=gateway.com \
    zowe-docker-release.jfrog.io/ompzowe/api-catalog-services:3
```

## Caching Service Image

Image `zowe-docker-release.jfrog.io/ompzowe/caching-service:3` should be able to run with minimal environment variables:

- `ZWE_configs_certificate_keystore_file`: path to keystore.
- `ZWE_configs_certificate_keystore_alias`: certificate alias stored in keystore.
- `ZWE_configs_certificate_keystore_password`: password of your keystore.
- `ZWE_configs_certificate_truststore_file`: path to truststore.
- `ZWE_configs_certificate_truststore_password`: password of your truststore.
- `ZWE_zowe_workspaceDirectory`: set to `/component`

Review the [Caching service package start script](../caching-service-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/caching-service:3
# start container
docker run -it --rm -p 7555:7555 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e ZWE_configs_certificate_keystore_file=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e ZWE_configs_certificate_keystore_alias=localhost \
    -e ZWE_configs_certificate_keystore_password=password \
    -e ZWE_configs_certificate_truststore_file=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e ZWE_configs_certificate_truststore_password=password \
    -e ZWE_zowe_workspaceDirectory=/component \
    zowe-docker-release.jfrog.io/ompzowe/caching-service:3
```

## ZAAS Image

Image `zowe-docker-release.jfrog.io/ompzowe/zaas:3` should be able to run with minimal environment variables:

- `ZWE_configs_certificate_keystore_file`: path to keystore.
- `ZWE_configs_certificate_keystore_alias`: certificate alias stored in keystore.
- `ZWE_configs_certificate_keystore_password`: password of your keystore.
- `ZWE_configs_certificate_truststore_file`: path to truststore.
- `ZWE_configs_certificate_truststore_password`: password of your truststore.
- `ZWE_zowe_workspaceDirectory`: set to `/component`

Review the [ZAAS package start script](../zaas-package/src/main/resources/bin/start.sh) to see other environment variables that can be set.

Example commands:

```
# pull image
docker pull zowe-docker-release.jfrog.io/ompzowe/zaas-service:3
# start container
docker run -it --rm -p 7558:7558 \
    -v $(pwd)/keystore:/home/zowe/keystore \
    -e ZWE_configs_certificate_keystore_file=/home/zowe/keystore/localhost/localhost.keystore.p12 \
    -e ZWE_configs_certificate_keystore_alias=localhost \
    -e ZWE_configs_certificate_keystore_password=password \
    -e ZWE_configs_certificate_truststore_file=/home/zowe/keystore/localhost/localhost.truststore.p12 \
    -e ZWE_configs_certificate_truststore_password=password \
    -e ZWE_zowe_workspaceDirectory=/component \
    zowe-docker-release.jfrog.io/ompzowe/caching-service:3
```
