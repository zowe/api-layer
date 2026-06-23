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
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link AbstractAuthSchemeFactory#cleanHeadersOnAuthFail(ServerWebExchange, String)}
 * focusing on the strict scheme enforcement feature.
 */
class AbstractAuthSchemeFactoryTest {

    private static final String BASIC_AUTH_VALUE = "Basic dXNlcjpwYXNz";
    private static final String BEARER_AUTH_VALUE = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.xxx";
    private static final String ERROR_MESSAGE = "auth failed";

    private OtelRequestContext otelContext;

    @BeforeEach
    void setUpOtelContext() {
        // OtelRequestContext has a static holder; just ensure it's initialized per test
        otelContext = null;
    }

    /**
     * Create an exchange with an Authorization header and setup OTEL context.
     */
    private ServerWebExchange createExchange(String authorizationValue) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
            .header(HttpHeaders.AUTHORIZATION, authorizationValue)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        otelContext = spy(OtelRequestContext.of(exchange));
        exchange.getAttributes().put("apiml.serviceId", "test-service");
        return exchange;
    }

    /**
     * Create a spy of AbstractAuthSchemeFactory with the given scheme and enforcement setting.
     */
    private AbstractAuthSchemeFactory<?, ?> createFactory(AuthenticationScheme scheme, boolean strictEnforcement) {
        AbstractAuthSchemeFactory<?, ?> factory = spy(AbstractAuthSchemeFactory.class);
        doReturn(scheme).when(factory).getAuthenticationScheme();
        ReflectionTestUtils.setField(factory, "strictSchemeEnforcement", strictEnforcement);
        return factory;
    }

    // ── Test 1: strictSchemeEnforcement=true + non-bypass scheme → Basic stripped ──

    @Test
    void givenStrictEnforcementAndNonBypassScheme_whenBasicAuthHeader_thenAuthorizationRemoved() {
        ServerWebExchange exchange = createExchange(BASIC_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.HTTP_BASIC_PASSTICKET, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Authorization header should be removed under strict enforcement");
        assertNotNull(result.getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER),
            "X-Zowe-Auth-Failure header should be set");
    }

    // ── Test 2: strictSchemeEnforcement=false (default) → Basic preserved ──

    @Test
    void givenStrictEnforcementDisabled_whenBasicAuthHeader_thenAuthorizationPreserved() {
        ServerWebExchange exchange = createExchange(BASIC_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.HTTP_BASIC_PASSTICKET, false);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Authorization header should be preserved when strict enforcement is disabled");
        assertEquals(BASIC_AUTH_VALUE, result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    // ── Test 3: scheme is BYPASS → Basic always preserved ──

    @Test
    void givenStrictEnforcementAndBypassScheme_whenBasicAuthHeader_thenAuthorizationPreserved() {
        ServerWebExchange exchange = createExchange(BASIC_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.BYPASS, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Authorization header should be preserved for BYPASS scheme even with strict enforcement");
        assertEquals(BASIC_AUTH_VALUE, result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    // ── Test 4: Authorization: Bearer → never stripped ──

    @Test
    void givenStrictEnforcementAndNonBypassScheme_whenBearerAuthHeader_thenAuthorizationPreserved() {
        ServerWebExchange exchange = createExchange(BEARER_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.ZOWE_JWT, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Bearer Authorization should never be stripped");
        assertEquals(BEARER_AUTH_VALUE, result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    // ── Test 5: x-zowe-auth-failure still set after stripping ──

    @Test
    void givenStrictEnforcement_whenBasicAuthHeaderStripped_thenAuthFailureHeaderSet() {
        ServerWebExchange exchange = createExchange(BASIC_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.HTTP_BASIC_PASSTICKET, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertEquals(ERROR_MESSAGE, result.getHeaders().getFirst(ApimlConstants.AUTH_FAIL_HEADER),
            "X-Zowe-Auth-Failure header should contain the error message");
    }

    // ── Test 6: scheme is null → no enforcement ──

    @Test
    void givenStrictEnforcementAndNullScheme_whenBasicAuthHeader_thenAuthorizationPreserved() {
        ServerWebExchange exchange = createExchange(BASIC_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(null, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Authorization header should be preserved when scheme is null");
        assertEquals(BASIC_AUTH_VALUE, result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    // ── Test 7: case-insensitive match ("basic " lowercase) ──

    @Test
    void givenStrictEnforcement_whenLowercaseBasicAuthHeader_thenAuthorizationRemoved() {
        ServerWebExchange exchange = createExchange("basic dXNlcjpwYXNz");
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.HTTP_BASIC_PASSTICKET, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE);

        assertNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Authorization header should be removed for case-insensitive 'basic ' match");
    }

    // ── Test 8: serviceId overload passes through ──

    @Test
    void givenServiceIdOverload_whenCleanHeadersOnAuthFail_thenBehaviorIdentical() {
        ServerWebExchange exchange = createExchange(BASIC_AUTH_VALUE);
        AbstractAuthSchemeFactory<?, ?> factory = createFactory(AuthenticationScheme.HTTP_BASIC_PASSTICKET, true);

        ServerHttpRequest result = factory.cleanHeadersOnAuthFail(exchange, ERROR_MESSAGE, "test-service");

        assertNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION),
            "Authorization header should be removed via 3-param overload");
        assertEquals(ERROR_MESSAGE, result.getHeaders().getFirst(ApimlConstants.AUTH_FAIL_HEADER));
    }

}
