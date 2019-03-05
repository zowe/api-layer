/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.enabler.impl;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.CommonConstants;

import java.util.Map;

public class ApimlEurekaInstanceConfig implements EurekaInstanceConfig {
    private boolean isSecurePortEnabled;
    private boolean instanceEnabledOnit = true;
    private boolean isNonSecurePortEnabled;
    private DataCenterInfo dataCenterInfo = new DataCenterInfo() {
        @Override
        public Name getName() {
            return Name.MyOwn;
        }
    };
    private int leaseExpirationDurationInSeconds = 90;
    private int leaseRenewalIntervalInSeconds = 30;
    private int nonSecurePort;
    private int securePort;
    private Map<String, String> metadataMap;
    private String appGroupName;
    private String appname;
    private String asgName;
    private String healthCheckUrl;
    private String healthCheckUrlPath;
    private String homePageUrl;
    private String homePageUrlPath;
    private String hostName;
    private String instanceId;
    private String ipAddress;
    private String secureHealthCheckUrl;
    private String secureVirtualHostName;
    private String statusPageUrl;
    private String statusPageUrlPath;
    private String virtualHostName;
    private String[] defaultAddressResolutionOrder = new String[0];
    private String namespace = CommonConstants.DEFAULT_CONFIG_NAMESPACE;

    public ApimlEurekaInstanceConfig() {
    }

    @Override
    public boolean getSecurePortEnabled() {
        return isSecurePortEnabled;
    }

    public void setSecurePortEnabled(boolean securePortEnabled) {
        this.isSecurePortEnabled = securePortEnabled;
    }

    @Override
    public String getASGName() {
        return asgName;
    }

    @Override
    public String getHostName(boolean refresh) {
        return hostName;
    }

    public boolean isInstanceEnabledOnit() {
        return this.instanceEnabledOnit;
    }

    public void setInstanceEnabledOnit(boolean instanceEnabledOnit) {
        this.instanceEnabledOnit = instanceEnabledOnit;
    }

    public boolean isNonSecurePortEnabled() {
        return this.isNonSecurePortEnabled;
    }

    public void setNonSecurePortEnabled(boolean nonSecurePortEnabled) {
        this.isNonSecurePortEnabled = nonSecurePortEnabled;
    }

    public DataCenterInfo getDataCenterInfo() {
        return this.dataCenterInfo;
    }

    public void setDataCenterInfo(DataCenterInfo dataCenterInfo) {
        this.dataCenterInfo = dataCenterInfo;
    }

    public int getLeaseExpirationDurationInSeconds() {
        return this.leaseExpirationDurationInSeconds;
    }

    public void setLeaseExpirationDurationInSeconds(int leaseExpirationDurationInSeconds) {
        this.leaseExpirationDurationInSeconds = leaseExpirationDurationInSeconds;
    }

    public int getLeaseRenewalIntervalInSeconds() {
        return this.leaseRenewalIntervalInSeconds;
    }

    public void setLeaseRenewalIntervalInSeconds(int leaseRenewalIntervalInSeconds) {
        this.leaseRenewalIntervalInSeconds = leaseRenewalIntervalInSeconds;
    }

    public int getNonSecurePort() {
        return this.nonSecurePort;
    }

    public void setNonSecurePort(int nonSecurePort) {
        this.nonSecurePort = nonSecurePort;
    }

    public int getSecurePort() {
        return this.securePort;
    }

    public void setSecurePort(int securePort) {
        this.securePort = securePort;
    }

    public Map<String, String> getMetadataMap() {
        return this.metadataMap;
    }

    public void setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
    }

    public String getAppGroupName() {
        return this.appGroupName;
    }

    public void setAppGroupName(String appGroupName) {
        this.appGroupName = appGroupName;
    }

    public String getAppname() {
        return this.appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getHealthCheckUrl() {
        return this.healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    public String getHealthCheckUrlPath() {
        return this.healthCheckUrlPath;
    }

    public void setHealthCheckUrlPath(String healthCheckUrlPath) {
        this.healthCheckUrlPath = healthCheckUrlPath;
    }

    public String getHomePageUrl() {
        return this.homePageUrl;
    }

    public void setHomePageUrl(String homePageUrl) {
        this.homePageUrl = homePageUrl;
    }

    public String getHomePageUrlPath() {
        return this.homePageUrlPath;
    }

    public void setHomePageUrlPath(String homePageUrlPath) {
        this.homePageUrlPath = homePageUrlPath;
    }

    public String getHostName() {
        return this.hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSecureHealthCheckUrl() {
        return this.secureHealthCheckUrl;
    }

    public void setSecureHealthCheckUrl(String secureHealthCheckUrl) {
        this.secureHealthCheckUrl = secureHealthCheckUrl;
    }

    public String getSecureVirtualHostName() {
        return this.secureVirtualHostName;
    }

    public void setSecureVirtualHostName(String secureVirtualHostName) {
        this.secureVirtualHostName = secureVirtualHostName;
    }

    public String getStatusPageUrl() {
        return this.statusPageUrl;
    }

    public void setStatusPageUrl(String statusPageUrl) {
        this.statusPageUrl = statusPageUrl;
    }

    public String getStatusPageUrlPath() {
        return this.statusPageUrlPath;
    }

    public void setStatusPageUrlPath(String statusPageUrlPath) {
        this.statusPageUrlPath = statusPageUrlPath;
    }

    public String getVirtualHostName() {
        return this.virtualHostName;
    }

    public void setVirtualHostName(String virtualHostName) {
        this.virtualHostName = virtualHostName;
    }

    public String[] getDefaultAddressResolutionOrder() {
        return this.defaultAddressResolutionOrder;
    }

    public void setDefaultAddressResolutionOrder(String[] defaultAddressResolutionOrder) {
        this.defaultAddressResolutionOrder = defaultAddressResolutionOrder;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setAsgName(String asgName) {
        this.asgName = asgName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApimlEurekaInstanceConfig)) return false;
        final ApimlEurekaInstanceConfig other = (ApimlEurekaInstanceConfig) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isSecurePortEnabled != other.isSecurePortEnabled) return false;
        if (this.isInstanceEnabledOnit() != other.isInstanceEnabledOnit()) return false;
        if (this.isNonSecurePortEnabled() != other.isNonSecurePortEnabled()) return false;
        final Object this$dataCenterInfo = this.getDataCenterInfo();
        final Object other$dataCenterInfo = other.getDataCenterInfo();
        if (this$dataCenterInfo == null ? other$dataCenterInfo != null : !this$dataCenterInfo.equals(other$dataCenterInfo))
            return false;
        if (this.getLeaseExpirationDurationInSeconds() != other.getLeaseExpirationDurationInSeconds()) return false;
        if (this.getLeaseRenewalIntervalInSeconds() != other.getLeaseRenewalIntervalInSeconds()) return false;
        if (this.getNonSecurePort() != other.getNonSecurePort()) return false;
        if (this.getSecurePort() != other.getSecurePort()) return false;
        final Object this$metadataMap = this.getMetadataMap();
        final Object other$metadataMap = other.getMetadataMap();
        if (this$metadataMap == null ? other$metadataMap != null : !this$metadataMap.equals(other$metadataMap))
            return false;
        final Object this$appGroupName = this.getAppGroupName();
        final Object other$appGroupName = other.getAppGroupName();
        if (this$appGroupName == null ? other$appGroupName != null : !this$appGroupName.equals(other$appGroupName))
            return false;
        final Object this$appname = this.getAppname();
        final Object other$appname = other.getAppname();
        if (this$appname == null ? other$appname != null : !this$appname.equals(other$appname)) return false;
        final Object this$asgName = this.getASGName();
        final Object other$asgName = other.getASGName();
        if (this$asgName == null ? other$asgName != null : !this$asgName.equals(other$asgName)) return false;
        final Object this$healthCheckUrl = this.getHealthCheckUrl();
        final Object other$healthCheckUrl = other.getHealthCheckUrl();
        if (this$healthCheckUrl == null ? other$healthCheckUrl != null : !this$healthCheckUrl.equals(other$healthCheckUrl))
            return false;
        final Object this$healthCheckUrlPath = this.getHealthCheckUrlPath();
        final Object other$healthCheckUrlPath = other.getHealthCheckUrlPath();
        if (this$healthCheckUrlPath == null ? other$healthCheckUrlPath != null : !this$healthCheckUrlPath.equals(other$healthCheckUrlPath))
            return false;
        final Object this$homePageUrl = this.getHomePageUrl();
        final Object other$homePageUrl = other.getHomePageUrl();
        if (this$homePageUrl == null ? other$homePageUrl != null : !this$homePageUrl.equals(other$homePageUrl))
            return false;
        final Object this$homePageUrlPath = this.getHomePageUrlPath();
        final Object other$homePageUrlPath = other.getHomePageUrlPath();
        if (this$homePageUrlPath == null ? other$homePageUrlPath != null : !this$homePageUrlPath.equals(other$homePageUrlPath))
            return false;
        final Object this$hostName = this.getHostName();
        final Object other$hostName = other.getHostName();
        if (this$hostName == null ? other$hostName != null : !this$hostName.equals(other$hostName)) return false;
        final Object this$instanceId = this.getInstanceId();
        final Object other$instanceId = other.getInstanceId();
        if (this$instanceId == null ? other$instanceId != null : !this$instanceId.equals(other$instanceId))
            return false;
        final Object this$ipAddress = this.getIpAddress();
        final Object other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress)) return false;
        final Object this$secureHealthCheckUrl = this.getSecureHealthCheckUrl();
        final Object other$secureHealthCheckUrl = other.getSecureHealthCheckUrl();
        if (this$secureHealthCheckUrl == null ? other$secureHealthCheckUrl != null : !this$secureHealthCheckUrl.equals(other$secureHealthCheckUrl))
            return false;
        final Object this$secureVirtualHostName = this.getSecureVirtualHostName();
        final Object other$secureVirtualHostName = other.getSecureVirtualHostName();
        if (this$secureVirtualHostName == null ? other$secureVirtualHostName != null : !this$secureVirtualHostName.equals(other$secureVirtualHostName))
            return false;
        final Object this$statusPageUrl = this.getStatusPageUrl();
        final Object other$statusPageUrl = other.getStatusPageUrl();
        if (this$statusPageUrl == null ? other$statusPageUrl != null : !this$statusPageUrl.equals(other$statusPageUrl))
            return false;
        final Object this$statusPageUrlPath = this.getStatusPageUrlPath();
        final Object other$statusPageUrlPath = other.getStatusPageUrlPath();
        if (this$statusPageUrlPath == null ? other$statusPageUrlPath != null : !this$statusPageUrlPath.equals(other$statusPageUrlPath))
            return false;
        final Object this$virtualHostName = this.getVirtualHostName();
        final Object other$virtualHostName = other.getVirtualHostName();
        if (this$virtualHostName == null ? other$virtualHostName != null : !this$virtualHostName.equals(other$virtualHostName))
            return false;
        if (!java.util.Arrays.deepEquals(this.getDefaultAddressResolutionOrder(), other.getDefaultAddressResolutionOrder()))
            return false;
        final Object this$namespace = this.getNamespace();
        final Object other$namespace = other.getNamespace();
        if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApimlEurekaInstanceConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSecurePortEnabled ? 79 : 97);
        result = result * PRIME + (this.isInstanceEnabledOnit() ? 79 : 97);
        result = result * PRIME + (this.isNonSecurePortEnabled() ? 79 : 97);
        final Object $dataCenterInfo = this.getDataCenterInfo();
        result = result * PRIME + ($dataCenterInfo == null ? 43 : $dataCenterInfo.hashCode());
        result = result * PRIME + this.getLeaseExpirationDurationInSeconds();
        result = result * PRIME + this.getLeaseRenewalIntervalInSeconds();
        result = result * PRIME + this.getNonSecurePort();
        result = result * PRIME + this.getSecurePort();
        final Object $metadataMap = this.getMetadataMap();
        result = result * PRIME + ($metadataMap == null ? 43 : $metadataMap.hashCode());
        final Object $appGroupName = this.getAppGroupName();
        result = result * PRIME + ($appGroupName == null ? 43 : $appGroupName.hashCode());
        final Object $appname = this.getAppname();
        result = result * PRIME + ($appname == null ? 43 : $appname.hashCode());
        final Object $asgName = this.getASGName();
        result = result * PRIME + ($asgName == null ? 43 : $asgName.hashCode());
        final Object $healthCheckUrl = this.getHealthCheckUrl();
        result = result * PRIME + ($healthCheckUrl == null ? 43 : $healthCheckUrl.hashCode());
        final Object $healthCheckUrlPath = this.getHealthCheckUrlPath();
        result = result * PRIME + ($healthCheckUrlPath == null ? 43 : $healthCheckUrlPath.hashCode());
        final Object $homePageUrl = this.getHomePageUrl();
        result = result * PRIME + ($homePageUrl == null ? 43 : $homePageUrl.hashCode());
        final Object $homePageUrlPath = this.getHomePageUrlPath();
        result = result * PRIME + ($homePageUrlPath == null ? 43 : $homePageUrlPath.hashCode());
        final Object $hostName = this.getHostName();
        result = result * PRIME + ($hostName == null ? 43 : $hostName.hashCode());
        final Object $instanceId = this.getInstanceId();
        result = result * PRIME + ($instanceId == null ? 43 : $instanceId.hashCode());
        final Object $ipAddress = this.getIpAddress();
        result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        final Object $secureHealthCheckUrl = this.getSecureHealthCheckUrl();
        result = result * PRIME + ($secureHealthCheckUrl == null ? 43 : $secureHealthCheckUrl.hashCode());
        final Object $secureVirtualHostName = this.getSecureVirtualHostName();
        result = result * PRIME + ($secureVirtualHostName == null ? 43 : $secureVirtualHostName.hashCode());
        final Object $statusPageUrl = this.getStatusPageUrl();
        result = result * PRIME + ($statusPageUrl == null ? 43 : $statusPageUrl.hashCode());
        final Object $statusPageUrlPath = this.getStatusPageUrlPath();
        result = result * PRIME + ($statusPageUrlPath == null ? 43 : $statusPageUrlPath.hashCode());
        final Object $virtualHostName = this.getVirtualHostName();
        result = result * PRIME + ($virtualHostName == null ? 43 : $virtualHostName.hashCode());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getDefaultAddressResolutionOrder());
        final Object $namespace = this.getNamespace();
        result = result * PRIME + ($namespace == null ? 43 : $namespace.hashCode());
        return result;
    }

    public String toString() {
        return "ApimlEurekaInstanceConfig(securePortEnabled=" + this.isSecurePortEnabled
            + ", instanceEnabledOnit=" + this.instanceEnabledOnit + ", nonSecurePortEnabled="
            + this.isNonSecurePortEnabled() + ", dataCenterInfo=" + this.getDataCenterInfo()
            + ", leaseExpirationDurationInSeconds=" + this.getLeaseExpirationDurationInSeconds()
            + ", leaseRenewalIntervalInSeconds=" + this.getLeaseRenewalIntervalInSeconds() + ", nonSecurePort="
            + this.getNonSecurePort() + ", securePort=" + this.getSecurePort() + ", metadataMap=" + this.getMetadataMap()
            + ", appGroupName=" + this.getAppGroupName() + ", appname=" + this.getAppname() + ", asgName="
            + this.asgName + ", healthCheckUrl=" + this.getHealthCheckUrl() + ", healthCheckUrlPath="
            + this.getHealthCheckUrlPath() + ", homePageUrl=" + this.getHomePageUrl() + ", homePageUrlPath="
            + this.getHomePageUrlPath() + ", hostName=" + this.getHostName() + ", instanceId=" + this.getInstanceId()
            + ", ipAddress=" + this.getIpAddress() + ", secureHealthCheckUrl=" + this.getSecureHealthCheckUrl()
            + ", secureVirtualHostName=" + this.getSecureVirtualHostName() + ", statusPageUrl=" + this.getStatusPageUrl()
            + ", statusPageUrlPath=" + this.getStatusPageUrlPath() + ", virtualHostName=" + this.getVirtualHostName()
            + ", defaultAddressResolutionOrder=" + java.util.Arrays.deepToString(this.getDefaultAddressResolutionOrder())
            + ", namespace=" + this.getNamespace() + ")";
    }
}
