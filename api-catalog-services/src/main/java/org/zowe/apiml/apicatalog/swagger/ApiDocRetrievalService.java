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
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.exceptions.ApiVersionNotFoundException;

import java.util.List;

public interface ApiDocRetrievalService {

    /**
     * Retrieves the available API versions for a registered service.
     * Takes the versions available in each 'apiml.service.apiInfo' element.
     *
     * @param serviceId the unique service ID
     * @return a list of API version strings
     * @throws ApiVersionNotFoundException if the API versions cannot be loaded
     */
    List<String> retrieveApiVersions(@NonNull String serviceId);

    /**
     * Retrieves the default API version for a registered service.
     * Uses 'apiml.service.apiInfo.defaultApi' field.
     * <p>
     * Returns version in the format 'v{majorVersion|'}. If no API is set as default, null is returned.
     *
     * @param serviceId the unique service ID
     * @return default API version in the format v{majorVersion}, or null.
     */
    String retrieveDefaultApiVersion(@NonNull String serviceId);

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
    String retrieveApiDoc(@NonNull String serviceId, String apiVersion);

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
    String retrieveDefaultApiDoc(@NonNull String serviceId);

}
