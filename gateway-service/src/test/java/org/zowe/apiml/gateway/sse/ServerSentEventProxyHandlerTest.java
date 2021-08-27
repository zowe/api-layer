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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
class ServerSentEventProxyHandlerTest {
    private ServerSentEventProxyHandler underTest;
    private DiscoveryClient mockDiscoveryClient;
    private HttpServletRequest mockHttpServletRequest;
    private HttpServletResponse mockHttpServletResponse;

    @BeforeEach
    public void setup() {
        mockHttpServletRequest = mock(HttpServletRequest.class);
        mockHttpServletResponse = mock(HttpServletResponse.class);

        mockDiscoveryClient = mock(DiscoveryClient.class);
        underTest = new ServerSentEventProxyHandler(mockDiscoveryClient);
    }

    @Nested
    class whenGetEmitter {
        @Nested
        class givenUriParts {
            @Test
            void givenNoUriParts_thenReturnEmitter() throws IOException {
                mockUriParts(null, null);
                SseEmitter result = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
                assertThat(result.getTimeout(), is(-1L));
            }

            // more testing of getUriParts
        }

        @Test
        void givenNoServiceInstances_thenReturnNull() throws IOException {
            mockUriParts("uri/with/at/least/five/parts", new HashMap<>());

            PrintWriter mockWriter = mock(PrintWriter.class);
            when(mockDiscoveryClient.getInstances(anyString())).thenReturn(new ArrayList<>());
            when(mockHttpServletResponse.getWriter()).thenReturn(mockWriter);

            SseEmitter result = underTest.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
            assertThat(result, is(nullValue()));
            verify(mockWriter, times(1)).print(anyString());
        }

        @Test
        void givenService_thenForwardEvents() throws IOException {
            ServerSentEventProxyHandler spy = Mockito.spy(underTest);
            doNothing().when(spy).forwardEvents(any(), any());
            mockUriParts("/uri/with/at/least/five/parts", new HashMap<>());

            List<ServiceInstance> serviceInstances = new ArrayList<>();
            serviceInstances.add(mock(ServiceInstance.class));
            when(mockDiscoveryClient.getInstances(anyString())).thenReturn(serviceInstances);

            SseEmitter result = spy.getEmitter(mockHttpServletRequest, mockHttpServletResponse);
            verify(spy).forwardEvents(any(), any());
            assertThat(result, is(not(nullValue())));
        }

        @Test
        void givenSameUrl_thenStillForwardEvents() throws IOException {
            // ...
        }
    }

    private void mockUriParts(String uri, Map<String, String[]> parameters) {
        when(mockHttpServletRequest.getRequestURI()).thenReturn(uri);
        when(mockHttpServletRequest.getParameterMap()).thenReturn(parameters);
    }
}
