# #4152 — Support of configuring new values to tweak resources / performance

**GitHub Issue:** https://github.com/zowe/api-layer/issues/4152
**Labels:** enhancement, Priority: Low | **Created:** 2025-06-05 | **State:** open

---

## Description

Some performance-related JVM and server settings can only be configured via system environment variables or JVM flags, not through APIML's standard YAML configuration. In Zowe SMP/E and containerised deployments, operators may not have a straightforward way to pass arbitrary JVM flags.

Specifically:
- Tomcat thread pool size (`server.tomcat.threads.max`, `server.tomcat.threads.min-spare`) — relevant for services using embedded Tomcat (zaas-service, api-catalog-services, discovery-service).
- Netty direct memory (`-Dio.netty.maxDirectMemory`) — relevant for gateway-service (uses Reactor Netty).

Note: gateway-service uses Reactor Netty, not Tomcat. `server.tomcat.*` properties do not apply to it.

---

## Acceptance Criteria

- Tomcat-based APIML services accept `server.tomcat.threads.max` and `server.tomcat.threads.min-spare` via `application.yml` and document their default values and recommended ranges.
- A start script (or extension of existing start scripts) reads an operator-provided JVM options file and passes them to the `java` invocation, enabling Netty direct-memory and other pre-startup JVM settings.
- The JVM options file format and location are documented.
- Setting `server.tomcat.threads.max=10` in a `@SpringBootTest` results in the Tomcat thread pool being configured with max 10 threads.

---

## Technical Solution

### Files to change

- Per-service `application.yml` files for Tomcat-based services — add documented `server.tomcat.threads.*` entries with defaults
- `bin/start.sh` (or equivalent) — add JVM options file support
- `docs/` — document new configuration options

### Changes

**`application.yml` for Tomcat services:**

```yaml
server:
  tomcat:
    threads:
      max: 200          # Maximum number of worker threads. Increase for high-throughput environments.
      min-spare: 10     # Minimum number of idle threads to keep alive.
```

These are standard Spring Boot properties already supported by embedded Tomcat — no code change required. The change is to document and expose them as first-class APIML configuration values.

**Start script extension for Netty/JVM settings:**

```bash
#!/bin/bash
# bin/start-gateway.sh

JVM_OPTIONS_FILE="${APIML_CONFIG_DIR}/jvm.options"
EXTRA_JVM_OPTS=""

if [ -f "$JVM_OPTIONS_FILE" ]; then
    while IFS= read -r line; do
        # Skip comments and blank lines
        [[ "$line" =~ ^#.*$ || -z "$line" ]] && continue
        EXTRA_JVM_OPTS="$EXTRA_JVM_OPTS $line"
    done < "$JVM_OPTIONS_FILE"
fi

exec java $EXTRA_JVM_OPTS -jar gateway-service.jar "$@"
```

Example `jvm.options` file:
```
# Netty direct memory limit
-Dio.netty.maxDirectMemory=256m
# Increase JVM heap
-Xmx2g
```

### Tests

**`@SpringBootTest` smoke test for Tomcat services:**
```java
@SpringBootTest(properties = "server.tomcat.threads.max=5")
class TomcatThreadPoolConfigTest {
    @Autowired
    private WebServerApplicationContext context;

    @Test
    void givenCustomThreadMax_whenStarted_thenTomcatUsesIt() {
        TomcatWebServer server = (TomcatWebServer) context.getWebServer();
        int maxThreads = server.getTomcat().getConnector()
            .getProtocolHandler().getExecutor()... // assert == 5
    }
}
```

**Shell script test (CI):**
Add a CI step that invokes `start-gateway.sh` with a test `jvm.options` file containing `-Dtest.marker=true` and asserts the flag appears in the Java process's system properties (e.g., via a health endpoint that exposes system properties in test mode).
