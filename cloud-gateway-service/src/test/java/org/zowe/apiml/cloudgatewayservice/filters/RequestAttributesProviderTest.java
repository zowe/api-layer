/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RequestAttributesProviderTest {

    @Test
    void givenRequestWithAttributes_whenFilter_thenCopyJustMissing() {
        Map<String, Object> requestFacadeAttributes = new HashMap<>();
        RequestFacade requestFacade = new RequestFacade(new Request(null));

        GatewayFilterChain filterChain = mock(GatewayFilterChain.class);
        MockServerHttpRequest request = spy(MockServerHttpRequest.get("/").build());
        doReturn(requestFacade).when(request).getNativeRequest();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        exchange.getAttributes().put("A", "Aexchange");
        exchange.getAttributes().put("B", "Bexchange");

        requestFacade.setAttribute("B", "Bservlet");
        requestFacade.setAttribute("C", "Cservlet");

        new RequestAttributesProvider().filter(exchange, filterChain);

        assertEquals("Aexchange", exchange.getAttribute("A"));
        assertEquals("Bexchange", exchange.getAttribute("B"));
        assertEquals("Cservlet", exchange.getAttribute("C"));
    }

    @Test
    void givenRequestAttributesProvider_whenOrder_thenIsTheFirstOne() {
        assertEquals(Integer.MIN_VALUE, new RequestAttributesProvider().getOrder());
    }

}