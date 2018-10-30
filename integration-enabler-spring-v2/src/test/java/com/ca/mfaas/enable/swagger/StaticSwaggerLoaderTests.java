/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.swagger;

import com.ca.mfaas.enable.api.ApiDocController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "eureka.client.enabled=false",
        "mfaas.discovery.info.swaggerLocation=swagger.json"
    },
    classes = {StaticSwaggerApplication.class}
)
public class StaticSwaggerLoaderTests {

    @Autowired
    private ApiDocController apiDocController;

    @Test
    public void givenASwaggerLocationThenLoadTheSwaggerFileNotAutoscan() throws IOException {
        String apiDoc = apiDocController.getApiDoc(null);
        Assert.assertNotNull(apiDoc);
        ObjectMapper mapper = new ObjectMapper();

        JsonNode rootNode = mapper.readValue(apiDoc, JsonNode.class);
        JsonNode description = rootNode.path("info").path("description");
        JsonNode version = rootNode.path("info").path("version");
        Assert.assertNotNull(description);
        Assert.assertNotNull(version);
        Assert.assertEquals("Static Swagger Info for Sample micro-service " +
            "showing how to enable a Spring Boot v2.x application", description.asText());
        Assert.assertEquals("1.0.1", version.asText());
    }
}
