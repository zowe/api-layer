package com.ca.mfaas.discovery;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.mfaas.message.core.MessageService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.AwsInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class GatewayNotifierTest {

    private PeerAwareInstanceRegistry registry;

    @Before
    public void setUp() {
        EurekaServerContext context = mock(EurekaServerContext.class);
        registry = mock(AwsInstanceRegistry.class);
        when(context.getRegistry()).thenReturn(registry);
        EurekaServerContextHolder.initialize(context);
    }

    private InstanceInfo createInstanceInfo(String hostName, int port, int securePort) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getHostName()).thenReturn(hostName);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        return out;
    }

    @Test
    public void testServiceUpdated() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MessageService messageService = mock(MessageService.class);
        GatewayNotifier gatewayNotifier = new GatewayNotifier(restTemplate, messageService);

        verify(restTemplate, never()).delete(anyString());

        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("hostname1", 1000, 1433),
            createInstanceInfo("hostname2", 1000, 0)
        );

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        gatewayNotifier.serviceUpdated("testService");
        verify(restTemplate, times(1)).delete("https://hostname1:1433/cache/services/testService");
        verify(restTemplate, times(1)).delete("http://hostname2:1000/cache/services/testService");

        gatewayNotifier.serviceUpdated(null);
        verify(restTemplate, times(1)).delete("https://hostname1:1433/cache/services");
        verify(restTemplate, times(1)).delete("http://hostname2:1000/cache/services");

        verify(restTemplate, times(4)).delete(anyString());
    }

}
