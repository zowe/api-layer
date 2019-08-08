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
        Map<String, String> newRoutes = metadata.entrySet().stream()
            .filter(entry -> entry.getKey().contains(V1_ROUTES))
            .collect(Collectors.toMap(entry -> entry.getKey()
                    .replace(V1_ROUTES, ROUTES)
                    .replace(V1_ROUTES_GATEWAY_URL, ROUTES_GATEWAY_URL)
                    .replace(V1_ROUTES_SERVICE_URL, ROUTES_SERVICE_URL),
                Map.Entry::getValue));
        metadata.putAll(newRoutes);
        metadata.keySet().removeIf(key -> key.contains(V1_ROUTES));

        // Catalog
        translateParameter(V1_CATALOG_ID, CATALOG_ID, metadata);
        translateParameter(V1_CATALOG_VERSION, CATALOG_VERSION, metadata);
        translateParameter(V1_CATALOG_TITLE, CATALOG_TITLE, metadata);
        translateParameter(V1_CATALOG_DESCRIPTION, CATALOG_DESCRIPTION, metadata);

        // Service
        translateParameter(V1_SERVICE_TITLE, SERVICE_TITLE, metadata);
        translateParameter(V1_SERVICE_DESCRIPTION, SERVICE_DESCRIPTION, metadata);

        // Apis
        Map<String, String> newApis = metadata.entrySet().stream()
            .filter(entry -> entry.getKey().contains(V1_APIS))
            .collect(Collectors.toMap(entry -> entry.getKey().replace(V1_APIS, APIS), Map.Entry::getValue));
        metadata.putAll(newApis);
        metadata.keySet().removeIf(key -> key.contains(V1_APIS));

        // Api-info
        metadata.remove(V1_API_INFO_BASE_PACKAGE);
        metadata.remove(V1_API_INFO_TITLE);
        metadata.remove(V1_API_INFO_VERSION);
        metadata.remove(V1_API_INFO_DESCRIPTION);

        // Other
        metadata.remove(V1_ENABLE_APIDOC);
    }

    private void translateParameter(String oldParameter, String newParameter, Map<String, String> metadata) {
        String parameterValue = metadata.get(oldParameter);
        if (parameterValue != null) {
            metadata.remove(oldParameter);
            metadata.put(newParameter, parameterValue);
        }
    }
}
