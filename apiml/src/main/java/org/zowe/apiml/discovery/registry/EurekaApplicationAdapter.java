/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zowe.apiml.registry.ServiceRegistry;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * Presents registry contents as Netflix {@code Application} objects.
 * <p>
 * <b>Temporary.</b> JWT invalidation is broadcast to the other Gateway instances, and
 * {@code AuthenticationService} in zaas-service takes a {@code com.netflix.discovery.shared.Application} to find
 * them. That class belongs to a module that is still a Eureka client, so changing its signature is Phase 5 work,
 * not this phase's. This adapter keeps those two call sites working unchanged in the meantime.
 * <p>
 * Note which Netflix artefact this needs: {@code Application} and {@code InstanceInfo} live in eureka-<em>client</em>,
 * which is still present. The server half - eureka-core and the Spring Cloud server starter - is gone. Delete this
 * class together with the zaas-service signature change.
 */
@Component
@RequiredArgsConstructor
public class EurekaApplicationAdapter {

    private final ServiceRegistry registry;

    /** The given service as a Netflix {@code Application}, or null when nothing is registered under that id. */
    public Application application(String serviceId) {
        return registry.application(serviceId)
            .map(application -> {
                Application result = new Application(application.name());
                application.instances().forEach(instance -> result.addInstance(toInstanceInfo(instance)));
                return result;
            })
            .orElse(null);
    }

    private InstanceInfo toInstanceInfo(ServiceInstance instance) {
        var lease = instance.lease();
        var leaseInfo = LeaseInfo.Builder.newBuilder()
            .setRenewalIntervalInSecs(lease == null ? 30 : lease.renewalIntervalSecs())
            .setDurationInSecs(lease == null ? 90 : lease.durationSecs())
            .setRegistrationTimestamp(lease == null ? 0 : lease.registrationTimestamp())
            .setRenewalTimestamp(lease == null ? 0 : lease.lastRenewalTimestamp())
            .setEvictionTimestamp(lease == null ? 0 : lease.evictionTimestamp())
            .setServiceUpTimestamp(lease == null ? 0 : lease.serviceUpTimestamp())
            .build();

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(instance.instanceId())
            .setAppName(instance.appName())
            .setAppGroupName(instance.appGroupName())
            .setHostName(instance.hostName())
            .setIPAddr(instance.ipAddr())
            .setPort(instance.port().port())
            .setSecurePort(instance.securePort().port())
            .enablePort(InstanceInfo.PortType.UNSECURE, instance.port().enabled())
            .enablePort(InstanceInfo.PortType.SECURE, instance.securePort().enabled())
            .setVIPAddress(instance.vipAddress())
            .setSecureVIPAddress(instance.secureVipAddress())
            .setHomePageUrl(null, instance.homePageUrl())
            .setStatusPageUrl(null, instance.statusPageUrl())
            .setHealthCheckUrls(null, instance.healthCheckUrl(), instance.secureHealthCheckUrl())
            .setStatus(InstanceInfo.InstanceStatus.toEnum(instance.status().name()))
            .setOverriddenStatus(InstanceInfo.InstanceStatus.toEnum(instance.overriddenStatus().name()))
            .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
            .setLeaseInfo(leaseInfo)
            .setMetadata(new java.util.HashMap<>(instance.metadata()))
            .setLastUpdatedTimestamp(instance.lastUpdatedTimestamp())
            .setLastDirtyTimestamp(instance.lastDirtyTimestamp())
            .build();
    }

}
