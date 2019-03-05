/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.enabler.config;

public class Route {
    private String gatewayUrl;
    private String serviceUrl;

    @java.beans.ConstructorProperties({"gatewayUrl", "serviceUrl"})
    public Route(String gatewayUrl, String serviceUrl) {
        this.gatewayUrl = gatewayUrl;
        this.serviceUrl = serviceUrl;
    }

    public Route() {
    }

    public String getGatewayUrl() {
        return this.gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getServiceUrl() {
        return this.serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
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
        final Object this$serviceUrl = this.getServiceUrl();
        final Object other$serviceUrl = other.getServiceUrl();
        if (this$serviceUrl == null ? other$serviceUrl != null : !this$serviceUrl.equals(other$serviceUrl))
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
        final Object $serviceUrl = this.getServiceUrl();
        result = result * PRIME + ($serviceUrl == null ? 43 : $serviceUrl.hashCode());
        return result;
    }

    public String toString() {
        return "Route(gatewayUrl=" + this.getGatewayUrl() + ", serviceUrl=" + this.getServiceUrl() + ")";
    }
}
