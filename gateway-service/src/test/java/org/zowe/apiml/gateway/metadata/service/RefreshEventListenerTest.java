/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.metadata.service;

import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class RefreshEventListenerTest {

    CorsMetadataProcessor corsMetadataProcessor = mock(CorsMetadataProcessor.class);
    DynamicServerListLoadBalancer loadBalancer = mock(DynamicServerListLoadBalancer.class);
    LoadBalancerEventListener loadBalancerEventListener = mock(LoadBalancerEventListener.class);
    List<ApplicationListener<ApplicationEvent>> applicationListeners;

    @BeforeEach
    void setUp() {
        when(loadBalancer.getName()).thenReturn("LB");
        loadBalancerEventListener = new LoadBalancerEventListener();
        loadBalancerEventListener.registerLoadBalancer(loadBalancer);
        applicationListeners = Arrays.asList(corsMetadataProcessor, loadBalancerEventListener);
    }

    @Test
    void refreshOnEventTest() {
        applicationListeners.forEach(app -> app.onApplicationEvent(new HeartbeatEvent("source", "state")));
        verify(loadBalancer, times(1)).updateListOfServers();

    }
}
