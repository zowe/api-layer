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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingConfigurationErrorFilterFactoryTest {

    private static final String MESSAGE = "test message";

    private RoutingConfigurationErrorFilterFactory underTest;
    private GatewayFilter filter;

    private MockServerHttpRequest request = MockServerHttpRequest.get("https://localhost/some/url").build();
    private MockServerWebExchange exchange = MockServerWebExchange.from(request);

    @BeforeAll
    void init() {
        var config = new RoutingConfigurationErrorFilterFactory.Config();
        config.setMessage(MESSAGE);
        config.setAuthenticationScheme("safIdt");
        config.setServiceId("serviceId");

        underTest = spy(new RoutingConfigurationErrorFilterFactory(null, null));
        filter = underTest.apply(config);
    }

    @Test
    void givenConfig_whenApply_thenSetAuthInformation() {
        var otelContext = spy(OtelRequestContext.of(exchange));
        exchange.getAttributes().put(OtelRequestContext.OTEL_CONTEXT, otelContext);

        filter.filter(exchange, e -> Mono.empty()).block();

        verify(otelContext).authenticationFailed();
        verify(otelContext).authMethod(AuthenticationScheme.SAF_IDT);
        verify(underTest).cleanHeadersOnAuthFail(exchange, MESSAGE);
    }

}
