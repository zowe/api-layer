/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerSentEventProxyHandlerTest {
    private static final String HOST = "host.com";
    private static final int PORT = 10010;
    private static final String SERVICE_URL = "/service";
    private static final String URL_SECURE = "https://" + HOST + ":" + PORT + SERVICE_URL;
    private static final String URL_INSECURE = "http://" + HOST + ":" + PORT + SERVICE_URL;
    private static final String SERVICE_ID = "serviceid";
    private static final String ENDPOINT = "/endpoint";
    private static final String MAJOR_VERSION = "v1";
    private static final String GATEWAY_PATH = "/" + SERVICE_ID + "/sse/" + MAJOR_VERSION + ENDPOINT;
    private static final String GATEWAY_PATH_OLD_FORMAT = "/sse/" + MAJOR_VERSION + "/" + SERVICE_ID + ENDPOINT;

    private ServerSentEventProxyHandler underTest;
    private DiscoveryClient mockDiscoveryClient;
    private HttpServletRequest mockHttpServletRequest;
    private HttpServletResponse mockHttpServletResponse;

    @BeforeEach
    public void setup() {
        mockHttpServletRequest = mock(HttpServletRequest.class);
        mockHttpServletResponse = mock(HttpServletResponse.class);

        mockDiscoveryClient = mock(DiscoveryClient.class);
        underTest = Mockito.spy(new ServerSentEventProxyHandler(mockDiscoveryClient));
    }

    @Nested
    class WhenGetEmitter {
        @Nested
        class GivenBadUri_thenReturnEmitter {
            @Test
            void givenNoUriParts() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(null);
                verifyConsumersNotUsed();
            }

            @Test
            void givenShortUri() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn("/notenoughparts");
                verifyConsumersNotUsed();
            }

            private void verifyConsumersNotUsed() throws IOException {
                SseEmitter result = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
                assertThat(result, is(not(nullValue())));
                verify(underTest, times(0)).consumer(any());
                verify(underTest, times(0)).error(any());
            }
        }

        @Nested
        class GivenNoServices_thenReturnNull {
            @Test
            void whenQueryDiscoveryService_thenReturnNull() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);

                PrintWriter mockWriter = mock(PrintWriter.class);
                when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);
                when(mockDiscoveryClient.getInstances(anyString())).thenReturn(new ArrayList<>());

                SseEmitter result = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
                assertThat(result, is(nullValue()));
                verify(mockWriter).print(anyString());
            }
        }

        @Nested
        class GivenService {
            @Test
            void givenSameServiceTwice_thenUtilizeConsumersTwice() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);
                mockServiceInstance(true);

                verifyConsumersUsed();
                verifyConsumersUsed();
                verifyTargetUrlUsed(URL_SECURE + ENDPOINT);
            }

            @Test
            void givenInsecureService_thenHttpProtocolUsed() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);
                mockServiceInstance(false);

                verifyConsumersUsed();
                verifyConsumersUsed();
                verifyTargetUrlUsed(URL_INSECURE + ENDPOINT);
            }
        }

        @Nested
        class ThenUtilizeConsumers {
            @Test
            void givenEndpoint() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);
                mockServiceInstance(true);

                verifyConsumersUsed();
                verifyTargetUrlUsed(URL_SECURE + ENDPOINT);
            }

            @Test
            void givenNoEndpointPath() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn("/serviceid/sse/v1/");
                mockServiceInstance(true);

                verifyConsumersUsed();
                verifyTargetUrlUsed(URL_SECURE + "/");
            }

            @Test
            void givenPathInEndpoint() throws IOException {
                String extraEndpoint = "/anotherendpoint";
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH + extraEndpoint);
                mockServiceInstance(true);

                verifyConsumersUsed();
                verifyTargetUrlUsed(URL_SECURE + ENDPOINT + extraEndpoint);
            }

            @Test
            void givenOldPathFormat() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH_OLD_FORMAT);
                mockServiceInstance(true);

                verifyConsumersUsed();
                verifyTargetUrlUsed(URL_SECURE + ENDPOINT);
            }
        }

        @Nested
        class WhenUseDataConsumer {
            @Test
            void givenContent_thenEmitData() throws IOException {
                SseEmitter mockEmitter = mock(SseEmitter.class);
                Consumer<ServerSentEvent<String>> result = underTest.consumer(mockEmitter);

                ServerSentEvent<String> event = ServerSentEvent.builder("event").build();
                result.accept(event);

                verify(mockEmitter).send(anyString());
            }

            @Test
            void givenIOExceptionWhenSendContent_thenCompleteStream() throws IOException {
                SseEmitter mockEmitter = mock(SseEmitter.class);
                Consumer<ServerSentEvent<String>> result = underTest.consumer(mockEmitter);

                doThrow(new IOException("error")).when(mockEmitter).send(anyString());

                ServerSentEvent<String> event = ServerSentEvent.builder("event").build();
                result.accept(event);

                verify(mockEmitter).complete();
            }
        }

        @Nested
        class WhenUseErrorConsumer {
            @Test
            void givenError_thenCompleteStream() {
                SseEmitter mockEmitter = mock(SseEmitter.class);
                Consumer<Throwable> result = underTest.error(mockEmitter);
                result.accept(new Exception("error"));

                verify(mockEmitter).complete();
            }
        }

        private void mockServiceInstance(boolean isSecure) {
            RoutedServices mockRoutedServices = mock(RoutedServices.class);
            RoutedService mockRoutedService = mock(RoutedService.class);
            when(mockRoutedService.getServiceUrl()).thenReturn(SERVICE_URL + "/");
            when(mockRoutedServices.findServiceByGatewayUrl(anyString())).thenReturn(mockRoutedService);
            underTest.addRoutedServices(SERVICE_ID, mockRoutedServices);

            ServiceInstance serviceInstance = mock(ServiceInstance.class);
            when(serviceInstance.getHost()).thenReturn(HOST);
            when(serviceInstance.getPort()).thenReturn(PORT);
            when(serviceInstance.isSecure()).thenReturn(isSecure);

            List<ServiceInstance> serviceInstances = new ArrayList<>();
            serviceInstances.add(serviceInstance);
            when(mockDiscoveryClient.getInstances(SERVICE_ID)).thenReturn(serviceInstances);
        }

        private void verifyConsumersUsed() throws IOException {
            SseEmitter emitter = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
            assertThat(emitter, is(not(nullValue())));
            verify(underTest).consumer(emitter);
            verify(underTest).error(emitter);
        }

        private void verifyTargetUrlUsed(String expectedUrl) {
            Set<String> usedUrls = underTest.getSseEventStreams().keySet();
            assertThat("Expected url '" + expectedUrl + "' was not used", usedUrls.contains(expectedUrl), is(true));
        }
    }
}
