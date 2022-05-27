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
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GatewayNotifierTest {

    private static final int TIMEOUT_ASYNC_CALL_SEC = 5;

    private PeerAwareInstanceRegistry registry;

    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine httpStatusLine;
    private MessageService messageService;
    private GatewayNotifier gatewayNotifierSync;

    @BeforeEach
    void setUp() throws IOException {
        EurekaServerContext context = mock(EurekaServerContext.class);
        registry = mock(AwsInstanceRegistry.class);
        when(context.getRegistry()).thenReturn(registry);
        EurekaServerContextHolder.initialize(context);

        httpClient = mock(CloseableHttpClient.class);
        httpResponse = mock(CloseableHttpResponse.class);
        httpStatusLine = mock(StatusLine.class);
        when(httpStatusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(httpStatusLine);
        when(httpClient.execute(any())).thenReturn(httpResponse);

        messageService = mock(MessageService.class);
        gatewayNotifierSync = new GatewayNotifierSync(httpClient, messageService);
    }

    private InstanceInfo createInstanceInfo(String serviceId, String hostName, int port, int securePort, boolean isSecureEnabled) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getHostName()).thenReturn(hostName);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        when(out.getInstanceId()).thenReturn(hostName + ":" + serviceId + ":" + (securePort == 0 ? port : securePort));
        when(out.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(isSecureEnabled);
        return out;
    }

    private InstanceInfo createInstanceInfo(String hostName, int port, int securePort, boolean isSecureEnabled) {
        return createInstanceInfo("service", hostName, port, securePort, isSecureEnabled);
    }

    private Message createMessage(String messageKey, Object... params) {
        final MessageTemplate mt = new MessageTemplate();
        mt.setKey(messageKey);
        mt.setText("message");
        mt.setType(MessageType.INFO);
        return Message.of(messageKey, mt, params);
    }

    @Test
    void testServiceRegistrationCancelled() throws IOException {
        verify(httpClient, never()).execute(any(HttpDelete.class));

        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("hostname1", 1000, 1433, true),
            createInstanceInfo("hostname2", 1000, 0, false)
        );

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        gatewayNotifierSync.serviceCancelledRegistration("testService");
        ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
        verify(httpClient, times(2)).execute(argument.capture());
        assertEquals("https://hostname1:1433/gateway/cache/services/testService", argument.getAllValues().get(0).getURI().toString());
        assertEquals("http://hostname2:1000/gateway/cache/services/testService", argument.getAllValues().get(1).getURI().toString());
    }

    @Test
    void testNullServiceRegistrationCancelled() throws IOException {
        verify(httpClient, never()).execute(any(HttpDelete.class));

        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("hostname1", 1000, 1433, true),
            createInstanceInfo("hostname2", 1000, 0, false)
        );

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        gatewayNotifierSync.serviceCancelledRegistration(null);
        ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
        verify(httpClient, times(2)).execute(argument.capture());
        assertEquals("https://hostname1:1433/gateway/cache/services", argument.getAllValues().get(0).getURI().toString());
        assertEquals("http://hostname2:1000/gateway/cache/services", argument.getAllValues().get(1).getURI().toString());
    }

    @Test
    void testServiceUpdated() throws IOException {
        verify(httpClient, never()).execute(any(HttpDelete.class));

        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("hostname1", 1000, 1433, true),
            createInstanceInfo("hostname2", 1000, 0, false)
        );

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        gatewayNotifierSync.serviceUpdated("testService", null);
        ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
        verify(httpClient, times(2)).execute(argument.capture());
        assertEquals("https://hostname1:1433/gateway/cache/services/testService", argument.getAllValues().get(0).getURI().toString());
        assertEquals("http://hostname2:1000/gateway/cache/services/testService", argument.getAllValues().get(1).getURI().toString());
    }

    @Test
    void testNullServiceUpdated() throws IOException {
        verify(httpClient, never()).execute(any(HttpDelete.class));

        List<InstanceInfo> instances = Arrays.asList(
            createInstanceInfo("hostname1", 1000, 1433, true),
            createInstanceInfo("hostname2", 1000, 0, false)
        );

        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        gatewayNotifierSync.serviceUpdated(null, null);
        ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
        verify(httpClient, times(2)).execute(argument.capture());
        assertEquals("https://hostname1:1433/gateway/cache/services", argument.getAllValues().get(0).getURI().toString());
        assertEquals("http://hostname2:1000/gateway/cache/services", argument.getAllValues().get(1).getURI().toString());
    }

    @Test
    void testMissingGateway() {
        final String messageKey = "org.zowe.apiml.discovery.errorNotifyingGateway";

        when(registry.getApplication(anyString())).thenReturn(null);
        when(messageService.createMessage(messageKey)).thenReturn(createMessage(messageKey));

        gatewayNotifierSync.serviceUpdated("serviceX", null);

        verify(messageService, times(1)).createMessage(messageKey);
    }

    @Test
    void testServiceRegistrationCancelledNotificationFailed() throws IOException {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);
        doThrow(new RuntimeException("any exception")).when(httpClient).execute(any(HttpDelete.class));
        List<InstanceInfo> instances = new LinkedList<>();
        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        // no gateway is registered
        gatewayNotifierSync.serviceCancelledRegistration("service");
        verify(httpClient, never()).execute(any(HttpDelete.class));


        // notify gateway and restTemplate failed
        instances.add(createInstanceInfo("GATEWAY", "host", 1000, 1433, true));
        gatewayNotifierSync.serviceCancelledRegistration("service");
        verify(httpClient, times(1)).execute(any(HttpDelete.class));
        verify(messageService).createMessage(
            "org.zowe.apiml.discovery.unregistration.gateway.notify",
            "https://host:1433/gateway/cache/services/service"
        );
    }

    @Test
    void testServiceUpdatedNotificationFailed() throws IOException {
        MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
        Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
        when(messageService.createMessage(anyString(), (Object[]) any())).thenReturn(message);
        doThrow(new RuntimeException("any exception")).when(httpClient).execute(any(HttpDelete.class));
        List<InstanceInfo> instances = new LinkedList<>();
        Application application = mock(Application.class);
        when(application.getInstances()).thenReturn(instances);
        when(registry.getApplication("GATEWAY")).thenReturn(application);

        // no gateway is registered
        gatewayNotifierSync.serviceUpdated("service", "host:service:1433");
        verify(httpClient, never()).execute(any(HttpDelete.class));

        // notify gateway itself
        instances.add(createInstanceInfo("GATEWAY", "host", 1000, 1433, true));
        gatewayNotifierSync.serviceUpdated("GATEWAY", "host:GATEWAY:1433");
        verify(httpClient, never()).execute(any(HttpDelete.class));

        // notify gateway and restTemplate failed
        gatewayNotifierSync.serviceUpdated("service", "host2:service:123");
        verify(httpClient, times(1)).execute(any(HttpDelete.class));
        verify(messageService).createMessage(
            "org.zowe.apiml.discovery.registration.gateway.notify",
            "https://host:1433/gateway/cache/services/service",
            "host2:service:123"
        );
    }

    @Test
    void testDistributeInvalidatedCredentials() throws IOException {
        InstanceInfo targetInstanceInfo = createInstanceInfo("host", 1000, 1433, true);
        String targetInstanceId = targetInstanceInfo.getInstanceId();

        InstanceInfo gatewayInstance = createInstanceInfo("gateway", 111, 123, true);
        String gatewayUrl = "https://gateway:123/gateway/auth/distribute/" + targetInstanceId;

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
        ArgumentCaptor<HttpGet> argument = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient, times(1)).execute(argument.capture());
        assertEquals(gatewayUrl, argument.getValue().getURI().toString());

        // error on notification
        when(httpClient.execute(any(HttpGet.class))).thenThrow(new RuntimeException());
        gatewayNotifierSync.distributeInvalidatedCredentials(targetInstanceId);
        verify(messageService, times(1)).createMessage(messageKey, gatewayUrl, targetInstanceId);
    }

    @Test
    void testAsynchronousTreatment() {
        GatewayNotifierHandler gatewayNotifier = new GatewayNotifierHandler(httpClient, messageService);
        gatewayNotifier.afterPropertiesSet();

        gatewayNotifier.serviceUpdated("serviceId", "instanceId");
        await().atMost(TIMEOUT_ASYNC_CALL_SEC, TimeUnit.SECONDS).untilAsserted(
            () -> assertEquals("serviceUpdatedProcess(serviceId,instanceId)", gatewayNotifier.getLastCall())
        );

        gatewayNotifier.distributeInvalidatedCredentials("instanceId");
        await().atMost(TIMEOUT_ASYNC_CALL_SEC, TimeUnit.SECONDS).untilAsserted(
            () -> assertEquals("distributeInvalidatedCredentialsProcess(instanceId)", gatewayNotifier.getLastCall())
        );

        gatewayNotifier.preDestroy();
    }

    private static class GatewayNotifierSync extends GatewayNotifier {

        public GatewayNotifierSync(CloseableHttpClient httpClient, MessageService messageService) {
            super(httpClient, messageService);
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
    private static class GatewayNotifierHandler extends GatewayNotifier {

        private String lastCall;

        public GatewayNotifierHandler(CloseableHttpClient httpClient, MessageService messageService) {
            super(httpClient, messageService);
        }

        public void serviceUpdatedProcess(String serviceId, String instanceId) {
            lastCall = "serviceUpdatedProcess(" + serviceId + "," + instanceId + ")";
        }

        public void distributeInvalidatedCredentialsProcess(String instanceId) {
            lastCall = "distributeInvalidatedCredentialsProcess(" + instanceId + ")";
        }

    }

}
