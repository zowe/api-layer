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
import org.zowe.apiml.eurekaservice.client.config.*;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.util.MapUtils;
import org.zowe.apiml.util.UrlUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

public class EurekaInstanceConfigCreator {

    public EurekaInstanceConfig createEurekaInstanceConfig(ApiMediationServiceConfig config) throws ServiceDefinitionException, MalformedURLException {
        // todo validator
        EurekaInstanceConfigValidator eurekaInstanceConfigValidator = new EurekaInstanceConfigValidator();
        eurekaInstanceConfigValidator.validateConfiguration(config);
        ApimlEurekaInstanceConfig result = new ApimlEurekaInstanceConfig();

        URL baseUrl = new URL(config.getBaseUrl());
        String hostname = baseUrl.getHost();
        int port = baseUrl.getPort();

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
                break;
        }

        try {
            result.setMetadataMap(createMetadata(config));
        } catch (IllegalArgumentException e) {
            throw new ServiceDefinitionException("Service configuration failed to create service metadata: ", e);
        }

        return result;
    }

    private Map<String, String> createMetadata(ApiMediationServiceConfig config) {
        Map<String, String> metadata = new HashMap<>();

        // fill authentication metadata
        Authentication authentication = config.getAuthentication();
        if (authentication != null) {
            metadata.put(AUTHENTICATION_SCHEME, authentication.getScheme());
            metadata.put(AUTHENTICATION_APPLID, authentication.getApplid());
        }

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

        // fill custom metadata
        metadata.putAll(flattenMetadata(config.getCustomMetadata()));

        // fill api-doc info
        for (ApiInfo apiInfo : config.getApiInfo()) {
            metadata.putAll(EurekaMetadataParser.generateMetadata(config.getServiceId(), apiInfo));
        }

        return metadata;
    }

    public Map<String, String> flattenMetadata(Map<String, Object> configurationMetadata) {
        return MapUtils.flattenMap(null, configurationMetadata);
    }

}
