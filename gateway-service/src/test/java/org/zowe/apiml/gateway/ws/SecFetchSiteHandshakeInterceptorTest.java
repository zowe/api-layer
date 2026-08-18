/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.zowe.apiml.gateway.filters.pre.SecFetchSitePolicy;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecFetchSiteHandshakeInterceptorTest {

    @Mock
    private SecFetchSitePolicy secFetchSitePolicy;
    @Mock
    private ServletServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private WebSocketHandler webSocketHandler;

    private SecFetchSiteHandshakeInterceptor underTest;

    @BeforeEach
    void setup() {
        underTest = new SecFetchSiteHandshakeInterceptor(secFetchSitePolicy);
        lenient().when(request.getHeaders()).thenReturn(new HttpHeaders());
    }

    @Nested
    class GivenPolicyAllowsRequest {

        @BeforeEach
        void setup() {
            when(secFetchSitePolicy.isAllowed(any(), any())).thenReturn(true);
        }

        @Test
        void thenHandshakeProceeds() {
            boolean result = underTest.beforeHandshake(request, response, webSocketHandler, new HashMap<>());

            assertTrue(result);
            verifyNoInteractions(response);
        }
    }

    @Nested
    class GivenServletBasedHandshake {

        @Test
        void thenTheUnderlyingServletRequestIsHandedToThePolicy() {
            MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/v1/serviceid/path");
            when(secFetchSitePolicy.isAllowed(any(), eq(servletRequest))).thenReturn(true);

            boolean result = underTest.beforeHandshake(new ServletServerHttpRequest(servletRequest), response,
                webSocketHandler, new HashMap<>());

            assertTrue(result);
            verifyNoInteractions(response);
        }
    }

    @Nested
    class GivenPolicyBlocksRequest {

        @BeforeEach
        void setup() {
            when(secFetchSitePolicy.isAllowed(any(), any())).thenReturn(false);
        }

        @Test
        void thenHandshakeIsRejectedWith403() {
            boolean result = underTest.beforeHandshake(request, response, webSocketHandler, new HashMap<>());

            assertFalse(result);
            verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        }
    }

}
