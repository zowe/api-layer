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

import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

/**
 * One peer Discovery Service, with an outbound queue.
 * <p>
 * Changes are accumulated and flushed in batches rather than sent one at a time - that is what makes replication
 * affordable when a hundred services heartbeat every thirty seconds. Eureka achieved this with a bounded
 * acceptor/executor/traffic-shaper trio ({@code com.netflix.eureka.util.batcher}, 17 classes); a de-duplicating
 * queue with a size and interval trigger covers the same ground in one class, because APIML replicates between a
 * handful of nodes rather than across a Netflix-scale fleet.
 * <p>
 * Two behaviours are load-bearing and reproduce the current implementation:
 * <ul>
 *     <li><b>De-duplication.</b> Only the newest change per instance per action is kept. Without it a slow peer
 *         accumulates a queue of stale heartbeats it can never drain.</li>
 *     <li><b>404 means re-register.</b> When a peer reports 404 for a replicated heartbeat it has never seen the
 *         instance, and the correct response is to send it the full registration. Dropping that leaves the
 *         instance permanently missing from that peer after a partition.</li>
 * </ul>
 */
public final class PeerNode {

    private final String peerUrl;
    private final ReplicationTransport transport;
    private final int maxRetries;
    private final int maxBatchSize;
    private final LongSupplier clock;

    /**
     * Resolves an instance for re-registration after a 404. Supplied rather than holding a registry reference so
     * this class stays independent of how the registry is stored.
     */
    private final BiFunction<String, String, ServiceInstance> instanceLookup;

    /** Keyed for de-duplication; insertion-ordered so a Register still precedes the Heartbeat that follows it. */
    private final Map<String, ReplicationItem> pending = new LinkedHashMap<>();

    private int consecutiveFailures;

    public PeerNode(
        String peerUrl,
        ReplicationTransport transport,
        int maxRetries,
        int maxBatchSize,
        BiFunction<String, String, ServiceInstance> instanceLookup,
        LongSupplier clock
    ) {
        this.peerUrl = peerUrl;
        this.transport = transport;
        this.maxRetries = maxRetries;
        this.maxBatchSize = maxBatchSize;
        this.instanceLookup = instanceLookup;
        this.clock = clock;
    }

    public String peerUrl() {
        return peerUrl;
    }

    /** Queue a change, superseding any earlier one for the same instance and action. */
    public synchronized void enqueue(ReplicationItem item) {
        pending.put(item.dedupeKey(), item);
    }

    public synchronized int pendingCount() {
        return pending.size();
    }

    public int consecutiveFailures() {
        return consecutiveFailures;
    }

    /** Whether this peer has failed more times in a row than {@code maxRetries} allows. */
    public boolean exhausted() {
        return consecutiveFailures > maxRetries;
    }

    /**
     * Send everything queued, up to {@link #maxBatchSize}.
     *
     * @return the number of items the peer accepted
     */
    public int flush() {
        List<ReplicationItem> batch;
        synchronized (this) {
            if (pending.isEmpty()) {
                return 0;
            }
            batch = new ArrayList<>(pending.values());
            if (batch.size() > maxBatchSize) {
                batch = new ArrayList<>(batch.subList(0, maxBatchSize));
            }
            batch.forEach(item -> pending.remove(item.dedupeKey()));
        }

        try {
            ReplicationResponse response = transport.send(peerUrl, new ReplicationBatch(batch));
            consecutiveFailures = 0;
            return handle(batch, response);
        } catch (ReplicationTransport.ReplicationTransportException e) {
            consecutiveFailures++;
            // Re-queue so the change is not lost, but only while retries remain. Past that the peer is treated as
            // down and the queue is dropped rather than grown without bound - an unreachable peer must not turn
            // into a memory leak on a node that is otherwise healthy.
            if (!exhausted()) {
                synchronized (this) {
                    for (ReplicationItem item : batch) {
                        pending.putIfAbsent(item.dedupeKey(), item);
                    }
                }
            }
            return 0;
        }
    }

    private int handle(List<ReplicationItem> batch, ReplicationResponse response) {
        int accepted = 0;
        for (int i = 0; i < batch.size(); i++) {
            ReplicationItem sent = batch.get(i);
            if (i >= response.items().size()) {
                continue;
            }
            ReplicationResponse.Item verdict = response.items().get(i);
            if (verdict.success()) {
                accepted++;
                continue;
            }
            if (verdict.notFound() && sent.action() != ReplicationAction.Register) {
                // The peer does not know this instance. Promote to a full registration so it learns about it.
                ServiceInstance instance = instanceLookup.apply(sent.appName(), sent.id());
                if (instance != null) {
                    enqueue(ReplicationItem.register(instance));
                }
            }
        }
        return accepted;
    }

}
