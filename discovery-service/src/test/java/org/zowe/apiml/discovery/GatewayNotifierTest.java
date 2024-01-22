/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.AwsInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.Getter;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GatewayNotifierTest {

    private static final int TIMEOUT_ASYNC_CALL_SEC = 5;

    private PeerAwareInstanceRegistry registry;

    private CloseableHttpClient httpClient;
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
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        httpStatusLine = mock(StatusLine.class);
        when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
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

    @Nested
    class GivenNoGateway {
        private final String messageKey = "org.zowe.apiml.discovery.errorNotifyingGateway";

        @BeforeEach
        void setup() {
            when(registry.getApplication(anyString())).thenReturn(null);
            when(messageService.createMessage(messageKey)).thenReturn(createMessage(messageKey));
        }

        @Test
        void testMissingGateway() {
            gatewayNotifierSync.serviceUpdated("serviceX", null);
            verify(messageService, times(1)).createMessage(messageKey);
        }
    }

    @Nested
    class GivenTwoInstances {

        @BeforeEach
        void setup() {
            List<InstanceInfo> instances = Arrays.asList(
                createInstanceInfo("hostname1", 1000, 1433, true),
                createInstanceInfo("hostname2", 1000, 0, false)
            );

            Application application = mock(Application.class);
            when(application.getInstances()).thenReturn(instances);
            when(registry.getApplication("GATEWAY")).thenReturn(application);

            MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
            Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
            when(messageService.createMessage(anyString(), any(Object[].class))).thenReturn(message);
        }

        @Nested
        class WhenCancelRegistration {
            @Test
            void thenAPIisCalledWithGivenServiceId() throws IOException, URISyntaxException {
                verify(httpClient, never()).execute(any(HttpDelete.class));

                gatewayNotifierSync.serviceCancelledRegistration("testService");
                ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
                verify(httpClient, times(2)).execute(argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services/testService", argument.getAllValues().get(0).getUri().toString());
                assertEquals("http://hostname2:1000/gateway/cache/services/testService", argument.getAllValues().get(1).getUri().toString());
            }

            @Test
            void thenAPIisCalledWithoutServiceId() throws IOException, URISyntaxException {
                verify(httpClient, never()).execute(any(HttpDelete.class));

                gatewayNotifierSync.serviceCancelledRegistration(null);
                ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
                verify(httpClient, times(2)).execute(argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services", argument.getAllValues().get(0).getUri().toString());
                assertEquals("http://hostname2:1000/gateway/cache/services", argument.getAllValues().get(1).getUri().toString());
            }
        }
        @Nested
        class WhenServiceUpdated {
            @Test
            void thenAPIisCalledWithGivenServiceId() throws IOException, URISyntaxException {
                verify(httpClient, never()).execute(any(HttpDelete.class));

                gatewayNotifierSync.serviceUpdated("testService", null);
                ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
                verify(httpClient, times(2)).execute(argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services/testService", argument.getAllValues().get(0).getUri().toString());
                assertEquals("http://hostname2:1000/gateway/cache/services/testService", argument.getAllValues().get(1).getUri().toString());
            }

            @Test
            void thenAPIisCalledWithoutServiceId() throws IOException, URISyntaxException {
                verify(httpClient, never()).execute(any(HttpDelete.class));

                gatewayNotifierSync.serviceUpdated(null, null);
                ArgumentCaptor<HttpDelete> argument = ArgumentCaptor.forClass(HttpDelete.class);
                verify(httpClient, times(2)).execute(argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services", argument.getAllValues().get(0).getUri().toString());
                assertEquals("http://hostname2:1000/gateway/cache/services", argument.getAllValues().get(1).getUri().toString());
            }
        }

        @Nested
        class WhenDistributeInvalidatedCredentials {
            @Test
            void testDistributeInvalidatedCredentials() throws IOException, URISyntaxException {
                verify(httpClient, never()).execute(any(HttpGet.class));

                gatewayNotifierSync.distributeInvalidatedCredentials("instance");
                ArgumentCaptor<HttpGet> argument = ArgumentCaptor.forClass(HttpGet.class);
                verify(httpClient, times(2)).execute(argument.capture());
                assertEquals("https://hostname1:1433/gateway/auth/distribute/instance", argument.getAllValues().get(0).getUri().toString());
                assertEquals("http://hostname2:1000/gateway/auth/distribute/instance", argument.getAllValues().get(1).getUri().toString());
            }
        }

        @Nested
        class GivenHttpErrorDuringCancelRegistration {

            @Test
            void whenHttpExceptionThenErrorLogged() throws IOException {
                doThrow(new IOException("any exception")).when(httpClient).execute(any(HttpDelete.class));
                gatewayNotifierSync.serviceCancelledRegistration("service");

                verify(httpClient, times(2)).execute(any(HttpDelete.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.unregistration.gateway.notify"), argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services/service", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/cache/services/service", argument.getAllValues().get(1));
            }

            @Test
            void whenHttpResponseForbiddenThenErrorLogged() throws IOException {
                when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
                gatewayNotifierSync.serviceCancelledRegistration("service");

                verify(httpClient, times(2)).execute(any(HttpDelete.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.unregistration.gateway.notify"), argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services/service", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/cache/services/service", argument.getAllValues().get(1));
            }

            @Test
            void whenHttpResponseProcessingThenErrorLogged() throws IOException {
                when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_PROCESSING);
                gatewayNotifierSync.serviceCancelledRegistration("service");

                verify(httpClient, times(2)).execute(any(HttpDelete.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.unregistration.gateway.notify"), argument.capture());
                assertEquals("https://hostname1:1433/gateway/cache/services/service", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/cache/services/service", argument.getAllValues().get(1));
            }
        }

        @Nested
        class GivenHttpErrorDuringServiceUpdate {

            @Test
            void whenHttpExceptionThenErrorLogged() throws IOException {
                doThrow(new IOException("any exception")).when(httpClient).execute(any(HttpDelete.class));
                gatewayNotifierSync.serviceUpdated("service", "instance");

                verify(httpClient, times(2)).execute(any(HttpDelete.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.registration.gateway.notify"), argument.capture(), eq("instance"));
                assertEquals("https://hostname1:1433/gateway/cache/services/service", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/cache/services/service", argument.getAllValues().get(1));
            }

            @Test
            void whenHttpResponseForbiddenThenErrorLogged() throws IOException {
                when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
                gatewayNotifierSync.serviceUpdated("service", "instance");

                verify(httpClient, times(2)).execute(any(HttpDelete.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.registration.gateway.notify"), argument.capture(), eq("instance"));
                assertEquals("https://hostname1:1433/gateway/cache/services/service", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/cache/services/service", argument.getAllValues().get(1));
            }

            @Test
            void whenHttpResponseProcessingThenErrorLogged() throws IOException {
                when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_PROCESSING);
                gatewayNotifierSync.serviceUpdated("service", "instance");

                verify(httpClient, times(2)).execute(any(HttpDelete.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.registration.gateway.notify"), argument.capture(), eq("instance"));
                assertEquals("https://hostname1:1433/gateway/cache/services/service", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/cache/services/service", argument.getAllValues().get(1));
            }
        }

        @Nested
        class GivenHttpErrorDuringDistributeInvalidatedCredentials {

            @BeforeEach
            void setup() {
                MessageTemplate messageTemplate = new MessageTemplate("key", "number", MessageType.ERROR, "text");
                Message message = Message.of("requestedKey", messageTemplate, new Object[0]);
                when(messageService.createMessage(anyString(), any(Object[].class))).thenReturn(message);
            }

            @Test
            void whenHttpExceptionThenErrorLogged() throws IOException {
                doThrow(new IOException("any exception")).when(httpClient).execute(any(HttpGet.class));
                gatewayNotifierSync.distributeInvalidatedCredentials("instance");

                verify(httpClient, times(2)).execute(any(HttpGet.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.registration.gateway.notify"), argument.capture(), eq("instance"));
                assertEquals("https://hostname1:1433/gateway/auth/distribute/instance", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/auth/distribute/instance", argument.getAllValues().get(1));
            }

            @Test
            void whenHttpResponseForbiddenThenErrorLogged() throws IOException {
                when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
                gatewayNotifierSync.distributeInvalidatedCredentials("instance");

                verify(httpClient, times(2)).execute(any(HttpGet.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.registration.gateway.notify"), argument.capture(), eq("instance"));
                assertEquals("https://hostname1:1433/gateway/auth/distribute/instance", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/auth/distribute/instance", argument.getAllValues().get(1));
            }

            @Test
            void whenHttpResponseProcessingThenErrorLogged() throws IOException {
                when(httpStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_PROCESSING);
                gatewayNotifierSync.distributeInvalidatedCredentials("instance");

                verify(httpClient, times(2)).execute(any(HttpGet.class));
                ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
                verify(messageService, times(2)).createMessage(
                    eq("org.zowe.apiml.discovery.registration.gateway.notify"), argument.capture(), eq("instance"));
                assertEquals("https://hostname1:1433/gateway/auth/distribute/instance", argument.getAllValues().get(0));
                assertEquals("http://hostname2:1000/gateway/auth/distribute/instance", argument.getAllValues().get(1));
            }
        }

        @Nested
        class WhenAsynchronousRequests {

            @Test
            void thenRequestsfinishedOk() {
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

        }
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
