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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.config.AdditionalRegistrationCondition;
import org.zowe.apiml.config.AdditionalRegistrationParser;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClientFactory;
import org.zowe.apiml.product.web.HttpConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

/**
 * This configuration override bean EurekaClient with custom ApimlDiscoveryClient. This bean offer additional method
 * fetchRegistry. User can call this method to asynchronously fetch new data from discovery service. There is no time
 * to fetching.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speeds up distribution of
 * changes in whole gateway.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
// There is an issue - clashing of XML configuration
@DependsOn({"cacheConfig", "cacheManager"})
public class DiscoveryClientConfig {
    private final ApimlDiscoveryClientFactory apimlDiscoveryClientFactory;
    private final ApplicationContext context;
    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;
//    private final EurekaJerseyClientImpl.EurekaJerseyClientBuilder eurekaJerseyClientBuilder;

    @Autowired
    private HttpConfig httpConfig;

    @Bean
    public List<AdditionalRegistration> additionalRegistration(StandardEnvironment environment) {
        List<AdditionalRegistration> additionalRegistrations = new AdditionalRegistrationParser().extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional registration: {}", additionalRegistrations.size(), additionalRegistrations);
        return additionalRegistrations;
    }

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public ApimlDiscoveryClient primaryApimlEurekaClient(ApplicationInfoManager manager,
                                                         EurekaClientConfigBean config,
                                                         @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager = ProxyUtils.getTargetObject(manager);

        RestTemplateDiscoveryClientOptionalArgs args1 = defaultArgs(getDefaultEurekaClientHttpRequestFactorySupplier());
        RestTemplateTransportClientFactories factories = new RestTemplateTransportClientFactories(args1);

        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(
                appManager, config, this.context, factories, args1);

        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        return discoveryClientClient;
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
            log.info("Eureka client is using insecure protocol");
        } else {
            System.setProperty("com.netflix.eureka.shouldSSLConnectionsUseSystemSocketFactory", "true");

            clientArgs.setSSLContext(httpConfig.getSecureSslContext());
            clientArgs.setHostnameVerifier(httpConfig.getSecureHostnameVerifier());
        }

        return clientArgs;
    }

    @Bean(destroyMethod = "shutdown")
    @Conditional({AdditionalRegistrationCondition.class})
    @RefreshScope
    public DiscoveryClientWrapper additionalDiscoveryClientWrapper(ApplicationInfoManager manager,
                                                                   EurekaClientConfig config,
                                                                   @Autowired(required = false) HealthCheckHandler healthCheckHandler,
                                                                   List<AdditionalRegistration> additionalRegistrations
    ) {

        List<ApimlDiscoveryClient> discoveryClientsList = new ArrayList<>(additionalRegistrations.size());
        for (AdditionalRegistration apimlRegistration : additionalRegistrations) {
            ApimlDiscoveryClient additionalApimlRegistration = registerInTheApimlInstance(config, healthCheckHandler, apimlRegistration, manager);
            discoveryClientsList.add(additionalApimlRegistration);
        }

        return new DiscoveryClientWrapper(discoveryClientsList);
    }

//    private ApimlDiscoveryClient registerInTheApimlInstance(EurekaClientConfig config, HealthCheckHandler healthCheckHandler, AdditionalRegistration apimlRegistration, ApplicationInfoManager appManager) {
//
//        EurekaClientConfigBean configBean = new EurekaClientConfigBean();
//        BeanUtils.copyProperties(config, configBean);
//
//        Map<String, String> urls = new HashMap<>();
//        log.debug("additional registration: {}", apimlRegistration.getDiscoveryServiceUrls());
//        urls.put(DEFAULT_ZONE, apimlRegistration.getDiscoveryServiceUrls());
//
//        configBean.setServiceUrl(urls);
//
//        MutableDiscoveryClientOptionalArgs args = new MutableDiscoveryClientOptionalArgs();
//        args.setEurekaJerseyClient(eurekaJerseyClientBuilder.build());
//
//        InstanceInfo newInfo = apimlDiscoveryClientFactory.createInstanceInfo(appManager.getEurekaInstanceConfig());
//        InstanceInfo ii = rewriteInstanceInfoRoutes(apimlRegistration, newInfo);
//
//        ApplicationInfoManager perClientAppManager = new ApplicationInfoManager(appManager.getEurekaInstanceConfig(), ii, null);
//        final ApimlDiscoveryClient discoveryClientClient = apimlDiscoveryClientFactory.buildApimlDiscoveryClient(perClientAppManager, configBean, args, context);
//        discoveryClientClient.registerHealthCheck(healthCheckHandler);
//
//        return discoveryClientClient;
//    }

    private ApimlDiscoveryClient registerInTheApimlInstance(EurekaClientConfig config, HealthCheckHandler healthCheckHandler, AdditionalRegistration apimlRegistration, ApplicationInfoManager appManager) {

        log.debug("additional registration: {}", apimlRegistration.getDiscoveryServiceUrls());
        Map<String, String> urls = new HashMap<>();
        urls.put(DEFAULT_ZONE, apimlRegistration.getDiscoveryServiceUrls());

        EurekaClientConfigBean configBean = new EurekaClientConfigBean();
        BeanUtils.copyProperties(config, configBean);
        configBean.setServiceUrl(urls);

        EurekaInstanceConfig eurekaInstanceConfig = appManager.getEurekaInstanceConfig();
        InstanceInfo newInfo = apimlDiscoveryClientFactory.createInstanceInfo(appManager.getEurekaInstanceConfig());
        InstanceInfo ii = rewriteInstanceInfoRoutes(apimlRegistration, newInfo);
        RestTemplateDiscoveryClientOptionalArgs args1 = defaultArgs(getDefaultEurekaClientHttpRequestFactorySupplier());
        RestTemplateTransportClientFactories factories = new RestTemplateTransportClientFactories(args1);
        ApplicationInfoManager perClientAppManager = new ApplicationInfoManager(appManager.getEurekaInstanceConfig(), ii, null);
        final ApimlDiscoveryClient discoveryClientClient = apimlDiscoveryClientFactory.buildApimlDiscoveryClient(perClientAppManager, configBean, context, factories, args1);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        return discoveryClientClient;
//        return eurekaFactory.createCloudEurekaClient(eurekaInstanceConfig, newInfo, configBean, context,factories, args1);
    }

    private InstanceInfo rewriteInstanceInfoRoutes(AdditionalRegistration apimlRegistration, InstanceInfo newInfo) {
        if (!CollectionUtils.isEmpty(apimlRegistration.getRoutes())) {
            Map<String, String> metadataWithRoutes = newInfo.getMetadata().entrySet().stream().filter(entry -> !entry.getKey().startsWith(ROUTES)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            int index = 0;
            for (AdditionalRegistration.Route route : apimlRegistration.getRoutes()) {
                metadataWithRoutes.put(ROUTES + "." + index + "." + ROUTES_GATEWAY_URL, route.getGatewayUrl());
                metadataWithRoutes.put(ROUTES + "." + index + "." + ROUTES_SERVICE_URL, route.getServiceUrl());
                index++;
            }
            InstanceInfo.Builder builder = new InstanceInfo.Builder(newInfo);
            builder.setMetadata(metadataWithRoutes);
            return builder.build();
        }
        return newInfo;
    }
}
