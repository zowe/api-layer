/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.gateway.apidoc.reader.ApiDocReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationOnboardServiceTest {

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;


    private static final ApiDocReader apiDocReader = new ApiDocReader();


    @Test
    void whenCheckingOnboardedService() {
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));
        assertFalse(verificationOnboardService.checkOnboarding("Test"));
        assertTrue(verificationOnboardService.checkOnboarding("OnboardedService"));
    }

    @Test
    void whenRetrievingSwaggerUrl() {
        final String swaggerUrl = "https://hostname/sampleclient/api-doc";
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", swaggerUrl);
        assertEquals(swaggerUrl, verificationOnboardService.FindSwaggerUrl(metadata));
    }


    @Test
    void whenRetrievingEmptySwaggerUrl() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", null);
        assertEquals("", verificationOnboardService.FindSwaggerUrl(metadata));
    }

    @Nested
    class givenSwaggerDocumentation {


        public String swaggerFromPath(String path) throws IOException {
            File file = new File(path);
            return new String(Files.readAllBytes(file.getAbsoluteFile().toPath()));
        }


        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenCorrectSwagger(String path) throws IOException {
            String sampleSwagger = swaggerFromPath(path);

            ArrayList<String> result;
            result = verificationOnboardService.validateConformanceToSwaggerSpecification(sampleSwagger);
            assertEquals(0, result.size());

        }


        @Test
        void whenWrongVersioningV2() throws IOException {
            ArrayList<String> result;

            String sampleSwagger2 = swaggerFromPath("src/test/resources/api-doc-v2.json");


            String brokenSwagger = sampleSwagger2.replace("2.0", "42");

            result = verificationOnboardService.validateConformanceToSwaggerSpecification(brokenSwagger);

            assertTrue(result.toString().contains("Swagger documentation is not conformant to either OpenAPI V2 nor V3"));
        }

        @Test
        void whenWrongVersioningV3() throws IOException {
            ArrayList<String> result;

            String sampleSwagger3 = swaggerFromPath("src/test/resources/api-doc.json");

            String brokenSwagger = sampleSwagger3.replace("3.0", "42");

            result = verificationOnboardService.validateConformanceToSwaggerSpecification(brokenSwagger);

            assertTrue(result.toString().contains("Swagger documentation is not conformant to either OpenAPI V2 nor V3"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenBrokenSwagger(String path) throws IOException {
            ArrayList<String> result;

            String sampleSwagger = swaggerFromPath(path);

            String brokenSwagger = sampleSwagger.substring(0, 250);

            result = verificationOnboardService.validateConformanceToSwaggerSpecification(brokenSwagger);

            assertTrue(result.toString().contains("Could not parse Swagger documentation"));
        }

    }

}

