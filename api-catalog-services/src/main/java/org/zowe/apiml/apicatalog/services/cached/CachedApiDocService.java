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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocCacheKey;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionsNotFoundException;
import org.zowe.apiml.apicatalog.swagger.TransformApiDocService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caching service for API Doc Info
 */

@Service
public class CachedApiDocService {
    private static final String LATEST_API_KEY = "latest";

    private static final Map<ApiDocCacheKey, String> serviceApiDocs = new HashMap<>();
    private static final Map<String, List<String>> serviceApiVersions = new HashMap<>();

    private final APIDocRetrievalService apiDocRetrievalService;
    private final TransformApiDocService transformApiDocService;

    @Autowired
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
        String apiDoc = CachedApiDocService.serviceApiDocs.get(new ApiDocCacheKey(serviceId, apiVersion));
        try {
            ApiDocInfo apiDocInfo = apiDocRetrievalService.retrieveApiDoc(serviceId, apiVersion);
            if (apiDocInfo != null && apiDocInfo.getApiDocContent() != null) {
                apiDoc = transformApiDocService.transformApiDoc(serviceId, apiDocInfo);
                CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, apiVersion), apiDoc);
            }
            if (apiDoc == null) {
                throw new ApiDocNotFoundException("No API Documentation was retrieved for the service " + serviceId + ".");
            }
        } catch (Exception e) {
            //if there's not apiDoc in cache
            if (apiDoc == null) {
                throw new ApiDocNotFoundException("No API Documentation was retrieved for the service " + serviceId + ".");
            }
        }
        return apiDoc;
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
        String apiDoc = CachedApiDocService.serviceApiDocs.get(new ApiDocCacheKey(serviceId, LATEST_API_KEY));
        try {
            ApiDocInfo apiDocInfo = apiDocRetrievalService.retrieveDefaultApiDoc(serviceId);
            if (apiDocInfo != null && apiDocInfo.getApiDocContent() != null) {
                apiDoc = transformApiDocService.transformApiDoc(serviceId, apiDocInfo);
                CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, LATEST_API_KEY), apiDoc);
            }
            if (apiDoc == null) {
                throw new ApiDocNotFoundException("No API Documentation was retrieved for the service " + serviceId + ".");
            }
        } catch (Exception e) {
            //if there's not apiDoc in cache
            if (apiDoc == null) {
                throw new ApiDocNotFoundException("No API Documentation was retrieved for the service " + serviceId + ".");
            }
        }
        return apiDoc;
    }

    /**
     * Update the latest version api doc for this service.
     * THis method should be executed if a new version of a service is discovered on renewal
     *
     * @param serviceId service identifier
     * @param apiDoc    API Doc info
     */
    public void updateLatestApiDocForService(final String serviceId, final String apiDoc) {
        CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, LATEST_API_KEY), apiDoc);
    }

    /**
     * Update the api versions for this service
     *
     * @param serviceId service identifier
     * @return List of API version strings for the requested service ID
     */
    public List<String> getApiVersionsForService(final String serviceId) {
        List<String> apiVersions = CachedApiDocService.serviceApiVersions.get(serviceId);
        try {
            List<String> versions = apiDocRetrievalService.retrieveApiVersions(serviceId);
            if (versions.isEmpty()) {
                throw new ApiVersionsNotFoundException("No API versions were retrieved for the service " + serviceId + ".");
            } else {
                apiVersions = versions;
                CachedApiDocService.serviceApiVersions.put(serviceId, apiVersions);
            }
        } catch (Exception e) {
            // if no versions in cache
            if (apiVersions == null) {
                throw new ApiVersionsNotFoundException("No API versions were retrieved for the service " + serviceId + ".");
            }
        }
        return apiVersions;
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
     * Reset the cache for this service
     */
    public void resetCache() {
        serviceApiDocs.clear();
        serviceApiVersions.clear();
    }
}
