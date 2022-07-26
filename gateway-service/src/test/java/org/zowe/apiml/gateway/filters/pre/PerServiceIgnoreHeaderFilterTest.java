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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

class PerServiceIgnoreHeaderFilterTest {
    private static final String SERVICE_ID = "serviceid";
    private static final String HEADER1 = "Header1";
    private static final String HEADER2 = "Header2";

    private PerServiceIgnoreHeaderFilter underTest;

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final ProxyRequestHelper proxyRequestHelper = mock(ProxyRequestHelper.class);
    private final ServiceInstance instance = mock(ServiceInstance.class);
    private final Map<String, String> metadata = new HashMap<>();

    @BeforeEach
    void setup() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);

        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.singletonList(instance));
        when(instance.getMetadata()).thenReturn(metadata);
        underTest = new PerServiceIgnoreHeaderFilter(discoveryClient, proxyRequestHelper);
    }

    @Nested
    class WhenIgnoreHeaders {
        @Test
        void givenOneHeaderToIgnore() {
            metadata.put("apiml.headersToIgnore", HEADER1);
            underTest.run();

            String[] ignoreHeaders = {HEADER1};
            verify(proxyRequestHelper).addIgnoredHeaders(ignoreHeaders);
        }

        @Test
        void givenMultipleHeadersToIgnore() {
            metadata.put("apiml.headersToIgnore", HEADER1 + "," + HEADER2);
            underTest.run();

            String[] ignoreHeaders = {HEADER1, HEADER2};
            verify(proxyRequestHelper).addIgnoredHeaders(ignoreHeaders);
        }

        @Test
        void givenMultipleHeadersToIgnoreWithWhitespace() {
            metadata.put("apiml.headersToIgnore", " " + HEADER1 + ", " + HEADER2 + " ");
            underTest.run();

            String[] ignoreHeaders = {HEADER1, HEADER2};
            verify(proxyRequestHelper).addIgnoredHeaders(ignoreHeaders);
        }
    }

    @Nested
    class WhenDontIgnoreHeaders {
        @Test
        void givenNoServiceInstance() {
            when(discoveryClient.getInstances(anyString())).thenReturn(Collections.emptyList());
            underTest.run();

            verify(proxyRequestHelper, never()).addIgnoredHeaders(any());
        }

        @Test
        void givenNullHeadersToIgnore() {
            metadata.put("apiml.headersToIgnore", null);
            underTest.run();

            verify(proxyRequestHelper, never()).addIgnoredHeaders(any());
        }

        @Test
        void givenEmptyHeadersToIgnore() {
            metadata.put("apiml.headersToIgnore", "");
            underTest.run();

            verify(proxyRequestHelper, never()).addIgnoredHeaders(any());
        }
    }
}
