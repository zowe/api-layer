# API Mediation Layer

[Build Status](https://wash.zowe.org:8443/job/API_Mediation/job/master/)

[![codecov](https://codecov.io/gh/zowe/api-layer/branch/master/graph/badge.svg)](https://codecov.io/gh/zowe/api-layer)
[![SonarQube](https://jayne.zowe.org:9000/api/project_badges/measure?project=zowe%3Aapi-mediation-layer&metric=alert_status)](https://jayne.zowe.org:9000/dashboard?id=zowe%3Aapi-mediation-layer)

The home of Zowe API Mediation Layer

## Prerequisites

Following platform is required to run the API Mediation Layer:

* Oracle or IBM Java SE Development Kit 8 (<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html> or <https://www.ibm.com/developerworks/java/jdk/>), Java 10 is not supported

### Other development prerequisites

Following tools are required to build and develop API Mediation Layer:

Nodejs and npm are required to be installed globally to be able to build the API Catalog ui

## Quick start

1. Install the package manager `pnpm` globally in order to build the project:
    
        npm add -g pnpm
    
2. Install `concurrently` globally:
   
       npm install -g concurrently
       
3. Build all modules:

       ./gradlew build

4. Run all service on local machine:

       npm run api-layer

## Authentication service

The API Mediation Layer uses a dummy authentication provides as a default security provider for the development purposes. To log in, use `user` as username and password.

### (Optional) z/OSMF Authentication

Perform the following steps to use the real authentication service:
 
1. Configure a valid z/OSMF instance using the following sample configuration `config/local/api-defs/zosmf-sample.yml`.

2. Modify [gateway-service.yml](config/local/gateway-service.yml) with a z/OSMF configuration

```
apiml:
    security:
        auth:
            provider: zosmf
            zosmfServiceId: zosmfId  # Replace me with the z/OSMF service id
```

Ensure that the z/OSMF certificate is trusted by APIML. You have two options:

1. Import the certificate to the APIML keystore using `scripts/apiml_cm.sh --action trust` as described at [Trust certificates of other services](/keystore/README.md#Trust-certificates-of-other-services)
   **The apiml_cm.sh script has been moved to [zowe-install-packaging repository](https://github.com/zowe/zowe-install-packaging/blob/staging/bin/apiml_cm.sh)**

2. Disable certificate validation. For more information, see [Disabling certificate validation on localhost](/keystore/README.md#Disabling-certificate-validation-on-localhost)

## Run unit tests

Unit tests for Java and TypeScript modules are executed as a part of the build process.

## Measure code coverage

For the code coverage of all modules, run:

    ./gradlew coverage

The code coverage for new code should be higher that 60% and should not be decreased for existing code.

The reports in HTML format are stored `build/reports/jacoco/test/html/index.html` for each Java module.

For the code coverage of a single Java module (for example `discovery-service`), run:

    ./gradlew :discovery-service:jacocoTestReport

You can an individual test class by:

    ./gradlew :discovery-service:test --tests com.ca.mfaas.discovery.staticdef.ServiceDefinitionProcessorTest

## Run integration tests

Follow the instructions in [Integration Tests](integration-tests/README.md) to run integration tests.

## Security

For more information about how the certificates between APIML services are setup, see [TLS Certificates for localhost](keystore/README.md).

## Contributor guidelines

Follow the guidelines in [Contributing](CONTRIBUTING.md) to add new functionality.

## Local configuration of services

Follow the guidelines in [Local Configuration](docs/local-configuration.md) to set local environment properties for testing on your local machine include HTTPS setup.

Also if you use IntelliJ IDEA, see [learn how to configure Run Dashboard](docs/idea-setup.md) to use these local configurations.

If you use Visual Studio Code, see [how to configure it](docs/vscode-setup.md) to develop and debug local configurations.

## Adding services that does not support API Mediation Layer natively

See [Adding Services to API Gateway without Code Changes](docs/static-apis.md).

## API Catalog UI

For more information about the UI of the Catalog see its [README](api-catalog-ui/frontend/README.md).
