package org.zowe.apiml.eurekaservice.client.impl;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

public class DiscoveryClientProvider implements org.zowe.apiml.eurekaservice.client.EurekaClientProvider {
    @Override
    public EurekaClient client(ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig, AbstractDiscoveryClientOptionalArgs args) {
        return new DiscoveryClient(applicationInfoManager, clientConfig, args);
    }
}
