/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This configuration override bean EurekaClient with custom ApimlDiscoveryClient. This bean offer additional method
 * fetchRegistry. User can call this method to asynchronously fetch new data from discovery service. There is no time
 * to fetching.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speeds up distribution of
 * changes in whole gateway.
 */
@Configuration
@RequiredArgsConstructor
public class DiscoveryClientConfig {
    private final ApplicationContext context;
    private final AbstractDiscoveryClientOptionalArgs<?> optionalArgs;
    private final EurekaJerseyClientImpl.EurekaJerseyClientBuilder eurekaJerseyClientBuilder;

    @Value("${apiml.service.centralRegistryUrls:-}")
    private String[] centralRegistryUrls;

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public ApimlDiscoveryClient primaryApimlEurekaClient(ApplicationInfoManager manager,
                                                         EurekaClientConfig config,
                                                         @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }

        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(appManager, config, this.optionalArgs, this.context);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        return discoveryClientClient;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "apiml.service.centralRegistryUrls")
    @RefreshScope
    public DiscoveryClientWrapper additionalDiscoverClientWrapper(ApplicationInfoManager manager,
                                                                  EurekaClientConfig config,
                                                                  @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }
        List<ApimlDiscoveryClient> listOfDiscoveryClientClient = new ArrayList<>();


        if (centralRegistryUrls != null) {
            for (String url : centralRegistryUrls) {

                EurekaClientConfigBean configBean = new EurekaClientConfigBean();
                BeanUtils.copyProperties(config, configBean);

                Map<String, String> urls = new HashMap<>();
                urls.put("defaultZone", url);

                configBean.setServiceUrl(urls);

                MutableDiscoveryClientOptionalArgs args = new MutableDiscoveryClientOptionalArgs();
                args.setEurekaJerseyClient(eurekaJerseyClientBuilder.build());

                final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(appManager, configBean, args, this.context);
                discoveryClientClient.registerHealthCheck(healthCheckHandler);


                listOfDiscoveryClientClient.add(discoveryClientClient);
            }
        }
        return new DiscoveryClientWrapper(listOfDiscoveryClientClient);
    }

    @Bean
    public EurekaDiscoveryClient discoveryClient(EurekaClient client,
                                                 EurekaClientConfig clientConfig) {
        return new EurekaDiscoveryClient(client, clientConfig);
    }


}
