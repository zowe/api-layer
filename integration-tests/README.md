## Integration Tests

### Introduction

Integration tests are meant to test functionality that requires multiple running services.
You should test majority of the test cases using unit tests in the module and use integration tests
only if necessary. 

Integration tests work only with a running instance of all services.
These services needs to be started by you.

Integration test suite detects when the services are started up 
completely and ready for tests to start.

### General Quick start

1. Deploy and run all services.

2. Run.
    ```shell
    ./gradlew runIntegrationTests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD>
    ``` 

3. (Optional) Change host/port/scheme for gateway-service and discovery-service.
    ```shell
    ./gradlew runIntegrationTests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD> -Ddiscovery.host=<DS_HOST> -Ddiscovery.port=<DS_PORT>  -Dgateway.host=<GW_HOST> -Dgateway.port=<GW_PORT> -Dgateway.scheme=https
    ```

### Localhost Quick start

1. Deploy and run all services.

2. Run.
    ```shell
    ./gradlew runLocalIntegrationTests
    ``` 

3. (Optional) Run all local tests including all sample services. 
    ```shell
    ./gradlew runAllLocalIntegrationTests
    ```

### Manual testing of Discovery Service in HTTP mode

1. Set the `spring.profiles` value in the Discovery Service configuration file to `http`.
2. Change the `discoveryServiceUrls` value in the configuration file of the service that you want to register to `http://eureka:password@localhost:10011/eureka/`.
3. Run Discovery Service and verify that you can login into the Discovery Service homepage by using basic authentication with Eureka credentials.
4. Run your service and check that it is registered to Eureka.

### Running all tests (including slow)

Some tests can be categorized as slow using:
```java
    @Test
    @Category(SlowTests.class)
    @SuppressWarnings("squid:S1160")
    public void shouldCallLongButBelowTimeoutRequest() throws IOException {
```

This is for special integration tests that need to be slow (e.g. to test timeouts).
They should not be executed always with others tests because it will make
the test suite slow and used less.

Such tests can be started using:
```shell
./gradlew :integrationTests:runAllIntegrationTests
```

All tests are executed on Jenkins.
