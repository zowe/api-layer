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

4. Run (or debug) the tests, e.g. from IntelliJ, or from the command line:

   ```
   ./gradlew :integration-tests:runContainerModulithTests -Denvironment.config=-docker-modulith -Denvironment.modulith=true
   ```

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

## A note on multi-instance (HA) topologies

Some topologies run two or three instances of the same service (`apiml-2`, `discovery-service-2`,
...). Those will publish each instance's port to a different host port (the checked-in
`environment-configuration-*.yml` files only carry one port per host, so an HA topology's `-local`
compose file pairs with a small additive `environment-configuration-*-local.yml` variant with the
remapped ports - the file CI uses stays untouched). This hasn't landed yet; see the workflow's
remaining `services:`-based jobs for what's still to be migrated.
