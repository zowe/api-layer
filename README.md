# API Mediation Layer

[Build Status](https://wash.zowe.org:8443/job/API_Mediation/job/master/)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=zowe_api-layer&metric=coverage)](https://sonarcloud.io/dashboard?id=zowe_api-layer)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zowe_api-layer&metric=alert_status)](https://sonarcloud.io/dashboard?id=zowe_api-layer)

The home of Zowe API Mediation Layer

## Prerequisites

The following platform is required to run the API Mediation Layer:

* Oracle or IBM Java SE Development Kit 8 (<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html> or <https://www.ibm.com/developerworks/java/jdk/>).

**Note:** Java 10 is not supported.

### Other development prerequisites

The following tools are required to build and develop the API Mediation Layer:

* Gloablly installed Node.js and npm are required to build the API Catalog UI.

## Quick start

1. Install the package manager `pnpm` globally to build the project:

    ```sh
    npm add -g pnpm
    ```

2. Install npm packages for the UI:

    ```sh
    cd api-catalog-ui/frontend/; pnpm install; cd ../..
    ```

3. Install `concurrently` globally:

    ```sh
    npm install -g concurrently
    ```

4. Build all modules:

    ```sh
    ./gradlew build
    ```

5. Run all service on a local machine:

    ```sh
    npm run api-layer
    ```

## Authentication service

The API Mediation Layer uses a dummy authentication which provides a default security provider for the development purposes. To log in, use `user` as username and password.

### (Optional) z/OSMF Authentication

Perform the following steps to use the real authentication service:

1. Configure a valid z/OSMF instance using the following sample configuration:

 `config/local/api-defs/zosmf-sample.yml`.

2. Modify [gateway-service.yml](config/local/gateway-service.yml) with z/OSMF configuration:

```yaml
apiml:
    security:
        auth:
            provider: zosmf
            zosmfServiceId: zosmfId  # Replace me with the z/OSMF service id
```

Ensure that the z/OSMF certificate is trusted by API ML. Use one of the following options:

1. Import the certificate to the API ML keystore using `scripts/apiml_cm.sh --action trust` as described at [Trust certificates of other services](/keystore/README.md#Trust-certificates-of-other-services).

   **Note:** The apiml_cm.sh script is now in [zowe-install-packaging repository](https://github.com/zowe/zowe-install-packaging/blob/staging/bin/apiml_cm.sh).

2. Disable certificate validation. For more information, see [Disabling certificate validation on localhost](/keystore/README.md#Disabling-certificate-validation-on-localhost).

## Run unit tests

Unit tests for Java and TypeScript modules are executed as a part of the build process.

## Measure code coverage

To determine code coverage of all modules, issue the following command:

```sh
    ./gradlew coverage
```

Ensure that code coverage for new code is higher that 60%. Also ensure that code coverage does not decrease for existing code.

The reports in HTML format are stored at `build/reports/jacoco/test/html/index.html` for each Java module.

For the code coverage of a single Java module (for example `discovery-service`), issue the following command:

```sh
    ./gradlew :discovery-service:jacocoTestReport
```

You can add an individual test class with the following command:

```sh
    ./gradlew :discovery-service:test --tests org.zowe.apiml.discovery.staticdef.ServiceDefinitionProcessorTest
```

## Run integration tests

Follow the instructions in [Integration Tests](integration-tests/README.md) to run integration tests.

## Security

For more information about how the certificates between APIML services are set up, see [TLS Certificates for localhost](keystore/README.md).

## Contributor guidelines

Follow the guidelines in [Contributing](CONTRIBUTING.md) to add new functionality.

## Local configuration of services

Follow the guidelines in [Local Configuration](docs/local-configuration.md) to set local environment properties for testing on your local machine. These guidelines include HTTPS setup.

If you use IntelliJ IDEA, see [learn how to configure Run Dashboard](docs/idea-setup.md) to use these local configurations.

If you use Visual Studio Code, see [how to configure it](docs/vscode-setup.md) to develop and debug local configurations.

## Adding services that does not support API Mediation Layer natively

See [Adding Services to API Gateway without Code Changes](docs/static-apis.md).

## API Catalog UI

For more information about the UI of the Catalog see the [API Catalog UI frontend README](api-catalog-ui/frontend/README.md).

