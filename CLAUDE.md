# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This is the **Zowe API Mediation Layer (API ML)**, a Gradle multi-module Java monorepo (with an embedded React frontend) that provides Gateway, Discovery, and API Catalog services for exposing mainframe REST APIs. Root package for Java code is `org.zowe.apiml`.

## Build, lint, test

Requires Java 17 (IBM Semeru recommended) and Node.js/npm installed globally.

- Build everything (includes Checkstyle lint, license header check, and unit tests): `./gradlew build`
- Build a single module: `./gradlew :gateway-service:build`
- Run unit tests for one module: `./gradlew :gateway-service:test`
- Run a single test class: `./gradlew :gateway-service:test --tests org.zowe.apiml.gateway.SomeTest`
- Checkstyle only: config lives in `codequality/checkstyle`; wired in via `gradle/code-quality.gradle`.
- License headers: enforced by the `net.minecraftforge.licenser` plugin (`gradle/license.gradle`, `license_header.txt`).

### Running services locally

1. `./gradlew build`
2. `npm install -g concurrently@^6.0.0`
3. `npm run api-layer` — runs all core services together (zaas, discovery, api-catalog, discoverable-client, mock-services, caching-service, gateway).

Other useful npm scripts (see root `package.json`): `api-layer-core` (just the 4 core services), `api-layer-thin`/`*-ci` variants (lighter JARs for CI), and per-service scripts (e.g. `npm run gateway-service`, `npm run discovery-service-debug` for remote debugging). Each service reads local config from `config/local/<service>.yml`.

`npm run api-layer-modulith` runs the single-JAR `apiml` modulith (gateway + discovery + zaas + caching + catalog in one process) plus discoverable-client and mock-services, configured by `config/local/apiml-service.yml`.

By default local auth uses a mock zOSMF provider (`USER`/`validPassword`). To use dummy credentials (`user`/`user`) instead, set `apiml.security.auth.provider: dummy` in `config/local/zaas-service.yml` (or `apiml-service.yml` for the modulith) — the gateway itself doesn't own the auth provider.

The npm scripts launch built JARs from `*/build/libs`, so rebuild the module (`./gradlew :<module>:build`) after code changes before restarting. `-thin`/`-ci` variants need the lite JARs (`gradle/lite.gradle`) built first.

### Integration tests

Defined in the `integration-tests` module. See `integration-tests/README.md` for full detail. Key commands:
- Services start/stop automatically (Java/Node must be on PATH): `./gradlew runCITests`
- Against already-running services: `./gradlew runAllIntegrationTests -Dcredentials.user=<id> -Dcredentials.password=<pw>`
- Single integration test: `./gradlew :integration-tests:runAllIntegrationTests --tests org.zowe.apiml.gatewayservice.PassTicketTest`
- Slow tests (marked `@SlowTests`) run separately: `./gradlew :integration-tests:runAllIntegrationTests`
- The `integration-tests` module defines many purpose-specific `Test` tasks (`runCITests`, `runLocalIntegrationTests`, `runContainerTests`, `runContainerModulithTests`, `runStartUpCheck`, z/OS-only `runAllIntegrationTestsForZowe*TestingOnZos`, …) — list them in `integration-tests/build.gradle` rather than guessing a task name.
- Tests excluded from Zowe RC testing are annotated `@TestsNotMeantForZowe`.

### Docker

Build local Jib images then run via docker-compose, e.g.:
```
./gradlew :apiml:jibDockerBuild :api-catalog-services:jibDockerBuild :caching-service:jibDockerBuild -Pzowe.docker.debug=true
docker-compose up
```
JIB image definitions are in `gradle/jib.gradle`; each service's docker config lives under `config/docker`.

## Architecture

### Core runtime services
- **`gateway-service`** — the API Gateway; single entry point that routes/proxies requests to registered services. Built on reactive Spring Cloud Gateway (WebFlux/Netty): behavior lives in custom route predicates and `*FilterFactory` classes under `org.zowe.apiml.gateway.filters` (PassTicket, x509, zOSMF, SAF IDT, rate limiting, header sanitizing, encoded-character blocking). The auth-scheme filters delegate credential work to ZAAS rather than doing it in the gateway.
- **`discovery-service`** — Eureka-based service registry; tracks location/status of service instances.
- **`api-catalog-services`** (backend) + **`api-catalog-ui`** (React frontend, in `api-catalog-ui/frontend`) — UI for browsing discovered services and their Swagger/OpenAPI docs.
- **`zaas-service`** / **`zaas-client`** — ZAAS (Zowe Authentication and Authorization Service) and its client library. Owns authentication providers (zOSMF, mock, dummy, SAF), JWT/PassTicket/x509 handling.
- **`apiml`** — modulith that bundles gateway, discovery, zaas, caching-service and api-catalog into one Spring Boot app (`org.zowe.apiml.ApimlApplication`, built with Spring Modulith). Changes to those services' wiring often need a matching change in `apiml` (e.g. its `ModulithConfig`, security config, Eureka instance config beans), and it has its own Jib image and integration-test tasks (`runContainerModulithTests`, `*ModulithNonHaTestingOnZos`).
- **`apiml-extension-loader`** / **`apiml-sample-extension`** — mechanism for loading third-party extensions into Zowe components via an extension manifest; the sample shows the contract.
- **`caching-service`** — shared caching service used by other components.
- **`discoverable-client`** / **`mock-services`** — test-only services used for local dev and integration tests (a sample registrable service, and mocked z/OSMF).

### Shared libraries
- **`apiml-common`**, **`apiml-security-common`**, **`apiml-utility`**, **`common-service-core`**, **`security-service-client-spring`**, **`certificate-common`**, **`apiml-tomcat-common`** — cross-cutting Java libraries (Spring config, security, certs, utilities) consumed by the core services above. Core services should NOT have cross-dependencies on each other — shared logic belongs in these libraries instead.

### Onboarding enablers (SDKs)
`onboarding-enabler-{java,spring,micronaut,nodejs,python}` are client SDKs that let external services register with the API ML from different language/framework stacks. Each has a paired `*-sample-app` module demonstrating usage (used in integration tests too).

### Packaging modules
Modules suffixed `-package` (e.g. `gateway-package`, `discovery-package`, `apiml-package`) assemble the distributable Zowe component packages (used for PAX/Zowe releases), separate from the service's own build module.

### Config and certs
- `config/local/*.yml` — per-service local dev config (referenced via `--spring.config.additional-location`); `config/local-multi/` variants support running two instances of a service side by side.
- `keystore/` — local TLS certs/keystores for localhost development (see `keystore/README.md`).
- `gradle/*.gradle` — shared cross-module Gradle logic: `license.gradle`, `code-quality.gradle` (Checkstyle), `coverage.gradle`, `sonar.gradle`, `jib.gradle`, `lite.gradle` (thin/lite JAR variants used in CI), `versions.gradle` (centralized dependency versions).

### CI
GitHub Actions workflows live in `.github/workflows`; reusable workflow logic in `.github/actions` and the external `zowe-actions/shared-actions` repo. Notable workflows: `service-registration.yml` (build + unit tests + registration check), `integration-tests.yml`, `changelog.yml`, `docs.yml`. Jib container publishing (`PublishJibContainers`) must complete before any job that depends on freshly built images.

## Contribution conventions

(from `CONTRIBUTING.md` — read it in full before opening a PR)

- Commit messages follow **Conventional Commits** (`feat:`, `fix:`, `docs:`, `refactor:`, `chore:`, `!` suffix for breaking changes), and must include `Signed-off-by:`.
- Branch names: `<team-tag>/<work-tag>/<name-tag>` for GitHub issues (e.g. `rip/GH752/per_service_timeout_options`) or `private/<person-tag>/<name-tag>` for small contributions.
- Indent with 4 spaces (see `.editorconfig`).
- Tests use JUnit5, `given_when_then` naming, and `@Nested` for grouping (JUnit4 exists only in legacy tests — don't write new ones in it).
- New code needs ≥80% coverage and must not lower overall coverage; SonarCloud quality gate must pass.
- Every PR should link a GitHub issue and keep changes as small/focused as possible.
