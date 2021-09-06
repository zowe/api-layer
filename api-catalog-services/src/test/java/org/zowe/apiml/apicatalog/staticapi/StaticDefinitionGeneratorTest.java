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

import org.junit.jupiter.api.*;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaticDefinitionGeneratorTest {

    @InjectMocks
    private StaticDefinitionGenerator staticDefinitionGenerator;

    @Nested
    class WhenStaticDefinitionGenerationResponse {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(staticDefinitionGenerator,"staticApiDefinitionsDirectories","config/local/api-defs");
        }

        @Test
        void givenRequestWithInvalidServiceId_thenThrow400() throws IOException {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            StaticAPIResponse actualResponse = staticDefinitionGenerator.generateFile("services: \\n  ", "");
            StaticAPIResponse expectedResponse = new StaticAPIResponse(400, "The service ID format is not valid.");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenValidRequest_thenThrowExceptionWithCorrectPath() {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            assertThrows(IOException.class, () ->
                staticDefinitionGenerator.generateFile("services: \\n serviceId: service\\n ", "service"));
        }

        @Test
        void givenHttpValidRequest_thenThrowExceptionWithCorrectPath() {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            assertThrows(IOException.class, () ->
                staticDefinitionGenerator.generateFile("services: \\n serviceId: service\\n ", "service"));
        }

    }


    @Nested
    class WhenStaticDefinitionOverrideResponse {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(staticDefinitionGenerator,"staticApiDefinitionsDirectories","config/local/api-defs");
        }

        @Test
        void givenInvalidRequest_thenThrowException() throws IOException {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            StaticAPIResponse actualResponse = staticDefinitionGenerator.overrideFile("services: \\n  ", "");
            StaticAPIResponse expectedResponse = new StaticAPIResponse(400, "The service ID format is not valid.");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenInvalidRequest_thenThrowExceptionWithCorrectPath() {
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            Exception exception = assertThrows(IOException.class, () ->
                staticDefinitionGenerator.overrideFile("services: \\n serviceId: service\\n ", "service"));
        }
    }

}
