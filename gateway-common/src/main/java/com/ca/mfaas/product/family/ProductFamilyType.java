/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.family;

/**
 * Type defining service IDs and names of basic API Mediation Layer services.
 */
public enum ProductFamilyType {
    GATEWAY("gateway", "Gateway Service for API Mediation Layer"),
    DISCOVERY_SERVICE("discovery", "Discovery Service for API Mediation Layer"),
    API_CATALOG("apicatalog", "API Catalog Service for API Mediation Layer");

    private final String description;
    private final String serviceId;

    ProductFamilyType(String serviceId, String description) {
        this.description = description;
        this.serviceId = serviceId;
    }

    public String getDescription() {
        return description;
    }

    public String getServiceId() {
        return serviceId;
    }
}
