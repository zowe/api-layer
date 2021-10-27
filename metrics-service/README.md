# Metrics Service

The Metrics service provides a UI that displays service metrics.
At this time only HTTP metrics are displayed, however eventually system metrics (e.g. CPU usage) will be displayed as well. 

The initial implementation of the service uses Netflix Hystrix to provide HTTP metrics, Netflix Turbine to aggregate the metrics, and Netflix Hystrix Dashboard to display them.
Eventually, the display of metrics will use either a better pre-built UI, or will be a custom implementation. Eventually, HTTP metrics will be able to be supplied by any provider, not just Hystrix/Turbine.

## Architecture

TODO

## How to use

The Metrics Service is built on top of the spring enabler, which means that it is dynamically registered to the API Mediation Layer. It appears in the API Catalog under the tile "Zowe Applications".

The Metrics Service is accessible from the Gateway homepage, or at `https://host:port/metrics-service/ui/v1`.
The Gateway must have the configuration parameter `apiml.metrics.enabled=true` in order to display the Metrics Service on the Gateway homepage.

The Gateway path is `/metrics-service/{type}/v1`. For example `https://{gwhost}:{gwport}/metrics-service/ui/v1` is the homepage and displays service metrics.
In order to view the metrics stream, aggregated for each service,use `https://{gwhost}:{gwport}/metrics-service/sse/v1/turbine/stream?cluster=${cluster}`, where ${cluster} is an uppercase service ID.

At this time only core APIML services are expected to be onboarded to the Metrics Service.
While it may be possible for Metrics Service configuration to be overridden to onboard other services, this is not supported and may not work properly.  

## How to run for local development

The Metrics Service is a Spring Boot application. You can either add it as a run configuration and run it together with other services.

At this time, the Gateway by default has the Metrics Service disabled. In order to view the Metrics Service set `apiml.metrics.enabled=true` for the Gateway. 

In local usage, the Metrics Service will run at `https://localhost:10019/metrics-service`.

**Note: At this time, the Gateway and Discovery Service need to have certificate verification disabled in order for the Metrics Service to register and be accessible.**

## Configuration properties

The Metrics Service uses the standard `application.yml` structure for configuration.

The `turbine` configuration block tells the Metrics Service where to collect Hystrix metrics from and how to aggregate them.
For example:

```yml
turbine:
    aggregator:
        clusterConfig: DISCOVERABLECLIENT,APICATALOG # How to aggregate Hystrix metrics, in this case by service ID. Uppercase required.
    instanceUrlSuffix:
        DISCOVERABLECLIENT: discoverableclient/application/hystrix.stream # Hystrix stream endpoint for Discoverable Client (not including host/port)
        APICATALOG: apicatalog/application/hystrix.stream # Hystrix stream endpoint for API Catalog (not including host/port)
    appConfig: discoverableclient,apicatalog # service IDs for which Hystrix metrics will be collected
```

Expected behaviour is each service needs to expose `hystrix.stream` in `management.endpoints.web.exposure.include`.
However, this is may change or not be needed if the Hystrix stream endpoint configured in the Metrics Service `turbine.instanceUrlSuffix` is a different endpoint.

In the Metrics Service endpoint that displays the streaming metrics, `https://localhost:10010/metrics-service/sse/v1/turbine/stream?cluster=${cluster}`,
${cluster} must match a value in `turbine.aggregator.clusterConfig`.
