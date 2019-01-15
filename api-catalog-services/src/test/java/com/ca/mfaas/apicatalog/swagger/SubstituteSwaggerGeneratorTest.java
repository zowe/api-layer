/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.product.model.ApiInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.PortType;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SubstituteSwaggerGeneratorTest {

    @Test
    public void testParseApiInfo() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.1.gatewayUrl", "api/v1");
        metadata.put("apiml.apiInfo.1.documentationUrl", "https://doc.ca.com/api");

        List<ApiInfo> info = new EurekaMetadataParser().parseApiInfo(metadata);

        InstanceInfo service = InstanceInfo.Builder.newBuilder().setAppName("serviceId").setHostName("localhost")
                .setSecurePort(8080).enablePort(PortType.SECURE, true).setMetadata(metadata).build();

        InstanceInfo gateway = InstanceInfo.Builder.newBuilder().setAppName("gateway").setHostName("localhost")
                .setSecurePort(10010).enablePort(PortType.SECURE, true).build();

        InstanceInfo httpGateway = InstanceInfo.Builder.newBuilder().setAppName("gateway").setHostName("localhost")
                .setPort(10010).enablePort(PortType.UNSECURE, true).build();

        ResponseEntity<String> result = new SubstituteSwaggerGenerator().generateSubstituteSwaggerForService(gateway,
                service, info.get(0));
        assertTrue(result.getBody().contains("https://doc.ca.com/api"));

        result = new SubstituteSwaggerGenerator().generateSubstituteSwaggerForService(httpGateway,
                service, info.get(0));
        assertTrue(result.getBody().contains("https://doc.ca.com/api"));

    }
}
