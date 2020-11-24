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

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.Map;

import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

@Configuration
public class GatewayConfig {


    private ConfigurableEnvironment env;

    public GatewayConfig(ConfigurableEnvironment env) {
        this.env = env;
    }

    @Bean
    public GatewayConfigProperties getGatewayConfigProperties(@Value("${apiml.gateway.hostname}") String hostname,
            @Value("${apiml.service.port}") String port, @Value("${apiml.service.scheme}") String scheme) {
        return GatewayConfigProperties.builder().scheme(scheme).hostname(hostname + ":" + port).build();
    }

    @Bean
    @Autowired
    public SimpleHostRoutingFilter simpleHostRoutingFilter2(ProxyRequestHelper helper, ZuulProperties zuulProperties,
            @Qualifier("secureHttpClientWithoutKeystore") CloseableHttpClient secureHttpClientWithoutKeystore) {
        return new SimpleHostRoutingFilter(helper, zuulProperties, secureHttpClientWithoutKeystore);
    }

    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
                                                             ManagementMetadataProvider managementMetadataProvider) {
        String hostname = getProperty("eureka.instance.hostname");
        boolean preferIpAddress = Boolean
            .parseBoolean(getProperty("eureka.instance.prefer-ip-address"));
        String ipAddress = getProperty("eureka.instance.ip-address");
        boolean isSecurePortEnabled = Boolean
            .parseBoolean(getProperty("eureka.instance.secure-port-enabled"));

        String serverContextPath = env.getProperty("server.servlet.context-path", "/");
        int serverPort = Integer
            .parseInt(env.getProperty("eureka.instance.port", env.getProperty("port", "8080")));

        Integer managementPort = env.getProperty("management.server.port", Integer.class);

        String managementContextPath = env
            .getProperty("management.server.servlet.context-path");

        EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);

        instance.setNonSecurePort(serverPort);
        instance.setInstanceId(getDefaultInstanceId(env));
        instance.setPreferIpAddress(preferIpAddress);
        instance.setSecurePortEnabled(isSecurePortEnabled);
        if (StringUtils.hasText(ipAddress)) {
            instance.setIpAddress(ipAddress);
        }

        if (isSecurePortEnabled) {
            instance.setSecurePort(serverPort);
        }

        if (StringUtils.hasText(hostname)) {
            instance.setHostname(hostname);
        }
        String statusPageUrlPath = getProperty("eureka.instance.status-page-url-path");
        String healthCheckUrlPath = getProperty("eureka.instance.health-check-url-path");

        if (StringUtils.hasText(statusPageUrlPath)) {
            instance.setStatusPageUrlPath(statusPageUrlPath);
        }
        if (StringUtils.hasText(healthCheckUrlPath)) {
            instance.setHealthCheckUrlPath(healthCheckUrlPath);
        }

        ManagementMetadata metadata = managementMetadataProvider.get(instance, serverPort,
            serverContextPath, managementContextPath, managementPort);

        if (metadata != null) {
            instance.setStatusPageUrl(metadata.getStatusPageUrl());
            instance.setHealthCheckUrl(metadata.getHealthCheckUrl());
            if (instance.isSecurePortEnabled()) {
                instance.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
            }
            Map<String, String> metadataMap = instance.getMetadataMap();
            metadataMap.computeIfAbsent("management.port",
                k -> String.valueOf(metadata.getManagementPort()));
        }
        return instance;
    }

    private String getProperty(String property) {
        return this.env.containsProperty(property) ? this.env.getProperty(property) : "";
    }
}
