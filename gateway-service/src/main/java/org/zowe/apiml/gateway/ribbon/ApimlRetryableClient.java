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

import com.netflix.client.config.IClientConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.apache.RetryableRibbonLoadBalancingHttpClient;
import org.zowe.apiml.gateway.ribbon.http.RequestContextNotPreparedException;

/**
 * Minimalistic extension of Ribbon's retryable client
 */
@SuppressWarnings("squid:S110")
public class ApimlRetryableClient extends RetryableRibbonLoadBalancingHttpClient {

    ServerIntrospector introspector;

    public ApimlRetryableClient(CloseableHttpClient delegate, IClientConfig config, ServerIntrospector serverIntrospector, LoadBalancedRetryFactory loadBalancedRetryFactory) {
        super(delegate, config, serverIntrospector, loadBalancedRetryFactory);
        this.introspector = serverIntrospector;
    }

    /**
     * Override method from {@link com.netflix.client.AbstractLoadBalancerAwareClient} because it constructs
     * a {@link RibbonLoadBalancerClient.RibbonServer} that is not fit for our use. Namely, it's getUri() method always returns http scheme, causing the
     * retryable client to always call http. We have stored the instance info from the load balancer in the context
     * so we recreate EurekaServiceInstance here, which correctly resolves whether instance is http or https when asked.
     *
     * @param serviceId
     * @return EurekaServiceInstance
     */
    @Override
    public ServiceInstance choose(String serviceId) {
        super.choose(serviceId);
        if(!RequestContextUtils.getInstanceInfo().isPresent()){
            System.out.println("");
        }
        return new EurekaDiscoveryClient.EurekaServiceInstance(
            RequestContextUtils.getInstanceInfo().orElseThrow(() -> new RequestContextNotPreparedException("request context not prepared")));
    }
}
