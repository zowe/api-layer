/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadBalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoadBalancerRuleAdapterTest {

    private static DiscoveryEnabledServer server;
    private static DiscoveryEnabledServer server1;
    private static DiscoveryEnabledServer server2;
    private static ILoadBalancer lb;

    @BeforeAll
    static void setup() {
        lb = mock(ILoadBalancer.class);
        server = mock(DiscoveryEnabledServer.class);
        server1 = mock(DiscoveryEnabledServer.class);
        server2 = mock(DiscoveryEnabledServer.class);
    }

    @Nested
    class GivenOnlyDefaultPredicate {
        @Test
        void choosesRoundRobin() {
            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(mock(InstanceInfo.class), mock(PredicateFactory.class), null);
            underTest.setLoadBalancer(lb);
            when(lb.getAllServers()).thenReturn(Arrays.asList(server, server1));
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

        private PredicateFactory predicateFactory;
        private RequestAwarePredicate requestAwarePredicate;
        private RequestAwarePredicate requestAwarePredicate1;
        private InstanceInfo instanceInfo;
        private Map<String, Object> predicateMap;

        @BeforeEach
        void setup() {
            predicateFactory = mock(PredicateFactory.class);
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
            when(predicateFactory.getInstances(any(), any())).thenReturn(predicateMap);
            when(lb.getAllServers()).thenReturn(Arrays.asList(server, server1));

            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(instanceInfo, predicateFactory, null);
            underTest.setLoadBalancer(lb);

            underTest.choose("key");

            verify(requestAwarePredicate, times(1)).apply(any(), eq(server));
            verify(requestAwarePredicate, times(1)).apply(any(), eq(server1));
            verify(requestAwarePredicate1, times(1)).apply(any(), eq(server));
            verify(requestAwarePredicate1, times(1)).apply(any(), eq(server1));
        }

        @Test
        void noServerFitsThePredicate() {
            when(predicateFactory.getInstances(any(), any())).thenReturn(predicateMap);
            when(requestAwarePredicate.apply(any(), any())).thenReturn(false);

            LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(instanceInfo, predicateFactory, null);

            underTest.setLoadBalancer(lb);
            assertNull(underTest.choose("key"));
        }
    }

}
