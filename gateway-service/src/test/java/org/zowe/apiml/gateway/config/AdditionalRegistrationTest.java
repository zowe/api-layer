/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.gateway.filters.proxyheaders.AdditionalRegistrationGatewayRegistry;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.client.spring.RegistryFetchProperties;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registering this Gateway into other API ML instances.
 * <p>
 * The Eureka version of these cases needed an {@code ApplicationInfoManager}, an {@code EurekaClientConfig}, a
 * request-factory supplier, transport factories, a {@code HealthCheckHandler} and a spy on the configuration
 * class before it could assert anything. What they were actually about - one client per registration, at the
 * configured address, advertising itself as an additional registration with the right routes - is what is
 * asserted here.
 */
@ExtendWith(MockitoExtension.class)
class AdditionalRegistrationTest {

    private final List<AdditionalRegistration.Route> routes =
        singletonList(new AdditionalRegistration.Route("/", "/"));

    private final AdditionalRegistration registration = AdditionalRegistration.builder()
        .discoveryServiceUrls("https://another-apiml-1:10011/eureka")
        .routes(routes)
        .build();

    private ServiceInstance primary;

    @BeforeEach
    void setUp() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.routes.0.gatewayUrl", "/api/v1");
        metadata.put("apiml.routes.0.serviceUrl", "/service/api/v1");
        metadata.put("apiml.service.title", "API Gateway");

        primary = ServiceInstance.builder()
            .instanceId("localhost:gateway:10010")
            .appName("gateway")
            .hostName("localhost")
            .port(new PortInfo(10010, false))
            .securePort(new PortInfo(10010, true))
            .status(InstanceStatus.UP)
            .metadata(metadata)
            .build();
    }

    @Nested
    class WhenBuildingTheRegistration {

        @Test
        @DisplayName("Then it is marked additional, so the receiving API ML knows it is not one of its own")
        void thenItIsMarkedAdditional() {
            var additional = GatewayRegistrationConfig.forAdditionalRegistration(primary, registration);

            assertThat(additional.metadata().get("apiml.registrationType")).isEqualTo("additional");
        }

        @Test
        @DisplayName("Then overridden routes replace the primary's, rather than adding to them")
        void thenRoutesAreReplaced() {
            var additional = GatewayRegistrationConfig.forAdditionalRegistration(primary, registration);

            assertThat(additional.metadata().get("apiml.routes.0.gatewayUrl")).isEqualTo("/");
            assertThat(additional.metadata().get("apiml.routes.0.serviceUrl")).isEqualTo("/");
            // Everything that is not a route is carried over
            assertThat(additional.metadata().get("apiml.service.title")).isEqualTo("API Gateway");
        }

        @Test
        void thenTheRoutesOfThePrimaryAreLeftAlone() {
            GatewayRegistrationConfig.forAdditionalRegistration(primary, registration);

            assertThat(primary.metadata().get("apiml.routes.0.gatewayUrl")).isEqualTo("/api/v1");
        }

        @Test
        @DisplayName("Then a registration without routes keeps the primary's")
        void thenAbsentRoutesAreInherited() {
            var noRoutes = AdditionalRegistration.builder().discoveryServiceUrls("https://another:10011").build();

            var additional = GatewayRegistrationConfig.forAdditionalRegistration(primary, noRoutes);

            assertThat(additional.metadata().get("apiml.routes.0.gatewayUrl")).isEqualTo("/api/v1");
        }

    }

    @Nested
    class WhenResolvingDiscoveryUrls {

        @Test
        void thenTheListIsSplitAndTrimmed() {
            var urls = GatewayRegistrationConfig.discoveryUrls(
                AdditionalRegistration.builder()
                    .discoveryServiceUrls("https://one:10011/eureka, https://two:10011/eureka")
                    .build(),
                true, "eureka", "password");

            assertThat(urls).containsExactly("https://one:10011/eureka", "https://two:10011/eureka");
        }

        /**
         * With TLS validation off, this Gateway's client certificate cannot be trusted by the other API ML, so
         * the credentials go into the URL and it authenticates with basic auth instead.
         */
        @Test
        void thenCredentialsAreEmbeddedWhenCertificatesAreNotVerified() {
            var urls = GatewayRegistrationConfig.discoveryUrls(registration, false, "eureka", "password");

            assertThat(urls).containsExactly("https://eureka:password@another-apiml-1:10011/eureka");
        }

        @Test
        void thenNoUrlsMeansNoRegistration() {
            assertThat(GatewayRegistrationConfig.discoveryUrls(
                AdditionalRegistration.builder().build(), true, "eureka", "password")).isEmpty();
            assertThat(GatewayRegistrationConfig.discoveryUrls(
                AdditionalRegistration.builder().discoveryServiceUrls("  ").build(), true, "eureka", "password")).isEmpty();
        }

    }

    @Nested
    class WhenTheRegistrationsAreDriven {

        private final RecordingTransport transportOne = new RecordingTransport();
        private final RecordingTransport transportTwo = new RecordingTransport();
        private final AdditionalRegistrationGatewayRegistry gatewayRegistry = new AdditionalRegistrationGatewayRegistry();

        private GatewayRegistrationConfig.AdditionalRegistrations registrations(RecordingTransport... transports) {
            org.springframework.test.util.ReflectionTestUtils.setField(
                gatewayRegistry, "registryExpiration", java.time.Duration.ofMinutes(5));
            gatewayRegistry.init();

            var config = new RegistryFetchProperties();
            config.setRegistryFetchIntervalSeconds(3600);
            config.setInstanceInfoReplicationIntervalSeconds(3600);

            var lifecycles = new ArrayList<org.zowe.apiml.registry.client.spring.RegistryClientLifecycle>();
            for (RecordingTransport transport : transports) {
                lifecycles.add(GatewayRegistrationConfig.newAdditionalRegistration(
                    transport, primary, registration, config, gatewayRegistry, event -> { }, null));
            }
            return new GatewayRegistrationConfig.AdditionalRegistrations(lifecycles);
        }

        @Test
        @DisplayName("Then nothing is registered until the container starts them")
        void thenConstructionHasNoSideEffects() {
            registrations(transportOne);

            assertThat(transportOne.registrations).isEmpty();
        }

        @Test
        void thenEachRegistrationRegistersItself() {
            var holder = registrations(transportOne, transportTwo);

            holder.start();

            assertThat(holder.count()).isEqualTo(2);
            assertThat(holder.isRunning()).isTrue();
            assertThat(transportOne.registrations).hasSize(1);
            assertThat(transportTwo.registrations).hasSize(1);
            assertThat(transportOne.registrations.get(0).metadata().get("apiml.registrationType"))
                .isEqualTo("additional");
        }

        @Test
        @DisplayName("Then shutting down cancels every lease, rather than leaving peers to expire them")
        void thenShutdownCancelsAll() {
            var holder = registrations(transportOne, transportTwo);
            holder.start();

            holder.stop();

            assertThat(holder.isRunning()).isFalse();
            assertThat(transportOne.cancellations).hasSize(1);
            assertThat(transportTwo.cancellations).hasSize(1);
        }

    }

    private static class RecordingTransport implements RegistryTransport {

        private final List<ServiceInstance> registrations = new ArrayList<>();
        private final List<String> cancellations = new ArrayList<>();

        @Override
        public Applications fetchApplications() {
            return new Applications(List.of(), 1L, Applications.computeHashCode(List.of()));
        }

        @Override
        public Applications fetchDelta() {
            return null;
        }

        @Override
        public void register(ServiceInstance instance) {
            registrations.add(instance);
        }

        @Override
        public boolean renew(String appName, String instanceId) {
            return true;
        }

        @Override
        public void cancel(String appName, String instanceId) {
            cancellations.add(appName + "/" + instanceId);
        }

        @Override
        public void updateStatus(String appName, String instanceId, InstanceStatus status) {
            // recorded elsewhere; not what these cases are about
        }

    }

}
