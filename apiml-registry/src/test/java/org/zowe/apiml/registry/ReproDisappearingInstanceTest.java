/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterises when the registry gives up a healthy, heartbeating instance.
 *
 * Written to test the hypothesis that the CI symptom - the API Catalog registers, starts, then disappears
 * from /eureka/apps - was self-preservation or eviction. It was not: with correct heartbeats the instance
 * survives any number of sweeps. What it does pin down is the sensitivity of that survival to the renewal
 * rate relative to the registry's own expected-client count, which is the number the Docker environment
 * makes uncomfortably small. */
class ReproDisappearingInstanceTest {

    /** Mirrors config/docker/api-catalog-services.yml: leaseExpirationDurationInSeconds: 6, renewal: 1s. */
    private static ServiceInstance catalog(long now) {
        return ServiceInstance.builder()
            .instanceId("localhost:apicatalog:10014")
            .appName("APICATALOG")
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(new PortInfo(10014, false))
            .securePort(new PortInfo(10014, true))
            .homePageUrl("https://localhost:10014/apicatalog")
            .status(InstanceStatus.UP)
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .lease(Lease.builder()
                .kind(Lease.Kind.RENEWABLE)
                .durationSecs(6)
                .renewalIntervalSecs(1)
                .registrationTimestamp(now)
                .lastRenewalTimestamp(now)
                .serviceUpTimestamp(now)
                .build())
            .build();
    }

    private static boolean present(InMemoryServiceRegistry registry) {
        return registry.application("APICATALOG").isPresent();
    }

    /**
     * A perfectly-behaving client is never evicted, however often the sweep runs. This is the negative result:
     * eviction and self-preservation are NOT the cause of the CI symptom.
     */
    @Test
    void aHeartbeatingInstanceIsNeverEvictedByTheSweep() {
        AtomicLong clock = new AtomicLong(0);
        InMemoryServiceRegistry registry =
            new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), clock::get);
        registry.openForTraffic(1);
        registry.register(catalog(0), RegistrationKind.DYNAMIC);

        List<String> evictedAt = new ArrayList<>();
        for (int second = 1; second <= 300; second++) {
            clock.set(second * 1000L);
            registry.renew("APICATALOG", "localhost:apicatalog:10014", false);
            if (second % 60 == 0) {
                registry.evict(0);
                if (!present(registry)) {
                    evictedAt.add("t=" + second + "s");
                }
            }
        }

        System.out.println("REPRO evictedAt=" + evictedAt);
        System.out.println("REPRO threshold=" + registry.renewalThresholdPerMinute()
            + " expectedClients=" + registry.expectedClientsSendingRenews());
        assertEquals(List.of(), evictedAt, "a heartbeating instance must survive every sweep");
    }

    /**
     * The survival margin, stated plainly: the threshold is
     * {@code expectedClients * (60 / expectedInterval) * 0.85}, so once the registry expects three or more
     * clients, a single renewal per minute is no longer enough to keep eviction switched on. With the shipped
     * defaults that threshold is reached as soon as three dynamic clients have ever registered - at which point
     * {@code evictionAllowed()} is decided by the registry's update from _zero_ clients, not by the one client
     * that matters.
     */
    @Test
    void thresholdScalesWithExpectedClientsNotWithObservedOnes() {
        AtomicLong clock = new AtomicLong(0);
        InMemoryServiceRegistry registry =
            new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), clock::get);

        registry.openForTraffic(1);
        assertEquals(1, registry.renewalThresholdPerMinute(),
            "one expected client, 30s interval, 85% -> (int)(1 * 2 * 0.85) = 1");

        registry.register(catalog(0), RegistrationKind.DYNAMIC);
        assertEquals(2, registry.expectedClientsSendingRenews(),
            "registering a dynamic client raises the expected count");
        assertEquals(3, registry.renewalThresholdPerMinute(),
            "(int)(2 * 2 * 0.85) = 3 - so two renewals a minute are now required from ONE client");

        // Two clients' worth of expected renewals can no longer be met by a single client renewing once.
        clock.set(1000);
        registry.renew("APICATALOG", "localhost:apicatalog:10014", false);
        assertFalse(registry.evictionAllowed(),
            "one renewal against a threshold of three suspends eviction");

        // The client is then only safe because self-preservation suspends eviction; verify the sweep is inert.
        clock.set(1000 + 6 * 60_000L);
        assertEquals(0, registry.evict(0), "suspended eviction must not remove anything");
        assertTrue(present(registry), "and the instance is still served");
    }

}
