/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.filters.post;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class PerServiceAddResponseHeadersFilterTest {
    private static final String SERVICE_ID = "serviceId";
    private static final String HEADER1 = "header1";
    private static final String HEADER2 = "header2";
    private static final String HEADER1_VALUE = "value1";
    private static final String HEADER2_VALUE = "value2";
    private static final String HEADER_VALUE_SEPARATOR = ":";
    private static final String HEADER_DELIMITER = ",";
    private static final String METADATA_KEY = "apiml.response.headers";

    private PerServiceAddResponseHeadersFilter underTest;

    private DiscoveryClient discoveryClient;
    private RequestContext context;
    private Map<String, String> metadata;

    @BeforeEach
    void setup() {
        context = spy(new RequestContext());
        context.put(SERVICE_ID_KEY, SERVICE_ID);
        RequestContext.testSetCurrentContext(context);

        metadata = new HashMap<>();
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getMetadata()).thenReturn(metadata);

        discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.singletonList(instance));
        underTest = new PerServiceAddResponseHeadersFilter(discoveryClient);
    }

    @Nested
    class WhenAddHeaders {
        @Test
        void givenOneHeaderToAdd() {
            metadata.put(METADATA_KEY, HEADER1 + HEADER_VALUE_SEPARATOR + HEADER1_VALUE);
            underTest.run();

            verify(context).addZuulResponseHeader(HEADER1, HEADER1_VALUE);
        }

        @Test
        void givenMultipleHeadersToAdd() {
            metadata.put(METADATA_KEY, HEADER1 + HEADER_VALUE_SEPARATOR + HEADER1_VALUE + HEADER_DELIMITER + HEADER2 + HEADER_VALUE_SEPARATOR + HEADER2_VALUE);
            underTest.run();

            verify(context).addZuulResponseHeader(HEADER1, HEADER1_VALUE);
        }

        @Test
        void givenMultipleHeadersToAddWithWhitespace_thenWhitespaceIsTrimmed() {
            metadata.put(METADATA_KEY, HEADER1 + HEADER_VALUE_SEPARATOR + HEADER1_VALUE + HEADER_DELIMITER + " " + HEADER2 + HEADER_VALUE_SEPARATOR + " " + HEADER2_VALUE);
            underTest.run();

            verify(context).addZuulResponseHeader(HEADER1, HEADER1_VALUE);
        }

        @Test
        void givenHeaderWithNoValue() {
            metadata.put(METADATA_KEY, HEADER1);
            underTest.run();

            verify(context).addZuulResponseHeader(HEADER1, "");
        }

        @Test
        void givenHeaderWithSeparatorInHeaderValue() {
            String value = HEADER1_VALUE + HEADER_VALUE_SEPARATOR;
            metadata.put(METADATA_KEY, HEADER1 + HEADER_VALUE_SEPARATOR + value);
            underTest.run();

            verify(context).addZuulResponseHeader(HEADER1, value);
        }
    }

    @Nested
    class WhenDontAddHeaders {
        @Test
        void givenNullServiceInstances() {
            when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(null);
            underTest.run();

            verify(context, never()).addZuulResponseHeader(anyString(), anyString());
        }

        @Test
        void givenNoServiceInstances() {
            when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.emptyList());
            underTest.run();

            verify(context, never()).addZuulResponseHeader(anyString(), anyString());
        }

        @Test
        void givenNoResponseHeadersConfig() {
            metadata.remove(METADATA_KEY);
            underTest.run();

            verify(context, never()).addZuulResponseHeader(anyString(), anyString());
        }

        @Test
        void givenEmptyResponseHeadersConfig() {
            metadata.put(METADATA_KEY, "");
            underTest.run();

            verify(context, never()).addZuulResponseHeader(anyString(), anyString());
        }
    }
}
