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

import org.zowe.apiml.registry.RegistryEvent;
import org.zowe.apiml.registry.RegistryListener;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fans local registry changes out to the peer nodes.
 * <p>
 * Wired as a {@link RegistryListener}, so replication is a consequence of a change rather than something every
 * write path has to remember to do - which is what the Eureka-based implementation needed
 * {@code replicateToPeers} reflection for.
 * <p>
 * The critical rule: <b>a change that arrived from a peer is never replicated onward.</b> Two nodes replicating
 * each other's replications is an infinite loop, and with three or more it is a broadcast storm.
 */
public final class PeerReplicator implements RegistryListener {

    private final List<PeerNode> peers;
    private final ServiceRegistry registry;

    public PeerReplicator(Collection<PeerNode> peers, ServiceRegistry registry) {
        this.peers = List.copyOf(peers);
        this.registry = registry;
    }

    public List<PeerNode> peers() {
        return peers;
    }

    /*
     * if-else over instanceof patterns rather than a pattern switch: switch patterns are still a preview feature
     * on the Java 17 baseline this project targets.
     */
    @Override
    public void onRegistryEvent(RegistryEvent event) {
        if (event instanceof RegistryEvent.InstanceRegistered registered) {
            if (!registered.fromPeer()) {
                broadcast(ReplicationItem.register(registered.instance()));
            }
        } else if (event instanceof RegistryEvent.InstanceRenewed renewed) {
            if (!renewed.fromPeer()) {
                lookup(renewed.appName(), renewed.instanceId())
                    .ifPresent(instance -> broadcast(ReplicationItem.heartbeat(instance)));
            }
        } else if (event instanceof RegistryEvent.StatusChanged changed) {
            if (!changed.fromPeer()) {
                lookup(changed.appName(), changed.instanceId())
                    .ifPresent(instance -> broadcast(ReplicationItem.statusUpdate(instance)));
            }
        } else if (event instanceof RegistryEvent.InstanceCancelled cancelled) {
            // An eviction is deliberately replicated too: the peer's own lease for that instance may not have
            // lapsed yet, and leaving it advertised there would keep routing traffic to something this node has
            // already given up on.
            if (!cancelled.fromPeer()) {
                broadcast(cancelTombstone(cancelled));
            }
        }
        // RegistryAvailable needs no replication; peers learn about us from our own registration.
    }

    /**
     * A cancel carries only identity, so it can be built after the instance has already gone from the registry.
     */
    private ReplicationItem cancelTombstone(RegistryEvent.InstanceCancelled cancelled) {
        return new ReplicationItem(
            ReplicationAction.Cancel,
            cancelled.appName(),
            cancelled.instanceId(),
            null,
            null,
            null,
            null
        );
    }

    private java.util.Optional<ServiceInstance> lookup(String appName, String instanceId) {
        return registry.instance(appName, instanceId);
    }

    private void broadcast(ReplicationItem item) {
        for (PeerNode peer : peers) {
            peer.enqueue(item);
        }
    }

    /**
     * The outcome of a flush.
     *
     * @param accepted how many items peers accepted in total
     * @param exhausted peers that have exceeded their retry budget and are being treated as down
     */
    public record FlushResult(int accepted, List<PeerNode> exhausted) {

        public boolean anyExhausted() {
            return !exhausted.isEmpty();
        }

    }

    /**
     * Flush every peer.
     * <p>
     * Failures are reported, not thrown and not logged here. One unreachable peer must not stop the others being
     * served, and it must certainly not fail the local registry operation that triggered the replication - the
     * local registry is still correct, it is only the copy that is behind. Logging is left to the Spring layer so
     * this module stays free of a logging dependency.
     */
    public FlushResult flushAll() {
        int accepted = 0;
        List<PeerNode> exhausted = new ArrayList<>();
        for (PeerNode peer : peers) {
            accepted += peer.flush();
            if (peer.exhausted()) {
                exhausted.add(peer);
            }
        }
        return new FlushResult(accepted, List.copyOf(exhausted));
    }

}
