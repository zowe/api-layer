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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.gateway.filters.proxyheaders.AdditionalRegistrationGatewayRegistry;
import org.zowe.apiml.product.eureka.EurekaServiceUrlUtils;
import org.zowe.apiml.registry.client.HttpRegistryTransport;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.spring.HealthStatusSource;
import org.zowe.apiml.registry.client.spring.RegistryClientLifecycle;
import org.zowe.apiml.registry.client.spring.RegistryFetchProperties;
import org.zowe.apiml.registry.client.spring.RegistryInstanceDefaults;
import org.zowe.apiml.registry.client.spring.RegistryInstanceProperties;
import org.zowe.apiml.registry.client.spring.SelfInstanceFactory;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.model.ServiceInstance;

import javax.net.ssl.SSLContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.REGISTRATION_TYPE;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES_GATEWAY_URL;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES_SERVICE_URL;

/**
 * How the Gateway puts itself into the registry - its own, and any others it has been told to join.
 * <p>
 * Replaces the Eureka registration machinery that lived in {@code ConnectionsConfig}: a hand-built
 * {@code CloudEurekaClient} for the primary registration, one more per additional registration, a
 * {@code EurekaFactory} to construct them, an {@code AdditionalEurekaClientsHolder} to keep them from
 * colliding with Spring Cloud's autoconfigured client, a 90-line copy of Netflix's
 * {@code InstanceInfoFactory.create}, and an {@code EurekaInstanceConfig} decorator that existed only because
 * Netflix's client re-derived the registration from its config on every refresh.
 * <p>
 * The primary registration is now the autoconfiguration's; this class only overrides the instance it
 * advertises, because the Gateway advertises {@code apiml.service.externalUrl} rather than the address it
 * binds to.
 */
@Slf4j
@Configuration
public class GatewayRegistrationConfig {

    /**
     * The Gateway's own registration, advertised at its external URL.
     * <p>
     * Overrides {@code RegistryClientAutoConfiguration.selfServiceInstance} by bean name.
     */
    @Bean
    ServiceInstance selfServiceInstance(
        RegistryInstanceProperties instanceConfig,
        Environment environment,
        ObjectProvider<InetUtils> inetUtils,
        @Value("${apiml.service.externalUrl:#{null}}") String externalUrl
    ) {
        RegistryInstanceDefaults.apply(
            instanceConfig,
            environment.getProperty("spring.application.name"),
            inetUtils.getIfAvailable(() -> new InetUtils(new InetUtilsProperties())));
        return SelfInstanceFactory.create(instanceConfig, System.currentTimeMillis(), externalUrl);
    }

    /**
     * Registrations into other API ML instances.
     * <p>
     * Each is a client of its own: its own Discovery Service URLs, its own copy of this Gateway's registration
     * with {@code apiml.registrationType=additional} and possibly overridden routes, and its own lease to
     * renew. They fetch as well as register, because {@link AdditionalRegistrationGatewayRegistry} learns the
     * addresses of the *other* API ML's Gateways from what comes back, and those addresses are what make its
     * forwarded headers trustworthy.
     */
    @Bean
    AdditionalRegistrations additionalRegistrations(
        List<AdditionalRegistration> additionalRegistrations,
        ServiceInstance selfServiceInstance,
        RegistryFetchProperties primaryConfig,
        AdditionalRegistrationGatewayRegistry gatewayRegistry,
        ApplicationEventPublisher publisher,
        ObjectProvider<HealthStatusSource> healthStatusSource,
        @Qualifier("secureSslContext") ObjectProvider<SSLContext> secureSslContext,
        @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}") boolean verifyCertificates,
        @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}") boolean nonStrictVerify,
        @Value("${apiml.service.discoveryServiceUserid:eureka}") String userid,
        @Value("${apiml.service.discoveryServicePassword:password}") String password
    ) {
        List<RegistryClientLifecycle> lifecycles = new ArrayList<>(additionalRegistrations.size());

        for (AdditionalRegistration registration : additionalRegistrations) {
            List<String> urls = discoveryUrls(registration, verifyCertificates, userid, password);
            if (urls.isEmpty()) {
                log.warn("Additional registration has no discovery service URLs configured; skipping it");
                continue;
            }
            log.debug("Additional registration into {}", urls);

            var transport = new HttpRegistryTransport(
                urls,
                secureSslContext.getIfAvailable(),
                new RegistryCodec(),
                verifyCertificates && !nonStrictVerify,
                primaryConfig.getEurekaServerConnectTimeoutSeconds() * 1000,
                primaryConfig.getEurekaServerReadTimeoutSeconds() * 1000,
                userid,
                password);

            lifecycles.add(newAdditionalRegistration(
                transport, selfServiceInstance, registration, primaryConfig, gatewayRegistry, publisher,
                healthStatusSource.getIfAvailable()));
        }

        return new AdditionalRegistrations(lifecycles);
    }

    /**
     * One additional registration, transport already chosen.
     * <p>
     * Separate from the bean method so it can be exercised without opening a connection pool: the bean's only
     * other job is turning configuration strings into URLs, which {@link #discoveryUrls} does and is tested on
     * its own.
     */
    static RegistryClientLifecycle newAdditionalRegistration(
        org.zowe.apiml.registry.client.RegistryTransport transport,
        ServiceInstance selfServiceInstance,
        AdditionalRegistration registration,
        RegistryFetchProperties primaryConfig,
        AdditionalRegistrationGatewayRegistry gatewayRegistry,
        ApplicationEventPublisher publisher,
        HealthStatusSource healthStatusSource
    ) {
        var client = new RegistryClient(transport, forAdditionalRegistration(selfServiceInstance, registration));
        gatewayRegistry.watch(client);

        var config = new RegistryFetchProperties();
        config.setRegisterWithEureka(true);
        config.setFetchRegistry(true);
        config.setRegistryFetchIntervalSeconds(primaryConfig.getRegistryFetchIntervalSeconds());
        config.setInstanceInfoReplicationIntervalSeconds(primaryConfig.getInstanceInfoReplicationIntervalSeconds());
        config.setShouldUnregisterOnShutdown(primaryConfig.isShouldUnregisterOnShutdown());
        config.getHealthcheck().setEnabled(primaryConfig.getHealthcheck().isEnabled());

        return new RegistryClientLifecycle(client, config, publisher, healthStatusSource);
    }

    /**
     * When TLS validation is disabled the client certificate cannot be trusted by the Discovery Service, so the
     * additional registration falls back to basic authentication by embedding the configured Discovery Service
     * credentials into the discovery service URLs. The primary registration is handled by
     * {@link org.zowe.apiml.product.web.EurekaBasicAuthEnvironmentPostProcessor}.
     */
    static List<String> discoveryUrls(
        AdditionalRegistration registration,
        boolean verifyCertificates,
        String userid,
        String password
    ) {
        String configured = registration.getDiscoveryServiceUrls();
        if (configured == null || configured.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configured.split(","))
            .map(String::trim)
            .filter(url -> !url.isEmpty())
            .map(url -> verifyCertificates ? url : EurekaServiceUrlUtils.addCredentials(url, userid, password))
            .collect(Collectors.toList());
    }

    /**
     * This Gateway's registration as the other API ML should see it.
     * <p>
     * Marked {@code additional} so the receiving API ML knows this Gateway is not one of its own, and with the
     * routes replaced when the registration overrides them - a Gateway can serve a different path prefix in
     * each API ML it joins.
     */
    static ServiceInstance forAdditionalRegistration(ServiceInstance primary, AdditionalRegistration registration) {
        Map<String, String> metadata = new LinkedHashMap<>(primary.metadata());
        metadata.put(REGISTRATION_TYPE, EurekaMetadataDefinition.RegistrationType.ADDITIONAL.getValue());

        if (registration.getRoutes() != null && !registration.getRoutes().isEmpty()) {
            metadata.keySet().removeIf(GatewayRegistrationConfig::isRouteKey);
            int index = 0;
            for (var route : registration.getRoutes()) {
                metadata.put(String.format("apiml.routes.%d.gatewayUrl", index), route.getGatewayUrl());
                metadata.put(String.format("apiml.routes.%d.serviceUrl", index++), route.getServiceUrl());
            }
        }

        return primary.toBuilder().metadata(metadata).build();
    }

    private static boolean isRouteKey(String key) {
        return key.startsWith(ROUTES + ".")
            && (key.endsWith("." + ROUTES_GATEWAY_URL) || key.endsWith("." + ROUTES_SERVICE_URL));
    }

    /**
     * Holds the additional registrations' lifecycles.
     * <p>
     * A single bean rather than one per registration, because the number is only known at runtime, and because
     * starting and stopping them together is the behaviour that matters: each holds a lease that peers would
     * otherwise take ninety seconds to expire. A {@code SmartLifecycle} rather than a bean with a destroy
     * method, so that nothing registers until the container says the application is ready - constructing this
     * bean has no side effects.
     */
    public static class AdditionalRegistrations implements SmartLifecycle {

        private final List<RegistryClientLifecycle> lifecycles;
        private volatile boolean running;

        AdditionalRegistrations(List<RegistryClientLifecycle> lifecycles) {
            this.lifecycles = lifecycles;
        }

        public int count() {
            return lifecycles.size();
        }

        @Override
        public int getPhase() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void start() {
            lifecycles.forEach(RegistryClientLifecycle::start);
            running = true;
        }

        @Override
        public void stop() {
            lifecycles.forEach(RegistryClientLifecycle::stop);
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

    }

}
