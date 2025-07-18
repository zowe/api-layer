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
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

                ResponseEntity<String> res = underTest.getApiDocInfo("service", "1.0.0").block();
                assertNotNull(res);
                assertEquals(API_DOC, res.getBody());
            }

            @Test
            void givenNoApiDoc_thenThrowException() {
                when(mockApiDocService.retrieveApiDoc("service", "1.0.0")).thenThrow(new ApiDocNotFoundException("error"));
                assertThrows(ApiDocNotFoundException.class, () -> underTest.getApiDocInfo("service", "1.0.0").block());
            }

        }

        @Nested
        class WhenGetApiDocVersionDefault {

            @Test
            void givenApiDocExists_thenReturnIt() {
                when(mockApiDocService.retrieveDefaultApiDoc("service")).thenReturn(Mono.just(API_DOC));

                ResponseEntity<String> res = underTest.getDefaultApiDocInfo("service").block();
                assertNotNull(res);
                assertEquals(API_DOC, res.getBody());
            }

            @Test
            void givenNoApiDocExists_thenThrowException() {
                when(mockApiDocService.retrieveDefaultApiDoc("service")).thenThrow(new ApiDocNotFoundException("error"));
                assertThrows(ApiDocNotFoundException.class, () -> underTest.getDefaultApiDocInfo("service").block());
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
                ResponseEntity<String> res = underTest.getApiDiff("service", "v1", "v2").block();
                assertNotNull(res);
                assertTrue(res.getBody().contains("<title>Api Change Log</title>"));
            }
        }

    }

}
