/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.model.CustomStyleConfig;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationSchemes;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;
import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

/**
 * Initialize the API catalog with the running instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerService {

    private static final String DEFAULT_APIINFO_KEY = "default";

    private final AuthenticationSchemes schemes = new AuthenticationSchemes();
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    private final EurekaClient eurekaClient;
    private final TransformService transformService;
    private final CustomStyleConfig customStyleConfig;

    @Value("${apiml.catalog.hide.serviceInfo:false}")
    private boolean hideServiceInfo;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    private Set<String> getProductIds() {
        return Optional.ofNullable(eurekaClient.getApplications())
            .map(Applications::getRegisteredApplications)
            .map(List::stream).orElse(Stream.empty())
            .map(Application::getInstances)
            .flatMap(Collection::stream)
            .map(InstanceInfo::getMetadata)
            .map(metadata -> metadata.get(CATALOG_ID))
            .collect(Collectors.toSet());
    }

    /**
     * Return all cached service instances
     *
     * @return instances
     */
    public Collection<APIContainer> getAllContainers() {
        return getProductIds().stream()
            .map(this::getContainerById)
            .toList();
    }

    private boolean isSso(InstanceInfo instanceInfo) {
        Map<String, String> eurekaMetadata = instanceInfo.getMetadata();
        return Authentication.builder()
            .scheme(schemes.map(eurekaMetadata.get(AUTHENTICATION_SCHEME)))
            .supportsSso(BooleanUtils.toBooleanObject(eurekaMetadata.get(AUTHENTICATION_SSO)))
            .build()
            .supportsSso();
    }

    private boolean hasHomePage(InstanceInfo instanceInfo) {
        String instanceHomePage = instanceInfo.getHomePageUrl();
        return instanceHomePage != null
            && !instanceHomePage.isEmpty();
    }

    /**
     * Try to transform the service homepage url and return it. If it fails,
     * return the original homepage url
     *
     * @param instanceInfo the service instance
     * @return the transformed homepage url
     */
    private String getInstanceHomePageUrl(InstanceInfo instanceInfo) {
        String instanceHomePage = instanceInfo.getHomePageUrl();

        //Gateway homePage is used to hold DVIPA address and must not be modified
        if (hasHomePage(instanceInfo) && !StringUtils.equalsIgnoreCase(GATEWAY.getServiceId(), instanceInfo.getAppName())) {
            instanceHomePage = instanceHomePage.trim();
            RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());
            try {
                instanceHomePage = transformService.transformURL(
                    ServiceType.UI,
                    instanceInfo.getVIPAddress(),
                    instanceHomePage,
                    routes,
                    isAttlsEnabled);
            } catch (URLTransformationException | IllegalArgumentException e) {
                apimlLog.log("org.zowe.apiml.apicatalog.homePageTransformFailed", instanceInfo.getAppName(), e.getMessage());
            }
        }

        log.debug("Homepage URL for {} service is: {}", instanceInfo.getVIPAddress(), instanceHomePage);
        return instanceHomePage;
    }

    /**
     * Get the base path for the service.
     *
     * @param instanceInfo the service instance
     * @return the base URL
     */
    private String getApiBasePath(InstanceInfo instanceInfo) {
        if (hasHomePage(instanceInfo)) {
            try {
                String apiBasePath = instanceInfo.getMetadata().get("apiml.apiBasePath");
                if (apiBasePath != null) {
                    return apiBasePath;
                }

                RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());
                return transformService.retrieveApiBasePath(
                    instanceInfo.getVIPAddress(),
                    instanceInfo.getHomePageUrl(),
                    routes);
            } catch (URLTransformationException e) {
                apimlLog.log("org.zowe.apiml.apicatalog.getApiBasePathFailed", instanceInfo.getAppName(), e.getMessage());
            }
        }
        return "";
    }

    /**
     * Create a APIService object using the instances metadata
     *
     * @param instanceInfo the service instance
     * @return a APIService object
     */
    APIService createAPIServiceFromInstance(InstanceInfo instanceInfo) {
        boolean secureEnabled = instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE);

        String instanceHomePage = getInstanceHomePageUrl(instanceInfo);
        String apiBasePath = getApiBasePath(instanceInfo);
        Map<String, ApiInfo> apiInfoById = new HashMap<>();

        try {
            List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
            apiInfoList.stream().filter(apiInfo -> apiInfo.getApiId() != null).forEach(apiInfo -> {
                String id = (apiInfo.getMajorVersion() < 0) ? DEFAULT_APIINFO_KEY : apiInfo.getApiId() + " v" + apiInfo.getVersion();
                apiInfoById.put(id, apiInfo);
            });
            if (!apiInfoById.containsKey(DEFAULT_APIINFO_KEY)) {
                ApiInfo defaultApiInfo = apiInfoList.stream().filter(ApiInfo::isDefaultApi).findFirst().orElse(null);
                apiInfoById.put(DEFAULT_APIINFO_KEY, defaultApiInfo);
            }
        } catch (Exception ex) {
            log.info("createApiServiceFromInstance#incorrectVersions {}", ex.getMessage());
        }

        String serviceId = instanceInfo.getAppName();
        String title = instanceInfo.getMetadata().get(SERVICE_TITLE);
        if (StringUtils.equalsIgnoreCase(GATEWAY.getServiceId(), serviceId)) {
            if (RegistrationType.of(instanceInfo.getMetadata()).isAdditional()) {
                // additional registration for GW means domain one, update serviceId and basePath with the ApimlId
                String apimlId = instanceInfo.getMetadata().get(APIML_ID);
                if (apimlId != null) {
                    serviceId = apimlId;
                    apiBasePath = String.join("/", "", serviceId.toLowerCase());
                    title += " (" + apimlId + ")";
                }
            } else {
                apiBasePath = "/";
            }
        }

        return new APIService.Builder(StringUtils.lowerCase(serviceId))
            .title(title)
            .description(instanceInfo.getMetadata().get(SERVICE_DESCRIPTION))
            .tileDescription(instanceInfo.getMetadata().get(CATALOG_DESCRIPTION))
            .secured(secureEnabled)
            .baseUrl(instanceInfo.getHomePageUrl())
            .homePageUrl(instanceHomePage)
            .basePath(apiBasePath)
            .sso(isSso(instanceInfo))
            .apis(apiInfoById)
            .instanceId(instanceInfo.getInstanceId())
            .build();
    }

    /**
     * Create a new container based on information in a new instance
     *
     * @param productFamilyId parent id
     * @param instanceInfos   all instances
     * @return a new container
     */
    private APIContainer createNewContainerFromService(String productFamilyId, InstanceInfo...instanceInfos) {
        if (instanceInfos.length == 0) {
            return null;
        }

        Map<String, String> instanceInfoMetadata = instanceInfos[0].getMetadata();
        String title = instanceInfoMetadata.get(CATALOG_TITLE);
        String description = instanceInfoMetadata.get(CATALOG_DESCRIPTION);
        String version = instanceInfoMetadata.get(CATALOG_VERSION);
        APIContainer container = new APIContainer();
        container.setStatus("UP");
        container.setId(productFamilyId);
        container.setDescription(description);
        container.setTitle(title);
        container.setVersion(version);
        log.debug("updated Container cache with product family: " + productFamilyId + ": " + title);

        // create API Service from instance and update container last changed date
        for (InstanceInfo instanceInfo : instanceInfos) {
            container.addService(createAPIServiceFromInstance(instanceInfo));
        }
        return container;
    }

    private boolean update(APIService apiService) {
        Application application = eurekaClient.getApplication(apiService.getServiceId());
        // service has not cached yet, but count as alive
        if (application == null) return true;

        List<InstanceInfo> instancies = application.getInstances();
        boolean isUp = instancies.stream().anyMatch(i -> InstanceInfo.InstanceStatus.UP.equals(i.getStatus()));
        boolean isSso = instancies.stream().allMatch(this::isSso);

        apiService.setStatus(isUp ? "UP" : "DOWN");
        apiService.setSsoAllInstances(isSso);

        return isUp;
    }

    private void setStatus(APIContainer apiContainer, int servicesCount, int activeServicesCount) {
        apiContainer.setTotalServices(servicesCount);
        apiContainer.setActiveServices(activeServicesCount);

        if (activeServicesCount == 0) {
            apiContainer.setStatus("DOWN");
        } else if (activeServicesCount == servicesCount) {
            apiContainer.setStatus("UP");
        } else {
            apiContainer.setStatus("WARNING");
        }
    }

    /**
     * Map the configuration to customize the Catalog UI to the container
     *
     * @param apiContainer
     */
    private void setCustomUiConfig(APIContainer apiContainer) {
        apiContainer.setCustomStyleConfig(customStyleConfig);
    }


    /**
     * Update the summary totals, sso and API IDs info for a container based on it's running services
     *
     * @param apiContainer calculate totals for this container
     */
    public void calculateContainerServiceValues(APIContainer apiContainer) {
        if (apiContainer.getServices() == null) {
            apiContainer.setServices(new HashSet<>());
        }

        int servicesCount = apiContainer.getServices().size();
        int activeServicesCount = 0;
        boolean isSso = servicesCount > 0;
        for (APIService apiService : apiContainer.getServices()) {
            if (update(apiService)) {
                activeServicesCount++;
            }
            isSso &= apiService.isSsoAllInstances();
        }

        setStatus(apiContainer, servicesCount, activeServicesCount);
        apiContainer.setSso(isSso);
        apiContainer.setHideServiceInfo(hideServiceInfo);

        // set metadata to customize the UI
        if (customStyleConfig != null) {
            setCustomUiConfig(apiContainer);
        }

    }

    /**
     * return cached service instance by id
     *
     * @param id service identifier
     * @return {@link APIContainer}
     */
    public APIContainer getContainerById(String id) {
        List<InstanceInfo> instances = Optional.ofNullable(eurekaClient.getApplications())
            .map(Applications::getRegisteredApplications)
            .map(List::stream).orElse(Stream.empty())
            .map(Application::getInstances)
            .flatMap(Collection::stream)
            .filter(instance -> StringUtils.equals(id, instance.getMetadata().get(CATALOG_ID)))
            .toList();

        if (instances.isEmpty()) {
            return null;
        }

        var container = createNewContainerFromService(id, instances.toArray(new InstanceInfo[0]));
        calculateContainerServiceValues(container);
        return container;
    }

    public APIService getService(String serviceId) {
        return Optional.ofNullable(eurekaClient.getApplication(serviceId))
            .map(Application::getInstances)
            .filter(applications -> !applications.isEmpty())
            .map(applications -> applications.get(0))
            .map(this::createAPIServiceFromInstance)
            .orElse(null);
    }

}
