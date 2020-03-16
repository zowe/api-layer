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
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GatewayNotifierTest {

    private static final int TIMEOUT_ASYNC_CALL_SEC = 5;

    private PeerAwareInstanceRegistry registry;

    private RestTemplate restTemplate;
    private MessageService messageService;
    private GatewayNotifier gatewayNotifierSync;

    @Before
    public void setUp() {
        EurekaServerContext context = mock(EurekaServerContext.class);
        registry = mock(AwsInstanceRegistry.class);
        when(context.getRegistry()).thenReturn(registry);
        EurekaServerContextHolder.initialize(context);

        restTemplate = mock(RestTemplate.class);
        messageService = mock(MessageService.class);
        gatewayNotifierSync = new GatewayNotifierSync(restTemplate, messageService);
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

    private Message createMessage(String messageKey, Object...params) {
        final MessageTemplate mt = new MessageTemplate();
        mt.setKey(messageKey);
        mt.setText("message");
        mt.setType(MessageType.INFO);
        return Message.of(messageKey, mt, params);
    }

    @Test
    public void testServiceUpdated() {
        verify(restTemplate, never()).delete(anyString());

        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("hostname1", 1000, 1433),
            createInstanceInfo("hostname2", 1000, 0)
        );

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        gatewayNotifierSync.serviceUpdated("testService", null);
        verify(restTemplate, times(1)).delete("https://hostname1:1433/cache/services/testService");
        verify(restTemplate, times(1)).delete("http://hostname2:1000/cache/services/testService");

        gatewayNotifierSync.serviceUpdated(null, null);
        verify(restTemplate, times(1)).delete("https://hostname1:1433/cache/services");
        verify(restTemplate, times(1)).delete("http://hostname2:1000/cache/services");

        verify(restTemplate, times(4)).delete(anyString());
    }

    @Test
    public void testMissingGateway() {
        final String messageKey = "org.zowe.apiml.discovery.errorNotifyingGateway";

        when(registry.getApplication(anyString())).thenReturn(null);
        when(messageService.createMessage(messageKey)).thenReturn(createMessage(messageKey));

        gatewayNotifierSync.serviceUpdated("serviceX", null);

        verify(messageService, times(1)).createMessage(messageKey);
    }

    @Test
    public void testNotificationFailed() {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);
        doThrow(new RuntimeException("any exception")).when(restTemplate).delete(anyString());
        List<InstanceInfo> instances = new LinkedList<>();
        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        // no gateway is registred
        gatewayNotifierSync.serviceUpdated("service", "host:service:1433");
        verify(restTemplate, never()).delete(anyString());

        // notify gateway itself
        instances.add(createInstanceInfo("GATEWAY","host", 1000, 1433));
        gatewayNotifierSync.serviceUpdated("GATEWAY", "host:GATEWAY:1433");
        verify(restTemplate, never()).delete(anyString());

        // notify gateway and restTemplate failed
        gatewayNotifierSync.serviceUpdated("service", "host2:service:123");
        verify(restTemplate, times(1)).delete(anyString());
        verify(messageService).createMessage(
            "org.zowe.apiml.discovery.registration.gateway.notify",
            "https://host:1433/cache/services/service",
            "host2:service:123"
        );
    }

    @Test
    public void testDistributeInvalidatedCredentials() {
        InstanceInfo targetInstanceInfo = createInstanceInfo("host", 1000, 1433);
        String targetInstanceId = targetInstanceInfo.getInstanceId();

        InstanceInfo gatewayInstance = createInstanceInfo("gateway", 111, 123);
        String gatewayUrl = "https://gateway:123/auth/distribute/" + targetInstanceId;

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(Collections.singletonList(gatewayInstance));
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        final String messageNotifyError = "org.zowe.apiml.discovery.errorNotifyingGateway";
        when(messageService.createMessage(messageNotifyError)).thenReturn(createMessage(messageNotifyError));
        final String messageKey = "org.zowe.apiml.discovery.registration.gateway.notify";
        Message msg = createMessage(messageKey, gatewayUrl, targetInstanceId);
        when(messageService.createMessage(messageKey, gatewayUrl, targetInstanceId)).thenReturn(msg);

        // succeed notified
        gatewayNotifierSync.distributeInvalidatedCredentials(targetInstanceId);
        verify(restTemplate, times(1)).getForEntity(eq(gatewayUrl), any(), (Exception) any());

        // error on notification
        when(restTemplate.getForEntity(anyString(), any())).thenThrow(new RuntimeException());
        gatewayNotifierSync.distributeInvalidatedCredentials(targetInstanceId);
        verify(messageService, times(1)).createMessage(messageKey, gatewayUrl, targetInstanceId);
    }

    @Test
    public void testAsynchronousTreatment() {
        GatewayNotifierHandler gatewayNotifier = new GatewayNotifierHandler(restTemplate, messageService);
        gatewayNotifier.afterPropertiesSet();

        gatewayNotifier.serviceUpdated("serviceId", "instanceId");
        await().atMost(TIMEOUT_ASYNC_CALL_SEC, TimeUnit.SECONDS).untilAsserted(
            () -> {
                assertEquals("serviceUpdatedProcess(serviceId,instanceId)", gatewayNotifier.getLastCall());
            }
        );

        gatewayNotifier.distributeInvalidatedCredentials("instanceId");
        await().atMost(TIMEOUT_ASYNC_CALL_SEC, TimeUnit.SECONDS).untilAsserted(
            () -> {
                assertEquals("distributeInvalidatedCredentialsProcess(instanceId)", gatewayNotifier.getLastCall());
            }
        );

        gatewayNotifier.preDestroy();
    }

    private class GatewayNotifierSync extends GatewayNotifier {

        public GatewayNotifierSync(RestTemplate restTemplate, MessageService messageService) {
            super(restTemplate, messageService);
        }

        public void afterPropertiesSet() {
            // remove implementation
        }

        @Override
        protected void addToQueue(GatewayNotifier.Notification notification) {
            notification.process();
        }

    }

    @Getter
    private class GatewayNotifierHandler extends GatewayNotifier {

        private String lastCall;

        public GatewayNotifierHandler(RestTemplate restTemplate, MessageService messageService) {
            super(restTemplate, messageService);
        }

        public void serviceUpdatedProcess(String serviceId, String instanceId) {
            lastCall = "serviceUpdatedProcess(" + serviceId + "," + instanceId + ")";
        }

        public void distributeInvalidatedCredentialsProcess(String instanceId) {
            lastCall = "distributeInvalidatedCredentialsProcess(" + instanceId + ")";
        }

    }

}
