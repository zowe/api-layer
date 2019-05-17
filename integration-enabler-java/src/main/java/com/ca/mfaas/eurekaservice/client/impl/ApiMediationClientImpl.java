/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.impl;

import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.EurekaClientConfiguration;
import com.ca.mfaas.eurekaservice.client.config.Route;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.eurekaservice.client.util.StringUtils;
import com.ca.mfaas.eurekaservice.client.util.UrlUtils;
import com.ca.mfaas.eurekaservice.model.ApiInfo;
import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsFactory;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class ApiMediationClientImpl implements ApiMediationClient {
    private static final Logger log = LoggerFactory.getLogger(ApiMediationClientImpl.class);

    private EurekaClient eurekaClient;

    @Override
    public synchronized void register(ApiMediationServiceConfig config) {
        ApplicationInfoManager infoManager = initializeApplicationInfoManager(config);
        EurekaClientConfiguration clientConfiguration = new EurekaClientConfiguration(config);
        eurekaClient = initializeEurekaClient(infoManager, clientConfiguration, config);
        log.debug("eurekaClient.getApplicationInfoManager().getInfo().getHostName(): {}", eurekaClient.getApplicationInfoManager().getInfo().getHostName());
    }

    @Override
    public synchronized void unregister() {
        if (eurekaClient != null) {
            eurekaClient.shutdown();
        }
    }

    private EurekaClient initializeEurekaClient(
        ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig, ApiMediationServiceConfig config) {

        Ssl sslConfig = config.getSsl();
        HttpsConfig httpsConfig = HttpsConfig.builder()
            .protocol(sslConfig.getProtocol())
            .keyAlias(sslConfig.getKeyAlias())
            .keyStore(sslConfig.getKeyStore())
            .keyPassword(sslConfig.getKeyPassword())
            .keyStorePassword(sslConfig.getKeyStorePassword())
            .keyStoreType(sslConfig.getKeyStoreType())
            .trustStore(sslConfig.getTrustStore())
            .trustStoreType(sslConfig.getTrustStoreType())
            .trustStorePassword(sslConfig.getTrustStorePassword())
            .verifySslCertificatesOfServices(sslConfig.isVerifySslCertificatesOfServices())
            .build();
        HttpsFactory factory = new HttpsFactory(httpsConfig);
        EurekaJerseyClient eurekaJerseyClient = factory.createEurekaJerseyClientBuilder(
            config.getDiscoveryServiceUrls().get(0), config.getServiceId()).build();

        AbstractDiscoveryClientOptionalArgs args = new DiscoveryClient.DiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClient);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        return new DiscoveryClient(applicationInfoManager, clientConfig, args);
    }

    private ApplicationInfoManager initializeApplicationInfoManager(ApiMediationServiceConfig config) {
        EurekaInstanceConfig eurekaInstanceConfig = createEurekaInstanceConfig(config);
        InstanceInfo instanceInformation = new EurekaConfigBasedInstanceInfoProvider(eurekaInstanceConfig).get();
        return new ApplicationInfoManager(eurekaInstanceConfig, instanceInformation);
    }

    private EurekaInstanceConfig createEurekaInstanceConfig(ApiMediationServiceConfig config) {
        ApimlEurekaInstanceConfig result = new ApimlEurekaInstanceConfig();

        String hostname;
        int port;
        URL baseUrl;

        try {
            baseUrl = new URL(config.getBaseUrl());
            hostname = baseUrl.getHost();
            port = baseUrl.getPort();
        } catch (MalformedURLException e) {
            String message = String.format("baseUrl: [%s] is not valid URL", config.getBaseUrl());
            throw new InvalidParameterException(message);
        }

        result.setInstanceId(String.format("%s:%s:%s", hostname, config.getServiceId(), port));
        result.setAppname(config.getServiceId());
        result.setHostName(hostname);
        result.setAppGroupName(null);
        result.setInstanceEnabledOnit(true);
        result.setSecureVirtualHostName(config.getServiceId());
        result.setVirtualHostName(config.getServiceId());
        result.setIpAddress(config.getEureka().getIpAddress());
        result.setMetadataMap(createMetadata(config));
        result.setStatusPageUrl(config.getBaseUrl() + config.getStatusPageRelativeUrl());

        if (!StringUtils.isNullOrEmpty(config.getHomePageRelativeUrl())) {
            result.setHomePageUrl(config.getBaseUrl() + config.getHomePageRelativeUrl());
        }

        String protocol = baseUrl.getProtocol();
        result.setNonSecurePort(port);
        result.setSecurePort(port);

        switch (protocol) {
            case "http":
                result.setNonSecurePortEnabled(true);
                result.setHealthCheckUrl(config.getBaseUrl() + config.getHealthCheckRelativeUrl());
                break;
            case "https":
                result.setSecurePortEnabled(true);
                result.setSecureHealthCheckUrl(config.getBaseUrl() + config.getHealthCheckRelativeUrl());
                break;
            default:
                throw new InvalidParameterException("Invalid protocol for baseUrl property");
        }

        return result;
    }


    private Map<String, String> createMetadata(ApiMediationServiceConfig config) {
        Map<String, String> metadata = new HashMap<>();

        // fill routing metadata
        for (Route route : config.getRoutes()) {
            String gatewayUrl = UrlUtils.trimSlashes(route.getGatewayUrl());
            String serviceUrl = route.getServiceUrl();
            String key = gatewayUrl.replace("/", "-");
            metadata.put(String.format("routed-services.%s.gateway-url", key), gatewayUrl);
            metadata.put(String.format("routed-services.%s.service-url", key), serviceUrl);
        }

        // fill tile metadata
        if (config.getCatalogUiTile() != null) {
            metadata.put("mfaas.discovery.catalogUiTile.id", config.getCatalogUiTile().getId());
            metadata.put("mfaas.discovery.catalogUiTile.version", config.getCatalogUiTile().getVersion());
            metadata.put("mfaas.discovery.catalogUiTile.title", config.getCatalogUiTile().getTitle());
            metadata.put("mfaas.discovery.catalogUiTile.description", config.getCatalogUiTile().getDescription());
        }

        // fill service metadata
        metadata.put("mfaas.discovery.service.title", config.getTitle());
        metadata.put("mfaas.discovery.service.description", config.getDescription());

        // fill api-doc info
        for (ApiInfo apiInfo : config.getApiInfo()) {
            metadata.putAll(apiInfo.generateMetadata(config.getServiceId()));
        }

        return metadata;
    }
}
