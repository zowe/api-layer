/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Applications;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

public class ApimlDiscoveryClientStub extends CloudEurekaClient {
    private ApplicationRegistry applicationRegistry;

    public ApimlDiscoveryClientStub(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config, ApplicationEventPublisher publisher, ApplicationRegistry applicationRegistry, RestClientTransportClientFactories factories, RestClientDiscoveryClientOptionalArgs args1) {

        super(applicationInfoManager, config, factories, args1, publisher);

        this.applicationRegistry = applicationRegistry;
    }

    @Override
    public Applications getApplications() {
        if (applicationRegistry != null) {
            return applicationRegistry.getApplications();
        } else {
            return new Applications();
        }
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure) {
        return applicationRegistry.getInstances().stream()
            .filter(x -> StringUtils.equalsAnyIgnoreCase(vipAddress, x.getVIPAddress()))
            .filter(x -> !secure || x.getSecurePort() != 0)
            .toList();
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure, String region) {
        return applicationRegistry.getInstances().stream()
            .filter(x -> StringUtils.equalsAnyIgnoreCase(vipAddress, x.getVIPAddress()))
            .filter(x -> !secure || x.getSecurePort() != 0)
            .toList();
    }

}
