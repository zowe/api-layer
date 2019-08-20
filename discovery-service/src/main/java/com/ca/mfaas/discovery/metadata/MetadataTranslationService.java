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

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;

@Service
public class MetadataTranslationService {

    public void translateMetadata(Map<String, String> metadata) {
        // Version check
        String version = metadata.get(VERSION);
        if (version == null) {
            translateV1toV2(metadata);
        }
    }

    private void translateV1toV2(Map<String, String> metadata) {
        // Routing
        translateRoutes(metadata);

        // Catalog
        translateParameter(CATALOG_ID_V1, CATALOG_ID, metadata);
        translateParameter(CATALOG_VERSION_V1, CATALOG_VERSION, metadata);
        translateParameter(CATALOG_TITLE_V1, CATALOG_TITLE, metadata);
        translateParameter(CATALOG_DESCRIPTION_V1, CATALOG_DESCRIPTION, metadata);

        // Service
        translateParameter(SERVICE_TITLE_V1, SERVICE_TITLE, metadata);
        translateParameter(SERVICE_DESCRIPTION_V1, SERVICE_DESCRIPTION, metadata);

        // Apis
        translateApis(metadata);

        // Api-info
        metadata.remove(API_INFO_BASE_PACKAGE_V1);
        metadata.remove(API_INFO_TITLE_V1);
        metadata.remove(API_INFO_VERSION_V1);
        metadata.remove(API_INFO_DESCRIPTION_V1);

        // Other
        metadata.remove(ENABLE_APIDOC_V1);
    }

    private void translateRoutes(Map<String, String> metadata) {
        Map<String, String> newRoutes = metadata.entrySet().stream()
            .filter(
                entry -> entry.getKey().contains(ROUTES_V1)
            )
            .collect(
                Collectors.toMap(this::translateRouteMapKey, Map.Entry::getValue)
            );

        metadata.putAll(newRoutes);
        metadata.keySet().removeIf(key -> key.contains(ROUTES_V1));
    }

    private String translateRouteMapKey(Map.Entry<String, String> map) {
        return map.getKey()
            .replace(ROUTES_V1, ROUTES)
            .replace(ROUTES_GATEWAY_URL_V1, ROUTES_GATEWAY_URL)
            .replace(ROUTES_SERVICE_URL_V1, ROUTES_SERVICE_URL);
    }

    private void translateApis(Map<String, String> metadata) {
        Map<String, String> newApis = metadata.entrySet().stream()
            .filter(
                entry -> entry.getKey().contains(APIS_V1)
            )
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().replace(APIS_V1, APIS),
                    Map.Entry::getValue)
            );

        metadata.putAll(newApis);
        metadata.keySet().removeIf(key -> key.contains(APIS_V1));
    }

    private void translateParameter(String oldParameter, String newParameter, Map<String, String> metadata) {
        String parameterValue = metadata.get(oldParameter);
        if (parameterValue != null) {
            metadata.remove(oldParameter);
            metadata.put(newParameter, parameterValue);
        }
    }
}
