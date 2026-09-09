/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * Builds this service's own registration from its configuration.
 * <p>
 * The replacement for Netflix's {@code InstanceInfoFactory}, and for the copy of it APIML kept in the
 * Gateway's {@code ConnectionsConfig} so that the Gateway could register under its external URL. Both are
 * here, and the external-URL case is just a different host and port passed in.
 */
public final class SelfInstanceFactory {

    private SelfInstanceFactory() {
    }

    public static ServiceInstance create(RegistryInstanceProperties config, long now) {
        String hostname = config.resolvedHostname();
        int port = config.getNonSecurePort();
        int securePort = config.getSecurePort();

        // Netflix registered a new instance as STARTING unless instanceEnabledOnit was set, so that peers and
        // the routing table learn about it before it can receive traffic. The lifecycle promotes it to UP once
        // the application reports healthy.
        InstanceStatus initialStatus = config.isInstanceEnabledOnit()
            ? InstanceStatus.UP
            : InstanceStatus.STARTING;

        String instanceId = config.getInstanceId() != null
            ? config.getInstanceId()
            : hostname + ":" + config.getAppname() + ":" + config.advertisedPort();

        return ServiceInstance.builder()
            .instanceId(instanceId)
            .appName(config.getAppname())
            .appGroupName(config.getAppGroupName())
            .hostName(hostname)
            .ipAddr(config.getIpAddress())
            .port(new PortInfo(port, config.isNonSecurePortEnabled()))
            .securePort(new PortInfo(securePort, config.isSecurePortEnabled()))
            .homePageUrl(config.resolvedHomePageUrl())
            .statusPageUrl(config.resolvedStatusPageUrl())
            .healthCheckUrl(config.resolvedHealthCheckUrl())
            .secureHealthCheckUrl(config.resolvedSecureHealthCheckUrl())
            .vipAddress(config.getVirtualHostName())
            .secureVipAddress(config.getSecureVirtualHostName())
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .status(initialStatus)
            .lease(Lease.renewable(
                config.getLeaseRenewalIntervalInSeconds(),
                config.getLeaseExpirationDurationInSeconds(),
                now))
            .metadata(config.getMetadataMap())
            .lastUpdatedTimestamp(now)
            .lastDirtyTimestamp(now)
            .build();
    }

}
