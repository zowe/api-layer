/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.discovery.metadata.MetadataTranslationService;
import org.zowe.apiml.discovery.registry.interceptor.ConformanceWarningInterceptor;
import org.zowe.apiml.discovery.registry.interceptor.DomainAllowListInterceptor;
import org.zowe.apiml.discovery.registry.interceptor.MetadataTranslationInterceptor;
import org.zowe.apiml.product.eureka.web.MetadataFilterService;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.codec.RegistryJacksonModule;
import org.zowe.apiml.registry.spi.RegistrationInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Wires the replacement registry into the Discovery Service.
 * <p>
 * This is what {@code EurekaConfig} used to be, minus the fight with Spring Cloud: no bean-definition removal to
 * displace an autoconfigured registry, no reflection into private Eureka state, no static
 * {@code EurekaServerContextHolder}.
 * <p>
 * The {@code eureka.*} property names are read unchanged on purpose. The cutover is a hard one - there is no
 * runtime toggle - so an existing deployment must keep behaving as it does today without anyone editing
 * configuration. Renaming these to {@code apiml.registry.*} is a later, separate, documented migration.
 */
@Configuration
@Slf4j
public class RegistryConfiguration {

    @Value("${apiml.discovery.serviceIdPrefixReplacer:#{null}}")
    private String serviceIdPrefixReplacer;

    @Value("${eureka.server.enableSelfPreservation:true}")
    private boolean selfPreservationEnabled;

    @Value("${eureka.server.renewalPercentThreshold:0.85}")
    private double renewalPercentThreshold;

    @Value("${eureka.server.expectedClientRenewalIntervalSeconds:30}")
    private int expectedClientRenewalIntervalSeconds;

    @Value("${eureka.server.retentionTimeInMSInDeltaQueue:180000}")
    private long deltaRetentionMs;

    @Value("${eureka.instance.leaseExpirationDurationInSeconds:90}")
    private int defaultLeaseDurationSecs;

    @Value("${eureka.instance.leaseRenewalIntervalInSeconds:30}")
    private int defaultRenewalIntervalSecs;

    /**
     * Named {@code registrySettings}, not {@code registryConfig}.
     * <p>
     * gateway-service has an {@code @Configuration} class called {@code RegistryConfig}, whose bean name is
     * {@code registryConfig}. In the modulith both live in one context, and the collision makes Spring hand the
     * gateway's {@code @Bean} factory methods the wrong instance - failing with "Illegal factory instance for
     * factory method 'gatewayServiceAddress'", which points nowhere near the actual cause.
     */
    @Bean
    public RegistrySettings registrySettings() {
        return new RegistrySettings(
            selfPreservationEnabled,
            renewalPercentThreshold,
            expectedClientRenewalIntervalSeconds,
            deltaRetentionMs,
            defaultLeaseDurationSecs,
            defaultRenewalIntervalSecs
        );
    }

    @Bean
    public ServiceIdPrefixRewriter serviceIdPrefixRewriter() {
        return new ServiceIdPrefixRewriter(serviceIdPrefixReplacer);
    }

    /**
     * The registration interceptor chain, in {@link RegistrationInterceptor#order()} order: translate metadata,
     * then warn about non-conformance, then apply the allow list. Translation has to come first so the later two
     * inspect current-version metadata rather than V1 keys.
     */
    @Bean
    public List<RegistrationInterceptor> registrationInterceptors(
        MetadataTranslationService metadataTranslationService,
        MetadataDefaultsService metadataDefaultsService,
        MetadataFilterService metadataFilterService
    ) {
        List<RegistrationInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new MetadataTranslationInterceptor(metadataTranslationService, metadataDefaultsService));
        interceptors.add(new ConformanceWarningInterceptor());
        interceptors.add(new DomainAllowListInterceptor(metadataFilterService));
        return interceptors;
    }

    @Bean
    public ServiceRegistry serviceRegistry(
        RegistrySettings registrySettings,
        List<RegistrationInterceptor> interceptors,
        ServiceIdPrefixRewriter rewriter
    ) {
        ServiceRegistry registry = new InMemoryServiceRegistry(
            registrySettings, interceptors, System::currentTimeMillis);

        if (rewriter.enabled()) {
            log.info("Service ID prefix replacement is configured; registry writes will be rewritten");
            return new PrefixRewritingServiceRegistry(registry, rewriter);
        }
        return registry;
    }

    @Bean
    public RegistryCodec registryCodec() {
        return new RegistryCodec();
    }

    /**
     * Makes the registry model serialisable by the application's general-purpose ObjectMapper.
     * <p>
     * Needed because {@code ServiceInstance} carries no Jackson annotations and uses record-style accessors, so
     * bean introspection cannot see it. Without this the static-definition refresh endpoints would return empty
     * objects for their instances.
     */
    @Bean
    public RegistryJacksonModule registryJacksonModule() {
        return new RegistryJacksonModule();
    }

}
