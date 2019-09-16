# WebSocket Support in Gateway

The gateway-service includes basic WebSocket proxy. It is intended for enabling applications that use WebSockets together with web UI and REST API to be accessed via the gateway.

The WebSockets are not documented via API Catalog.

The service can define what WebSocket services are exposed using Eureka metadata.

Example:

    eureka:
        instance:
            metadata-map:
                routes:
                    ws_v1:
                        gatewayUrl: "ws/v1"
                        serviceUrl: /discoverableclient/ws

This maps requests `wss://gatewayHost:port/ws/v1/serviceId/path` to `ws://serviceHost:port/discoverableclient/ws/path` where `serviceId` is the service ID of the service and `path` is the ramaining path segment in the URL.

## Security

The API gateway is usually using TLS so the protocol is `wss`. The services can be using TLS and then the gateway will use `wss` to access them as well. If the service is not using TLS then the gateway will use `ws` protocol without TLS.

## Diagnostics 

The list of active routed WebSocket sessions is available at new Actuator endpoint `websockets`. On `localhost`, it is available at https://localhost:10010/application/websockets.

## Limitations

The error HTTP status codes can be different. The WebSocket session starts before the session between the gateway and the service is started. In case of failure to connect to the service, the WebSocket session is terminated with a WebSocket close code and reason text instead of HTTP error code.
