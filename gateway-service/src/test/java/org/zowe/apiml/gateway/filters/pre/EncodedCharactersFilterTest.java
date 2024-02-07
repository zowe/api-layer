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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;



@ExtendWith(MockitoExtension.class)
class EncodedCharactersFilterTest {

    private EncodedCharactersFilter filter;

    private final String METADATA_KEY = EncodedCharactersFilter.METADATA_KEY;
    private final String SERVICE_ID = "serviceid";

    private final DefaultServiceInstance serviceInstanceWithTrueConfiguration = new DefaultServiceInstance("INSTANCE1", SERVICE_ID ,"",0,true, new HashMap<String, String>());
    private final DefaultServiceInstance serviceInstanceWithFalseConfiguration = new DefaultServiceInstance("INSTANCE2", SERVICE_ID ,"",0,true, new HashMap<String, String>());
    private final DefaultServiceInstance serviceInstanceWithNoConfiguration = new DefaultServiceInstance("INSTANCE3", SERVICE_ID ,"",0,true, new HashMap<String, String>());

    private static MessageService messageService;

    @Mock
    DiscoveryClient discoveryClient;

    @BeforeAll
    static void initMessageService() {
        messageService = new YamlMessageService("/gateway-log-messages.yml");
    }

    @BeforeEach
    void setup() {
        filter = new EncodedCharactersFilter(discoveryClient, messageService);
        serviceInstanceWithTrueConfiguration.getMetadata().put(METADATA_KEY, "true");
        serviceInstanceWithFalseConfiguration.getMetadata().put(METADATA_KEY, "false");
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(PROXY_KEY, "api/v1/" + SERVICE_ID);
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
        ctx.setResponse(new MockHttpServletResponse());
    }

    @Test
    void givenSingleInstance_WhenConfiguredFalse_ShouldFilter() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithFalseConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(true)));
    }

    @Test
    void givenSingleInstance_WhenConfiguredTrue_ShouldNotFilter() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithTrueConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(false)));
    }

    @Test
    void givenSingleInstance_WhenNotConfigured_ShouldNotFilter() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithNoConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(false)));
    }

    @Test
    void shouldReturnFilterType() {
        String filterType = this.filter.filterType();
        assertEquals("pre", filterType);
    }

    @Test
    void shouldReturnFilterOrder() {
        int filterOrder = this.filter.filterOrder();
        assertEquals(7, filterOrder);
    }

    @Test
    void givenMultipleInstances_WhenMixedSetup_ShouldBePessimistic() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithFalseConfiguration);
        instanceList.add(serviceInstanceWithTrueConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(true)));
    }

    @Test
    void shouldRejectRequestsWithEncodedCharacters() {
        RequestContext context = RequestContext.getCurrentContext();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/He%2f%2f0%2dwor%2fd");
        context.setRequest(mockRequest);
        this.filter.run();
        assertTrue(context.getResponseBody().contains("Service 'serviceid' does not allow encoded characters in the request path: '/He%2f%2f0%2dwor%2fd'."));
        assertTrue(context.getResponseBody().contains("ZWEAG701E"));
        assertEquals(400, context.getResponse().getStatus());
    }

    @Test
    void shouldAllowRequestsWithoutEncodedCharacters() {
        RequestContext context = RequestContext.getCurrentContext();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/HelloWorld");
        context.setRequest(mockRequest);

        this.filter.run();

        assertEquals(200, context.getResponse().getStatus());
    }

    @Test
    void shouldPassNullRequest() {
        RequestContext context = RequestContext.getCurrentContext();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI(null);
        context.setRequest(mockRequest);

        this.filter.run();

        assertEquals(200, context.getResponse().getStatus());
    }
}
