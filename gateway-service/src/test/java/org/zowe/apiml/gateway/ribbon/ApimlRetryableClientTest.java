/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    ApimlRetryableClientTest.Context.class
})
class ApimlRetryableClientTest {

    @Configuration
    public static class Context {
        @Bean
        public IClientConfig config() {
            return  IClientConfig.Builder.newBuilder(DefaultClientConfigImpl.class, "apicatalog")
                .withSecure(false)
                .withFollowRedirects(false)
                .withDeploymentContextBasedVipAddresses("apicatalog")
                .withLoadBalancerEnabled(false)
                .build();
        }
    }

    @Autowired
    IClientConfig config;
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    ServerIntrospector introspector = mock(ServerIntrospector.class);
    LoadBalancedRetryFactory retryFactory = mock(LoadBalancedRetryFactory.class);
    ILoadBalancer lb = mock(ILoadBalancer.class);

    @Test
    void givenServiceId_whenChoose_thenProducesServiceInstance() {
        ApimlRetryableClient client = new ApimlRetryableClient(
            httpClient, config, introspector, retryFactory
        );
        doReturn(new Server("aaa", 22)).when(lb).chooseServer(any());
        RequestContextUtils.setInstanceInfo(
            InstanceInfo.Builder.newBuilder().setAppName("Sonya").build()
        );
        client.setLoadBalancer(lb);

        ServiceInstance instance = client.choose("service1");

        assertThat(instance, instanceOf(EurekaDiscoveryClient.EurekaServiceInstance.class));
    }
}
