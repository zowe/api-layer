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
import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsFactory;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.PortType;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ApiMediationClientImpl implements ApiMediationClient {
    private EurekaClient eurekaClient;
    private ApplicationInfoManager applicationInfoManager;

    @Override
    public void register(ApiMediationServiceConfig config) {
        ApplicationInfoManager infoManager = initializeApplicationInfoManager(config);
        EurekaClientConfiguration clientConfiguration = new EurekaClientConfiguration(config);
        initializeEurekaClient(infoManager, clientConfiguration, config);
    }

    @Override
    public void unregister() {
        if (eurekaClient != null) {
            eurekaClient.shutdown();
        }
    }

    private synchronized EurekaClient initializeEurekaClient(
        ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig, ApiMediationServiceConfig config) {

        Ssl sslConfig = config.getSsl();
        URL baseUrl;
        try {
            baseUrl = new URL(config.getBaseUrl());
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("baseUrl: [%s] is not valid URL", config.getBaseUrl()), e);
        }

        HttpsConfig httpsConfig = HttpsConfig.builder()
            .protocol("TLSv1.2")
            .keyAlias(sslConfig.getKeyAlias())
            .keyStore(sslConfig.getKeyStore())
            .keyPassword(sslConfig.getKeyPassword())
            .keyStorePassword(sslConfig.getKeyStorePassword())
            .keyStoreType(sslConfig.getKeyStoreType())
            .trustStore(sslConfig.getTrustStore())
            .trustStoreType(sslConfig.getTrustStoreType())
            .trustStorePassword(sslConfig.getTrustStorePassword())
            .build();
        HttpsFactory factory = new HttpsFactory(httpsConfig);
        SSLContext secureSslContext = factory.createSslContext();

        DiscoveryClient.DiscoveryClientOptionalArgs args = new DiscoveryClient.DiscoveryClientOptionalArgs();
        args.setSSLContext(secureSslContext);
        if (this.eurekaClient == null) {
            eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig, args);
        }
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        return eurekaClient;
    }

    private synchronized ApplicationInfoManager initializeApplicationInfoManager(ApiMediationServiceConfig config) {
        if (this.applicationInfoManager == null) {
            EurekaInstanceConfig eurekaInstanceConfig = new MyDataCenterInstanceConfig();
            InstanceInfo instanceInformation = createInstanceInfo(eurekaInstanceConfig, config);
            applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, instanceInformation);
        }
        return applicationInfoManager;
    }

    private InstanceInfo createInstanceInfo(EurekaInstanceConfig eurekaInstanceConfig, ApiMediationServiceConfig config) {
        InstanceInfo.Builder builder = new InstanceInfo.Builder(
            new EurekaConfigBasedInstanceInfoProvider(eurekaInstanceConfig).get());
        String hostname;
        int port;
        URL baseUrl;
        try {
            baseUrl = new URL(config.getBaseUrl());
            hostname = baseUrl.getHost();
            port = baseUrl.getPort();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("baseUrl: [%s] is not valid URL", config.getBaseUrl()), e);
        }

        builder.setInstanceId(String.format("%s:%s:%d", hostname, config.getServiceId(), port))
            .setAppName(config.getServiceId())
            .setVIPAddress(config.getServiceId())
            .setHostName(hostname)
            .setAppGroupName(null)
            .setMetadata(createMetadata(config))
            .setStatusPageUrl(null, config.getBaseUrl() + config.getStatusPageRelativeUrl());

        if (!StringUtils.isNullOrEmpty(config.getHomePageRelativeUrl())) {
            builder.setHomePageUrl(null, config.getBaseUrl() + config.getHomePageRelativeUrl());
        } else {
            builder.setHomePageUrl(null, "");
        }

        String protocol = baseUrl.getProtocol();
        switch (protocol) {
            case "http":
                builder.enablePort(PortType.SECURE, false).enablePort(PortType.UNSECURE, true)
                    .setPort(baseUrl.getPort());
                builder.setHealthCheckUrls(null, config.getBaseUrl() + config.getHealthCheckRelativeUrl(), null);
                break;
            case "https":
                builder.enablePort(PortType.SECURE, true).enablePort(PortType.UNSECURE, false)
                    .setSecurePort(baseUrl.getPort());
                builder.setHealthCheckUrls(null, null, config.getBaseUrl() + config.getHealthCheckRelativeUrl());
                break;
            default:
                throw new RuntimeException(new MalformedURLException("Invalid protocol for baseUrl property"));
        }

        InstanceInfo instanceInformation = builder.build();

        return instanceInformation;
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

        return metadata;
    }
}
