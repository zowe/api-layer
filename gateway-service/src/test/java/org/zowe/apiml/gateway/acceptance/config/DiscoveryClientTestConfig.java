/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClientConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.RestTemplateTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.gateway.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.gateway.acceptance.netflix.ApplicationRegistry;
import reactor.core.publisher.Flux;

/**
 * This configuration provides the bean for the ApplicationRegistry and overrides bean CloudEurekaClient with custom ApimlDiscoveryClient. This bean mocks Eureka Client to allow virtual services registration.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speeds up distribution of
 * changes in whole central gateway.
 */
@TestConfiguration
@RequiredArgsConstructor
public class DiscoveryClientTestConfig {

    private final ApplicationContext context;

    @Bean
    public ApplicationRegistry registry() {
        return new ApplicationRegistry();
    }

    @Bean
    public ReactiveDiscoveryClient mockServicesReactiveDiscoveryClient(ApplicationRegistry applicationRegistry) {
        return new ReactiveDiscoveryClient() {

            @Override
            public String description() {
                return "mocked services";
            }

            @Override
            public Flux<ServiceInstance> getInstances(String serviceId) {
                return Flux.just(applicationRegistry.getServiceInstance(serviceId).toArray(new ServiceInstance[0]));
            }

            @Override
            public Flux<String> getServices() {
                return Flux.just(applicationRegistry.getInstances().stream()
                    .map(a -> a.getId())
                    .distinct()
                    .toArray(String[]::new));
            }
        };
    }

    @Bean(destroyMethod = "shutdown", name = "test")
    @Primary
    @RefreshScope
    public ApimlDiscoveryClientStub eurekaClient(ApplicationInfoManager manager,
                                                 EurekaClientConfig config,
                                                 EurekaInstanceConfig instance,
                                                 @Autowired(required = false) HealthCheckHandler healthCheckHandler,
                                                 ApplicationRegistry applicationRegistry
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }


        var factorySupplier = new DefaultEurekaClientHttpRequestFactorySupplier(new RestTemplateTimeoutProperties());
        var args1 = new RestTemplateDiscoveryClientOptionalArgs(factorySupplier);
        var factories = new RestTemplateTransportClientFactories(args1);
        final var discoveryClient = new ApimlDiscoveryClientStub(appManager, config, this.context, applicationRegistry, factories, args1);
        discoveryClient.registerHealthCheck(healthCheckHandler);

        discoveryClient.registerEventListener(event -> {
        });
        return discoveryClient;
    }


}
