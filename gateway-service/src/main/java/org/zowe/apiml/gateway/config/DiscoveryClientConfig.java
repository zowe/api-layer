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
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.MutableDiscoveryClientOptionalArgs;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;

/**
 * This configuration override bean EurekaClient with custom ApimlDiscoveryClient. This bean offer additional method
 * fetchRegistry. User can call this method to asynchronously fetch new data from discovery service. There is no time
 * to fetching.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speeds up distribution of
 * changes in whole gateway.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DiscoveryClientConfig {
    private final ApplicationContext context;
    private final AbstractDiscoveryClientOptionalArgs<?> optionalArgs;
    private final EurekaJerseyClientImpl.EurekaJerseyClientBuilder eurekaJerseyClientBuilder;

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public ApimlDiscoveryClient primaryApimlEurekaClient(ApplicationInfoManager manager,
                                                         EurekaClientConfig config,
                                                         @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager = ProxyUtils.getTargetObject(manager);

        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(appManager, config, this.optionalArgs, this.context);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        return discoveryClientClient;
    }

    @Bean(destroyMethod = "shutdown")
    @Conditional({AdditionalRegistrationCondition.class})
    @RefreshScope
    public DiscoveryClientWrapper additionalDiscoveryClientWrapper(ApplicationInfoManager manager,
                                                                   EurekaClientConfig config,
                                                                   @Autowired(required = false) HealthCheckHandler healthCheckHandler,
                                                                   List<AdditionalRegistration> additionalRegistrations
    ) {

        List<ApimlDiscoveryClient> discoveryClientsList = new ArrayList<>(additionalRegistrations.size());
        for (AdditionalRegistration apimlRegistration : additionalRegistrations) {
            ApimlDiscoveryClient additionalApimlRegistration = registerInTheApimlInstance(config, healthCheckHandler, apimlRegistration, manager);
            discoveryClientsList.add(additionalApimlRegistration);
        }

        return new DiscoveryClientWrapper(discoveryClientsList);
    }

    private ApimlDiscoveryClient registerInTheApimlInstance(EurekaClientConfig config, HealthCheckHandler healthCheckHandler, AdditionalRegistration apimlRegistration, ApplicationInfoManager appManager) {

        EurekaClientConfigBean configBean = new EurekaClientConfigBean();
        BeanUtils.copyProperties(config, configBean);

        Map<String, String> urls = new HashMap<>();
        log.debug("additional registration: {}", apimlRegistration.getDiscoveryServiceUrls());
        urls.put(DEFAULT_ZONE, apimlRegistration.getDiscoveryServiceUrls());

        configBean.setServiceUrl(urls);

        MutableDiscoveryClientOptionalArgs args = new MutableDiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClientBuilder.build());

        ApplicationInfoManager bareManager = ProxyUtils.getTargetObject(appManager);
        InstanceInfo.Builder builder = new InstanceInfo.Builder(bareManager.getInfo());
        InstanceInfo ii = builder.build();
        ApplicationInfoManager perClientAppManager = new ApplicationInfoManager(bareManager.getEurekaInstanceConfig(), ii, null);

        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(perClientAppManager, configBean, args, this.context);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        return discoveryClientClient;
    }
}
