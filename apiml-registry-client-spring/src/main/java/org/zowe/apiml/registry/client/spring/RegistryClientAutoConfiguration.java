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
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration;
import org.zowe.apiml.registry.client.CachedRegistryDiscoveryClient;
import org.zowe.apiml.registry.client.HttpRegistryTransport;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.model.ServiceInstance;

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
@EnableConfigurationProperties(RegistryFetchProperties.class)
@AutoConfigureBefore({
    SimpleDiscoveryClientAutoConfiguration.class,
    SimpleReactiveDiscoveryClientAutoConfiguration.class
})
public class RegistryClientAutoConfiguration {

    /**
     * How this service describes itself.
     * <p>
     * Declared here rather than through {@code @EnableConfigurationProperties} so the ports can be set before
     * {@code eureka.instance.*} is bound over them, which is the order Spring Cloud used and the only reason a
     * service that does not configure a port still registers the one it listens on. See
     * {@link RegistryInstanceDefaults#applyPorts} - ZAAS is the service that depends on it, and it registered
     * itself on 80 and 443 without it.
     */
    @Bean
    RegistryInstanceProperties registryInstanceProperties(Environment environment) {
        RegistryInstanceProperties properties = new RegistryInstanceProperties();
        RegistryInstanceDefaults.applyPorts(properties, environment);
        return properties;
    }

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

        /**
         * The blocking view of the registry.
         * <p>
         * The condition is on this class, not on {@code DiscoveryClient} - a bare {@code @ConditionalOnMissingBean}
         * resolves to the method's return type. Naming the interface instead makes the bean back off against Spring
         * Cloud's {@code CompositeDiscoveryClient}, which is {@code @Primary} and is itself a
         * {@code DiscoveryClient}: the registry client was then never created at all, while
         * {@code RegistryClientLifecycle} went on registering this service and filling a cache that nothing read.
         * Every consumer saw an empty registry - the Gateway built no routes, and the API Catalog and ZAAS reported
         * DOWN - with registration working perfectly, which is what made it hard to see.
         * <p>
         * Coexisting is the intended design: the composite aggregates every discovery implementation, so this is
         * how Spring Cloud's own clients are declared.
         * <p>
         * The name carries the {@code apiml} prefix because the Discovery Service ships its own
         * {@code RegistryDiscoveryClient} as a component, whose bean name is {@code registryDiscoveryClient}. A
         * method of that name here collides with it and stops the Discovery Service from starting at all.
         */
        @Bean
        @ConditionalOnMissingBean
        CachedRegistryDiscoveryClient apimlRegistryDiscoveryClient(RegistryClient registryClient) {
            return new CachedRegistryDiscoveryClient(registryClient);
        }

        /**
         * The reactive view, which is what a reactive application actually reads.
         * <p>
         * Guarded on reactor rather than declared unconditionally: a servlet service has no use for it and must
         * not gain a reactor dependency. Both this and the blocking client are declared here, before Spring
         * Cloud's simple fallbacks, so those back off instead of leaving an application with an empty client that
         * starts, registers and routes nothing.
         */
        @Bean
        @ConditionalOnClass(name = "reactor.core.publisher.Flux")
        @ConditionalOnMissingBean
        CachedRegistryReactiveDiscoveryClient apimlRegistryReactiveDiscoveryClient(RegistryClient registryClient) {
            return new CachedRegistryReactiveDiscoveryClient(registryClient);
        }

        @Bean
        RegistryClientLifecycle registryClientLifecycle(
            RegistryClient registryClient,
            RegistryFetchProperties clientConfig,
            RegistryInstanceProperties instanceConfig,
            ApplicationEventPublisher publisher,
            ObjectProvider<HealthStatusSource> healthStatusSource
        ) {
            return new RegistryClientLifecycle(
                registryClient, clientConfig, instanceConfig, publisher, healthStatusSource.getIfAvailable());
        }

        /**
         * The {@code /application/eurekaversion} endpoint, which the integration startup check reads on every
         * instance to decide whether they have converged.
         * <p>
         * Guarded on actuator rather than declared unconditionally: a service without it has no actuator
         * endpoints at all, and the endpoint type would not resolve.
         * <p>
         * Guarded on the services that serve this endpoint from their own registry, because a second endpoint
         * with the same id stops the application from starting:
         * <pre>
         *   Found two endpoints with the id 'eurekaversion':
         *     'apimlRegistryVersionEndpoint' and 'registryVersionEndpoint'
         * </pre>
         * None of those three declares this module, so the guards look redundant - but they are not. The
         * {@code liteLibJarAll} classpath used to start the services for the integration tests bundles every
         * module, so the Discovery Service sees this autoconfiguration whether it declares it or not. The same
         * leak put a {@code registryDiscoveryClient} bean of this module's naming into that service before.
         * <p>
         * The names are the bean names of those services' own endpoints, and the autoconfiguration test
         * asserts this bean backs off for each of them, so the guards cannot be dropped unnoticed.
         */
        @Bean
        @ConditionalOnClass(org.springframework.boot.actuate.endpoint.annotation.Endpoint.class)
        @ConditionalOnMissingBean(name = {
            "registryVersionEndpoint",
            "cachingEurekaRegistryVersionEndpoint",
            "clientEurekaRegistryVersionEndpoint"
        })
        RegistryClientVersionEndpoint apimlRegistryVersionEndpoint(RegistryClient registryClient) {
            return new RegistryClientVersionEndpoint(registryClient);
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
