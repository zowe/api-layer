/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.ActionType;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client's cache, delta folding and self-registration.
 * <p>
 * The delta-divergence path is the one to get right: applying a delta to a stale base produces a routing table
 * that quietly disagrees with the registry, and the symptom is traffic sent to an instance that no longer exists.
 */
class RegistryClientTest {

    private static final long NOW = 1_700_000_000_000L;

    /** Records calls and answers whatever the test sets up. */
    private static final class StubTransport implements RegistryTransport {

        Applications full = new Applications(List.of(), 1L, "");
        Applications delta;
        boolean failFetch;
        boolean renewFound = true;
        final List<String> calls = new ArrayList<>();

        @Override
        public Applications fetchApplications() throws RegistryTransportException {
            calls.add("fetchApplications");
            if (failFetch) {
                throw new RegistryTransportException("unreachable");
            }
            return full;
        }

        @Override
        public Applications fetchDelta() throws RegistryTransportException {
            calls.add("fetchDelta");
            if (failFetch) {
                throw new RegistryTransportException("unreachable");
            }
            return delta;
        }

        @Override
        public void register(ServiceInstance instance) {
            calls.add("register");
        }

        @Override
        public boolean renew(String appName, String instanceId) {
            calls.add("renew");
            return renewFound;
        }

        @Override
        public void cancel(String appName, String instanceId) {
            calls.add("cancel");
        }

        @Override
        public void updateStatus(String appName, String instanceId, InstanceStatus status) {
            calls.add("updateStatus");
        }
    }

    private StubTransport transport;
    private RegistryClient client;

    @BeforeEach
    void setUp() {
        transport = new StubTransport();
        client = new RegistryClient(transport);
    }

    private static ServiceInstance instance(String service, int port, InstanceStatus status) {
        return ServiceInstance.builder()
            .instanceId("localhost:" + service + ":" + port)
            .appName(service)
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .securePort(port, true)
            .vipAddress(service)
            .status(status)
            .lease(Lease.renewable(30, 90, NOW))
            .build();
    }

    private static Applications snapshot(List<ServiceInstance> instances, Long version) {
        List<Application> apps = new ArrayList<>();
        instances.forEach(instance -> apps.add(new Application(instance.appName(), List.of(instance))));
        return new Applications(apps, version, Applications.computeHashCode(apps));
    }

    // -----------------------------------------------------------------------------------------------------
    // Fetching
    // -----------------------------------------------------------------------------------------------------

    @Test
    void aFullFetchReplacesTheView() {
        transport.full = snapshot(List.of(instance("gateway", 10010, InstanceStatus.UP)), 1L);
        transport.delta = null;

        assertTrue(client.refresh());
        assertEquals(1, client.cache().size());
        assertEquals(1, client.cache().upInstances("GATEWAY").size());
    }

    @Test
    @DisplayName("a transport with no delta support stops being asked for one")
    void stopsAskingForDeltasWhenUnsupported() {
        transport.delta = null;
        transport.full = snapshot(List.of(instance("gateway", 10010, InstanceStatus.UP)), 1L);

        client.refresh();
        client.refresh();

        assertEquals(1, transport.calls.stream().filter("fetchDelta"::equals).count(),
            "the transport should only be asked once");
    }

    @Test
    @DisplayName("an unreachable registry keeps the last known view rather than emptying it")
    void keepsTheLastViewWhenTheRegistryIsUnreachable() {
        transport.full = snapshot(List.of(instance("gateway", 10010, InstanceStatus.UP)), 1L);
        transport.delta = null;
        client.refresh();
        assertEquals(1, client.cache().size());

        transport.failFetch = true;
        assertFalse(client.refresh());

        assertEquals(1, client.cache().size(),
            "a registry that cannot be reached is not a registry that is empty - discarding the cache here would "
                + "stop the Gateway routing to healthy services");
        assertEquals(1, client.consecutiveFetchFailures());
    }

    // -----------------------------------------------------------------------------------------------------
    // Deltas
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Deltas {

        @Test
        void addsAnInstanceFromADelta() {
            var gateway = instance("gateway", 10010, InstanceStatus.UP);
            // Seeded directly rather than by a full fetch: a fetchDelta() of null is how a transport declares it
            // has no deltas at all (LocalRegistryTransport does exactly that), and it latches. Using it to set up
            // a delta test would disable the very path under test.
            client.cache().replace(snapshot(List.of(gateway), 1L));

            var catalog = instance("apicatalog", 10014, InstanceStatus.UP).toBuilder()
                .actionType(ActionType.ADDED).build();
            var merged = List.of(new Application("GATEWAY", List.of(gateway)),
                new Application("APICATALOG", List.of(catalog)));
            transport.delta = new Applications(List.of(new Application("APICATALOG", List.of(catalog))), 2L,
                Applications.computeHashCode(merged));

            assertTrue(client.refresh());
            assertEquals(2, client.cache().size());
            assertEquals(1, client.cache().upInstances("APICATALOG").size());
        }

        @Test
        void removesAnInstanceMarkedDeleted() {
            var gateway = instance("gateway", 10010, InstanceStatus.UP);
            var catalog = instance("apicatalog", 10014, InstanceStatus.UP);
            var seeded = List.of(new Application("GATEWAY", List.of(gateway)),
                new Application("APICATALOG", List.of(catalog)));
            client.cache().replace(new Applications(seeded, 1L, Applications.computeHashCode(seeded)));
            assertEquals(2, client.cache().size());

            var removed = catalog.toBuilder().actionType(ActionType.DELETED).build();
            transport.delta = new Applications(
                List.of(new Application("APICATALOG", List.of(removed))), 2L,
                Applications.computeHashCode(List.of(new Application("GATEWAY", List.of(gateway)))));

            client.refresh();
            assertEquals(1, client.cache().size());
            assertTrue(client.cache().upInstances("APICATALOG").isEmpty());
        }

        @Test
        @DisplayName("a delta that leaves the view inconsistent triggers a full fetch instead")
        void divergentDeltaFallsBackToAFullFetch() {
            transport.full = snapshot(List.of(instance("gateway", 10010, InstanceStatus.UP)), 5L);
            client.cache().replace(transport.full);
            // A delta whose declared hash code cannot match what folding it produces - the registry and this
            // client disagree, so serving the folded result would mean routing on a wrong table.
            transport.delta = new Applications(
                List.of(new Application("GHOST", List.of(
                    instance("ghost", 1, InstanceStatus.UP).toBuilder().actionType(ActionType.ADDED).build()))),
                6L, "THIS_WILL_NOT_MATCH_");

            client.refresh();

            assertTrue(transport.calls.contains("fetchApplications"),
                "the client must fall back to a full fetch rather than serve a divergent view");
            assertEquals(1, client.cache().size());
            assertTrue(client.cache().upInstances("GHOST").isEmpty());
        }

        @Test
        void notifiesListenersOnEveryRefreshThatChangesTheView() {
            var seen = new ArrayList<Integer>();
            client.addListener(cache -> seen.add(cache.size()));

            transport.full = snapshot(List.of(instance("gateway", 10010, InstanceStatus.UP)), 1L);
            transport.delta = null;
            client.refresh();

            assertEquals(List.of(1), seen);
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Self-registration
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class SelfRegistration {

        @Test
        @DisplayName("a heartbeat the registry does not recognise triggers a re-registration")
        void reRegistersWhenTheRegistryHasForgottenUs() {
            var self = instance("gateway", 10010, InstanceStatus.UP);
            var selfClient = new RegistryClient(transport, self);

            transport.renewFound = false;
            assertTrue(selfClient.heartbeat());

            assertTrue(transport.calls.contains("renew"));
            assertTrue(transport.calls.contains("register"),
                "a registry that restarted comes back empty; a client that only renews would never reappear");
            assertTrue(selfClient.registered());
        }

        @Test
        void aSuccessfulRenewalDoesNotReRegister() {
            var selfClient = new RegistryClient(transport, instance("gateway", 10010, InstanceStatus.UP));
            transport.renewFound = true;

            assertTrue(selfClient.heartbeat());
            assertFalse(transport.calls.contains("register"));
        }

        @Test
        void aClientWithoutASelfInstanceDoesNotRegister() {
            assertFalse(client.register());
            assertFalse(client.heartbeat());
            assertTrue(transport.calls.isEmpty());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Local transport
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Local {

        @Test
        @DisplayName("the local transport reads the registry directly, with no HTTP and no delta reconciliation")
        void readsTheRegistryInProcess() {
            var registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), () -> NOW);
            registry.register(instance("gateway", 10010, InstanceStatus.UP), RegistrationKind.DYNAMIC);

            var local = new RegistryClient(new LocalRegistryTransport(registry));
            assertTrue(local.refresh());

            assertEquals(1, local.cache().size());
            assertEquals(1, local.cache().upInstances("GATEWAY").size());
        }

        @Test
        void writesGoStraightToTheRegistry() {
            var registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), () -> NOW);
            var self = instance("gateway", 10010, InstanceStatus.UP);
            var local = new RegistryClient(new LocalRegistryTransport(registry), self);

            assertTrue(local.register());
            assertEquals(1, registry.size());
            assertTrue(local.heartbeat());

            local.unregister();
            assertEquals(0, registry.size());
        }
    }

    @Test
    void exposesTheCacheAsASpringCloudDiscoveryClient() {
        transport.full = snapshot(List.of(
            instance("gateway", 10010, InstanceStatus.UP),
            instance("downservice", 10099, InstanceStatus.DOWN)), 1L);
        transport.delta = null;
        client.refresh();

        var discoveryClient = new CachedRegistryDiscoveryClient(client);

        assertEquals(1, discoveryClient.getInstances("gateway").size());
        assertTrue(discoveryClient.getInstances("downservice").isEmpty(),
            "a DOWN instance must not be offered for routing");
        assertTrue(discoveryClient.getServices().contains("gateway"));

        var springInstance = discoveryClient.getInstances("gateway").get(0);
        assertEquals("localhost", springInstance.getHost());
        assertEquals(10010, springInstance.getPort());
        assertTrue(springInstance.isSecure());
    }

    @Test
    void springInstancesExposeTheirTransportScheme() {
        var secure = instance("secure", 10010, InstanceStatus.UP);
        var nonSecure = instance("nonsecure", 10011, InstanceStatus.UP).toBuilder()
            .port(10011, true)
            .securePort(10011, false)
            .build();

        assertEquals("https", CachedRegistryDiscoveryClient.toSpringInstance(secure).getScheme());
        assertEquals("http", CachedRegistryDiscoveryClient.toSpringInstance(nonSecure).getScheme());
    }

}
