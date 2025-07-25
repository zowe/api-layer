/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.apicatalog.config.BeanConfig;
import org.zowe.apiml.apicatalog.controllers.handlers.StaticDefinitionControllerExceptionHandler;
import org.zowe.apiml.apicatalog.staticapi.StaticAPIResponse;
import org.zowe.apiml.apicatalog.staticapi.StaticDefinitionGenerator;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
    StaticDefinitionControllerMicroservice.class,
    StaticDefinitionControllerExceptionHandler.class,
    BeanConfig.class
})
@WebFluxTest(controllers = StaticDefinitionControllerMicroservice.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
class StaticDefinitionControllerTest {

    private static final String STATIC_DEF_GENERATE_ENDPOINT = "/apicatalog/static-api/generate";
    private static final String STATIC_DEF_OVERRIDE_ENDPOINT = "/apicatalog/static-api/override";
    private static final String STATIC_DEF_DELETE_ENDPOINT = "/apicatalog/static-api/delete";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private StaticDefinitionGenerator staticDefinitionGenerator;

    @Nested
    class GivenIOException {

        @Nested
        class whenCallStaticGenerationAPI {

            @Test
            void thenResponseShouldBe500WithSpecificMessage() throws Exception {
                when(staticDefinitionGenerator.generateFile("services", "test")).thenThrow(
                    new IOException("Exception")
                );

                webTestClient.post().uri(STATIC_DEF_GENERATE_ENDPOINT)
                    .header("Service-Id", "test")
                    .bodyValue("services")
                .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .expectBody()
                        .jsonPath("$.messages").value(hasSize(1))
                        .jsonPath("$.messages[0].messageType").value(equalTo("ERROR"))
                        .jsonPath("$.messages[0].messageNumber").value(equalTo("ZWEAC709E"))
                        .jsonPath("$.messages[0].messageContent").value(equalTo("Static definition generation failed, caused by exception: java.io.IOException: Exception"))
                        .jsonPath("$.messages[0].messageKey").value(equalTo("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed"));
            }

        }

    }

    @Nested
    class GivenRequestWithNoContent {

        @Nested
        class whenCallStaticGenerationAPI {

            @Test
            void thenResponseIs400() {
                webTestClient.post().uri(STATIC_DEF_GENERATE_ENDPOINT).exchange()
                    .expectStatus().isBadRequest();
            }

        }

    }

    @Nested
    class GivenFileAlreadyExistsException {

        @Nested
        class whenCallStaticGenerationAPI {

            @Test
            void thenResponseShouldBe409WithSpecificMessage() throws Exception {
                when(staticDefinitionGenerator.generateFile("invalid", "test")).thenThrow(
                    new FileAlreadyExistsException("Exception")
                );

                webTestClient.post().uri(STATIC_DEF_GENERATE_ENDPOINT)
                    .header("Service-Id", "test")
                    .bodyValue("invalid")
                .exchange()
                    .expectBody()
                        .jsonPath("$.messages").value(hasSize(1))
                        .jsonPath("$.messages[0].messageType").value(equalTo("ERROR"))
                        .jsonPath("$.messages[0].messageNumber").value(equalTo("ZWEAC709E"))
                        .jsonPath("$.messages[0].messageContent").value(equalTo("Static definition generation failed, caused by exception: java.nio.file.FileAlreadyExistsException: Exception"))
                        .jsonPath("$.messages[0].messageKey").value(equalTo("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed"));
            }

        }

    }

    @Nested
    class GivenRequestWithValidContent {

        @Nested
        class whenCallStaticGenerationAPI {

            @Test
            void thenResponseIs201() throws Exception {
                String payload = """
                    services:
                      - serviceId: service" +
                        title: a" +
                        description: description" +
                        instanceBaseUrls:" +
                          - a" +
                        routes:" +
                    """;
                when(staticDefinitionGenerator.generateFile(payload, "service")).thenReturn(
                    new StaticAPIResponse(201, "This is body")
                );

                webTestClient.post().uri(STATIC_DEF_GENERATE_ENDPOINT)
                    .header("Service-Id", "service")
                    .bodyValue(payload)
                .exchange()
                    .expectStatus().is2xxSuccessful();
            }

        }

        @Nested
        class whenCallStaticOverrideAPI {

            @Test
            void thenResponseIs201() throws Exception {
                String payload = "\"services:\\n  - serviceId: service\\n    title: a\\n    description: description\\n    instanceBaseUrls:\\n      - a\\n   routes:\\n ";
                when(staticDefinitionGenerator.overrideFile(payload, "service")).thenReturn(
                    new StaticAPIResponse(201, "This is body")
                );

                webTestClient.post().uri(STATIC_DEF_OVERRIDE_ENDPOINT)
                    .header("Service-Id", "service")
                    .bodyValue(payload)
                .exchange()
                    .expectStatus().is2xxSuccessful();
            }

        }

        @Nested
        class WhenCallDelete {

            @Test
            void givenValidId_thenResponseIsOK() throws Exception {
                when(staticDefinitionGenerator.deleteFile("test-service")).thenReturn(new StaticAPIResponse(201, "OK"));
                webTestClient.delete().uri(STATIC_DEF_DELETE_ENDPOINT)
                    .header("Service-Id", "test-service")
                .exchange()
                    .expectStatus().is2xxSuccessful();
            }

        }

    }

}
