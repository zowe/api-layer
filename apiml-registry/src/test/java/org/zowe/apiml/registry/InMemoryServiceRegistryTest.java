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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.model.ActionType;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavioural tests for the registry core, written against the table in
 * TASK-Discovery-Native-Registry.md section 4.3 - the APIML deviations from stock Eureka - plus the
 * self-preservation arithmetic taken from Eureka 2.0.6.
 * <p>
 * Time is injected rather than slept on, so lease expiry and the renewal window can be driven exactly.
 */
class InMemoryServiceRegistryTest {

    private static final long T0 = 1_700_000_000_000L;

    private long now;
    private InMemoryServiceRegistry registry;
    private List<RegistryEvent> events;

    @BeforeEach
    void setUp() {
        now = T0;
        events = new ArrayList<>();
        registry = newRegistry(RegistrySettings.defaults(), List.of());
    }

    private InMemoryServiceRegistry newRegistry(RegistrySettings config, List<RegistrationInterceptor> interceptors) {
        InMemoryServiceRegistry created = new InMemoryServiceRegistry(config, interceptors, () -> now);
        created.addListener(events::add);
        return created;
    }

    private ServiceInstance instance(String service, int port) {
        return ServiceInstance.builder()
            .instanceId("localhost:" + service + ":" + port)
            .appName(service.toUpperCase())
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(port, true)
            .vipAddress(service)
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, now))
            .build();
    }

    // -----------------------------------------------------------------------------------------------------
    // Basics
    // -----------------------------------------------------------------------------------------------------

    @Test
    void registersAndReadsBackAnInstance() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

        assertEquals(1, registry.size());
        assertTrue(registry.instance("GATEWAY", "localhost:gateway:10010").isPresent());
        // Lookup is case-insensitive on the app name because the Java enabler upper-cases it on the wire while
        // configuration and metadata use lower case.
        assertTrue(registry.instance("gateway", "localhost:gateway:10010").isPresent());
    }

    @Test
    @DisplayName("renewing an unknown instance reports false, which is the client's signal to re-register")
    void renewingAnUnknownInstanceReportsFalse() {
        assertFalse(registry.renew("NOPE", "localhost:nope:1", false));
    }

    @Test
    void cancellingRemovesTheInstanceAndTheEmptyApplication() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        assertTrue(registry.cancel("GATEWAY", "localhost:gateway:10010", false));

        assertEquals(0, registry.size());
        assertTrue(registry.application("GATEWAY").isEmpty());
        assertFalse(registry.cancel("GATEWAY", "localhost:gateway:10010", false));
    }

    @Test
    void carriesServiceUpTimestampAcrossAReRegistration() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        long firstServiceUp = registry.instance("GATEWAY", "localhost:gateway:10010")
            .orElseThrow().lease().serviceUpTimestamp();

        now += 60_000;
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

        ServiceInstance after = registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow();
        assertEquals(firstServiceUp, after.lease().serviceUpTimestamp(),
            "re-registration must not reset how long the service has been up");
        assertNotEquals(firstServiceUp, after.lease().registrationTimestamp());
    }

    // -----------------------------------------------------------------------------------------------------
    // Static registration - section 4.3
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class StaticRegistrations {

        @Test
        void neverExpireHoweverLongTheyAreSilent() {
            registry.register(instance("staticclient", 10013), RegistrationKind.STATIC);
            registry.openForTraffic(0);

            now += 100L * 365 * 24 * 60 * 60 * 1000; // a century
            assertEquals(0, registry.evict(0), "a static registration must survive indefinitely without heartbeats");
            assertEquals(1, registry.size());
        }

        @Test
        void doNotRaiseTheExpectedRenewalRate() {
            registry.openForTraffic(4);
            int before = registry.expectedClientsSendingRenews();

            registry.register(instance("staticclient", 10013), RegistrationKind.STATIC);

            assertEquals(before, registry.expectedClientsSendingRenews(),
                "counting a service that never heartbeats would drag the observed rate below the threshold "
                    + "and suspend eviction for every real service");
        }

        @Test
        void dynamicRegistrationsDoRaiseIt() {
            registry.openForTraffic(4);
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            assertEquals(5, registry.expectedClientsSendingRenews());
        }

        @Test
        void cancellingAStaticRegistrationDoesNotLowerTheRate() {
            registry.openForTraffic(4);
            registry.register(instance("staticclient", 10013), RegistrationKind.STATIC);
            registry.cancel("STATICCLIENT", "localhost:staticclient:10013", false);
            assertEquals(4, registry.expectedClientsSendingRenews());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Status override rules
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class StatusOverrides {

        @Test
        @DisplayName("a registrant reporting itself DOWN is believed")
        void badNewsFromTheInstanceIsBelieved() {
            registry.register(instance("gateway", 10010).toBuilder().status(InstanceStatus.UP).build(),
                RegistrationKind.DYNAMIC);
            registry.register(instance("gateway", 10010).toBuilder().status(InstanceStatus.DOWN).build(),
                RegistrationKind.DYNAMIC);

            assertEquals(InstanceStatus.DOWN,
                registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow().status());
        }

        @Test
        @DisplayName("a stored UP beats a re-registration also claiming UP, so an operator's view is not lost")
        void anExistingUpBeatsAFreshClaim() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            registry.overrideStatus("GATEWAY", "localhost:gateway:10010", InstanceStatus.OUT_OF_SERVICE, false);

            // The service restarts and cheerfully announces UP again.
            registry.register(instance("gateway", 10010).toBuilder().status(InstanceStatus.UP).build(),
                RegistrationKind.DYNAMIC);

            ServiceInstance stored = registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow();
            assertEquals(InstanceStatus.OUT_OF_SERVICE, stored.status(),
                "the operator's decision must outlive a service restart");
            assertEquals(InstanceStatus.OUT_OF_SERVICE, stored.overriddenStatus());
        }

        @Test
        void anOverrideSurvivesCancellationAndReappearance() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            registry.overrideStatus("GATEWAY", "localhost:gateway:10010", InstanceStatus.OUT_OF_SERVICE, false);
            registry.cancel("GATEWAY", "localhost:gateway:10010", false);

            registry.register(instance("gateway", 10010).toBuilder().status(InstanceStatus.UP).build(),
                RegistrationKind.DYNAMIC);

            assertEquals(InstanceStatus.OUT_OF_SERVICE,
                registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow().status());
        }

        @Test
        void clearingAnOverrideRestoresTheReportedStatus() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            registry.overrideStatus("GATEWAY", "localhost:gateway:10010", InstanceStatus.OUT_OF_SERVICE, false);
            registry.clearStatusOverride("GATEWAY", "localhost:gateway:10010", InstanceStatus.UP, false);

            ServiceInstance stored = registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow();
            assertEquals(InstanceStatus.UP, stored.status());
            assertEquals(InstanceStatus.UNKNOWN, stored.overriddenStatus());
            assertEquals(InstanceStatus.UP, stored.effectiveStatus());
        }

        @Test
        @DisplayName("a replicated registration is not overridden by our own stale copy")
        void replicationIgnoresTheLocalLeaseRule() {
            registry.register(instance("gateway", 10010).toBuilder().status(InstanceStatus.UP).build(),
                RegistrationKind.DYNAMIC);

            // A peer tells us it is OUT_OF_SERVICE. Rule 3 is skipped for replication, so the peer wins.
            registry.register(instance("gateway", 10010).toBuilder()
                .status(InstanceStatus.OUT_OF_SERVICE).build(), RegistrationKind.REPLICATED);

            assertEquals(InstanceStatus.OUT_OF_SERVICE,
                registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow().status());
        }

        @Test
        void anOverriddenStatusArrivingOnRegistrationSeedsTheOverride() {
            registry.register(instance("gateway", 10010).toBuilder()
                .overriddenStatus(InstanceStatus.OUT_OF_SERVICE).build(), RegistrationKind.DYNAMIC);

            assertEquals(InstanceStatus.OUT_OF_SERVICE,
                registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow().overriddenStatus());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Self-preservation - the safety-critical part
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class SelfPreservationBehaviour {

        @Test
        @DisplayName("threshold is expected * (60 / interval) * 0.85, truncated")
        void reproducesEurekasThresholdArithmetic() {
            registry.openForTraffic(10);
            // 10 * (60/30) * 0.85 = 17.0
            assertEquals(17, registry.renewalThresholdPerMinute());

            InMemoryServiceRegistry three = newRegistry(RegistrySettings.defaults(), List.of());
            three.openForTraffic(3);
            // 3 * 2 * 0.85 = 5.1, truncated to 5 - Eureka truncates and so do we
            assertEquals(5, three.renewalThresholdPerMinute());
        }

        @Test
        @DisplayName("a partition that silences every client does not empty the registry")
        void aPartitionDoesNotCauseMassEviction() {
            for (int i = 0; i < 10; i++) {
                registry.register(instance("service" + i, 10100 + i), RegistrationKind.DYNAMIC);
            }
            registry.openForTraffic(10);
            assertEquals(10, registry.size());

            // Every heartbeat stops arriving and every lease lapses. This is what a network partition looks like
            // from the registry's side, and it is indistinguishable from every service dying at once.
            now += 10 * 60 * 1000;

            assertFalse(registry.evictionAllowed(),
                "with no renewals arriving, eviction must be suspended rather than trusted");
            assertEquals(0, registry.evict(0));
            assertEquals(10, registry.size(),
                "a partition must degrade to stale routing, never to no routing");
        }

        @Test
        void evictsWhenRenewalsAreHealthyAndALeaseGenuinelyLapses() {
            for (int i = 0; i < 10; i++) {
                registry.register(instance("service" + i, 10100 + i), RegistrationKind.DYNAMIC);
            }
            registry.openForTraffic(10);

            // Nine instances keep heartbeating briskly; the tenth goes quiet.
            for (int minute = 0; minute < 3; minute++) {
                for (int beat = 0; beat < 4; beat++) {
                    for (int i = 1; i < 10; i++) {
                        registry.renew("SERVICE" + i, "localhost:service" + i + ":" + (10100 + i), false);
                    }
                    now += 15_000;
                }
            }

            assertTrue(registry.renewalsInLastMinute() > registry.renewalThresholdPerMinute());
            assertTrue(registry.evictionAllowed());

            // The silent lease lapses at exactly this instant, and expiry is a strict greater-than, so nothing
            // goes yet. Worth asserting rather than stepping over: it pins which side of the boundary is
            // inclusive, and getting that wrong evicts a service one eviction cycle early.
            assertEquals(0, registry.evict(0), "a lease is not expired at the very instant it lapses");

            now += 1;
            int evicted = registry.evict(0);
            assertEquals(1, evicted, "only the silent instance should go");
            assertTrue(registry.instance("SERVICE0", "localhost:service0:10100").isEmpty());
            assertEquals(9, registry.size());
        }

        @Test
        @DisplayName("even with eviction enabled, one sweep cannot take more than (1 - threshold) of the registry")
        void theEvictionLimitCapsASingleSweep() {
            RegistrySettings noSelfPreservation = RegistrySettings.defaults().withSelfPreservation(false);
            InMemoryServiceRegistry open = newRegistry(noSelfPreservation, List.of());

            for (int i = 0; i < 20; i++) {
                open.register(instance("service" + i, 10100 + i), RegistrationKind.DYNAMIC);
            }
            open.openForTraffic(20);

            now += 10 * 60 * 1000; // every lease lapses

            // 20 - (int)(20 * 0.85) = 20 - 17 = 3
            assertEquals(3, open.evict(0));
            assertEquals(17, open.size(), "the remainder is held back for the next sweep");
        }

        @Test
        void selfPreservationCanBeTurnedOff() {
            RegistrySettings off = RegistrySettings.defaults().withSelfPreservation(false);
            InMemoryServiceRegistry open = newRegistry(off, List.of());
            open.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            open.openForTraffic(1);

            now += 10 * 60 * 1000;
            assertTrue(open.evictionAllowed());
            assertEquals(1, open.evict(0));
        }

        @Test
        @DisplayName("additionalLeaseMs postpones eviction, absorbing a GC pause or clock drift")
        void additionalLeaseSlackPostponesEviction() {
            RegistrySettings off = RegistrySettings.defaults().withSelfPreservation(false);
            InMemoryServiceRegistry open = newRegistry(off, List.of());
            open.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            open.openForTraffic(1);

            // Default lease is 90s and Eureka's doubled window makes that 180s.
            now += 200_000;
            assertEquals(0, open.evict(60_000), "60s of slack should still protect the lease");
            assertEquals(1, open.evict(0));
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Delta
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Delta {

        @Test
        void reportsRecentlyRegisteredInstances() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

            List<Application> apps = registry.delta().applications();
            assertEquals(1, apps.size());
            assertEquals(ActionType.ADDED, apps.get(0).instances().get(0).actionType());
        }

        @Test
        @DisplayName("a cancelled instance appears as DELETED so clients drop it instead of keeping it stale")
        void reportsCancellationsAsDeleted() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            registry.cancel("GATEWAY", "localhost:gateway:10010", false);

            List<Application> apps = registry.delta().applications();
            assertEquals(1, apps.size());
            assertEquals(ActionType.DELETED, apps.get(0).instances().get(0).actionType());
        }

        @Test
        void forgetsChangesOlderThanTheRetentionWindow() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            assertFalse(registry.delta().applications().isEmpty());

            now += RegistrySettings.DEFAULT_DELTA_RETENTION_MS + 1;
            assertTrue(registry.delta().applications().isEmpty());
        }

        @Test
        void carriesTheHashCodeOfTheWholeRegistryNotOfTheDelta() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            registry.register(instance("catalog", 10014), RegistrationKind.DYNAMIC);
            now += RegistrySettings.DEFAULT_DELTA_RETENTION_MS + 1;
            registry.register(instance("zaas", 10023), RegistrationKind.DYNAMIC);

            // Only the newest change is in the delta window, but the hashcode still describes all three - that is
            // how a client checks whether applying the delta left it consistent.
            assertEquals(1, registry.delta().applications().size());
            assertEquals("UP_3_", registry.delta().appsHashCode());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Interceptors and events
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Interceptors {

        @Test
        void applyInOrderAndCanRewriteTheInstance() {
            RegistrationInterceptor prefix = new RegistrationInterceptor() {
                @Override
                public ServiceInstance intercept(ServiceInstance instance) {
                    return instance.toBuilder().appName("ZOWE" + instance.appName()).build();
                }

                @Override
                public int order() {
                    return 1;
                }
            };
            RegistrationInterceptor tag = new RegistrationInterceptor() {
                @Override
                public ServiceInstance intercept(ServiceInstance instance) {
                    return instance.toBuilder().putMetadata("seen.by", instance.appName()).build();
                }

                @Override
                public int order() {
                    return 2;
                }
            };

            InMemoryServiceRegistry withInterceptors = newRegistry(RegistrySettings.defaults(), List.of(tag, prefix));
            withInterceptors.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

            ServiceInstance stored = withInterceptors.instance("ZOWEGATEWAY", "localhost:gateway:10010")
                .orElseThrow();
            assertEquals("ZOWEGATEWAY", stored.appName());
            assertEquals("ZOWEGATEWAY", stored.metadata().get("seen.by"),
                "the lower-order interceptor must run first, so the tag sees the rewritten name");
        }

        @Test
        void canRefuseARegistrationOutright() {
            RegistrationInterceptor denyList = instance -> {
                throw new RegistrationRejectedException("host not in the allow list: " + instance.hostName());
            };
            InMemoryServiceRegistry guarded = newRegistry(RegistrySettings.defaults(), List.of(denyList));

            assertThrows(RegistrationRejectedException.class,
                () -> guarded.register(instance("gateway", 10010), RegistrationKind.DYNAMIC));
            assertEquals(0, guarded.size());
        }
    }

    @Nested
    class Events {

        @Test
        void publishesTheLifecycleTheDiscoveryServiceListensFor() {
            registry.openForTraffic(1);
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            registry.renew("GATEWAY", "localhost:gateway:10010", false);
            registry.overrideStatus("GATEWAY", "localhost:gateway:10010", InstanceStatus.OUT_OF_SERVICE, false);
            registry.cancel("GATEWAY", "localhost:gateway:10010", false);

            List<Class<?>> kinds = events.stream().<Class<?>>map(Object::getClass).toList();
            assertEquals(List.of(
                RegistryEvent.RegistryAvailable.class,
                RegistryEvent.InstanceRegistered.class,
                RegistryEvent.InstanceRenewed.class,
                RegistryEvent.StatusChanged.class,
                RegistryEvent.InstanceCancelled.class
            ), kinds);
        }

        @Test
        void marksAnEvictionAsExpiredSoItCanBeDistinguishedFromADeliberateShutdown() {
            RegistrySettings off = RegistrySettings.defaults().withSelfPreservation(false);
            InMemoryServiceRegistry open = newRegistry(off, List.of());
            open.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            open.openForTraffic(1);
            events.clear();

            now += 10 * 60 * 1000;
            open.evict(0);

            RegistryEvent.InstanceCancelled cancelled = events.stream()
                .filter(RegistryEvent.InstanceCancelled.class::isInstance)
                .map(RegistryEvent.InstanceCancelled.class::cast)
                .findFirst()
                .orElseThrow();
            assertTrue(cancelled.expired());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------------------------------------------

    @Test
    void findsInstancesByVipAddress() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        registry.register(instance("gateway", 10011), RegistrationKind.DYNAMIC);
        registry.register(instance("catalog", 10014), RegistrationKind.DYNAMIC);

        assertEquals(2, registry.byVipAddress("gateway").size());
        assertEquals(1, registry.byVipAddress("catalog").size());
        assertTrue(registry.byVipAddress("nope").isEmpty());
    }

    @Test
    void mergesMetadataWithoutDiscardingWhatWasAlreadyThere() {
        registry.register(instance("gateway", 10010).toBuilder()
            .putMetadata("apiml.service.title", "Gateway").build(), RegistrationKind.DYNAMIC);

        assertTrue(registry.updateMetadata("GATEWAY", "localhost:gateway:10010",
            Map.of("apiml.externalUrl", "https://example.org")));

        ServiceInstance stored = registry.instance("GATEWAY", "localhost:gateway:10010").orElseThrow();
        assertEquals("Gateway", stored.metadata().get("apiml.service.title"));
        assertEquals("https://example.org", stored.metadata().get("apiml.externalUrl"));
    }

    @Test
    void ordersApplicationsAndInstancesSoRepeatedReadsAreByteStable() {
        registry.register(instance("zaas", 10023), RegistrationKind.DYNAMIC);
        registry.register(instance("catalog", 10014), RegistrationKind.DYNAMIC);
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

        List<String> names = registry.applications().applications().stream().map(Application::name).toList();
        assertEquals(List.of("CATALOG", "GATEWAY", "ZAAS"), names);
    }

}
