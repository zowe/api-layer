/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import com.fasterxml.jackson.core.Version;
import com.netflix.appinfo.InstanceInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.config.ApiInfo;

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

    public static Map<String, ServiceInfo.Instances> getInstances(List<InstanceInfo> appInstances) {
        return appInstances.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        InstanceInfo::getInstanceId,
                        instanceInfo -> ServiceInfo.Instances.builder()
                                .status(instanceInfo.getStatus())
                                .hostname(instanceInfo.getHostName())
                                .ipAddr(instanceInfo.getIPAddr())
                                .protocol(getProtocol(instanceInfo))
                                .port(getPort(instanceInfo))
                                .homePageUrl(instanceInfo.getHomePageUrl())
                                .healthCheckUrl(getHealthCheckUrl(instanceInfo))
                                .statusPageUrl(instanceInfo.getStatusPageUrl())
                                .customMetadata(getCustomMetadata(instanceInfo.getMetadata()))
                                .build()
                ));
    }

    public static String getBasePath(ApiInfo apiInfo, InstanceInfo instanceInfo) {
        return String.format("/%s/%s", instanceInfo.getAppName().toLowerCase(), apiInfo.getGatewayUrl());
    }

    private static String getHealthCheckUrl(InstanceInfo instanceInfo) {
        return instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ?
                instanceInfo.getSecureHealthCheckUrl() : instanceInfo.getHealthCheckUrl();
    }

    private static int getPort(InstanceInfo instanceInfo) {
        return instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ?
                instanceInfo.getSecurePort() : instanceInfo.getPort();
    }

    private static String getProtocol(InstanceInfo instanceInfo) {
        return instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https" : "http";
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

    public static InstanceInfo.InstanceStatus getStatus(List<InstanceInfo> instances) {
        if (instances.stream().anyMatch(instance -> instance.getStatus().equals(InstanceInfo.InstanceStatus.UP))) {
            return InstanceInfo.InstanceStatus.UP;
        } else {
            return InstanceInfo.InstanceStatus.DOWN;
        }
    }
}
