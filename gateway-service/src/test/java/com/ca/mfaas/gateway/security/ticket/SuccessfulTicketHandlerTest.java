/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.ticket;

import com.ca.mfaas.passticket.PassTicketService;
import com.ca.apiml.security.common.ticket.TicketRequest;
import com.ca.apiml.security.common.token.TokenAuthentication;
import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.yaml.YamlMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.UnsupportedEncodingException;

import static com.ca.mfaas.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID;
import static com.ca.mfaas.passticket.PassTicketService.DefaultPassTicketImpl.ZOWE_DUMMY_PASS_TICKET_PREFIX;
import static org.junit.Assert.*;

public class SuccessfulTicketHandlerTest {
    private static final String TOKEN = "token";
    private static final String USER = "user";
    private static final String APPLICATION_NAME = "app";

    private final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
    private final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageService messageService = new YamlMessageService("/gateway-messages.yml");
    private final PassTicketService passTicketService = new PassTicketService();
    private final SuccessfulTicketHandler successfulTicketHandlerHandler = new SuccessfulTicketHandler(mapper, passTicketService, messageService);
    private final TokenAuthentication tokenAuthentication = new TokenAuthentication(USER, TOKEN);

    @Before
    public void setUp() {
        httpServletResponse.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        assertNotEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
    }

    @Test
    public void shouldReturnDummyPassTicket() throws JsonProcessingException, UnsupportedEncodingException {
        httpServletRequest.setContent(mapper.writeValueAsBytes(new TicketRequest(APPLICATION_NAME)));

        successfulTicketHandlerHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
        assertTrue(httpServletResponse.getContentAsString().contains(ZOWE_DUMMY_PASS_TICKET_PREFIX));
        assertTrue(httpServletResponse.isCommitted());
    }

    @Test
    public void shouldFailWhenNoApplicationName() throws UnsupportedEncodingException, JsonProcessingException {
        successfulTicketHandlerHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.BAD_REQUEST.value(), httpServletResponse.getStatus());
        assertTrue(httpServletResponse.getContentAsString().contains("ZWEAG140E"));
        assertTrue(httpServletResponse.isCommitted());

        httpServletRequest.setContent(mapper.writeValueAsBytes(new TicketRequest("")));
        httpServletResponse.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        successfulTicketHandlerHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.BAD_REQUEST.value(), httpServletResponse.getStatus());
        assertTrue(httpServletResponse.getContentAsString().contains("ZWEAG140E"));
        assertTrue(httpServletResponse.isCommitted());
    }

    @Test
    public void shouldFailWhenGenerationFails() throws JsonProcessingException, UnsupportedEncodingException {
        httpServletRequest.setContent(mapper.writeValueAsBytes(new TicketRequest(UNKNOWN_APPLID)));

        successfulTicketHandlerHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.BAD_REQUEST.value(), httpServletResponse.getStatus());
        assertTrue(httpServletResponse.getContentAsString().contains("ZWEAG141E"));
        assertTrue(httpServletResponse.isCommitted());
    }
}
