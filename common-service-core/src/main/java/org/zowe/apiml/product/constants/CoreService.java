/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.constants;

/**
 * Api Mediation Layer core service enumeration
 */
public enum CoreService {
    GATEWAY("gateway"),
    DISCOVERY("discovery"),
    API_CATALOG("apicatalog"),
    AUTH("auth");

    private final String serviceId;

    CoreService(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }
}
