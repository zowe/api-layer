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

import java.net.MalformedURLException;
import java.net.URL;
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
        URL baseUrl;
        validateRoutes(config.getRoutes());

        validateSsl(config.getSsl());

        try {
            baseUrl = new URL(config.getBaseUrl());
            baseUrl.getHost();
            baseUrl.getPort();
        } catch (MalformedURLException e) {
            String message = String.format("baseUrl: [%s] is not valid URL", config.getBaseUrl());
            throw new MetadataValidationException(message, e);
        }

        String protocol = baseUrl.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new MetadataValidationException(String.format("'%s' is not valid protocol for baseUrl property", protocol));
        }
        if (config.getCatalog() == null) {
            log.warn("The API Catalog UI tile configuration is not provided. Try to add apiml.service.catalog.tile section.");
        }

        if (config.getHomePageRelativeUrl() == null || config.getHomePageRelativeUrl().isEmpty() || config.getHomePageRelativeUrl().contains("${apiml.")) {
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
            if (route.getGatewayUrl() == null ||
                route.getGatewayUrl().isEmpty() ||
                route.getGatewayUrl().contains("{apiml.") ||
                route.getServiceUrl() == null ||
                route.getServiceUrl().isEmpty() ||
                route.getServiceUrl().contains("{apiml.")) {
                throw new MetadataValidationException("Routes parameters are missing or were not replaced by the system properties.");
            }
        });
    }

    private void validateSsl(Ssl ssl) {
        if (ssl == null) {
            throw new MetadataValidationException("SSL configuration was not provided. Try add apiml.service.ssl section.");
        }
        if (ssl.getProtocol() == null || ssl.getProtocol().isEmpty() || ssl.getProtocol().contains("{apiml.") ||
            ssl.getTrustStorePassword() == null || ssl.getTrustStorePassword().isEmpty() || ssl.getTrustStorePassword().contains("{apiml.") ||
            ssl.getTrustStore() == null || ssl.getTrustStore().isEmpty() || ssl.getTrustStore().contains("{apiml.") ||
            ssl.getKeyStorePassword() == null || ssl.getKeyStorePassword().isEmpty() || ssl.getKeyStorePassword().contains("{apiml.") ||
            ssl.getKeyStore() == null || ssl.getKeyStore().isEmpty() || ssl.getKeyStore().contains("{apiml.") ||
            ssl.getKeyAlias() == null || ssl.getKeyAlias().isEmpty() || ssl.getKeyAlias().contains("{apiml.") ||
            ssl.getCiphers() == null || ssl.getCiphers().isEmpty() || ssl.getCiphers().contains("{apiml.") ||
            ssl.getKeyStoreType() == null || ssl.getKeyStoreType().isEmpty() || ssl.getKeyStoreType().contains("{apiml.") ||
            ssl.getTrustStoreType() == null || ssl.getTrustStoreType().isEmpty() || ssl.getTrustStoreType().contains("{apiml.") ||
            ssl.getKeyPassword() == null || ssl.getKeyPassword().isEmpty() || ssl.getKeyPassword().contains("{apiml.") ||
            ssl.getEnabled() == null) {
            throw new MetadataValidationException("SSL parameters are missing or were not replaced by the system properties.");
        }
    }
}
