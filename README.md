# API Mediation Layer

[Build Status](https://wash.zowe.org:8443/job/API_Mediation/job/master/)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=zowe_api-layer&metric=coverage)](https://sonarcloud.io/dashboard?id=zowe_api-layer)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zowe_api-layer&metric=alert_status)](https://sonarcloud.io/dashboard?id=zowe_api-layer)

The API Mediation Layer provides a single point of access for mainframe service REST APIs. The layer offers enterprise, cloud-like features such as high-availability, scalability, dynamic API discovery, consistent security, a single sign-on experience, and documentation. The API Mediation Layer facilitates secure communication across loosely coupled microservices through the API Gateway. The API Mediation Layer consists of three components: the Gateway, the Discovery Service, and the Catalog. The Gateway provides secure communication across loosely coupled API services. The Discovery Service enables you to determine the location and status of service instances running inside the API ML ecosystem. The Catalog provides an easy-to-use interface to view all discovered services, their associated APIs, and Swagger documentation in a user-friendly manner.

[More information](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)

To learn more about the changes consult [CHANGELOG](CHANGELOG.md)

## Contents

  * [Run API Mediation Layer locally](#run-api-mediation-layer-locally)
    + [Prerequisites](#prerequisites)
    + [Quick start](#quick-start)
  * [Security](#security)
  * [Run integration tests](#run-integration-tests)
  * [Certificates](#certificates)
  * [Contributor guidelines](#contributor-guidelines)
  * [Local configuration of services](#local-configuration-of-services)
  * [Onboarding Services](#onboarding-services)
  * [More Information](#more-information)

## Run API Mediation Layer locally

### Prerequisites

Following platform is required to run the API Mediation Layer:

* Java SE Development Kit 8 
(<https://jdk.java.net/java-se-ri/8-MR3> or 
<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html> or <https://www.ibm.com/developerworks/java/jdk/>)

Following tools are required to build and develop API Mediation Layer:

* Node.js version 8.x and npm are required to be installed globally to be able to build the API Catalog UI
(<https://nodejs.org/dist/latest-v8.x/>)

### Quick start

1. Install the package manager `pnpm` globally in order to build the project:

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

5. Run all service on local machine:

  ```sh
   npm run api-layer
   ```

Alternatively if you want to use Docker to use the api-layer consult the [README](docker/README.md)


## Security

The API Mediation Layer uses a dummy authentication provides as a default security provider for the development purposes. To log in, use `user` as username and password.

[Learn more about security here](#https://docs.zowe.org/stable/extend/extend-apiml/api-mediation-security.html#authentication-for-api-ml-services)

## Run integration tests

Follow the instructions in [Integration Tests](integration-tests/README.md) to run integration tests.

## Certificates

For more information about how the certificates between APIML services are setup for localhost, see [TLS Certificates for localhost](keystore/README.md).

## Contributor guidelines

Follow the guidelines in [Contributing](CONTRIBUTING.md) to add new functionality.

## Local configuration of services

Follow the guidelines in [Local Configuration](docs/local-configuration.md) to set local environment properties for testing on your local machine include HTTPS setup.

Also if you use IntelliJ IDEA, see [learn how to configure Run Dashboard](docs/idea-setup.md) to use these local configurations.

If you use Visual Studio Code, see [how to configure it](docs/vscode-setup.md) to develop and debug local configurations.

## Onboarding Services

The guidelines to onboard services are available in the [Zowe Docs#Onboarding Overview](https://docs.zowe.org/stable/extend/extend-apiml/onboard-overview.html)

## More Information

| To learn about:                       | Refer here:                                                                                                                                                                             |
|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Core Service - API Catalog            | [Zowe Docs](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)                                                                                                          |
|       Core Service - API Catalog UI   | [README](api-catalog-ui/frontend/README.md), [Integration Test README](api-catalog-ui/frontend/src/integration-tests/README.md)       |
| Core Service - Discovery Service      | [Zowe Docs](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)                                                                                                          |
| Core Service - Gateway Service        | [Zowe Docs](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)                                                                                                          |
| APIML SDK - Java Enabler              | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler.html)                                                                                                        |
| APIML SDK - Spring Enabler            | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/onboard-spring-boot-enabler.html)                                                                                                       |
| APIML SDK - ZAAS Client               | [README](zaas-client/README.md)                                                                                                                                                         |
| Sample Service - Java Enabler         | [README](onboarding-enabler-java-sample-app/README.md)                                                                                                                                  |
|       Sample Service - Spring Enabler | [README](integration-enabler-spring-v1-sample-app/README.md)                                                                                                                            |

