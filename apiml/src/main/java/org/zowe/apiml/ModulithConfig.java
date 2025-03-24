/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.product.constants.CoreService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class ModulithConfig {

    private final EurekaServerContext eurekaContext;
    private final Map<String, InstanceInfo> instances = new HashMap<>();;

    @Value("${server.ssl.enabled:true}")
    private boolean https;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.ipAddress:127.0.0.1}")
    private String ipAddress;

    @Value("${apiml.service.port:10010}")
    private int port;

    private InstanceInfo getInstanceInfo(String serviceId) {
        // TODO: Does this support HA?
        var leaseInfo = LeaseInfo.Builder.newBuilder()
            .setDurationInSecs(Integer.MAX_VALUE)
            .setRegistrationTimestamp(System.currentTimeMillis())
            .setRenewalTimestamp(System.currentTimeMillis())
            .setRenewalIntervalInSecs(Integer.MAX_VALUE)
            .setServiceUpTimestamp(System.currentTimeMillis())
            .build();

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(String.format("%s:%s:%d", hostname, serviceId, port))
            .setAppName(serviceId)
            .setHostName(hostname)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setIPAddr(ipAddress)
            .setPort(port)
            .setSecurePort(port)
            .enablePort(InstanceInfo.PortType.SECURE, https)
            .enablePort(InstanceInfo.PortType.UNSECURE, !https)
            .setVIPAddress(serviceId)
            .setDataCenterInfo(() -> DataCenterInfo.Name.MyOwn)
            .setLeaseInfo(leaseInfo)
            .setLastUpdatedTimestamp(System.currentTimeMillis())
            .build();
    }

    private ApimlInstanceRegistry getRegistry() {
        return Optional.ofNullable(EurekaServerContextHolder.getInstance())
            .map(EurekaServerContextHolder::getServerContext)
            .map(EurekaServerContext::getRegistry)
            .map(ApimlInstanceRegistry.class::cast)
            .orElse(null);
    }

    @PostConstruct
    void createLocalInstances() {
        instances.put(CoreService.GATEWAY.getServiceId(), getInstanceInfo(CoreService.GATEWAY.getServiceId()));
        instances.put(CoreService.DISCOVERY.getServiceId(), getInstanceInfo(CoreService.DISCOVERY.getServiceId()));
        EurekaServerContextHolder.initialize(eurekaContext);
    }

    @EventListener
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        ApimlInstanceRegistry registry = getRegistry();
        instances.entrySet()
            .stream()
            .forEach(entry -> registry.registerStatically(instances.get(entry.getKey()), CoreService.GATEWAY.getServiceId().equals(entry.getKey())));
    }


}
