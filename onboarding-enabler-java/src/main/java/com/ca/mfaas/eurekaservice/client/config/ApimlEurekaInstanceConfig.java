/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.config;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.CommonConstants;

import lombok.Data;

import java.util.Map;

/**
 * An inplementation class of {@link EurekaInstanceConfig} interface.
 * Has members corresponding with the properties required by Eureka for registering a REST service.
 */
@Data
public class ApimlEurekaInstanceConfig implements EurekaInstanceConfig {
    private boolean securePortEnabled;
    private boolean instanceEnabledOnit = true;
    private boolean nonSecurePortEnabled;
    private DataCenterInfo dataCenterInfo = () -> DataCenterInfo.Name.MyOwn;
    private int leaseExpirationDurationInSeconds = 90;
    private int leaseRenewalIntervalInSeconds = 30;
    private int nonSecurePort;
    private int securePort;
    private Map<String, String> metadataMap;
    private String appGroupName;
    private String appname;
    private String aSGName;
    private String healthCheckUrl;
    private String healthCheckUrlPath;
    private String homePageUrl;
    private String homePageUrlPath;
    private String hostName;
    private String instanceId;

    /**
     * According to Netflix: "This information is for academic
     * purposes only as the communication from other instances primarily happen
     * using the information supplied in {@link #getHostName(boolean)}.
     *
     * We keep the field here, because the method {@link EurekaInstanceConfig#getIpAddress()} is part of EurekaInstanceConfig interface.
     */
    private String ipAddress;
    private String secureHealthCheckUrl;
    private String secureVirtualHostName;
    private String statusPageUrl;
    private String statusPageUrlPath;
    private String virtualHostName;
    private String[] defaultAddressResolutionOrder = new String[0];
    private String namespace = CommonConstants.DEFAULT_CONFIG_NAMESPACE;

    /**
     * Implementation of getSecurePortEnabled because the interface doesn't specify isSecurePortEnabled
     * @return
     */
    @Override
    public boolean getSecurePortEnabled() {
        return securePortEnabled;
    }

    /**
     * Special "get" method accepting boolean parameter for triggering "refresh". Not actually used in our implementation
     * @param refresh
     * @return
     */
    @Override
    public String getHostName(boolean refresh) {
        return hostName;
    }
}
