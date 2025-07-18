/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

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
import org.springframework.cloud.netflix.eureka.RestClientTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;
import org.zowe.apiml.gateway.ApimlDiscoveryClientStub;
import org.zowe.apiml.gateway.ApplicationRegistry;
import reactor.core.publisher.Flux;

@TestConfiguration
@Profile("ApimlModulithAcceptanceTest")
@RequiredArgsConstructor
public class TestConfig {

    private final ApplicationContext context;

    @Bean
    ApplicationRegistry applicationRegistry() {
        return new ApplicationRegistry();
    }

    ReactiveDiscoveryClient mockServicesReactiveDiscoveryClient(ApplicationRegistry applicationRegistry) {
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
    ApimlDiscoveryClientStub eurekaClient(ApplicationInfoManager manager,
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

        var factorySupplier = new DefaultEurekaClientHttpRequestFactorySupplier(new RestClientTimeoutProperties());
        var args1 = new RestClientDiscoveryClientOptionalArgs(factorySupplier, RestClient::builder);
        var factories = new RestClientTransportClientFactories(args1);
        final var discoveryClient = new ApimlDiscoveryClientStub(appManager, config, this.context, applicationRegistry, factories, args1);

        discoveryClient.registerHealthCheck(healthCheckHandler);
        discoveryClient.registerEventListener(event -> { });

        return discoveryClient;
    }

}
