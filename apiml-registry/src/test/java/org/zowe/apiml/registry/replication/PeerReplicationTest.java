/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.replication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrationKind;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Batching, retry and loop prevention for peer replication, driven through a stub transport.
 * <p>
 * No socket and no TLS handshake here on purpose: the logic that decides <em>what</em> to send, when to give up
 * and when to promote a heartbeat into a registration is where the HA bugs live, and it is worth testing without
 * a live peer. The transport implementation itself is the Spring layer's problem.
 */
class PeerReplicationTest {

    private static final long NOW = 1_700_000_000_000L;

    /** Records what it was asked to send and answers with whatever the test queued up. */
    private static final class StubTransport implements ReplicationTransport {

        private final List<ReplicationBatch> sent = new ArrayList<>();
        private final List<Object> answers = new ArrayList<>();

        void willRespond(ReplicationResponse response) {
            answers.add(response);
        }

        void willFail() {
            answers.add(new ReplicationTransportException("peer unreachable"));
        }

        @Override
        public ReplicationResponse send(String peerUrl, ReplicationBatch batch)
            throws ReplicationTransportException {

            sent.add(batch);
            Object answer = answers.isEmpty() ? null : answers.remove(0);
            if (answer instanceof ReplicationTransportException failure) {
                throw failure;
            }
            if (answer instanceof ReplicationResponse response) {
                return response;
            }
            // Default: accept everything.
            List<ReplicationResponse.Item> items = new ArrayList<>();
            batch.items().forEach(item -> items.add(new ReplicationResponse.Item(200, null)));
            return new ReplicationResponse(items);
        }
    }

    private long now;
    private StubTransport transport;
    private InMemoryServiceRegistry registry;
    private PeerNode peer;
    private PeerReplicator replicator;

    @BeforeEach
    void setUp() {
        now = NOW;
        transport = new StubTransport();
        registry = new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), () -> now);
        peer = new PeerNode("https://peer:10011/eureka/", transport, 3, 250,
            (app, id) -> registry.instance(app, id).orElse(null), () -> now);
        replicator = new PeerReplicator(List.of(peer), registry);
        registry.addListener(replicator);
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
    // Loop prevention
    // -----------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("a local registration is replicated")
    void replicatesLocalChanges() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        assertEquals(1, peer.pendingCount());

        peer.flush();
        assertEquals(1, transport.sent.size());
        assertEquals(ReplicationAction.Register, transport.sent.get(0).items().get(0).action());
    }

    @Test
    @DisplayName("a change that arrived from a peer is never sent back - otherwise two nodes loop forever")
    void doesNotReplicateChangesThatCameFromAPeer() {
        registry.register(instance("gateway", 10010), RegistrationKind.REPLICATED);
        assertEquals(0, peer.pendingCount());

        registry.renew("GATEWAY", "localhost:gateway:10010", true);
        assertEquals(0, peer.pendingCount());

        registry.overrideStatus("GATEWAY", "localhost:gateway:10010", InstanceStatus.OUT_OF_SERVICE, true);
        assertEquals(0, peer.pendingCount());

        registry.cancel("GATEWAY", "localhost:gateway:10010", true);
        assertEquals(0, peer.pendingCount());
    }

    @Test
    @DisplayName("an eviction is replicated, so the peer stops routing to it too")
    void replicatesEvictions() {
        InMemoryServiceRegistry evicting = new InMemoryServiceRegistry(
            RegistrySettings.defaults().withSelfPreservation(false), List.of(), () -> now);
        PeerNode evictingPeer = new PeerNode("https://peer:10011/eureka/", transport, 3, 250,
            (app, id) -> evicting.instance(app, id).orElse(null), () -> now);
        evicting.addListener(new PeerReplicator(List.of(evictingPeer), evicting));

        evicting.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        evicting.openForTraffic(1);
        evictingPeer.flush();

        now += 10 * 60 * 1000;
        evicting.evict(0);

        assertEquals(1, evictingPeer.pendingCount());
        evictingPeer.flush();
        ReplicationBatch last = transport.sent.get(transport.sent.size() - 1);
        assertEquals(ReplicationAction.Cancel, last.items().get(0).action());
    }

    // -----------------------------------------------------------------------------------------------------
    // Batching
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Batching {

        @Test
        @DisplayName("repeated heartbeats collapse - a slow peer must not accumulate a queue it cannot drain")
        void deduplicatesRepeatedChanges() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            peer.flush();

            for (int i = 0; i < 50; i++) {
                registry.renew("GATEWAY", "localhost:gateway:10010", false);
            }
            assertEquals(1, peer.pendingCount(), "50 heartbeats for one instance are one pending change");
        }

        @Test
        void sendsEveryQueuedInstanceInOneBatch() {
            for (int i = 0; i < 5; i++) {
                registry.register(instance("service" + i, 10100 + i), RegistrationKind.DYNAMIC);
            }
            assertEquals(5, peer.pendingCount());

            peer.flush();
            assertEquals(1, transport.sent.size(), "one round trip, not five");
            assertEquals(5, transport.sent.get(0).size());
        }

        @Test
        void respectsTheMaximumBatchSize() {
            PeerNode small = new PeerNode("https://peer:10011/eureka/", transport, 3, 2,
                (app, id) -> registry.instance(app, id).orElse(null), () -> now);
            for (int i = 0; i < 5; i++) {
                small.enqueue(ReplicationItem.register(instance("service" + i, 10100 + i)));
            }

            small.flush();
            assertEquals(2, transport.sent.get(0).size());
            assertEquals(3, small.pendingCount(), "the remainder waits for the next flush");
        }

        @Test
        void flushingAnEmptyQueueSendsNothing() {
            assertEquals(0, peer.flush());
            assertTrue(transport.sent.isEmpty());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // Failure handling
    // -----------------------------------------------------------------------------------------------------

    @Nested
    class Failures {

        @Test
        void requeuesAfterATransportFailureSoTheChangeIsNotLost() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            transport.willFail();

            assertEquals(0, peer.flush());
            assertEquals(1, peer.pendingCount(), "the change must survive a failed attempt");
            assertEquals(1, peer.consecutiveFailures());

            assertEquals(1, peer.flush(), "the retry succeeds");
            assertEquals(0, peer.pendingCount());
            assertEquals(0, peer.consecutiveFailures(), "a success clears the failure count");
        }

        @Test
        @DisplayName("past the retry budget the queue is dropped, so an unreachable peer is not a memory leak")
        void stopsRequeueingOnceTheRetryBudgetIsSpent() {
            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);

            for (int attempt = 0; attempt < 5; attempt++) {
                transport.willFail();
                peer.flush();
            }

            assertTrue(peer.exhausted());
            assertEquals(0, peer.pendingCount(),
                "an unreachable peer must not grow an unbounded backlog on a node that is otherwise healthy");
        }

        @Test
        void oneDeadPeerDoesNotStopTheOthers() {
            StubTransport deadTransport = new StubTransport();
            PeerNode dead = new PeerNode("https://dead:10011/eureka/", deadTransport, 0, 250,
                (app, id) -> null, () -> now);
            PeerReplicator two = new PeerReplicator(List.of(peer, dead), registry);
            registry.addListener(two);

            registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
            deadTransport.willFail();
            deadTransport.willFail();

            PeerReplicator.FlushResult result = two.flushAll();
            assertTrue(result.accepted() >= 1, "the healthy peer was still served");
            assertTrue(result.anyExhausted());
            assertEquals("https://dead:10011/eureka/", result.exhausted().get(0).peerUrl());
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // The 404 path
    // -----------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("a 404 on a replicated heartbeat is promoted to a full registration")
    void promotesAHeartbeatThePeerDoesNotRecogniseIntoARegistration() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        peer.flush();
        transport.sent.clear();

        registry.renew("GATEWAY", "localhost:gateway:10010", false);
        // The peer has never seen this instance - it restarted with an empty registry, or a partition healed.
        transport.willRespond(new ReplicationResponse(List.of(new ReplicationResponse.Item(404, null))));

        peer.flush();
        assertEquals(1, peer.pendingCount(),
            "the 404 must leave a follow-up queued, or the instance stays missing from that peer");

        peer.flush();
        ReplicationBatch followUp = transport.sent.get(transport.sent.size() - 1);
        assertEquals(ReplicationAction.Register, followUp.items().get(0).action());
        assertTrue(followUp.items().get(0).instance() != null,
            "the follow-up has to carry the instance body, which is the whole point");
    }

    @Test
    void doesNotRePromoteARegistrationThatWasItselfRejected() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        transport.willRespond(new ReplicationResponse(List.of(new ReplicationResponse.Item(404, null))));

        peer.flush();
        assertEquals(0, peer.pendingCount(),
            "a rejected Register must not loop; promoting it again would send the same thing forever");
    }

    @Test
    void doesNotPromoteWhenTheInstanceHasSinceGoneAway() {
        registry.register(instance("gateway", 10010), RegistrationKind.DYNAMIC);
        peer.flush();
        registry.renew("GATEWAY", "localhost:gateway:10010", false);
        registry.cancel("GATEWAY", "localhost:gateway:10010", false);

        // Cancel supersedes the heartbeat in the queue, and the instance is gone, so there is nothing to promote.
        transport.willRespond(new ReplicationResponse(List.of(
            new ReplicationResponse.Item(404, null), new ReplicationResponse.Item(404, null))));
        peer.flush();

        assertFalse(peer.pendingCount() > 0 && registry.instance("GATEWAY", "localhost:gateway:10010").isPresent());
    }

}
