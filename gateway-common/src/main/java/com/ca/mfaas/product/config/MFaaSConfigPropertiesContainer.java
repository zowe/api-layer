/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

/**
 * Deprecated: See https://github.com/zowe/api-layer/wiki/Externalizing-Java-Code-Configuration-in-APIML
 */
@Data
@Component
@ConfigurationProperties(prefix = "mfaas", ignoreUnknownFields = false)
@SuppressWarnings("WeakerAccess")
@Deprecated
public class MFaaSConfigPropertiesContainer {

    // Service discovery properties
    @NotBlank
    private DiscoveryProperties discovery;

    // Service instance properties
    @NotBlank
    private ServiceProperties service;

    // Values used to create or add this service to a UI dashboard tile
    @NotBlank
    private CatalogUiTileProperties catalogUiTile;

    // Service instance properties, merge with service
    @NotBlank
    private ServerProperties server;

    // Security properties
    private SecurityProperties security;

    // Gateway properties
    private GatewayProperties gateway;

    // Internal properties used for retrieving service info
    private ServiceRegistryProperties serviceRegistry;

    @Data
    public static class DiscoveryProperties {

        // TRUE if this service is discoverable
        private Boolean enabled;

        // Unique identifier for all instances of this service, must be unique to this service (not instance)
        // Defaults to the $spring.application.name
        @NotBlank
        private String serviceId;

        // Comma separated URLs of the location of all discovery services
        @NotBlank
        private String locations;

        // region in which the discovery services are running
        @NotBlank
        private String region;

        // TRUE if this service should register with the discovery services
        private Boolean registerWithEureka;

        // TRUE if this service should request discovery service instance info periodically
        private Boolean fetchRegistry;

        // TRUE if this service uses HTTP communication
        private Boolean nonSecurePortEnabled;

        // TRUE if this service uses HTTPS communication
        private Boolean securePortEnabled;

        // User id for accessing the discovery service
        private String eurekaUserName;

        // Password for accessing the discovery service
        private String eurekaUserPassword;

        // Info, Health and Homepage locations
        private DiscoveryEndpoints endpoints;

        // Information describing this service
        private ServiceInfo info;
    }

    @Data
    public static class ServiceProperties {

        // Hostname for this service
        @NotBlank
        private String hostname;

        // Ip address for this service
        @NotBlank
        private String ipAddress;
    }

    @Data
    public static class CatalogUiTileProperties {
        // Unique identifier for the product family or UI parent container for this service
        @NotBlank
        private String id;

        // UI tile title
        @NotBlank
        private String title;

        // UI tile description
        @NotBlank
        private String description;

        // UI tile version (semantic versioning used)
        @NotBlank
        private String version;
    }

    @Data
    public static class ServerProperties {

        // HTTP/HTTPS scheme for this service
        @NotBlank
        private String scheme;

        // TRUE if discovery should use given ip addresses over dns hostnames
        private Boolean preferIpAddress;

        // Server ip address
        @NotBlank
        private String ipAddress;

        // Port on which this service is running
        @NotBlank
        private String port;

        // Secure port on which this service is running
        private String securePort;

        // Optional context path for this service
        private String contextPath;
    }

    @Data
    public static class SecurityProperties {
        // TRUE if ESM security is enabled for this service
        private Boolean esmEnabled;

        // TRUE if SSL is enabled for this service
        @NotBlank
        private Boolean sslEnabled;

        // SSL/TLS protocol for this service
        @NotBlank
        private String protocol;

        // SSL/TLS ciphers for this service
        @NotBlank
        private String ciphers;

        // Location of the Java truststore
        private String trustStore;

        // Truststore type (JKS, PKCS12...)
        private String trustStoreType;

        // Truststore password
        private String trustStorePassword;

        // Key alias
        private String keyAlias;

        // Key password
        private String keyPassword;

        // Location of the Java keystore
        private String keyStore;

        // Keystore type (JKS, PKCS12...)
        private String keyStoreType;

        // Keystore password
        private String keyStorePassword;
    }

    @Data
    public static class GatewayProperties {
        // The homepage of the gateway
        @NotBlank
        private String gatewayHomePageUrl;

        // Timeout duration for all requests going via the gateway
        // default = 30,000 (see histrixTimeoutInMillis if changing this value)
        private Integer timeoutInMillis;

        // optional value to add debug information about the gateway requests
        private Boolean debugHeaders;

        // Hystrix timeout should be not less then ribbon.ConnectTimeout + ribbon.ReadTimeout) ...
        // ... * (ribbon.MaxAutoRetries(default = 0) + 1) * (ribbon.MaxAutoRetriesNextServer(default = 1) + 1)
        // default = 240,000
        private Integer histrixTimeoutInMillis;
    }

    /**
     * Internal class used for changing timing values for discovery services and cache updates
     * Should not normally need to be changed by a client service
     * Change with caution or weirdness may occur !
     */
    @Data
    public static class ServiceRegistryProperties {
        // amount of time to wait before retrying to fetch a service from discovery
        private Integer serviceFetchDelayInMillis;

        // check for any container instances which have changed within this period
        private Integer cacheRefreshUpdateThresholdInMillis;

        // wait this amount of time before populating the cache
        private Integer cacheRefreshInitialDelayInMillis;

        // if the cache cannot read discovery data, then wait this amount of time before retrying
        private Integer cacheRefreshRetryDelayInMillis;
    }

    @Data
    public static class DiscoveryEndpoints {
        // Home page of the client
        private String statusPage;

        // health url of the client (defaults to actuator health endpoint)
        private String healthPage;

        // status url of the client (defaults to actuator status endpoint)
        private String homepage;
    }

    @Data
    public static class ServiceInfo {
        // Descriptive title of the service, used to populate the UI
        private String serviceTitle;

        // Detailed description of the service, used to populate the UI
        private String description;

        // TRUE if the service has API documentation it want's to expose to the gateway/catalog
        private Boolean enableApiDoc;

        // The location of a manually defined swagger endpoints, note disables automatic discovery of endpoints and swagger generation
        private String swaggerLocation;
    }
}
