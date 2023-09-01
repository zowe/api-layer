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
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;
import static org.zowe.apiml.services.ServiceInfoUtils.getBasePath;
import static org.zowe.apiml.services.ServiceInfoUtils.getInstances;
import static org.zowe.apiml.services.ServiceInfoUtils.getMajorVersion;
import static org.zowe.apiml.services.ServiceInfoUtils.getStatus;
import static org.zowe.apiml.services.ServiceInfoUtils.getVersion;

/**
 * Similar to {@link org.zowe.apiml.gateway.services.ServicesInfoService} service which does not depend on gateway-service components.
 * Following properties left blank:
 * {@link ServiceInfo.Service#homePageUrl} and {@link ServiceInfo.ApiInfoExtended#swaggerUrl}
 */
@Slf4j
@RequiredArgsConstructor
public class BasicInfoService {

    private final EurekaClient eurekaClient;
    private final EurekaMetadataParser eurekaMetadataParser;

    public List<ServiceInfo> getServicesInfo() {
        List<ServiceInfo> servicesInfo = new LinkedList<>();
        for (Application application : eurekaClient.getApplications().getRegisteredApplications()) {
            servicesInfo.add(getServiceInfo(application));
        }

        return servicesInfo;
    }

    private ServiceInfo getServiceInfo(Application application) {
        String serviceId = application.getName().toLowerCase();

        List<InstanceInfo> appInstances = application.getInstances();
        if (ObjectUtils.isEmpty(appInstances)) {
            return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceInfo.InstanceStatus.DOWN)
                    .build();
        }

        return ServiceInfo.builder()
                .serviceId(serviceId)
                .status(getStatus(appInstances))
                .apiml(getApiml(appInstances))
                .instances(getInstances(appInstances))
                .build();
    }

    /**
     * uses simplified:
     * - getApiInfos
     * - getApiInfos
     */
    private ServiceInfo.Apiml getApiml(List<InstanceInfo> appInstances) {
        return ServiceInfo.Apiml.builder()
                .apiInfo(getApiInfos(appInstances))
                .service(getService(appInstances))
                .authentication(getAuthentication(appInstances))
                .build();
    }


    /**
     * simplified version, following part is excluded:
     * - homePageUrl
     */
    private ServiceInfo.Service getService(List<InstanceInfo> appInstances) {
        InstanceInfo instanceInfo = getInstanceWithHighestVersion(appInstances);

        return ServiceInfo.Service.builder()
                .title(instanceInfo.getMetadata().get(SERVICE_TITLE))
                .description(instanceInfo.getMetadata().get(SERVICE_DESCRIPTION))
                .build();
    }

    /**
     * Simplified version, following properties are excluded:
     * - baseUrl
     * - swaggerUrl
     */
    private List<ServiceInfo.ApiInfoExtended> getApiInfos(List<InstanceInfo> appInstances) {
        List<ServiceInfo.ApiInfoExtended> completeList = new ArrayList<>();

        for (InstanceInfo instanceInfo : appInstances) {
            List<ApiInfo> apiInfoList = eurekaMetadataParser.parseApiInfo(instanceInfo.getMetadata());
            completeList.addAll(apiInfoList.stream()
                    .map(apiInfo -> ServiceInfo.ApiInfoExtended.builder()
                            .apiId(apiInfo.getApiId())
                            .basePath(getBasePath(apiInfo, instanceInfo))
                            .gatewayUrl(apiInfo.getGatewayUrl())
                            .documentationUrl(apiInfo.getDocumentationUrl())
                            .version(apiInfo.getVersion())
                            .codeSnippet(apiInfo.getCodeSnippet())
                            .isDefaultApi(apiInfo.isDefaultApi())
                            .build())
                    .collect(Collectors.toList()));
        }

        return completeList.stream()
                .collect(groupingBy(
                        apiInfo -> new AbstractMap.SimpleEntry<>(apiInfo.getApiId(), getMajorVersion(apiInfo)),
                        minBy(Comparator.comparingInt(ServiceInfoUtils::getMajorVersion))
                ))
                .values()
                .stream()
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<Authentication> getAuthentication(List<InstanceInfo> appInstances) {
        return appInstances.stream()
                .map(instanceInfo -> {
                    Authentication authentication = eurekaMetadataParser.parseAuthentication(instanceInfo.getMetadata());
                    return authentication.isEmpty() ? null : authentication;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private InstanceInfo getInstanceWithHighestVersion(List<InstanceInfo> appInstances) {
        InstanceInfo instanceInfo = appInstances.get(0);
        Version highestVersion = Version.unknownVersion();

        for (InstanceInfo currentInfo : appInstances) {
            List<ApiInfo> apiInfoList = eurekaMetadataParser.parseApiInfo(currentInfo.getMetadata());
            for (ApiInfo apiInfo : apiInfoList) {
                Version version = getVersion(apiInfo.getVersion());
                if (version.compareTo(highestVersion) > 0) {
                    highestVersion = version;
                    instanceInfo = currentInfo;
                }
            }
        }
        return instanceInfo;
    }
}
