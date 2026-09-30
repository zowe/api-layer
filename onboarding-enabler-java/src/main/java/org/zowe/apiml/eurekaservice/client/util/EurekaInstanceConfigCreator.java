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

import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.Authentication;
import org.zowe.apiml.eurekaservice.client.config.Catalog;
import org.zowe.apiml.eurekaservice.client.config.Route;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.util.MapUtils;
import org.zowe.apiml.util.UrlUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

/**
 * Builds this service's registration - a registry {@link ServiceInstance} - out of an {@link ApiMediationServiceConfig}.
 * <p>
 * The class name and the derivation are kept from the Eureka-based enabler: it used to assemble a Netflix
 * {@code EurekaInstanceConfig} that Netflix's own {@code EurekaConfigBasedInstanceInfoProvider} then turned into an
 * {@code InstanceInfo}. The intermediate Netflix types are gone with the client, so this class now produces the
 * registration directly, with the same field-for-field result - which the registry serialises onto the unchanged
 * wire contract. The lease cadence is the one Netflix's {@code EurekaInstanceConfigBean} defaulted to and the
 * enabler never overrode: renewed every 30 seconds, expiring after 90.
 */
public class EurekaInstanceConfigCreator {

    private static final int DEFAULT_LEASE_RENEWAL_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_LEASE_EXPIRATION_DURATION_SECONDS = 90;

    public ServiceInstance createServiceInstance(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        EurekaInstanceConfigValidator eurekaInstanceConfigValidator = new EurekaInstanceConfigValidator();
        eurekaInstanceConfigValidator.validate(config);

        String hostname;
        int port;
        URL baseUrl;

        try {
            baseUrl = new URL(config.getBaseUrl());
            hostname = baseUrl.getHost();
            port = baseUrl.getPort();
        } catch (MalformedURLException e) {
            String message = String.format("baseUrl: [%s] is not valid URL", config.getBaseUrl());
            throw new MetadataValidationException(message, e);
        }
        if (config.isPreferIpAddress()) {
            hostname = config.getServiceIpAddress();
            config.setBaseUrl(baseUrl.getProtocol() + "://" + hostname + ":" + port);
        }

        long now = System.currentTimeMillis();

        ServiceInstance.Builder result = ServiceInstance.builder()
            .instanceId(String.format("%s:%s:%s", hostname, config.getServiceId(), port))
            .appName(config.getServiceId())
            .appGroupName(config.getServiceId())
            .hostName(hostname)
            .ipAddr(config.getServiceIpAddress())
            // The enabler registers immediately as UP, which is what instanceEnabledOnit meant on the Eureka path.
            .status(InstanceStatus.UP)
            .vipAddress(config.getServiceId())
            .secureVipAddress(config.getServiceId())
            .statusPageUrl(config.getBaseUrl() + config.getStatusPageRelativeUrl())
            .lease(Lease.renewable(
                DEFAULT_LEASE_RENEWAL_INTERVAL_SECONDS,
                DEFAULT_LEASE_EXPIRATION_DURATION_SECONDS,
                now))
            .lastUpdatedTimestamp(now)
            .lastDirtyTimestamp(now);

        if ((config.getHomePageRelativeUrl() != null) && !config.getHomePageRelativeUrl().isEmpty()) {
            result.homePageUrl(config.getBaseUrl() + config.getHomePageRelativeUrl());
        }

        String protocol = baseUrl.getProtocol();


        switch (protocol) {
            case "http":
                result.port(new PortInfo(port, true));
                result.healthCheckUrl(config.getBaseUrl() + config.getHealthCheckRelativeUrl());
                break;
            case "https":
                result.securePort(new PortInfo(port, true));
                result.secureHealthCheckUrl(config.getBaseUrl() + config.getHealthCheckRelativeUrl());
                break;
            default:
                throw new MetadataValidationException(String.format("'%s' is not valid protocol for baseUrl property", protocol));
        }

        try {
            result.metadata(createMetadata(config));
        } catch (MetadataValidationException | IllegalArgumentException e) {
            throw new ServiceDefinitionException("Service configuration failed to create service metadata: ", e);
        }

        return result.build();
    }

    private Map<String, String> createMetadata(ApiMediationServiceConfig config) {
        Map<String, String> metadata = new HashMap<>();

        // fill authentication metadata
        Authentication authentication = config.getAuthentication();
        if (authentication != null) {
            putMetadata(metadata, AUTHENTICATION_SCHEME, authentication.getScheme());
            putMetadata(metadata, AUTHENTICATION_APPLID, authentication.getApplid());
            putMetadata(metadata, AUTHENTICATION_HEADERS, authentication.getHeaders());
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
                putMetadata(metadata, CATALOG_ID, tile.getId());
                putMetadata(metadata, CATALOG_VERSION, tile.getVersion());
                putMetadata(metadata, CATALOG_TITLE, tile.getTitle());
                putMetadata(metadata, CATALOG_DESCRIPTION, tile.getDescription());
            }
        }

        // fill service metadata
        putMetadata(metadata, SERVICE_TITLE, config.getTitle());
        putMetadata(metadata, SERVICE_DESCRIPTION, config.getDescription());

        // fill custom metadata
        metadata.putAll(flattenMetadata(config.getCustomMetadata()));

        // fill api-doc info
        for (ApiInfo apiInfo : config.getApiInfo()) {
            metadata.putAll(EurekaMetadataParser.generateMetadata(config.getServiceId(), apiInfo));
        }

        return metadata;
    }

    /**
     * A key with a null value is dropped rather than carried: the Netflix client the enabler used to build its
     * registration through did the same, so a key that was never set and one that was set to null stayed
     * indistinguishable on the wire.
     */
    private static void putMetadata(Map<String, String> metadata, String key, String value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    public Map<String, String> flattenMetadata(Map<String, Object> configurationMetadata) {
        return MapUtils.flattenMap(null, configurationMetadata);
    }

}
