/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.cached;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocCacheKey;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.APIDocRetrievalService;
import com.ca.mfaas.apicatalog.swagger.TransformApiDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Caching service for API Doc Info
 */
@Slf4j
@Service
public class CachedApiDocService {
    private static final Map<ApiDocCacheKey, String> serviceApiDocs = new HashMap<>();
    private final APIDocRetrievalService apiDocRetrievalService;
    private final TransformApiDocService transformApiDocService;
    private final InstanceRetrievalService instanceRetrievalService;

    @Autowired
    public CachedApiDocService(APIDocRetrievalService apiDocRetrievalService, TransformApiDocService transformApiDocService, InstanceRetrievalService instanceRetrievalService) {
        this.apiDocRetrievalService = apiDocRetrievalService;
        this.transformApiDocService = transformApiDocService;
        this.instanceRetrievalService = instanceRetrievalService;
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
        if (apiDoc == null) {
            ApiDocInfo apiDocInfo = apiDocRetrievalService.retrieveApiDoc(serviceId, apiVersion);
            if (apiDocInfo.getApiDocContent() == null) {
                return null;
            } else {
                apiDoc = transformApiDocService.transformApiDoc(getOriginalServiceId(serviceId), apiDocInfo);
                CachedApiDocService.serviceApiDocs.put(new ApiDocCacheKey(serviceId, apiVersion), apiDoc);
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
     * Reset the cache for this service
     */
    public void resetCache() {
        serviceApiDocs.clear();
    }

    /**
     * serviceId is converted to lower case by Discovery Service. This method recovers serviceId from VIPAddress,
     * because VIPAddress has the same value as serviceId, and it is not converted by lower case
     *
     * @param serviceId current serviceId
     * @return original serviceId which may contain mixed case letters
     */
    private String getOriginalServiceId(String serviceId) {
        return instanceRetrievalService.getInstanceInfo(serviceId).getVIPAddress();
    }
}
