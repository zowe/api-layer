/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration;
import org.zowe.apiml.registry.client.CachedRegistryDiscoveryClient;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.model.Application;
import org.zowe.apiml.registry.model.Applications;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The registry-backed discovery clients have to be the ones an application gets.
 * <p>
 * Spring Cloud's simple clients are registered on {@code @ConditionalOnMissingBean}, so which client wins is
 * decided by auto-configuration ordering. Losing that race is quiet and severe: a reactive application keeps the
 * empty {@code SimpleReactiveDiscoveryClient}, and since Spring Cloud Gateway builds one route per service
 * enumerated from {@code getServices()}, the Gateway starts, registers, reports healthy and routes nothing.
 * <p>
 * The simple auto-configurations are loaded here on purpose, so this asserts the ordering rather than the
 * presence of a bean in isolation.
 */
class RegistryDiscoveryClientAutoConfigurationTest {

    private static final String SERVICE = "discoverableclient";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            UtilAutoConfiguration.class,
            SimpleDiscoveryClientAutoConfiguration.class,
            SimpleReactiveDiscoveryClientAutoConfiguration.class,
            // What an application actually injects: the composite is @Primary and resolves the ambiguity between
            // the registry client and Spring Cloud's simple one.
            CompositeDiscoveryClientAutoConfiguration.class,
            RegistryClientAutoConfiguration.class))
        .withPropertyValues(
            "spring.application.name=gateway",
            "eureka.client.serviceUrl.defaultZone=https://localhost:10011/eureka/",
            "eureka.instance.appname=gateway",
            "eureka.instance.hostname=localhost",
            "eureka.instance.securePort=10010",
            "eureka.instance.securePortEnabled=true");

    private static ServiceInstance instance(String service, int port, InstanceStatus status) {
        return ServiceInstance.builder()
            .instanceId("localhost:" + service + ":" + port)
            .appName(service)
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .securePort(port, true)
            .vipAddress(service)
            .status(status)
            .build();
    }

    @Test
    @DisplayName("a reactive application gets the registry, not the empty simple client")
    void reactiveClientIsTheRegistryBackedOne() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
            assertInstanceOf(CachedRegistryReactiveDiscoveryClient.class,
                context.getBean(ReactiveDiscoveryClient.class),
                "a reactive application reading the simple client would build no routes at all");
        });
    }

    @Test
    @DisplayName("a servlet application resolves the registry through whatever DiscoveryClient it is given")
    void blockingClientServesTheRegistry() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();

            // Spring Cloud's simple client is registered on @ConditionalOnMissingBean(SimpleDiscoveryClient), so it
            // does not back off when another DiscoveryClient exists and the two coexist. What matters is not how
            // many there are but that the one an application is handed - resolved by type, as its own code does -
            // answers from the registry rather than from the empty simple client.
            assertThat(context).hasBean("apimlRegistryDiscoveryClient");
            assertInstanceOf(CachedRegistryDiscoveryClient.class,
                context.getBean("apimlRegistryDiscoveryClient"));

            context.getBean(RegistryClient.class).cache().replace(new Applications(
                List.of(new Application(SERVICE.toUpperCase(),
                    List.of(instance(SERVICE, 10012, InstanceStatus.UP)))),
                1L,
                "UP_1_"));

            assertThat(context.getBean(DiscoveryClient.class).getInstances(SERVICE))
                .as("the client an application injects must serve the registry")
                .hasSize(1);
        });
    }

    @Test
    @DisplayName("both clients serve the registry view, so route building and health agree")
    void bothClientsServeTheSameView() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();

            // What the lifecycle does once the registry answers.
            context.getBean(RegistryClient.class).cache().replace(new Applications(
                List.of(new Application(SERVICE.toUpperCase(),
                    List.of(instance(SERVICE, 10012, InstanceStatus.UP)))),
                1L,
                "UP_1_"));

            var blocking = context.getBean(DiscoveryClient.class);
            var reactive = context.getBean(ReactiveDiscoveryClient.class);

            assertThat(blocking.getServices()).contains(SERVICE);
            assertThat(blocking.getInstances(SERVICE)).hasSize(1);

            assertThat(reactive.getServices().collectList().block()).contains(SERVICE);
            assertThat(reactive.getInstances(SERVICE).collectList().block()).hasSize(1);
        });
    }

    @Test
    @DisplayName("an application with its own registryDiscoveryClient bean still starts")
    void doesNotCollideWithAnApplicationDefinedClient() {
        // The Discovery Service ships its own RegistryDiscoveryClient as a component, so that name is taken
        // there. A bean of the same name in this auto-configuration stopped it from starting at all, which is
        // how this was found.
        runner.withBean("registryDiscoveryClient", DiscoveryClient.class, () -> new DiscoveryClient() {
                @Override
                public String description() {
                    return "the application's own discovery client";
                }

                @Override
                public List<org.springframework.cloud.client.ServiceInstance> getInstances(String serviceId) {
                    return List.of();
                }

                @Override
                public List<String> getServices() {
                    return List.of();
                }
            })
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("registryDiscoveryClient");
                assertThat(context).hasBean("apimlRegistryDiscoveryClient");
            });
    }

    @Test
    @DisplayName("an application that neither registers nor fetches still starts")
    void canBeTurnedOff() {
        runner.withPropertyValues("eureka.client.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ReactiveDiscoveryClient.class);
            });
    }

}
