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
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EurekaMetadataParserTest {

    @Test
    public void testParseApiInfo() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.shortApiInfo.1.gatewayUrl", "gatewayUrl");
        metadata.put("apiml.shortApiInfo.2.gatewayUrl", "gatewayUrl2");
        metadata.put("apiml.shortApiInfo.2.swaggerUrl", "swagger");
        metadata.put("apiml.shortApiInfo.2.documentationUrl", "doc");

        List<ApiInfo> info = new EurekaMetadataParser().parseApiInfo(metadata);
        assertEquals(2, info.size());
        assertEquals("gatewayUrl", info.get(0).getGatewayUrl());
        assertEquals("gatewayUrl2", info.get(1).getGatewayUrl());
        assertEquals("swagger", info.get(1).getSwaggerUrl());
        assertEquals("doc", info.get(1).getDocumentationUrl());
    }
}
