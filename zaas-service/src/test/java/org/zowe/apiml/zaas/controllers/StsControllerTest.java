/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.zaas.security.mapping.NativeMapperWrapper;
import org.zowe.commons.usermap.MapperResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StsControllerTest {

    @Mock
    private PassTicketService passTicketService;

    @Mock
    private NativeMapperWrapper nativeMapper;

    @InjectMocks
    private StsController stsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stsController.registry = "testRegistry";
    }

    @Nested
    class SuccessfulRequests {

        @Test
        void shouldReturnPassTicketWhenRequestIsValid() throws Exception {
            StsController.PassTicketRequest request = new StsController.PassTicketRequest();
            request.setApplId("TESTAPP");
            request.setEmailId("test@company.com");

            MapperResponse mapperResponse = new MapperResponse("ZOSUSER", 0, 0, 0, 0);

            when(nativeMapper.getUserIDForDN("test@company.com", "testRegistry"))
                .thenReturn(mapperResponse);
            when(passTicketService.generate("ZOSUSER", "TESTAPP"))
                .thenReturn("TICKET123");

            ResponseEntity<StsController.PassTicketResponse> response =
                stsController.getPassTicket(request);

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("TICKET123", response.getBody().getPassticket());
            assertEquals("ZOSUSER", response.getBody().getTsoUserid());

            verify(nativeMapper)
                .getUserIDForDN("test@company.com", "testRegistry");
            verify(passTicketService)
                .generate("ZOSUSER", "TESTAPP");
        }

        @Test
        void shouldReturnPassTicketWhenMapperReturnsEmptyUser() throws Exception {
            StsController.PassTicketRequest request = new StsController.PassTicketRequest();
            request.setApplId("APPID");
            request.setEmailId("test@company.com");

            MapperResponse mapperResponse = new MapperResponse("", 0, 0, 0, 0);

            when(nativeMapper.getUserIDForDN(anyString(), anyString()))
                .thenReturn(mapperResponse);
            when(passTicketService.generate("", "APPID"))
                .thenReturn("TICKET123");

            ResponseEntity<StsController.PassTicketResponse> response =
                stsController.getPassTicket(request);

            assertEquals(200, response.getStatusCode().value());
            assertEquals("TICKET123", response.getBody().getPassticket());
            assertEquals("", response.getBody().getTsoUserid());
        }
    }

    @Nested
    class BadRequests {

        @Test
        void shouldReturnBadRequestWhenEmailIsBlank() throws Exception {
            StsController.PassTicketRequest request = new StsController.PassTicketRequest();
            request.setApplId("APPID");
            request.setEmailId("");

            ResponseEntity<StsController.PassTicketResponse> response =
                stsController.getPassTicket(request);

            assertEquals(400, response.getStatusCode().value());
            verifyNoInteractions(passTicketService, nativeMapper);
        }

        @Test
        void shouldReturnBadRequestWhenApplIdIsBlank() throws Exception {
            StsController.PassTicketRequest request = new StsController.PassTicketRequest();
            request.setEmailId("test@company.com");
            request.setApplId("");

            ResponseEntity<StsController.PassTicketResponse> response =
                stsController.getPassTicket(request);

            assertEquals(400, response.getStatusCode().value());
            verifyNoInteractions(passTicketService, nativeMapper);
        }
    }

    @Nested
    class FailureScenarios {

        @Test
        void shouldPropagateExceptionWhenNativeMapperFails() throws Exception {
            StsController.PassTicketRequest request = new StsController.PassTicketRequest();
            request.setApplId("APPID");
            request.setEmailId("test@company.com");

            when(nativeMapper.getUserIDForDN(anyString(), anyString()))
                .thenThrow(new RuntimeException("Mapper failed"));

            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stsController.getPassTicket(request)
            );

            assertEquals("Mapper failed", exception.getMessage());
        }
    }
}
