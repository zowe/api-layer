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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
import org.zowe.apiml.registry.RegistryView;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;
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

    /**
     * The registry in its own terms, not through Spring Cloud's {@code DiscoveryClient}.
     * <p>
     * This class reports on the state of the mediation layer, so it needs the whole registration - IP address,
     * status page, health-check URL, effective status - and it needs to see instances that are <em>not</em> up.
     * Every one of those used to come from casting Spring's {@code ServiceInstance} to
     * {@code EurekaServiceInstance} and unwrapping the {@code InstanceInfo} inside; there were five such casts
     * in this file, and each silently dropped any instance that came from a different discovery client.
     */
    private final RegistryView registry;
    private final EurekaMetadataParser eurekaMetadataParser;
    private final GatewayClient gatewayClient;
    private final TransformService transformService;

    public List<ServiceInfo> getServicesInfo() {
        return registry.serviceIds()
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
        var knownServices = registry.serviceIds();
        if (knownServices.stream().anyMatch(id -> id.equalsIgnoreCase(serviceId))) {
            return getServiceInfo(serviceId, registry.instances(serviceId));
        }
        return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceStatus.UNKNOWN)
                    .build();
    }

    private String getBaseUrl(ApiInfo apiInfo, ServiceInstance instanceInfo) {
        ServiceAddress gatewayAddress = gatewayClient.getGatewayConfigProperties();
        return String.format("%s://%s%s",
                gatewayAddress.getScheme(), gatewayAddress.getHostname(), getBasePath(apiInfo, instanceInfo));
    }

    /**
     * Only the primary registrations.
     * <p>
     * A Gateway that has joined several API MLs registers into each of them, and the extra registrations are
     * marked {@code additional}. Reporting them here would list the same Gateway several times.
     */
    static List<ServiceInstance> getPrimaryInstances(List<ServiceInstance> serviceInstances) {
        return serviceInstances.stream()
            .filter(instance -> EurekaMetadataDefinition.RegistrationType.of(instance.metadata()).isPrimary())
            .toList();
    }

    private ServiceInfo getServiceInfo(String serviceId, List<ServiceInstance> serviceInstances) {
        serviceId = serviceInstances.stream().findFirst().map(ServiceInstance::serviceId).orElse(serviceId);
        var primaryInstances = getPrimaryInstances(serviceInstances);
        if (primaryInstances.isEmpty()) {
            return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceStatus.DOWN)
                    .build();
        }

        return ServiceInfo.builder()
                .serviceId(serviceId)
                .status(getStatus(primaryInstances))
                .apiml(getApiml(primaryInstances))
                .instances(getInstances(primaryInstances))
                .build();
    }

    private ServiceInfo.Apiml getApiml(List<ServiceInstance> serviceInstances) {
        return ServiceInfo.Apiml.builder()
                .apiInfo(getApiInfos(serviceInstances))
                .service(getService(serviceInstances))
                .authentication(getAuthentication(serviceInstances))
                .build();
    }

    private List<ServiceInfo.ApiInfoExtended> getApiInfos(List<ServiceInstance> serviceInstances) {
        List<ServiceInfo.ApiInfoExtended> completeList = new ArrayList<>();

        for (ServiceInstance instanceInfo : serviceInstances) {
            List<ApiInfo> apiInfoList = eurekaMetadataParser.parseApiInfo(instanceInfo.metadata());
            completeList.addAll(apiInfoList.stream()
                    .map(apiInfo -> ServiceInfo.ApiInfoExtended.builder()
                            .apiId(apiInfo.getApiId())
                            .basePath(getBasePath(apiInfo, instanceInfo))
                            .baseUrl(getBaseUrl(apiInfo, instanceInfo))
                            .gatewayUrl(apiInfo.getGatewayUrl())
                            .swaggerUrl(getGatewayUrl(
                                    apiInfo.getSwaggerUrl(),
                                    instanceInfo.serviceId(),
                                    ServiceType.API,
                                    eurekaMetadataParser.parseRoutes(instanceInfo.metadata())
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
        ServiceInstance instanceInfo = getInstanceWithHighestVersion(serviceInstances);
        RoutedServices routes = eurekaMetadataParser.parseRoutes(instanceInfo.metadata());

        return ServiceInfo.Service.builder()
                .title(instanceInfo.metadata().get(SERVICE_TITLE))
                .description(instanceInfo.metadata().get(SERVICE_DESCRIPTION))
                .homePageUrl(getGatewayUrl(instanceInfo.homePageUrl(), instanceInfo.serviceId(), ServiceType.UI, routes))
                .build();
    }

    private List<Authentication> getAuthentication(List<ServiceInstance> serviceInstances) {
        return serviceInstances.stream()
                .map(instanceInfo -> {
                    Authentication authentication = eurekaMetadataParser.parseAuthentication(instanceInfo.metadata());
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

    private InstanceStatus getStatus(List<ServiceInstance> instances) {
        if (instances.stream().anyMatch(instance -> instance.effectiveStatus() == InstanceStatus.UP)) {
            return InstanceStatus.UP;
        } else if (instances.isEmpty()) {
            return InstanceStatus.UNKNOWN;
        }
        return InstanceStatus.DOWN;
    }

    private ServiceInstance getInstanceWithHighestVersion(List<ServiceInstance> serviceInstances) {
        ServiceInstance instanceInfo = serviceInstances.get(0);
        Version highestVersion = Version.unknownVersion();

        for (ServiceInstance currentInfo : serviceInstances) {
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
