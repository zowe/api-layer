/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.util;

import com.netflix.appinfo.EurekaInstanceConfig;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.ApimlEurekaInstanceConfig;
import org.zowe.apiml.eurekaservice.client.config.Catalog;
import org.zowe.apiml.eurekaservice.client.config.Route;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.util.UrlUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EurekaInstanceConfigCreator {

    public EurekaInstanceConfig createEurekaInstanceConfig(ApiMediationServiceConfig config) throws ServiceDefinitionException {
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
        result.setAppGroupName(config.getServiceId());
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
                throw new ServiceDefinitionException(String.format("'%s' is not valid protocol for baseUrl property", protocol));
        }

        try {
            result.setMetadataMap(createMetadata(config));
        } catch (MetadataValidationException | IllegalArgumentException e) {
            throw new ServiceDefinitionException("Service configuration failed to create service metadata: ", e);
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
            metadata.put(String.format("%s.%s.%s", EurekaMetadataDefinition.ROUTES, key, EurekaMetadataDefinition.ROUTES_GATEWAY_URL), gatewayUrl);
            metadata.put(String.format("%s.%s.%s", EurekaMetadataDefinition.ROUTES, key, EurekaMetadataDefinition.ROUTES_SERVICE_URL), serviceUrl);
        }

        // fill tile metadata
        if (config.getCatalog() != null) {
            Catalog.Tile tile = config.getCatalog().getTile();
            if (tile != null) {
                metadata.put(EurekaMetadataDefinition.CATALOG_ID, tile.getId());
                metadata.put(EurekaMetadataDefinition.CATALOG_VERSION, tile.getVersion());
                metadata.put(EurekaMetadataDefinition.CATALOG_TITLE, tile.getTitle());
                metadata.put(EurekaMetadataDefinition.CATALOG_DESCRIPTION, tile.getDescription());
            }
        }

        // fill service metadata
        metadata.put(EurekaMetadataDefinition.SERVICE_TITLE, config.getTitle());
        metadata.put(EurekaMetadataDefinition.SERVICE_DESCRIPTION, config.getDescription());

        metadata.putAll(flattenMetadata(config.getCustomMetadata()));

        // fill api-doc info
        for (ApiInfo apiInfo : config.getApiInfo()) {
            metadata.putAll(EurekaMetadataParser.generateMetadata(config.getServiceId(), apiInfo));
        }

        return metadata;
    }

    public Map<String, String> flattenMetadata(Map<java.lang.String, Object> configurationMetadata) {
        return flattenMap(null, configurationMetadata);
    }

    private Map<String, String> flattenMap(String rootKey, Map<String, Object> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : collection.entrySet()) {

            if (entry.getValue() == null) {
                result.put( mergeKey(rootKey, entry.getKey()), "");
                continue;
            }


            if (entry.getValue() instanceof Map) {
                result.putAll(flattenMap(mergeKey(rootKey, entry.getKey()), (Map<String, Object>)entry.getValue()));
                continue;
            }

            if (entry.getValue() instanceof String ) {
                result.put(mergeKey(rootKey, entry.getKey()), entry.getValue().toString());
                continue;
            }

            if (entry.getValue() instanceof List) {
                throw new IllegalArgumentException("List parsing is not supported");
            }
            if (entry.getValue().getClass().isArray()) {
                throw new IllegalArgumentException("Array parsing is not supported");
            }
        }

        return result;
    }


    private String mergeKey(String rootKey, String newKey) {
        return rootKey != null ? rootKey + "." + newKey : newKey;
    }
}
