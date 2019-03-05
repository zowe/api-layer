/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.discovery.staticdef;

/**
 * Represents one routes subservice inside a service.
 */
class Route {
    /**
     * The beginning of the path at the gateway.
     */
    private String gatewayUrl;

    /**
     * Continuation of the path at the service after the base path of the service.
     */
    private String serviceRelativeUrl;

    public Route() {
    }

    public String getGatewayUrl() {
        return this.gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getServiceRelativeUrl() {
        return this.serviceRelativeUrl;
    }

    public void setServiceRelativeUrl(String serviceRelativeUrl) {
        this.serviceRelativeUrl = serviceRelativeUrl;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Route)) return false;
        final Route other = (Route) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$gatewayUrl = this.getGatewayUrl();
        final Object other$gatewayUrl = other.getGatewayUrl();
        if (this$gatewayUrl == null ? other$gatewayUrl != null : !this$gatewayUrl.equals(other$gatewayUrl))
            return false;
        final Object this$serviceRelativeUrl = this.getServiceRelativeUrl();
        final Object other$serviceRelativeUrl = other.getServiceRelativeUrl();
        if (this$serviceRelativeUrl == null ? other$serviceRelativeUrl != null : !this$serviceRelativeUrl.equals(other$serviceRelativeUrl))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Route;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $gatewayUrl = this.getGatewayUrl();
        result = result * PRIME + ($gatewayUrl == null ? 43 : $gatewayUrl.hashCode());
        final Object $serviceRelativeUrl = this.getServiceRelativeUrl();
        result = result * PRIME + ($serviceRelativeUrl == null ? 43 : $serviceRelativeUrl.hashCode());
        return result;
    }

    public String toString() {
        return "Route(gatewayUrl=" + this.getGatewayUrl() + ", serviceRelativeUrl=" + this.getServiceRelativeUrl() + ")";
    }
}
