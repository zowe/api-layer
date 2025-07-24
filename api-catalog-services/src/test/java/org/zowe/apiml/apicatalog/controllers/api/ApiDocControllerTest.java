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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.compare.OpenApiDiffOptions;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiDocControllerTest {

    private static final String API_DOC = "Some API Doc";

    private ApiDocService mockApiDocService;
    private ApiDocController underTest;

    @BeforeEach
    void setup() {
        mockApiDocService = mock(ApiDocService.class);
        underTest = new ApiDocController(mockApiDocService);
    }

    @Test
    void whenCreateController_thenItIsInstantiated() {
        assertNotNull(underTest);
    }

    @Nested
    class GivenService {

        @Nested
        class WhenGetApiDocByVersion {

            @Test
            void givenApiDoc_thenReturnApiDoc() {
                when(mockApiDocService.retrieveApiDoc("service", "1.0.0")).thenReturn(Mono.just(API_DOC));

                var elapsed = StepVerifier.create(underTest.getApiDocInfo("service", "1.0.0"))
                    .assertNext(res -> {
                        assertNotNull(res);
                        assertEquals(API_DOC, res.getBody());
                    })
                    .verifyComplete();
                assertEquals(0L, elapsed.getSeconds());
            }

            @Test
            void givenNoApiDoc_thenThrowException() {
                when(mockApiDocService.retrieveApiDoc("service", "1.0.0")).thenThrow(new ApiDocNotFoundException("error"));

                var elapsed = StepVerifier.create(Mono.defer(() -> underTest.getApiDocInfo("service", "1.0.0")))
                    .expectErrorMatches(ApiDocNotFoundException.class::isInstance)
                    .verify();
                assertEquals(0L, elapsed.toSeconds());
            }

        }

        @Nested
        class WhenGetApiDocVersionDefault {

            @Test
            void givenApiDocExists_thenReturnIt() {
                when(mockApiDocService.retrieveDefaultApiDoc("service")).thenReturn(Mono.just(API_DOC));

                var elapsed = StepVerifier.create(underTest.getDefaultApiDocInfo("service"))
                    .assertNext(res -> {
                        assertNotNull(res);
                        assertEquals(API_DOC, res.getBody());
                    })
                    .verifyComplete();
                assertEquals(0L, elapsed.getSeconds());
            }

            @Test
            void givenNoApiDocExists_thenThrowException() {
                when(mockApiDocService.retrieveDefaultApiDoc("service")).thenThrow(new ApiDocNotFoundException("error"));

                var elapsed = StepVerifier.create(Mono.defer(() -> underTest.getDefaultApiDocInfo("service")))
                    .expectErrorMatches(ApiDocNotFoundException.class::isInstance)
                    .verify();
                assertEquals(0L, elapsed.toSeconds());
            }

        }

        @Test
        void whenGetApiDiff_thenReturnApiDiffHtml() {
            ChangedOpenApi changedOpenApi = new ChangedOpenApi(OpenApiDiffOptions.builder().build());
            changedOpenApi.setChangedOperations(Collections.emptyList());
            changedOpenApi.setMissingEndpoints(Collections.emptyList());
            changedOpenApi.setNewEndpoints(Collections.emptyList());
            doReturn(Mono.just("doc1")).when(mockApiDocService).retrieveApiDoc("service", "v1");
            doReturn(Mono.just("doc2")).when(mockApiDocService).retrieveApiDoc("service", "v2");

            try (MockedStatic<OpenApiCompare> openApiCompare = Mockito.mockStatic(OpenApiCompare.class)) {
                openApiCompare.when(() -> OpenApiCompare.fromContents("doc1", "doc2")).thenReturn(changedOpenApi);
                var elapsed = StepVerifier.create(underTest.getApiDiff("service", "v1", "v2"))
                    .assertNext(res -> {
                        assertNotNull(res);
                        assertTrue(res.getBody().contains("<title>Api Change Log</title>"));
                    })
                    .verifyComplete();
                assertEquals(0L, elapsed.toSeconds());
            }

        }

    }

}
