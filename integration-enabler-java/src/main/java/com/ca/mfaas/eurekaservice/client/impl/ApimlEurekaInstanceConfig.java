/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.impl;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.CommonConstants;
import lombok.Data;

import java.util.Map;

@Data
public class ApimlEurekaInstanceConfig implements EurekaInstanceConfig {
    private boolean securePortEnabled;
    private boolean instanceEnabledOnit = true;
    private boolean nonSecurePortEnabled;
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

    @Override
    public boolean getSecurePortEnabled() {
        return securePortEnabled;
    }

    @Override
    public String getASGName() {
        return asgName;
    }

    @Override
    public String getHostName(boolean refresh) {
        return hostName;
    }
}
