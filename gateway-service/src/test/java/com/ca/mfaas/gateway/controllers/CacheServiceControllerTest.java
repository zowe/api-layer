package com.ca.mfaas.gateway.controllers;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.mfaas.gateway.discovery.ApimlDiscoveryClient;
import com.ca.mfaas.gateway.security.service.ServiceCacheEvict;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class CacheServiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ServiceCacheEvict service1;

    @Mock
    private ServiceCacheEvict service2;

    @Mock
    private ApimlDiscoveryClient discoveryClient;

    private CacheServiceController cacheServiceController;

    @Before
    public void setUp() {
        cacheServiceController = new CacheServiceController(Arrays.asList(service1, service2), discoveryClient);
        mockMvc = MockMvcBuilders.standaloneSetup(cacheServiceController).build();
    }

    @Test
    public void testEvictAll() throws Exception {
        verify(service1, never()).evictCacheAllService();
        verify(service2, never()).evictCacheAllService();
        verify(discoveryClient, never()).fetchRegistry();

        this.mockMvc.perform(delete("/cache/services")).andExpect(status().isOk());

        verify(service1, times(1)).evictCacheAllService();
        verify(service2, times(1)).evictCacheAllService();
        verify(discoveryClient, times(1)).fetchRegistry();
    }

    @Test
    public void testEvict() throws Exception {
        verify(service1, never()).evictCacheService(any());
        verify(service2, never()).evictCacheService(any());
        verify(discoveryClient, never()).fetchRegistry();

        this.mockMvc.perform(delete("/cache/services/service01")).andExpect(status().isOk());

        verify(service1, times(1)).evictCacheService("service01");
        verify(service2, times(1)).evictCacheService("service01");
        verify(discoveryClient, times(1)).fetchRegistry();
    }

}
