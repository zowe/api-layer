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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.replication.PeerNode;
import org.zowe.apiml.registry.replication.PeerReplicator;
import org.zowe.apiml.registry.replication.ReplicationTransport;

import javax.net.ssl.SSLContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Peer replication wiring.
 * <p>
 * Peers come from {@code apiml.discovery.allPeersUrls}, which lists every node including this one - so the
 * local URL has to be filtered out or the node replicates to itself, doubling its own registry traffic and
 * confusing the self-preservation counters.
 */
@Configuration
@Slf4j
public class ReplicationConfiguration {

    @Value("${apiml.discovery.allPeersUrls:}")
    private String allPeersUrls;

    @Value("${apiml.service.port:10011}")
    private int servicePort;

    /**
     * The port this node is actually reachable on.
     * <p>
     * Checked ahead of {@code apiml.service.port} because in the modulith the Discovery endpoint listens on its
     * own port while {@code apiml.service.port} is the Gateway's. Ported from
     * {@code RefreshablePeerEurekaNodes.getPort}.
     */
    @Value("${apiml.internal-discovery.port:#{null}}")
    private Integer internalDiscoveryPort;

    @Value("${apiml.discovery.maxPeerRetries:10}")
    private int maxPeerRetries;

    @Value("${eureka.server.maxElementsInPeerReplicationPool:250}")
    private int maxBatchSize;

    @Value("${eureka.server.peerNodeConnectTimeoutMs:1000}")
    private int peerConnectTimeoutMs;

    @Value("${eureka.server.peerNodeReadTimeoutMs:30000}")
    private int peerReadTimeoutMs;

    @Value("${eureka.server.peerNodeTotalConnectionsPerHost:10}")
    private int connectionsPerPeer;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerify;

    @Bean
    public ReplicationTransport replicationTransport(
        @Qualifier("secureSslContext") SSLContext secureSslContext,
        RegistryCodec codec
    ) {
        return new HttpReplicationTransport(
            secureSslContext,
            codec,
            !nonStrictVerify,
            peerConnectTimeoutMs,
            peerReadTimeoutMs,
            connectionsPerPeer
        );
    }

    @Bean
    public PeerReplicator peerReplicator(ServiceRegistry registry, ReplicationTransport transport) {
        List<PeerNode> peers = new ArrayList<>();
        for (String url : peerUrls()) {
            peers.add(new PeerNode(
                url,
                transport,
                maxPeerRetries,
                maxBatchSize,
                (appName, instanceId) -> registry.instance(appName, instanceId).orElse(null),
                System::currentTimeMillis
            ));
        }
        if (peers.isEmpty()) {
            log.info("No peer Discovery Services configured; running standalone");
        } else {
            log.info("Replicating to {} peer(s): {}", peers.size(), peers.stream().map(PeerNode::peerUrl).toList());
        }

        PeerReplicator replicator = new PeerReplicator(peers, registry);
        registry.addListener(replicator);
        return replicator;
    }

    /** Every configured peer except this node. */
    List<String> peerUrls() {
        if (allPeersUrls == null || allPeersUrls.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allPeersUrls.split("[,\\s]+"))
            .map(String::trim)
            .filter(url -> !url.isEmpty())
            .filter(url -> !isLocal(url))
            .toList();
    }

    /**
     * Whether a configured peer URL is in fact this node.
     * <p>
     * Both candidate ports have to be considered. In the standalone Discovery Service the URL carries
     * {@code apiml.service.port}; in the modulith the same node's URL is built from the <em>Gateway's</em>
     * {@code apiml.service.port} while the Discovery endpoint listens on {@code apiml.internal-discovery.port}.
     * Checking only the internal port - which is what a literal reading of
     * {@code RefreshablePeerEurekaNodes.getPort} suggests - makes the modulith treat itself as a peer and POST a
     * replication batch to its own Gateway port every flush interval.
     * <p>
     * The hostname is not compared: an HA pair on one LPAR shares a hostname and differs only by port, so
     * requiring a hostname difference would wrongly filter out the real peer.
     */
    private boolean isLocal(String url) {
        try {
            int urlPort = URI.create(url).getPort();
            if (urlPort == servicePort) {
                return true;
            }
            return internalDiscoveryPort != null && urlPort == internalDiscoveryPort;
        } catch (RuntimeException e) {
            log.warn("Cannot parse peer URL {}; it will be treated as a remote peer", url);
            return false;
        }
    }

    /**
     * Drains the outbound queues.
     * <p>
     * Separate from the registry write path on purpose: replication must never make a local registration slower
     * or fail it. A peer being down is a stale copy, not a local error.
     */
    @Component
    @Slf4j
    public static class ReplicationFlushTask {

        private final PeerReplicator replicator;

        public ReplicationFlushTask(PeerReplicator replicator) {
            this.replicator = replicator;
        }

        @Scheduled(fixedDelayString = "${apiml.discovery.replicationFlushIntervalMs:1000}")
        public void flush() {
            if (replicator.peers().isEmpty()) {
                return;
            }
            var result = replicator.flushAll();
            if (result.anyExhausted()) {
                result.exhausted().forEach(peer -> log.warn(
                    "Peer {} has failed {} consecutive replication attempts and is being treated as down",
                    peer.peerUrl(), peer.consecutiveFailures()));
            }
        }

    }

}
