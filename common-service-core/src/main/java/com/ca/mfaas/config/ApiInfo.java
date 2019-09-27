/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.config;

import com.ca.mfaas.eurekaservice.client.util.UrlUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.RandomStringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;

/**
 * Represents one API provided by a service
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApiInfo {

    @JsonProperty(required = true)
    private String apiId;
    private String gatewayUrl;
    private String version;
    private String swaggerUrl;
    private String documentationUrl;

    /**
     * Generate Eureka metadata for ApiInfo configuration
     *
     * @param serviceId the identifier of a service which ApiInfo configuration belongs
     * @return the generated Eureka metadata
     */
    //TODO: move to EurekaMetadataParser
    public Map<String, String> generateMetadata(String serviceId) {
        Map<String, String> metadata = new HashMap<>();
        String encodedGatewayUrl = UrlUtils.getEncodedUrl(gatewayUrl);

        if (gatewayUrl != null) {
            metadata.put(createMetadataKey(encodedGatewayUrl, API_INFO_GATEWAY_URL), gatewayUrl);
        }

        if (version != null) {
            metadata.put(createMetadataKey(encodedGatewayUrl, API_INFO_VERSION), version);
        }

        if (swaggerUrl != null) {
            UrlUtils.validateUrl(swaggerUrl,
                () -> String.format("The Swagger URL \"%s\" for service %s is not valid", swaggerUrl, serviceId)
            );

            metadata.put(createMetadataKey(encodedGatewayUrl, API_INFO_SWAGGER_URL), swaggerUrl);
        }

        if (documentationUrl != null) {
            UrlUtils.validateUrl(documentationUrl,
                () -> String.format("The documentation URL \"%s\" for service %s is not valid", documentationUrl, serviceId)
            );

            metadata.put(createMetadataKey(encodedGatewayUrl, API_INFO_DOCUMENTATION_URL), documentationUrl);
        }

        return metadata;
    }

    private String createMetadataKey(String encodedGatewayUrl, String url) {
        return String.format("%s.%s.%s", API_INFO, encodedGatewayUrl, url);
    }

    private String getEncodedGatewayUrl(String gatewayUrl) {
        if (gatewayUrl != null) {
            return gatewayUrl.replaceAll("\\W", "-");
        } else {
            return RandomStringUtils.randomAlphanumeric(10);
        }
    }

    private void validateUrl(String url, Supplier<String> exceptionSupplier) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new InvalidParameterException(exceptionSupplier.get() + ": " + e.getMessage());
        }
    }
}
