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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatorFactoryTest {

    final String DUMMY_SERVICE_ID = "sampleservice";

    @Mock
    GatewayConfigProperties gatewayConfigProperties;

    public String swaggerFromPath(String path) throws IOException {
        File file = new File(path);
        return new String(Files.readAllBytes(file.getAbsoluteFile().toPath()));
    }


    @Nested
    class GivenSwaggerDocumentation {

        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenCorrectSwagger_thenNoMessages(String path) throws IOException {
            String sampleSwagger = swaggerFromPath(path);

            List<String> result;
            result = ValidatorFactory.parseSwagger(sampleSwagger, null, gatewayConfigProperties, DUMMY_SERVICE_ID).getMessages();
            assertEquals(0, result.size());

        }


        @Test
        void whenWrongVersioningV2_thenNonconformant() throws IOException {

            String sampleSwagger2 = swaggerFromPath("src/test/resources/api-doc-v2.json");


            String brokenSwagger = sampleSwagger2.replace("2.0", "42");

            Exception e = assertThrows(ValidationException.class, () -> ValidatorFactory.parseSwagger(brokenSwagger, null, gatewayConfigProperties, DUMMY_SERVICE_ID));

            assertTrue(e.getMessage().contains("Swagger documentation is not conformant to either OpenAPI V2 nor V3"));
        }

        @Test
        void whenWrongVersioningV3_thenNonconformant() throws IOException {

            String sampleSwagger3 = swaggerFromPath("src/test/resources/api-doc.json");

            String brokenSwagger = sampleSwagger3.replace("3.0", "42");

            Exception e = assertThrows(ValidationException.class, () -> ValidatorFactory.parseSwagger(brokenSwagger, null, gatewayConfigProperties, DUMMY_SERVICE_ID));

            assertTrue(e.getMessage().contains("Swagger documentation is not conformant to either OpenAPI V2 nor V3"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenBrokenSwagger_thenDoesntParse(String path) throws IOException {

            String sampleSwagger = swaggerFromPath(path);

            String brokenSwagger = sampleSwagger.substring(0, 250);

            Exception e = assertThrows(ValidationException.class, () -> ValidatorFactory.parseSwagger(brokenSwagger, null, gatewayConfigProperties, DUMMY_SERVICE_ID));

            assertTrue(e.getMessage().contains("Could not parse Swagger documentation"));
        }

    }


    @Nested
    class GivenMetadataAndEndpoints {

        HashMap<String, String> metadata;

        @BeforeEach
        void setup() {
            metadata = new HashMap<>();
            metadata.put("apiml.apiInfo.0.version", "1.0.0");
            metadata.put("apiml.apiInfo.0.documentationUrl", "https://zowe.github.io/docs-site/");
            metadata.put("apiml.apiInfo.0.apiId", "zowe.apiml.sampleservice");
            metadata.put("apiml.apiInfo.0.gatewayUrl", "/api/v1");
            metadata.put("apiml.routes.api_v1.gatewayUrl", "/api/v1");
            metadata.put("apiml.routes.api_v1.serviceUrl", "/sampleservice");

        }

        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenSwagger_thenCorrectlyParses(String path) throws IOException {
            when(gatewayConfigProperties.getHostname()).thenReturn("hostname");
            when(gatewayConfigProperties.getScheme()).thenReturn("https");

            String sampleSwagger = swaggerFromPath(path);

            AbstractSwaggerValidator result = ValidatorFactory.parseSwagger(sampleSwagger, metadata, gatewayConfigProperties, DUMMY_SERVICE_ID);
            Set<Endpoint> endpoints = result.getAllEndpoints();
            assertFalse(endpoints.isEmpty());
            assertTrue(endpoints.iterator().next().getUrl().startsWith("https://hostname/sampleservice/"));
            List<String> problems = result.getProblemsWithEndpointUrls();
            assertTrue(problems.isEmpty());
        }


        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenBadVersion_thenCorrectlyParses(String path) throws IOException {
            metadata.put("apiml.apiInfo.0.gatewayUrl", "/api/x1");
            metadata.put("apiml.routes.api_v1.gatewayUrl", "/api/z1");

            when(gatewayConfigProperties.getHostname()).thenReturn("hostname");
            when(gatewayConfigProperties.getScheme()).thenReturn("https");

            String sampleSwagger = swaggerFromPath(path);

            if (sampleSwagger.contains("sampleservice/api/v1")) {
                sampleSwagger = sampleSwagger.replace("sampleservice/api/v1", "sampleservice/x1");
            }

            AbstractSwaggerValidator result = ValidatorFactory.parseSwagger(sampleSwagger, metadata, gatewayConfigProperties, DUMMY_SERVICE_ID);
            Set<Endpoint> endpoints = result.getAllEndpoints();
            assertFalse(endpoints.isEmpty());
            assertTrue(endpoints.iterator().next().getUrl().startsWith("https://hostname/sampleservice/"));
            List<String> problems = result.getProblemsWithEndpointUrls();
            assertTrue(problems.toString().contains("is not versioned according to item 8 of the conformance criteria"));
        }


        @ParameterizedTest
        @ValueSource(strings = {"src/test/resources/api-doc-v2.json", "src/test/resources/api-doc.json"})
        void whenMissingApi_thenCorrectlyParses(String path) throws IOException {
            metadata.put("apiml.apiInfo.0.gatewayUrl", "/v1");
            metadata.put("apiml.routes.api_v1.gatewayUrl", "/v1");

            when(gatewayConfigProperties.getHostname()).thenReturn("hostname");
            when(gatewayConfigProperties.getScheme()).thenReturn("https");

            String sampleSwagger = swaggerFromPath(path);

            if (sampleSwagger.contains("sampleservice/api/v1")) {
                sampleSwagger = sampleSwagger.replace("sampleservice/api/v1", "sampleservice/v1");
            }

            AbstractSwaggerValidator result = ValidatorFactory.parseSwagger(sampleSwagger, metadata, gatewayConfigProperties, DUMMY_SERVICE_ID);
            Set<Endpoint> endpoints = result.getAllEndpoints();
            assertFalse(endpoints.isEmpty());

            assertTrue(endpoints.iterator().next().getUrl().startsWith("https://hostname/sampleservice"));
            List<String> problems = result.getProblemsWithEndpointUrls();
            assertTrue(problems.toString().contains("missing /api/"));
        }


        @ParameterizedTest
        @ValueSource(strings = {"get", "post", "delete", "patch", "head", "options", "delete", "put"})
        void whenSwagger2andDifferentOperations_thenCorrectlyParses(String operation) throws IOException {
            when(gatewayConfigProperties.getHostname()).thenReturn("hostname");
            when(gatewayConfigProperties.getScheme()).thenReturn("https");

            String sampleSwagger = swaggerFromPath("src/test/resources/api-doc-v2.json").replace("get", operation);

            AbstractSwaggerValidator result = ValidatorFactory.parseSwagger(sampleSwagger, metadata, gatewayConfigProperties, DUMMY_SERVICE_ID);
            Set<Endpoint> endpoints = result.getAllEndpoints();
            assertFalse(endpoints.isEmpty());
            assertTrue(endpoints.iterator().next().getUrl().startsWith("https://hostname/sampleservice/"));
            List<String> problems = result.getProblemsWithEndpointUrls();
            assertTrue(problems.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"get", "post", "delete", "patch", "head", "options", "delete", "put"})
        void whenSwagger3andDifferentOperations_thenCorrectlyParses(String operation) throws IOException {
            when(gatewayConfigProperties.getHostname()).thenReturn("hostname");
            when(gatewayConfigProperties.getScheme()).thenReturn("https");

            String sampleSwagger = swaggerFromPath("src/test/resources/api-doc.json").replace("get", operation);

            AbstractSwaggerValidator result = ValidatorFactory.parseSwagger(sampleSwagger, metadata, gatewayConfigProperties, DUMMY_SERVICE_ID);
            Set<Endpoint> endpoints = result.getAllEndpoints();
            assertFalse(endpoints.isEmpty());
            assertTrue(endpoints.iterator().next().getUrl().startsWith("https://hostname/sampleservice/"));
            List<String> problems = result.getProblemsWithEndpointUrls();
            assertTrue(problems.isEmpty());
        }
    }


}
