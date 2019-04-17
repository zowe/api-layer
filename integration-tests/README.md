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

2. Run
    ```shell
    ./gradlew runIntegrationTests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD>
    ``` 

3. (Optional) Change host/port/scheme for gateway-service and discovery-service.
    ```shell
    ./gradlew runIntegrationTests -Dcredentials.user=<MAINFRAME_USERID> -Dcredentials.password=<PASSWORD> -Ddiscovery.host=<DS_HOST> -Ddiscovery.port=<DS_PORT>  -Dgateway.host=<GW_HOST> -Dgateway.port=<GW_PORT> -Dgateway.scheme=https
    ```

### Localhost Quick start

1. Deploy and run all services.

2. Run
    ```shell
    ./gradlew runLocalIntegrationTests
    ``` 

3. (Optional) Run all local tests including all sample services 
    ```shell
    ./gradlew runAllLocalIntegrationTests
    ```

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
