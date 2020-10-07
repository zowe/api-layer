/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.swagger;

import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.config.ApiInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.PortType;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;
import static org.junit.Assert.assertTrue;

public class SubstituteSwaggerGeneratorTest {
    private static final String GATEWAY_SCHEME = "https";
    private static final String GATEWAY_HOST = "localhost:8080";
    private static final String DOC_URL = "https://doc.ca.com/api";
    private static final String HOST_NAME = "localhost";
    private static final String APP_NAME = "serviceId";

    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    @Test
    public void testParseApiInfo() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, "api/v1");
        metadata.put(API_INFO + ".1." + API_INFO_DOCUMENTATION_URL, DOC_URL);

        List<ApiInfo> info = new EurekaMetadataParser().parseApiInfo(metadata);

        InstanceInfo service = InstanceInfo.Builder.newBuilder().setAppName(APP_NAME).setHostName(HOST_NAME)
            .setSecurePort(8080).enablePort(PortType.SECURE, true).setMetadata(metadata).build();

        String result = swaggerGenerator.generateSubstituteSwaggerForService(service,
            info.get(0), GATEWAY_SCHEME, GATEWAY_HOST);
        assertTrue(result.contains(DOC_URL));
    }

    @Test
    public void testParseApiInfoWithGatewayUrlSlashes() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, "/api/v1/");
        metadata.put(API_INFO + ".1." + API_INFO_DOCUMENTATION_URL, DOC_URL);

        List<ApiInfo> info = new EurekaMetadataParser().parseApiInfo(metadata);

        InstanceInfo service = InstanceInfo.Builder.newBuilder().setAppName(APP_NAME).setHostName(HOST_NAME)
            .setSecurePort(8080).enablePort(PortType.SECURE, true).setMetadata(metadata).build();

        String result = swaggerGenerator.generateSubstituteSwaggerForService(service,
            info.get(0), GATEWAY_SCHEME, GATEWAY_HOST);
        assertTrue(result.contains(DOC_URL));
    }
}
