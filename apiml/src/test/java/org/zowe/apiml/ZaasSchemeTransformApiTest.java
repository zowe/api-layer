/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.filters.RequestCredentials;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZaasSchemeTransformApiTest {

    private AuthSourceService authSourceService;
    private PassTicketService passTicketService;
    private ZaasSchemeTransformApi transformApi;

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        passTicketService = mock(PassTicketService.class);
        ZosmfService zosmfService = mock(ZosmfService.class);
        TokenCreationService tokenCreationService = mock(TokenCreationService.class);
        MessageService messageService = mock(MessageService.class);

        transformApi = new ZaasSchemeTransformApi(
            authSourceService,
            passTicketService,
            zosmfService,
            tokenCreationService,
            messageService
        );
    }

    @Test
    void testPassticket_returnsExpectedTicket() throws PassTicketException {
        RequestCredentials credentials = mockCredentials();

        AuthSource authSource = mock(AuthSource.class);
        AuthSource.Parsed parsed = mock(AuthSource.Parsed.class);
        when(parsed.getUserId()).thenReturn("USER1");


        when(parsed.getUserId()).thenReturn("USER1");
        when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
        when(authSourceService.parse(authSource)).thenReturn(parsed);

        when(passTicketService.generate("USER1", "app1")).thenReturn("ticket123");
        when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
        when(authSourceService.parse(authSource)).thenReturn(parsed);
        when(passTicketService.generate("USER1", "app1")).thenReturn("ticket123");

        var result = transformApi.passticket(credentials).block();

        assertNotNull(result);
        TicketResponse response = result.getBody();
        assertNotNull(response);
        assertEquals("USER1", response.getUserId());
        assertEquals("ticket123", response.getTicket());
        assertEquals("app1", response.getApplicationName());
    }

    @Test
    void testSafIdt_missingAppId_returnsError() {
        RequestCredentials credentials = mockCredentials();

        var result = transformApi.safIdt(credentials).block();

        assertNotNull(result);
        assertNull(result.getBody());
    }

    @Test
    void testZoweJwt_returnsJwt() {
        RequestCredentials credentials = mockCredentials();

        var authSource = mock(AuthSource.class);
        when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
        when(authSourceService.getJWT(authSource)).thenReturn("jwt-token");

        var result = transformApi.zoweJwt(credentials).block();

        assertNotNull(result);
        ZaasTokenResponse response = result.getBody();
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    private RequestCredentials mockCredentials() {
        RequestCredentials credentials = mock(RequestCredentials.class);
        when(credentials.getApplId()).thenReturn("app1");
        when(credentials.getRequestURI()).thenReturn("/dummy");
        when(credentials.getCookies()).thenReturn(Map.of("JSESSIONID", "xyz"));
        when(credentials.getHeaders()).thenReturn(Map.of("authorization", new String[]{"Basic abc"}));
        return credentials;
    }

}
