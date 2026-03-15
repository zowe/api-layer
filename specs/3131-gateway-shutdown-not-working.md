# #3131 — The Gateway says that is shutting down but nothing happened

**GitHub Issue:** https://github.com/zowe/api-layer/issues/3131
**Labels:** bug, Priority: Medium, V3 | **Created:** 2023-10-10 | **State:** open

---

## Description

After `System.exit()` was removed from the Gateway's shutdown path (PR #3068), the Gateway logs a "shutting down" message when a shutdown signal is received but the JVM process does not actually terminate. The Gateway continues to accept and process requests indefinitely.

Spring Boot's graceful shutdown (`server.shutdown: graceful`) is not configured in the gateway, so the embedded Netty server does not wait for in-flight requests to complete and does not issue a clean shutdown sequence. Additionally, the Gateway is not deregistered from Eureka before its port closes, which can cause other services to route requests to a partially-stopped Gateway instance.

---

## Acceptance Criteria

- After receiving `SIGTERM`, the Gateway stops accepting new connections and waits up to 30 seconds for in-flight requests to complete before terminating.
- The Gateway deregisters from Eureka **before** stopping the Netty server, so other services stop routing to it immediately.
- After the graceful shutdown window, the JVM exits cleanly with exit code 0.
- An in-flight request that is already being processed when shutdown is triggered completes with a normal response (not a connection reset).
- The Eureka deregistration `SmartLifecycle` bean runs at a higher phase than the web server, ensuring deregistration happens before port close.

---

## Technical Solution

### Files to change

- `gateway-service/src/main/resources/application.yml` — add `server.shutdown: graceful`
- New: `gateway-service/src/main/java/org/zowe/apiml/gateway/lifecycle/EurekaDeregistrationLifecycle.java`
- `gateway-service/src/test/java/org/zowe/apiml/gateway/lifecycle/EurekaDeregistrationLifecycleTest.java`

### Changes

**`application.yml`**

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    # Must be shorter than Kubernetes terminationGracePeriodSeconds (or z/OS kill timeout)
    timeout-per-shutdown-phase: 30s
```

**New `EurekaDeregistrationLifecycle`**

```java
@Component
public class EurekaDeregistrationLifecycle implements SmartLifecycle {

    @Autowired
    private EurekaClient eurekaClient;

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        eurekaClient.shutdown();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Must be HIGHER than WebServerStartStopLifecycle.PHASE (Integer.MAX_VALUE - 1)
        // so this bean stops FIRST (higher phase = stops first in Spring lifecycle)
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
```

**Critical:** `getPhase()` must return `Integer.MAX_VALUE` (highest), not `Integer.MIN_VALUE` (lowest). In Spring's `SmartLifecycle` contract, **higher phase values stop first**. Eureka deregistration must occur before the web server shuts down.

### Tests

**New `EurekaDeregistrationLifecycleTest`:**
- `givenLifecycleStarted_whenStop_thenEurekaClientShutdownCalled()` — mock `EurekaClient`, call `stop()`, verify `eurekaClient.shutdown()` is called once.
- `givenPhase_thenIsHigherThanWebServerPhase()` — assert `getPhase()` returns a value strictly greater than `Integer.MAX_VALUE - 1` (the Spring web server lifecycle phase), acting as a regression guard.
- `givenStopCalledTwice_thenShutdownCalledOnlyOnce()` — idempotency: `stop()` called twice must not call `eurekaClient.shutdown()` twice.
- `givenLifecycleNotStarted_whenIsRunning_thenReturnFalse()`.

**Integration test:**
Start the Gateway, send a long-running (5-second delay) request, trigger graceful shutdown, assert:
1. The in-flight request completes with a `2xx` response (not a connection reset).
2. The Eureka registry no longer shows the Gateway instance after shutdown completes.
3. The JVM process exits with code 0 within `timeout-per-shutdown-phase + 5s`.
