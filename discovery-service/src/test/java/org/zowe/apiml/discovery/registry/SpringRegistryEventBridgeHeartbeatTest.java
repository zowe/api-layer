/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.zowe.apiml.discovery.registry.event.RegistryAvailableEvent;
import org.zowe.apiml.discovery.registry.event.RegistryInstanceRegisteredEvent;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Discovery Service's side of the gateway lookup.
 * <p>
 * {@code GatewayInstanceInitializer} re-resolves the Gateway's address on Spring Cloud's {@code HeartbeatEvent}
 * and on {@code ApplicationReadyEvent}. The second of those runs before any Gateway has registered - the
 * Discovery Service is ready first - so on this service the event is the only thing that makes the lookup happen
 * at all. Nothing published it after the Eureka client was removed, which is why
 * {@code /application/eurekaversion} answered 401 and every integration job failed its startup check.
 * <p>
 * Asserting the event rather than the 401, because the event is what this class is responsible for: the address
 * resolution and the security chain behind it belong to {@code apiml-common} and would need a live Gateway.
 */
class SpringRegistryEventBridgeHeartbeatTest {

    private InMemoryServiceRegistry registry;
    private List<Object> published;

    @BeforeEach
    void setUp() {
        registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), System::currentTimeMillis);
        published = new ArrayList<>();
        SpringRegistryEventBridge bridge = new SpringRegistryEventBridge(registry, published::add);
        bridge.subscribe();
    }

    @Test
    void givenRegistration_whenItReachesTheRegistry_thenAHeartbeatIsPublished() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

        assertEquals(1, published.stream().filter(HeartbeatEvent.class::isInstance).count(),
            "the Gateway's registration is the moment its address becomes resolvable");
        assertTrue(published.stream().anyMatch(RegistryInstanceRegisteredEvent.class::isInstance));
    }

    /**
     * A Gateway brought in by a peer has to be resolvable too. The two HA nodes each hold the other's
     * registrations - including ZAAS and the second Gateway - so a heartbeat that only fired for locally
     * received registrations would leave exactly the HA jobs broken.
     */
    @Test
    void givenReplicatedRegistration_whenItReachesTheRegistry_thenAHeartbeatIsPublished() {
        registry.register(instance("gateway", 10010), RegistrationKind.REPLICATED);

        assertEquals(1, published.stream().filter(HeartbeatEvent.class::isInstance).count());
    }

    @Test
    void givenOpeningForTraffic_whenStaticRegistrationsLand_thenAHeartbeatIsPublished() {
        registry.openForTraffic(1);

        assertEquals(1, published.stream().filter(HeartbeatEvent.class::isInstance).count());
        assertTrue(published.stream().anyMatch(RegistryAvailableEvent.class::isInstance));
    }

    /**
     * Renewals are the one event deliberately not turned into a heartbeat: they arrive once per service every
     * thirty seconds, and each one would re-run the Gateway lookup and rebuild the modulith's route table.
     */
    @Test
    void givenOnlyRenewals_whenTheRegistryRuns_thenNoHeartbeatIsPublished() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        published.clear();

        registry.renew("GATEWAY", "localhost:gateway:10010", false);

        assertEquals(0, published.size());
    }

    @Test
    void givenTheFirstHeartbeat_whenConsumersCompareValues_thenEachOneIsDistinct() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        registry.register(instance("zaas", 10023), RegistrationKind.DYNAMIC);

        List<Object> values = published.stream()
            .filter(HeartbeatEvent.class::isInstance)
            .map(event -> ((HeartbeatEvent) event).getValue())
            .toList();

        assertEquals(2, values.size());
        assertEquals(2, values.stream().distinct().count(),
            "consumers distinguish one refresh from the next by the heartbeat's value");
    }

    private static ServiceInstance instance(String service, int port) {
        return ServiceInstance.builder()
            .instanceId("localhost:" + service + ":" + port)
            .appName(service.toUpperCase())
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(port, true)
            .vipAddress(service)
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, System.currentTimeMillis()))
            .build();
    }

}
