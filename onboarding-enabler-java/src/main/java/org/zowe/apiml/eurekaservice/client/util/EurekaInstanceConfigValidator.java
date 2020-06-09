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
import org.zowe.apiml.eurekaservice.client.config.Route;
import org.zowe.apiml.eurekaservice.client.config.Ssl;
import org.zowe.apiml.exception.MetadataValidationException;

import java.util.List;

/**
 * Class that validates a service configuration before the registration with API ML
 */
@Slf4j
public class EurekaInstanceConfigValidator {

    /**
     * Validation method that validates mandatory and non-mandatory parameters
     * @param config
     * @throws MetadataValidationException if the validation fails
     */
    public void validate(ApiMediationServiceConfig config) {
        validateRoutes(config.getRoutes());
        validateSsl(config.getSsl());

        if (config.getCatalog() == null) {
            log.warn("The API Catalog UI tile configuration is not provided. Try to add apiml.service.catalog.tile section.");
        }

        if (isInvalid(config.getHomePageRelativeUrl())) {
            log.warn("The home page URL is not provided. Try to add apiml.service.homePageRelativeUrl property or check its value.");
        }

        if (config.getApiInfo() == null || config.getApiInfo().isEmpty()) {
            log.warn("The API info configuration is not provided. Try to add apiml.service.apiInfo section.");
        }
    }

    private void validateRoutes(List<Route> routes) {
        if (routes == null || routes.isEmpty()) {
            throw new MetadataValidationException("Routes configuration was not provided. Try to add apiml.service.routes section.");
        }
        routes.forEach(route -> {
            if (isInvalid(route.getGatewayUrl()) || isInvalid(route.getServiceUrl())) {
                throw new MetadataValidationException("Routes parameters are missing or were not replaced by the system properties.");
            }
        });
    }

    private void validateSsl(Ssl ssl) {
        if (ssl == null) {
            throw new MetadataValidationException("SSL configuration was not provided. Try add apiml.service.ssl section.");
        }
        if (isInvalid(ssl.getProtocol()) ||
            isInvalid(ssl.getTrustStorePassword()) ||
            isInvalid(ssl.getTrustStore()) ||
            isInvalid(ssl.getKeyStorePassword()) ||
            isInvalid(ssl.getKeyStore()) ||
            isInvalid(ssl.getKeyAlias()) ||
            isInvalid(ssl.getKeyStoreType()) ||
            isInvalid(ssl.getTrustStoreType()) ||
            isInvalid(ssl.getKeyPassword()) ||
            ssl.getEnabled() == null) {
            throw new MetadataValidationException("SSL parameters are missing or were not replaced by the system properties.");
        }
    }

    private boolean isInvalid(String value) {
        return value == null || value.isEmpty() || value.contains("{apiml.");
    }
}
