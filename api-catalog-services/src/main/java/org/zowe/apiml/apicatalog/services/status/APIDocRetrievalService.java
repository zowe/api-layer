/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.status;

import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.apicatalog.instance.InstanceRetrievalService;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionNotFoundException;
import org.zowe.apiml.apicatalog.swagger.SubstituteSwaggerGenerator;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retrieves the API documentation for a registered service
 */
@Service
@RequiredArgsConstructor
public class APIDocRetrievalService {

    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;
    private final GatewayClient gatewayClient;

    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    /**
     * Retrieves the available API versions for a registered service.
     * Takes the versions available in each 'apiml.service.apiInfo' element.
     *
     * @param serviceId the unique service ID
     * @return a list of API version strings
     * @throws ApiVersionNotFoundException if the API versions cannot be loaded
     */
    public List<String> retrieveApiVersions(@NonNull String serviceId) {
        InstanceInfo instanceInfo;

        try {
            instanceInfo = getInstanceInfo(serviceId);
        } catch (ApiDocNotFoundException e) {
            throw new ApiVersionNotFoundException(e.getMessage());
        }

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        List<String> apiVersions = new ArrayList<>();
        for (ApiInfo apiInfo : apiInfoList) {
            int majorVersion = getMajorVersion(apiInfo);
            if (majorVersion >= 0) {
                // -1 indicates major version not found
                apiVersions.add("v" + majorVersion);
            }
        }
        return apiVersions;
    }

    /**
     * Retrieves the default API version for a registered service.
     * Uses 'apiml.service.apiInfo.defaultApi' field.
     * <p>
     * Returns version in the format 'v{majorVersion|'}. If no API is set as default, null is returned.
     *
     * @param serviceId the unique service ID
     * @return default API version in the format v{majorVersion}, or null.
     */
    public String retrieveDefaultApiVersion(@NonNull String serviceId) {
        InstanceInfo instanceInfo;

        try {
            instanceInfo = getInstanceInfo(serviceId);
        } catch (ApiDocNotFoundException e) {
            throw new ApiVersionNotFoundException(e.getMessage());
        }

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        ApiInfo defaultApiInfo = getApiInfoSetAsDefault(apiInfoList);

        if (defaultApiInfo == null) {
            return null;
        }

        return "v" + getMajorVersion(defaultApiInfo);
    }

    /**
     * Retrieve the API docs for a registered service
     * <p>
     * API doc URL is taken from the application metadata in the following
     * order:
     * <p>
     * 1. 'apiml.service.apiInfo.swaggerUrl' (preferred way)
     * 2. 'apiml.service.apiInfo' is present and 'swaggerUrl' is not, ApiDoc info is automatically generated
     * 3. URL is constructed from 'apiml.routes.api-doc.serviceUrl'. This method is deprecated and used for
     * backwards compatibility only
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return the API doc and related information for transformation
     * @throws ApiDocNotFoundException if the response is error
     */
    public ApiDocInfo retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        InstanceInfo instanceInfo = getInstanceInfo(serviceId);

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        ApiInfo apiInfo = findApi(apiInfoList, apiVersion);

        return buildApiDocInfo(serviceId, apiInfo, instanceInfo);
    }

    private ApiDocInfo buildApiDocInfo(String serviceId, ApiInfo apiInfo, InstanceInfo instanceInfo) {
        RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());
        String apiDocUrl = getApiDocUrl(apiInfo, instanceInfo, routes);

        if (apiDocUrl == null) {
            return getApiDocInfoBySubstituteSwagger(instanceInfo, routes, apiInfo);
        }

        // The Swagger generated by zOSMF is invalid because it has null in enum values for boolean.
        // Remove once we know that zOSMF APARs for zOS2.2 and 2.3 is prerequisite
        if (serviceId.equals("zosmf") || serviceId.equals("ibmzosmf")) {
            try {
                String apiDocContent = getApiDocContentByUrl(serviceId, apiDocUrl);
                apiDocContent = apiDocContent.replace("null, null", "true, false");
                return new ApiDocInfo(apiInfo, apiDocContent, routes);
            } catch (Exception e) {
                return getApiDocInfoBySubstituteSwagger(instanceInfo, routes, apiInfo);
            }
        }

        String apiDocContent = getApiDocContentByUrl(serviceId, apiDocUrl);
        return new ApiDocInfo(apiInfo, apiDocContent, routes);
    }

    /**
     * Retrieve the default API docs for a registered service.
     * <p>
     * Default API doc is selected via the configuration parameter 'apiml.service.apiInfo.isDefault'.
     * <p>
     * If there are multiple apiInfo elements with isDefault set to 'true', or there are none set to 'true',
     * then the high API version will be selected.
     *
     * @param serviceId the unique service id
     * @return the default API doc and related information for transfer
     * @throws ApiDocNotFoundException if the response is error
     */
    public ApiDocInfo retrieveDefaultApiDoc(@NonNull String serviceId) {
        InstanceInfo instanceInfo = getInstanceInfo(serviceId);

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        ApiInfo defaultApiInfo = getApiInfoSetAsDefault(apiInfoList);

        if (defaultApiInfo == null) {
            defaultApiInfo = getHighestApiVersion(apiInfoList);
        }

        return buildApiDocInfo(serviceId, defaultApiInfo, instanceInfo);
    }

    private ApiInfo getApiInfoSetAsDefault(List<ApiInfo> apiInfoList) {
        ApiInfo defaultApiInfo = null;
        for (ApiInfo apiInfo : apiInfoList) {
            if (apiInfo.isDefaultApi()) {
                if (defaultApiInfo != null) {
                    // multiple APIs set as default, can't handle conflict so stop looking for set default
                    return null;
                } else {
                    defaultApiInfo = apiInfo;
                }
            }
        }
        return defaultApiInfo;
    }

    private ApiInfo getHighestApiVersion(List<ApiInfo> apiInfoList) {
        if (apiInfoList == null || apiInfoList.isEmpty()) {
            return null;
        }

        ApiInfo highestVersionApi = apiInfoList.get(0);
        for (ApiInfo apiInfo : apiInfoList) {
            if (isHigherVersion(apiInfo, highestVersionApi)) {
                highestVersionApi = apiInfo;
            }
        }
        return highestVersionApi;
    }

    private boolean isHigherVersion(ApiInfo toTest, ApiInfo comparedAgainst) {
        int versionToTest = getMajorVersion(toTest);
        int versionToCompare = getMajorVersion(comparedAgainst);

        return versionToTest > versionToCompare;
    }

    /**
     * Return the major version from the version field in ApiInfo.
     * <p>
     * Major version is assumed to be the first integer in the version string.
     * <p>
     * If there is no major version (that is, no integers in the version string),
     * -1 is returned as it assumed valid major versions will be 0 or higher. Thus,
     * -1 can be used in an integer comparison for highest integer.
     *
     * @param apiInfo ApiInfo for which major version will be retrieved.
     * @return int representing major version. If no version integer
     */
    private int getMajorVersion(ApiInfo apiInfo) {
        if (apiInfo == null || apiInfo.getVersion() == null) {
            return -1;
        }

        String[] versionFields = apiInfo.getVersion().split("[^0-9a-zA-Z]");
        String majorVersionStr = versionFields[0].replaceAll("[^0-9]", "");
        return majorVersionStr.isEmpty() ? -1 : Integer.parseInt(majorVersionStr);
    }

    /**
     * Get ApiDoc url
     *
     * @param apiInfo      the apiInfo of service instance
     * @param instanceInfo the information about service instance
     * @param routes       the routes of service instance
     * @return the url of apidoc
     */
    private String getApiDocUrl(ApiInfo apiInfo, InstanceInfo instanceInfo, RoutedServices routes) {
        String apiDocUrl = null;
        if (apiInfo == null) {
            apiDocUrl = createApiDocUrlFromRouting(instanceInfo, routes);
        } else if (apiInfo.getSwaggerUrl() != null) {
            apiDocUrl = apiInfo.getSwaggerUrl();
        }

        return apiDocUrl;
    }


    /**
     * Get ApiDoc content by Url
     *
     * @param serviceId the unique service id
     * @param apiDocUrl the url of apidoc
     * @return the information about ApiDoc content as application/json
     * @throws ApiDocNotFoundException if the response is error
     */
    private String getApiDocContentByUrl(@NonNull String serviceId, String apiDocUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

        ResponseEntity<String> response = restTemplate.exchange(
            apiDocUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        if (response.getStatusCode().isError()) {
            throw new ApiDocNotFoundException("No API Documentation was retrieved due to " + serviceId + " server error: '" + response.getBody() + "'.");
        }
        return response.getBody();
    }

    /**
     * Get ApiDocInfo by Substitute Swagger
     *
     * @param instanceInfo the information about service instance
     * @param routes       the routes of service instance
     * @param apiInfo      the apiInfo of service instance
     * @return the information about APIDocInfo
     */
    private ApiDocInfo getApiDocInfoBySubstituteSwagger(InstanceInfo instanceInfo,
                                                        RoutedServices routes,
                                                        ApiInfo apiInfo) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String response = swaggerGenerator.generateSubstituteSwaggerForService(
            instanceInfo,
            apiInfo,
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname());
        return new ApiDocInfo(apiInfo, response, routes);
    }

    /**
     * Find ApiInfo for the corresponding version, if not found the first one is returned
     *
     * @param apiInfos   the list of APIs information
     * @param apiVersion the version to be find
     * @return the information about API
     */
    private ApiInfo findApi(List<ApiInfo> apiInfos, String apiVersion) {
        if (apiInfos.isEmpty()) {
            return null;
        }

        return apiInfos.stream()
            .filter(
                f -> f.getGatewayUrl().equals(apiVersion == null ? "api" : "api/" + apiVersion)
            )
            .findFirst()
            .orElse(apiInfos.get(0));
    }

    private InstanceInfo getInstanceInfo(String serviceId) {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(serviceId);
        if (instanceInfo == null) {
            throw new ApiDocNotFoundException("Could not load instance information for service " + serviceId + ".");
        }
        return instanceInfo;
    }

    /**
     * Creates a URL from the routing metadata 'apiml.routes.api-doc.serviceUrl' when 'apiml.apiInfo.swaggerUrl' is
     * not present
     *
     * @param instanceInfo the information about service instance
     * @return the URL of API doc endpoint
     * @deprecated Added to support services which were on-boarded before 'apiml.apiInfo.swaggerUrl' parameter was
     * introduced. It will be removed when all services will be using the new configuration style.
     */
    @Deprecated
    private String createApiDocUrlFromRouting(InstanceInfo instanceInfo, RoutedServices routes) {
        String scheme;
        int port;
        if (instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            scheme = "https";
            port = instanceInfo.getSecurePort();
        } else {
            scheme = "http";
            port = instanceInfo.getPort();
        }

        String path = null;
        RoutedService route = routes.findServiceByGatewayUrl("api/v1/api-doc");
        if (route != null) {
            path = route.getServiceUrl();
        }

        if (path == null) {
            throw new ApiDocNotFoundException("No API Documentation defined for service " + instanceInfo.getAppName().toLowerCase() + ".");
        }

        UriComponents uri = UriComponentsBuilder
            .newInstance()
            .scheme(scheme)
            .host(instanceInfo.getHostName())
            .port(port)
            .path(path)
            .build();

        return uri.toUriString();
    }
}
