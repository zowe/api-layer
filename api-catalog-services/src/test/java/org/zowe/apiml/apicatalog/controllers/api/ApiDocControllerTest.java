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
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.apicatalog.services.status.ApiDocRetrievalService;
import org.zowe.apiml.apicatalog.services.status.OpenApiCompareProducer;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ApiDocControllerTest {

    private static final String API_DOC = "Some API Doc";

    private ApiDocRetrievalService mockApiDocRetrievalService;
    private OpenApiCompareProducer mockOpenApiCompareProducer;
    private ApiDocController underTest;

    @BeforeEach
    void setup() {
        mockApiDocRetrievalService = Mockito.mock(ApiDocRetrievalService.class);
        mockOpenApiCompareProducer = Mockito.mock(OpenApiCompareProducer.class);
        underTest = new ApiDocController(mockApiDocRetrievalService, mockOpenApiCompareProducer);
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
                when(mockApiDocRetrievalService.retrieveApiDoc("service", "1.0.0")).thenReturn(API_DOC);

                ResponseEntity<String> res = underTest.getApiDocInfo("service", "1.0.0");
                assertNotNull(res);
                assertEquals(API_DOC, res.getBody());
            }

            @Test
            void givenNoApiDoc_thenThrowException() {
                when(mockApiDocRetrievalService.retrieveApiDoc("service", "1.0.0")).thenThrow(new ApiDocNotFoundException("error"));
                assertThrows(ApiDocNotFoundException.class, () -> underTest.getApiDocInfo("service", "1.0.0"));
            }
        }

        @Nested
        class WhenGetApiDocVersionDefault {
            @Test
            void givenApiDocExists_thenReturnIt() {
                when(mockApiDocRetrievalService.retrieveDefaultApiDoc("service")).thenReturn(API_DOC);

                ResponseEntity<String> res = underTest.getDefaultApiDocInfo("service");
                assertNotNull(res);
                assertEquals(API_DOC, res.getBody());
            }

            @Test
            void givenNoApiDocExists_thenThrowException() {
                when(mockApiDocRetrievalService.retrieveDefaultApiDoc("service")).thenThrow(new ApiDocNotFoundException("error"));
                assertThrows(ApiDocNotFoundException.class, () -> underTest.getDefaultApiDocInfo("service"));
            }
        }

        /*@Test
        void whenGetApiDiff_thenReturnApiDiffHtml() {
            String responseString = "<html>Some Diff</html>";
            ResponseEntity<String> response = new ResponseEntity<>("<html>Some Diff</html>", HttpStatus.OK);

            when(mockApiServiceStatusService.getApiDiffInfo("service", "v1", "v2")).thenReturn(response);
            ResponseEntity<String> res = underTest.getApiDiff("service", "v1", "v2");
            assertNotNull(res);
            assertEquals(responseString, res.getBody());
        }*/

    }

}
