/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status;

import com.ca.mfaas.apicatalog.services.cached.CachedApiDocService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.netflix.discovery.shared.Applications;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;

import static com.ca.mfaas.product.constants.ApimConstants.API_DOC_NORMALISED;

@Slf4j
@Service
public class APIServiceStatusService {

    private final CachedServicesService cachedServicesService;
    private final CachedApiDocService cachedApiDocService;


    @Autowired
    public APIServiceStatusService(CachedServicesService cachedServicesService,
                                   CachedApiDocService cachedApiDocService) {
        this.cachedServicesService = cachedServicesService;
        this.cachedApiDocService = cachedApiDocService;
    }

    /**
     * Return a cached snapshot of services and instances
     *
     * @return Applications from cache
     */
    public Applications getCachedApplicationState() {
        return cachedServicesService.getAllCachedServices();
    }

    /**
     * Return the cached API docs for a service
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return a version of an API Doc
     */
    public ResponseEntity<String> getServiceCachedApiDocInfo(@NonNull String serviceId, String apiVersion) throws IOException {
        return new ResponseEntity<>(cachedApiDocService.getApiDocForService(serviceId, apiVersion), createHeaders(), HttpStatus.OK);
    }

    /**
     * HTTP headers
     *
     * @return headers for requests
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
        headers.set(API_DOC_NORMALISED, "true");
        return headers;
    }
}
