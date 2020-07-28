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

    private List<String> missingSslParameters = new ArrayList<>();
    private List<String> missingRoutesParameters = new ArrayList<>();

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
            if (isInvalid(route.getGatewayUrl())) {
                createListOfMissingRoutesParameters("gatewayUrl", missingRoutesParameters);
            }
            if (isInvalid(route.getServiceUrl())) {
                createListOfMissingRoutesParameters("serviceUrl", missingRoutesParameters);
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
            createListOfMissingSslParameters("protocol", missingSslParameters);
        }
        if (isInvalid(ssl.getTrustStore())) {
            createListOfMissingSslParameters("trustStore", missingSslParameters);
        }
        if (isInvalid(ssl.getKeyStore())) {
            createListOfMissingSslParameters("keyStore", missingSslParameters);
        }
        if (isInvalid(ssl.getKeyAlias())) {
            createListOfMissingSslParameters("keyAlias", missingSslParameters);
        }
        if (isInvalid(ssl.getKeyStoreType())) {
            createListOfMissingSslParameters("keyStoreType", missingSslParameters);
        }
        if (isInvalid(ssl.getTrustStoreType())) {
            createListOfMissingSslParameters("trustStoreType", missingSslParameters);
        }
        if (isInvalid(ssl.getTrustStorePassword())) {
            if (isInvalid(ssl.getTrustStoreType()) ||
                (!isInvalid(ssl.getTrustStoreType()) && !ssl.getTrustStoreType().equals("JCERACFKS"))) {
                createListOfMissingSslParameters("trustStorePassword", missingSslParameters);
            }
        }
        if (isInvalid(ssl.getKeyStorePassword())) {
            if (isInvalid(ssl.getKeyStoreType()) ||
                (!isInvalid(ssl.getKeyStoreType()) && !ssl.getKeyStoreType().equals("JCERACFKS"))) {
                createListOfMissingSslParameters("keyStorePassword", missingSslParameters);
            }
        }
        if (isInvalid(ssl.getKeyPassword())) {
            createListOfMissingSslParameters("keyPassword", missingSslParameters);
        }
        if (ssl.getEnabled() == null) {
            createListOfMissingSslParameters("enabled", missingSslParameters);
        }
        if (!missingSslParameters.isEmpty()) {
            throw new MetadataValidationException(String.format("SSL parameters ** %s ** are missing or were not replaced by the system properties.", String.join(", ", missingSslParameters)));
        }
    }

    private boolean isInvalid(String value) {
        return value == null || value.isEmpty() || value.contains(UNSET_VALUE_STRING);
    }

    private boolean isInvalid(char[] value) {
        return value == null || value.length == 0 || Chars.indexOf(value, UNSET_VALUE_CHAR_ARRAY) >= 0;
    }

    private void createListOfMissingSslParameters(String parameter, List parameters) {
        parameters.add(parameter);
    }

    private void createListOfMissingRoutesParameters(String parameter, List parameters) {
        parameters.add(parameter);
    }

}
