/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.config;


/**
 * Enum representing internal API Layer services
 * Used to identify services that are part of API Layer itself
 */
public enum ApiLayerServices {
    DISCOVERY("discovery"),
    GATEWAY("gateway"),
    APIML("apiml"),
    ZAAS("zaas"),
    API_CATALOG("apicatalog"),
    CACHING_SERVICE("cachingservice");

    private final String serviceId;

    ApiLayerServices(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public static boolean isApiLayerService(String serviceId) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return false;
        }
        String normalizedServiceId = serviceId.toLowerCase().trim();
        for (ApiLayerServices service : ApiLayerServices.values()) {
            if (service.serviceId.equals(normalizedServiceId)) {
                return true;
            }
        }
        return false;
    }
}
