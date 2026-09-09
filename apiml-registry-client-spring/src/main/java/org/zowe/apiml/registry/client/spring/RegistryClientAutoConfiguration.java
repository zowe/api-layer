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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.core.env.Environment;
import org.zowe.apiml.registry.SelfRegistration;
import org.zowe.apiml.registry.client.CachedRegistryDiscoveryClient;
import org.zowe.apiml.registry.client.HttpRegistryTransport;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.model.ServiceInstance;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.util.List;

/**
 * Wires a service onto the API ML registry.
 * <p>
 * The replacement for {@code spring-cloud-starter-netflix-eureka-client}. A service gets this by depending on
 * {@code apiml-registry-client-spring} - no annotation, no starter - and keeps consuming Spring Cloud's
 * {@code DiscoveryClient}, so route building, load balancing and the API Catalog are untouched.
 * <p>
 * Turn it off with {@code eureka.client.enabled: false}, or leave a service neither registering nor fetching
 * - {@code registerWithEureka: false} and {@code fetchRegistry: false} - and no client is built at all. The
 * modulith is the second case: the registry is a bean in the same JVM there, so it supplies its own
 * {@code DiscoveryClient} over the registry directly rather than have a client poll its own port over HTTP.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "eureka.client", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({RegistryInstanceProperties.class, RegistryFetchProperties.class})
public class RegistryClientAutoConfiguration {

    /**
     * This service's registration.
     * <p>
     * The bean is named rather than injected by type on purpose: Spring Cloud's {@code ServiceInstance} is
     * also on the classpath, and a same-named type resolved by type is a trap for whoever reads this next.
     */
    @Bean
    @ConditionalOnMissingBean(name = "selfServiceInstance")
    ServiceInstance selfServiceInstance(
        RegistryInstanceProperties instanceConfig,
        Environment environment,
        ObjectProvider<InetUtils> inetUtils
    ) {
        RegistryInstanceDefaults.apply(
            instanceConfig,
            environment.getProperty("spring.application.name"),
            inetUtils.getIfAvailable(() -> new InetUtils(new InetUtilsProperties())));
        return SelfInstanceFactory.create(instanceConfig, System.currentTimeMillis());
    }

    @Bean
    @ConditionalOnMissingBean
    SelfRegistration selfRegistration(ServiceInstance selfServiceInstance) {
        return new SelfRegistration() {

            @Override
            public String instanceId() {
                return selfServiceInstance.instanceId();
            }

            @Override
            public String serviceId() {
                return selfServiceInstance.serviceId();
            }

        };
    }

    /**
     * The client and everything that drives it.
     * <p>
     * Absent when a service neither registers nor fetches, so that the modulith - which does both through the
     * in-JVM registry instead - does not build an HTTP client, a connection pool and a scheduler thread that
     * nothing would ever use.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnExpression(
        "${eureka.client.registerWithEureka:true} or ${eureka.client.fetchRegistry:true}")
    static class ActiveClientConfiguration {

        /**
         * @param secureSslContext   the keystore-bearing context from {@code HttpConfig}, injected by bean name so
         *                           this module needs no dependency on {@code apiml-common}
         * @param verifyCertificates when off, the Discovery Service certificate's host name is not checked - the
         *                           same {@code apiml.security.ssl.verifySslCertificatesOfServices} switch that
         *                           governed the Eureka client
         */
        @Bean
        @ConditionalOnMissingBean
        RegistryTransport registryTransport(
            RegistryFetchProperties clientConfig,
            @Qualifier("secureSslContext") ObjectProvider<SSLContext> secureSslContext,
            @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}") boolean verifyCertificates,
            @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}") boolean nonStrictVerify,
            @Value("${apiml.service.discoveryServiceUserid:eureka}") String userid,
            @Value("${apiml.service.discoveryServicePassword:password}") String password
        ) {
            List<String> urls = clientConfig.discoveryServiceUrls();
            if (urls.isEmpty()) {
                throw new IllegalStateException("eureka.client.serviceUrl.defaultZone is not set, so this service "
                    + "has no Discovery Service to register with");
            }
            return new HttpRegistryTransport(
                urls,
                secureSslContext.getIfAvailable(),
                new RegistryCodec(),
                verifyCertificates && !nonStrictVerify,
                clientConfig.getEurekaServerConnectTimeoutSeconds() * 1000,
                clientConfig.getEurekaServerReadTimeoutSeconds() * 1000,
                userid,
                password
            );
        }

        @Bean
        @ConditionalOnMissingBean
        RegistryClient registryClient(
            RegistryTransport transport,
            RegistryFetchProperties clientConfig,
            ServiceInstance selfServiceInstance
        ) {
            return clientConfig.isRegisterWithEureka()
                ? new RegistryClient(transport, selfServiceInstance)
                : new RegistryClient(transport);
        }

        @Bean
        @ConditionalOnMissingBean(org.springframework.cloud.client.discovery.DiscoveryClient.class)
        CachedRegistryDiscoveryClient registryDiscoveryClient(RegistryClient registryClient) {
            return new CachedRegistryDiscoveryClient(registryClient);
        }

        @Bean
        RegistryClientLifecycle registryClientLifecycle(
            RegistryClient registryClient,
            RegistryFetchProperties clientConfig,
            ApplicationEventPublisher publisher,
            ObjectProvider<HealthStatusSource> healthStatusSource
        ) {
            return new RegistryClientLifecycle(
                registryClient, clientConfig, publisher, healthStatusSource.getIfAvailable());
        }

    }

    /**
     * Present only when actuator is on the classpath and {@code eureka.client.healthcheck.enabled} is on,
     * which is how every API ML service is configured.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.springframework.boot.actuate.health.StatusAggregator.class)
    @ConditionalOnProperty(prefix = "eureka.client.healthcheck", name = "enabled", havingValue = "true")
    static class HealthCheckConfiguration {

        @Bean
        @ConditionalOnMissingBean
        HealthStatusSource registryHealthStatusSource(
            org.springframework.boot.actuate.health.StatusAggregator statusAggregator,
            ApplicationContext applicationContext
        ) {
            return new HealthStatusSource(
                statusAggregator,
                applicationContext.getBeansOfType(org.springframework.boot.actuate.health.HealthContributor.class),
                applicationContext.getBeansOfType(org.springframework.boot.actuate.health.ReactiveHealthContributor.class)
            );
        }

    }

}
