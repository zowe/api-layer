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
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.ObjectUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;
import org.zowe.apiml.services.ServiceInfo;
import org.zowe.apiml.services.ServiceInfoUtils;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;
import static org.zowe.apiml.services.ServiceInfoUtils.*;

@Slf4j
@RequiredArgsConstructor
public class ServicesInfoService {

    public static final String VERSION_HEADER = "Content-Version";
    public static final String CURRENT_VERSION = "1";

    private final EurekaClient eurekaClient;
    private final EurekaMetadataParser eurekaMetadataParser;
    private final GatewayClient gatewayClient;
    private final TransformService transformService;

    public List<ServiceInfo> getServicesInfo() {
        List<ServiceInfo> servicesInfo = new LinkedList<>();
        for (Application application : eurekaClient.getApplications().getRegisteredApplications()) {
            servicesInfo.add(getServiceInfo(application));
        }

        return servicesInfo;
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
        Application application = eurekaClient.getApplication(serviceId);

        if (application == null) {
            return ServiceInfo.builder()
                    .serviceId(serviceId)
                    .status(InstanceInfo.InstanceStatus.UNKNOWN)
                    .build();
        }

        return getServiceInfo(application);
    }

    private String getBaseUrl(ApiInfo apiInfo, InstanceInfo instanceInfo) {
        ServiceAddress gatewayAddress = gatewayClient.getGatewayConfigProperties();
        return String.format("%s://%s%s",
                gatewayAddress.getScheme(), gatewayAddress.getHostname(), getBasePath(apiInfo, instanceInfo));
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

    private ServiceInfo.Apiml getApiml(List<InstanceInfo> appInstances) {
        return ServiceInfo.Apiml.builder()
                .apiInfo(getApiInfos(appInstances))
                .service(getService(appInstances))
                .authentication(getAuthentication(appInstances))
                .build();
    }

    private List<ServiceInfo.ApiInfoExtended> getApiInfos(List<InstanceInfo> appInstances) {
        List<ServiceInfo.ApiInfoExtended> completeList = new ArrayList<>();

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

    private ServiceInfo.Service getService(List<InstanceInfo> appInstances) {
        InstanceInfo instanceInfo = getInstanceWithHighestVersion(appInstances);
        RoutedServices routes = eurekaMetadataParser.parseRoutes(getInstanceWithHighestVersion(appInstances).getMetadata());

        return ServiceInfo.Service.builder()
                .title(instanceInfo.getMetadata().get(SERVICE_TITLE))
                .description(instanceInfo.getMetadata().get(SERVICE_DESCRIPTION))
                .homePageUrl(getGatewayUrl(instanceInfo.getHomePageUrl(), instanceInfo.getAppName().toLowerCase(), ServiceType.UI, routes))
                .build();
    }

    private List<Authentication> getAuthentication(List<InstanceInfo> appInstances) {
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

    private InstanceInfo.InstanceStatus getStatus(List<InstanceInfo> instances) {
        if (instances.stream().anyMatch(instance -> instance.getStatus().equals(InstanceInfo.InstanceStatus.UP))) {
            return InstanceInfo.InstanceStatus.UP;
        } else {
            return InstanceInfo.InstanceStatus.DOWN;
        }
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
