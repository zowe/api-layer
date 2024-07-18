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

import lombok.Getter;

/**
 * Api Mediation Layer core service enumeration
 */
@Getter
public enum CoreService {

    GATEWAY("gateway"),
    ZAAS("zaas"),
    DISCOVERY("discovery"),
    API_CATALOG("apicatalog"),
    AUTH("auth");

    private final String serviceId;

    CoreService(String serviceId) {
        this.serviceId = serviceId;
    }

}
