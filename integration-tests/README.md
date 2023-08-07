<!-- omit in toc -->
# Integration Tests

- [Introduction](#introduction)
- [The tests take care of the services](#the-tests-take-care-of-the-services)
- [Local run of services and then integration tests](#local-run-of-services-and-then-integration-tests)
- [The services run elsewhere](#the-services-run-elsewhere)
- [Manual testing of Discovery Service in HTTP mode](#manual-testing-of-discovery-service-in-http-mode)
- [Running all tests (including slow)](#running-all-tests-including-slow)
- [Running a Specific Test](#running-a-specific-test)
- [Running specific tests to test Zowe RC](#running-specific-tests-to-test-zowe-rc)

## Introduction

Integration tests are meant to test a functionality that requires multiple running services. We recommend you test most test cases using unit tests in the module. Use integration tests only when necessary.

The Integration tests can be run against specific setup and instance or they can start the services itself. Either way the test suite detects when services are started up and are ready to begin testing.

## The tests take care of the services

In this setup the integration test suite starts and stops the service. It is aimed at all runs for testing the integrations off-platform.

**Note:** In this mode, the code assumes both Java and NodeJs are accessible in the PATH, make sure these are the correct versions.
`JAVA_HOME` and `NODE_HOME` can be used to customise their location as well.

**Follow these steps:**

Perform a Localhost Quick start when you need to run the tests on your local machine. The setup won't work on the Windows machines as we use production shell scripts in this setup. In case of Window consult [Local run of services and then integration tests](#local-run-of-services-and-then-integration-tests)

1. Run the following shell script:

    ```shell
    ./gradlew runCITests
    ```

2. (Optional) Change the host/port/scheme for the Gateway and Discovery Service with the following shell script:

    ```sh
    ./gradlew runCITests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD> -Ddiscovery.host=<DS_HOST> -Ddiscovery.port=<DS_PORT>  -Dgateway.host=<GW_HOST> -Dgateway.port=<GW_PORT> -Dgateway.scheme=https
    ```

*Note:* As mentioned above, this alternative uses the production start.sh scripts to startup the services. Make sure the desired Java version is the default in the PATH.

## Local run of services and then integration tests

In this case you are using either Windows machine or want to start services yourselves for any reason.

**Follow these steps:**

1. Install `concurrently` globally:

  ```sh
   npm install -g concurrently
   ```

2. Build all modules:

  ```sh
   ./gradlew build
   ```

3. Run all service on your local machine:

  ```sh
   npm run api-layer-ci
   ```

4. Run integration tests

   ```sh
   ./gradlew runCITests -Denvironment.offPlatform=true
   ```

## The services run elsewhere

In this case the services are running somewhere, and the integration tests verify that the services work well.

**Follow these steps:**

1. Run the following shell script:

    ```sh
   ./gradlew runAllIntegrationTests \
        -Dcredentials.user=${MF_USERID} \
        -Dcredentials.password=${MF_PASSWORD} \
        -Denvironment.offPlatform=true
    ```

## Manual testing of Discovery Service in HTTP mode

The Discovery Service in HTTP mode is not integrated within the pipeline. You can perfom tests of the Discovery Service in HTTP mode manually.

**Follow these steps:**

1. Set the `spring.profiles` value in the Discovery Service configuration file to `http`.
2. Change the `discoveryServiceUrls` value in the configuration file of the service that you want to register to `http://eureka:password@localhost:10011/eureka/`.
3. Run Discovery Service and verify that you can login into the Discovery Service homepage by using basic authentication with Eureka credentials.
4. Run your service and check that it is registered to Eureka.

**Note:** API Catalog can call refresh of static API definitions on discovery service. Discovery service in HTTP mode protects it's endpoints with basic auth instead of client certificate. If you want this to work, you have to supply additional command line parameters to API Catalog at startup. Default values are `eureka:password`. The same set of parameters exist on Discovery service to configure the username and password for HTTP mode.

```txt
-Dapiml.discovery.userid=**** -Dapiml.discovery.password=****
```

## Running all tests (including slow)

Run special integration tests for tests that need to be performed slowly such as when you need to test timeouts.

**Note:** Executing these slow steps with other tests causes
the entire test suite to take longer to execute.

Slow tests are annotated using @SlowTests as in the following example:

```java
@Test
@SlowTests
@SuppressWarnings("squid:S1160")
public void shouldCallLongButBelowTimeoutRequest() throws IOException {
```

Start the suite of slow tests by executing the following shell script:

```shell
./gradlew :integration-tests:runAllIntegrationTests
```

## Running a Specific Test

```sh
./gradlew :integration-tests:runIntegrationTests --tests org.zowe.apiml.gatewayservice.PassTicketTest
```

## Running specific tests to test Zowe RC

Run special integration tests to test a Zowe instance as part of the RC testing process.

```shell
./gradlew :integration-tests:runAllIntegrationTestsForZoweTesting
```

Tests annotated with `@TestsNotMeantForZowe` are excluded from this test suite (e.g Discoverable Client tests, PassTicket tests, etc...).
