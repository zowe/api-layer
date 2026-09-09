/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.services;

import com.fasterxml.jackson.core.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class containing mapping functions for ServiceInfo formatting
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceInfoUtils {

    public static Map<String, ServiceInfo.Instances> getInstances(List<ServiceInstance> appInstances) {
        return appInstances.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ServiceInstance::instanceId,
                        instance -> ServiceInfo.Instances.builder()
                                .status(instance.status())
                                .hostname(instance.hostName())
                                .ipAddr(instance.ipAddr())
                                .protocol(getProtocol(instance))
                                .port(getPort(instance))
                                .homePageUrl(instance.homePageUrl())
                                .healthCheckUrl(getHealthCheckUrl(instance))
                                .statusPageUrl(instance.statusPageUrl())
                                .customMetadata(getCustomMetadata(instance.metadata()))
                                .build()
                ));
    }

    public static String getBasePath(ApiInfo apiInfo, ServiceInstance instance) {
        return String.format("/%s/%s", instance.serviceId(), apiInfo.getGatewayUrl());
    }

    private static String getHealthCheckUrl(ServiceInstance instance) {
        return isSecure(instance) ? instance.secureHealthCheckUrl() : instance.healthCheckUrl();
    }

    private static int getPort(ServiceInstance instance) {
        return isSecure(instance) ? instance.securePort().port() : instance.port().port();
    }

    private static String getProtocol(ServiceInstance instance) {
        return isSecure(instance) ? "https" : "http";
    }

    private static boolean isSecure(ServiceInstance instance) {
        return instance.securePort() != null && instance.securePort().enabled();
    }

    public static int getMajorVersion(ServiceInfo.ApiInfoExtended apiInfo) {
        return getVersion(apiInfo.getVersion()).getMajorVersion();
    }

    public static Map<String, String> getCustomMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("apiml."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Version getVersion(String version) {
        if (version == null) return Version.unknownVersion();

        String[] versions = version.split("\\.");

        int major = 0;
        int minor = 0;
        int patch = 0;
        try {
            if (versions.length >= 1) major = Integer.parseInt(versions[0]);
            if (versions.length >= 2) minor = Integer.parseInt(versions[1]);
            if (versions.length >= 3) patch = Integer.parseInt(versions[2]);
        } catch (NumberFormatException ex) {
            log.debug("Incorrect version {}", version);
        }

        return new Version(major, minor, patch, null, null, null);
    }

    public static InstanceStatus getStatus(List<ServiceInstance> instances) {
        if (instances.stream().anyMatch(instance -> instance.effectiveStatus() == InstanceStatus.UP)) {
            return InstanceStatus.UP;
        } else {
            return InstanceStatus.DOWN;
        }
    }
}
