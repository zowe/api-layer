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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.exceptions.ApiVersionNotFoundException;
import org.zowe.apiml.apicatalog.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.util.EurekaUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiDocService {

    private static final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private static final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    private final DiscoveryClient discoveryClient;
    private final GatewayClient gatewayClient;
    private final TransformApiDocService transformApiDocService;
    private final ApiDocRetrievalServiceLocal apiDocRetrievalServiceLocal;
    private final ApiDocRetrievalServiceRest apiDocRetrievalServiceRest;

    ServiceInstance getInstanceInfo(String serviceId) {
        return EurekaUtils.getInstanceInfo(discoveryClient, serviceId)
            .orElseThrow(() -> new ApiDocNotFoundException("Could not load instance information for service " + serviceId + "."));
    }

    private ApiInfo getApiInfoSetAsDefault(List<ApiInfo> apiInfoList) {
        ApiInfo defaultApiInfo = null;
        for (ApiInfo apiInfo : apiInfoList) {
            if (apiInfo.isDefaultApi()) {
                if (defaultApiInfo != null) {
                    log.warn("Multiple API are set as default: '{} {}' and '{} {}'. Neither will be treated as the default.",
                        defaultApiInfo.getApiId(), apiInfo.getVersion(),
                        apiInfo.getApiId(), apiInfo.getVersion()
                    );
                    return null;
                } else {
                    defaultApiInfo = apiInfo;
                }
            }
        }
        return defaultApiInfo;
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
        if (apiInfo == null) {
            return -1;
        }

        return apiInfo.getMajorVersion();
    }

    private boolean isHigherVersion(ApiInfo toTest, ApiInfo comparedAgainst) {
        int versionToTest = getMajorVersion(toTest);
        int versionToCompare = getMajorVersion(comparedAgainst);

        return versionToTest > versionToCompare;
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

    private ApiInfo getDefaultApiInfo(List<ApiInfo> apiInfoList) {
        ApiInfo defaultApiInfo = getApiInfoSetAsDefault(apiInfoList);

        if (defaultApiInfo == null) {
            log.debug("No API set as default, will use highest major version as default");
            defaultApiInfo = getHighestApiVersion(apiInfoList);
        }

        return defaultApiInfo;
    }

    private List<String> retrieveApiVersions(@NonNull Map<String, String> metadata) {
        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(metadata);
        List<String> apiVersions = new ArrayList<>();
        for (ApiInfo apiInfo : apiInfoList) {
            apiVersions.add(apiInfo.getApiId() + " v" + apiInfo.getVersion());
        }

        return apiVersions;
    }

    private String retrieveDefaultApiVersion(@NonNull Map<String, String> metadata) {
        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(metadata);
        ApiInfo defaultApiInfo = getDefaultApiInfo(apiInfoList);

        if (defaultApiInfo == null) {
            return "";
        }

        return String.format("%s v%s", defaultApiInfo.getApiId(), defaultApiInfo.getVersion());
    }

    /**
     * Retrieves the available API versions for a registered service.
     * Takes the versions available in each 'apiml.service.apiInfo' element.
     *
     * @param serviceId the unique service ID
     * @return a list of API version strings
     * @throws ApiVersionNotFoundException if the API versions cannot be loaded
     */
    public List<String> retrieveApiVersions(@NonNull String serviceId) {
        log.debug("Retrieving API versions for service '{}'", serviceId);
        ServiceInstance serviceInstance;

        try {
            serviceInstance = getInstanceInfo(serviceId);
        } catch (ApiDocNotFoundException e) {
            throw new ApiVersionNotFoundException(e.getMessage());
        }

        List<String> apiVersions = retrieveApiVersions(serviceInstance.getMetadata());
        log.debug("For service '{}' found API versions '{}'", serviceId, apiVersions);

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
        log.debug("Retrieving default API version for service '{}'", serviceId);
        ServiceInstance serviceInstance;

        try {
            serviceInstance = getInstanceInfo(serviceId);
        } catch (ApiDocNotFoundException e) {
            throw new ApiVersionNotFoundException(e.getMessage());
        }

        String defaultVersion = retrieveDefaultApiVersion(serviceInstance.getMetadata());
        log.debug("For service '{}' found default API version '{}'", serviceId, defaultVersion);

        return defaultVersion;
    }

    /**
     * Creates a URL from the routing metadata 'apiml.routes.api-doc.serviceUrl' when 'apiml.apiInfo.swaggerUrl' is
     * not present
     *
     * @param serviceInstance the information about service instance
     * @return the URL of API doc endpoint
     */
    private String createApiDocUrlFromRouting(ServiceInstance serviceInstance, RoutedServices routes) {
        String scheme = serviceInstance.isSecure() ? "https" : "http";
        int port = serviceInstance.getPort();

        String path = null;
        RoutedService route = routes.findServiceByGatewayUrl("api/v1/api-doc");
        if (route != null) {
            path = route.getServiceUrl();
        }

        if (path == null) {
            return null;
        }

        UriComponents uri = UriComponentsBuilder
            .newInstance()
            .scheme(scheme)
            .host(serviceInstance.getHost())
            .port(port)
            .path(path)
            .build();

        return uri.toUriString();
    }

    private String getGatewayUrl() {
        ServiceAddress gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();

        return String.format("%s://%s/",
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname()
        );
    }

    /**
     * Get ApiDocInfo by Substitute Swagger
     *
     * @param serviceInstance the information about service instance
     * @return the information about APIDocInfo
     */
    private Mono<ApiDocInfo> getApiDocInfoBySubstituteSwagger(ServiceInstance serviceInstance, ApiInfo apiInfo) {
        return Mono.fromSupplier(gatewayClient::getGatewayConfigProperties)
            .map(gw -> swaggerGenerator.generateSubstituteSwaggerForService(
                serviceInstance,
                apiInfo,
                gw.getScheme(),
                gw.getHostname())
            )
            .map(content -> ApiDocInfo.builder().apiInfo(apiInfo).apiDocContent(content).routes(new RoutedServices()).build());
    }

    Mono<String> retrieveApiDoc(ServiceInstance serviceInstance, ApiInfo apiInfo) {
        String serviceId = StringUtils.lowerCase(serviceInstance.getServiceId());
        var routes = metadataParser.parseRoutes(serviceInstance.getMetadata());

        if (apiInfo == null) {
            apiInfo = ApiInfo.builder().gatewayUrl(getGatewayUrl()).build();
        }

        if (apiInfo.getSwaggerUrl() == null) {
            apiInfo.setSwaggerUrl(createApiDocUrlFromRouting(serviceInstance, routes));
        }

        Mono<ApiDocInfo> apiDocInfo;
        if (apiInfo.getSwaggerUrl() == null) {
            apiDocInfo = getApiDocInfoBySubstituteSwagger(serviceInstance, apiInfo);
        } else if (apiDocRetrievalServiceLocal.isSupported(serviceId)) {
            apiDocInfo = apiDocRetrievalServiceLocal.retrieveApiDoc(serviceInstance, apiInfo);
        } else {
            apiDocInfo = apiDocRetrievalServiceRest.retrieveApiDoc(serviceInstance, apiInfo);
        }

        return apiDocInfo
            .map(a -> {
                a.setRoutes(routes);
                return transformApiDocService.transformApiDoc(serviceId, a);
            });
    }

    /**
     * Find ApiInfo for the corresponding version, if not found the first one is returned
     *
     * @param apiInfos   the list of APIs information
     * @param apiVersion the version to be found
     * @return the information about API
     */
    private ApiInfo findApi(List<ApiInfo> apiInfos, String apiVersion) {
        if (apiInfos.isEmpty()) {
            return null;
        }

        if (apiVersion == null) {
            return apiInfos.get(0);
        }

        String[] api = apiVersion.split(" ");
        String apiId = api.length > 0 ? api[0] : "";
        String version = api.length > 1 ? api[1].replace("v", "") : "";

        return apiInfos.stream()
            .filter(
                f -> apiId.equals(f.getApiId()) && (version.equals(f.getVersion()))
            )
            .findFirst()
            .orElseThrow(() -> {
                String errMessage = String.format("Error finding api doc: there is no api doc for '%s %s'.", apiId, version);
                log.error(errMessage);
                return new ApiDocNotFoundException(errMessage);
            });
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
     * @return the API doc
     * @throws ApiDocNotFoundException if the response is error
     */
    public Mono<String> retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        ServiceInstance serviceInstance = getInstanceInfo(serviceId);
        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(serviceInstance.getMetadata());
        var apiInfo = findApi(apiInfoList, apiVersion);
        return retrieveApiDoc(serviceInstance, apiInfo);
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
     * @return the default API doc
     * @throws ApiDocNotFoundException if the response is error
     */
    public Mono<String> retrieveDefaultApiDoc(@NonNull String serviceId) {
        ServiceInstance serviceInstance = getInstanceInfo(serviceId);
        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(serviceInstance.getMetadata());
        var apiInfo = getDefaultApiInfo(apiInfoList);
        return retrieveApiDoc(serviceInstance, apiInfo);
    }

}
