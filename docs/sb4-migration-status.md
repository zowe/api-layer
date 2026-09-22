# Spring Boot 4.1.1 upgrade — unit-test status

**Branch:** `feat/sb4-upgrade` (from `v3.x.x`) · **Base:** `v3.x.x` · **Draft PR:** #4968 · **Issue:** #4616

**Target stack (resolved and verified):** Spring Boot **4.1.1** · Spring Framework **7.0.9** ·
Spring Security **7.1.1** · Tomcat **11.0.24** · Spring Cloud **2025.1.3** · Netflix **5.0.2** ·
Jackson **3** (shipped by SB 4.1) · bytecode target **Java 17**.

## How to read this

Compilation is green across every module, but **compilation is not the hard part of this upgrade**.
The table below is measured from actual test execution on **JDK 17** (the same JDK CI's `setup` action
defaults to). **21 of 25 modules are fully green; 4 modules have 56 failing tests.**

| module | tests | failing |
|---|---|---|
| **gateway-service** | 700 | **24** |
| **api-catalog-services** | 185 | **14** |
| **caching-service** | 327 | **13** |
| **apiml** | 451 | **5** |
| apiml-common | 179 | 0 |
| apiml-extension-loader | 13 | 0 |
| apiml-security-common | 272 | 0 |
| apiml-tomcat-common | 62 | 0 |
| apiml-utility | 88 | 0 |
| certificate-analyser | 58 | 0 |
| certificate-common | 30 | 0 |
| client-cert-auth-sample | 1 | 0 |
| common-service-core | 305 | 0 |
| discoverable-client | 90 | 0 |
| discovery-service | 159 | 0 |
| mock-services | 79 | 0 |
| onboarding-enabler-java | 59 | 0 |
| onboarding-enabler-micronaut | 2 | 0 |
| onboarding-enabler-micronaut-sample-app | 3 | 0 |
| onboarding-enabler-spring | 9 | 0 |
| onboarding-enabler-spring-sample-app | 1 | 0 |
| security-service-client-spring | 37 | 0 |
| zaas-client | 114 | 0 |
| zaas-service | 573 | 0 |
| zosmf-jwt-check | 60 | 0 |

Remaining failures are **categorised, not open-ended**:

- **A — vacuous mocks (6 tests).** `@ExtendWith(SpringExtension.class)` + `@Mock` no longer initialises
  mocks under Spring 7 / Mockito 5. Same class of defect already fixed in `mock-services`,
  `zaas-service` and others; these are the sites in the four remaining modules
  (`AttlsConfigTest` in gateway / api-catalog / caching / apiml). Known fix: `MockitoExtension`.
- **B — `HttpHeaders` no longer a `MultiValueMap` (4 tests).** Third site of this Spring 7 removal.
- **C — library calls removed `HttpHeaders.keySet()` (0 remaining).** Fixed by moving REST Assured to
  the Spring 7 line (6.0.1).
- **D — component not initialised at startup (7 tests).** `servletContainerCustomizer`,
  `TomcatKeyringFix` and `TomcatAcceptFixConfig` do not run. Suspected a conditional-on-bean flip:
  `WebServerSecurityConfig#servletContainerCustomizer` is
  `@ConditionalOnMissingBean(name = "modulithConfig")`, and SB4 changes which modulith beans register.
  **This one looks like a potential production behaviour change and is the highest-value item left.**
- **E — individual review (39 tests).** Websocket handshakes, x-forwarded-header propagation, Redis URI
  credential formatting (Lettuce 7.5), Eureka homepage 500, and `AttlsConfigTest`/`TokenControllerTest`
  TLS trust issues. No single shared cause.

## Bugs found that are NOT test-only

Three of these would have shipped as production defects:

1. **Archaius dropped from the runtime classpath.** Spring Cloud Netflix 5.0.x's BOM **excludes
   `archaius-core`**, but `eureka-core`'s `DefaultEurekaServerConfig` still initialises it statically.
   `eureka-client` 2.0.5 declared it (v3.x.x got `0.3.3 -> 0.7.6`); **2.0.6 dropped it**. Discovery
   service would fail at startup. Fixed by declaring `archaius-core` explicitly.
2. **gateway-service could not start.** `gateway-service/build.gradle` excluded
   `spring-boot-starter-reactor-netty` (a line inherited from `v3.x.x`, harmless on SB3 because gateway
   runs on Tomcat). SB4's Spring Cloud Gateway `NettyConfiguration` is active **regardless of server** and
   both `@Bean`-defines `HttpClientProperties` (injected by our routing/WebSocket code) and needs
   `NettyServerProperties`, which only exists in that starter. The configuration failed as a whole →
   no bean → `NoSuchBeanDefinitionException` → **31 of gateway's 47 failing suites came from this one
   excluded starter.** Removing it took gateway from 47 failing suites to 24 failing tests.
3. **springdoc 2.9.1 aborted ApplicationContext startup.** `org.springdoc.webflux.ui.SwaggerConfig`
   references `WebFluxProperties`, which SB4 moved. The `NoClassDefFoundError` surfaces while Spring Boot
   introspects `@ConditionalOnMissingBean` *deducing a bean type*, so it throws
   `OnBeanCondition$BeanTypeDeductionException` and kills startup for the whole module — ~80 test suites
   across 3 modules from one dependency. Fixed by moving to springdoc **3.1.1**.

## Dependencies that cannot run on this stack, and what replaced them

| dependency | why it broke | resolution |
|---|---|---|
| springdoc 2.9.1 | references SB3-moved `WebFluxProperties`; kills context startup | **3.1.1** |
| REST Assured 5.5.7 | compiled against Spring 6: calls removed `HttpHeaders.keySet()` and casts `HttpHeaders` to `MultiValueMap`. Also Groovy 4 vs the BOM's Groovy 5 (`NullPointerException` in `ClosureMetaClass`) | **6.0.1** (the Spring 7 line; declares spring 7.0.1, requires Groovy 5) |
| Groovy 4.0.32 pin | was working around REST Assured 5.5.7 | **removed** — REST Assured 6 requires Groovy 5, so the pin would hold it on an incompatible Groovy; left to the SB 4.1 BOM. Confirmed safe by `:discovery-service:test` passing on JDK 17 |
| Jersey stack | resolution drift could land on the Java-21-only `jersey-common:4.0.1` | **forced to 3.1.12** via `resolutionStrategy` |

## Java 17 requirement

`sourceCompatibility`, `targetCompatibility` and `liteJar.targetJavaVersion` are **17**, not 21.
Setting them to 21 fails twice for a Java 17 requirement: `javac 17` cannot compile it
(`invalid source release: 21`) *and* major-65 bytecode cannot be **loaded** by a Java 17 runtime. The
Jersey pin alone does not satisfy "runs on Java 17".

## Caveat on the gateway reactor-netty fix

Removing the `spring-boot-starter-reactor-netty` exclude changes the runtime classpath of a service that
runs on Tomcat. It is well-evidenced (it resolved 31 suites) and the mechanism is confirmed, but
**it should be validated by an integration run before being trusted**, not by unit tests alone.

## Not covered here

- **Integration/acceptance tests** — this document is unit tests only. `integration-tests.yml` runs
  `./gradlew clean jib` (the container-image build), which does **not** depend on `test`, so unit tests
  are not implied by a green integration job.
- **z/OS USS acceptance**, CVE pass, enabler compatibility (5 languages), user-facing migration guide.
- **Infinispan starter swap** to `spring-boot4-starter-embedded` — **check `infinispan-commons` 16.2.x:
  it is class file 65/66 (Java 21 only)**, which conflicts with the Java 17 requirement.

## Full record

Every mapping below was confirmed against the actual jar with `javap`/`unzip -l`, not inferred:
`references/sb4-migration-playbook.md` in the `apiml-architecture` skill.
