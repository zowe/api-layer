/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.netflix.eureka.server.ReplicationClientAdditionalFilters;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(Lifecycle.PER_CLASS)
class RefreshablePeerEurekaNodesTest {

    private static final int DEFAULT_MAX_RETRIES = 10;

    PeerAwareInstanceRegistry registry;
    EurekaServerConfig serverConfig = mock(EurekaServerConfig.class);
    EurekaClientConfig clientConfig = mock(EurekaClientConfig.class);
    ServerCodecs serverCodecs = mock(ServerCodecs.class);
    ApplicationInfoManager applicationInfoManager;
    ReplicationClientAdditionalFilters replicationClientAdditionalFilters = mock(ReplicationClientAdditionalFilters.class);

    @Test
    void givenEurekaNodeUrl_thenCreateNode() {
        when(serverConfig.getPeerNodeTotalConnections()).thenReturn(100);
        when(serverConfig.getPeerNodeTotalConnectionsPerHost()).thenReturn(10);
        when(replicationClientAdditionalFilters.getFilters()).thenReturn(Collections.emptyList());
        RefreshablePeerEurekaNodes eurekaNodes = mock(RefreshablePeerEurekaNodes.class);
        ReflectionTestUtils.setField(eurekaNodes, "serverCodecs", serverCodecs);
        ReflectionTestUtils.setField(eurekaNodes, "serverConfig", serverConfig);
        ReflectionTestUtils.setField(eurekaNodes, "replicationClientAdditionalFilters", replicationClientAdditionalFilters);

        when(eurekaNodes.createPeerEurekaNode(any())).thenCallRealMethod();

        PeerEurekaNode node = eurekaNodes.createPeerEurekaNode("https://localhost:10013/");
        assertTrue(node instanceof ApimlPeerEurekaNode);
    }

    static Stream<Set<String>> values() {
        Set<String> clientRegion = new HashSet<>();
        clientRegion.add("eureka.client.region");
        Set<String> zones = new HashSet<>();
        zones.add("eureka.client.availability-zones.");
        zones.add("eureka.client.service-url.");
        return Stream.of(clientRegion, zones);
    }

    @ParameterizedTest
    @MethodSource("values")
    void givenClientEvent_thenUpdate(Set<String> changedKeys) {
        RefreshablePeerEurekaNodes eurekaNodes = new RefreshablePeerEurekaNodes(registry, serverConfig, clientConfig, serverCodecs, applicationInfoManager, replicationClientAdditionalFilters, DEFAULT_MAX_RETRIES);
        when(clientConfig.shouldUseDnsForFetchingServiceUrls()).thenReturn(false);

        assertTrue(eurekaNodes.shouldUpdate(changedKeys));
    }

    @Test
    void givenDNSShoudBeUsed_thenDoNotUpdate() {
        RefreshablePeerEurekaNodes eurekaNodes = new RefreshablePeerEurekaNodes(registry, serverConfig, clientConfig, serverCodecs, applicationInfoManager, replicationClientAdditionalFilters, DEFAULT_MAX_RETRIES);
        when(clientConfig.shouldUseDnsForFetchingServiceUrls()).thenReturn(true);
        assertFalse(eurekaNodes.shouldUpdate(new HashSet<>()));
    }

    @Test
    void givenNoEvents_thenDoNotUpdate() {
        RefreshablePeerEurekaNodes eurekaNodes = new RefreshablePeerEurekaNodes(registry, serverConfig, clientConfig, serverCodecs, applicationInfoManager, replicationClientAdditionalFilters, DEFAULT_MAX_RETRIES);
        when(clientConfig.shouldUseDnsForFetchingServiceUrls()).thenReturn(false);
        assertFalse(eurekaNodes.shouldUpdate(new HashSet<>()));
    }

}
