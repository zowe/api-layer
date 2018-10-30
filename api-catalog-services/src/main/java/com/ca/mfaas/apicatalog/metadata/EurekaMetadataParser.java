/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.metadata;

import com.ca.mfaas.product.model.ApiInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EurekaMetadataParser {
    public List<ApiInfo> parseApiInfo(Map<String, String> eurekaMetadata) {
        Map<String, ApiInfo> apiInfo = new HashMap<>();

        for (Entry<String, String> entry : eurekaMetadata.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            if (keys.length == 4)
                if (keys[0].equals("apiml") && keys[1].equals("apiInfo")) {
                    apiInfo.putIfAbsent(keys[2], new ApiInfo());
                    ApiInfo api = apiInfo.get(keys[2]);
                    switch (keys[3]) {
                    case "apiId":
                        api.setApiId(entry.getValue());
                        break;
                    case "gatewayUrl":
                        api.setGatewayUrl(entry.getValue());
                        break;
                    case "version":
                        api.setVersion(entry.getValue());
                        break;
                    case "swaggerUrl":
                        api.setSwaggerUrl(entry.getValue());
                        break;
                    case "documentationUrl":
                        api.setDocumentationUrl(entry.getValue());
                        break;
                    }
                }
        }

        if (apiInfo.size() == 0) {
            return null;
        } else {
            return new ArrayList<ApiInfo>(apiInfo.values());
        }
    }
}
