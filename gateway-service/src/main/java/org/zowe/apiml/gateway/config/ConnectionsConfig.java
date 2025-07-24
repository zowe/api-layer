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

import com.google.common.annotations.VisibleForTesting;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.cloud.gateway.config.HttpClientFactory;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.config.HttpClientSslConfigurer;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.RestClientTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.config.AdditionalRegistrationCondition;
import org.zowe.apiml.config.AdditionalRegistrationParser;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.gateway.filters.proxyheaders.AdditionalRegistrationGatewayRegistry;
import org.zowe.apiml.gateway.filters.proxyheaders.X509AndGwAwareXForwardedHeadersFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.util.CorsUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientSecurityUtils;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.REGISTRATION_TYPE;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES_GATEWAY_URL;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES_SERVICE_URL;


//TODO this configuration should be removed as redundancy of the HttpConfig in the apiml-common
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ConnectionsConfig {

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;
    private final ApplicationContext context;
    private final HttpConfig config;
    private static final ApimlLogger apimlLog = ApimlLogger.of(ConnectionsConfig.class, YamlMessageServiceInstance.getInstance());

    @Value("${apiml.service.externalUrl:}")
    private String externalUrl;

    /**
     * @param httpClient             default http client
     * @param headersFiltersProvider header filter for spring gateway router
     * @param properties             client HTTP properties
     * @return instance of NettyRoutingFilterApiml
     */
    @Bean
    NettyRoutingFilterApiml createNettyRoutingFilterApiml(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider, HttpClientProperties properties) {
        return new NettyRoutingFilterApiml(getHttpClient(httpClient, false), getHttpClient(httpClient, true), headersFiltersProvider, properties);
    }

    public HttpClient getHttpClient(HttpClient httpClient, boolean useClientCert) {
        var sslContextBuilder = SslProvider.builder().sslContext(getSslContext(useClientCert));
        if (!config.isNonStrictVerifySslCertificatesOfServices()) {
            sslContextBuilder.handlerConfigurator(HttpClientSecurityUtils.HOSTNAME_VERIFICATION_CONFIGURER);
        }
        return httpClient.secure(sslContextBuilder.build());
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

    @VisibleForTesting
    X509KeyManager x509KeyManagerSelectedAlias(KeyManagerFactory keyManagerFactory) {
        return new X509KeyManagerSelectedAlias(keyManagerFactory, config.getKeyAlias());
    }

    /**
     * @return io.netty.handler.ssl.SslContext for http client.
     */
    SslContext getSslContext(boolean setKeystore) {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();

            KeyStore trustStore = SecurityUtils.loadKeyStore(
                config.getTrustStoreType(), config.getTrustStorePath(), config.getTrustStorePassword());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            builder.trustManager(trustManagerFactory);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (setKeystore) {
                log.info("Loading keystore: {}: {}", config.getKeyStoreType(), config.getKeyStorePath());
                KeyStore keyStore = SecurityUtils.loadKeyStore(
                    config.getKeyStoreType(), config.getKeyStorePath(), config.getKeyStorePassword());
                keyManagerFactory.init(keyStore, config.getKeyStorePassword());
                builder.keyManager(x509KeyManagerSelectedAlias(keyManagerFactory));
            } else {
                KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
                emptyKeystore.load(null, null);
                keyManagerFactory.init(emptyKeystore, null);
                builder.keyManager(keyManagerFactory);
            }

            if (config.isVerifySslCertificatesOfServices() && config.isNonStrictVerifySslCertificatesOfServices()) {
                builder.endpointIdentificationAlgorithm(null);
            }

            return builder.build();
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config.httpsConfig());
        }
    }

    @Bean(destroyMethod = "shutdown", name = "eurekaClient")
    @RefreshScope
    @ConditionalOnMissingBean(EurekaClient.class)
    CloudEurekaClient primaryEurekaClient(ApplicationInfoManager manager, EurekaClientConfig config,
                                                 @Autowired(required = false) HealthCheckHandler healthCheckHandler) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }
        RestClientDiscoveryClientOptionalArgs args1 = defaultArgs(getDefaultEurekaClientHttpRequestFactorySupplier());
        RestClientTransportClientFactories factories = new RestClientTransportClientFactories(args1);
        final CloudEurekaClient cloudEurekaClient = new CloudEurekaClient(appManager, config, factories, args1, this.context);
        cloudEurekaClient.registerHealthCheck(healthCheckHandler);
        return cloudEurekaClient;
    }

    private static DefaultEurekaClientHttpRequestFactorySupplier getDefaultEurekaClientHttpRequestFactorySupplier() {
        RestClientTimeoutProperties properties = new RestClientTimeoutProperties();
        properties.setConnectTimeout(180000);
        properties.setConnectRequestTimeout(180000);
        properties.setSocketTimeout(180000);
        return new DefaultEurekaClientHttpRequestFactorySupplier(properties);
    }

    public RestClientDiscoveryClientOptionalArgs defaultArgs(DefaultEurekaClientHttpRequestFactorySupplier factorySupplier) {
        RestClientDiscoveryClientOptionalArgs clientArgs = new RestClientDiscoveryClientOptionalArgs(factorySupplier, RestClient::builder);

        if (eurekaServerUrl.startsWith("http://")) {
            apimlLog.log("org.zowe.apiml.common.insecureHttpWarning");
        } else {
            clientArgs.setSSLContext(config.httpsFactory().getSslContext());
            clientArgs.setHostnameVerifier(config.httpsFactory().getHostnameVerifier());
        }

        return clientArgs;
    }

    @Bean
    @DependsOn("discoveryClient")
    List<AdditionalRegistration> additionalRegistration() {
        List<AdditionalRegistration> additionalRegistrations = new AdditionalRegistrationParser().extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional registration: {}", additionalRegistrations.size(), additionalRegistrations);
        return additionalRegistrations;
    }

    @Bean(destroyMethod = "shutdown")
    @Conditional(AdditionalRegistrationCondition.class)
    @RefreshScope
    AdditionalEurekaClientsHolder additionalEurekaClientsHolder(
        ApplicationInfoManager manager,
        EurekaClientConfig config,
        List<AdditionalRegistration> additionalRegistrations,
        EurekaFactory eurekaFactory,
        @Autowired(required = false) HealthCheckHandler healthCheckHandler,
        AdditionalRegistrationGatewayRegistry additionalRegistrationGatewayRegistry,
        Optional<X509AndGwAwareXForwardedHeadersFilter> x509awareXForwardedHeadersFilter
    ) {
        List<CloudEurekaClient> additionalClients = new ArrayList<>(additionalRegistrations.size());
        for (AdditionalRegistration apimlRegistration : additionalRegistrations) {
            CloudEurekaClient cloudEurekaClient = registerInTheApimlInstance(config, apimlRegistration, manager, eurekaFactory);
            additionalClients.add(cloudEurekaClient);
            cloudEurekaClient.registerHealthCheck(healthCheckHandler);

            x509awareXForwardedHeadersFilter
                .ifPresent(__ ->
                    additionalRegistrationGatewayRegistry.registerCacheRefreshEventListener(cloudEurekaClient));
        }
        return new AdditionalEurekaClientsHolder(additionalClients);
    }

    private CloudEurekaClient registerInTheApimlInstance(EurekaClientConfig config, AdditionalRegistration apimlRegistration, ApplicationInfoManager appManager, EurekaFactory eurekaFactory) {
        log.debug("additional registration: {}", apimlRegistration.getDiscoveryServiceUrls());
        Map<String, String> urls = new HashMap<>();
        urls.put(DEFAULT_ZONE, apimlRegistration.getDiscoveryServiceUrls());

        EurekaClientConfigBean configBean = new EurekaClientConfigBean();
        BeanUtils.copyProperties(config, configBean);
        configBean.setServiceUrl(urls);

        EurekaInstanceConfig eurekaInstanceConfig = appManager.getEurekaInstanceConfig();
        InstanceInfo newInfo = create(eurekaInstanceConfig);

        updateMetadata(newInfo, apimlRegistration);

        RestClientDiscoveryClientOptionalArgs args1 = defaultArgs(getDefaultEurekaClientHttpRequestFactorySupplier());
        RestClientTransportClientFactories factories = new RestClientTransportClientFactories(args1);
        return eurekaFactory.createCloudEurekaClient(new AdditionalEurekaConfiguration(eurekaInstanceConfig, newInfo), newInfo, configBean, context, factories, args1);
    }

    private boolean isRouteKey(String key) {
        return StringUtils.startsWith(key, ROUTES + ".") &&
            (
                StringUtils.endsWith(key, "." + ROUTES_GATEWAY_URL) ||
                StringUtils.endsWith(key, "." + ROUTES_SERVICE_URL)
            );
    }

    private void updateMetadata(InstanceInfo instanceInfo, AdditionalRegistration additionalRegistration) {
        var metadata = instanceInfo.getMetadata();
        metadata.put(REGISTRATION_TYPE, EurekaMetadataDefinition.RegistrationType.ADDITIONAL.getValue());

        // if routes were override replace them in the map, otherwise use the default from the primary registration
        if (!CollectionUtils.isEmpty(additionalRegistration.getRoutes())) {
            // remove current routes
            var currentRoutes = metadata.keySet().stream().filter(this::isRouteKey).collect(Collectors.toSet());
            currentRoutes.forEach(metadata::remove);

            // generate new routes metadata
            int index = 0;
            for (var route : additionalRegistration.getRoutes()) {
                metadata.put(String.format("apiml.routes.%d.gatewayUrl", index), route.getGatewayUrl());
                metadata.put(String.format("apiml.routes.%d.serviceUrl", index++), route.getServiceUrl());
            }
        }
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
    HttpClientFactory gatewayHttpClientFactory(
        HttpClientProperties properties,
        ServerProperties serverProperties, List<HttpClientCustomizer> customizers,
        HttpClientSslConfigurer sslConfigurer
    ) {
        SslContext sslContext = getSslContext(false);
        return new HttpClientFactory(properties, serverProperties, sslConfigurer, customizers) {
            @Override
            protected HttpClient createInstance() {
                return super.createInstance()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
                    .resolver(DefaultAddressResolverGroup.INSTANCE);
            }
        };
    }

    @Bean
    @Primary
    WebClient webClient(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(getHttpClient(httpClient, false)))
            .build();
    }

    @Bean
    WebClient webClientClientCert(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(getHttpClient(httpClient, true)))
            .build();
    }

    @Bean
    CorsUtils corsUtils() {
        return new CorsUtils(corsEnabled, null);
    }

    @Bean
    WebFilter corsWebFilter(ServiceCorsUpdater serviceCorsUpdater) {
        return new CorsWebFilter(serviceCorsUpdater.getUrlBasedCorsConfigurationSource());
    }

    public InstanceInfo create(EurekaInstanceConfig config)  {
        LeaseInfo.Builder leaseInfoBuilder = LeaseInfo.Builder.newBuilder()
            .setRenewalIntervalInSecs(config.getLeaseRenewalIntervalInSeconds())
            .setDurationInSecs(config.getLeaseExpirationDurationInSeconds());

        // Builder the instance information to be registered with eureka
        // server
        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();

        String namespace = config.getNamespace();
        if (!namespace.endsWith(".")) {
            namespace = namespace + ".";
        }
        URL url;
        try {
            url = new URL(externalUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        builder
            .setNamespace(namespace)
            .setAppName(config.getAppname())
            .setInstanceId(config.getInstanceId())
            .setAppGroupName(config.getAppGroupName())
            .setDataCenterInfo(config.getDataCenterInfo())
            .setIPAddr(config.getIpAddress())
            .setHostName(url.getHost())
            .setPort(url.getPort())
            .enablePort(InstanceInfo.PortType.UNSECURE, config.isNonSecurePortEnabled())
            .setSecurePort(url.getPort())
            .enablePort(InstanceInfo.PortType.SECURE, config.getSecurePortEnabled())
            .setVIPAddress(config.getVirtualHostName())
            .setSecureVIPAddress(config.getSecureVirtualHostName())
            .setHomePageUrl(null, UriComponentsBuilder.fromUriString(externalUrl).path(config.getHomePageUrlPath()).toUriString())
            .setStatusPageUrl(null, UriComponentsBuilder.fromUriString(externalUrl).path(config.getStatusPageUrlPath()).toUriString())
            .setHealthCheckUrls(config.getHealthCheckUrlPath(), null,null)
            .setASGName(config.getASGName());

        // Start off with the STARTING state to avoid traffic
        if (!config.isInstanceEnabledOnit()) {
            InstanceInfo.InstanceStatus initialStatus = InstanceInfo.InstanceStatus.STARTING;
            if (log.isInfoEnabled()) {
                log.info("Setting initial instance status as: " + initialStatus);
            }
            builder.setStatus(initialStatus);
        }
        else {
            if (log.isInfoEnabled()) {
                log.info("Setting initial instance status as: " + InstanceInfo.InstanceStatus.UP
                    + ". This may be too early for the instance to advertise itself as available. "
                    + "You would instead want to control this via a healthcheck handler.");
            }
        }

        // Add any user-specific metadata information
        var fromUrl = UriComponentsBuilder.fromUriString(config.getHomePageUrl()).path("/").toUriString();
        var toUrl = UriComponentsBuilder.fromUriString(externalUrl).path("/").toUriString();
        for (Map.Entry<String, String> mapEntry : config.getMetadataMap().entrySet()) {
            String key = mapEntry.getKey();
            String value = mapEntry.getValue();
            // only add the metadata if the value is present
            if (value != null && !value.isEmpty()) {
                value = value.replace(fromUrl, toUrl);
                builder.add(key, value);
            }
        }

        InstanceInfo instanceInfo = builder.build();
        instanceInfo.setLeaseInfo(leaseInfoBuilder.build());
        return instanceInfo;
    }

    @RequiredArgsConstructor
    static class AdditionalEurekaConfiguration implements EurekaInstanceConfig {

        @Delegate(excludes = NonDelegated.class)
        private final EurekaInstanceConfig eurekaInstanceConfig;

        private final InstanceInfo instanceInfo;

        @Override
        public String getHostName(boolean refresh) {
            eurekaInstanceConfig.getHostName(refresh);
            return instanceInfo.getHostName();
        }

        @Override
        public String getHealthCheckUrl() {
            if (instanceInfo.isPortEnabled(InstanceInfo.PortType.UNSECURE)) {
                return instanceInfo.getHealthCheckUrl();
            }
            return instanceInfo.getSecureHealthCheckUrl();
        }

        @Override
        public String getSecureHealthCheckUrl() {
            return instanceInfo.getSecureHealthCheckUrl();
        }

        @Override
        public String getHomePageUrl() {
            return instanceInfo.getHomePageUrl();
        }

        @Override
        public String getStatusPageUrl() {
            return instanceInfo.getStatusPageUrl();
        }

        interface NonDelegated {

            String getHostName(boolean refresh);
            String getHealthCheckUrl();
            String getSecureHealthCheckUrl();
            String getHomePageUrl();
            String getStatusPageUrl();

        }

    }

    static class X509KeyManagerSelectedAlias implements X509KeyManager {

        private final X509KeyManager originalKm;
        private final String keyAlias;

        X509KeyManagerSelectedAlias(KeyManagerFactory keyManagerFactory, String keyAlias) {
            this.originalKm = (X509KeyManager) keyManagerFactory.getKeyManagers()[0];
            this.keyAlias = keyAlias;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return originalKm.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            if (keyAlias != null) {
                return keyAlias;
            }
            return originalKm.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return originalKm.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            if (keyAlias != null) {
                return keyAlias;
            }
            return originalKm.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return originalKm.getCertificateChain(alias);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return originalKm.getPrivateKey(alias);
        }

    }

}
