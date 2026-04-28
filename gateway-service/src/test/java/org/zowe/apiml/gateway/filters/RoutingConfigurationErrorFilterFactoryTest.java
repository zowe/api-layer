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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class RoutingConfigurationErrorFilterFactoryTest {

    private static final String MESSAGE = "test message";

    private GatewayFilter filter;

    private MockServerHttpRequest request = MockServerHttpRequest.get("https://localhost/some/url").build();
    private MockServerWebExchange exchange = MockServerWebExchange.from(request);

    @Spy
    private RoutingConfigurationErrorFilterFactory underTest = new RoutingConfigurationErrorFilterFactory(null, null);
    @Spy
    private OtelRequestContext otelContext = OtelRequestContext.of(exchange);

    @BeforeEach
    void init() {
        exchange = MockServerWebExchange.from(request);
        var config = new RoutingConfigurationErrorFilterFactory.Config();
        config.setMessage(MESSAGE);
        config.setAuthenticationScheme("safIdt");
        config.setServiceId("serviceId");

        filter = underTest.apply(config);
    }

    @Test
    void givenConfig_whenApply_thenSetAuthInformationWithoutErrorType() {
        exchange.getAttributes().put(OtelRequestContext.OTEL_CONTEXT, otelContext);

        StepVerifier.create(filter.filter(exchange, e -> Mono.empty())).verifyComplete();

        verify(otelContext).authenticationFailed();
        verify(otelContext).authErrorMessage(MESSAGE);

        verify(otelContext).authMethod(AuthenticationScheme.SAF_IDT);
        verify(underTest).cleanHeadersOnAuthFail(exchange, MESSAGE);
    }

    @Test
    @Disabled("TODO: fix this test") // TODO
    void givenConfig_whenApply_thenSetFailedAuthInformationWithErrorType() {
        exchange.getAttributes().put(OtelRequestContext.OTEL_CONTEXT, otelContext);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        StepVerifier.create(filter.filter(exchange, e -> Mono.empty())).verifyComplete();

        verify(otelContext).authenticationFailed();
        verify(otelContext).authErrorMessage(MESSAGE);
        verify(otelContext).authErrorType(HttpStatus.UNAUTHORIZED.getReasonPhrase());

        verify(otelContext).authMethod(AuthenticationScheme.SAF_IDT);
        verify(underTest).cleanHeadersOnAuthFail(exchange, MESSAGE);
    }

}
