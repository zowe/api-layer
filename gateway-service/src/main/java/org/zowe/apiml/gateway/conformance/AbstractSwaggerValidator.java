/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;


import org.springframework.http.HttpMethod;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.*;

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
                String version = endpoint.getUrl().substring(start);
                version = version.substring(0, version.indexOf("/"));

                if (!(version.startsWith("v") && isNumber(version.substring(1)))) {
                    result.add("REST endpoint at " + result + " is not versioned according to item 8 of the conformance criteria");
                }
            }
        }
        return result;
    }

    public Set<Endpoint> getGetMethodEndpoints() {
        Set<Endpoint> result = new HashSet<>();

        for (Endpoint i : getAllEndpoints()) {
            if (i.getHttpMethods().contains(HttpMethod.GET)) {
                result.add(i);
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

    private boolean containsAll(String main, String... parts) {
        for (String i : parts) {
            if (!main.contains(i)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isNumber(String input) {
        try {
            Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
