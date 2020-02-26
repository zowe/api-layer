package org.zowe.apiml.discovery;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.AwsInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import java.util.Arrays;
import java.util.LinkedList;
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

    private InstanceInfo createInstanceInfo(String serviceId, String hostName, int port, int securePort) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getHostName()).thenReturn(hostName);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        when(out.getInstanceId()).thenReturn(hostName + ":" + serviceId + ":" + (securePort == 0 ? port : securePort));
        return out;
    }

    private InstanceInfo createInstanceInfo(String hostName, int port, int securePort) {
        return createInstanceInfo("service", hostName, port, securePort);
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

        gatewayNotifier.serviceUpdated("testService", null);
        verify(restTemplate, times(1)).delete("https://hostname1:1433/cache/services/testService");
        verify(restTemplate, times(1)).delete("http://hostname2:1000/cache/services/testService");

        gatewayNotifier.serviceUpdated(null, null);
        verify(restTemplate, times(1)).delete("https://hostname1:1433/cache/services");
        verify(restTemplate, times(1)).delete("http://hostname2:1000/cache/services");

        verify(restTemplate, times(4)).delete(anyString());
    }

    @Test
    public void testMissingGateway() {
        final String messageKey = "org.zowe.apiml.discovery.errorNotifyingGateway";
        final MessageTemplate mt = new MessageTemplate();
        mt.setKey(messageKey);
        mt.setText("message");
        mt.setType(MessageType.INFO);
        final Message m = Message.of(messageKey, mt, new Object[0]);

        RestTemplate restTemplate = mock(RestTemplate.class);
        MessageService messageService = mock(MessageService.class);
        GatewayNotifier gatewayNotifier = new GatewayNotifier(restTemplate, messageService);
        when(registry.getApplication(anyString())).thenReturn(null);
        when(messageService.createMessage(messageKey)).thenReturn(m);

        gatewayNotifier.serviceUpdated("serviceX", null);

        verify(messageService, times(1)).createMessage(messageKey);
    }

    @Test
    public void testNotificationFailed() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MessageService messageService = mock(MessageService.class);
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);
        GatewayNotifier gatewayNotifier = new GatewayNotifier(restTemplate, messageService);
        doThrow(new RuntimeException("any exception")).when(restTemplate).delete(anyString());
        List<InstanceInfo> instances = new LinkedList<>();
        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        // no gateway is registred
        gatewayNotifier.serviceUpdated("service", "host:service:1433");
        verify(restTemplate, never()).delete(anyString());

        // notify gateway itself
        instances.add(createInstanceInfo("GATEWAY","host", 1000, 1433));
        gatewayNotifier.serviceUpdated("GATEWAY", "host:GATEWAY:1433");
        verify(restTemplate, never()).delete(anyString());

        // notify gateway and restTemplate failed
        gatewayNotifier.serviceUpdated("service", "host2:service:123");
        verify(restTemplate, times(1)).delete(anyString());
        verify(messageService).createMessage(
            "org.zowe.apiml.discovery.registration.gateway.notify",
            "https://host:1433/cache/services/service",
            "host2:service:123"
        );
    }

}
