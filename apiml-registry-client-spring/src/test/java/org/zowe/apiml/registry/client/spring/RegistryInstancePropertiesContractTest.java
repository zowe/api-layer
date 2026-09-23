/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that replacing the Eureka client did not change how a service describes itself.
 * <p>
 * The {@code eureka.instance.*} properties are a configuration contract with every existing installation and
 * every onboarded service, so this test binds real service configuration into Netflix's
 * {@code EurekaInstanceConfigBean} and into {@link RegistryInstanceProperties}, builds a registration from
 * each, and compares them field by field. Reasoning about Spring's relaxed binding - particularly what it does
 * to a YAML list nested inside a {@code Map<String, String>} - is not good enough here; a difference in one
 * metadata key is the difference between the API Catalog showing a service's documentation and not.
 * <p>
 * Eureka is a test-only dependency of this module, present for exactly this comparison. When the enablers are
 * done and Eureka leaves the build entirely, this test becomes a set of golden assertions on the expected
 * values, in the same way the wire-contract corpus did.
 */
class RegistryInstancePropertiesContractTest {

    private static Binder binderFor(String resource) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
            .load(resource, new ClassPathResource(resource));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        return Binder.get(environment);
    }

    private static EurekaInstanceConfigBean netflixConfig(Binder binder) {
        EurekaInstanceConfigBean bean = new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));
        binder.bind("eureka.instance", org.springframework.boot.context.properties.bind.Bindable.ofInstance(bean));
        return bean;
    }

    private static RegistryInstanceProperties ourConfig(Binder binder) {
        RegistryInstanceProperties bean = new RegistryInstanceProperties();
        binder.bind("eureka.instance", org.springframework.boot.context.properties.bind.Bindable.ofInstance(bean));
        return bean;
    }

    private static void assertRegistrationsMatch(String resource) throws IOException {
        Binder binder = binderFor(resource);
        EurekaInstanceConfigBean netflix = netflixConfig(binder);
        RegistryInstanceProperties ours = ourConfig(binder);

        // The appname is supplied by Spring Cloud from spring.application.name rather than bound; the
        // autoconfiguration does the same thing, so it is set identically here to keep the comparison honest.
        netflix.setAppname("zaas");
        netflix.setVirtualHostName("zaas");
        netflix.setSecureVirtualHostName("zaas");
        RegistryInstanceDefaults.apply(ours, "zaas", new InetUtils(new InetUtilsProperties()));

        assertEquals(netflix.getMetadataMap(), ours.getMetadataMap(),
            "The flattened metadata keys must be identical - this is the whole registration payload");

        InstanceInfo expected = new InstanceInfoFactory().create(netflix);
        ServiceInstance actual = SelfInstanceFactory.create(ours, 1_700_000_000_000L);

        assertEquals(expected.getAppName(), actual.appName());
        assertEquals(expected.getInstanceId(), actual.instanceId());
        assertEquals(expected.getHostName(), actual.hostName());
        assertEquals(expected.getIPAddr(), actual.ipAddr());
        assertEquals(expected.getPort(), actual.port().port());
        assertEquals(expected.isPortEnabled(InstanceInfo.PortType.UNSECURE), actual.port().enabled());
        assertEquals(expected.getSecurePort(), actual.securePort().port());
        assertEquals(expected.isPortEnabled(InstanceInfo.PortType.SECURE), actual.securePort().enabled());
        assertEquals(expected.getHomePageUrl(), actual.homePageUrl());
        assertEquals(expected.getStatusPageUrl(), actual.statusPageUrl());
        assertEquals(expected.getHealthCheckUrl(), actual.healthCheckUrl());
        assertEquals(expected.getSecureHealthCheckUrl(), actual.secureHealthCheckUrl());
        assertEquals(expected.getVIPAddress(), actual.vipAddress());
        assertEquals(expected.getSecureVipAddress(), actual.secureVipAddress());
        assertEquals(expected.getStatus().name(), actual.status().name());
        assertEquals(expected.getLeaseInfo().getRenewalIntervalInSecs(), actual.lease().renewalIntervalSecs());
        assertEquals(expected.getLeaseInfo().getDurationInSecs(), actual.lease().durationSecs());
        assertEquals(expected.getMetadata(), actual.metadata());
    }

    @Nested
    class GivenRealServiceConfiguration {

        @Test
        @DisplayName("ZAAS registers identically")
        void zaasRegistrationIsUnchanged() throws IOException {
            assertRegistrationsMatch("zaas-instance.yml");
        }

        @Test
        @DisplayName("The API Catalog registers identically, relative health-check path and all")
        void catalogRegistrationIsUnchanged() throws IOException {
            assertRegistrationsMatch("catalog-instance.yml");
        }

    }

    @Nested
    class GivenAListInsideTheMetadataMap {

        /**
         * Worth pinning down, because everything downstream depends on it and it is not obvious. A YAML list
         * under a {@code Map<String, String>} is flattened by Spring's binder to dot-and-index keys -
         * {@code apiml.apiInfo.0.apiId}, not {@code apiml.apiInfo[0].apiId}. {@code EurekaMetadataParser}
         * splits metadata keys on {@code .} and ignores anything that does not produce at least four
         * segments, so the bracket form would silently drop every service's API documentation. It is only
         * correct because the property is declared with the same type at the same depth as Netflix's.
         */
        @Test
        void thenKeysAreFlattenedWithDottedIndexes() throws IOException {
            RegistryInstanceProperties ours = ourConfig(binderFor("zaas-instance.yml"));

            assertEquals("zowe.apiml.zaas", ours.getMetadataMap().get("apiml.apiInfo.0.apiId"));
            assertEquals("/api/v1", ours.getMetadataMap().get("apiml.routes.api_v1.gatewayUrl"));
            assertTrue(ours.getMetadataMap().keySet().stream().noneMatch(key -> key.contains("[")),
                "Bracketed index keys would not be parsed by EurekaMetadataParser");
        }

    }

    @Nested
    class GivenTheApiCatalogConfiguration {

        /**
         * {@code eureka.instance.port} has no setter on {@code EurekaInstanceConfigBean}, so it has never
         * bound to anything. Honouring it now would change the port the Catalog advertises, which is why
         * {@link RegistryInstanceProperties} does not have the property either.
         */
        @Test
        void thenTheDeadPortPropertyStaysDead() throws IOException {
            RegistryInstanceProperties ours = ourConfig(binderFor("catalog-instance.yml"));

            assertEquals(80, ours.getNonSecurePort(), "eureka.instance.port must not bind, as before");
            assertEquals(10014, ours.getSecurePort());
        }

    }

    @Nested
    class GivenPortsAndPaths {

        @Test
        @DisplayName("A relative health-check path yields no https URL when the secure port is off")
        void thenNoSecureHealthCheckUrlIsInvented() {
            RegistryInstanceProperties config = new RegistryInstanceProperties();
            config.setHostname("localhost");
            config.setHealthCheckUrlPath("/application/health");
            config.setSecurePortEnabled(false);

            assertNull(config.resolvedSecureHealthCheckUrl());
            assertEquals("http://localhost:80/application/health", config.resolvedHealthCheckUrl());

            // ... and none at all when neither port is enabled, which is the API Catalog's case for http
            config.setNonSecurePortEnabled(false);
            assertNull(config.resolvedHealthCheckUrl());
            // The home page keeps its relative fallback regardless - only the health-check URLs are guarded
            config.setHomePageUrlPath("/apicatalog");
            assertEquals("http://localhost:80/apicatalog", config.resolvedHomePageUrl());
        }

        @Test
        void thenPreferIpAddressChangesTheAdvertisedHost() {
            RegistryInstanceProperties config = new RegistryInstanceProperties();
            config.setHostname("zos.example.com");
            config.setIpAddress("10.1.2.3");

            assertEquals("zos.example.com", config.resolvedHostname());
            config.setPreferIpAddress(true);
            assertEquals("10.1.2.3", config.resolvedHostname());
        }

        @Test
        @DisplayName("An instance is STARTING until it reports healthy")
        void thenTheInitialStatusKeepsTrafficAway() {
            RegistryInstanceProperties config = new RegistryInstanceProperties();
            config.setHostname("localhost");
            config.setAppname("zaas");

            assertEquals("STARTING", SelfInstanceFactory.create(config, 1L).status().name());

            config.setInstanceEnabledOnit(true);
            assertEquals("UP", SelfInstanceFactory.create(config, 1L).status().name());
        }

        @Test
        void thenAnAbsentInstanceIdFallsBackToHostAppPort() {
            RegistryInstanceProperties config = new RegistryInstanceProperties();
            config.setHostname("localhost");
            config.setAppname("zaas");
            config.setSecurePortEnabled(true);
            config.setSecurePort(10023);

            assertEquals("localhost:zaas:10023", SelfInstanceFactory.create(config, 1L).instanceId());
        }

    }

    /**
     * The Gateway advertises {@code apiml.service.externalUrl} rather than the address it binds to. Netflix's
     * factory could not do that, so ConnectionsConfig carried its own 90-line copy of
     * {@code InstanceInfoFactory.create} to override the host, the port and every URL. This asserts the
     * replacement produces the same registration that copy did, so the copy can be deleted.
     */
    @Nested
    class GivenAnExternalUrl {

        private ServiceInstance advertised() throws IOException {
            RegistryInstanceProperties config = ourConfig(binderFor("gateway-instance.yml"));
            RegistryInstanceDefaults.apply(config, "gateway", new InetUtils(new InetUtilsProperties()));
            return SelfInstanceFactory.create(config, 1_700_000_000_000L, "https://apiml.example.com:443");
        }

        @Test
        void thenHostPortAndUrlsAllComeFromIt() throws IOException {
            ServiceInstance instance = advertised();

            assertEquals("apiml.example.com", instance.hostName());
            assertEquals(443, instance.port().port());
            assertEquals(443, instance.securePort().port());
            assertEquals("https://apiml.example.com:443/", instance.homePageUrl());
            assertEquals("https://apiml.example.com:443/application/info", instance.statusPageUrl());
            assertEquals("https://apiml.example.com:443/application/health", instance.secureHealthCheckUrl());
            // The non-secure port is disabled for the Gateway, so no plain health-check URL is advertised
            assertNull(instance.healthCheckUrl());
        }

        @Test
        @DisplayName("Then metadata URLs pointing at the bound address are rewritten")
        void thenMetadataIsRewritten() throws IOException {
            ServiceInstance instance = advertised();

            assertEquals(
                "https://apiml.example.com:443/gateway/api-docs",
                instance.metadata().get("apiml.apiInfo.0.swaggerUrl"),
                "A swaggerUrl on an address clients cannot reach is a broken API Catalog entry");
            // Not rewritten, and deliberately so: the replacement is of the home page URL *with* its trailing
            // slash, which is what Netflix's factory replaced, and this value has none. It matters little in
            // practice - when an operator sets apiml.service.externalUrl the metadata already holds the
            // external address - but a substring rule that fired here would also fire inside longer URLs.
            assertEquals("https://localhost:10010", instance.metadata().get("apiml.service.externalUrl"));
            // Values that are not URLs are untouched
            assertEquals("primary", instance.metadata().get("apiml.registrationType"));
            assertEquals("/gateway/api/v1", instance.metadata().get("apiml.apiBasePath"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalidUrl", "localhost:10010", "https://apiml.example.com", "/relative"})
        @DisplayName("Then anything that is not a full URL is rejected rather than registered")
        void thenAMalformedExternalUrlIsRejected(String externalUrl) throws IOException {
            RegistryInstanceProperties config = ourConfig(binderFor("gateway-instance.yml"));
            RegistryInstanceDefaults.apply(config, "gateway", new InetUtils(new InetUtilsProperties()));

            var e = assertThrows(IllegalArgumentException.class,
                () -> SelfInstanceFactory.create(config, 1L, externalUrl));
            assertTrue(e.getMessage().contains("apiml.service.externalUrl"));
        }

        @Test
        void thenTheInstanceIdIsLeftAlone() throws IOException {
            // It identifies the registration, not the address; changing it would orphan the previous lease.
            assertEquals("localhost:gateway:10010", advertised().instanceId());
        }

    }

    @Nested
    class GivenClientProperties {

        @Test
        void thenTheDefaultZoneIsSplitIntoUrls() throws IOException {
            RegistryFetchProperties config = new RegistryFetchProperties();
            binderFor("catalog-instance.yml")
                .bind("eureka.client", org.springframework.boot.context.properties.bind.Bindable.ofInstance(config));

            assertEquals(
                List.of("https://localhost:10011/eureka/", "https://localhost:10021/eureka/"),
                config.discoveryServiceUrls());
        }

        @Test
        void thenApimlDefaultsAreCarriedOver() throws IOException {
            RegistryFetchProperties config = new RegistryFetchProperties();
            binderFor("zaas-instance.yml")
                .bind("eureka.client", org.springframework.boot.context.properties.bind.Bindable.ofInstance(config));

            assertTrue(config.isFetchRegistry());
            assertTrue(config.isRegisterWithEureka());
            assertTrue(config.getHealthcheck().isEnabled());
            assertEquals(30, config.getRegistryFetchIntervalSeconds());
            assertEquals(30, config.getInstanceInfoReplicationIntervalSeconds());
        }

        @Test
        void thenHealthcheckIsOffByDefault() {
            assertFalse(new RegistryFetchProperties().getHealthcheck().isEnabled());
        }

    }

    /**
     * The ports a service advertises when its configuration does not spell them out.
     * <p>
     * Spring Cloud's {@code EurekaClientAutoConfiguration} set both from {@code server.port} on the instance
     * config bean, and Spring then bound {@code eureka.instance.*} over the top. So a service that configured a
     * port kept it, and a service that did not still registered the port it actually listens on. ZAAS is the
     * only API ML service in the second group - its YAML says the ports are computed in code - and dropping the
     * derivation registered it on 80 and 443 while it listened on 10023.
     * <p>
     * That is not cosmetic. Nothing downstream could resolve ZAAS from the registry, and the Discovery Service
     * authenticates {@code /application/**} through ZAAS, so the Discovery Service's own
     * {@code eurekaversion} endpoint answered 401 and every integration test job failed its startup check
     * waiting for a registry version it could never read.
     */
    @Nested
    class GivenThePortsAreNotConfigured {

        /**
         * Builds the registration the way the autoconfiguration does, from a real service configuration.
         */
        private RegistryInstanceProperties asRegistered(String resource, String applicationName,
                                                        int serverPort) throws IOException {
            StandardEnvironment environment = new StandardEnvironment();
            new YamlPropertySourceLoader().load(resource, new ClassPathResource(resource))
                .forEach(source -> environment.getPropertySources().addFirst(source));
            environment.getPropertySources().addFirst(
                new org.springframework.core.env.MapPropertySource("testServerPort",
                    java.util.Map.of("server.port", String.valueOf(serverPort))));

            // The order the autoconfiguration uses: the ports come from server.port first, then the configured
            // eureka.instance.* values are bound over them, so a configured port always wins.
            RegistryInstanceProperties config = new RegistryInstanceProperties();
            RegistryInstanceDefaults.applyPorts(config, environment);
            Binder.get(environment).bind("eureka.instance",
                org.springframework.boot.context.properties.bind.Bindable.ofInstance(config));
            // null InetUtils: these assertions are about the ports, not the host, and the fixtures set one
            RegistryInstanceDefaults.apply(config, applicationName, null);
            return config;
        }

        @Test
        @DisplayName("ZAAS advertises the port it listens on, as both its secure and its plain port")
        void thenZaasAdvertisesItsRealPort() throws IOException {
            RegistryInstanceProperties config = asRegistered("zaas-instance.yml", "zaas", 10023);

            assertEquals(10023, config.getNonSecurePort());
            assertEquals(10023, config.getSecurePort());

            ServiceInstance instance = SelfInstanceFactory.create(config, 1L);
            assertEquals(10023, instance.port().port());
            assertEquals(10023, instance.securePort().port());
            assertEquals("localhost:zaas:10023", instance.instanceId());
        }

        @Test
        @DisplayName("ZAAS registers on the port it serves, not on the ports of a service that never ran")
        void thenZaasIsReachableFromTheRegistry() throws IOException {
            RegistryInstanceProperties config = asRegistered("zaas-instance.yml", "zaas", 10023);
            ServiceInstance instance = SelfInstanceFactory.create(config, 1L);

            // The URLs the registry hands to every consumer, which resolved to 80 and 443 before.
            assertTrue(instance.homePageUrl().contains(":10023"), instance.homePageUrl());
            assertTrue(instance.statusPageUrl().contains(":10023"), instance.statusPageUrl());
            assertTrue(instance.secureHealthCheckUrl().contains(":10023"), instance.secureHealthCheckUrl());
        }

        @Test
        @DisplayName("The API Catalog keeps the port it configured")
        void thenAnExplicitSecurePortStillWins() throws IOException {
            // The Catalog sets eureka.instance.securePort: ${apiml.service.port}, so binding must beat the
            // server.port default - exactly as it did when Netflix's bean was bound after its construction.
            RegistryInstanceProperties config = asRegistered("catalog-instance.yml", "apicatalog", 19999);

            assertEquals(10014, config.getSecurePort());
            assertFalse(config.isNonSecurePortEnabled());
            // ... and the port it did not configure still comes from server.port rather than staying at 80.
            assertEquals(19999, config.getNonSecurePort());
        }

        @Test
        @DisplayName("The Gateway keeps the port it configured")
        void thenTheGatewayKeepsItsPort() throws IOException {
            RegistryInstanceProperties config = asRegistered("gateway-instance.yml", "gateway", 19999);

            assertEquals(10010, config.getSecurePort());
            assertEquals(10010, config.getNonSecurePort());
        }

    }

}
