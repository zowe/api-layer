/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.constants;

public final class EurekaMetadataFormat {

    private EurekaMetadataFormat() {
        throw new IllegalStateException("Eureka metadata constant class");
    }

    public static final String CURRENT_VERSION = "2";

    //v2
    public static final String VERSION = "version";
    public static final String ROUTES = "routes";
    public static final String GATEWAY_URL = "gatewayUrl";
    public static final String SERVICE_URL = "serviceUrl";
    public static final String CATALOG_TITLE = "catalog.title";

    //v1
    public static final String V1_ROUTES = "routed-services";
    public static final String V1_GATEWAY_URL = "gateway-url";
    public static final String V1_SERVICE_URL = "service-url";
    public static final String V1_CATALOG_TITLE = "mfaas.discovery.catalogUiTile.title";
}
