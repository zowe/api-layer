/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.services;

import com.fasterxml.jackson.core.Version;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;
import org.zowe.apiml.services.ServiceInfo;
import org.zowe.apiml.services.ServiceInfoUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;
import static org.zowe.apiml.services.ServiceInfoUtils.getBasePath;
import static org.zowe.apiml.services.ServiceInfoUtils.getInstances;
import static org.zowe.apiml.services.ServiceInfoUtils.getMajorVersion;
import static org.zowe.apiml.services.ServiceInfoUtils.getVersion;

@RequiredArgsConstructor
public class ServicesInfoService {

    public static final String VERSION_HEADER = "Content-Version";
    public static final String CURRENT_VERSION = "1";

    private final DiscoveryClient discoveryClient;
    private final EurekaMetadataParser eurekaMetadataParser;
    private final GatewayClient gatewayClient;
    private final TransformService transformService;

    public List<ServiceInfo> getServicesInfo() {
        return discoveryClient.getServices()
            .stream()
            .map(this::getServiceInfo)
            .toList();
    }

    public List<ServiceInfo> getServicesInfo(String apiId) {
        List<ServiceInfo> servicesInfo = getServicesInfo();

        if (apiId == null) return servicesInfo;

        return servicesInfo.stream()
                .filter(serviceInfo -> {
                    if (serviceInfo.getApiml() == null || serviceInfo.getApiml().getApiInfo() == null) return false;
                    return serviceInfo.getApiml().getApiInfo().stream().anyMatch(apiInfo ->
                            StringUtils.equals(apiInfo.getApiId(), apiId));
                })
                .toList();
    }

    public ServiceInfo getServiceInfo(String serviceId) {
        var knownServices = discoveryClient.getServices();
        if (knownServices.stream().anyMatch(id -> id.equalsIgnoreCase(serviceId))) {
            return getServiceInfo(serviceId, discoveryClient.getInstances(serviceId));
        }
        return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceInfo.InstanceStatus.UNKNOWN)
                    .build();
    }

    private String getBaseUrl(ApiInfo apiInfo, InstanceInfo instanceInfo) {
        ServiceAddress gatewayAddress = gatewayClient.getGatewayConfigProperties();
        return String.format("%s://%s%s",
                gatewayAddress.getScheme(), gatewayAddress.getHostname(), getBasePath(apiInfo, instanceInfo));
    }

    static List<InstanceInfo> getPrimaryInstances(Application application) {
        return application.getInstances()
            .stream()
            .filter(instanceInfo -> EurekaMetadataDefinition.RegistrationType.of(instanceInfo.getMetadata()).isPrimary())
            .toList();
    }

    static List<ServiceInstance> getPrimaryInstances(List<ServiceInstance> serviceInstances) {
        return serviceInstances.stream()
            .filter(serviceInstance -> EurekaMetadataDefinition.RegistrationType.of(serviceInstance.getMetadata()).isPrimary())
            .toList();
    }

    private ServiceInfo getServiceInfo(String serviceId, List<ServiceInstance> serviceInstances) {
        serviceId = serviceInstances.stream().findFirst().map(ServiceInstance::getServiceId).map(String::toLowerCase).orElse(serviceId);
        var primaryInstances = getPrimaryInstances(serviceInstances);
        if (primaryInstances.isEmpty()) {
            return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceInfo.InstanceStatus.DOWN)
                    .build();
        }

        return ServiceInfo.builder()
                .serviceId(serviceId)
                .status(getStatus(primaryInstances))
                .apiml(getApiml(primaryInstances))
                .instances(
                    getInstances(
                        primaryInstances.stream()
                            .filter(EurekaServiceInstance.class::isInstance)
                            .map(EurekaServiceInstance.class::cast)
                            .map(EurekaServiceInstance::getInstanceInfo)
                            .toList()
                        )
                    )
                .build();
    }

    private ServiceInfo.Apiml getApiml(List<ServiceInstance> serviceInstances) {
        return ServiceInfo.Apiml.builder()
                .apiInfo(getApiInfos(serviceInstances))
                .service(getService(serviceInstances))
                .authentication(getAuthentication(serviceInstances))
                .build();
    }

    private List<InstanceInfo> extractInstanceInfo(List<ServiceInstance> serviceInstances) {
        return serviceInstances.stream()
        .filter(EurekaServiceInstance.class::isInstance)
        .map(EurekaServiceInstance.class::cast)
        .map(EurekaServiceInstance::getInstanceInfo)
        .toList();
    }

    private List<ServiceInfo.ApiInfoExtended> getApiInfos(List<ServiceInstance> serviceInstances) {
        List<ServiceInfo.ApiInfoExtended> completeList = new ArrayList<>();
        var appInstances = extractInstanceInfo(serviceInstances);

        for (InstanceInfo instanceInfo : appInstances) {
            List<ApiInfo> apiInfoList = eurekaMetadataParser.parseApiInfo(instanceInfo.getMetadata());
            completeList.addAll(apiInfoList.stream()
                    .map(apiInfo -> ServiceInfo.ApiInfoExtended.builder()
                            .apiId(apiInfo.getApiId())
                            .basePath(getBasePath(apiInfo, instanceInfo))
                            .baseUrl(getBaseUrl(apiInfo, instanceInfo))
                            .gatewayUrl(apiInfo.getGatewayUrl())
                            .swaggerUrl(getGatewayUrl(
                                    apiInfo.getSwaggerUrl(),
                                    instanceInfo.getAppName().toLowerCase(),
                                    ServiceType.API,
                                    eurekaMetadataParser.parseRoutes(instanceInfo.getMetadata())
                            ))
                            .documentationUrl(apiInfo.getDocumentationUrl())
                            .version(apiInfo.getVersion())
                            .codeSnippet(apiInfo.getCodeSnippet())
                            .isDefaultApi(apiInfo.isDefaultApi())
                            .build())
                    .toList());
        }

        return completeList.stream()
                .collect(groupingBy(
                        apiInfo -> new AbstractMap.SimpleEntry<>(apiInfo.getApiId(), getMajorVersion(apiInfo)),
                        minBy(Comparator.comparingInt(ServiceInfoUtils::getMajorVersion))
                ))
                .values()
                .stream()
                .map(Optional::get)
                .toList();
    }

    private ServiceInfo.Service getService(List<ServiceInstance> serviceInstances) {
        InstanceInfo instanceInfo = getInstanceWithHighestVersion(serviceInstances);
        RoutedServices routes = eurekaMetadataParser.parseRoutes(getInstanceWithHighestVersion(serviceInstances).getMetadata());

        return ServiceInfo.Service.builder()
                .title(instanceInfo.getMetadata().get(SERVICE_TITLE))
                .description(instanceInfo.getMetadata().get(SERVICE_DESCRIPTION))
                .homePageUrl(getGatewayUrl(instanceInfo.getHomePageUrl(), instanceInfo.getAppName().toLowerCase(), ServiceType.UI, routes))
                .build();
    }

    private List<Authentication> getAuthentication(List<ServiceInstance> serviceInstances) {
        var appInstances = extractInstanceInfo(serviceInstances);
        return appInstances.stream()
                .map(instanceInfo -> {
                    Authentication authentication = eurekaMetadataParser.parseAuthentication(instanceInfo.getMetadata());
                    return authentication.isEmpty() ? null : authentication;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String getGatewayUrl(String url, String serviceId, ServiceType type, RoutedServices routes) {
        if (url == null) return null;

        try {
            return transformService.transformURL(
                    type,
                    serviceId,
                    url,
                    routes,
                    false);
        } catch (URLTransformationException e) {
            return url;
        }
    }

    private InstanceInfo.InstanceStatus getStatus(List<ServiceInstance> instances) {
        if (instances.stream()
            .filter(EurekaServiceInstance.class::isInstance)
            .map(EurekaServiceInstance.class::cast)
            .anyMatch(instance ->  instance.getInstanceInfo().getStatus().equals(InstanceInfo.InstanceStatus.UP))) {
            return InstanceInfo.InstanceStatus.UP;
        } else if (instances.isEmpty()) {
            return InstanceInfo.InstanceStatus.UNKNOWN;
        }
        return InstanceInfo.InstanceStatus.DOWN;
    }

    private InstanceInfo getInstanceWithHighestVersion(List<ServiceInstance> serviceInstances) {
        var appInstances = extractInstanceInfo(serviceInstances);
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
