/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class CacheServiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ServiceCacheEvict service1;

    @Mock
    private ServiceCacheEvict service2;

    @Mock
    private ApimlDiscoveryClient discoveryClient;


    @BeforeEach
    public void setUp() {
        CacheServiceController cacheServiceController = new CacheServiceController(
            Arrays.asList(service1, service2), discoveryClient);
        mockMvc = MockMvcBuilders.standaloneSetup(cacheServiceController).build();
    }

    @Test
    void testEvictAll() throws Exception {
        verify(service1, never()).evictCacheAllService();
        verify(service2, never()).evictCacheAllService();
        verify(discoveryClient, never()).fetchRegistry();

        this.mockMvc.perform(delete("/gateway/cache/services")).andExpect(status().isOk());

        verify(service1, times(1)).evictCacheAllService();
        verify(service2, times(1)).evictCacheAllService();
        verify(discoveryClient, times(1)).fetchRegistry();
    }

    @Test
    void testEvict() throws Exception {
        verify(service1, never()).evictCacheService(any());
        verify(service2, never()).evictCacheService(any());
        verify(discoveryClient, never()).fetchRegistry();

        this.mockMvc.perform(delete("/gateway/cache/services/service01")).andExpect(status().isOk());

        verify(service1, times(1)).evictCacheService("service01");
        verify(service2, times(1)).evictCacheService("service01");
        verify(discoveryClient, times(1)).fetchRegistry();
    }

}
