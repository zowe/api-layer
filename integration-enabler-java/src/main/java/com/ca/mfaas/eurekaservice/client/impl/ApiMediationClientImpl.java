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
import com.ca.mfaas.eurekaservice.client.config.*;
import com.ca.mfaas.eurekaservice.client.util.EurekaMetadataParser;
import com.ca.mfaas.exception.MetadataValidationException;
import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.utils.UrlUtils;
import com.ca.mfaas.config.ApiInfo;
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

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;

public class ApiMediationClientImpl implements ApiMediationClient {
    private static final Logger log = LoggerFactory.getLogger(ApiMediationClientImpl.class);

    private EurekaClient eurekaClient;

    @Override
    public synchronized void register(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        EurekaClientConfiguration clientConfiguration = new EurekaClientConfiguration(config);
        try {
            ApplicationInfoManager infoManager = initializeApplicationInfoManager(config);
            eurekaClient = initializeEurekaClient(infoManager, clientConfiguration, config);
        } catch (RuntimeException rte) {
            throw new ServiceDefinitionException("Registration was not successful due to unexpected RuntimeException: ", rte);
        }
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

    private ApplicationInfoManager initializeApplicationInfoManager(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        EurekaInstanceConfig eurekaInstanceConfig = createEurekaInstanceConfig(config);
        InstanceInfo instanceInformation = new EurekaConfigBasedInstanceInfoProvider(eurekaInstanceConfig).get();
        return new ApplicationInfoManager(eurekaInstanceConfig, instanceInformation);
    }

    private EurekaInstanceConfig createEurekaInstanceConfig(ApiMediationServiceConfig config) throws ServiceDefinitionException {
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
            throw new ServiceDefinitionException(message, e);
        }

        result.setInstanceId(String.format("%s:%s:%s", hostname, config.getServiceId(), port));
        result.setAppname(config.getServiceId());
        result.setHostName(hostname);
        result.setIpAddress(config.getServiceIpAddress());
        result.setInstanceEnabledOnit(true);
        result.setSecureVirtualHostName(config.getServiceId());
        result.setVirtualHostName(config.getServiceId());
        result.setStatusPageUrl(config.getBaseUrl() + config.getStatusPageRelativeUrl());

        if ((config.getHomePageRelativeUrl() != null) && !config.getHomePageRelativeUrl().isEmpty()) {
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

        try {
            result.setMetadataMap(createMetadata(config));
        } catch (MetadataValidationException e) {
            throw new ServiceDefinitionException("Service configuration failed: ", e);
        }

        return result;
    }


    private Map<String, String> createMetadata(ApiMediationServiceConfig config) throws MetadataValidationException {
        Map<String, String> metadata = new HashMap<>();

        // fill routing metadata
        for (Route route : config.getRoutes()) {
            String gatewayUrl = UrlUtils.trimSlashes(route.getGatewayUrl());
            String serviceUrl = route.getServiceUrl();
            String key = gatewayUrl.replace("/", "-");
            metadata.put(String.format("%s.%s.%s", ROUTES, key, ROUTES_GATEWAY_URL), gatewayUrl);
            metadata.put(String.format("%s.%s.%s", ROUTES, key, ROUTES_SERVICE_URL), serviceUrl);
        }

        // fill tile metadata
        if (config.getCatalog() != null) {
            Catalog.Tile tile = config.getCatalog().getTile();
            if (tile != null) {
                metadata.put(CATALOG_ID, tile.getId());
                metadata.put(CATALOG_VERSION, tile.getVersion());
                metadata.put(CATALOG_TITLE, tile.getTitle());
                metadata.put(CATALOG_DESCRIPTION, tile.getDescription());
            }
        }

        // fill service metadata
        metadata.put(SERVICE_TITLE, config.getTitle());
        metadata.put(SERVICE_DESCRIPTION, config.getDescription());

        // fill api-doc info
        for (ApiInfo apiInfo : config.getApiInfo()) {
            try {
                metadata.putAll(EurekaMetadataParser.generateMetadata(config.getServiceId(), apiInfo));
            } catch (MalformedURLException e) {
                throw new MetadataValidationException("Metadata creation failed for apiInfo: " + apiInfo.getApiId(), e);
            }
        }

        return metadata;
    }
}
