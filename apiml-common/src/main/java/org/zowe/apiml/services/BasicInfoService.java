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
import lombok.RequiredArgsConstructor;
import org.springframework.util.ObjectUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.registry.RegistryView;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;
import static org.zowe.apiml.services.ServiceInfo.ApiInfoExtended;
import static org.zowe.apiml.services.ServiceInfoUtils.*;

/**
 * Similar to {@link org.zowe.apiml.gateway.services.ServicesInfoService} service which does not depend on gateway-service components.
 * Following properties left blank:
 * {@link ServiceInfo.Service#homePageUrl} and {@link ServiceInfo.ApiInfoExtended#swaggerUrl}
 * <p>
 * Reads a {@link RegistryView} rather than a Eureka client. That removed a wart as well as the dependency: the
 * modulith used to subclass this class anonymously, pass {@code null} for the Eureka client and override
 * {@code getServicesInfo()} with a {@code DiscoveryClient} implementation, because the registry it needed to
 * read was in its own JVM. Both deployments now supply a view and there is one implementation.
 */
@RequiredArgsConstructor
public class BasicInfoService {

    private final RegistryView registry;
    private final EurekaMetadataParser eurekaMetadataParser;

    public List<ServiceInfo> getServicesInfo() {
        return registry.serviceIds().stream()
                .map(this::getServiceInfo)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private ServiceInfo getServiceInfo(String serviceId) {
        List<ServiceInstance> appInstances = registry.instances(serviceId);
        if (ObjectUtils.isEmpty(appInstances)) {
            return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceStatus.DOWN)
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
    public ServiceInfo.Apiml getApiml(List<ServiceInstance> appInstances) {
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
    private ServiceInfo.Service getService(List<ServiceInstance> appInstances) {
        ServiceInstance instanceInfo = getInstanceWithHighestVersion(appInstances);

        return ServiceInfo.Service.builder()
                .title(instanceInfo.metadata().get(SERVICE_TITLE))
                .description(instanceInfo.metadata().get(SERVICE_DESCRIPTION))
                .build();
    }

    /**
     * Simplified version, following properties are excluded:
     * - baseUrl
     * - swaggerUrl
     */
    private List<ServiceInfo.ApiInfoExtended> getApiInfos(List<ServiceInstance> appInstances) {
        return appInstances.stream()
                .map(instance -> new AbstractMap.SimpleEntry<>(instance, eurekaMetadataParser.parseApiInfo(instance.metadata())))
                .flatMap(entry -> entry.getValue().stream()
                        .map(apiInfo -> ApiInfoExtended.builder()
                                .apiId(apiInfo.getApiId())
                                .basePath(getBasePath(apiInfo, entry.getKey()))
                                .gatewayUrl(apiInfo.getGatewayUrl())
                                .documentationUrl(apiInfo.getDocumentationUrl())
                                .version(apiInfo.getVersion())
                                .codeSnippet(apiInfo.getCodeSnippet())
                                .isDefaultApi(apiInfo.isDefaultApi())
                                .build()))
                .collect(groupingBy(
                        apiInfo -> new AbstractMap.SimpleEntry<>(apiInfo.getApiId(), getMajorVersion(apiInfo)),
                        minBy(Comparator.comparingInt(ServiceInfoUtils::getMajorVersion))
                ))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<Authentication> getAuthentication(List<ServiceInstance> appInstances) {
        return appInstances.stream()
                .map(instance -> eurekaMetadataParser.parseAuthentication(instance.metadata()))
                .filter(a -> !a.isEmpty())
                .distinct()
            .toList();
    }

    private ServiceInstance getInstanceWithHighestVersion(List<ServiceInstance> appInstances) {
        ServiceInstance instanceInfo = appInstances.get(0);
        Version highestVersion = Version.unknownVersion();

        for (ServiceInstance currentInfo : appInstances) {
            List<ApiInfo> apiInfoList = eurekaMetadataParser.parseApiInfo(currentInfo.metadata());
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
