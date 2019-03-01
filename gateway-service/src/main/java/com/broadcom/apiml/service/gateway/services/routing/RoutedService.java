/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.gateway.services.routing;

public class RoutedService {

    private final String subServiceId;
    private final String gatewayUrl;
    private final String serviceUrl;

    @java.beans.ConstructorProperties({"subServiceId", "gatewayUrl", "serviceUrl"})
    public RoutedService(String subServiceId, String gatewayUrl, String serviceUrl) {
        this.subServiceId = subServiceId;
        this.gatewayUrl = gatewayUrl;
        this.serviceUrl = serviceUrl;
    }

    public String getSubServiceId() {
        return this.subServiceId;
    }

    public String getGatewayUrl() {
        return this.gatewayUrl;
    }

    public String getServiceUrl() {
        return this.serviceUrl;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof RoutedService)) return false;
        final RoutedService other = (RoutedService) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$subServiceId = this.getSubServiceId();
        final Object other$subServiceId = other.getSubServiceId();
        if (this$subServiceId == null ? other$subServiceId != null : !this$subServiceId.equals(other$subServiceId))
            return false;
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
        return other instanceof RoutedService;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $subServiceId = this.getSubServiceId();
        result = result * PRIME + ($subServiceId == null ? 43 : $subServiceId.hashCode());
        final Object $gatewayUrl = this.getGatewayUrl();
        result = result * PRIME + ($gatewayUrl == null ? 43 : $gatewayUrl.hashCode());
        final Object $serviceUrl = this.getServiceUrl();
        result = result * PRIME + ($serviceUrl == null ? 43 : $serviceUrl.hashCode());
        return result;
    }

    public String toString() {
        return "RoutedService(subServiceId=" + this.getSubServiceId() + ", gatewayUrl=" + this.getGatewayUrl() + ", serviceUrl=" + this.getServiceUrl() + ")";
    }
}
