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

import static com.ca.mfaas.product.constants.EurekaMetadataFormat.*;

@Service
public class MetadataTranslationService {

    public void translateMetadata(Map<String, String> metadata) {
        // Version check
        String version = metadata.get(VERSION);
        if (version != null && version.equals(CURRENT_VERSION)) {
            return;
        }

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
    }
}
