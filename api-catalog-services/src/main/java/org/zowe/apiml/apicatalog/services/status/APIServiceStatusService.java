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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.HtmlRender;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;
import org.zowe.apiml.apicatalog.services.status.model.ApiDiffNotAvailableException;

import java.util.Collections;

@Slf4j
@Service
@AllArgsConstructor
public class APIServiceStatusService {

    private final InstanceInitializeService instanceInitializeService;
    private final CachedApiDocService cachedApiDocService;
    private final OpenApiCompareProducer openApiCompareProducer;

    /**
     * Return the cached API docs for a service
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return a version of an API Doc
     */
    public ResponseEntity<String> getServiceCachedApiDocInfo(@NonNull String serviceId, String apiVersion) {
        return new ResponseEntity<>(cachedApiDocService.getApiDocForService(serviceId, apiVersion), createHeaders(), HttpStatus.OK);
    }

    /**
     * Return the cached default API doc for a service
     *
     * @param serviceId  the unique service id
     * @return the default version of an API Doc
     */
    public ResponseEntity<String> getServiceCachedDefaultApiDocInfo(@NonNull String serviceId) {
        return new ResponseEntity<>(cachedApiDocService.getDefaultApiDocForService(serviceId), createHeaders(), HttpStatus.OK);
    }

    /**
     * Return the diff of two api versions
     * @param serviceId the unique service id
     * @param apiVersion1 the old version of the api
     * @param apiVersion2 the new version of the api
     * @return response containing HTML document detailing changes between api doc versions
     */
    public ResponseEntity<String> getApiDiffInfo(@NonNull String serviceId, String apiVersion1, String apiVersion2) {
        try {
            String doc1 = cachedApiDocService.getApiDocForService(serviceId, apiVersion1);
            String doc2 = cachedApiDocService.getApiDocForService(serviceId, apiVersion2);
            ChangedOpenApi diff = openApiCompareProducer.fromContents(doc1, doc2);
            HtmlRender render = new HtmlRender();
            String result = render.render(diff);
            //Remove external stylesheet
            result = result.replace("<link rel=\"stylesheet\" href=\"http://deepoove.com/swagger-diff/stylesheets/demo.css\">", "");
            return new ResponseEntity<>(result, createHeaders(), HttpStatus.OK);
        } catch (Exception e) {
            String errorMessage = String.format("Error retrieving API diff for '%s' with versions '%s' and '%s'", serviceId, apiVersion1, apiVersion2);
            log.error(errorMessage, e);
            throw new ApiDiffNotAvailableException(errorMessage);
        }
    }

// ============================== HELPER METHODS


    /**
     * HTTP headers
     *
     * @return headers for requests
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
