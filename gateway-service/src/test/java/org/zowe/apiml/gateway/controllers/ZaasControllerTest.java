/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.ParsedTokenAuthSource;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZaasControllerTest {

    private PassTicketService passTicketService;

    private MessageService messageService;
    private ZaasController zaasController;

    private AuthSource.Parsed authSource;

    private static final String PASSTICKET = "test_passticket";

    @BeforeEach
    void setUp() throws IRRPassTicketGenerationException {
        messageService = new YamlMessageService("/gateway-messages.yml");
        passTicketService = mock(PassTicketService.class);
        when(passTicketService.generate(anyString(), anyString())).thenReturn(PASSTICKET);
        zaasController = new ZaasController(passTicketService, messageService);
    }

    @Nested
    class GivenAuthenticated {

        private static final String USER = "test_user";

        @BeforeEach
        void setUp() {
            authSource = new ParsedTokenAuthSource(USER, new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
        }

        @Test
        void whenApplNameProvided_thenPassTicketInResponse() {
            TicketRequest request = new TicketRequest("applid_test");
            ResponseEntity<Object> response = zaasController.getPassTicket(request, authSource);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            TicketResponse ticketResponse = (TicketResponse) response.getBody();
            assertNotNull(ticketResponse);
            assertEquals(PASSTICKET, ticketResponse.getTicket());
            assertEquals("applid_test", ticketResponse.getApplicationName());
            assertEquals(USER, ticketResponse.getUserId());
        }

        @Test
        void whenNoApplNameProvided_thenBadRequest() {
            TicketRequest request = new TicketRequest("");
            ResponseEntity<Object> response = zaasController.getPassTicket(request, authSource);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiMessageView errorMessage = (ApiMessageView) response.getBody();
            assertNotNull(errorMessage);
            assertEquals(1, errorMessage.getMessages().size());
            assertEquals("The 'applicationName' parameter name is missing.", errorMessage.getMessages().get(0).getMessageContent());
        }

        @Test
        void whenErrorGeneratingPassticket_thenInternalServerError() throws IRRPassTicketGenerationException {
            TicketRequest request = new TicketRequest("applid_test");
            when(passTicketService.generate(anyString(), anyString())).thenThrow(new IRRPassTicketGenerationException(8, 8, 8));
            ResponseEntity<Object> response = zaasController.getPassTicket(request, authSource);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            ApiMessageView errorMessage = (ApiMessageView) response.getBody();
            assertNotNull(errorMessage);
            assertEquals(1, errorMessage.getMessages().size());
            assertEquals("The generation of the PassTicket failed. Reason: An internal error was encountered.", errorMessage.getMessages().get(0).getMessageContent());
        }
    }

    @Nested
    class GivenNotAuthenticated {

        @BeforeEach
        void setUp() {
            authSource = new ParsedTokenAuthSource(null, null, null, null);
        }

        @Test
        void thenRespondUnauthorized() {
            TicketRequest request = new TicketRequest("applid_test");
            ResponseEntity<Object> response = zaasController.getPassTicket(request, authSource);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        }
    }
}
