/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.ribbon.loadbalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.zowe.apiml.zaas.context.ConfigurableNamedContextFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadBalancerRuleAdapterTest {

    private static DiscoveryEnabledServer server;
    private static DiscoveryEnabledServer server1;
    private static ILoadBalancer lb;

    @BeforeAll
    static void setup() {
        lb = mock(ILoadBalancer.class);
        server = createServer("server");
        server1 = createServer("server2");
    }

    @Nested
    class GivenOnlyDefaultPredicate {
        @Test
        void choosesRoundRobin() {
            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(mock(InstanceInfo.class), mock(ConfigurableNamedContextFactory.class), null);
            underTest.setLoadBalancer(lb);
            List<Server> serverList = Arrays.asList(server, server1);
            Collections.shuffle(serverList);
            when(lb.getAllServers()).thenReturn(serverList);
            Server theChosenOne = underTest.choose("key");
            Server theChosenTwo = underTest.choose("key");
            Server theChosenThree = underTest.choose("key");
            assertNotNull(theChosenOne);
            assertNotNull(theChosenTwo);
            assertNotNull(theChosenThree);
            assertNotEquals(theChosenOne, theChosenTwo);
            assertEquals(theChosenOne, theChosenThree);
        }
    }

    @Nested
    class GivenAdditionalPredicate {

        private ConfigurableNamedContextFactory<NamedContextFactory.Specification> configurableNamedContextFactory;
        private RequestAwarePredicate requestAwarePredicate;
        private RequestAwarePredicate requestAwarePredicate1;
        private InstanceInfo instanceInfo;
        private Map<String, Object> predicateMap;

        @BeforeEach
        void setup() {
            configurableNamedContextFactory = mock(ConfigurableNamedContextFactory.class);
            requestAwarePredicate = mock(RequestAwarePredicate.class);
            requestAwarePredicate1 = mock(RequestAwarePredicate.class);
            instanceInfo = InstanceInfo.Builder.newBuilder().setAppName("app_one").build();
            predicateMap = new HashMap<>();
            predicateMap.put("predicate", requestAwarePredicate);
            predicateMap.put("predicate1", requestAwarePredicate1);
        }

        @Test
        void useAllPredicates() {
            when(requestAwarePredicate.apply(any(), any())).thenReturn(true);
            when(requestAwarePredicate1.apply(any(), any())).thenReturn(true);
            when(configurableNamedContextFactory.getInstances(any(), any())).thenReturn(predicateMap);
            when(lb.getAllServers()).thenReturn(Arrays.asList(server, server1));

            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(instanceInfo, configurableNamedContextFactory, null);
            underTest.setLoadBalancer(lb);

            underTest.choose("key");

            verify(requestAwarePredicate, times(1)).apply(any(), eq(server));
            verify(requestAwarePredicate, times(1)).apply(any(), eq(server1));
            verify(requestAwarePredicate1, times(1)).apply(any(), eq(server));
            verify(requestAwarePredicate1, times(1)).apply(any(), eq(server1));
        }

        @Test
        void noServerFitsThePredicate() {
            when(configurableNamedContextFactory.getInstances(any(), any())).thenReturn(predicateMap);
            when(requestAwarePredicate.apply(any(), any())).thenReturn(false);

            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(instanceInfo, configurableNamedContextFactory, null);

            underTest.setLoadBalancer(lb);
            assertNull(underTest.choose("key"));
        }
    }

    @Nested
    class givenHeterogeneousListOfServers {


        private final Map<String, Object> predicateMap = new HashMap<>();
        private final ConfigurableNamedContextFactory<NamedContextFactory.Specification> configurableNamedContextFactory = mock(ConfigurableNamedContextFactory.class);

        @Test
        void shouldFailFast() {
            predicateMap.put("predicate", mock(RequestAwarePredicate.class));
            when(configurableNamedContextFactory.getInstances(any(), any())).thenReturn(predicateMap);
            when(lb.getAllServers()).thenReturn(Arrays.asList(server, new Server("host", 80)));

            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(mock(InstanceInfo.class), configurableNamedContextFactory, null);
            underTest.setLoadBalancer(lb);
            assertThrows(IllegalStateException.class, () -> underTest.choose("key"));
        }
    }

    private static DiscoveryEnabledServer createServer(String name) {
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName(name)
                .setHostName(name)
                .build();
        return new DiscoveryEnabledServer(instanceInfo, true);
    }

}
