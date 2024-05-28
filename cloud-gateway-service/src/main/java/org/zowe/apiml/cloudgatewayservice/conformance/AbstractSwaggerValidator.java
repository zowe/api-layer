/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.conformance;


import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.math.NumberUtils.isCreatable;

public abstract class AbstractSwaggerValidator {

    protected final Map<String, String> metadata;
    protected final GatewayConfigProperties gatewayConfigProperties;
    protected final String serviceId;

    protected AbstractSwaggerValidator(Map<String, String> metadata, GatewayConfigProperties gatewayConfigProperties, String serviceId) {
        this.metadata = metadata;
        this.gatewayConfigProperties = gatewayConfigProperties;
        this.serviceId = serviceId;
    }

    /**
     * Returns list of issues with compliance with the OpenAPI specification
     */
    abstract List<String> getMessages();

    /**
     * Returns list of all documented endpoints
     */
    abstract Set<Endpoint> getAllEndpoints();

    /**
     * Returns list of issues with the endpoint URLs, mainly regarding the versioning part of conformance criteria
     */
    public List<String> getProblemsWithEndpointUrls() {
        Set<Endpoint> endpoints = getAllEndpoints();

        ArrayList<String> result = new ArrayList<>();

        boolean isVersionedApi = !searchMetadata(metadata, "apiml", "apiInfo", "version").isEmpty();

        for (Endpoint endpoint : endpoints) {
            if (endpoint.getHttpMethods().isEmpty()) {
                result.add("Endpoint at " + result + " does not have a specified Http method in Swagger");
            }
            if (!endpoint.getUrl().contains("/api/")) {
                result.add("REST endpoint at " + result + " is not routed according to item 8 of the conformance criteria, missing /api/");
                continue;
            }
            if (isVersionedApi) {
                int start = endpoint.getUrl().indexOf("/api/") + 5; //this is where the version starts
                String version;
                try {
                    version = endpoint.getUrl().substring(start);
                    version = version.substring(0, version.indexOf("/"));
                } catch (IndexOutOfBoundsException e) {
                    version = "";
                }

                if (!(version.startsWith("v") && isCreatable(version.substring(1)))) {
                    result.add("REST endpoint at " + result + " is not versioned according to item 8 of the conformance criteria");
                }
            }
        }
        return result;
    }

    public String searchMetadata(Map<String, String> metadata, String... partsOfKey) {
        for (Map.Entry<String, String> metadataEntry : metadata.entrySet()) {
            if (containsAll(metadataEntry.getKey(), partsOfKey)) {
                return metadataEntry.getValue();
            }
        }
        return "";
    }

    private boolean containsAll(String main, String... keywords) {
        for (String keyword : keywords) {
            if (!main.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

}
