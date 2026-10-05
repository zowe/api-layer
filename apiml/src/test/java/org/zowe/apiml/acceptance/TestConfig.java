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

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.zowe.apiml.gateway.ApplicationRegistry;

import java.util.List;

/**
 * Exposes the modulith acceptance tests' mock services through Spring Cloud discovery.
 * <p>
 * The production modulith already adapts its blocking discovery client to a reactive one, so this configuration
 * only contributes the blocking view. Spring Cloud's composite client combines it with the local registry and
 * the existing reactive adapter then serves both to Gateway route construction.
 */
@TestConfiguration
@Profile("ApimlModulithAcceptanceTest")
public class TestConfig {

    @Bean
    ApplicationRegistry applicationRegistry() {
        return new ApplicationRegistry();
    }

    /**
     * Not {@code @Primary}: Spring Cloud's composite client is primary and aggregates this test registry with
     * the real in-process registry.
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

}
