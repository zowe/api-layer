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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

class PageRedirectionFilterFactoryTest {

    private GatewayClient gatewayClient;
    private EurekaClient eurekaClient;
    private final EurekaMetadataParser eurekaMetadataParser = new EurekaMetadataParser();
    private static final String GW_HOSTNAME = "gateway";
    private static final String GW_PORT = "10010";
    private static final String GW_SCHEME = "https";
    private static final String GW_BASE_URL = GW_SCHEME + "://" + GW_HOSTNAME + ":" + GW_PORT;
    private final ServiceAddress serviceAddress = ServiceAddress.builder()
        .scheme(GW_SCHEME).hostname(GW_HOSTNAME + ":" + GW_PORT).build();

    @BeforeEach
    void setUp() {
        gatewayClient = mock(GatewayClient.class);
        eurekaClient = mock(EurekaClient.class);
    }

    private void commonSetup(PageRedirectionFilterFactory factory, ServerWebExchange exchange, ServerHttpResponse res, GatewayFilterChain chain, boolean isAttlsEnabled) {
        ReflectionTestUtils.setField(factory, "isAttlsEnabled", isAttlsEnabled);
        when(res.getStatusCode()).thenReturn(HttpStatusCode.valueOf(HttpStatus.SC_MOVED_PERMANENTLY));
        when(exchange.getResponse()).thenReturn(res);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(gatewayClient.isInitialized()).thenReturn(true);
        when(gatewayClient.getGatewayConfigProperties()).thenReturn(serviceAddress);
    }

    private PageRedirectionFilterFactory.Config createConfig() {
        var config = new PageRedirectionFilterFactory.Config();
        config.setInstanceId("instanceId");
        config.setServiceId("GATEWAY");
        return config;
    }

    private void setupInstanceInfo() {
        var instanceInfo = mock(InstanceInfo.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        when(instanceInfo.getMetadata()).thenReturn(metadata);
        when(eurekaClient.getInstancesById("instanceId")).thenReturn(Collections.singletonList(instanceInfo));
    }

    @Nested
    class GivenValidUrl {

        @Test
        void whenNoAttls_thenAddRedirectionUrl() {
            var expectedUrl = GW_BASE_URL + "/gateway/api/v1/api/v1/redirected_url";
            var factory = new PageRedirectionFilterFactory(eurekaClient, eurekaMetadataParser, gatewayClient);
            var chain = mock(GatewayFilterChain.class);
            var exchange = mock(ServerWebExchange.class);
            var res = mock(ServerHttpResponse.class);
            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.singletonList("https://localhost:10010/api/v1/redirected_url"));
            when(res.getHeaders()).thenReturn(header);

            commonSetup(factory, exchange, res, chain, false);
            setupInstanceInfo();
            var config = createConfig();

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertEquals(expectedUrl, res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }

        @Test
        void whenAttls_thenAddRedirectionUrl() {
            var expectedUrl = GW_BASE_URL + "/gateway/api/v1/api/v1/redirected_url";
            var factory = new PageRedirectionFilterFactory(eurekaClient, eurekaMetadataParser, gatewayClient);
            var chain = mock(GatewayFilterChain.class);
            var exchange = mock(ServerWebExchange.class);
            var res = mock(ServerHttpResponse.class);
            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.singletonList("http://localhost:10010/api/v1/redirected_url"));
            when(res.getHeaders()).thenReturn(header);

            commonSetup(factory, exchange, res, chain, true);
            setupInstanceInfo();
            var config = createConfig();

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertEquals(expectedUrl, res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }

    @Nested
    class GivenMissingGwConfig {
        @Test
        void thenDoNotTransform() {
            var expectedUrl = GW_BASE_URL + "http://localhost:10010/api/v1/redirected_url";
            var factory = new PageRedirectionFilterFactory(eurekaClient, eurekaMetadataParser, gatewayClient);
            var chain = mock(GatewayFilterChain.class);
            var exchange = mock(ServerWebExchange.class);
            var res = mock(ServerHttpResponse.class);
            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.singletonList(expectedUrl));
            when(res.getHeaders()).thenReturn(header);
            commonSetup(factory, exchange, res, chain, false);
            setupInstanceInfo();
            var config = createConfig();
            when(gatewayClient.isInitialized()).thenReturn(false);

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertEquals(expectedUrl, res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }

    @Nested
    class GivenNullUrl {
        @Test
        void thenDoNotTransform() {
            var factory = new PageRedirectionFilterFactory(eurekaClient, eurekaMetadataParser, gatewayClient);
            var chain = mock(GatewayFilterChain.class);
            var exchange = mock(ServerWebExchange.class);
            var res = mock(ServerHttpResponse.class);
            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.emptyList());
            when(res.getHeaders()).thenReturn(header);
            commonSetup(factory, exchange, res, chain, false);
            setupInstanceInfo();
            var config = createConfig();

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertNull(res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }

    @Nested
    class GivenDifferentResponseStatusCode {
        @Test
        void thenDoNotTransform() {
            var factory = new PageRedirectionFilterFactory(eurekaClient, eurekaMetadataParser, gatewayClient);
            var chain = mock(GatewayFilterChain.class);
            var exchange = mock(ServerWebExchange.class);
            var res = mock(ServerHttpResponse.class);
            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.emptyList());
            when(res.getHeaders()).thenReturn(header);
            commonSetup(factory, exchange, res, chain, false);
            when(res.getStatusCode()).thenReturn(HttpStatusCode.valueOf(HttpStatus.SC_CONTINUE));

            setupInstanceInfo();
            var config = createConfig();

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertNull(res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }
}

