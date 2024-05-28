/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerSentEventProxyHandlerTest {
    private static final String HOST = "host.com";
    private static final int PORT = 10010;
    private static final String SERVICE_URL = "/service";
    private static final String URL_SECURE = "https://" + HOST + ":" + PORT + SERVICE_URL;
    private static final String URL_INSECURE = "http://" + HOST + ":" + PORT + SERVICE_URL;
    private static final String SERVICE_ID = "serviceid";
    private static final String ENDPOINT = "/endpoint/";
    private static final String MAJOR_VERSION = "v1";
    private static final String GATEWAY_PATH = "/" + SERVICE_ID + "/sse/" + MAJOR_VERSION + ENDPOINT;

    private ServerSentEventProxyHandler underTest;
    private DiscoveryClient mockDiscoveryClient;
    private HttpServletRequest mockHttpServletRequest;
    private HttpServletResponse mockHttpServletResponse;
    private final MessageService messageService = new YamlMessageService("/gateway-messages.yml");

    @BeforeEach
    public void setup() {
        mockHttpServletRequest = mock(HttpServletRequest.class);
        mockHttpServletResponse = mock(HttpServletResponse.class);

        mockDiscoveryClient = mock(DiscoveryClient.class);
        underTest = Mockito.spy(new ServerSentEventProxyHandler(mockDiscoveryClient, messageService));
    }

    @Nested
    class WhenGetEmitter {

        @Nested
        class WhenError_thenWriteErrorAndReturnNull {
            @Nested
            class GivenBadUri_thenReturnEmitter {
                @Test
                void givenNoUriParts() throws IOException {
                    when(mockHttpServletRequest.getRequestURI()).thenReturn(null);
                    PrintWriter mockWriter = mock(PrintWriter.class);
                    when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);

                    verifyError(mockWriter, HttpStatus.BAD_REQUEST);
                }

                @Test
                void givenShortUri() throws IOException {
                    when(mockHttpServletRequest.getRequestURI()).thenReturn("/notenoughparts");
                    PrintWriter mockWriter = mock(PrintWriter.class);
                    when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);

                    verifyError(mockWriter, HttpStatus.BAD_REQUEST);
                }
            }

            @Nested
            class GivenNoServices_thenWriteErrorAndReturnNull {
                @Test
                void whenQueryDiscoveryService() throws IOException {
                    when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);

                    PrintWriter mockWriter = mock(PrintWriter.class);
                    when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);
                    when(mockDiscoveryClient.getInstances(anyString())).thenReturn(new ArrayList<>());

                    verifyError(mockWriter, HttpStatus.NOT_FOUND);
                }

                @Test
                void whenQueryRoutedServices() throws IOException {
                    when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);

                    PrintWriter mockWriter = mock(PrintWriter.class);
                    when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);

                    List<ServiceInstance> serviceInstances = new ArrayList<>();
                    serviceInstances.add(mock(ServiceInstance.class));
                    when(mockDiscoveryClient.getInstances(SERVICE_ID)).thenReturn(serviceInstances);

                    verifyError(mockWriter, HttpStatus.NOT_FOUND);
                }

                @Test
                void whenQueryRoutedService() throws IOException {
                    when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);

                    PrintWriter mockWriter = mock(PrintWriter.class);
                    when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);

                    List<ServiceInstance> serviceInstances = new ArrayList<>();
                    serviceInstances.add(mock(ServiceInstance.class));
                    when(mockDiscoveryClient.getInstances(SERVICE_ID)).thenReturn(serviceInstances);

                    RoutedServices mockRoutedServices = mock(RoutedServices.class);
                    underTest.addRoutedServices(SERVICE_ID, mockRoutedServices);

                    verifyError(mockWriter, HttpStatus.NOT_FOUND);
                }
            }

            private void verifyError(PrintWriter writer, HttpStatus expectedStatus) throws IOException {
                SseEmitter result = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
                verify(writer).print(anyString());
                verify(mockHttpServletResponse).setStatus(expectedStatus.value());
                assertThat(result, is(nullValue()));
            }
        }

        @Nested
        class GivenService_thenUseConsumers {
            @Test
            void givenInsecureService_thenHttpProtocolUsed() throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);
                mockServiceInstance(false);

                verifyConsumerUsed();
                verify(underTest).getSseStream(URL_INSECURE + ENDPOINT);
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void givenServiceUrlFormat(boolean serviceUrlEndingSlash) throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);
                mockServiceInstance(true, serviceUrlEndingSlash);

                verifyConsumerUsed();
                verify(underTest).getSseStream(URL_SECURE + ENDPOINT);
            }

            @ParameterizedTest(name = "givenEndpoint {0}")
            @MethodSource("org.zowe.apiml.zaas.sse.ServerSentEventProxyHandlerTest#endpoints")
            void givenEndpoint(String endpoint, String expectedEndpoint) throws IOException {
                when(mockHttpServletRequest.getRequestURI()).thenReturn(endpoint);
                mockServiceInstance(true);

                verifyConsumerUsed();
                verify(underTest).getSseStream(URL_SECURE + expectedEndpoint);
            }

            @Test
            void givenUriParameters() throws IOException {
                String params = "param=123";
                when(mockHttpServletRequest.getRequestURI()).thenReturn(GATEWAY_PATH);
                when(mockHttpServletRequest.getQueryString()).thenReturn(params);
                mockServiceInstance(true);

                verifyConsumerUsed();
                verify(underTest).getSseStream(URL_SECURE + ENDPOINT + "?" + params);
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
            void givenIOExceptionWhenSendContent_thenCompleteWithError() throws IOException {
                SseEmitter mockEmitter = mock(SseEmitter.class);
                Consumer<ServerSentEvent<String>> result = underTest.consumer(mockEmitter);

                IOException error = new IOException("error");
                doThrow(error).when(mockEmitter).send(anyString());

                ServerSentEvent<String> event = ServerSentEvent.builder("event").build();
                result.accept(event);

                verify(mockEmitter).completeWithError(error);
            }
        }

        private void mockServiceInstance(boolean isSecure) {
            mockServiceInstance(isSecure, true);
        }

        private void mockServiceInstance(boolean isSecure, boolean serviceUrlEndWithSlash) {
            RoutedServices mockRoutedServices = mock(RoutedServices.class);
            RoutedService mockRoutedService = mock(RoutedService.class);
            when(mockRoutedService.getServiceUrl()).thenReturn(SERVICE_URL + (serviceUrlEndWithSlash ? "/" : ""));
            when(mockRoutedServices.findServiceByGatewayUrl("sse/" + MAJOR_VERSION)).thenReturn(mockRoutedService);
            underTest.addRoutedServices(SERVICE_ID, mockRoutedServices);

            ServiceInstance serviceInstance = mock(ServiceInstance.class);
            when(serviceInstance.getHost()).thenReturn(HOST);
            when(serviceInstance.getPort()).thenReturn(PORT);
            when(serviceInstance.isSecure()).thenReturn(isSecure);

            List<ServiceInstance> serviceInstances = new ArrayList<>();
            serviceInstances.add(serviceInstance);
            when(mockDiscoveryClient.getInstances(SERVICE_ID)).thenReturn(serviceInstances);
        }

        private void verifyConsumerUsed() throws IOException {
            SseEmitter emitter = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
            assertThat(emitter, is(not(nullValue())));
            verify(underTest).consumer(emitter);
        }
    }

    private static Stream<Arguments> endpoints() {
        return Stream.of(
            Arguments.of(GATEWAY_PATH, ENDPOINT),
            Arguments.of(GATEWAY_PATH + "anotherendpoint", ENDPOINT + "anotherendpoint"),
            Arguments.of(GATEWAY_PATH + "anotherendpoint/", ENDPOINT + "anotherendpoint/"),
            Arguments.of("/serviceid/sse/v1", "/"),
            Arguments.of("/serviceid/sse/v1/", "/")
        );
    }
}
