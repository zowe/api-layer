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

        assertThat(metadata, hasEntry(API_INFO + ".0.apiId", "apiId"));
        assertThat(metadata, hasEntry(API_INFO + ".0.swaggerUrl", "swaggerUrl"));
        assertThat(metadata, hasEntry(API_INFO + ".0.documentationUrl", "documentationUrl"));
        assertThat(metadata, hasEntry(API_INFO + ".0.gatewayUrl", "gatewayUrl"));

        assertThat(metadata, not(hasEntry(API_VERSION_PROPERTIES_BASE_PACKAGE_V1, "api info base package")));
        assertThat(metadata, not(hasEntry(API_VERSION_PROPERTIES_TITLE_V1, "api info title")));
        assertThat(metadata, not(hasEntry(API_VERSION_PROPERTIES_VERSION_V1, "api info version")));
        assertThat(metadata, not(hasEntry(API_VERSION_PROPERTIES_DESCRIPTION_V1, "api info description")));

        assertThat(metadata, not(hasEntry(ENABLE_APIDOC_V1, "true")));
    }

    private Map<String, String> generateMetadataV1() {
        Map<String, String> metadata = new HashMap<>();
        //routes
        metadata.put(ROUTES_V1 + ".ui__v1." + ROUTES_SERVICE_URL_V1, "/" + INSTANCE_NAME);
        metadata.put(ROUTES_V1 + ".ui__v1." + ROUTES_GATEWAY_URL_V1, "ui/v1");
        metadata.put(ROUTES_V1 + ".api__v1." + ROUTES_SERVICE_URL_V1, "/" + INSTANCE_NAME + "/api/v1");
        metadata.put(ROUTES_V1 + ".api__v1." + ROUTES_GATEWAY_URL_V1, "/api/v1");

        //catalog
        metadata.put(CATALOG_ID_V1, "id");
        metadata.put(CATALOG_TITLE_V1, "Title");
        metadata.put(CATALOG_DESCRIPTION_V1, "Description");
        metadata.put(CATALOG_VERSION_V1, "Version");

        //service
        metadata.put(SERVICE_TITLE_V1, "service title");
        metadata.put(SERVICE_DESCRIPTION_V1, "service description");

        //apis
        metadata.put(API_INFO + ".0.apiId", "apiId");
        metadata.put(API_INFO + ".0.swaggerUrl", "swaggerUrl");
        metadata.put(API_INFO + ".0.documentationUrl", "documentationUrl");
        metadata.put(API_INFO + ".0.gatewayUrl", "gatewayUrl");

        //api-info
        metadata.put(API_VERSION_PROPERTIES_BASE_PACKAGE_V1, "api info base package");
        metadata.put(API_VERSION_PROPERTIES_TITLE_V1, "api info title");
        metadata.put(API_VERSION_PROPERTIES_VERSION_V1, "api info version");
        metadata.put(API_VERSION_PROPERTIES_DESCRIPTION_V1, "api info description");

        //other
        metadata.put(ENABLE_APIDOC_V1, "true");
        return metadata;
    }
}
