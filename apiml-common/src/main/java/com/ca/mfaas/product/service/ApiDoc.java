/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.service;

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

/**
 * Represents one API provided by a service
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApiDoc {
    @JsonProperty(required = true)
    private String apiId;
    private String gatewayUrl;
    private String version;
    private String swaggerUrl;
    private String documentationUrl;

    /**
     * Generate Eureka metadata for ApiDoc configuration
     *
     * @param serviceId the identifier of a service which ApiDoc configuration belongs
     * @return the generated Eureka metadata
     */
    public Map<String, String> generateMetadata(String serviceId) {
        Map<String, String> metadata = new HashMap<>();
        String encodedGatewayUrl;

        if (gatewayUrl != null) {
            encodedGatewayUrl = gatewayUrl.replaceAll("\\W", "-");
            metadata.put(String.format("apiml.apiDocs.%s.gatewayUrl", encodedGatewayUrl), gatewayUrl);
        } else {
            encodedGatewayUrl = RandomStringUtils.randomAlphanumeric(10);
        }

        if (version != null) {
            metadata.put(String.format("apiml.apiDocs.%s.version", encodedGatewayUrl), version);
        }

        if (swaggerUrl != null) {
            try {
                new URL(swaggerUrl);
            } catch (MalformedURLException e) {
                throw new InvalidParameterException(
                    String.format("The Swagger URL \"%s\" for service %s is not valid: %s",
                        swaggerUrl, serviceId, e.getMessage()));
            }
            metadata.put(String.format("apiml.apiDocs.%s.swaggerUrl", encodedGatewayUrl), swaggerUrl);
        }

        if (documentationUrl != null) {
            try {
                new URL(documentationUrl);
            } catch (MalformedURLException e) {
                throw new InvalidParameterException(
                    String.format("The documentation URL \"%s\" for service %s is not valid: %s",
                        documentationUrl, serviceId, e.getMessage()));
            }
            metadata.put(String.format("apiml.apiDocs.%s.documentationUrl", encodedGatewayUrl), documentationUrl);
        }

        return metadata;
    }
}
