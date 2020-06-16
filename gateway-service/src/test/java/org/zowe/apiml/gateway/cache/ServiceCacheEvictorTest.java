/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.cache;

import com.netflix.discovery.CacheRefreshedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.ribbon.ApimlZoneAwareLoadBalancer;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class ServiceCacheEvictorTest {

    private ApimlDiscoveryClient apimlDiscoveryClient = mock(ApimlDiscoveryClient.class);

    private ApimlZoneAwareLoadBalancer<?> apimlZoneAwareLoadBalancer1 = mock(ApimlZoneAwareLoadBalancer.class);
    private ApimlZoneAwareLoadBalancer<?> apimlZoneAwareLoadBalancer2 = mock(ApimlZoneAwareLoadBalancer.class);
    private ApimlZoneAwareLoadBalancer<?> apimlZoneAwareLoadBalancer3 = mock(ApimlZoneAwareLoadBalancer.class);
    private ApimlZoneAwareLoadBalancer<?> apimlZoneAwareLoadBalancer4 = mock(ApimlZoneAwareLoadBalancer.class);

    private List<ServiceCacheEvict> serviceCacheEvicts = Arrays.asList(
        mock(ServiceCacheEvict.class),
        mock(ServiceCacheEvict.class)
    );

    ServiceCacheEvictor serviceCacheEvictor = mock(ServiceCacheEvictor.class);

    @BeforeEach
    public void setUp() {
        serviceCacheEvictor = new ServiceCacheEvictor(apimlDiscoveryClient, serviceCacheEvicts);
        when(apimlZoneAwareLoadBalancer1.getName()).thenReturn("service1");
        when(apimlZoneAwareLoadBalancer2.getName()).thenReturn("service2");
        when(apimlZoneAwareLoadBalancer3.getName()).thenReturn("service3");
        when(apimlZoneAwareLoadBalancer4.getName()).thenReturn("service4");
        serviceCacheEvictor.addApimlZoneAwareLoadBalancer(apimlZoneAwareLoadBalancer1);
        serviceCacheEvictor.addApimlZoneAwareLoadBalancer(apimlZoneAwareLoadBalancer2);
        serviceCacheEvictor.addApimlZoneAwareLoadBalancer(apimlZoneAwareLoadBalancer3);
        serviceCacheEvictor.addApimlZoneAwareLoadBalancer(apimlZoneAwareLoadBalancer4);
    }

    @Test
    void testService() {
        serviceCacheEvictor.onEvent(mock(CacheRefreshedEvent.class));
        verify(apimlZoneAwareLoadBalancer1, never()).updateListOfServers();

        serviceCacheEvictor.evictCacheService("service1");
        serviceCacheEvictor.evictCacheService("service2");
        serviceCacheEvictor.onEvent(mock(CacheRefreshedEvent.class));
        serviceCacheEvicts.forEach(x -> {
            verify(x, times(1)).evictCacheService("service1");
            verify(x, times(1)).evictCacheService("service2");
        });
        verify(apimlZoneAwareLoadBalancer1, times(1)).updateListOfServers();

        serviceCacheEvictor.evictCacheService("service3");
        serviceCacheEvictor.evictCacheAllService();
        serviceCacheEvictor.evictCacheService("service4");
        serviceCacheEvictor.onEvent(mock(CacheRefreshedEvent.class));
        serviceCacheEvicts.forEach(x -> {
            verify(x, never()).evictCacheService("service3");
            verify(x, times(1)).evictCacheAllService();
        });
        verify(apimlZoneAwareLoadBalancer3, times(3)).updateListOfServers();
    }

}
