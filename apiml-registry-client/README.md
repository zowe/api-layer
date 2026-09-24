# apiml-registry-client

The client half of the in-sourced registry: enough to register a service, keep its lease alive, and read the
registry — without Spring and without Netflix.

It replaces `com.netflix.discovery.DiscoveryClient` for APIML's own services. It is **deliberately passive**: it
owns no threads and starts no timers. Whatever drives it (a scheduler, a test, an application lifecycle) supplies
the cadence, which is what makes registration and heartbeats testable without sleeping. The Spring lifecycle that
drives it in production is in `apiml-registry-client-spring`.

## Responsibility

| Type | Content |
|---|---|
| `RegistryTransport` | What the client needs from a registry: fetch applications, fetch delta, register, renew, cancel, update status |
| `HttpRegistryTransport` | The HTTP implementation, against `eureka.client.serviceUrl` |
| `LocalRegistryTransport` | The in-JVM implementation, for a process that *is* the registry (the modulith) |
| `RegistryClient` | Register, renew, cancel, refresh, and the self-registration record |
| `RegistryCache` | The last known view, and the failures since it was last refreshed |
| `CachedRegistryDiscoveryClient` | Spring Cloud's `DiscoveryClient` interface over `RegistryCache` |
| `RegistryServiceInstance` | Spring Cloud's `ServiceInstance` over the registry model |

## Wire-contract guarantees

- It speaks the frozen Eureka protocol as a *client*: `POST /eureka/apps/{app}`, `PUT`/`DELETE` for renewals,
  `GET /eureka/apps` and `/apps/delta`, `PUT /eureka/apps/{app}/{id}/status`. Nothing here changes the bytes on
  the wire; the encoder is `apiml-registry`'s `RegistryCodec`, so there is one implementation of the format, not
  two that can drift.
- A `404` on a renewal is not an error: it is the registry saying "I have never seen that instance", and the
  client treats it as the signal to register again.
- **A failed fetch keeps the last known view.** A registry the client cannot reach must not look like a registry
  that is empty — that would take a working service out of rotation. `consecutiveFetchFailures()` exposes how
  stale the view is, and the caller decides what to do about it.

## Configuration

None. This module reads no properties; `apiml-registry-client-spring` binds them. See that module's README.
