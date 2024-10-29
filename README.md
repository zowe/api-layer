# API Mediation Layer

[Build Status](https://github.com/zowe/api-layer/actions)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=zowe_api-layer&metric=coverage&branch=v2.x.x)](https://sonarcloud.io/dashboard?id=zowe_api-layer)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zowe_api-layer&metric=alert_status&branch=v2.x.x)](https://sonarcloud.io/dashboard?id=zowe_api-layer)

The API Mediation Layer (API ML) provides a single point of access for mainframe service REST APIs. The API ML offers enterprise, cloud-like features such as high-availability, scalability, dynamic API discovery, consistent security, a single sign-on experience, and documentation. The API ML consists of three components: the Gateway, the Discovery Service, and the API Catalog. The API ML facilitates secure communication across loosely coupled microservices through the API Gateway. The Discovery Service enables you to determine the location and status of service instances running inside the API ML ecosystem. The API Catalog provides a user-friendly, easy-to-use interface to view all discovered services, their associated APIs, and Swagger documentation.

**Notes:**

* For more general information for end-users, see the API ML [Overview](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer).
* To learn more about changes to the API ML, consult the [CHANGELOG](CHANGELOG.md).
* For developers, review the [developer documentation](./docs) and the [Contributor guidelines](#contributor-guidelines).

## Contents

* [Run API Mediation Layer locally](#run-api-mediation-layer-locally)
  * [Prerequisites](#prerequisites)
  * [Quick start](#quick-start)
* [Security](#security)
* [Run integration tests](#run-integration-tests)
* [Certificates](#certificates)
* [Contributor guidelines](#contributor-guidelines)
* [Local configuration of services](#local-configuration-of-services)
* [Onboarding Services](#onboarding-services)
* [More Information](#more-information)
* [Contact Us](#contact-us)

## Run API Mediation Layer locally

### Prerequisites

The following platform is required to run the API Mediation Layer:

* Java SE Development Kit 8 or 11
* Java version 17 isn't supported for building yet

The following tools are required to build and develop the API Mediation Layer:

* Node.js and npm are required to be installed globally to run npm commands in project root folder.
  
  * <https://nodejs.org/dist/>

  * During build, correct node version is automatically downloaded and built with.

### Quick start

**Follow these steps:**

1. Build all modules:

  ```sh
   ./gradlew build
   ```

2. Install the same version of the `concurrently` globally:

  ```sh
   npm install -g concurrently@^6.0.0
   ```

3. Run all service on your local machine:

  ```sh
   npm run api-layer
   ```

Alternatively, to use Docker to run the API ML, consult the [Docker README](docker/README.md).

## Security

By default the API Mediation Layer for local development uses mock zOSMF as the authentication provider. For development purposes, log in using the default setting `USER` for the username, and `validPassword` as the password

The API Mediation Layer can also use dummy credentials for development purposes. For development purposes, log in using the default setting `user` for the username, and `user` as the password.  
<details>
  <summary>Configure dummy credentials provider</summary>

  ### Configure `dummy` credentials provider

  Modify [gateway-service.yml](/config/local/gateway-service.yml)

  ```yaml
        apiml:
          security:
            auth:
              #provider: zosmf
              provider: dummy

  ```

</details>

For more information, see [API Mediation Layer Security](https://docs.zowe.org/stable/extend/extend-apiml/zowe-api-mediation-layer-security-overview).

## Run integration tests

To run integration tests, follow the instructions in [Integration Tests](integration-tests/README.md).

## Certificates

For more information about how the certificates between API ML services are set up for localhost, see [TLS Certificates for localhost](keystore/README.md).

## Contributor guidelines

To add new functionality, follow the guidelines in [Contributing](CONTRIBUTING.md).

## Local configuration of services

To set local environment properties for testing on your local machine including HTTPS setup, follow the guidelines in [Local Configuration](docs/local-configuration.md).

Review [IDE setup](docs/ide-setup.md) to see how to configure popular IDEs for API ML development.

## Onboarding Services

For guidelines to onboard services, see [Zowe Docs#Onboarding Overview](https://docs.zowe.org/stable/extend/extend-apiml/onboard-overview.html).

## More Information

| To learn about:                       | Refer to:                                                                                                                              |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Core Service - API Catalog            | [Zowe Docs](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)                                            |
|       Core Service - API Catalog UI   | [README](api-catalog-ui/frontend/README.md)                                                                                            |
| Core Service - Discovery Service      | [Zowe Docs](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)                                            |
| Core Service - Gateway Service        | [Zowe Docs](https://docs.zowe.org/stable/getting-started/overview.html#api-mediation-layer)                                            |
| Core Service - Metrics Service        | [README](metrics-service/README.md)                                                                                                    |
|       Core Service - Metrics Service UI | [README](metrics-service-ui/README.md)                                                                                               |
| APIML SDK - Java Enabler              | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler)                                               |
| APIML SDK - Micronaut Enabler         | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/onboard-micronaut-enabler)                                                |
| APIML SDK - Node.js Enabler           | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/onboard-nodejs-enabler)                                                   |
| APIML SDK - Spring Enabler            | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/onboard-spring-boot-enabler)                                              |
| APIML SDK - ZAAS Client               | [Zowe Docs](https://docs.zowe.org/stable/extend/extend-apiml/api-mediation-security/#zaas-client)                                      |
| Sample Service - Java Enabler         | [README](onboarding-enabler-java-sample-app/README.md)                                                                                 |
| Sample Service - Spring Enabler       | [README](onboarding-enabler-spring-sample-app/README.md)                                                                               |
| Sample Service - Micronaut Enabler    | [README](onboarding-enabler-micronaut-sample-app/README.md)                                                                         |
| Sample Service - NodeJS Enabler       | [README](onboarding-enabler-nodejs-sample-app/README.md)                                                                               |

## Contact Us

Get in touch using [Zowe Communication Channels](https://github.com/zowe/community/blob/master/README.md#communication-channels). You can find us in the `#zowe-api` channel on Slack.
