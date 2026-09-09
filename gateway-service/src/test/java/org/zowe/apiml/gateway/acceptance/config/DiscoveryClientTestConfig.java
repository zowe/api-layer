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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.zowe.apiml.gateway.ApplicationRegistry;
import org.zowe.apiml.registry.RegistryView;
import org.zowe.apiml.registry.SelfRegistration;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Serves the acceptance tests' mock services as if they were registered.
 * <p>
 * Three views of the same {@link ApplicationRegistry}, because the Gateway consumes discovery three ways: a
 * reactive client for route building, a blocking one for the components that ask directly, and a
 * {@link RegistryView} for the parts that report on registrations in full.
 * <p>
 * This used to be one bean - {@code ApimlDiscoveryClientStub}, a subclass of Netflix's
 * {@code CloudEurekaClient} that had to be handed an {@code ApplicationInfoManager}, an
 * {@code EurekaClientConfig}, a request-factory supplier, a transport-factories object and a
 * {@code HealthCheckHandler} before it would serve a single mock instance. None of that had anything to do
 * with the test.
 */
@TestConfiguration
@RequiredArgsConstructor
@Profile("!ApimlModulithAcceptanceTest")
public class DiscoveryClientTestConfig {

    @Bean
    @Primary
    ApplicationRegistry registry() {
        return new ApplicationRegistry();
    }

    @Bean
    @Primary
    RegistryView registryView(ApplicationRegistry applicationRegistry) {
        return applicationRegistry;
    }

    @Bean
    @Primary
    SelfRegistration selfRegistration() {
        return new SelfRegistration() {

            @Override
            public String instanceId() {
                return "localhost:gateway:10010";
            }

            @Override
            public String serviceId() {
                return "gateway";
            }

        };
    }

    /**
     * Not {@code @Primary}: Spring Cloud's {@code CompositeDiscoveryClient} is the primary one and aggregates
     * every other {@code DiscoveryClient} bean, so marking this one primary as well leaves two primaries and
     * nothing injectable.
     */
    @Bean
    DiscoveryClient mockServicesDiscoveryClient(ApplicationRegistry applicationRegistry) {
        return new DiscoveryClient() {

            @Override
            public String description() {
                return "mocked services";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return applicationRegistry.getServiceInstance(serviceId);
            }

            @Override
            public List<String> getServices() {
                return applicationRegistry.serviceIds();
            }

        };
    }

    @Bean
    ReactiveDiscoveryClient mockServicesReactiveDiscoveryClient(ApplicationRegistry applicationRegistry) {
        return new ReactiveDiscoveryClient() {

            @Override
            public String description() {
                return "mocked services";
            }

            @Override
            public Flux<ServiceInstance> getInstances(String serviceId) {
                return Flux.fromIterable(applicationRegistry.getServiceInstance(serviceId));
            }

            @Override
            public Flux<String> getServices() {
                return Flux.fromIterable(applicationRegistry.serviceIds());
            }

        };
    }

}
