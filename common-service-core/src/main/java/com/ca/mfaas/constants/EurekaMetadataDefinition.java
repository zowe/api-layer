/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.constants;

public final class EurekaMetadataDefinition {

    private EurekaMetadataDefinition() {
        throw new IllegalStateException("Eureka metadata constant class");
    }

    public static final String CURRENT_VERSION = "2.0.0";

    //v2
    public static final String VERSION = "version";
    public static final String ROUTES = "routes";
    public static final String ROUTES_GATEWAY_URL = "gatewayUrl";
    public static final String ROUTES_SERVICE_URL = "serviceUrl";

    public static final String CATALOG_ID = "catalog.id";
    public static final String CATALOG_VERSION = "catalog.version";
    public static final String CATALOG_TITLE = "catalog.title";
    public static final String CATALOG_DESCRIPTION = "catalog.description";

    public static final String SERVICE_TITLE = "service.title";
    public static final String SERVICE_DESCRIPTION = "service.description";

    public static final String APIS = "apis";
    public static final String APIS_API_ID = "apiId";
    public static final String APIS_GATEWAY_URL = "gatewayUrl";
    public static final String APIS_VERSION = "version";
    public static final String APIS_SWAGGER_URL = "swaggerUrl";
    public static final String APIS_DOCUMENTATION_URL = "documentationUrl";

    //v1
    public static final String ROUTES_V1 = "routed-services";
    public static final String ROUTES_GATEWAY_URL_V1 = "gateway-url";
    public static final String ROUTES_SERVICE_URL_V1 = "service-url";

    public static final String CATALOG_ID_V1 = "mfaas.discovery.catalogUiTile.id";
    public static final String CATALOG_VERSION_V1 = "mfaas.discovery.catalogUiTile.version";
    public static final String CATALOG_TITLE_V1 = "mfaas.discovery.catalogUiTile.title";
    public static final String CATALOG_DESCRIPTION_V1 = "mfaas.discovery.catalogUiTile.description";

    public static final String SERVICE_TITLE_V1 = "mfaas.discovery.service.title";
    public static final String SERVICE_DESCRIPTION_V1 = "mfaas.discovery.service.description";
    public static final String ENABLE_APIDOC_V1 = "mfaas.discovery.enableApiDoc";

    public static final String APIS_V1 = "apiml.apiInfo";

    public static final String API_INFO_BASE_PACKAGE_V1 = "mfaas.api-info.apiVersionProperties.v1.basePackage";
    public static final String API_INFO_TITLE_V1 = "mfaas.api-info.apiVersionProperties.v1.title";
    public static final String API_INFO_VERSION_V1 = "mfaas.api-info.apiVersionProperties.v1.version";
    public static final String API_INFO_DESCRIPTION_V1 = "mfaas.api-info.apiVersionProperties.v1.description";
}
