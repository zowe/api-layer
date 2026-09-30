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
     * The hostname this node is reachable on, and the only thing that distinguishes it from its peer in the
     * deployments that matter - see {@link #isLocal}.
     */
    @Value("${apiml.service.hostname:localhost}")
    private String serviceHostname;

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
     * <b>Both the hostname and the port have to match.</b> Deciding it on the port alone is wrong, and wrong in
     * the deployment that matters most: an HA pair configured the documented Zowe way is two nodes on
     * <em>different hostnames listening on the same port</em> - one APIML per LPAR, or one container per node in
     * the integration tests, where both are on 10011 and only the hostname differs:
     * <pre>
     *   APIML_SERVICE_HOSTNAME: discovery-service
     *   APIML_DISCOVERY_ALLPEERSURLS: https://discovery-service-2:10011/eureka,https://discovery-service:10011/eureka
     * </pre>
     * A port-only test calls <em>both</em> of those URLs local, {@link #peerUrls()} then returns nothing, and the
     * node runs "standalone" while believing it is configured as a peer. Nothing replicates in either direction,
     * so each node holds only what registered directly with it - which is exactly what the HA jobs showed: the
     * primary had {@code caching-service-2} but not {@code caching-service}, and the {@code -1} form of the
     * others but not the {@code -2}s, so the startup check reported four instances as never having onboarded.
     * <p>
     * The other candidate port still has to be considered: in the standalone Discovery Service a peer URL carries
     * {@code apiml.service.port}, while in the modulith the same node's URL is built from the <em>Gateway's</em>
     * {@code apiml.service.port} and the Discovery endpoint listens on
     * {@code apiml.internal-discovery.port}.
     * <p>
     * The hostname is compared rather than required to <em>differ</em>. The distinction matters, and is what the
     * previous implementation got wrong: a peer on the same hostname with a different port is not this node
     * (different port), and a peer on the same port with a different hostname is not this node either (different
     * host). Requiring a hostname <em>difference</em> would drop the second case along with the first.
     */
    private boolean isLocal(String url) {
        try {
            URI uri = URI.create(url);
            int urlPort = uri.getPort();
            if (urlPort == -1) {
                // No explicit port: falls back to the scheme's default, which is never how a peer URL is built.
                log.warn("Peer URL {} has no port; it will be treated as a remote peer", url);
                return false;
            }
            // Compared as primitives, deliberately. internalDiscoveryPort is a boxed Integer, and comparing two
            // Integers with == compares references: 10011 is outside the Integer cache, so the comparison is
            // false and this node stops recognising its own internal-discovery URL - which is the
            // replicate-to-yourself failure below, arrived at by accident.
            boolean portIsOurs = urlPort == servicePort
                || (internalDiscoveryPort != null && urlPort == internalDiscoveryPort.intValue());
            if (!portIsOurs) {
                return false;
            }
            String host = uri.getHost();
            boolean hostIsOurs = host != null && host.equalsIgnoreCase(serviceHostname);
            if (!hostIsOurs) {
                // Logged at debug rather than silently accepted: a node whose own URL is not recognised as local
                // replicates to itself, which doubles its registry traffic and skews the self-preservation
                // counters. That is the failure this branch exists to avoid, so it is worth being able to see.
                log.debug("Peer URL {} is not this node ({}:{})", url, serviceHostname, urlPort);
            }
            return hostIsOurs;
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
