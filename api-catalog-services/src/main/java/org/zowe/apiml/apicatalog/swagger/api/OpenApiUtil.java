/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.swagger.api;

import lombok.experimental.UtilityClass;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

@UtilityClass
public class OpenApiUtil {

    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String HARDCODED_VERSION = "/v1";
    public static final String SEPARATOR = "/";

    public static String getOpenApiLink(String serviceId, GatewayConfigProperties gatewayConfigProperties) {
        String link = gatewayConfigProperties.getScheme() + "://" + gatewayConfigProperties.getHostname()
            + SEPARATOR + CoreService.API_CATALOG.getServiceId() + CATALOG_VERSION
            + CATALOG_APIDOC_ENDPOINT + SEPARATOR + serviceId + HARDCODED_VERSION;
        return "\n\n" + SWAGGER_LOCATION_LINK + "(" + link + ")";
    }
}
