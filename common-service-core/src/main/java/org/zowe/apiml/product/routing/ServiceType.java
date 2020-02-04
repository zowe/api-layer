/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.routing;

import lombok.Getter;

@Getter
public enum ServiceType {
    ALL(1, "All services"),
    API(2, "API"),
    UI(3, "UI"),
    WS(4, "WS");

    private int serviceCode;
    private String name;
    ServiceType(int serviceCode, String name) {
        this.serviceCode = serviceCode;
        this.name = name;
    }
}
