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
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
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
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryClientLifecycleTest {

    private RecordingTransport transport;
    private RegistryClient client;
    private RegistryFetchProperties config;
    private RegistryInstanceProperties instanceConfig;
    private List<ApplicationEvent> events;

    @BeforeEach
    void setUp() {
        transport = new RecordingTransport();
        instanceConfig = new RegistryInstanceProperties();
        instanceConfig.setAppname("zaas");
        instanceConfig.setHostname("localhost");
        instanceConfig.setIpAddress("127.0.0.1");
        client = new RegistryClient(transport, SelfInstanceFactory.create(instanceConfig, 1L));

        config = new RegistryFetchProperties();
        // Long enough that no scheduled run can interleave with the assertions; start() and stop() do their
        // work synchronously, which is the point of testing them this way.
        config.setRegistryFetchIntervalSeconds(3600);
        config.setInstanceInfoReplicationIntervalSeconds(3600);
        // The heartbeat is paced by the lease, not by instance-info replication, so this is what has to be pushed
        // out for the scheduled work not to interleave.
        instanceConfig.setLeaseRenewalIntervalInSeconds(3600);

        events = new ArrayList<>();
    }

    private RegistryClientLifecycle lifecycle(HealthStatusSource healthStatusSource) {
        ApplicationEventPublisher publisher = event -> events.add((ApplicationEvent) event);
        return new RegistryClientLifecycle(client, config, instanceConfig, publisher, healthStatusSource);
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
            assertEquals(2, transport.registrations.size());
            // Registered as STARTING so nothing routes to it before it is ready, then promoted.
            assertEquals(InstanceStatus.STARTING, transport.registrations.get(0).status());
            assertEquals(InstanceStatus.UP, transport.registrations.get(1).status());
            // A health change is not an operator override - see RegistryClient.updateStatus.
            assertEquals(List.of(), transport.statusUpdates,
                "a health-driven status change must not be written as a status override");
            assertEquals(1, transport.fullFetches, "The first view must be fetched before serving requests");

            assertInstanceOf(RegistryCacheRefreshedEvent.class, events.get(0));
            // Spring Cloud's own discovery event, published on every cache refresh exactly as Eureka's client
            // did. GatewayInstanceInitializer re-resolves the Gateway on it and RouteRefreshListener rebuilds
            // the Gateway's routes, so dropping it removes both behaviours without failing to compile.
            assertInstanceOf(HeartbeatEvent.class, events.get(1));
            assertInstanceOf(RegistryRegisteredEvent.class, events.get(2));

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

            // STARTING at registration and UP when health promoted it; the second start does neither again.
            assertEquals(2, transport.registrations.size());
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

            assertEquals(2, transport.registrations.size());
            assertEquals(InstanceStatus.DOWN, transport.registrations.get(1).status());
            assertEquals(List.of(), transport.statusUpdates,
                "an unhealthy service must report its own status, not an operator override");
        }

        @Test
        @DisplayName("Then an unknown health status leaves the registered status alone")
        void thenUnknownHealthChangesNothing() {
            lifecycle(health(Status.UNKNOWN)).start();

            // Only the initial STARTING registration: an unknown status changes nothing.
            assertEquals(1, transport.registrations.size());
            assertEquals(List.of(), transport.statusUpdates);
        }

    }

    @Nested
    class GivenNoHealthCheck {

        @Test
        @DisplayName("Then the service is advertised UP once registered")
        void thenItGoesUpOnRegistration() {
            lifecycle(null).start();

            assertEquals(2, transport.registrations.size());
            assertEquals(InstanceStatus.UP, transport.registrations.get(1).status());
            assertEquals(List.of(), transport.statusUpdates);
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

            // STARTING at registration, then UP from the health status; nothing else is read or written.
            assertEquals(2, transport.registrations.size());
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
    @Test
    @DisplayName("the lease is renewed on the interval the lease is measured against")
    void heartbeatsFollowTheLeaseRenewalInterval() {
        // The API Catalog's docker configuration is the case that matters: a six second lease, renewed every
        // second. Pacing the heartbeat by instanceInfoReplicationIntervalSeconds - thirty seconds there - left the
        // Catalog registered, evicted six seconds later, and unheard from until the next replication tick. It was
        // the only service in the whole deployment that never appeared in the registry, and every integration test
        // job failed its startup check on it.
        instanceConfig.setLeaseRenewalIntervalInSeconds(1);
        config.setInstanceInfoReplicationIntervalSeconds(3600);
        config.setRegistryFetchIntervalSeconds(3600);

        var lifecycle = lifecycle(null);
        lifecycle.start();
        try {
            await().atMost(Duration.ofSeconds(15)).until(() -> {
                synchronized (transport.renewals) {
                    return transport.renewals.size() >= 2;
                }
            });
        } finally {
            lifecycle.stop();
        }

        assertTrue(transport.renewals.size() >= 2,
            "a lease that expires in a second has to be renewed well inside instanceInfoReplicationIntervalSeconds");
    }

    private static class RecordingTransport implements RegistryTransport {

        private final List<ServiceInstance> registrations = new ArrayList<>();
        private final List<InstanceStatus> statusUpdates = new ArrayList<>();
        private final List<Long> renewals = new ArrayList<>();
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
            synchronized (renewals) {
                renewals.add(System.currentTimeMillis());
            }
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
