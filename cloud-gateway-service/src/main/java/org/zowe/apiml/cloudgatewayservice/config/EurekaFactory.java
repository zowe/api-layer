/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Eureka dependencies injection helper
 */
@Component
public class EurekaFactory {

    /**
     * Create new copy of instance info
     *
     * @param instanceConfig eureka instance config to copy from
     */
    InstanceInfo createInstanceInfo(EurekaInstanceConfig instanceConfig) {
        return new InstanceInfoFactory().create(instanceConfig);
    }

    public CloudEurekaClient createCloudEurekaClient(EurekaInstanceConfig eurekaInstanceConfig, InstanceInfo newInfo, EurekaClientConfigBean configBean, ApplicationContext context, RestTemplateTransportClientFactories factories, RestTemplateDiscoveryClientOptionalArgs args1) {
        ApplicationInfoManager perClientAppManager = new ApplicationInfoManager(eurekaInstanceConfig, newInfo, null);
       return new CloudEurekaClient(perClientAppManager, configBean, factories, args1, context);
    }
}
