/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.swagger.ApiDocTransformationException;

import java.io.IOException;
import java.nio.charset.Charset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ExampleServiceTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ExampleRepository {

        private final ExampleService exampleService = new ExampleService();

        @BeforeAll
        void test1() throws IOException {
            String apiDoc = IOUtils.toString(
                    new ClassPathResource("standalone/services/apiDocs/service2_zowe v2.0.0.json").getURL(),
                    Charset.forName("UTF-8")
            );

            ((Map<String, List<Object>>) ReflectionTestUtils.getField(exampleService, "examples")).clear();
            exampleService.generateExamples("testService", apiDoc);
        }

        @Test
        void existingGetExample() throws JSONException {
            ExampleService.Example example = exampleService.getExample("GET", "/testService/pet/findByStatus");
            assertEquals("GET", example.getMethod());
            assertEquals("/testService/pet/findByStatus", example.getPath());
            assertEquals(200, example.getResponseCode());
            JSONArray json = new JSONArray(example.getContent());
            assertEquals(1, json.length());
            assertEquals(10, json.getJSONObject(0).get("id"));
            assertEquals("doggie", json.getJSONObject(0).get("name"));
        }

        @Test
        void generateExampleDoesNotThrowException() throws IOException {

            String apiDoc = IOUtils.toString(
                new ClassPathResource("standalone/services/apiDocs/service2_zowe v2.0.0.json").getURL(),
                Charset.forName("UTF-8")
            );
             assertDoesNotThrow( () -> exampleService.generateExamples("testService", apiDoc));
        }

        @Test
        void generateExampleThrowsExceptionWhenPathIsNull() {
            OpenAPI swagger = Mockito.mock(OpenAPI.class);
            Mockito.when(swagger.getPaths()).thenReturn(null);
            assertThrows( ApiDocTransformationException.class, () -> exampleService.generateExamples("testService", " "));
        }
        @Test
        void nonExistingGetExample() {
            ExampleService.Example example = exampleService.getExample("GET", "/unkwnown");
            assertEquals(200, example.getResponseCode());
            assertEquals("{}", example.getContent());
        }

        @Test
        void replay() throws IOException {
            MockHttpServletResponse response = new MockHttpServletResponse();
            exampleService.replyExample(response, "GET", "/unknown");
            assertEquals(200, response.getStatus());
            assertEquals("{}", response.getContentAsString());
            assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getHeaders("Content-Type").get(0));
        }

    }

    @Nested
    class Example {

        @Test
        void antMatcherIsWorking() {
            ExampleService.Example example = ExampleService.Example.builder()
                .path("/some/path/{id}")
                .build();
            assertTrue(example.isMatching("/some/path/1"));
            assertTrue(example.isMatching("/some/path/abc"));
            assertFalse(example.isMatching("/some/otherPath/2"));
        }

    }

    @Nested
    class InternalMethods {

        @Nested
        class GetResponseCode {

            @Test
            void whenDefaultThenReturn200() {
                assertEquals(200, ExampleService.getResponseCode("default"));
            }

            @Test
            void whenIsNumericThenParse() {
                assertEquals(213, ExampleService.getResponseCode("213"));
            }

            @Test
            void whenIsNotNumericThenReturn0() {
                assertEquals(0, ExampleService.getResponseCode("unknown"));
            }

        }

        @Nested
        class GetFirstApiRespones {

            private Operation createOperation(String...responseCodes) {
                Map<String, ApiResponse> responses = new LinkedHashMap<>();
                for (String responseCode : responseCodes) {
                    responses.put(responseCode, mock(ApiResponse.class));
                }

                ApiResponses apiResponses = mock(ApiResponses.class);
                Operation operation = mock(Operation.class);

                doReturn(responses.entrySet()).when(apiResponses).entrySet();
                doReturn(apiResponses).when(operation).getResponses();

                return operation;
            }

            @Test
            void whenContainsSuccessResponseCode() {
                assertEquals("200", ExampleService.getFirstApiResponses(createOperation("303", "200", "400")).getKey());
            }

            @Test
            void whenDoesntContainsSuccessResponseCode() {
                assertEquals("400", ExampleService.getFirstApiResponses(createOperation("400", "401", "404")).getKey());
            }

            @Test
            void whenIsEmpty() {
                assertNull(ExampleService.getFirstApiResponses(createOperation()));
            }

        }

    }

}
