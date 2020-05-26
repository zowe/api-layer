# WebSocket Support in Gateway

The gateway-service includes a basic WebSocket proxy which enables Gateway access to applications that use WebSockets together with a web UI and REST API.

The WebSockets are not documented via the API Catalog.

The service can define what WebSocket services are exposed using Eureka metadata.

Example:

    eureka:
        instance:
            metadata-map:
                apiml:
                    routes:
                        ws_v1:
                            gatewayUrl: "ws/v1"
                            serviceUrl: /discoverableclient/ws

This maps requests `wss://gatewayHost:port/ws/v1/serviceId/path` to `ws://serviceHost:port/discoverableclient/ws/path` 
where:

* `serviceId` is the service ID of the service
* `path` is the remaining path segment in the URL.

## Security

The API Gateway is usually using TLS with the `wss` protocol. Services that use TLS enable the Gateway to use `wss` to access these services. Services that do not use TLS require the gateway to use the `ws` protocol without TLS.

## Diagnostics 

The list of active routed WebSocket sessions is available at the Actuator endpoint `websockets`. On `localhost`, it is available at https://localhost:10010/application/websockets.

## Limitations

Different HTTP status code errors may result. The WebSocket session starts before the session between the Gateway and the service starts. When a failure occurs when connecting to a service, the WebSocket session terminates with a WebSocket close code and the reason for the failure rather than an HTTP error code.
