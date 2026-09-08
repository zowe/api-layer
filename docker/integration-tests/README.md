# Integration test environments (Docker Compose)

Each subdirectory here is a self-contained `docker-compose.yml` for one integration-test
topology - the same file is used by CI (`.github/workflows/integration-tests.yml`) and by a
developer running the tests locally, so a failure that reproduces on your machine is running
through the exact same containers as the CI job.

This replaces GitHub Actions' native `container:`/`services:` orchestration, which is hard to
troubleshoot (no `logs`/`exec`/`ps`, containers vanish the moment the job ends). Everything here
is driven with plain `docker compose`.

| Directory   | CI job(s)                    | Topology                                             |
|-------------|-------------------------------|-------------------------------------------------------|
| `modulith/` | `CITestsModulith`             | Single `apiml` (gateway+discovery+catalog+caching+zaas bundled), `discoverable-client`, `mock-services` |
| `split/`    | `CITests`                     | One container per service, plus a second discovery/gateway/zaas trio for multi-tenancy tests |

More topologies (HA, SAF provider, chaotic, etc.) will be added the same way as they're migrated.

## Running a test locally

1. Generate certificates and build the images you need (only once, or after pulling service
   code changes):

   ```
   ./gradlew generateCertificates
   ./gradlew :apiml:jibDockerBuild :discoverable-client:jibDockerBuild :mock-services:jibDockerBuild -Pzowe.docker.tag=local
   ```

   (swap in whichever services the topology you're running needs - see that topology's
   `docker-compose.yml`). This bakes your freshly generated certificates into the images, so
   the test client and the containers trust the same CA.

   `jibDockerBuild` automatically targets your machine's own architecture (see
   `gradle/jib.gradle`), so this produces a native image on Apple Silicon too - no emulation,
   no extra flags needed. Only `jibDockerBuild` is affected; `jib`/`jibBuildTar` (used by CI to
   publish images) are untouched and always build `amd64`, matching the GitHub Actions runners
   that consume them.

2. One-time per machine: alias the container hostnames to `127.0.0.1` so a process running on
   your host (Gradle, IntelliJ) can reach them by the same hostnames the containers use to talk
   to each other. Requires admin/sudo since it edits your hosts file; safe to re-run.

   ```
   ./docker/integration-tests/setup-hosts.sh add apiml discoverable-client mock-services
   ```

   (use the hostnames for the topology you're running - see the "hosts" list passed to
   `compose-integration-env` for that job in the workflow, or just the service names in the
   compose file). Remove them later with `./docker/integration-tests/setup-hosts.sh remove`.

   On Windows, add the same `127.0.0.1 <hostname>` lines to
   `C:\Windows\System32\drivers\etc\hosts` (as Administrator).

3. Start the stack:

   ```
   cd docker/integration-tests/modulith   # or split/
   docker compose up -d
   ```

4. Run (or debug) the tests. From the command line, this is the same task CI runs:

   ```
   ./gradlew :integration-tests:runContainerTests -Denvironment.config=-docker
   ```

   From IntelliJ, either:

   - **Run the whole Gradle task**: Gradle tool window → `integration-tests` → `Tasks` →
     `integration tests` → double-click `runContainerTests` (or `runContainerModulithTests` /
     `runStartUpCheck` / whichever task the job you're reproducing runs - check its `run:` step
     in `.github/workflows/integration-tests.yml`). Gradle prompts for missing `-D` properties;
     add them under Run/Debug Configurations → the Gradle task's own "Arguments" field, e.g.
     `-Denvironment.config=-docker -DcentralGateway.instances=0 -DcentralHosts=discovery-service-2,gateway-service-2,zaas-service-2`
     (copy the exact flags from the workflow step you're reproducing).
   - **Run/debug a single test class**, e.g. `org.zowe.apiml.startup.ApiMediationLayerStartTest`
     (what `runStartUpCheck` actually runs) or any class under `integration-tests/src/test/java`:
     right-click the class → Run/Debug, then edit the generated run configuration's "VM options"
     to add the same `-D` flags (at minimum `-Denvironment.config=-docker`, plus `-Denvironment.modulith=true`
     for a modulith topology). Debugging this way gives you real breakpoints in the test code,
     stepping through exactly what failed in CI.
   - To **also** step into a service while a test runs against it, add a second "Remote JVM
     Debug" configuration pointed at that service's port (see below) and start it before/during
     the test run - both debugger sessions can be active at once.

5. When done: `docker compose down -v`.

## Debugging into a running service from IntelliJ

Every service image already has a fixed remote-debug (JDWP) port baked in (see each
`build.gradle`'s `debugPort`), published by the compose file to the same port on `127.0.0.1`
(e.g. `apiml` &rarr; `5130`, `gateway-service` &rarr; `5130`, `zaas-service` &rarr; `5120`,
`discovery-service` &rarr; `5121`, `discoverable-client` &rarr; `5122`, `mock-services` &rarr;
`5123`, `api-catalog-services` &rarr; `5124`, `caching-service` &rarr; `5126`). Create a "Remote
JVM Debug" run configuration in IntelliJ pointed at `localhost:<port>` and attach - no extra
setup needed.

## Why hostname/SAN validation isn't affected

TLS hostname verification (SNI + certificate SAN checking) is based on the hostname string used
to make the request (e.g. `gateway-service`), not on how that string resolved to an IP. Whether
`gateway-service` resolves via Docker's internal DNS (container-to-container) or via the hosts
file alias above (host-to-container), the client sends the same SNI and checks the same SAN, and
the certificate content is unchanged (it's generated once by `./gradlew generateCertificates` and
baked into every image the same way it always was). Negative/unknown-hostname test scenarios
(e.g. `CITestsModulithUnknownHostnames`) are entirely container-to-container and are unaffected
either way.

## A note on multi-instance (HA / multi-tenancy) topologies

Some topologies run a second instance of a service on the *same* real port as the primary
(`discovery-service` / `discovery-service-2`, both listening on `10011` internally - the
checked-in `environment-configuration-*.yml` files always used one `port` value per service,
regardless of how many hosts share it). Aliasing both hostnames to plain `127.0.0.1` made the
second instance's hostname silently resolve to the *first* instance's published port instead of
failing outright - which is exactly what caused `CITests` to fail in CI.

Publishing the second instance to a different **loopback address** (e.g. `127.0.0.2`) instead of
a different port seemed like the fix, but doesn't actually work: Docker Desktop for both macOS
and Windows can only publish a container port to `127.0.0.1` or `0.0.0.0`, not other loopback
addresses ([moby/moby#33088](https://github.com/moby/moby/issues/33088),
[docker/for-mac#4607](https://github.com/docker/for-mac/issues/4607)) - confirmed by hand on this
project. WSL2 (real Linux underneath) doesn't have that limitation, but plain macOS/Windows do.

The fix that works everywhere: publish the second instance to a **different host port** instead
(`split/docker-compose.yml` maps `discovery-service-2`'s real, unchanged internal port `10011` to
host port `10021`), and teach `ApiMediationLayerStartupChecker` to connect on a different port
than the one it expects the instance to self-report as its identity in Eureka
(`Instance.connectPort` vs `Instance.port` - see the class for details). `discoveryServiceConfiguration.additionalPort`
already existed in the config schema for exactly this but was previously unused by the checker.
Since changing that value affects every job still sharing `environment-configuration-docker.yml`
(most haven't been migrated off native `services:` yet, where the distinction doesn't apply),
`CITests` uses its own `environment-configuration-docker-compose.yml` instead - an exact copy
with `additionalPort: 10021` - leaving the shared file untouched for everyone else.
