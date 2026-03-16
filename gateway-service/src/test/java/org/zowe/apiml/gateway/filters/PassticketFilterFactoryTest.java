/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

public class PassticketFilterFactoryTest {

    private static final String USER_ID = "userId";

    MockServerHttpRequest request = MockServerHttpRequest.get("/aPath").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    OtelRequestContext otelRequestContext;

    @BeforeEach
    void setup() {
        otelRequestContext = spy(OtelRequestContext.of(exchange));
        exchange.getAttributes().put("otel-context", otelRequestContext);
    }

    @Test
    void givenPassticketFilterFactory_whenProcessEmptyResponse_thenSetHttpBasicPassticketScheme() {
        var passticketFilterFactory = new PassticketFilterFactory(null, null, null);
        passticketFilterFactory.processResponse(exchange, e -> Mono.empty().then(),
            new AbstractAuthSchemeFactory.AuthorizationResponse(null, new TicketResponse())
        );

        verify(otelRequestContext, times(1)).authMethod(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        verify(otelRequestContext, never()).userId(any());
    }

    @Test
    void givenUserInRequest_whenProcessResponse_thenSetIt() {
        var passticketFilterFactory = new PassticketFilterFactory(null, null, null);
        passticketFilterFactory.processResponse(exchange, e -> Mono.empty().then(),
            new AbstractAuthSchemeFactory.AuthorizationResponse(null, new TicketResponse("token", USER_ID, "appName", "ticket"))
        );

        verify(otelRequestContext, times(1)).authMethod(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        verify(otelRequestContext, times(1)).userId(USER_ID);
    }

}
