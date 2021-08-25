/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.pre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.gzip.GZipResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerServiceGZipFilterTest {

    DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    final static String SERVICE_WITH_COMPRESSION = "testclient";
    final static String SERVICE_WITHOUT_COMPRESSION = "testclient2";
    static Map<String, String> metadata = new HashMap<>();
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletRequest request = new MockHttpServletRequest();
    PerServiceGZipFilter filter = null;
    static ServiceInstance instance;
    static ServiceInstance instanceNoCompression;
    static List<ServiceInstance> instances;
    static List<ServiceInstance> instancesWithoutCompression;

    static {
        metadata.put("apiml.response.compress", "true");
        instance = mock(ServiceInstance.class);
        instanceNoCompression = mock(ServiceInstance.class);
        instances = Collections.singletonList(instance);
        when(instance.getMetadata()).thenReturn(metadata);
        instancesWithoutCompression = Collections.singletonList(instanceNoCompression);
        when(instanceNoCompression.getMetadata()).thenReturn(Collections.emptyMap());
    }

    @Nested
    class ServiceAcceptsGZip {

        @BeforeEach
        void setup() {
            request.setRequestURI("/api/v1/" + SERVICE_WITH_COMPRESSION);

            when(discoveryClient.getInstances(SERVICE_WITH_COMPRESSION)).thenReturn(instances);
            filter = new PerServiceGZipFilter(discoveryClient);
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
            filter.doFilterInternal(request, response, (request, response1) -> {
                assertNotEquals(GZipResponseWrapper.class, response1.getClass());
            });
        }
    }

    @Test
    void whenNoInstancesAvailable_thenDoNotWrapResponse() throws ServletException, IOException {
        request.setRequestURI("/api/v1/" + SERVICE_WITHOUT_COMPRESSION);
        filter = new PerServiceGZipFilter(discoveryClient);
        when(discoveryClient.getInstances(SERVICE_WITHOUT_COMPRESSION)).thenReturn(new ArrayList<>());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, (request, response1) -> {
            assertNotEquals(GZipResponseWrapper.class, response1.getClass());
        });
    }

}
