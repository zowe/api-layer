/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance.netflix;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.zowe.apiml.gateway.cache.ServiceCacheEvictor;
import org.zowe.apiml.gateway.ribbon.ApimlZoneAwareLoadBalancer;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;

public class ApimlZoneAwareLoadBalancerStub extends ApimlZoneAwareLoadBalancer {
    private ApplicationRegistry applicationRegistry;

    public ApimlZoneAwareLoadBalancerStub(IClientConfig clientConfig, IRule rule, IPing ping, ServerList serverList, ServerListFilter filter, ServerListUpdater serverListUpdater, ServiceCacheEvictor serviceCacheEvictor, ApplicationRegistry applicationRegistry) {
        super(clientConfig, rule, ping, serverList, filter, serverListUpdater, serviceCacheEvictor);

        this.applicationRegistry = applicationRegistry;
    }

    /**
     * This method stores the instance info of chosen instance in the RequestContext for later usage, for
     * example by authentication logic.
     */
    @Override
    public Server chooseServer(Object key) {
        Server server = new DiscoveryEnabledServer(applicationRegistry.getInstanceInfo(), true);
        server.setAlive(true);
        server.setReadyToServe(true);

        if (server instanceof DiscoveryEnabledServer) {
            RequestContextUtils.setInstanceInfo(((DiscoveryEnabledServer) server).getInstanceInfo());
            RequestContextUtils.addDebugInfo("Load Balancer chooses: " + ((DiscoveryEnabledServer) server).getInstanceInfo());
        } else {
            throw new IllegalStateException("Unexpected error, please contact Broadcom support");
        }

        return server;
    }
}
