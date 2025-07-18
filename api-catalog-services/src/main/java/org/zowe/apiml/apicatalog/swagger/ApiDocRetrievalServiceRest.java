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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.exceptions.ApiVersionNotFoundException;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.util.EurekaUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Retrieves the API documentation for a registered service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiDocRetrievalServiceRest implements ApiDocRetrievalService {

    private static final UnaryOperator<String> exceptionMessage = serviceId -> "No API Documentation was retrieved for the service " + serviceId + ".";

    @Qualifier("secureHttpClientWithoutKeystore")
    private final CloseableHttpClient secureHttpClientWithoutKeystore;

    private final DiscoveryClient discoveryClient;
    private final GatewayClient gatewayClient;

    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    private final TransformApiDocService transformApiDocService;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    @Override
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

    private List<String> retrieveApiVersions(@NonNull Map<String, String> metadata) {
        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(metadata);
        List<String> apiVersions = new ArrayList<>();
        for (ApiInfo apiInfo : apiInfoList) {
            apiVersions.add(apiInfo.getApiId() + " v" + apiInfo.getVersion());
        }

        return apiVersions;
    }

    @Override
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

    private String retrieveDefaultApiVersion(@NonNull Map<String, String> metadata) {
        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(metadata);
        ApiInfo defaultApiInfo = getDefaultApiInfo(apiInfoList);

        if (defaultApiInfo == null) {
            return "";
        }

        return String.format("%s v%s", defaultApiInfo.getApiId(), defaultApiInfo.getVersion());
    }

    @Override
    public String retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        log.debug("Retrieving API doc for '{} {}'", serviceId, apiVersion);
        ServiceInstance serviceInstance = getInstanceInfo(serviceId);

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(serviceInstance.getMetadata());
        ApiInfo apiInfo = findApi(apiInfoList, apiVersion);

        return buildApiDocInfo(serviceId, apiInfo, serviceInstance);
    }

    String buildApiDocInfo(String serviceId, ApiInfo apiInfo, ServiceInstance serviceInstance) {
        RoutedServices routes = metadataParser.parseRoutes(serviceInstance.getMetadata());
        String apiDocUrl = getApiDocUrl(apiInfo, serviceInstance, routes);

        ApiDocInfo apiDocInfo;
        if (apiDocUrl == null) {
            log.warn("No api doc URL for '{} {} {}'", serviceId, apiInfo.getApiId(), apiInfo.getVersion());
            apiDocInfo = getApiDocInfoBySubstituteSwagger(serviceInstance, routes, apiInfo);
        } else {
            String apiDocContent = "";
            try {
                apiDocContent = getApiDocContentByUrl(serviceId, apiDocUrl);
                apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routes);
            } catch (IOException e) {
                apimlLogger.log("org.zowe.apiml.apicatalog.apiDocHostCommunication", serviceId, e.getMessage());
                log.debug("Error retrieving api doc for '{}'", serviceId, e);
                throw new ApiDocNotFoundException(
                    exceptionMessage.apply(serviceId) + " Root cause: " + e.getMessage(), e
                );
            }
        }

        return transformApiDocService.transformApiDoc(serviceId, apiDocInfo);
    }

    @Override
    public String retrieveDefaultApiDoc(@NonNull String serviceId) {
        log.debug("Retrieving default API doc for service '{}'", serviceId);
        ServiceInstance serviceInstance = getInstanceInfo(serviceId);

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(serviceInstance.getMetadata());
        ApiInfo defaultApiInfo = getDefaultApiInfo(apiInfoList);

        return buildApiDocInfo(serviceId, defaultApiInfo, serviceInstance);
    }

    private ApiInfo getDefaultApiInfo(List<ApiInfo> apiInfoList) {
        ApiInfo defaultApiInfo = getApiInfoSetAsDefault(apiInfoList);

        if (defaultApiInfo == null) {
            log.debug("No API set as default, will use highest major version as default");
            defaultApiInfo = getHighestApiVersion(apiInfoList);
        }

        return defaultApiInfo;
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
        if (apiInfo == null) {
            return -1;
        }

        return apiInfo.getMajorVersion();
    }

    /**
     * Get ApiDoc url
     *
     * @param apiInfo      the apiInfo of service instance
     * @param serviceInstance the information about service instance
     * @param routes       the routes of service instance
     * @return the url of apidoc
     */
    private String getApiDocUrl(ApiInfo apiInfo, ServiceInstance serviceInstance, RoutedServices routes) {
        String apiDocUrl = null;
        if (apiInfo == null) {
            apiDocUrl = createApiDocUrlFromRouting(serviceInstance, routes);
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
    private String getApiDocContentByUrl(@NonNull String serviceId, String apiDocUrl) throws IOException {
        HttpGet httpGet = new HttpGet(apiDocUrl);
        httpGet.setHeader(org.apache.http.HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return secureHttpClientWithoutKeystore.execute(httpGet, response -> {
                String responseBody = "";
                var responseEntity = response.getEntity();
                if (responseEntity != null) {
                    responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                }

            if (HttpStatus.SC_OK == response.getCode()) {
                return responseBody;
            } else {
                    throw new ApiDocNotFoundException("No API Documentation was retrieved due to " + serviceId +
                        " server error: '" + responseBody + "'.");
                }
            }
        );
    }

    /**
     * Get ApiDocInfo by Substitute Swagger
     *
     * @param serviceInstance the information about service instance
     * @param routes       the routes of service instance
     * @param apiInfo      the apiInfo of service instance
     * @return the information about APIDocInfo
     */
    private ApiDocInfo getApiDocInfoBySubstituteSwagger(ServiceInstance serviceInstance,
                                                        RoutedServices routes,
                                                        ApiInfo apiInfo) {
        ServiceAddress gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String response = swaggerGenerator.generateSubstituteSwaggerForService(
            serviceInstance,
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
    private ServiceInstance getInstanceInfo(String serviceId) {
        return EurekaUtils.getInstanceInfo(discoveryClient, serviceId)
            .orElseThrow(() -> new ApiDocNotFoundException("Could not load instance information for service " + serviceId + "."));
    }

    /**
     * Creates a URL from the routing metadata 'apiml.routes.api-doc.serviceUrl' when 'apiml.apiInfo.swaggerUrl' is
     * not present
     *
     * @param serviceInstance the information about service instance
     * @return the URL of API doc endpoint
     * @deprecated Added to support services which were on-boarded before 'apiml.apiInfo.swaggerUrl' parameter was
     * introduced. It will be removed when all services will be using the new configuration style.
     */
    @Deprecated(forRemoval = false)
    private String createApiDocUrlFromRouting(ServiceInstance serviceInstance, RoutedServices routes) {
        String path = null;
        RoutedService route = routes.findServiceByGatewayUrl("api/v1/api-doc");
        if (route != null) {
            path = route.getServiceUrl();
        }

        if (path == null) {
            throw new ApiDocNotFoundException("No API Documentation defined for service " + serviceInstance.getServiceId().toLowerCase() + ".");
        }

        return UriComponentsBuilder.fromUri(serviceInstance.getUri())
            .path(path)
            .build()
            .toUriString();
    }

}
