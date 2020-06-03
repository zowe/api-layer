/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.util;

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class EurekaInstanceConfigValidator {

    public void validateConfiguration(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        URL baseUrl;
        if (config.getRoutes() == null) {
            throw new MetadataValidationException("Routes configuration was not provided. Try to add apiml.service.routes section.");
        }
//        if (config.getCatalog() == null) {
//            throw new MetadataValidationException("Tile configuration for API Catalog was not provided. Try add apiml.service.catalog.tile section.");
//        }
        try {
            baseUrl = new URL(config.getBaseUrl());
            baseUrl.getHost();
            baseUrl.getPort();
        } catch (MalformedURLException e) {
            String message = String.format("baseUrl: [%s] is not valid URL", config.getBaseUrl());
            throw new ServiceDefinitionException(message, e);
        }

        // maybe useless
        String protocol = baseUrl.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new ServiceDefinitionException(String.format("'%s' is not valid protocol for baseUrl property", protocol));
        }
        if (config.getCatalog() == null) {
            log.warn("The API Catalog UI tile configuration is not provided. Try to add apiml.service.catalog.tile section.");
        }

        if (config.getHomePageRelativeUrl() == null || config.getHomePageRelativeUrl().isEmpty()) {
            log.warn("The home page URL parameter is not provided in the configuration. Try to add apiml.service.homePageRelativeUrl property.");
        }

        if (config.getApiInfo() == null) {
            log.warn("The API info configuration is not provided. Try to add apiml.service.apiInfo section.");
        }

    }
}
