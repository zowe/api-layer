# apiml-registry

The service registry, in-sourced. This module is the replacement for the Netflix Eureka **server** — the model,
the wire codec, the in-memory registry, its policy, and peer replication.

It has **no Spring dependency**. Everything that touches the framework lives in the service that uses it
(`discovery-service` for the standalone server, `apiml` for the modulith) or in `apiml-registry-client-spring`
for the client side.

## Responsibility

| Package | Content |
|---|---|
| `codec/` | `RegistryCodec` (the wire format), `WireMapper`/`WireParsers`/`WireWriter` (JSON and XML, full and compact), `RegistryJacksonModule` (teaches an ordinary `ObjectMapper` the registry model), `WireConstants` |
| `model/` | `ServiceInstance`, `Applications`, `Application`, `Lease`, `PortInfo`, `InstanceStatus`, `DiscoveryMetadata`, `DataCenterInfo` |
| `policy/` | `SelfPreservation`, `StatusOverridePolicy` |
| `replication/` | `PeerReplicator`, `ReplicationBatch`/`Item`/`Transport`, `PeerNode`, `ReplicationResponse` |
| `spi/` | `RegistrationInterceptor` — the extension point API ML's customisations plug into |
| — | `ServiceRegistry`, `InMemoryServiceRegistry`, `RegistrySettings`, `RegistryView`, `RegistryListener` |

## Wire-contract guarantees

This module owns the bytes on the wire, and they are frozen. See
[`docs/service-registry-adr.md`](../../docs/service-registry-adr.md) for the decision and the excluded paths.

- The contract was captured from Netflix Eureka **2.0.6** with Spring Cloud Netflix **4.3.3**. The fixtures live
  in `src/test/resources/wire-contract/` and the capture tests drive *Eureka's own codecs* to prove our encoder
  still matches it — field presence, field order, the JSON/XML difference in where `metadata` falls, and the
  compact-vs-full form.
- `PortInfo` is encoded as an object (`{"$":10010,"@enabled":"true"}`) in JSON and as element text with
  attributes in XML, and the enabled flag is a **string**. This is the detail a plain bean serializer gets
  wrong; serialising these DTOs with an ordinary Jackson mapper looks like it works and silently drops the port.
- `WireConstants.DATA_CENTER_INFO_CLASS` is the literal string
  `com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo`. It stays exactly as it is: it is a value *in* the
  contract, not a dependency on the class.
- The registry model carries no Jackson annotations on purpose — the wire format is the codec's business, not
  the domain's. `RegistryJacksonModule` is how a general-purpose mapper is taught the model, and it must be
  registered wherever a registry type can end up in a response body.

## Configuration

None. This module reads no properties; the service that embeds it configures it through `RegistrySettings`.
`discovery-service` binds those from `eureka.server.*` and `eureka.instance.*`, keeping the original property
names on purpose — see the ADR's policy section.

## Tests

`./gradlew :apiml-registry:test`. The wire-contract capture tests additionally need the Eureka artifacts, which
this module declares in **test scope only**.
