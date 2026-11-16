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
                            gatewayUrl: "/ws/v1"
                            serviceUrl: /discoverableclient/ws

This maps requests `wss://gatewayHost:port/serviceId/ws/v1/path` to `ws://serviceHost:port/discoverableclient/ws/path` 
where:

* `serviceId` is the service ID of the service
* `path` is the remaining path segment in the URL.

## Security

The API Gateway is usually using TLS with the `wss` protocol. Services that use TLS enable the Gateway to use `wss` to access these services. Services that do not use TLS require the gateway to use the `ws` protocol without TLS.

## Diagnostics 

The list of active routed WebSocket sessions is available at the Actuator endpoint `websockets`. On `localhost`, it is available at https://localhost:10010/application/websockets.

## Limitations

The gateway connects to the service before completing the client's WebSocket handshake. Backend connection failures now reject the client's HTTP upgrade with 502 (or 504 for a connection deadline) instead of accepting the upgrade and then sending a WebSocket close code. Once both connections are established, failures use WebSocket close codes.

## Partial forwarding

Partial forwarding is the default for all WebSocket routes. No gateway setting or service metadata is required to forward text and binary messages incrementally. To use whole-message aggregation for all services, disable partial forwarding:

```yaml
# Gateway application configuration
server:
    webSocket:
        partial:
            enabled: false
```

To use aggregation for just one service, set its Eureka metadata:

```yaml
# Service registration
eureka:
    instance:
        metadata-map:
            apiml.websocket.partialForwarding: "false"
```

Missing service metadata defaults to partial forwarding; an explicit `false` selects aggregation. The gateway switch takes precedence: a service cannot enable partial forwarding when it is disabled globally. Service metadata cannot override gateway limits or timeouts. The gateway still performs the normal authentication, route selection, header filtering and TLS verification. Each connection retains the mode selected at its handshake. Disabling the gateway switch returns new connections to aggregation; existing connections must drain or reconnect.

Partial forwarding preserves message content and completion boundaries, including empty final parts. It does not preserve wire-frame boundaries or convert a fragmented message into separate application messages. A destination using whole-message handlers still buffers the message itself.

Client-leg compression is disabled for partial routes because Tomcat may retain compressed output until FIN. Aggregation routes retain compression negotiation. Ping/pong is handled independently by Tomcat on each connection; control frames are not relayed end to end. Suspended reads delay processing of incoming control frames until pending sends complete or the connection times out.

Both forwarding modes preserve the client's offered subprotocol list and make one backend handshake before completing the client upgrade. The gateway uses the backend's selected protocol on the client connection, even if it is not the client's first offer. If the backend selects no protocol, the client connection also has none. Backend reads remain suspended until the client endpoint is ready. A defensive consistency check still closes mismatched connections with 1011 and logs `ZWEAG725E` with the service and both protocols.

### Limits and timeouts

`server.webSocket.requestBufferSize`, mapped by launchers to `spring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length`, remains the aggregation buffer limit: bytes for binary messages and UTF-16 characters for text. Partial mode inherits this total-message limit unless a separate limit below is set. Enabling partial mode alone does not permit larger messages.

All entries in this table are under `server.webSocket.partial`:

| Setting | Default | Meaning |
| --- | ---: | --- |
| `enabled` | `true` | Uses partial forwarding unless the service selects aggregation; `false` forces aggregation globally |
| `binaryBufferSize` | 65536 | Receive staging bytes |
| `textBufferSize` | 32768 | Receive staging UTF-16 characters |
| `pendingBytes` | 1048576 | Accounted queued and in-flight bytes per direction |
| `pendingParts` | 64 | Queued and in-flight parts per direction, including empty parts |
| `globalPendingBytes` | 67108864 | Shared queued and in-flight byte budget |
| `maxConnections` | 256 | Active partial connections, including cleanup |
| `writerThreads` | 16 | Shared blocking-send workers |
| `connectThreads` | 4 | Shared backend-connect workers |
| `maxBinaryMessageSize` | 0 | Total binary bytes; zero inherits the legacy limit |
| `maxTextMessageSize` | 0 | Total UTF-16 characters; zero inherits the legacy limit |
| `messageTimeout` | 120000 | Milliseconds from the first partial callback to message completion |

Pending-byte accounting includes payload ownership, part overhead, and text conversion allowance. Staging, socket/TLS buffers, and thread stacks are additional memory costs. Receiving pauses while a directional send is pending. If already admitted callbacks exceed the bounded capacity, the relay closes instead of dropping parts. Message-limit failures close with 1009; capacity, timeout, and transport failures use 1011. A partially delivered message is never completed synthetically.

Startup validation requires at least two pending parts, `pendingBytes >= 2 * (max(binaryBufferSize, 5 * textBufferSize) + 64)`, and `globalPendingBytes >= 2 * pendingBytes`. Connection capacity must be between 1 and 100000. These checks reserve room for callbacks already admitted when receiving is suspended.

`server.webSocket.asyncWriteTimeout` now controls sends in both modes (default 60000 ms). Partial mode applies it to blocking sends and time spent queued for a writer. `connectTimeout` bounds backend connections (45000 ms), and `maxIdleTimeout` applies to both connections (3600000 ms; zero disables idle expiry). These previously unused settings now take effect for aggregation too.

In `zowe.yaml`, configure these values below `components.gateway.server.webSocket`. The combined launcher prefers `ZWE_components_gateway_server_webSocket_*` over `ZWE_configs_server_webSocket_*`; the standalone launcher uses the latter. Partial settings extend the environment names with `partial_`, for example `ZWE_configs_server_webSocket_partial_enabled`. Direct application configuration uses the property names above. Gateway limits always take precedence over service metadata.

### Monitoring and rollout

Micrometer exposes `apiml.websocket.active`, `apiml.websocket.pending.bytes`, `apiml.websocket.paused.reads`, `apiml.websocket.write.duration`, `apiml.websocket.connections` (mode), and `apiml.websocket.failures` (reason) for the partial relay. No payloads or credentials are recorded by these meters. Measure time to first part, throughput, memory, GC, and worker usage with ordinary, large, compressible, and slow-consumer traffic before increasing capacity. The bounded defaults are initial settings, not a throughput guarantee.

When rolling back a service that requires larger messages, also ensure aggregation has a sufficient buffer limit or change its application transfer protocol. Turning off partial forwarding alone cannot preserve streaming of oversized messages.

### Validation and local measurements

The [Discoverable Client integration test](../integration-tests/src/test/java/org/zowe/apiml/integration/proxy/PartialWebSocketProxyTest.java) uses the authenticated `/discoverableclient/ws/v1/partial-echo` route through a running gateway. Discoverable Client echoes each text or binary part with its original final-part flag. The test withholds FIN until the echo arrives, proving incremental forwarding in both directions; aggregation on either connection causes the test to time out. It also checks Unicode, binary content, empty final parts, and the next message on the same connection. Receive callbacks may divide data differently from the source frames.

Start the gateway and Discoverable Client with their default forwarding configuration. Wait for Discoverable Client to register, then run:

```sh
./gradlew :integration-tests:runCITests --rerun \
    --tests 'org.zowe.apiml.integration.proxy.PartialWebSocketProxyTest'
```

The test runs by default. For a deployment explicitly configured to use aggregation, pass `-Denvironment.websocket.partial=false` to skip it; this property does not configure the running services. `--rerun` forces execution against the running services even when Gradle considers the test inputs unchanged. Use the existing `environment.config` and gateway host/port overrides to target another deployment. As a negative control, leave the test enabled and disable gateway partial forwarding: it must fail while waiting for the first nonfinal echo.

The [standalone acceptance tests](../gateway-service/src/test/java/org/zowe/apiml/gateway/acceptance/PartialWebSocketTest.java) cover forwarding before FIN in both directions, exact 16 MiB fragmented and single-frame transfers, Unicode boundaries, slow receivers, protocol negotiation, compression, TLS, and concurrent sessions. The [combined deployment tests](../apiml/src/test/java/org/zowe/apiml/acceptance/PartialWebSocketTest.java) exercise both forwarding modes through APIML. Existing Tyrus and strict/non-strict TLS regressions remain in the suite.

The concurrent-transfer test also prints local comparison samples. On October 16, 2026, with TLS on the client leg, uncompressed data, a shared test JVM, and 64 KiB sends, one run produced:

| Mode | Connections | Message size per direction and connection | Transfer and verification time | JVM GC collections during sample |
| --- | ---: | ---: | ---: | ---: |
| Aggregation | 1 | 64 KiB | 65 ms | 5 |
| Partial | 1 | 64 KiB | 1 ms | 0 |
| Aggregation | 32 | 1 MiB | 4339 ms | 26 |
| Partial | 32 | 1 MiB | 1505 ms | 6 |

These are reproducible test workloads, not production throughput estimates: the timings include peer fixtures and content verification, and run order, JIT compilation, and debug logging affect the results. The partial worker pool stayed within 16 threads. Separate slow-receiver tests transferred 64 MiB as independent messages in each direction, verified pending data stayed within the directional budget, and successfully issued an unrelated HTTP request while the source was backpressured. Unit tests also exercise shared-budget exhaustion, write/connect/message timeouts, cancellation, and cleanup.

Gateway-only peak retained heap and allocation profiles still require an isolated deployment benchmark; this shared-JVM harness includes the sending and receiving applications. Measure those alongside bandwidth and latency before increasing the initial limits. In particular, declining compression can increase bandwidth even when forwarding starts sooner.
