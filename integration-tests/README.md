## Integration Tests

### Introduction

Integration tests are meant to test a functionality that requires multiple running services.
We recommend you test most test cases using unit tests in the module. Use integration tests
only when necessary. 

Integration tests require running instances of all services.
These services must be started by the user.

The integration test suite detects when services are started up and are ready to begin testing.

### General Quick start

Perform a general quick start to execute tests within the pipeline. 

**Follow these steps:**

1. Deploy and run all services.

2. Run the following shell script:
    ```shell
    ./gradlew runIntegrationTests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD>
    ``` 

3. (Optional) Change the host/port/scheme for the Gateway and Discovery Service with the following shell script:

    ```shell
    ./gradlew runIntegrationTests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD> -Ddiscovery.host=<DS_HOST> -Ddiscovery.port=<DS_PORT>  -Dgateway.host=<GW_HOST> -Dgateway.port=<GW_PORT> -Dgateway.scheme=https
    ```

### Localhost Quick start

Perform a Localhost Quick start when you need to run the tests on your local machine.

**Follow these steps:**

1. Deploy and run all services.

2. Run the following shell script:

    ```shell
    ./gradlew runLocalIntegrationTests
    ``` 

3. (Optional) Run all local tests including all sample services with the following shell script: 
    ```shell
    ./gradlew runAllLocalIntegrationTests
    ```
### Manual testing of Discovery Service in HTTP mode

The Discovery Service in HTTP mode is not integrated within the pipeline. You can perfom tests of the Discovery Service in HTTP mode manually.

**Follow these steps:**

1. Set the `spring.profiles` value in the Discovery Service configuration file to `http`.
2. Change the `discoveryServiceUrls` value in the configuration file of the service that you want to register to `http://eureka:password@localhost:10011/eureka/`.
3. Run Discovery Service and verify that you can login into the Discovery Service homepage by using basic authentication with Eureka credentials.
4. Run your service and check that it is registered to Eureka.

### Running all tests (including slow)

Run special integration tests for tests that need to be performed slowly such as when you need to test timeouts. 

**Note:** Executing these slow steps with other tests causes 
the entire test suite to take longer to execute.

Slow tests are annotated using @Category(SlowTests.class) as in the following example: 

```java
    @Test
    @Category(SlowTests.class)
    @SuppressWarnings("squid:S1160")
    public void shouldCallLongButBelowTimeoutRequest() throws IOException {
```

Start the suite of slow tests by executing the following shell script:

```shell
./gradlew :integrationTests:runAllIntegrationTests
```
