/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.services.cached;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocCacheKey;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionNotFoundException;
import org.zowe.apiml.apicatalog.swagger.TransformApiDocService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Caching service for API Doc Info
 */

@Service
@Slf4j
public class CachedApiDocService {
    public static final String DEFAULT_API_KEY = "default";

    private static final Map<ApiDocCacheKey, String> serviceApiDocs = new HashMap<>();
    private static final Map<String, List<String>> serviceApiVersions = new HashMap<>();
    private static final Map<String, String> serviceApiDefaultVersions = new HashMap<>();

    private final APIDocRetrievalService apiDocRetrievalService;
    private final TransformApiDocService transformApiDocService;

    private static final UnaryOperator<String> exceptionMessage = serviceId -> "No API Documentation was retrieved for the service " + serviceId + ".";

    public CachedApiDocService(APIDocRetrievalService apiDocRetrievalService, TransformApiDocService transformApiDocService) {
        this.apiDocRetrievalService = apiDocRetrievalService;
        this.transformApiDocService = transformApiDocService;
    }

    /**
     * Update the api docs for this service
     *
     * @param serviceId  service identifier
     * @param apiVersion the version of the API
     * @return api doc info for the requested service id
     */
    public String getApiDocForService(final String serviceId, final String apiVersion) {
        // First try to fetch apiDoc from the DS
        try {
            ApiDocInfo apiDocInfo = apiDocRetrievalService.retrieveApiDoc(serviceId, apiVersion);
            if (apiDocInfo != null && apiDocInfo.getApiDocContent() != null) {
                String apiDoc = transformApiDocService.transformApiDoc(serviceId, apiDocInfo);
                CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, apiVersion), apiDoc);
                return apiDoc;
            }
        } catch (Exception e) {
            log.debug("Exception updating API doc in cache for '{} {}'", serviceId, apiVersion, e);
        }

        // if no DS is available try to use cached data
        String apiDoc = CachedApiDocService.serviceApiDocs.get(new ApiDocCacheKey(serviceId, apiVersion));
        if (apiDoc != null) {
            return apiDoc;
        }

        // cannot obtain apiDoc ends with exception
        log.error("No API doc available for '{} {}'", serviceId, apiVersion);
        throw new ApiDocNotFoundException(exceptionMessage.apply(serviceId));
    }

    /**
     * Update the api docs for this service
     * This method should be executed if a new version of a service is discovered on renewal
     *
     * @param serviceId  service identifier
     * @param apiVersion the version of the API
     * @param apiDoc     API Doc info
     */
    public void updateApiDocForService(final String serviceId, final String apiVersion, final String apiDoc) {
        CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, apiVersion), apiDoc);
    }

    /**
     * Update the docs for the latest API version for this service
     *
     * @param serviceId service identifier
     * @return api doc info for the latest API of the request service id
     */
    public String getDefaultApiDocForService(final String serviceId) {
        // First try to fetch apiDoc from the DS
        try {
            ApiDocInfo apiDocInfo = apiDocRetrievalService.retrieveDefaultApiDoc(serviceId);
            if (apiDocInfo != null && apiDocInfo.getApiDocContent() != null) {
                String apiDoc = transformApiDocService.transformApiDoc(serviceId, apiDocInfo);
                CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, DEFAULT_API_KEY), apiDoc);
                return apiDoc;
            }
        } catch (Throwable t) {
            log.debug("Exception updating default API doc in cache for '{}'.", serviceId, t);
        }

        // if no DS is available try to use cached data
        String apiDoc = CachedApiDocService.serviceApiDocs.get(new ApiDocCacheKey(serviceId, DEFAULT_API_KEY));
        if (apiDoc != null) {
            return apiDoc;
        }

        // cannot obtain apiDoc ends with exception
        log.error("No default API doc available for service '{}'", serviceId);
        throw new ApiDocNotFoundException(exceptionMessage.apply(serviceId));
    }

    /**
     * Update the latest version api doc for this service.
     * THis method should be executed if a new version of a service is discovered on renewal
     *
     * @param serviceId service identifier
     * @param apiDoc    API Doc info
     */
    public void updateDefaultApiDocForService(final String serviceId, final String apiDoc) {
        CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, DEFAULT_API_KEY), apiDoc);
    }

    /**
     * Update the api versions for this service
     *
     * @param serviceId service identifier
     * @return List of API version strings for the requested service ID
     */
    public List<String> getApiVersionsForService(final String serviceId) {
        // First try to fetch apiDoc from the DS
        try {
            List<String> versions = apiDocRetrievalService.retrieveApiVersions(serviceId);
            if (!versions.isEmpty()) {
                CachedApiDocService.serviceApiVersions.put(serviceId, versions);
                return versions;
            }
        } catch (Exception e) {
            log.debug("Exception updating API versions in cache for {}", serviceId, e);
        }

        // if no DS is available try to use cached data
        List<String> versions = CachedApiDocService.serviceApiVersions.get(serviceId);
        if (versions != null) {
            return versions;
        }

        // cannot obtain apiDoc ends with exception
        log.error("No API versions available for service '{}'", serviceId);
        throw new ApiVersionNotFoundException("No API versions were retrieved for the service " + serviceId + ".");
    }

    /**
     * Update the api versions for this service.
     * This method should be executed if a new version of a service is discovered on renewal.
     *
     * @param serviceId   service identifier
     * @param apiVersions the API versions
     */
    public void updateApiVersionsForService(final String serviceId, final List<String> apiVersions) {
        CachedApiDocService.serviceApiVersions.put(serviceId, apiVersions);
    }

    /**
     * Update the default API version for this service.
     *
     * @param serviceId service identifier
     * @return default API version for given service id
     */
    public String getDefaultApiVersionForService(final String serviceId) {
        // First try to fetch apiDoc from the DS
        try {
            String version = apiDocRetrievalService.retrieveDefaultApiVersion(serviceId);
            if (version != null) {
                CachedApiDocService.serviceApiDefaultVersions.put(serviceId, version);
                return version;
            }
        } catch (Exception e) {
            log.error("No default API version available for service '{}'", serviceId, e);
        }

        // if no DS is available try to use cached data
        String version = CachedApiDocService.serviceApiDefaultVersions.get(serviceId);
        if (version != null) {
            return version;
        }

        // cannot obtain apiDoc ends with exception
        throw new ApiVersionNotFoundException("Error trying to find default API version");
    }

    /**
     * Update the default api version for this service.
     * This method should be executed if a new version of a service is discovered on renewal.
     *
     * @param serviceId  service identifier
     * @param apiVersion the default API version
     */
    public void updateDefaultApiVersionForService(final String serviceId, final String apiVersion) {
        CachedApiDocService.serviceApiDefaultVersions.put(serviceId, apiVersion);
    }

    /**
     * Reset the cache for this service
     */
    public void resetCache() {
        serviceApiDocs.clear();
        serviceApiVersions.clear();
    }
}
