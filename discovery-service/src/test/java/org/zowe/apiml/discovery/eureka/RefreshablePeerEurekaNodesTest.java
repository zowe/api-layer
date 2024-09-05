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
import com.netflix.servo.monitor.StatsMonitor;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.product.eureka.client.ApimlPeerEurekaNode;

import javax.net.ssl.SSLContext;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class RefreshablePeerEurekaNodesTest {

    private static final int DEFAULT_MAX_RETRIES = 10;
    private static final VarHandle MODIFIERS;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    PeerAwareInstanceRegistry registry;
    @Mock
    EurekaServerConfig serverConfig;
    @Mock
    EurekaClientConfig clientConfig;
    @Mock
    ServerCodecs serverCodecs;
    SSLContext secureSslContextWithoutKeystore;

    List<ClientRequestFilter> replicationClientAdditionalFilters = new ArrayList<>();

    ApplicationInfoManager applicationInfoManager;

    RefreshablePeerEurekaNodes eurekaNodes;

    @BeforeAll
    @SuppressWarnings("unused")
    void init() throws NoSuchAlgorithmException {
        Class<?> monitor = StatsMonitor.class;
        secureSslContextWithoutKeystore = SSLContext.getDefault();
    }

    @BeforeEach
    void setUp() {
        eurekaNodes = new RefreshablePeerEurekaNodes(registry, serverConfig, clientConfig, serverCodecs, applicationInfoManager, replicationClientAdditionalFilters, secureSslContextWithoutKeystore, DEFAULT_MAX_RETRIES);
    }

    @Test
    void givenEurekaNodeUrl_thenCreateNode() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        when(serverConfig.getPeerNodeTotalConnections()).thenReturn(100);
        when(serverConfig.getPeerNodeTotalConnectionsPerHost()).thenReturn(10);

        Field defaultExecutor = StatsMonitor.class.getDeclaredField("DEFAULT_EXECUTOR");
        MODIFIERS.set(defaultExecutor, defaultExecutor.getModifiers() & ~Modifier.FINAL);
        defaultExecutor.setAccessible(true);
        defaultExecutor.set(null, Executors.newSingleThreadScheduledExecutor());

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
        when(clientConfig.shouldUseDnsForFetchingServiceUrls()).thenReturn(false);

        assertTrue(eurekaNodes.shouldUpdate(changedKeys));
    }

    @Test
    void givenDNSShoudBeUsed_thenDoNotUpdate() {
        when(clientConfig.shouldUseDnsForFetchingServiceUrls()).thenReturn(true);
        assertFalse(eurekaNodes.shouldUpdate(new HashSet<>()));
    }

    @Test
    void givenNoEvents_thenDoNotUpdate() {
        when(clientConfig.shouldUseDnsForFetchingServiceUrls()).thenReturn(false);
        assertFalse(eurekaNodes.shouldUpdate(new HashSet<>()));
    }
}
