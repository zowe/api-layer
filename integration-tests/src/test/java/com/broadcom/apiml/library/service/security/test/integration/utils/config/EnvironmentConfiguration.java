/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.utils.config;

public class EnvironmentConfiguration {
    private GatewayServiceConfiguration gatewayServiceConfiguration;
    private DiscoveryServiceConfiguration discoveryServiceConfiguration;
    private ApiCatalogServiceConfiguration apiCatalogServiceConfiguration;
    private TlsConfiguration tlsConfiguration;
    private ZosmfServiceConfiguration zosmfServiceConfiguration;

    @java.beans.ConstructorProperties({"gatewayServiceConfiguration", "discoveryServiceConfiguration", "apiCatalogServiceConfiguration", "tlsConfiguration", "zosmfServiceConfiguration"})
    public EnvironmentConfiguration(GatewayServiceConfiguration gatewayServiceConfiguration, DiscoveryServiceConfiguration discoveryServiceConfiguration, ApiCatalogServiceConfiguration apiCatalogServiceConfiguration, TlsConfiguration tlsConfiguration, ZosmfServiceConfiguration zosmfServiceConfiguration) {
        this.gatewayServiceConfiguration = gatewayServiceConfiguration;
        this.discoveryServiceConfiguration = discoveryServiceConfiguration;
        this.apiCatalogServiceConfiguration = apiCatalogServiceConfiguration;
        this.tlsConfiguration = tlsConfiguration;
        this.zosmfServiceConfiguration = zosmfServiceConfiguration;
    }

    public EnvironmentConfiguration() {
    }

    public GatewayServiceConfiguration getGatewayServiceConfiguration() {
        return this.gatewayServiceConfiguration;
    }

    public void setGatewayServiceConfiguration(GatewayServiceConfiguration gatewayServiceConfiguration) {
        this.gatewayServiceConfiguration = gatewayServiceConfiguration;
    }

    public DiscoveryServiceConfiguration getDiscoveryServiceConfiguration() {
        return this.discoveryServiceConfiguration;
    }

    public void setDiscoveryServiceConfiguration(DiscoveryServiceConfiguration discoveryServiceConfiguration) {
        this.discoveryServiceConfiguration = discoveryServiceConfiguration;
    }

    public ApiCatalogServiceConfiguration getApiCatalogServiceConfiguration() {
        return this.apiCatalogServiceConfiguration;
    }

    public void setApiCatalogServiceConfiguration(ApiCatalogServiceConfiguration apiCatalogServiceConfiguration) {
        this.apiCatalogServiceConfiguration = apiCatalogServiceConfiguration;
    }

    public TlsConfiguration getTlsConfiguration() {
        return this.tlsConfiguration;
    }

    public void setTlsConfiguration(TlsConfiguration tlsConfiguration) {
        this.tlsConfiguration = tlsConfiguration;
    }

    public ZosmfServiceConfiguration getZosmfServiceConfiguration() {
        return this.zosmfServiceConfiguration;
    }

    public void setZosmfServiceConfiguration(ZosmfServiceConfiguration zosmfServiceConfiguration) {
        this.zosmfServiceConfiguration = zosmfServiceConfiguration;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EnvironmentConfiguration)) return false;
        final EnvironmentConfiguration other = (EnvironmentConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$gatewayServiceConfiguration = this.getGatewayServiceConfiguration();
        final Object other$gatewayServiceConfiguration = other.getGatewayServiceConfiguration();
        if (this$gatewayServiceConfiguration == null ? other$gatewayServiceConfiguration != null : !this$gatewayServiceConfiguration.equals(other$gatewayServiceConfiguration))
            return false;
        final Object this$discoveryServiceConfiguration = this.getDiscoveryServiceConfiguration();
        final Object other$discoveryServiceConfiguration = other.getDiscoveryServiceConfiguration();
        if (this$discoveryServiceConfiguration == null ? other$discoveryServiceConfiguration != null : !this$discoveryServiceConfiguration.equals(other$discoveryServiceConfiguration))
            return false;
        final Object this$apiCatalogServiceConfiguration = this.getApiCatalogServiceConfiguration();
        final Object other$apiCatalogServiceConfiguration = other.getApiCatalogServiceConfiguration();
        if (this$apiCatalogServiceConfiguration == null ? other$apiCatalogServiceConfiguration != null : !this$apiCatalogServiceConfiguration.equals(other$apiCatalogServiceConfiguration))
            return false;
        final Object this$tlsConfiguration = this.getTlsConfiguration();
        final Object other$tlsConfiguration = other.getTlsConfiguration();
        if (this$tlsConfiguration == null ? other$tlsConfiguration != null : !this$tlsConfiguration.equals(other$tlsConfiguration))
            return false;
        final Object this$zosmfServiceConfiguration = this.getZosmfServiceConfiguration();
        final Object other$zosmfServiceConfiguration = other.getZosmfServiceConfiguration();
        if (this$zosmfServiceConfiguration == null ? other$zosmfServiceConfiguration != null : !this$zosmfServiceConfiguration.equals(other$zosmfServiceConfiguration))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof EnvironmentConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $gatewayServiceConfiguration = this.getGatewayServiceConfiguration();
        result = result * PRIME + ($gatewayServiceConfiguration == null ? 43 : $gatewayServiceConfiguration.hashCode());
        final Object $discoveryServiceConfiguration = this.getDiscoveryServiceConfiguration();
        result = result * PRIME + ($discoveryServiceConfiguration == null ? 43 : $discoveryServiceConfiguration.hashCode());
        final Object $apiCatalogServiceConfiguration = this.getApiCatalogServiceConfiguration();
        result = result * PRIME + ($apiCatalogServiceConfiguration == null ? 43 : $apiCatalogServiceConfiguration.hashCode());
        final Object $tlsConfiguration = this.getTlsConfiguration();
        result = result * PRIME + ($tlsConfiguration == null ? 43 : $tlsConfiguration.hashCode());
        final Object $zosmfServiceConfiguration = this.getZosmfServiceConfiguration();
        result = result * PRIME + ($zosmfServiceConfiguration == null ? 43 : $zosmfServiceConfiguration.hashCode());
        return result;
    }

    public String toString() {
        return "EnvironmentConfiguration(gatewayServiceConfiguration=" + this.getGatewayServiceConfiguration() + ", discoveryServiceConfiguration=" + this.getDiscoveryServiceConfiguration() + ", apiCatalogServiceConfiguration=" + this.getApiCatalogServiceConfiguration() + ", tlsConfiguration=" + this.getTlsConfiguration() + ", zosmfServiceConfiguration=" + this.getZosmfServiceConfiguration() + ")";
    }
}
