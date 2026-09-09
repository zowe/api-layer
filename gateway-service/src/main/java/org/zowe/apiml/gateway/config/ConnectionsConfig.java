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

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilter;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.config.AdditionalRegistrationCondition;
import org.zowe.apiml.config.AdditionalRegistrationParser;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.gateway.filters.proxyheaders.AdditionalRegistrationGatewayRegistry;
import org.zowe.apiml.gateway.filters.proxyheaders.X509AndGwAwareXForwardedHeadersFilter;
import org.zowe.apiml.gateway.filters.security.SecFetchSiteFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.eureka.EurekaServiceUrlUtils;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.common.util.ConnectionUtil;
import org.zowe.apiml.util.CorsUtils;
import reactor.netty.http.client.HttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

//TODO this configuration should be removed as redundancy of the HttpConfig in the apiml-common
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ConnectionsConfig {

    private static final ApimlLogger apimlLog = ApimlLogger.of(ConnectionsConfig.class, YamlMessageServiceInstance.getInstance());

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    @Value("${apiml.discovery.userid:#{null}}")
    private String discoveryUserid;

    @Value("${apiml.discovery.password:#{null}}")
    private char[] discoveryPassword;

    @Value("${apiml.service.corsEnabled:false}")
    private boolean gatewayCorsEnabled;

    @Value("${apiml.service.corsAllowedMethods:GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS}")
    private List<String> corsAllowedMethods;

    @Value("#{T(org.springframework.util.StringUtils).hasText('${apiml.service.corsDefaultAllowedOrigins:}') ? '${apiml.service.corsDefaultAllowedOrigins:}' : 'https://${apiml.service.hostname:localhost}:${apiml.service.port}'}")
    private String corsDefaultAllowedOrigins;

    @Value("#{T(org.springframework.util.StringUtils).hasText('${apiml.service.corsDefaultAllowedHeaders:}') ? '${apiml.service.corsDefaultAllowedHeaders:}' : '*'}")
    private String corsDefaultAllowedHeaders;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.port}")
    private String port;

    @Value("${server.attlsClient.enabled:false}")
    private boolean isClientAttlsEnabled;

    private final ApplicationContext context;
    private final HttpConfig config;

    @Value("${apiml.service.externalUrl:}")
    private String externalUrl;

    @Value("${apiml.service.corsAllowedEndpoints:/gateway/**}")
    private final List<String> corsEnabledEndpoints;

    /**
     * @param httpClient             default http client
     * @param headersFiltersProvider header filter for spring gateway router
     * @param properties             client HTTP properties
     * @return instance of NettyRoutingFilterApiml
     */
    @Bean
    NettyRoutingFilterApiml createNettyRoutingFilterApiml(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider, HttpClientProperties properties) {
        boolean isKeyLoadPrevented = StringUtils.isBlank(config.getKeyStorePath()) && isClientAttlsEnabled;
        log.debug("ConnectionsConfig.createNettyRoutingFilterApiml - Creating routing filter with SSL config: verifySslCertificatesOfServices={}, nonStrictVerifySslCertificatesOfServices={}, isKeyLoadPrevented={}",
            config.isVerifySslCertificatesOfServices(),
            config.isNonStrictVerifySslCertificatesOfServices(),
            isKeyLoadPrevented);
        try {
            return new NettyRoutingFilterApiml(
                ConnectionUtil.getHttpClient(config, httpClient, false),
                ConnectionUtil.getHttpClient(config, httpClient, !isKeyLoadPrevented),
                headersFiltersProvider, properties
            );
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config.httpsConfig());
        }
    }

    /**
     * This bean processor is used to override bean routingFilter defined at
     * org.springframework.cloud.gateway.config.GatewayAutoConfiguration.NettyConfiguration#routingFilter(HttpClient, ObjectProvider, HttpClientProperties)
     * <p>
     * There is no simple way how to override this specific bean, but bean processing could handle that.
     *
     * @return bean processor to replace NettyRoutingFilter by NettyRoutingFilterApiml
     */
    @Bean
    static BeanPostProcessor routingFilterHandler(ApplicationContext context) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if ("routingFilter".equals(beanName)) {
                    log.debug("Updating routing bean {}", NettyRoutingFilterApiml.class);
                    // once is creating original bean by autoconfiguration replace it with custom implementation
                    return context.getBean(NettyRoutingFilterApiml.class);
                }
                // do not touch any other bean
                return bean;
            }
        };
    }

    /*
     * The @DependsOn("discoveryClient") that used to be here named a Spring Cloud Netflix bean that no longer
     * exists. It is not needed either: this bean only parses environment variables, and what consumes it -
     * GatewayRegistrationConfig.additionalRegistrations - is a SmartLifecycle in the last startup phase.
     */
    @Bean
    List<AdditionalRegistration> additionalRegistration() {
        List<AdditionalRegistration> additionalRegistrations = new AdditionalRegistrationParser().extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional registration: {}", additionalRegistrations.size(), additionalRegistrations);
        return additionalRegistrations;
    }

    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .timeLimiterConfig(
                TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofMillis(config.getRequestConnectionTimeout()))
                    .build()).build());
    }

    @Bean
    CorsUtils corsUtils() {
        return CorsUtils.builder()
            .gatewayCorsEnabled(gatewayCorsEnabled)
            .corsAllowedEndpoints(corsEnabledEndpoints)
            .defaultAllowedCorsHttpMethods(corsAllowedMethods)
            .defaultAllowedCorsOrigins(Arrays.asList(corsDefaultAllowedOrigins.split(",")))
            .defaultAllowedCorsHeaders(Arrays.asList(corsDefaultAllowedHeaders.split(",")))
            .defaultAllowCredentials(true)
            .build();
    }

    @Bean
    WebFilter corsWebFilter(ServiceCorsUpdater serviceCorsUpdater) {
        return new CorsWebFilter(serviceCorsUpdater.getUrlBasedCorsConfigurationSource());
    }

    /**
     * Token-free CSRF protection for cross-site requests based on the {@code Sec-Fetch-Site} request
     * header. A request the CORS filter ({@link #corsWebFilter(ServiceCorsUpdater)}) already validates
     * the {@code Origin} of is deferred to it; anything else is allowed only as a safe top-level
     * navigation and rejected otherwise. See {@link SecFetchSiteFilter} for the full policy.
     */
    @Bean
    @ConditionalOnProperty(name = "apiml.security.secFetch.enabled", havingValue = "true")
    WebFilter secFetchSiteFilter(
        ServiceCorsUpdater serviceCorsUpdater,
        @Value("${apiml.security.secFetch.safeNavigationModes:navigate,same-origin}") Set<String> safeNavigationModes,
        @Value("${apiml.security.secFetch.safeNavigationDestinations:#{null}}") Set<String> safeNavigationDestinations
    ) {
        return new SecFetchSiteFilter(
            gatewayCorsEnabled,
            serviceCorsUpdater.getUrlBasedCorsConfigurationSource(),
            safeNavigationModes,
            safeNavigationDestinations
        );
    }

}
