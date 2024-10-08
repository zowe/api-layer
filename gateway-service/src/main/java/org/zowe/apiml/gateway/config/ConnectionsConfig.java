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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.cloud.gateway.config.*;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.RestTemplateTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.pattern.PathPatternParser;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.config.AdditionalRegistrationCondition;
import org.zowe.apiml.config.AdditionalRegistrationParser;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.util.CorsUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientSecurityUtils;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;


//TODO this configuration should be removed as redundancy of the HttpConfig in the apiml-common
@Configuration
@Slf4j
public class ConnectionsConfig {

    private static final char[] KEYRING_PASSWORD = "password".toCharArray();

    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStorePath;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStorePath;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${server.ssl.trustStoreRequired:false}")
    private boolean trustStoreRequired;

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    @Value("${apiml.connection.idleConnectionTimeoutSeconds:#{5}}")
    private int idleConnTimeoutSeconds;

    @Value("${apiml.connection.timeout:60000}")
    private int requestTimeout;
    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;
    private final ApplicationContext context;
    private static final ApimlLogger apimlLog = ApimlLogger.of(ConnectionsConfig.class, YamlMessageServiceInstance.getInstance());
    private HttpsFactory httpsFactory;

    public ConnectionsConfig(ApplicationContext context) {
        this.context = context;
    }

    @PostConstruct
    public void updateConfigParameters() {
        ServerProperties serverProperties = context.getBean(ServerProperties.class);
        if (SecurityUtils.isKeyring(keyStorePath)) {
            keyStorePath = SecurityUtils.formatKeyringUrl(keyStorePath);
            serverProperties.getSsl().setKeyStore(keyStorePath);
            if (keyStorePassword == null) keyStorePassword = KEYRING_PASSWORD;
        }
        if (SecurityUtils.isKeyring(trustStorePath)) {
            trustStorePath = SecurityUtils.formatKeyringUrl(trustStorePath);
            serverProperties.getSsl().setTrustStore(trustStorePath);
            if (trustStorePassword == null) trustStorePassword = KEYRING_PASSWORD;
        }
        httpsFactory = factory();
    }

    public HttpsFactory factory() {
        HttpsConfig config = HttpsConfig.builder()
            .protocol(protocol)
            .verifySslCertificatesOfServices(verifySslCertificatesOfServices)
            .nonStrictVerifySslCertificatesOfServices(nonStrictVerifySslCertificatesOfServices)
            .trustStorePassword(trustStorePassword).trustStoreRequired(trustStoreRequired)
            .idleConnTimeoutSeconds(idleConnTimeoutSeconds).requestConnectionTimeout(requestTimeout)
            .trustStore(trustStorePath).trustStoreType(trustStoreType)
            .keyAlias(keyAlias).keyStore(keyStorePath).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
        log.info("Using HTTPS configuration: {}", config.toString());

        return new HttpsFactory(config);
    }

    /**
     * @param httpClient             default http client
     * @param headersFiltersProvider header filter for spring gateway router
     * @param properties             client HTTP properties
     * @return instance of NettyRoutingFilterApiml
     */
    @Bean
    public NettyRoutingFilterApiml createNettyRoutingFilterApiml(HttpClient httpClient, ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider, HttpClientProperties properties) {
        return new NettyRoutingFilterApiml(getHttpClient(httpClient, false), getHttpClient(httpClient, true), headersFiltersProvider, properties);
    }

    public HttpClient getHttpClient(HttpClient httpClient, boolean useClientCert) {
        var sslContextBuilder = SslProvider.builder().sslContext(getSslContext(useClientCert));
        if (!nonStrictVerifySslCertificatesOfServices) {
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
    public static BeanPostProcessor routingFilterHandler(ApplicationContext context) {
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

    /**
     * @return io.netty.handler.ssl.SslContext for http client.
     */
    SslContext getSslContext(boolean setKeystore) {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();

            KeyStore trustStore = SecurityUtils.loadKeyStore(trustStoreType, trustStorePath, trustStorePassword);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            builder.trustManager(trustManagerFactory);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (setKeystore) {
                log.info("Loading keystore: {}: {}", keyStoreType, keyStorePath);
                KeyStore keyStore = SecurityUtils.loadKeyStore(keyStoreType, keyStorePath, keyStorePassword);
                keyManagerFactory.init(keyStore, keyStorePassword);
                builder.keyManager(keyManagerFactory);
            } else {
                KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
                emptyKeystore.load(null, null);
                keyManagerFactory.init(emptyKeystore, null);
                builder.keyManager(keyManagerFactory);
            }

            return builder.build();
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, factory().getConfig());
        }
    }

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    @ConditionalOnMissingBean(EurekaClient.class)
    public CloudEurekaClient primaryEurekaClient(ApplicationInfoManager manager, EurekaClientConfig config,
                                                 @Autowired(required = false) HealthCheckHandler healthCheckHandler) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }
        RestTemplateDiscoveryClientOptionalArgs args1 = defaultArgs(getDefaultEurekaClientHttpRequestFactorySupplier());
        RestTemplateTransportClientFactories factories = new RestTemplateTransportClientFactories(args1);
        final CloudEurekaClient cloudEurekaClient = new CloudEurekaClient(appManager, config, factories, args1, this.context);
        cloudEurekaClient.registerHealthCheck(healthCheckHandler);
        return cloudEurekaClient;
    }

    private static DefaultEurekaClientHttpRequestFactorySupplier getDefaultEurekaClientHttpRequestFactorySupplier() {
        RestTemplateTimeoutProperties properties = new RestTemplateTimeoutProperties();
        properties.setConnectTimeout(180000);
        properties.setConnectRequestTimeout(180000);
        properties.setSocketTimeout(180000);
        return new DefaultEurekaClientHttpRequestFactorySupplier(properties);
    }

    public RestTemplateDiscoveryClientOptionalArgs defaultArgs(DefaultEurekaClientHttpRequestFactorySupplier factorySupplier) {
        RestTemplateDiscoveryClientOptionalArgs clientArgs = new RestTemplateDiscoveryClientOptionalArgs(factorySupplier);

        if (eurekaServerUrl.startsWith("http://")) {
            apimlLog.log("org.zowe.apiml.common.insecureHttpWarning");
        } else {
            clientArgs.setSSLContext(httpsFactory.getSslContext());
            clientArgs.setHostnameVerifier(httpsFactory.getHostnameVerifier());
        }

        return clientArgs;
    }

    @Bean
    public List<AdditionalRegistration> additionalRegistration() {
        List<AdditionalRegistration> additionalRegistrations = new AdditionalRegistrationParser().extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional registration: {}", additionalRegistrations.size(), additionalRegistrations);
        return additionalRegistrations;
    }

    @Bean(destroyMethod = "shutdown")
    @Conditional(AdditionalRegistrationCondition.class)
    @RefreshScope
    public AdditionalEurekaClientsHolder additionalEurekaClientsHolder(ApplicationInfoManager manager,
                                                                       EurekaClientConfig config,
                                                                       List<AdditionalRegistration> additionalRegistrations,
                                                                       EurekaFactory eurekaFactory,
                                                                       @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        List<CloudEurekaClient> additionalClients = new ArrayList<>(additionalRegistrations.size());
        for (AdditionalRegistration apimlRegistration : additionalRegistrations) {
            CloudEurekaClient cloudEurekaClient = registerInTheApimlInstance(config, apimlRegistration, manager, eurekaFactory);
            additionalClients.add(cloudEurekaClient);
            cloudEurekaClient.registerHealthCheck(healthCheckHandler);
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
        InstanceInfo newInfo = eurekaFactory.createInstanceInfo(eurekaInstanceConfig);
        RestTemplateDiscoveryClientOptionalArgs args1 = defaultArgs(getDefaultEurekaClientHttpRequestFactorySupplier());
        RestTemplateTransportClientFactories factories = new RestTemplateTransportClientFactories(args1);
        return eurekaFactory.createCloudEurekaClient(eurekaInstanceConfig, newInfo, configBean, context, factories, args1);
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults()).timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(requestTimeout)).build()).build());
    }

    @Bean
    public HttpClientFactory gatewayHttpClientFactory(
        HttpClientProperties properties,
        ServerProperties serverProperties, List<HttpClientCustomizer> customizers,
        HttpClientSslConfigurer sslConfigurer
    ) {
        SslContext sslContext = getSslContext(false);
        return new HttpClientFactory(properties, serverProperties, sslConfigurer, customizers) {
            @Override
            protected HttpClient createInstance() {
                return super.createInstance().secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            }
        };
    }

    @Bean
    @Primary
    public WebClient webClient(HttpClient httpClient) {
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(getHttpClient(httpClient, false))).build();
    }

    @Bean
    public WebClient webClientClientCert(HttpClient httpClient) {
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(getHttpClient(httpClient, true))).build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(RoutePredicateHandlerMapping handlerMapping, GlobalCorsProperties globalCorsProperties, CorsUtils corsUtils) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.setCorsConfigurations(globalCorsProperties.getCorsConfigurations());
        corsUtils.registerDefaultCorsConfiguration(source::registerCorsConfiguration);
        handlerMapping.setCorsConfigurationSource(source);
        return source;
    }

    @Bean
    public CorsUtils corsUtils() {
        return new CorsUtils(corsEnabled, null);
    }

}
