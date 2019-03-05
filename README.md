# API Mediation Layer.

This is a page about API Mediation Layer.

* [Quick start 1-2-3](#quick-start-1-2-3)
    * [Build](#build)
        * [Prerequisites for build](#prerequisites-for-build)
        * [Build all modules](#build-all-modules)
        * [Unit tests](#unit-tests)
        * [Measure code coverage](#measure-code-coverage)
    * [Run](#run)
        * [Prerequisites for run](#prerequisites-for-run)
    * [Use](#use)
        * [Run integration tests](#run-integration-tests)
* [Features](#features)
* [Topics](#topics)

## Quick start 1-2-3


1. Build
2. Run
3. Use

## Build


### Prerequisites for build
Following platform is required to run the API Mediation Layer:

* **Java** Oracle or IBM Java SE Development Kit 8 (http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html or https://www.ibm.com/developerworks/java/jdk/), Java 10 is not supported.
* **Node.js** You should be able to run Node.js on your machine.

### Build all modules
```shell
./gradlew build
```

### Unit tests
Unit tests for Java and TypeScript modules are executed as a part of the build process.


#### Measure code coverage

For the code coverage of all modules, run:
```shell
./gradlew coverage
```
The code coverage for new code should be higher than 60% and should not be decreased for existing code.

The reports in HTML format are stored `build/reports/jacoco/test/html/index.html` for each Java module.

For the code coverage of a single Java module (for example `discovery-service`), run:
```shell
./gradlew :discovery-service:jacocoTestReport
```
You can an individual test class by:
```shell
./gradlew :discovery-service:test --tests com.ca.mfaas.discovery.staticdef.ServiceDefinitionProcessorTest
```

## Run
### Prerequisites for run
* **concurrently**
You need to install `concurrently` globally:
```shell
npm install -g concurrently
Run all service on the local machine:
```shell
npm run api-layer
```
## Use

### Run integration tests

Follow the instructions in [Integration Tests](integration-tests/README.md) to run integration tests.

## Features

### Security

For more information about how the certificates between APIML services are set up, see [TLS Certificates for localhost](keystore/README.md).

## Topics

### Contributor guidelines

Follow the guidelines in [Contributing](CONTRIBUTING.md) to add new functionality.


### Local configuration of services

Follow the guidelines in [Local Configuration](docs/local-configuration.md) to set local environment properties for testing on your local machine include HTTPS setup.

Also if you use IntelliJ IDEA, see [learn how to configure Run Dashboard](docs/idea-setup.md) to use these local configurations.


### Adding services that do not support API Mediation Layer natively

See [Adding Services to API Gateway without Code Changes](docs/static-apis.md).


### API Catalog UI

For more information about the UI of the Catalog see its [README](api-catalog-ui/frontend/README.md).

Yan collobarator
