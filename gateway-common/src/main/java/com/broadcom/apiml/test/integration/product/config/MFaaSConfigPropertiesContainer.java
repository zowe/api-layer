/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

/**
 * Deprecated: See https://github.com/zowe/api-layer/wiki/Externalizing-Java-Code-Configuration-in-APIML
 */
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

    public MFaaSConfigPropertiesContainer() {
    }

    public @NotBlank DiscoveryProperties getDiscovery() {
        return this.discovery;
    }

    public void setDiscovery(@NotBlank DiscoveryProperties discovery) {
        this.discovery = discovery;
    }

    public @NotBlank ServiceProperties getService() {
        return this.service;
    }

    public void setService(@NotBlank ServiceProperties service) {
        this.service = service;
    }

    public @NotBlank CatalogUiTileProperties getCatalogUiTile() {
        return this.catalogUiTile;
    }

    public void setCatalogUiTile(@NotBlank CatalogUiTileProperties catalogUiTile) {
        this.catalogUiTile = catalogUiTile;
    }

    public @NotBlank ServerProperties getServer() {
        return this.server;
    }

    public void setServer(@NotBlank ServerProperties server) {
        this.server = server;
    }

    public SecurityProperties getSecurity() {
        return this.security;
    }

    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    public GatewayProperties getGateway() {
        return this.gateway;
    }

    public void setGateway(GatewayProperties gateway) {
        this.gateway = gateway;
    }

    public ServiceRegistryProperties getServiceRegistry() {
        return this.serviceRegistry;
    }

    public void setServiceRegistry(ServiceRegistryProperties serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MFaaSConfigPropertiesContainer)) return false;
        final MFaaSConfigPropertiesContainer other = (MFaaSConfigPropertiesContainer) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$discovery = this.getDiscovery();
        final Object other$discovery = other.getDiscovery();
        if (this$discovery == null ? other$discovery != null : !this$discovery.equals(other$discovery)) return false;
        final Object this$service = this.getService();
        final Object other$service = other.getService();
        if (this$service == null ? other$service != null : !this$service.equals(other$service)) return false;
        final Object this$catalogUiTile = this.getCatalogUiTile();
        final Object other$catalogUiTile = other.getCatalogUiTile();
        if (this$catalogUiTile == null ? other$catalogUiTile != null : !this$catalogUiTile.equals(other$catalogUiTile))
            return false;
        final Object this$server = this.getServer();
        final Object other$server = other.getServer();
        if (this$server == null ? other$server != null : !this$server.equals(other$server)) return false;
        final Object this$security = this.getSecurity();
        final Object other$security = other.getSecurity();
        if (this$security == null ? other$security != null : !this$security.equals(other$security)) return false;
        final Object this$gateway = this.getGateway();
        final Object other$gateway = other.getGateway();
        if (this$gateway == null ? other$gateway != null : !this$gateway.equals(other$gateway)) return false;
        final Object this$serviceRegistry = this.getServiceRegistry();
        final Object other$serviceRegistry = other.getServiceRegistry();
        if (this$serviceRegistry == null ? other$serviceRegistry != null : !this$serviceRegistry.equals(other$serviceRegistry))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MFaaSConfigPropertiesContainer;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $discovery = this.getDiscovery();
        result = result * PRIME + ($discovery == null ? 43 : $discovery.hashCode());
        final Object $service = this.getService();
        result = result * PRIME + ($service == null ? 43 : $service.hashCode());
        final Object $catalogUiTile = this.getCatalogUiTile();
        result = result * PRIME + ($catalogUiTile == null ? 43 : $catalogUiTile.hashCode());
        final Object $server = this.getServer();
        result = result * PRIME + ($server == null ? 43 : $server.hashCode());
        final Object $security = this.getSecurity();
        result = result * PRIME + ($security == null ? 43 : $security.hashCode());
        final Object $gateway = this.getGateway();
        result = result * PRIME + ($gateway == null ? 43 : $gateway.hashCode());
        final Object $serviceRegistry = this.getServiceRegistry();
        result = result * PRIME + ($serviceRegistry == null ? 43 : $serviceRegistry.hashCode());
        return result;
    }

    public String toString() {
        return "MFaaSConfigPropertiesContainer(discovery=" + this.getDiscovery() + ", service=" + this.getService() + ", catalogUiTile=" + this.getCatalogUiTile() + ", server=" + this.getServer() + ", security=" + this.getSecurity() + ", gateway=" + this.getGateway() + ", serviceRegistry=" + this.getServiceRegistry() + ")";
    }

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

        public DiscoveryProperties() {
        }

        public Boolean getEnabled() {
            return this.enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public @NotBlank String getServiceId() {
            return this.serviceId;
        }

        public void setServiceId(@NotBlank String serviceId) {
            this.serviceId = serviceId;
        }

        public @NotBlank String getLocations() {
            return this.locations;
        }

        public void setLocations(@NotBlank String locations) {
            this.locations = locations;
        }

        public @NotBlank String getRegion() {
            return this.region;
        }

        public void setRegion(@NotBlank String region) {
            this.region = region;
        }

        public Boolean getRegisterWithEureka() {
            return this.registerWithEureka;
        }

        public void setRegisterWithEureka(Boolean registerWithEureka) {
            this.registerWithEureka = registerWithEureka;
        }

        public Boolean getFetchRegistry() {
            return this.fetchRegistry;
        }

        public void setFetchRegistry(Boolean fetchRegistry) {
            this.fetchRegistry = fetchRegistry;
        }

        public Boolean getNonSecurePortEnabled() {
            return this.nonSecurePortEnabled;
        }

        public void setNonSecurePortEnabled(Boolean nonSecurePortEnabled) {
            this.nonSecurePortEnabled = nonSecurePortEnabled;
        }

        public Boolean getSecurePortEnabled() {
            return this.securePortEnabled;
        }

        public void setSecurePortEnabled(Boolean securePortEnabled) {
            this.securePortEnabled = securePortEnabled;
        }

        public String getEurekaUserName() {
            return this.eurekaUserName;
        }

        public void setEurekaUserName(String eurekaUserName) {
            this.eurekaUserName = eurekaUserName;
        }

        public String getEurekaUserPassword() {
            return this.eurekaUserPassword;
        }

        public void setEurekaUserPassword(String eurekaUserPassword) {
            this.eurekaUserPassword = eurekaUserPassword;
        }

        public DiscoveryEndpoints getEndpoints() {
            return this.endpoints;
        }

        public void setEndpoints(DiscoveryEndpoints endpoints) {
            this.endpoints = endpoints;
        }

        public ServiceInfo getInfo() {
            return this.info;
        }

        public void setInfo(ServiceInfo info) {
            this.info = info;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof DiscoveryProperties))
                return false;
            final DiscoveryProperties other = (DiscoveryProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$enabled = this.getEnabled();
            final Object other$enabled = other.getEnabled();
            if (this$enabled == null ? other$enabled != null : !this$enabled.equals(other$enabled)) return false;
            final Object this$serviceId = this.getServiceId();
            final Object other$serviceId = other.getServiceId();
            if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId))
                return false;
            final Object this$locations = this.getLocations();
            final Object other$locations = other.getLocations();
            if (this$locations == null ? other$locations != null : !this$locations.equals(other$locations))
                return false;
            final Object this$region = this.getRegion();
            final Object other$region = other.getRegion();
            if (this$region == null ? other$region != null : !this$region.equals(other$region)) return false;
            final Object this$registerWithEureka = this.getRegisterWithEureka();
            final Object other$registerWithEureka = other.getRegisterWithEureka();
            if (this$registerWithEureka == null ? other$registerWithEureka != null : !this$registerWithEureka.equals(other$registerWithEureka))
                return false;
            final Object this$fetchRegistry = this.getFetchRegistry();
            final Object other$fetchRegistry = other.getFetchRegistry();
            if (this$fetchRegistry == null ? other$fetchRegistry != null : !this$fetchRegistry.equals(other$fetchRegistry))
                return false;
            final Object this$nonSecurePortEnabled = this.getNonSecurePortEnabled();
            final Object other$nonSecurePortEnabled = other.getNonSecurePortEnabled();
            if (this$nonSecurePortEnabled == null ? other$nonSecurePortEnabled != null : !this$nonSecurePortEnabled.equals(other$nonSecurePortEnabled))
                return false;
            final Object this$securePortEnabled = this.getSecurePortEnabled();
            final Object other$securePortEnabled = other.getSecurePortEnabled();
            if (this$securePortEnabled == null ? other$securePortEnabled != null : !this$securePortEnabled.equals(other$securePortEnabled))
                return false;
            final Object this$eurekaUserName = this.getEurekaUserName();
            final Object other$eurekaUserName = other.getEurekaUserName();
            if (this$eurekaUserName == null ? other$eurekaUserName != null : !this$eurekaUserName.equals(other$eurekaUserName))
                return false;
            final Object this$eurekaUserPassword = this.getEurekaUserPassword();
            final Object other$eurekaUserPassword = other.getEurekaUserPassword();
            if (this$eurekaUserPassword == null ? other$eurekaUserPassword != null : !this$eurekaUserPassword.equals(other$eurekaUserPassword))
                return false;
            final Object this$endpoints = this.getEndpoints();
            final Object other$endpoints = other.getEndpoints();
            if (this$endpoints == null ? other$endpoints != null : !this$endpoints.equals(other$endpoints))
                return false;
            final Object this$info = this.getInfo();
            final Object other$info = other.getInfo();
            if (this$info == null ? other$info != null : !this$info.equals(other$info)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof DiscoveryProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $enabled = this.getEnabled();
            result = result * PRIME + ($enabled == null ? 43 : $enabled.hashCode());
            final Object $serviceId = this.getServiceId();
            result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
            final Object $locations = this.getLocations();
            result = result * PRIME + ($locations == null ? 43 : $locations.hashCode());
            final Object $region = this.getRegion();
            result = result * PRIME + ($region == null ? 43 : $region.hashCode());
            final Object $registerWithEureka = this.getRegisterWithEureka();
            result = result * PRIME + ($registerWithEureka == null ? 43 : $registerWithEureka.hashCode());
            final Object $fetchRegistry = this.getFetchRegistry();
            result = result * PRIME + ($fetchRegistry == null ? 43 : $fetchRegistry.hashCode());
            final Object $nonSecurePortEnabled = this.getNonSecurePortEnabled();
            result = result * PRIME + ($nonSecurePortEnabled == null ? 43 : $nonSecurePortEnabled.hashCode());
            final Object $securePortEnabled = this.getSecurePortEnabled();
            result = result * PRIME + ($securePortEnabled == null ? 43 : $securePortEnabled.hashCode());
            final Object $eurekaUserName = this.getEurekaUserName();
            result = result * PRIME + ($eurekaUserName == null ? 43 : $eurekaUserName.hashCode());
            final Object $eurekaUserPassword = this.getEurekaUserPassword();
            result = result * PRIME + ($eurekaUserPassword == null ? 43 : $eurekaUserPassword.hashCode());
            final Object $endpoints = this.getEndpoints();
            result = result * PRIME + ($endpoints == null ? 43 : $endpoints.hashCode());
            final Object $info = this.getInfo();
            result = result * PRIME + ($info == null ? 43 : $info.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.DiscoveryProperties(enabled=" + this.getEnabled() + ", serviceId=" + this.getServiceId() + ", locations=" + this.getLocations() + ", region=" + this.getRegion() + ", registerWithEureka=" + this.getRegisterWithEureka() + ", fetchRegistry=" + this.getFetchRegistry() + ", nonSecurePortEnabled=" + this.getNonSecurePortEnabled() + ", securePortEnabled=" + this.getSecurePortEnabled() + ", eurekaUserName=" + this.getEurekaUserName() + ", eurekaUserPassword=" + this.getEurekaUserPassword() + ", endpoints=" + this.getEndpoints() + ", info=" + this.getInfo() + ")";
        }
    }

    public static class ServiceProperties {

        // Hostname for this service
        @NotBlank
        private String hostname;

        // Ip address for this service
        @NotBlank
        private String ipAddress;

        public ServiceProperties() {
        }

        public @NotBlank String getHostname() {
            return this.hostname;
        }

        public void setHostname(@NotBlank String hostname) {
            this.hostname = hostname;
        }

        public @NotBlank String getIpAddress() {
            return this.ipAddress;
        }

        public void setIpAddress(@NotBlank String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof ServiceProperties)) return false;
            final ServiceProperties other = (ServiceProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$hostname = this.getHostname();
            final Object other$hostname = other.getHostname();
            if (this$hostname == null ? other$hostname != null : !this$hostname.equals(other$hostname)) return false;
            final Object this$ipAddress = this.getIpAddress();
            final Object other$ipAddress = other.getIpAddress();
            if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof ServiceProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $hostname = this.getHostname();
            result = result * PRIME + ($hostname == null ? 43 : $hostname.hashCode());
            final Object $ipAddress = this.getIpAddress();
            result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.ServiceProperties(hostname=" + this.getHostname() + ", ipAddress=" + this.getIpAddress() + ")";
        }
    }

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

        public CatalogUiTileProperties() {
        }

        public @NotBlank String getId() {
            return this.id;
        }

        public void setId(@NotBlank String id) {
            this.id = id;
        }

        public @NotBlank String getTitle() {
            return this.title;
        }

        public void setTitle(@NotBlank String title) {
            this.title = title;
        }

        public @NotBlank String getDescription() {
            return this.description;
        }

        public void setDescription(@NotBlank String description) {
            this.description = description;
        }

        public @NotBlank String getVersion() {
            return this.version;
        }

        public void setVersion(@NotBlank String version) {
            this.version = version;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof CatalogUiTileProperties))
                return false;
            final CatalogUiTileProperties other = (CatalogUiTileProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final Object this$title = this.getTitle();
            final Object other$title = other.getTitle();
            if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
            final Object this$description = this.getDescription();
            final Object other$description = other.getDescription();
            if (this$description == null ? other$description != null : !this$description.equals(other$description))
                return false;
            final Object this$version = this.getVersion();
            final Object other$version = other.getVersion();
            if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof CatalogUiTileProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final Object $title = this.getTitle();
            result = result * PRIME + ($title == null ? 43 : $title.hashCode());
            final Object $description = this.getDescription();
            result = result * PRIME + ($description == null ? 43 : $description.hashCode());
            final Object $version = this.getVersion();
            result = result * PRIME + ($version == null ? 43 : $version.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.CatalogUiTileProperties(id=" + this.getId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", version=" + this.getVersion() + ")";
        }
    }

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

        public ServerProperties() {
        }

        public @NotBlank String getScheme() {
            return this.scheme;
        }

        public void setScheme(@NotBlank String scheme) {
            this.scheme = scheme;
        }

        public Boolean getPreferIpAddress() {
            return this.preferIpAddress;
        }

        public void setPreferIpAddress(Boolean preferIpAddress) {
            this.preferIpAddress = preferIpAddress;
        }

        public @NotBlank String getIpAddress() {
            return this.ipAddress;
        }

        public void setIpAddress(@NotBlank String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public @NotBlank String getPort() {
            return this.port;
        }

        public void setPort(@NotBlank String port) {
            this.port = port;
        }

        public String getSecurePort() {
            return this.securePort;
        }

        public void setSecurePort(String securePort) {
            this.securePort = securePort;
        }

        public String getContextPath() {
            return this.contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof ServerProperties)) return false;
            final ServerProperties other = (ServerProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$scheme = this.getScheme();
            final Object other$scheme = other.getScheme();
            if (this$scheme == null ? other$scheme != null : !this$scheme.equals(other$scheme)) return false;
            final Object this$preferIpAddress = this.getPreferIpAddress();
            final Object other$preferIpAddress = other.getPreferIpAddress();
            if (this$preferIpAddress == null ? other$preferIpAddress != null : !this$preferIpAddress.equals(other$preferIpAddress))
                return false;
            final Object this$ipAddress = this.getIpAddress();
            final Object other$ipAddress = other.getIpAddress();
            if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress))
                return false;
            final Object this$port = this.getPort();
            final Object other$port = other.getPort();
            if (this$port == null ? other$port != null : !this$port.equals(other$port)) return false;
            final Object this$securePort = this.getSecurePort();
            final Object other$securePort = other.getSecurePort();
            if (this$securePort == null ? other$securePort != null : !this$securePort.equals(other$securePort))
                return false;
            final Object this$contextPath = this.getContextPath();
            final Object other$contextPath = other.getContextPath();
            if (this$contextPath == null ? other$contextPath != null : !this$contextPath.equals(other$contextPath))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof ServerProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $scheme = this.getScheme();
            result = result * PRIME + ($scheme == null ? 43 : $scheme.hashCode());
            final Object $preferIpAddress = this.getPreferIpAddress();
            result = result * PRIME + ($preferIpAddress == null ? 43 : $preferIpAddress.hashCode());
            final Object $ipAddress = this.getIpAddress();
            result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
            final Object $port = this.getPort();
            result = result * PRIME + ($port == null ? 43 : $port.hashCode());
            final Object $securePort = this.getSecurePort();
            result = result * PRIME + ($securePort == null ? 43 : $securePort.hashCode());
            final Object $contextPath = this.getContextPath();
            result = result * PRIME + ($contextPath == null ? 43 : $contextPath.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.ServerProperties(scheme=" + this.getScheme() + ", preferIpAddress=" + this.getPreferIpAddress() + ", ipAddress=" + this.getIpAddress() + ", port=" + this.getPort() + ", securePort=" + this.getSecurePort() + ", contextPath=" + this.getContextPath() + ")";
        }
    }

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

        public SecurityProperties() {
        }

        public Boolean getEsmEnabled() {
            return this.esmEnabled;
        }

        public void setEsmEnabled(Boolean esmEnabled) {
            this.esmEnabled = esmEnabled;
        }

        public @NotBlank Boolean getSslEnabled() {
            return this.sslEnabled;
        }

        public void setSslEnabled(@NotBlank Boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
        }

        public @NotBlank String getProtocol() {
            return this.protocol;
        }

        public void setProtocol(@NotBlank String protocol) {
            this.protocol = protocol;
        }

        public @NotBlank String getCiphers() {
            return this.ciphers;
        }

        public void setCiphers(@NotBlank String ciphers) {
            this.ciphers = ciphers;
        }

        public String getTrustStore() {
            return this.trustStore;
        }

        public void setTrustStore(String trustStore) {
            this.trustStore = trustStore;
        }

        public String getTrustStoreType() {
            return this.trustStoreType;
        }

        public void setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
        }

        public String getTrustStorePassword() {
            return this.trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public String getKeyAlias() {
            return this.keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getKeyPassword() {
            return this.keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        public String getKeyStore() {
            return this.keyStore;
        }

        public void setKeyStore(String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStoreType() {
            return this.keyStoreType;
        }

        public void setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
        }

        public String getKeyStorePassword() {
            return this.keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof SecurityProperties)) return false;
            final SecurityProperties other = (SecurityProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$esmEnabled = this.getEsmEnabled();
            final Object other$esmEnabled = other.getEsmEnabled();
            if (this$esmEnabled == null ? other$esmEnabled != null : !this$esmEnabled.equals(other$esmEnabled))
                return false;
            final Object this$sslEnabled = this.getSslEnabled();
            final Object other$sslEnabled = other.getSslEnabled();
            if (this$sslEnabled == null ? other$sslEnabled != null : !this$sslEnabled.equals(other$sslEnabled))
                return false;
            final Object this$protocol = this.getProtocol();
            final Object other$protocol = other.getProtocol();
            if (this$protocol == null ? other$protocol != null : !this$protocol.equals(other$protocol)) return false;
            final Object this$ciphers = this.getCiphers();
            final Object other$ciphers = other.getCiphers();
            if (this$ciphers == null ? other$ciphers != null : !this$ciphers.equals(other$ciphers)) return false;
            final Object this$trustStore = this.getTrustStore();
            final Object other$trustStore = other.getTrustStore();
            if (this$trustStore == null ? other$trustStore != null : !this$trustStore.equals(other$trustStore))
                return false;
            final Object this$trustStoreType = this.getTrustStoreType();
            final Object other$trustStoreType = other.getTrustStoreType();
            if (this$trustStoreType == null ? other$trustStoreType != null : !this$trustStoreType.equals(other$trustStoreType))
                return false;
            final Object this$trustStorePassword = this.getTrustStorePassword();
            final Object other$trustStorePassword = other.getTrustStorePassword();
            if (this$trustStorePassword == null ? other$trustStorePassword != null : !this$trustStorePassword.equals(other$trustStorePassword))
                return false;
            final Object this$keyAlias = this.getKeyAlias();
            final Object other$keyAlias = other.getKeyAlias();
            if (this$keyAlias == null ? other$keyAlias != null : !this$keyAlias.equals(other$keyAlias)) return false;
            final Object this$keyPassword = this.getKeyPassword();
            final Object other$keyPassword = other.getKeyPassword();
            if (this$keyPassword == null ? other$keyPassword != null : !this$keyPassword.equals(other$keyPassword))
                return false;
            final Object this$keyStore = this.getKeyStore();
            final Object other$keyStore = other.getKeyStore();
            if (this$keyStore == null ? other$keyStore != null : !this$keyStore.equals(other$keyStore)) return false;
            final Object this$keyStoreType = this.getKeyStoreType();
            final Object other$keyStoreType = other.getKeyStoreType();
            if (this$keyStoreType == null ? other$keyStoreType != null : !this$keyStoreType.equals(other$keyStoreType))
                return false;
            final Object this$keyStorePassword = this.getKeyStorePassword();
            final Object other$keyStorePassword = other.getKeyStorePassword();
            if (this$keyStorePassword == null ? other$keyStorePassword != null : !this$keyStorePassword.equals(other$keyStorePassword))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof SecurityProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $esmEnabled = this.getEsmEnabled();
            result = result * PRIME + ($esmEnabled == null ? 43 : $esmEnabled.hashCode());
            final Object $sslEnabled = this.getSslEnabled();
            result = result * PRIME + ($sslEnabled == null ? 43 : $sslEnabled.hashCode());
            final Object $protocol = this.getProtocol();
            result = result * PRIME + ($protocol == null ? 43 : $protocol.hashCode());
            final Object $ciphers = this.getCiphers();
            result = result * PRIME + ($ciphers == null ? 43 : $ciphers.hashCode());
            final Object $trustStore = this.getTrustStore();
            result = result * PRIME + ($trustStore == null ? 43 : $trustStore.hashCode());
            final Object $trustStoreType = this.getTrustStoreType();
            result = result * PRIME + ($trustStoreType == null ? 43 : $trustStoreType.hashCode());
            final Object $trustStorePassword = this.getTrustStorePassword();
            result = result * PRIME + ($trustStorePassword == null ? 43 : $trustStorePassword.hashCode());
            final Object $keyAlias = this.getKeyAlias();
            result = result * PRIME + ($keyAlias == null ? 43 : $keyAlias.hashCode());
            final Object $keyPassword = this.getKeyPassword();
            result = result * PRIME + ($keyPassword == null ? 43 : $keyPassword.hashCode());
            final Object $keyStore = this.getKeyStore();
            result = result * PRIME + ($keyStore == null ? 43 : $keyStore.hashCode());
            final Object $keyStoreType = this.getKeyStoreType();
            result = result * PRIME + ($keyStoreType == null ? 43 : $keyStoreType.hashCode());
            final Object $keyStorePassword = this.getKeyStorePassword();
            result = result * PRIME + ($keyStorePassword == null ? 43 : $keyStorePassword.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.SecurityProperties(esmEnabled=" + this.getEsmEnabled() + ", sslEnabled=" + this.getSslEnabled() + ", protocol=" + this.getProtocol() + ", ciphers=" + this.getCiphers() + ", trustStore=" + this.getTrustStore() + ", trustStoreType=" + this.getTrustStoreType() + ", trustStorePassword=" + this.getTrustStorePassword() + ", keyAlias=" + this.getKeyAlias() + ", keyPassword=" + this.getKeyPassword() + ", keyStore=" + this.getKeyStore() + ", keyStoreType=" + this.getKeyStoreType() + ", keyStorePassword=" + this.getKeyStorePassword() + ")";
        }
    }

    public static class GatewayProperties {
        // The hostname of the gateway / DVIPA address
        @NotBlank
        private String gatewayHostname;

        // Timeout duration for all requests going via the gateway
        // default = 30,000 (see histrixTimeoutInMillis if changing this value)
        private Integer timeoutInMillis;

        // optional value to add debug information about the gateway requests
        private Boolean debugHeaders;

        // Hystrix timeout should be not less then ribbon.ConnectTimeout + ribbon.ReadTimeout) ...
        // ... * (ribbon.MaxAutoRetries(default = 0) + 1) * (ribbon.MaxAutoRetriesNextServer(default = 1) + 1)
        // default = 240,000
        private Integer histrixTimeoutInMillis;

        public GatewayProperties() {
        }

        public @NotBlank String getGatewayHostname() {
            return this.gatewayHostname;
        }

        public void setGatewayHostname(@NotBlank String gatewayHostname) {
            this.gatewayHostname = gatewayHostname;
        }

        public Integer getTimeoutInMillis() {
            return this.timeoutInMillis;
        }

        public void setTimeoutInMillis(Integer timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
        }

        public Boolean getDebugHeaders() {
            return this.debugHeaders;
        }

        public void setDebugHeaders(Boolean debugHeaders) {
            this.debugHeaders = debugHeaders;
        }

        public Integer getHistrixTimeoutInMillis() {
            return this.histrixTimeoutInMillis;
        }

        public void setHistrixTimeoutInMillis(Integer histrixTimeoutInMillis) {
            this.histrixTimeoutInMillis = histrixTimeoutInMillis;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof GatewayProperties)) return false;
            final GatewayProperties other = (GatewayProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$gatewayHostname = this.getGatewayHostname();
            final Object other$gatewayHostname = other.getGatewayHostname();
            if (this$gatewayHostname == null ? other$gatewayHostname != null : !this$gatewayHostname.equals(other$gatewayHostname))
                return false;
            final Object this$timeoutInMillis = this.getTimeoutInMillis();
            final Object other$timeoutInMillis = other.getTimeoutInMillis();
            if (this$timeoutInMillis == null ? other$timeoutInMillis != null : !this$timeoutInMillis.equals(other$timeoutInMillis))
                return false;
            final Object this$debugHeaders = this.getDebugHeaders();
            final Object other$debugHeaders = other.getDebugHeaders();
            if (this$debugHeaders == null ? other$debugHeaders != null : !this$debugHeaders.equals(other$debugHeaders))
                return false;
            final Object this$histrixTimeoutInMillis = this.getHistrixTimeoutInMillis();
            final Object other$histrixTimeoutInMillis = other.getHistrixTimeoutInMillis();
            if (this$histrixTimeoutInMillis == null ? other$histrixTimeoutInMillis != null : !this$histrixTimeoutInMillis.equals(other$histrixTimeoutInMillis))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof GatewayProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $gatewayHostname = this.getGatewayHostname();
            result = result * PRIME + ($gatewayHostname == null ? 43 : $gatewayHostname.hashCode());
            final Object $timeoutInMillis = this.getTimeoutInMillis();
            result = result * PRIME + ($timeoutInMillis == null ? 43 : $timeoutInMillis.hashCode());
            final Object $debugHeaders = this.getDebugHeaders();
            result = result * PRIME + ($debugHeaders == null ? 43 : $debugHeaders.hashCode());
            final Object $histrixTimeoutInMillis = this.getHistrixTimeoutInMillis();
            result = result * PRIME + ($histrixTimeoutInMillis == null ? 43 : $histrixTimeoutInMillis.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.GatewayProperties(gatewayHostname=" + this.getGatewayHostname() + ", timeoutInMillis=" + this.getTimeoutInMillis() + ", debugHeaders=" + this.getDebugHeaders() + ", histrixTimeoutInMillis=" + this.getHistrixTimeoutInMillis() + ")";
        }
    }

    /**
     * Internal class used for changing timing values for discovery services and cache updates
     * Should not normally need to be changed by a client service
     * Change with caution or weirdness may occur !
     */
    public static class ServiceRegistryProperties {
        // amount of time to wait before retrying to fetch a service from discovery
        private Integer serviceFetchDelayInMillis;

        // check for any container instances which have changed within this period
        private Integer cacheRefreshUpdateThresholdInMillis;

        // wait this amount of time before populating the cache
        private Integer cacheRefreshInitialDelayInMillis;

        // if the cache cannot read discovery data, then wait this amount of time before retrying
        private Integer cacheRefreshRetryDelayInMillis;

        public ServiceRegistryProperties() {
        }

        public Integer getServiceFetchDelayInMillis() {
            return this.serviceFetchDelayInMillis;
        }

        public void setServiceFetchDelayInMillis(Integer serviceFetchDelayInMillis) {
            this.serviceFetchDelayInMillis = serviceFetchDelayInMillis;
        }

        public Integer getCacheRefreshUpdateThresholdInMillis() {
            return this.cacheRefreshUpdateThresholdInMillis;
        }

        public void setCacheRefreshUpdateThresholdInMillis(Integer cacheRefreshUpdateThresholdInMillis) {
            this.cacheRefreshUpdateThresholdInMillis = cacheRefreshUpdateThresholdInMillis;
        }

        public Integer getCacheRefreshInitialDelayInMillis() {
            return this.cacheRefreshInitialDelayInMillis;
        }

        public void setCacheRefreshInitialDelayInMillis(Integer cacheRefreshInitialDelayInMillis) {
            this.cacheRefreshInitialDelayInMillis = cacheRefreshInitialDelayInMillis;
        }

        public Integer getCacheRefreshRetryDelayInMillis() {
            return this.cacheRefreshRetryDelayInMillis;
        }

        public void setCacheRefreshRetryDelayInMillis(Integer cacheRefreshRetryDelayInMillis) {
            this.cacheRefreshRetryDelayInMillis = cacheRefreshRetryDelayInMillis;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof ServiceRegistryProperties))
                return false;
            final ServiceRegistryProperties other = (ServiceRegistryProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$serviceFetchDelayInMillis = this.getServiceFetchDelayInMillis();
            final Object other$serviceFetchDelayInMillis = other.getServiceFetchDelayInMillis();
            if (this$serviceFetchDelayInMillis == null ? other$serviceFetchDelayInMillis != null : !this$serviceFetchDelayInMillis.equals(other$serviceFetchDelayInMillis))
                return false;
            final Object this$cacheRefreshUpdateThresholdInMillis = this.getCacheRefreshUpdateThresholdInMillis();
            final Object other$cacheRefreshUpdateThresholdInMillis = other.getCacheRefreshUpdateThresholdInMillis();
            if (this$cacheRefreshUpdateThresholdInMillis == null ? other$cacheRefreshUpdateThresholdInMillis != null : !this$cacheRefreshUpdateThresholdInMillis.equals(other$cacheRefreshUpdateThresholdInMillis))
                return false;
            final Object this$cacheRefreshInitialDelayInMillis = this.getCacheRefreshInitialDelayInMillis();
            final Object other$cacheRefreshInitialDelayInMillis = other.getCacheRefreshInitialDelayInMillis();
            if (this$cacheRefreshInitialDelayInMillis == null ? other$cacheRefreshInitialDelayInMillis != null : !this$cacheRefreshInitialDelayInMillis.equals(other$cacheRefreshInitialDelayInMillis))
                return false;
            final Object this$cacheRefreshRetryDelayInMillis = this.getCacheRefreshRetryDelayInMillis();
            final Object other$cacheRefreshRetryDelayInMillis = other.getCacheRefreshRetryDelayInMillis();
            if (this$cacheRefreshRetryDelayInMillis == null ? other$cacheRefreshRetryDelayInMillis != null : !this$cacheRefreshRetryDelayInMillis.equals(other$cacheRefreshRetryDelayInMillis))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof ServiceRegistryProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $serviceFetchDelayInMillis = this.getServiceFetchDelayInMillis();
            result = result * PRIME + ($serviceFetchDelayInMillis == null ? 43 : $serviceFetchDelayInMillis.hashCode());
            final Object $cacheRefreshUpdateThresholdInMillis = this.getCacheRefreshUpdateThresholdInMillis();
            result = result * PRIME + ($cacheRefreshUpdateThresholdInMillis == null ? 43 : $cacheRefreshUpdateThresholdInMillis.hashCode());
            final Object $cacheRefreshInitialDelayInMillis = this.getCacheRefreshInitialDelayInMillis();
            result = result * PRIME + ($cacheRefreshInitialDelayInMillis == null ? 43 : $cacheRefreshInitialDelayInMillis.hashCode());
            final Object $cacheRefreshRetryDelayInMillis = this.getCacheRefreshRetryDelayInMillis();
            result = result * PRIME + ($cacheRefreshRetryDelayInMillis == null ? 43 : $cacheRefreshRetryDelayInMillis.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.ServiceRegistryProperties(serviceFetchDelayInMillis=" + this.getServiceFetchDelayInMillis() + ", cacheRefreshUpdateThresholdInMillis=" + this.getCacheRefreshUpdateThresholdInMillis() + ", cacheRefreshInitialDelayInMillis=" + this.getCacheRefreshInitialDelayInMillis() + ", cacheRefreshRetryDelayInMillis=" + this.getCacheRefreshRetryDelayInMillis() + ")";
        }
    }

    public static class DiscoveryEndpoints {
        // Home page of the client
        private String statusPage;

        // health url of the client (defaults to actuator health endpoint)
        private String healthPage;

        // status url of the client (defaults to actuator status endpoint)
        private String homepage;

        public DiscoveryEndpoints() {
        }

        public String getStatusPage() {
            return this.statusPage;
        }

        public void setStatusPage(String statusPage) {
            this.statusPage = statusPage;
        }

        public String getHealthPage() {
            return this.healthPage;
        }

        public void setHealthPage(String healthPage) {
            this.healthPage = healthPage;
        }

        public String getHomepage() {
            return this.homepage;
        }

        public void setHomepage(String homepage) {
            this.homepage = homepage;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof DiscoveryEndpoints)) return false;
            final DiscoveryEndpoints other = (DiscoveryEndpoints) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$statusPage = this.getStatusPage();
            final Object other$statusPage = other.getStatusPage();
            if (this$statusPage == null ? other$statusPage != null : !this$statusPage.equals(other$statusPage))
                return false;
            final Object this$healthPage = this.getHealthPage();
            final Object other$healthPage = other.getHealthPage();
            if (this$healthPage == null ? other$healthPage != null : !this$healthPage.equals(other$healthPage))
                return false;
            final Object this$homepage = this.getHomepage();
            final Object other$homepage = other.getHomepage();
            if (this$homepage == null ? other$homepage != null : !this$homepage.equals(other$homepage)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof DiscoveryEndpoints;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $statusPage = this.getStatusPage();
            result = result * PRIME + ($statusPage == null ? 43 : $statusPage.hashCode());
            final Object $healthPage = this.getHealthPage();
            result = result * PRIME + ($healthPage == null ? 43 : $healthPage.hashCode());
            final Object $homepage = this.getHomepage();
            result = result * PRIME + ($homepage == null ? 43 : $homepage.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.DiscoveryEndpoints(statusPage=" + this.getStatusPage() + ", healthPage=" + this.getHealthPage() + ", homepage=" + this.getHomepage() + ")";
        }
    }

    public static class ServiceInfo {
        // Descriptive title of the service, used to populate the UI
        private String serviceTitle;

        // Detailed description of the service, used to populate the UI
        private String description;

        // TRUE if the service has API documentation it want's to expose to the gateway/catalog
        private Boolean enableApiDoc;

        // The location of a manually defined swagger endpoints, note disables automatic discovery of endpoints and swagger generation
        private String swaggerLocation;

        public ServiceInfo() {
        }

        public String getServiceTitle() {
            return this.serviceTitle;
        }

        public void setServiceTitle(String serviceTitle) {
            this.serviceTitle = serviceTitle;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getEnableApiDoc() {
            return this.enableApiDoc;
        }

        public void setEnableApiDoc(Boolean enableApiDoc) {
            this.enableApiDoc = enableApiDoc;
        }

        public String getSwaggerLocation() {
            return this.swaggerLocation;
        }

        public void setSwaggerLocation(String swaggerLocation) {
            this.swaggerLocation = swaggerLocation;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof ServiceInfo)) return false;
            final ServiceInfo other = (ServiceInfo) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$serviceTitle = this.getServiceTitle();
            final Object other$serviceTitle = other.getServiceTitle();
            if (this$serviceTitle == null ? other$serviceTitle != null : !this$serviceTitle.equals(other$serviceTitle))
                return false;
            final Object this$description = this.getDescription();
            final Object other$description = other.getDescription();
            if (this$description == null ? other$description != null : !this$description.equals(other$description))
                return false;
            final Object this$enableApiDoc = this.getEnableApiDoc();
            final Object other$enableApiDoc = other.getEnableApiDoc();
            if (this$enableApiDoc == null ? other$enableApiDoc != null : !this$enableApiDoc.equals(other$enableApiDoc))
                return false;
            final Object this$swaggerLocation = this.getSwaggerLocation();
            final Object other$swaggerLocation = other.getSwaggerLocation();
            if (this$swaggerLocation == null ? other$swaggerLocation != null : !this$swaggerLocation.equals(other$swaggerLocation))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof ServiceInfo;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $serviceTitle = this.getServiceTitle();
            result = result * PRIME + ($serviceTitle == null ? 43 : $serviceTitle.hashCode());
            final Object $description = this.getDescription();
            result = result * PRIME + ($description == null ? 43 : $description.hashCode());
            final Object $enableApiDoc = this.getEnableApiDoc();
            result = result * PRIME + ($enableApiDoc == null ? 43 : $enableApiDoc.hashCode());
            final Object $swaggerLocation = this.getSwaggerLocation();
            result = result * PRIME + ($swaggerLocation == null ? 43 : $swaggerLocation.hashCode());
            return result;
        }

        public String toString() {
            return "MFaaSConfigPropertiesContainer.ServiceInfo(serviceTitle=" + this.getServiceTitle() + ", description=" + this.getDescription() + ", enableApiDoc=" + this.getEnableApiDoc() + ", swaggerLocation=" + this.getSwaggerLocation() + ")";
        }
    }
}
