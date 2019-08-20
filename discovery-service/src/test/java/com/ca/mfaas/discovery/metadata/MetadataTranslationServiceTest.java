/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.metadata;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

public class MetadataTranslationServiceTest {

    private static final String INSTANCE_NAME = "discoverableclient";

    @Test
    public void translateMetadata() {
        Map<String, String> metadata = generateMetadataV1();

        MetadataTranslationService metadataTranslationService = new MetadataTranslationService();
        metadataTranslationService.translateMetadata(metadata);

        assertThat(metadata, hasEntry(ROUTES + ".ui__v1." + ROUTES_SERVICE_URL, "/" + INSTANCE_NAME));
        assertThat(metadata, hasEntry(ROUTES + ".ui__v1." + ROUTES_GATEWAY_URL, "ui/v1"));
        assertThat(metadata, hasEntry(ROUTES + ".api__v1." + ROUTES_SERVICE_URL, "/" + INSTANCE_NAME + "/api/v1"));
        assertThat(metadata, hasEntry(ROUTES + ".api__v1." + ROUTES_GATEWAY_URL, "/api/v1"));

        assertThat(metadata, hasEntry(CATALOG_ID, "id"));
        assertThat(metadata, hasEntry(CATALOG_TITLE, "Title"));
        assertThat(metadata, hasEntry(CATALOG_DESCRIPTION, "Description"));
        assertThat(metadata, hasEntry(CATALOG_VERSION, "Version"));

        assertThat(metadata, hasEntry(SERVICE_TITLE, "service title"));
        assertThat(metadata, hasEntry(SERVICE_DESCRIPTION, "service description"));

        assertThat(metadata, hasEntry(APIS + ".0.apiId", "apiId"));
        assertThat(metadata, hasEntry(APIS + ".0.swaggerUrl", "swaggerUrl"));
        assertThat(metadata, hasEntry(APIS + ".0.documentationUrl", "documentationUrl"));
        assertThat(metadata, hasEntry(APIS + ".0.gatewayUrl", "gatewayUrl"));

        assertThat(metadata, not(hasEntry(API_INFO_BASE_PACKAGE_V1, "api info base package")));
        assertThat(metadata, not(hasEntry(API_INFO_TITLE_V1, "api info title")));
        assertThat(metadata, not(hasEntry(API_INFO_VERSION_V1, "api info version")));
        assertThat(metadata, not(hasEntry(API_INFO_DESCRIPTION_V1, "api info description")));

        assertThat(metadata, not(hasEntry(ENABLE_APIDOC_V1, "true")));
    }

    private Map<String, String> generateMetadataV1() {
        Map<String, String> metadatas = new HashMap<>();
        //routes
        metadatas.put(ROUTES_V1 + ".ui__v1." + ROUTES_SERVICE_URL_V1, "/" + INSTANCE_NAME);
        metadatas.put(ROUTES_V1 + ".ui__v1." + ROUTES_GATEWAY_URL_V1, "ui/v1");
        metadatas.put(ROUTES_V1 + ".api__v1." + ROUTES_SERVICE_URL_V1, "/" + INSTANCE_NAME + "/api/v1");
        metadatas.put(ROUTES_V1 + ".api__v1." + ROUTES_GATEWAY_URL_V1, "/api/v1");

        //catalog
        metadatas.put(CATALOG_ID_V1, "id");
        metadatas.put(CATALOG_TITLE_V1, "Title");
        metadatas.put(CATALOG_DESCRIPTION_V1, "Description");
        metadatas.put(CATALOG_VERSION_V1, "Version");

        //service
        metadatas.put(SERVICE_TITLE_V1, "service title");
        metadatas.put(SERVICE_DESCRIPTION_V1, "service description");

        //apis
        metadatas.put(APIS_V1 + ".0.apiId", "apiId");
        metadatas.put(APIS_V1 + ".0.swaggerUrl", "swaggerUrl");
        metadatas.put(APIS_V1 + ".0.documentationUrl", "documentationUrl");
        metadatas.put(APIS_V1 + ".0.gatewayUrl", "gatewayUrl");

        //api-info
        metadatas.put(API_INFO_BASE_PACKAGE_V1, "api info base package");
        metadatas.put(API_INFO_TITLE_V1, "api info title");
        metadatas.put(API_INFO_VERSION_V1, "api info version");
        metadatas.put(API_INFO_DESCRIPTION_V1, "api info description");

        //other
        metadatas.put(ENABLE_APIDOC_V1, "true");
        return metadatas;
    }
}
