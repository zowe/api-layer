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

import static com.ca.mfaas.product.constants.EurekaMetadataDefinition.*;

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
                    .replace(V1_GATEWAY_URL, GATEWAY_URL)
                    .replace(V1_SERVICE_URL, SERVICE_URL),
                Map.Entry::getValue));
        metadata.putAll(newRoutes);
        metadata.keySet().removeIf(key -> key.contains(V1_ROUTES));

        // Catalog
        translateParameter(V1_CATALOG_ID, CATALOG_ID, metadata);
        translateParameter(V1_CATALOG_VERSION, CATALOG_VERSION, metadata);
        translateParameter(V1_CATALOG_TITLE, CATALOG_TITLE, metadata);
        translateParameter(V1_CATALOG_DESCRIPTION, CATALOG_DESCRIPTION, metadata);
    }

    private void translateParameter(String oldParameter, String newParameter, Map<String, String> metadata) {
        String parameterValue = metadata.get(oldParameter);
        if (parameterValue != null) {
            metadata.remove(oldParameter);
            metadata.put(newParameter, parameterValue);
        }
    }
}
