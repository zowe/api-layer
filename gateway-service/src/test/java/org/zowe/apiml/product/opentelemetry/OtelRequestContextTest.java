/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.product.opentelemetry.OtelRequestContext.OTEL_CONTEXT;

class OtelRequestContextTest {

    MockServerHttpRequest request = MockServerHttpRequest.get("/aPath").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    private <T> T getValue(String key, Function<String, AttributeKey<?>> keyMapper) {
        var otelContext = OtelRequestContext.of(exchange);
        var attributesBuilder = (AttributesBuilder) ReflectionTestUtils.getField(otelContext, "attributesBuilder");
        return (T) attributesBuilder.build().get(keyMapper.apply(key));
    }

    private <T> T getValue(String key) {
        return getValue(key, AttributeKey::stringKey);
    }

    @Test
    void givenEmptyExchange_whenCallMethodOf_thenCreateOne() {
        exchange = MockServerWebExchange.from(request);
        assertNull(exchange.getAttributes().get(OTEL_CONTEXT));

        var otelRequestContext = OtelRequestContext.of(exchange);
        assertSame(otelRequestContext, exchange.getAttributes().get(OTEL_CONTEXT));
    }

    @Test
    void givenNonMarkedContext_whenMark_thenOnlyFirstCallReturnsTrue() {
        assertTrue(OtelRequestContext.of(exchange).mark());
        for (int i = 0; i < 3; i++) {
            assertFalse(OtelRequestContext.of(exchange).mark());
        }
    }

    @Test
    void givenOtelContext_whenSetMethod_thenTransformToString() {
        OtelRequestContext.of(exchange).method(HttpMethod.DELETE);
        assertEquals("DELETE", getValue("http.request.method"));
    }

    @Test
    void givenOtelContext_whenSetScheme_thenSetIt() {
        OtelRequestContext.of(exchange).scheme("wss");
        assertEquals("wss", getValue("url.scheme"));
    }

    @Test
    void givenOtelContext_whenSetPath_thenSetIt() {
        OtelRequestContext.of(exchange).path("/a/b/path");
        assertEquals("/a/b/path", getValue("url.path"));
    }

    @Test
    void givenOtelContext_whenSetResponseCode_thenTransformToString() {
        OtelRequestContext.of(exchange).responseCode(204);
        assertEquals("204", getValue("service.response_code"));
    }

    @Test
    void givenOtelContext_whenSetServiceId_thenStoreLowerCase() {
        OtelRequestContext.of(exchange).serviceId("serviceID");
        assertEquals("serviceid", getValue("service.id"));
    }

    @Test
    void givenOtelContext_whenSetInstanceId_thenStoreLowerCase() {
        OtelRequestContext.of(exchange).instanceId("serviceID:Host:Port");
        assertEquals("serviceid:host:port", getValue("service.instance.id"));
    }

    @Test
    void givenOtelContext_whenSetByPassAuthMethod_thenTransformToString() {
        OtelRequestContext.of(exchange).authMethod(AuthenticationScheme.BYPASS);
        assertEquals("bypass", getValue("auth.method"));
    }

    @Test
    void givenOtelContext_whenSetZoweJwtAuthMethod_thenTransformToString() {
        OtelRequestContext.of(exchange).authMethod(AuthenticationScheme.ZOWE_JWT);
        assertEquals("zoweJwt", getValue("auth.method"));
    }

    @Test
    void givenOtelContext_whenAuthenticationFailed_thenStoreFailedStringAsStatus() {
        OtelRequestContext.of(exchange).authenticationFailed();
        assertEquals("FAILED", getValue("auth.status"));
    }

    @Test
    void givenOtelContext_whenauthenticationSuccess_thenStoreOkStringAsStatus() {
        OtelRequestContext.of(exchange).authenticationSuccess();
        assertEquals("OK", getValue("auth.status"));
    }

    @Test
    void givenOtelContext_whenSetUserId_thenStoreUpperCase() {
        OtelRequestContext.of(exchange).userId("userId");
        assertEquals("USERID", getValue("user.id"));
    }

    @Test
    void givenOtelContext_whenSetDistributedUserId_thenStoreAsArray() {
        OtelRequestContext.of(exchange).distributedIds(Arrays.asList("userId1", "userId2"));
        List<String> value = getValue("user.distributed.id", AttributeKey::stringArrayKey);
        assertEquals(2, value.size());
        assertEquals("userId1", value.get(0));
        assertEquals("userId2", value.get(1));
    }

    @Test
    void givenOtelContext_whenToString_thenReturnJson() {
        exchange = MockServerWebExchange.from(request);
        assertEquals("{\"auth.status\":\"OK\",\"user.id\":\"ZWESVR\"}", OtelRequestContext.of(exchange).userId("zwesvr").authenticationSuccess().toString());
    }

    @Test
    void givenInvalidData_whenObjectMapperFails_thenThrowIllegalStateException() throws JsonProcessingException {
        exchange = MockServerWebExchange.from(request);
        var objectMapper = mock(ObjectMapper.class);
        var otelRequestContext = spy(OtelRequestContext.of(exchange));
        var jsonProcessingException = new JsonProcessingException("test") {};

        doReturn(objectMapper).when(otelRequestContext).getObjectMapper();
        doThrow(jsonProcessingException).when(objectMapper).writeValueAsString(any());

        var exception = assertThrows(IllegalStateException.class, otelRequestContext::toString);
        assertSame(jsonProcessingException, exception.getCause());
        assertEquals("Cannot serialize attributes", exception.getMessage());
    }

    @Test
    void givenOtelContext_whenIssue_thenCallOtelLogger() {
        var logger = mock(Logger.class);
        exchange = MockServerWebExchange.from(request);

        var otelRequestContext = spy(OtelRequestContext.of(exchange));
        doReturn(logger).when(otelRequestContext).getOtelLogger();

        otelRequestContext.userId("myUserName").issue();

        verify(logger, times(1)).info("{\"user.id\":\"MYUSERNAME\"}");
    }

}
