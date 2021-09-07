/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaticDefinitionGeneratorTest {

    @InjectMocks
    private StaticDefinitionGenerator staticDefinitionGenerator;

    String configFileLocation = "../config/local/api-defs";
    String testServiceId = "test-static-def";

    @Nested
    class WhenStaticDefinitionGenerationResponse {

        @BeforeEach
        void setUp() {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            ReflectionTestUtils.setField(staticDefinitionGenerator, "staticApiDefinitionsDirectories", configFileLocation);
        }

        @Test
        void givenRequestWithInvalidServiceId_thenThrow400() throws IOException {

            StaticAPIResponse actualResponse = staticDefinitionGenerator.generateFile("services: \\n  ", "");
            StaticAPIResponse expectedResponse = new StaticAPIResponse(400, "The service ID format is not valid.");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenDeleteRequestWithInvalidServiceId_thenThrow400() throws IOException {

            StaticAPIResponse actualResponse = staticDefinitionGenerator.deleteFile("");
            StaticAPIResponse expectedResponse = new StaticAPIResponse(400, "The service ID format is not valid.");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenCreateRequestWithValidServiceId_thenStatusOK() throws IOException {
            StaticAPIResponse actualResponse = staticDefinitionGenerator.generateFile("services: \\n  ", testServiceId);
            try {
                assertEquals(201, actualResponse.getStatusCode());
            } finally {
                // cleanup
                staticDefinitionGenerator.deleteFile(testServiceId);
            }

        }

        @Test
        void givenDeleteRequestWithValidServiceId_thenStatusOK() throws IOException {
            //create file before deletion
            staticDefinitionGenerator.generateFile("services: \\n  ", testServiceId);
            StaticAPIResponse actualResponse = staticDefinitionGenerator.deleteFile(testServiceId);
            StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "The static definition file %s has been deleted by the user!");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenDeleteNonExistingFileRequest_thenStatusNotFound() throws IOException {
            //create file before deletion
            StaticAPIResponse actualResponse = staticDefinitionGenerator.deleteFile(testServiceId);
            StaticAPIResponse expectedResponse = new StaticAPIResponse(404, "The static definition file %s does not exist!");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenFileAlreadyExists_thenThrowException() throws IOException {
            staticDefinitionGenerator.generateFile("services: \\n  ", testServiceId);
            try {
                assertThrows(FileAlreadyExistsException.class, () ->
                    staticDefinitionGenerator.generateFile("services: \\n serviceId: service\\n ", testServiceId));
            } finally {
                // cleanup
                staticDefinitionGenerator.deleteFile(testServiceId);
            }
        }

    }


    @Nested
    class WhenStaticDefinitionOverrideResponse {

        @BeforeEach
        void setUp() {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            ReflectionTestUtils.setField(staticDefinitionGenerator, "staticApiDefinitionsDirectories", "../config/local/api-defs");
        }

        @Test
        void givenInvalidRequest_thenThrowException() throws IOException {
            StaticAPIResponse actualResponse = staticDefinitionGenerator.overrideFile("services: \\n  ", "");
            StaticAPIResponse expectedResponse = new StaticAPIResponse(400, "The service ID format is not valid.");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenValidServiceId_thenResponseIsOK() throws IOException {

            StaticAPIResponse actualResponse = staticDefinitionGenerator.overrideFile("services: \\n  ", testServiceId);
            try {
                assertEquals(201, actualResponse.getStatusCode());
            } finally {
                // cleanup
                staticDefinitionGenerator.deleteFile(testServiceId);
            }
        }
    }

}
