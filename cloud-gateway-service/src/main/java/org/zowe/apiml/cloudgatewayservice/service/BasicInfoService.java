package org.zowe.apiml.cloudgatewayservice.service;


import com.fasterxml.jackson.core.Version;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.services.ServiceInfo;


import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;

@Slf4j
@RequiredArgsConstructor
@Service
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

    private InstanceInfo.InstanceStatus getStatus(List<InstanceInfo> instances) {
        if (instances.stream().anyMatch(instance -> instance.getStatus().equals(InstanceInfo.InstanceStatus.UP))) {
            return InstanceInfo.InstanceStatus.UP;
        } else {
            return InstanceInfo.InstanceStatus.DOWN;
        }
    }

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

    private Map<String, ServiceInfo.Instances> getInstances(List<InstanceInfo> appInstances) {
        return appInstances.stream()
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
                        minBy(Comparator.comparingInt(this::getMajorVersion))
                ))
                .values()
                .stream()
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private String getBasePath(ApiInfo apiInfo, InstanceInfo instanceInfo) {
        return String.format("/%s/%s", instanceInfo.getAppName().toLowerCase(), apiInfo.getGatewayUrl());
    }

    private String getHealthCheckUrl(InstanceInfo instanceInfo) {
        return instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ?
                instanceInfo.getSecureHealthCheckUrl() : instanceInfo.getHealthCheckUrl();
    }

    private int getPort(InstanceInfo instanceInfo) {
        return instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ?
                instanceInfo.getSecurePort() : instanceInfo.getPort();
    }

    private String getProtocol(InstanceInfo instanceInfo) {
        return instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https" : "http";
    }

    private int getMajorVersion(ServiceInfo.ApiInfoExtended apiInfo) {
        return getVersion(apiInfo.getVersion()).getMajorVersion();
    }

    private Map<String, String> getCustomMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("apiml."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Version getVersion(String version) {
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


}
