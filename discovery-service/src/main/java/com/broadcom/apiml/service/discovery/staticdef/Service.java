/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.discovery.staticdef;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * Represents one services with multiple instances.
 * <p>
 * Each instance can have different base URL (http(s)://hostname:port/contextPath/).
 * The other URLs are relative to it.
 */
class Service {
    private String serviceId;
    private String title;
    private String description;
    private String catalogUiTileId;
    private List<String> instanceBaseUrls;
    private String homePageRelativeUrl;
    private String statusPageRelativeUrl;
    private String healthCheckRelativeUrl;
    @JsonAlias({"routedServices"})
    private List<Route> routes;

    public Service() {
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

    public String getCatalogUiTileId() {
        return this.catalogUiTileId;
    }

    public void setCatalogUiTileId(String catalogUiTileId) {
        this.catalogUiTileId = catalogUiTileId;
    }

    public List<String> getInstanceBaseUrls() {
        return this.instanceBaseUrls;
    }

    public void setInstanceBaseUrls(List<String> instanceBaseUrls) {
        this.instanceBaseUrls = instanceBaseUrls;
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

    public List<Route> getRoutes() {
        return this.routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Service)) return false;
        final Service other = (Service) o;
        if (!other.canEqual((Object) this)) return false;
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
        final Object this$catalogUiTileId = this.getCatalogUiTileId();
        final Object other$catalogUiTileId = other.getCatalogUiTileId();
        if (this$catalogUiTileId == null ? other$catalogUiTileId != null : !this$catalogUiTileId.equals(other$catalogUiTileId))
            return false;
        final Object this$instanceBaseUrls = this.getInstanceBaseUrls();
        final Object other$instanceBaseUrls = other.getInstanceBaseUrls();
        if (this$instanceBaseUrls == null ? other$instanceBaseUrls != null : !this$instanceBaseUrls.equals(other$instanceBaseUrls))
            return false;
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
        final Object this$routes = this.getRoutes();
        final Object other$routes = other.getRoutes();
        if (this$routes == null ? other$routes != null : !this$routes.equals(other$routes)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Service;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $serviceId = this.getServiceId();
        result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
        final Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $catalogUiTileId = this.getCatalogUiTileId();
        result = result * PRIME + ($catalogUiTileId == null ? 43 : $catalogUiTileId.hashCode());
        final Object $instanceBaseUrls = this.getInstanceBaseUrls();
        result = result * PRIME + ($instanceBaseUrls == null ? 43 : $instanceBaseUrls.hashCode());
        final Object $homePageRelativeUrl = this.getHomePageRelativeUrl();
        result = result * PRIME + ($homePageRelativeUrl == null ? 43 : $homePageRelativeUrl.hashCode());
        final Object $statusPageRelativeUrl = this.getStatusPageRelativeUrl();
        result = result * PRIME + ($statusPageRelativeUrl == null ? 43 : $statusPageRelativeUrl.hashCode());
        final Object $healthCheckRelativeUrl = this.getHealthCheckRelativeUrl();
        result = result * PRIME + ($healthCheckRelativeUrl == null ? 43 : $healthCheckRelativeUrl.hashCode());
        final Object $routes = this.getRoutes();
        result = result * PRIME + ($routes == null ? 43 : $routes.hashCode());
        return result;
    }

    public String toString() {
        return "Service(serviceId=" + this.getServiceId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", catalogUiTileId=" + this.getCatalogUiTileId() + ", instanceBaseUrls=" + this.getInstanceBaseUrls() + ", homePageRelativeUrl=" + this.getHomePageRelativeUrl() + ", statusPageRelativeUrl=" + this.getStatusPageRelativeUrl() + ", healthCheckRelativeUrl=" + this.getHealthCheckRelativeUrl() + ", routes=" + this.getRoutes() + ")";
    }
}
