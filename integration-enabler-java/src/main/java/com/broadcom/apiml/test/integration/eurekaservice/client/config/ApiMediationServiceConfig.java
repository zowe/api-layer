/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.eurekaservice.client.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ApiMediationServiceConfig {
    private List<String> discoveryServiceUrls;
    private String serviceId;
    private String title;
    private String description;
    private String baseUrl;
    private String homePageRelativeUrl;
    private String statusPageRelativeUrl;
    private String healthCheckRelativeUrl;
    private String contextPath;
    private String defaultZone;
    private Boolean securePortEnabled;
    private List<Route> routes;
    private ShortApiInfo shortApiInfo;
    private CatalogUiTile catalogUiTile;
    private Ssl ssl;
    private Eureka eureka;

    @java.beans.ConstructorProperties({"discoveryServiceUrls", "serviceId", "title", "description", "baseUrl", "homePageRelativeUrl", "statusPageRelativeUrl", "healthCheckRelativeUrl", "contextPath", "defaultZone", "securePortEnabled", "routes", "shortApiInfo", "catalogUiTile", "ssl", "eureka"})
    public ApiMediationServiceConfig(List<String> discoveryServiceUrls, String serviceId, String title, String description, String baseUrl, String homePageRelativeUrl, String statusPageRelativeUrl, String healthCheckRelativeUrl, String contextPath, String defaultZone, Boolean securePortEnabled, List<Route> routes, ShortApiInfo shortApiInfo, CatalogUiTile catalogUiTile, Ssl ssl, Eureka eureka) {
        this.discoveryServiceUrls = discoveryServiceUrls;
        this.serviceId = serviceId;
        this.title = title;
        this.description = description;
        this.baseUrl = baseUrl;
        this.homePageRelativeUrl = homePageRelativeUrl;
        this.statusPageRelativeUrl = statusPageRelativeUrl;
        this.healthCheckRelativeUrl = healthCheckRelativeUrl;
        this.contextPath = contextPath;
        this.defaultZone = defaultZone;
        this.securePortEnabled = securePortEnabled;
        this.routes = routes;
        this.shortApiInfo = shortApiInfo;
        this.catalogUiTile = catalogUiTile;
        this.ssl = ssl;
        this.eureka = eureka;
    }

    public ApiMediationServiceConfig() {
    }

    public static ApiMediationServiceConfigBuilder builder() {
        return new ApiMediationServiceConfigBuilder();
    }

    public List<String> getDiscoveryServiceUrls() {
        return this.discoveryServiceUrls;
    }

    public void setDiscoveryServiceUrls(List<String> discoveryServiceUrls) {
        this.discoveryServiceUrls = discoveryServiceUrls;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHomePageRelativeUrl() {
        return this.homePageRelativeUrl;
    }

    public void setHomePageRelativeUrl(String homePageRelativeUrl) {
        this.homePageRelativeUrl = homePageRelativeUrl;
    }

    public String getStatusPageRelativeUrl() {
        return this.statusPageRelativeUrl;
    }

    public void setStatusPageRelativeUrl(String statusPageRelativeUrl) {
        this.statusPageRelativeUrl = statusPageRelativeUrl;
    }

    public String getHealthCheckRelativeUrl() {
        return this.healthCheckRelativeUrl;
    }

    public void setHealthCheckRelativeUrl(String healthCheckRelativeUrl) {
        this.healthCheckRelativeUrl = healthCheckRelativeUrl;
    }

    public String getContextPath() {
        return this.contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getDefaultZone() {
        return this.defaultZone;
    }

    public void setDefaultZone(String defaultZone) {
        this.defaultZone = defaultZone;
    }

    public Boolean getSecurePortEnabled() {
        return this.securePortEnabled;
    }

    public void setSecurePortEnabled(Boolean securePortEnabled) {
        this.securePortEnabled = securePortEnabled;
    }

    public List<Route> getRoutes() {
        return this.routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public ShortApiInfo getShortApiInfo() {
        return this.shortApiInfo;
    }

    public void setShortApiInfo(ShortApiInfo shortApiInfo) {
        this.shortApiInfo = shortApiInfo;
    }

    public CatalogUiTile getCatalogUiTile() {
        return this.catalogUiTile;
    }

    public void setCatalogUiTile(CatalogUiTile catalogUiTile) {
        this.catalogUiTile = catalogUiTile;
    }

    public Ssl getSsl() {
        return this.ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    public Eureka getEureka() {
        return this.eureka;
    }

    public void setEureka(Eureka eureka) {
        this.eureka = eureka;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiMediationServiceConfig)) return false;
        final ApiMediationServiceConfig other = (ApiMediationServiceConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$discoveryServiceUrls = this.getDiscoveryServiceUrls();
        final Object other$discoveryServiceUrls = other.getDiscoveryServiceUrls();
        if (this$discoveryServiceUrls == null ? other$discoveryServiceUrls != null : !this$discoveryServiceUrls.equals(other$discoveryServiceUrls))
            return false;
        final Object this$serviceId = this.getServiceId();
        final Object other$serviceId = other.getServiceId();
        if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId)) return false;
        final Object this$title = this.getTitle();
        final Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final Object this$baseUrl = this.getBaseUrl();
        final Object other$baseUrl = other.getBaseUrl();
        if (this$baseUrl == null ? other$baseUrl != null : !this$baseUrl.equals(other$baseUrl)) return false;
        final Object this$homePageRelativeUrl = this.getHomePageRelativeUrl();
        final Object other$homePageRelativeUrl = other.getHomePageRelativeUrl();
        if (this$homePageRelativeUrl == null ? other$homePageRelativeUrl != null : !this$homePageRelativeUrl.equals(other$homePageRelativeUrl))
            return false;
        final Object this$statusPageRelativeUrl = this.getStatusPageRelativeUrl();
        final Object other$statusPageRelativeUrl = other.getStatusPageRelativeUrl();
        if (this$statusPageRelativeUrl == null ? other$statusPageRelativeUrl != null : !this$statusPageRelativeUrl.equals(other$statusPageRelativeUrl))
            return false;
        final Object this$healthCheckRelativeUrl = this.getHealthCheckRelativeUrl();
        final Object other$healthCheckRelativeUrl = other.getHealthCheckRelativeUrl();
        if (this$healthCheckRelativeUrl == null ? other$healthCheckRelativeUrl != null : !this$healthCheckRelativeUrl.equals(other$healthCheckRelativeUrl))
            return false;
        final Object this$contextPath = this.getContextPath();
        final Object other$contextPath = other.getContextPath();
        if (this$contextPath == null ? other$contextPath != null : !this$contextPath.equals(other$contextPath))
            return false;
        final Object this$defaultZone = this.getDefaultZone();
        final Object other$defaultZone = other.getDefaultZone();
        if (this$defaultZone == null ? other$defaultZone != null : !this$defaultZone.equals(other$defaultZone))
            return false;
        final Object this$securePortEnabled = this.getSecurePortEnabled();
        final Object other$securePortEnabled = other.getSecurePortEnabled();
        if (this$securePortEnabled == null ? other$securePortEnabled != null : !this$securePortEnabled.equals(other$securePortEnabled))
            return false;
        final Object this$routes = this.getRoutes();
        final Object other$routes = other.getRoutes();
        if (this$routes == null ? other$routes != null : !this$routes.equals(other$routes)) return false;
        final Object this$shortApiInfo = this.getShortApiInfo();
        final Object other$shortApiInfo = other.getShortApiInfo();
        if (this$shortApiInfo == null ? other$shortApiInfo != null : !this$shortApiInfo.equals(other$shortApiInfo))
            return false;
        final Object this$catalogUiTile = this.getCatalogUiTile();
        final Object other$catalogUiTile = other.getCatalogUiTile();
        if (this$catalogUiTile == null ? other$catalogUiTile != null : !this$catalogUiTile.equals(other$catalogUiTile))
            return false;
        final Object this$ssl = this.getSsl();
        final Object other$ssl = other.getSsl();
        if (this$ssl == null ? other$ssl != null : !this$ssl.equals(other$ssl)) return false;
        final Object this$eureka = this.getEureka();
        final Object other$eureka = other.getEureka();
        if (this$eureka == null ? other$eureka != null : !this$eureka.equals(other$eureka)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApiMediationServiceConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $discoveryServiceUrls = this.getDiscoveryServiceUrls();
        result = result * PRIME + ($discoveryServiceUrls == null ? 43 : $discoveryServiceUrls.hashCode());
        final Object $serviceId = this.getServiceId();
        result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
        final Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $baseUrl = this.getBaseUrl();
        result = result * PRIME + ($baseUrl == null ? 43 : $baseUrl.hashCode());
        final Object $homePageRelativeUrl = this.getHomePageRelativeUrl();
        result = result * PRIME + ($homePageRelativeUrl == null ? 43 : $homePageRelativeUrl.hashCode());
        final Object $statusPageRelativeUrl = this.getStatusPageRelativeUrl();
        result = result * PRIME + ($statusPageRelativeUrl == null ? 43 : $statusPageRelativeUrl.hashCode());
        final Object $healthCheckRelativeUrl = this.getHealthCheckRelativeUrl();
        result = result * PRIME + ($healthCheckRelativeUrl == null ? 43 : $healthCheckRelativeUrl.hashCode());
        final Object $contextPath = this.getContextPath();
        result = result * PRIME + ($contextPath == null ? 43 : $contextPath.hashCode());
        final Object $defaultZone = this.getDefaultZone();
        result = result * PRIME + ($defaultZone == null ? 43 : $defaultZone.hashCode());
        final Object $securePortEnabled = this.getSecurePortEnabled();
        result = result * PRIME + ($securePortEnabled == null ? 43 : $securePortEnabled.hashCode());
        final Object $routes = this.getRoutes();
        result = result * PRIME + ($routes == null ? 43 : $routes.hashCode());
        final Object $shortApiInfo = this.getShortApiInfo();
        result = result * PRIME + ($shortApiInfo == null ? 43 : $shortApiInfo.hashCode());
        final Object $catalogUiTile = this.getCatalogUiTile();
        result = result * PRIME + ($catalogUiTile == null ? 43 : $catalogUiTile.hashCode());
        final Object $ssl = this.getSsl();
        result = result * PRIME + ($ssl == null ? 43 : $ssl.hashCode());
        final Object $eureka = this.getEureka();
        result = result * PRIME + ($eureka == null ? 43 : $eureka.hashCode());
        return result;
    }

    public String toString() {
        return "ApiMediationServiceConfig(discoveryServiceUrls=" + this.getDiscoveryServiceUrls() + ", serviceId=" + this.getServiceId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", baseUrl=" + this.getBaseUrl() + ", homePageRelativeUrl=" + this.getHomePageRelativeUrl() + ", statusPageRelativeUrl=" + this.getStatusPageRelativeUrl() + ", healthCheckRelativeUrl=" + this.getHealthCheckRelativeUrl() + ", contextPath=" + this.getContextPath() + ", defaultZone=" + this.getDefaultZone() + ", securePortEnabled=" + this.getSecurePortEnabled() + ", routes=" + this.getRoutes() + ", shortApiInfo=" + this.getShortApiInfo() + ", catalogUiTile=" + this.getCatalogUiTile() + ", ssl=" + this.getSsl() + ", eureka=" + this.getEureka() + ")";
    }

    public ApiMediationServiceConfigBuilder toBuilder() {
        return new ApiMediationServiceConfigBuilder().discoveryServiceUrls(this.discoveryServiceUrls).serviceId(this.serviceId).title(this.title).description(this.description).baseUrl(this.baseUrl).homePageRelativeUrl(this.homePageRelativeUrl).statusPageRelativeUrl(this.statusPageRelativeUrl).healthCheckRelativeUrl(this.healthCheckRelativeUrl).contextPath(this.contextPath).defaultZone(this.defaultZone).securePortEnabled(this.securePortEnabled).routes(this.routes).shortApiInfo(this.shortApiInfo).catalogUiTile(this.catalogUiTile).ssl(this.ssl).eureka(this.eureka);
    }

    public static class ApiMediationServiceConfigBuilder {
        private ArrayList<String> discoveryServiceUrls;
        private String serviceId;
        private String title;
        private String description;
        private String baseUrl;
        private String homePageRelativeUrl;
        private String statusPageRelativeUrl;
        private String healthCheckRelativeUrl;
        private String contextPath;
        private String defaultZone;
        private Boolean securePortEnabled;
        private ArrayList<Route> routes;
        private ShortApiInfo shortApiInfo;
        private CatalogUiTile catalogUiTile;
        private Ssl ssl;
        private Eureka eureka;

        ApiMediationServiceConfigBuilder() {
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder discoveryServiceUrl(String discoveryServiceUrl) {
            if (this.discoveryServiceUrls == null) this.discoveryServiceUrls = new ArrayList<String>();
            this.discoveryServiceUrls.add(discoveryServiceUrl);
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder discoveryServiceUrls(Collection<? extends String> discoveryServiceUrls) {
            if (this.discoveryServiceUrls == null) this.discoveryServiceUrls = new ArrayList<String>();
            this.discoveryServiceUrls.addAll(discoveryServiceUrls);
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder clearDiscoveryServiceUrls() {
            if (this.discoveryServiceUrls != null)
                this.discoveryServiceUrls.clear();

            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder homePageRelativeUrl(String homePageRelativeUrl) {
            this.homePageRelativeUrl = homePageRelativeUrl;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder statusPageRelativeUrl(String statusPageRelativeUrl) {
            this.statusPageRelativeUrl = statusPageRelativeUrl;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder healthCheckRelativeUrl(String healthCheckRelativeUrl) {
            this.healthCheckRelativeUrl = healthCheckRelativeUrl;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder defaultZone(String defaultZone) {
            this.defaultZone = defaultZone;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder securePortEnabled(Boolean securePortEnabled) {
            this.securePortEnabled = securePortEnabled;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder route(Route route) {
            if (this.routes == null) this.routes = new ArrayList<Route>();
            this.routes.add(route);
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder routes(Collection<? extends Route> routes) {
            if (this.routes == null) this.routes = new ArrayList<Route>();
            this.routes.addAll(routes);
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder clearRoutes() {
            if (this.routes != null)
                this.routes.clear();

            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder shortApiInfo(ShortApiInfo shortApiInfo) {
            this.shortApiInfo = shortApiInfo;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder catalogUiTile(CatalogUiTile catalogUiTile) {
            this.catalogUiTile = catalogUiTile;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder ssl(Ssl ssl) {
            this.ssl = ssl;
            return this;
        }

        public ApiMediationServiceConfig.ApiMediationServiceConfigBuilder eureka(Eureka eureka) {
            this.eureka = eureka;
            return this;
        }

        public ApiMediationServiceConfig build() {
            List<String> discoveryServiceUrls;
            switch (this.discoveryServiceUrls == null ? 0 : this.discoveryServiceUrls.size()) {
                case 0:
                    discoveryServiceUrls = java.util.Collections.emptyList();
                    break;
                case 1:
                    discoveryServiceUrls = java.util.Collections.singletonList(this.discoveryServiceUrls.get(0));
                    break;
                default:
                    discoveryServiceUrls = java.util.Collections.unmodifiableList(new ArrayList<String>(this.discoveryServiceUrls));
            }
            List<Route> routes;
            switch (this.routes == null ? 0 : this.routes.size()) {
                case 0:
                    routes = java.util.Collections.emptyList();
                    break;
                case 1:
                    routes = java.util.Collections.singletonList(this.routes.get(0));
                    break;
                default:
                    routes = java.util.Collections.unmodifiableList(new ArrayList<Route>(this.routes));
            }

            return new ApiMediationServiceConfig(discoveryServiceUrls, serviceId, title, description, baseUrl, homePageRelativeUrl, statusPageRelativeUrl, healthCheckRelativeUrl, contextPath, defaultZone, securePortEnabled, routes, shortApiInfo, catalogUiTile, ssl, eureka);
        }

        public String toString() {
            return "ApiMediationServiceConfig.ApiMediationServiceConfigBuilder(discoveryServiceUrls=" + this.discoveryServiceUrls + ", serviceId=" + this.serviceId + ", title=" + this.title + ", description=" + this.description + ", baseUrl=" + this.baseUrl + ", homePageRelativeUrl=" + this.homePageRelativeUrl + ", statusPageRelativeUrl=" + this.statusPageRelativeUrl + ", healthCheckRelativeUrl=" + this.healthCheckRelativeUrl + ", contextPath=" + this.contextPath + ", defaultZone=" + this.defaultZone + ", securePortEnabled=" + this.securePortEnabled + ", routes=" + this.routes + ", shortApiInfo=" + this.shortApiInfo + ", catalogUiTile=" + this.catalogUiTile + ", ssl=" + this.ssl + ", eureka=" + this.eureka + ")";
        }
    }
}
