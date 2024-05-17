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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RequestAttributesProviderTest {

    private static Stream<Arguments> filterInterface() {
        return Stream.of(
            Arguments.of("WebFilter",(BiConsumer<RequestAttributesProvider, ServerWebExchange>) (f, e) -> f.filter(e, mock(WebFilterChain.class))),
            Arguments.of("GlobalFilter",(BiConsumer<RequestAttributesProvider, ServerWebExchange>) (f, e) -> f.filter(e, mock(GatewayFilterChain.class)))
        );
    }

    @ParameterizedTest(name = "givenRequestWithAttributes_whenFilter_thenCopyJustMissing with {0}")
    @MethodSource("filterInterface")
    void givenRequestWithAttributes_whenFilter_thenCopyJustMissing(String filterName, BiConsumer<RequestAttributesProvider, ServerWebExchange> filter) {
        RequestFacade requestFacade = new RequestFacade(new Request(null));

        GatewayFilterChain filterChain = mock(GatewayFilterChain.class);
        MockServerHttpRequest request = spy(MockServerHttpRequest.get("/").build());
        doReturn(requestFacade).when(request).getNativeRequest();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        exchange.getAttributes().put("A", "Aexchange");
        exchange.getAttributes().put("B", "Bexchange");

        requestFacade.setAttribute("B", "Bservlet");
        requestFacade.setAttribute("C", "Cservlet");

        filter.accept(new RequestAttributesProvider(), exchange);

        assertEquals("Aexchange", exchange.getAttribute("A"));
        assertEquals("Bexchange", exchange.getAttribute("B"));
        assertEquals("Cservlet", exchange.getAttribute("C"));
    }

    @Test
    void givenRequestAttributesProvider_whenOrder_thenIsTheFirstOne() {
        assertEquals(Integer.MIN_VALUE, new RequestAttributesProvider().getOrder());
    }

}