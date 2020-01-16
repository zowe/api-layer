/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.filters.pre;

import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;



@RunWith(MockitoJUnitRunner.class)
public class EncodedCharactersFilterTest {

    private EncodedCharactersFilter filter;

    private final String METADATA_KEY = EncodedCharactersFilter.METADATA_KEY;
    private final String SERVICE_ID = "serviceid";

    private final DefaultServiceInstance serviceInstanceWithoutConfiguration = new DefaultServiceInstance("INSTANCE1", SERVICE_ID ,"",0,true, new HashMap<String, String>());
    private final DefaultServiceInstance serviceInstanceWithConfiguration = new DefaultServiceInstance("INSTANCE2", SERVICE_ID ,"",0,true, new HashMap<String, String>());

    @Mock
    DiscoveryClient discoveryClient;

    @Before
    public void setup() {
        filter = new EncodedCharactersFilter(discoveryClient);
        serviceInstanceWithConfiguration.getMetadata().put(METADATA_KEY, "TRUE");

        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(REQUEST_URI_KEY, "/path");
        ctx.set(PROXY_KEY, "api/v1/" + SERVICE_ID);
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
    }

    @Test
    public void givenSingleInstanceWhenNotConfiguredShouldFilter() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithoutConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(true)));
    }

    @Test
    public void givenSingleInstanceWhenConfiguredShouldNotFilter() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(false)));
    }

    @Test
    public void givenMultipleInstancesWhenMixedSetupShouldBePesimistic() {
        List<ServiceInstance> instanceList = new ArrayList<>();
        instanceList.add(serviceInstanceWithoutConfiguration);
        instanceList.add(serviceInstanceWithConfiguration);
        when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(instanceList);

        assertThat(filter.shouldFilter(), is(equalTo(true)));
    }

}
