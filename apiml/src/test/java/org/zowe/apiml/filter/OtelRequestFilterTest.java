/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.product.opentelemetry.OtelRequestContext;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.product.opentelemetry.OtelRequestContext.OTEL_CONTEXT;

class OtelRequestFilterTest {

    @ParameterizedTest(name = "When basePath is `{1}` on port {0} then mark it as `{2}` service")
    @CsvSource({
        "10010,/,gateway,localhost:gateway:10010",
        "10010,/application,gateway,localhost:gateway:10010",
        "10010,/application/health,gateway,localhost:gateway:10010",
        "10010,/gateway,gateway,localhost:gateway:10010",
        "10010,/gateway/api/v1/auth/login,gateway,localhost:gateway:10010",
        "10010,/v3/api-docs,gateway,localhost:gateway:10010",
        "10010,/v3/api-docs/gateway,gateway,localhost:gateway:10010",
        "10010,/v3/api-docs/apicatalog,apicatalog,localhost:apicatalog:10010",
        "10010,/v3/api-docs/caching,cachingservice,localhost:cachingservice:10010",
        // there is no ZAAS anymore as a separated component
        "10010,/v3/api-docs/zaas,gateway,localhost:gateway:10010",
        "10010,/v3/api-docs/x,gateway,localhost:gateway:10010",
        "10010,/v3/api-docs,gateway,localhost:gateway:10010",
        // treat as an unknown service
        "10010,/v3,v3,",
        "10010,/v3/api-doc,v3,",
        "10010,/images,gateway,localhost:gateway:10010",
        "10010,/images/a/b/x.png,gateway,localhost:gateway:10010",
        "10010,/apicatalog,apicatalog,localhost:apicatalog:10010",
        "10010,/apicatalog/ui/v1/x,apicatalog,localhost:apicatalog:10010",
        "10010,/cachingservice,cachingservice,localhost:cachingservice:10010",
        "10010,/cachingservice/ui/v1/x,cachingservice,localhost:cachingservice:10010",
        // on GW port in not available eureka endpoint
        "10010,/eureka/apps,eureka,",
        "10010,/,gateway,localhost:gateway:10010",
        "10010,/service1/api/v1,service1,",

        // discovery port
        "10011,/eureka/apps,discovery,localhost:discovery:10011",
        "10011,/,discovery,localhost:discovery:10011",
        "10011,/x/y/z,discovery,localhost:discovery:10011"
    })
    void givenBasePath_whenAnalyzeRequest_thenDetectServiceId(int port, String basePath, String serviceId, String instanceId) {
        var filter = new OtelRequestFilter();
        ReflectionTestUtils.setField(filter, "discoveryPort", 10011);
        ReflectionTestUtils.setField(filter, "hostname", "localhost");

        var request = MockServerHttpRequest.get(basePath).localAddress(InetSocketAddress.createUnresolved("localhost", port)).build();
        var exchange = MockServerWebExchange.from(request);
        var otelContext = OtelRequestContext.of(exchange);
        filter.setDefaults(exchange, otelContext);

        var attributes = ((AttributesBuilder) ReflectionTestUtils.getField(otelContext, "attributesBuilder")).build();
        assertEquals(serviceId, attributes.get(AttributeKey.stringKey("service.id")));
        assertEquals(instanceId, attributes.get(AttributeKey.stringKey("service.instance.id")));
        assertEquals(basePath, attributes.get(AttributeKey.stringKey("url.path")));
    }

    @Test
    void givenAttls_whenInitialize_thenSetSchemeAsHttps() {
        var filter = new OtelRequestFilter();
        ReflectionTestUtils.setField(filter, "isServerAttlsEnabled", true);

        var request = MockServerHttpRequest.get("http://localhost/").localAddress(InetSocketAddress.createUnresolved("localhost", 10010)).build();
        var exchange = MockServerWebExchange.from(request);
        var otelContext = OtelRequestContext.of(exchange);
        filter.setDefaults(exchange, otelContext);

        var attributes = (AttributesBuilder) ReflectionTestUtils.getField(otelContext, "attributesBuilder");
        assertEquals("https", attributes.build().get(AttributeKey.stringKey("url.scheme")));
    }

    @Test
    void givenNativeTls_whenInitialize_thenSetSchemeAsHttps() {
        var filter = new OtelRequestFilter();

        var request = MockServerHttpRequest.get("http://localhost/").localAddress(InetSocketAddress.createUnresolved("localhost", 10010)).build();
        var exchange = MockServerWebExchange.from(request);
        var otelContext = OtelRequestContext.of(exchange);
        filter.setDefaults(exchange, otelContext);

        var attributes = (AttributesBuilder) ReflectionTestUtils.getField(otelContext, "attributesBuilder");
        assertEquals("http", attributes.build().get(AttributeKey.stringKey("url.scheme")));
    }

    @Test
    void givenMultipleFiltersInPipeline_whenStarted_thenLogOnceResponse() {
        var filter = new OtelRequestFilter();

        var request = MockServerHttpRequest.patch("http://localhost/").localAddress(InetSocketAddress.createUnresolved("localhost", 10010)).build();
        var exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(503));

        OtelRequestContext otelContext = spy(OtelRequestContext.of(exchange));
        exchange.getAttributes().put(OTEL_CONTEXT, otelContext);

        for (int i = 0; i < 3; i++) {
            // call multiple time the same filter
            StepVerifier.create(filter.filter(exchange, (WebFilterChain) exchange2 -> Mono.empty())).verifyComplete();
        }

        var attributes = ((AttributesBuilder) ReflectionTestUtils.getField(otelContext, "attributesBuilder")).build();
        assertEquals("PATCH", attributes.get(AttributeKey.stringKey("http.request.method")));
        assertEquals("503", attributes.get(AttributeKey.stringKey("service.response_code")));
        verify(otelContext, times(1)).issue();
    }

    @Test
    void givenRequest_whenFailed_thenIssueLog() {
        var filter = new OtelRequestFilter();

        var request = MockServerHttpRequest.get("http://localhost/").localAddress(InetSocketAddress.createUnresolved("localhost", 10010)).build();
        var exchange = MockServerWebExchange.from(request);
        var otelContext = spy(OtelRequestContext.of(exchange));
        var chain = (GatewayFilterChain) e -> Mono.error(new RuntimeException("Any exception"));
        exchange.getAttributes().put(OTEL_CONTEXT, otelContext);

        StepVerifier.create(filter.filter(exchange, chain))
            .expectError(RuntimeException.class)
            .verify();

        verify(otelContext, times(1)).issue();
    }

    @Test
    void givenUnknownService_whenNoResourceFound_thenSet404AndErrorType() {
        var filter = new OtelRequestFilter();

        var request = MockServerHttpRequest.get("http://localhost/unknownservice/api/v1/data")
            .localAddress(InetSocketAddress.createUnresolved("localhost", 10010)).build();
        var exchange = MockServerWebExchange.from(request);
        var otelContext = spy(OtelRequestContext.of(exchange));
        exchange.getAttributes().put(OTEL_CONTEXT, otelContext);

        var chain = (GatewayFilterChain) e -> Mono.error(
            new NoResourceFoundException(exchange.getRequest().getURI().getPath()));

        StepVerifier.create(filter.filter(exchange, chain))
            .expectError(NoResourceFoundException.class)
            .verify();

        verify(otelContext, times(1)).statusCode(404);
        verify(otelContext, times(1)).errorType("Service not onboarded");
        verify(otelContext, times(1)).errorMessage("Service unknownservice is not registered in the API ML");
        verify(otelContext, times(1)).issue();
    }

    @Test
    void givenServiceNotAccessible_whenCalled_thenSet503AndErrorType() {
        var filter = new OtelRequestFilter();

        var request = MockServerHttpRequest.get("http://localhost/discoverable-service/api/v1/data")
            .localAddress(InetSocketAddress.createUnresolved("localhost", 10010)).build();
        var exchange = MockServerWebExchange.from(request);
        var otelContext = spy(OtelRequestContext.of(exchange));
        exchange.getAttributes().put(OTEL_CONTEXT, otelContext);

        var chain = (GatewayFilterChain) e -> Mono.error(
            new ServiceNotAccessibleException("Service discoverable-service has no available instances"));

        StepVerifier.create(filter.filter(exchange, chain))
            .expectError(ServiceNotAccessibleException.class)
            .verify();

        verify(otelContext, times(1)).statusCode(503);
        verify(otelContext, times(1)).errorType("Service instance not available");
        verify(otelContext, times(1)).errorMessage("Service discoverable-service has no available instances");
        verify(otelContext, times(1)).issue();
    }

}
