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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which configured peer URLs are this node.
 * <p>
 * This decides whether peer replication happens at all, and getting it wrong is silent: the node logs that it is
 * running standalone, starts up cleanly, serves everything that registers with it directly, and only diverges
 * from its peer over time. In the HA integration jobs it produced exactly that - the primary discovery's
 * {@code /eureka/apps} held {@code caching-service-2} but no {@code caching-service}, and the {@code -1} form of
 * the other services but no {@code -2}s, so the startup check reported four instances as never having onboarded
 * and the job died before running a test.
 */
class ReplicationConfigurationPeerTest {

    /**
     * The integration tests' HA topology, and the documented Zowe one: two nodes, same port, different
     * hostnames. The node must keep the peer and drop only itself.
     */
    @Test
    @DisplayName("A peer on another hostname and the same port is a peer, not this node")
    void thenAPeerOnAnotherHostKeepsTheSamePort() {
        ReplicationConfiguration config = configuration(
            "https://discovery-service-2:10011/eureka,https://discovery-service:10011/eureka",
            "discovery-service", 10011, null);

        assertEquals(List.of("https://discovery-service-2:10011/eureka"), config.peerUrls());
    }

    /**
     * The shape the previous implementation was written for, where the two nodes differ by port and may share a
     * hostname. Still has to work.
     */
    @Test
    @DisplayName("A peer on the same hostname and another port is a peer, not this node")
    void thenAPeerOnAnotherPortKeepsTheSameHost() {
        ReplicationConfiguration config = configuration(
            "https://localhost:10011/eureka,https://localhost:10021/eureka",
            "localhost", 10011, null);

        assertEquals(List.of("https://localhost:10021/eureka"), config.peerUrls());
    }

    /**
     * The modulith builds its own URL from the Gateway's {@code apiml.service.port} while the Discovery endpoint
     * listens on {@code apiml.internal-discovery.port}. Both have to be recognised as this node, or the modulith
     * POSTs a replication batch to its own Gateway port on every flush.
     */
    @Test
    @DisplayName("The modulith recognises itself on both its gateway port and its internal discovery port")
    void thenTheModulithKnowsBothOfItsPorts() {
        ReplicationConfiguration config = configuration(
            "https://apiml:10011/eureka,https://apiml-2:10011/eureka",
            "apiml", 10010, 10011);

        assertEquals(List.of("https://apiml-2:10011/eureka"), config.peerUrls());
    }

    @Test
    @DisplayName("A peer list holding only this node leaves nothing to replicate to")
    void thenASelfOnlyListYieldsNoPeers() {
        ReplicationConfiguration config = configuration(
            "https://discovery-service:10011/eureka",
            "discovery-service", 10011, null);

        assertTrue(config.peerUrls().isEmpty());
    }

    @Test
    @DisplayName("Hostnames are compared case-insensitively, since they arrive from configuration")
    void thenTheHostnameComparisonIsCaseInsensitive() {
        ReplicationConfiguration config = configuration(
            "https://DISCOVERY-SERVICE:10011/eureka,https://discovery-service-2:10011/eureka",
            "discovery-service", 10011, null);

        assertEquals(List.of("https://discovery-service-2:10011/eureka"), config.peerUrls());
    }

    @Test
    @DisplayName("An unparseable or portless entry is treated as a remote peer rather than dropped")
    void thenAnUnparseableEntryIsKept() {
        // The peer list is split on commas and whitespace, so a malformed entry has to be a single token.
        ReplicationConfiguration config = configuration(
            "%%%,https://discovery-service-2/eureka", "discovery-service", 10011, null);

        assertEquals(2, config.peerUrls().size(),
            "a URL this node cannot parse must not be silently assumed to be itself");
    }

    private static ReplicationConfiguration configuration(
        String allPeersUrls, String hostname, int servicePort, Integer internalDiscoveryPort
    ) {
        ReplicationConfiguration config = new ReplicationConfiguration();
        ReflectionTestUtils.setField(config, "allPeersUrls", allPeersUrls);
        ReflectionTestUtils.setField(config, "serviceHostname", hostname);
        ReflectionTestUtils.setField(config, "servicePort", servicePort);
        ReflectionTestUtils.setField(config, "internalDiscoveryPort", internalDiscoveryPort);
        return config;
    }

}
