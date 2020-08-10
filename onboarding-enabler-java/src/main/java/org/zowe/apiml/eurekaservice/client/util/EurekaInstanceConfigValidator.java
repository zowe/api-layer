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

import com.google.common.primitives.Chars;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.Route;
import org.zowe.apiml.eurekaservice.client.config.Ssl;
import org.zowe.apiml.exception.MetadataValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that validates a service configuration before the registration with API ML
 */
@Slf4j
public class EurekaInstanceConfigValidator {

    private static final String UNSET_VALUE_STRING = "{apiml.";
    private static final char[] UNSET_VALUE_CHAR_ARRAY = UNSET_VALUE_STRING.toCharArray();

    private final List<String> missingSslParameters = new ArrayList<>();
    private final List<String> missingRoutesParameters = new ArrayList<>();
    private final List<String> poorlyFormedRelativeUrlParameters = new ArrayList<>();

    /**
     * Validation method that validates mandatory and non-mandatory parameters
     *
     * @param config
     * @throws MetadataValidationException if the validation fails
     */
    public void validate(ApiMediationServiceConfig config) {
        validateRoutes(config.getRoutes());
        validateSsl(config.getSsl());
        validateUrls(config);

        if (config.getCatalog() == null) {
            log.warn("The API Catalog UI tile configuration is not provided. Try to add apiml.service.catalog.tile section.");
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
            if (isInvalid(route.getGatewayUrl())) {
                addParameterToProblemsList("gatewayUrl", missingRoutesParameters);
            }
            if (isInvalid(route.getServiceUrl())) {
                addParameterToProblemsList("serviceUrl", missingRoutesParameters);
            }
            if (!missingRoutesParameters.isEmpty()) {
                throw new MetadataValidationException(String.format("Routes parameters  ** %s ** are missing or were not replaced by the system properties.", String.join(", ", missingRoutesParameters)));
            }
        });
    }

    private void validateSsl(Ssl ssl) {
        if (ssl == null) {
            throw new MetadataValidationException("SSL configuration was not provided. Try add apiml.service.ssl section.");
        }
        if (isInvalid(ssl.getProtocol())) {
            addParameterToProblemsList("protocol", missingSslParameters);
        }
        if (isInvalid(ssl.getTrustStore())) {
            addParameterToProblemsList("trustStore", missingSslParameters);
        }
        if (isInvalid(ssl.getKeyStore())) {
            addParameterToProblemsList("keyStore", missingSslParameters);
        }
        if (isInvalid(ssl.getKeyAlias())) {
            addParameterToProblemsList("keyAlias", missingSslParameters);
        }
        if (isInvalid(ssl.getKeyStoreType())) {
            addParameterToProblemsList("keyStoreType", missingSslParameters);
        }
        if (isInvalid(ssl.getTrustStoreType())) {
            addParameterToProblemsList("trustStoreType", missingSslParameters);
        }
        if (isInvalid(ssl.getTrustStorePassword())) {
            if (isInvalid(ssl.getTrustStoreType()) ||
                (!isInvalid(ssl.getTrustStoreType()) && !ssl.getTrustStoreType().equals("JCERACFKS"))) {
                addParameterToProblemsList("trustStorePassword", missingSslParameters);
            }
        }
        if (isInvalid(ssl.getKeyStorePassword())) {
            if (isInvalid(ssl.getKeyStoreType()) ||
                (!isInvalid(ssl.getKeyStoreType()) && !ssl.getKeyStoreType().equals("JCERACFKS"))) {
                addParameterToProblemsList("keyStorePassword", missingSslParameters);
            }
        }
        if (isInvalid(ssl.getKeyPassword())) {
            addParameterToProblemsList("keyPassword", missingSslParameters);
        }
        if (ssl.getEnabled() == null) {
            addParameterToProblemsList("enabled", missingSslParameters);
        }
        if (!missingSslParameters.isEmpty()) {
            throw new MetadataValidationException(String.format("SSL parameters ** %s ** are missing or were not replaced by the system properties.", String.join(", ", missingSslParameters)));
        }
    }

    private void validateUrls(ApiMediationServiceConfig config) {
        validateHomePageRelativeUrl(config);

        if (isPoorlyFormedRelativeUrl(config.getHealthCheckRelativeUrl())) {
            addParameterToProblemsList("healthCheckRelativeUrl", poorlyFormedRelativeUrlParameters);
        }

        if (isPoorlyFormedRelativeUrl(config.getStatusPageRelativeUrl())) {
            addParameterToProblemsList("statusPageRelativeUrl", poorlyFormedRelativeUrlParameters);
        }

        if (isPoorlyFormedRelativeUrl(config.getContextPath())) {
            addParameterToProblemsList("contextPath", poorlyFormedRelativeUrlParameters);
        }

        if (!poorlyFormedRelativeUrlParameters.isEmpty()) {
            log.warn(String.format("Relative URL parameters ** %s ** don't begin with '/' which often causes malformed URLs.", String.join(", ", poorlyFormedRelativeUrlParameters)));
        }

        if (config.getBaseUrl() != null && config.getBaseUrl().endsWith("/")) {
            log.warn("The baseUrl parameter ends with a trailing '/'. This often causes malformed URLs when relative URLs are used.");
        }

        if (config.getContextPath() != null && config.getContextPath().endsWith("/")) {
            log.warn("The contextPath parameter ends with a trailing '/'. This often causes malformed URLs when relative URLs are used.");
        }
    }

    private void validateHomePageRelativeUrl(ApiMediationServiceConfig config) {
        String homePageUrl = config.getHomePageRelativeUrl();
        if (isInvalid(homePageUrl) && config.getRoutes().stream().noneMatch(route -> route.getGatewayUrl().toLowerCase().startsWith("ui"))) {
            // Some applications may not require a home page, so don't log a warning that the home page URL doesn't exist
            // unless there is a gateway UI URL, which indicates there should have been a home page URL.
            log.warn("The home page URL is not provided. Try to add apiml.service.homePageRelativeUrl property or check its value.");
        } else if (isPoorlyFormedRelativeUrl(homePageUrl)) {
            addParameterToProblemsList("homePageRelativeUrl", poorlyFormedRelativeUrlParameters);
        }
    }

    private boolean isPoorlyFormedRelativeUrl(String url) {
        // URL not existing **is not** poorly formed. A check for it existing should be done elsewhere.
        return url != null && !url.startsWith("/");
    }

    private boolean isInvalid(String value) {
        return value == null || value.isEmpty() || value.contains(UNSET_VALUE_STRING);
    }

    private boolean isInvalid(char[] value) {
        return value == null || value.length == 0 || Chars.indexOf(value, UNSET_VALUE_CHAR_ARRAY) >= 0;
    }

    private void addParameterToProblemsList(String parameter, List<String> problemParameters) {
        problemParameters.add(parameter);
    }
}
