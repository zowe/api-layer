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

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

class PageRedirectionFilterFactoryTest {

    public static final String DISCOVERABLECLIENT = "DISCOVERABLECLIENT";
    public static final String GATEWAY = CoreService.GATEWAY.toString();
    private GatewayClient gatewayClient;
    private DiscoveryClient discoveryClient;
    private static final String GW_HOSTNAME = "gateway";
    private static final String GW_PORT = "10010";
    private static final String GW_SCHEME = "https";
    private static final String GW_BASE_URL = GW_SCHEME + "://" + GW_HOSTNAME + ":" + GW_PORT;
    private final ServiceAddress serviceAddress = ServiceAddress.builder()
        .scheme(GW_SCHEME).hostname(GW_HOSTNAME + ":" + GW_PORT).build();

    PageRedirectionFilterFactory factory;
    GatewayFilterChain chain;
    ServerWebExchange exchange;
    ServerHttpResponse res;
    ServiceInstance serviceInstance;

    @BeforeEach
    void setUp() {
        gatewayClient = mock(GatewayClient.class);
        discoveryClient = mock(DiscoveryClient.class);
        factory = new PageRedirectionFilterFactory(gatewayClient, discoveryClient);

        chain = mock(GatewayFilterChain.class);
        exchange = mock(ServerWebExchange.class);
        res = mock(ServerHttpResponse.class);
        serviceInstance = mock(ServiceInstance.class);
        when(gatewayClient.getGatewayConfigProperties()).thenReturn(serviceAddress);
        when(gatewayClient.isInitialized()).thenReturn(true);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(res.getStatusCode()).thenReturn(HttpStatusCode.valueOf(HttpStatus.SC_MOVED_PERMANENTLY));
        when(exchange.getResponse()).thenReturn(res);
    }


    private PageRedirectionFilterFactory.Config createConfig(String serviceId) {
        var config = new PageRedirectionFilterFactory.Config();
        config.setInstanceId("localhost:" + serviceId.toLowerCase() + ":10012");
        config.setServiceId(serviceId);
        config.setGatewayUrl("api/v1");
        config.setServiceUrl("/discoverableclient");
        return config;
    }

    private void mockServiceInstance(String serviceId) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        when(serviceInstance.getMetadata()).thenReturn(metadata);
        when(serviceInstance.getInstanceId()).thenReturn("instanceId");
        when(serviceInstance.getServiceId()).thenReturn(serviceId);
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(10010);
        when(discoveryClient.getInstances(serviceId)).thenReturn(new ArrayList<>(Collections.singletonList(serviceInstance)));
        when(discoveryClient.getServices()).thenReturn(Collections.singletonList(serviceId));
    }

    void mockLocationHeaderResponse(String url) {
        var header = new HttpHeaders();
        header.put(HttpHeaders.LOCATION, Collections.singletonList(url));
        when(res.getHeaders()).thenReturn(header);
    }

    @Nested
    class GivenValidUrl {

        static Stream<Arguments> locationUrls() {
            return Stream.of(
                Arguments.of("/discoverableclient/api/v1/login?redirected_url=%2Fsome%2Fpath", "https://localhost:10010/discoverableclient/login?redirected_url=%2Fsome%2Fpath", false),
                Arguments.of("discoverableclient/api/v1/login?redirected_url=%2Fsome%2Fpath", "discoverableclient/api/v1/login?redirected_url=%2Fsome%2Fpath", false),
                Arguments.of("http://localhost:10010/api/v1/redirected_url?arg=1&arg=2", "http://localhost:10010/api/v1/redirected_url?arg=1&arg=2", true),
                Arguments.of("%2Fdiscoverableclient%2Fapi%2Fv1%2Frequest", "%2Fdiscoverableclient%2Fapi%2Fv1%2Frequest", true),
                Arguments.of("api/request", "api/request", true),
                Arguments.of("//localhost:10010/api/v1/request", "//localhost:10010/api/v1/request", true),
                Arguments.of("/discoverableclient/api/v1/api/v1/redirected_url?arg=1&arg=2", "http://localhost:10010/discoverableclient/api/v1/redirected_url?arg=1&arg=2", true)

            );
        }

        @ParameterizedTest
        @MethodSource(value = "locationUrls")
        void whenNoAttls_thenAddRedirectionUrl(String expectedUrl, String originalUrl, boolean attlsEnabled) {
            ReflectionTestUtils.setField(factory, "isServerAttlsEnabled", attlsEnabled);

            mockLocationHeaderResponse(originalUrl);
            var config = createConfig(DISCOVERABLECLIENT);

            GatewayFilter gatewayFilter = factory.apply(config);
            StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .thenAwait()
                .expectComplete()
                .verify();
            assertEquals(expectedUrl, res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }

        @Test
        void whenTargetInstanceIsGateway_thenDoNotUpdate() {
            var url = "https://localhost:10010/discoverableclient/api/v1/redirected_url?arg=1&arg=2";
            mockLocationHeaderResponse(url);
            var config = createConfig(GATEWAY);
            mockServiceInstance(GATEWAY);
            GatewayFilter gatewayFilter = factory.apply(config);
            StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .thenAwait()
                .expectComplete()
                .verify();
            assertEquals(url, res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }

    }

    @Nested
    class GivenMissingGwConfig {

        @Test
        void thenDoNotTransform() {
            var expectedUrl = GW_BASE_URL + "/api/v1/redirected_url";

            mockLocationHeaderResponse(expectedUrl);
            mockServiceInstance(DISCOVERABLECLIENT);
            var config = createConfig(DISCOVERABLECLIENT);
            when(gatewayClient.isInitialized()).thenReturn(false);

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertEquals(expectedUrl, res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }

    @Nested
    class GivenNullUrl {
        @Test
        void thenDoNotTransform() {

            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.emptyList());
            when(res.getHeaders()).thenReturn(header);
            var config = createConfig(DISCOVERABLECLIENT);

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertNull(res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }

    @Nested
    class GivenDifferentResponseStatusCode {
        @Test
        void thenDoNotTransform() {

            var header = new HttpHeaders();
            header.put(HttpHeaders.LOCATION, Collections.emptyList());
            when(res.getHeaders()).thenReturn(header);
            when(res.getStatusCode()).thenReturn(HttpStatusCode.valueOf(HttpStatus.SC_CONTINUE));

            mockServiceInstance(DISCOVERABLECLIENT);
            var config = createConfig(DISCOVERABLECLIENT);

            StepVerifier.create(factory.apply(config).filter(exchange, chain)).expectComplete().verify();
            assertNull(res.getHeaders().getFirst(HttpHeaders.LOCATION));
        }
    }
}

