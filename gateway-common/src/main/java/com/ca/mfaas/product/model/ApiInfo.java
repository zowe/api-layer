/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, String> generateMetadata(String serviceId) {
        Map<String, String> metadata = new HashMap<>();
        if (gatewayUrl != null) {
            metadata.put(String.format("apiml.apiInfo.%s.gatewayUrl", gatewayUrl), gatewayUrl);
        }

        if (version != null) {
            metadata.put(String.format("apiml.apiInfo.%s.version", gatewayUrl), version);
        }

        if (swaggerUrl != null) {
            try {
                new URL(swaggerUrl);
            } catch (MalformedURLException e) {
                throw new InvalidParameterException(
                    String.format("The Swagger URL \"%s\" for service %s is not valid: %s",
                        serviceId, swaggerUrl, e.getMessage()));
            }
            metadata.put(String.format("apiml.apiInfo.%s.swaggerUrl", gatewayUrl), swaggerUrl);
        }

        if (documentationUrl != null) {
            try {
                new URL(documentationUrl);
            } catch (MalformedURLException e) {
                throw new InvalidParameterException(
                    String.format("The documentation URL \"%s\" for service %s is not valid: %s",
                        serviceId, documentationUrl, e.getMessage()));
            }
            metadata.put(String.format("apiml.apiInfo.%s.documentationUrl", gatewayUrl), documentationUrl);
        }

        return metadata;
    }

}
