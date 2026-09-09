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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryClientLifecycleTest {

    private RecordingTransport transport;
    private RegistryClient client;
    private RegistryFetchProperties config;
    private List<ApplicationEvent> events;

    @BeforeEach
    void setUp() {
        transport = new RecordingTransport();
        RegistryInstanceProperties instanceConfig = new RegistryInstanceProperties();
        instanceConfig.setAppname("zaas");
        instanceConfig.setHostname("localhost");
        instanceConfig.setIpAddress("127.0.0.1");
        client = new RegistryClient(transport, SelfInstanceFactory.create(instanceConfig, 1L));

        config = new RegistryFetchProperties();
        // Long enough that no scheduled run can interleave with the assertions; start() and stop() do their
        // work synchronously, which is the point of testing them this way.
        config.setRegistryFetchIntervalSeconds(3600);
        config.setInstanceInfoReplicationIntervalSeconds(3600);

        events = new ArrayList<>();
    }

    private RegistryClientLifecycle lifecycle(HealthStatusSource healthStatusSource) {
        ApplicationEventPublisher publisher = event -> events.add((ApplicationEvent) event);
        return new RegistryClientLifecycle(client, config, publisher, healthStatusSource);
    }

    private static HealthStatusSource health(Status status) {
        HealthIndicator indicator = () -> Health.status(status).build();
        return new HealthStatusSource(
            new SimpleStatusAggregator(),
            Map.<String, HealthContributor>of("test", indicator),
            Map.of());
    }

    @Nested
    class GivenAHealthyService {

        @Test
        @DisplayName("Then it registers, fetches once, and advertises UP")
        void thenItRegistersAndBecomesUp() {
            RegistryClientLifecycle lifecycle = lifecycle(health(Status.UP));

            lifecycle.start();

            assertTrue(lifecycle.isRunning());
            assertEquals(1, transport.registrations.size());
            // Registered as STARTING so nothing routes to it before it is ready, then promoted.
            assertEquals(InstanceStatus.STARTING, transport.registrations.get(0).status());
            assertEquals(List.of(InstanceStatus.UP), transport.statusUpdates);
            assertEquals(1, transport.fullFetches, "The first view must be fetched before serving requests");

            assertInstanceOf(RegistryRegisteredEvent.class, events.get(1));
            assertInstanceOf(RegistryCacheRefreshedEvent.class, events.get(0));

            lifecycle.stop();

            assertFalse(lifecycle.isRunning());
            // Upper-cased app name, as the registry keys applications - see ServiceInstance.appName()
            assertEquals(List.of("ZAAS/localhost:zaas:80"), transport.cancellations);
        }

        @Test
        @DisplayName("Then a second start is ignored")
        void thenStartIsIdempotent() {
            RegistryClientLifecycle lifecycle = lifecycle(health(Status.UP));

            lifecycle.start();
            lifecycle.start();

            assertEquals(1, transport.registrations.size());
        }

    }

    @Nested
    class GivenAnUnhealthyService {

        /**
         * The reason {@code eureka.client.healthcheck.enabled} exists: a process that is running but cannot
         * serve must not be advertised as available. It stays out of the routing table rather than accepting
         * requests it will fail.
         */
        @Test
        void thenItIsNotAdvertisedAsUp() {
            lifecycle(health(Status.DOWN)).start();

            assertEquals(List.of(InstanceStatus.DOWN), transport.statusUpdates);
        }

        @Test
        @DisplayName("Then an unknown health status leaves the registered status alone")
        void thenUnknownHealthChangesNothing() {
            lifecycle(health(Status.UNKNOWN)).start();

            assertEquals(List.of(), transport.statusUpdates);
        }

    }

    @Nested
    class GivenNoHealthCheck {

        @Test
        @DisplayName("Then the service is advertised UP once registered")
        void thenItGoesUpOnRegistration() {
            lifecycle(null).start();

            assertEquals(List.of(InstanceStatus.UP), transport.statusUpdates);
        }

    }

    @Nested
    class GivenRegistrationIsDisabled {

        @Test
        @DisplayName("Then it only reads, and shutdown cancels nothing")
        void thenItOnlyReads() {
            config.setRegisterWithEureka(false);
            client = new RegistryClient(transport);
            RegistryClientLifecycle lifecycle = lifecycle(null);

            lifecycle.start();
            lifecycle.stop();

            assertEquals(List.of(), transport.registrations);
            assertEquals(List.of(), transport.cancellations);
            assertEquals(1, transport.fullFetches);
        }

    }

    @Nested
    class GivenFetchingIsDisabled {

        @Test
        @DisplayName("Then nothing is read, which is what a service that only publishes itself needs")
        void thenNothingIsFetched() {
            config.setFetchRegistry(false);

            lifecycle(null).start();

            assertEquals(1, transport.registrations.size());
            assertEquals(0, transport.fullFetches);
            assertEquals(0, transport.deltaFetches);
        }

    }

    @Nested
    class GivenUnregisterOnShutdownIsOff {

        @Test
        void thenTheLeaseIsLeftToExpire() {
            config.setShouldUnregisterOnShutdown(false);
            RegistryClientLifecycle lifecycle = lifecycle(null);

            lifecycle.start();
            lifecycle.stop();

            assertEquals(List.of(), transport.cancellations);
        }

    }

    /** Records what the lifecycle asked of the registry. */
    private static class RecordingTransport implements RegistryTransport {

        private final List<ServiceInstance> registrations = new ArrayList<>();
        private final List<InstanceStatus> statusUpdates = new ArrayList<>();
        private final List<String> cancellations = new ArrayList<>();
        private int fullFetches;
        private int deltaFetches;

        @Override
        public Applications fetchApplications() {
            fullFetches++;
            return new Applications(List.of(), 1L, Applications.computeHashCode(List.of()));
        }

        @Override
        public Applications fetchDelta() {
            deltaFetches++;
            // Null means "no delta support", which sends the client to a full fetch - the same path the
            // in-JVM transport takes.
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
            statusUpdates.add(status);
        }

    }

}
