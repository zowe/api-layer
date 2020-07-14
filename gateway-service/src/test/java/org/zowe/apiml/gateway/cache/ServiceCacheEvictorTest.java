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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.zowe.apiml.gateway.ribbon.ApimlLoadBalancer;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class ServiceCacheEvictorTest {

    private ApimlLoadBalancer<?> apimlLoadBalancer1 = mock(ApimlLoadBalancer.class);
    private ApimlLoadBalancer<?> apimlLoadBalancer2 = mock(ApimlLoadBalancer.class);
    private ApimlLoadBalancer<?> apimlLoadBalancer3 = mock(ApimlLoadBalancer.class);
    private ApimlLoadBalancer<?> apimlLoadBalancer4 = mock(ApimlLoadBalancer.class);

    private List<ServiceCacheEvict> serviceCacheEvicts = Arrays.asList(
        mock(ServiceCacheEvict.class),
        mock(ServiceCacheEvict.class)
    );

    ServiceCacheEvictor serviceCacheEvictor = mock(ServiceCacheEvictor.class);

    @BeforeEach
    public void setUp() {
        serviceCacheEvictor = new ServiceCacheEvictor(serviceCacheEvicts);
        when(apimlLoadBalancer1.getName()).thenReturn("service1");
        when(apimlLoadBalancer2.getName()).thenReturn("service2");
        when(apimlLoadBalancer3.getName()).thenReturn("service3");
        when(apimlLoadBalancer4.getName()).thenReturn("service4");
    }

    @Test
    void testService() {

        serviceCacheEvictor.evictCacheService("service1");
        serviceCacheEvictor.evictCacheService("service2");
        serviceCacheEvictor.onApplicationEvent(new HeartbeatEvent("", ""));
        serviceCacheEvicts.forEach(x -> {
            verify(x, times(1)).evictCacheService("service1");
            verify(x, times(1)).evictCacheService("service2");
        });
        serviceCacheEvictor.evictCacheService("service3");
        serviceCacheEvictor.evictCacheAllService();
        serviceCacheEvictor.evictCacheService("service4");
        serviceCacheEvictor.refresh();
        serviceCacheEvicts.forEach(x -> {
            verify(x, never()).evictCacheService("service3");
            verify(x, times(1)).evictCacheAllService();
        });
    }

}
