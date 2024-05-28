/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.filters.pre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.gzip.GZipResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerServiceGZipFilterTest {

    DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    final String SERVICE_WITH_COMPRESSION = "testclient";
    final String SERVICE_WITHOUT_COMPRESSION = "testclient2";
    Map<String, String> metadata = new HashMap<>();
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletRequest request = new MockHttpServletRequest();
    PerServiceGZipFilter filter = null;
    ServiceInstance instance;
    ServiceInstance instanceNoCompression;
    List<ServiceInstance> instances;
    List<ServiceInstance> instancesWithoutCompression;

    @BeforeEach
    void setup() {
        metadata = new HashMap<>();
        metadata.put("apiml.response.compress", "true");
        instance = mock(ServiceInstance.class);
        instanceNoCompression = mock(ServiceInstance.class);
        instances = Collections.singletonList(instance);
        when(instance.getMetadata()).thenReturn(metadata);
        instancesWithoutCompression = Collections.singletonList(instanceNoCompression);
        when(instanceNoCompression.getMetadata()).thenReturn(Collections.emptyMap());
    }

    @Nested
    class ClientAcceptsCompression {
        @BeforeEach
        void setup() {
            request.addHeader("Accept-Encoding", "gzip;bz");
        }


        @Nested
        class ServiceAcceptsGZip {
            private String url = "/" + SERVICE_WITH_COMPRESSION + "/api/v1";

            @Nested
            class OnAllPaths {
                @BeforeEach
                void setup() {
                    when(discoveryClient.getInstances(SERVICE_WITH_COMPRESSION)).thenReturn(instances);
                    filter = new PerServiceGZipFilter(discoveryClient);
                    request.setRequestURI(url);
                }

                @Test
                void whenResponseIsEmpty_thenLengthIsZero() throws ServletException, IOException {                    
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    filter.doFilterInternal(request, response, chain);
                    assertEquals(0, response.getContentLength());
                }

                @Test
                void whenResponseContainsData_thenCompress() throws ServletException, IOException {
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    filter.doFilterInternal(request, response, (request, response1) -> {
                        GZipResponseWrapper gzipWrapper = (GZipResponseWrapper) response1;
                        gzipWrapper.getOutputStream().write("Hello worlds".getBytes());
                        gzipWrapper.getOutputStream().flush();
                    });
                    assertEquals("gzip", response.getHeader("Content-Encoding"));
                }

                @Test
                void whenStatusIsNoContent_thenContentIsNotCompressed() throws ServletException, IOException {
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    filter.doFilterInternal(request, response, (request, response1) -> {
                        GZipResponseWrapper gzipWrapper = (GZipResponseWrapper) response1;
                        gzipWrapper.setStatus(204);
                    });
                    assertNull(response.getHeader("Content-Encoding"));
                }
            }

            @Nested
            class OnCompressedPath {
                @BeforeEach
                void setup() {
                    request.setRequestURI("/" + SERVICE_WITH_COMPRESSION + "/api/v1/compressed");
                    metadata.put("apiml.response.compressRoutes", "/**/compressed,/api/v1/,**/" + SERVICE_WITH_COMPRESSION + "/comp2ress");

                    when(discoveryClient.getInstances(SERVICE_WITH_COMPRESSION)).thenReturn(instances);
                    filter = new PerServiceGZipFilter(discoveryClient);
                }

                @Test
                void whenResponseContainsData_thenCompress() throws ServletException, IOException {
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    filter.doFilterInternal(request, response, (request, response1) -> {
                        GZipResponseWrapper gzipWrapper = (GZipResponseWrapper) response1;
                        gzipWrapper.getOutputStream().write("Hello worlds".getBytes());
                        gzipWrapper.getOutputStream().flush();
                    });
                    assertEquals("gzip", response.getHeader("Content-Encoding"));
                }
            }

            @Nested
            class OnNonCompressedPath {
                @BeforeEach
                void setup() {
                    request.setRequestURI("/api/v1/" + SERVICE_WITH_COMPRESSION + "/nocompression");
                    metadata.put("apiml.response.compressRoutes", "/**/compressed,/api/v1/" + SERVICE_WITH_COMPRESSION + "/comp2ress");

                    when(discoveryClient.getInstances(SERVICE_WITH_COMPRESSION)).thenReturn(instances);
                    filter = new PerServiceGZipFilter(discoveryClient);
                }


                @Test
                void whenRouteNotWithinEnabled_thenContentIsNotCompressed() throws ServletException, IOException {
                    MockHttpServletResponse response = new MockHttpServletResponse();

                    filter.doFilterInternal(request, response, (request, response1) -> {
                        response1.getOutputStream().write("Hello worlds".getBytes());
                        response1.getOutputStream().flush();
                    });
                    assertThat(response.getHeader("Content-Encoding"), is(nullValue()));
                }
            }
        }

        @Nested
        class ServiceDoesntAcceptGZip {

            @BeforeEach
            void setup() {
                request.setRequestURI("/api/v1/" + SERVICE_WITHOUT_COMPRESSION);
                filter = new PerServiceGZipFilter(discoveryClient);
                when(discoveryClient.getInstances(SERVICE_WITHOUT_COMPRESSION)).thenReturn(instancesWithoutCompression);
            }

            @Test
            void dontWrapTheResponse() throws ServletException, IOException {
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilterInternal(request, response, (request, response1) ->
                    assertNotEquals(GZipResponseWrapper.class, response1.getClass()));
            }
        }
    }

    @Nested
    class ClientDoesntAcceptCompression {
        @BeforeEach
        void setUp() {
            when(discoveryClient.getInstances(SERVICE_WITH_COMPRESSION)).thenReturn(instances);
            filter = new PerServiceGZipFilter(discoveryClient);
            request.setRequestURI("/api/v1/" + SERVICE_WITHOUT_COMPRESSION);
        }

        @Test
        void theCompressionDoesntHappen() throws IOException, ServletException {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, (request, response1) ->
                assertNotEquals(GZipResponseWrapper.class, response1.getClass()));

        }
    }

    @Test
    void whenNoInstancesAvailable_thenDoNotWrapResponse() throws ServletException, IOException {
        request.setRequestURI("/api/v1/" + SERVICE_WITHOUT_COMPRESSION);
        filter = new PerServiceGZipFilter(discoveryClient);
        when(discoveryClient.getInstances(SERVICE_WITHOUT_COMPRESSION)).thenReturn(new ArrayList<>());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, (request, response1) ->
            assertNotEquals(GZipResponseWrapper.class, response1.getClass()));
    }

}
