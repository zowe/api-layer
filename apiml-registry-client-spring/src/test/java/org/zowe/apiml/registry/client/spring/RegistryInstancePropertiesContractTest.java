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

}
