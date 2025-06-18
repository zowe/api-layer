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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.filters.RequestCredentials;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

import javax.management.ServiceNotFoundException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.ApimlConstants.AUTH_FAIL_HEADER;

class ZaasSchemeTransformApiTest {

    private AuthSourceService authSourceService;
    private PassTicketService passTicketService;
    private ZaasSchemeTransformApi transformApi;
    private final MessageService messageService = new YamlMessageService("/apiml-log-messages.yml");

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        passTicketService = mock(PassTicketService.class);
        ZosmfService zosmfService = mock(ZosmfService.class);
        TokenCreationService tokenCreationService = mock(TokenCreationService.class);

        transformApi = new ZaasSchemeTransformApi(
            authSourceService,
            passTicketService,
            zosmfService,
            tokenCreationService,
            messageService
        );
    }

    @Nested
    class GivenPassticket {
        @Test
        void thenReturnsExpectedTicket() throws PassTicketException {
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
        void whenAuthSourceMissing_returnsMissingAuthError() {
            RequestCredentials credentials = mockCredentials();
            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.empty());

            var result = transformApi.passticket(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
            assertTrue(result.getHeaders().header("x-zowe-error").isEmpty());
        }

        @Test
        void whenTicketGenerationFails_writeErrorHeader() throws PassTicketException {
            RequestCredentials credentials = mockCredentials();
            AuthSource authSource = mock(AuthSource.class);
            AuthSource.Parsed parsed = mock(AuthSource.Parsed.class);

            when(parsed.getUserId()).thenReturn("USER1");
            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.parse(authSource)).thenReturn(parsed);
            when(passTicketService.generate("USER1", "app1")).thenThrow(new RuntimeException("boom"));

            var result = transformApi.passticket(credentials).block();
            assertEquals("boom", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
        }

        @Test
        void whenApplicationNameIsMissing_inPassticket_thenReturnsError() {
            RequestCredentials credentials = mockCredentialsWithAppId(null);

            var result = transformApi.passticket(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }

    }

    @Nested
    class GivenSafIdt {
        @Test
        void whenMissingAppId_returnsError() {
            RequestCredentials credentials = mockCredentials();

            var result = transformApi.safIdt(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }

        @Test
        void whenValidAuthSource_returnsToken() throws PassTicketException {
            RequestCredentials credentials = mockCredentials();

            var authSource = mock(AuthSource.class);
            var parsed = mock(AuthSource.Parsed.class);
            when(parsed.getUserId()).thenReturn("USER1");

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.parse(authSource)).thenReturn(parsed);

            var tokenCreationService = mock(TokenCreationService.class);
            when(tokenCreationService.createSafIdTokenWithoutCredentials("USER1", "app1"))
                .thenReturn("saf-idt");

            transformApi = new ZaasSchemeTransformApi(
                authSourceService,
                passTicketService,
                mock(ZosmfService.class),
                tokenCreationService,
                messageService
            );

            var result = transformApi.safIdt(credentials).block();

            assertNotNull(result);
            assertEquals("saf-idt", result.getBody().getToken());
        }

        @Test
        void whenSafIdTokenCreationFails_returnsError() throws Exception {
            RequestCredentials credentials = mockCredentials();

            var authSource = mock(AuthSource.class);
            var parsed = mock(AuthSource.Parsed.class);
            when(parsed.getUserId()).thenReturn("USER1");

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.parse(authSource)).thenReturn(parsed);

            TokenCreationService tokenCreationService = mock(TokenCreationService.class);
            when(tokenCreationService.createSafIdTokenWithoutCredentials("USER1", "app1"))
                .thenThrow(new RuntimeException("Simulated SAF IDT failure"));

            transformApi = new ZaasSchemeTransformApi(
                authSourceService,
                passTicketService,
                mock(ZosmfService.class),
                tokenCreationService,
                messageService
            );

            var result = transformApi.safIdt(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }

        @Test
        void whenApplicationNameIsMissing_inSafIdt_thenReturnsError() {
            RequestCredentials credentials = mockCredentialsWithAppId(" "); // blank

            var result = transformApi.safIdt(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }
    }

    @Nested
    class GivenZoweJwt {
        @Test
        void thenReturnsJwt() {
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

        @Test
        void whenMissingAuthSource_returnsError() {
            RequestCredentials credentials = mockCredentials();

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.empty());

            var result = transformApi.zoweJwt(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }

        @Test
        void whenJwtRetrievalFails_returnsErrorResponse() {
            RequestCredentials credentials = mockCredentials();
            AuthSource authSource = mock(AuthSource.class);

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.getJWT(authSource)).thenThrow(new RuntimeException("boom"));

            var result = transformApi.zoweJwt(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }
    }


    @Nested
    class GivenZosmf {
        @Test
        void whenValidAuthSource_returnsTokenResponse() throws ServiceNotFoundException {
            RequestCredentials credentials = mockCredentials();

            var authSource = mock(AuthSource.class);
            var parsed = mock(AuthSource.Parsed.class);

            when(authSource.getRawSource()).thenReturn("raw".toCharArray());
            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.parse(authSource)).thenReturn(parsed);

            ZaasTokenResponse mockResponse = ZaasTokenResponse.builder().token("zosmf-token").build();

            ZosmfService zosmfService = mock(ZosmfService.class);
            when(zosmfService.exchangeAuthenticationForZosmfToken(anyString(), eq(parsed)))
                .thenReturn(mockResponse);

            transformApi = new ZaasSchemeTransformApi(
                authSourceService,
                passTicketService,
                zosmfService,
                mock(TokenCreationService.class),
                messageService
            );

            var result = transformApi.zosmf(credentials).block();

            assertNotNull(result);
            assertEquals("zosmf-token", result.getBody().getToken());
        }

        @Test
        void whenAuthSourceMissing_returnsMissingAuthError() {
            RequestCredentials credentials = mockCredentials();

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.empty());

            var result = transformApi.zosmf(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }

        @Test
        void testZosmf_serviceThrowsException_returnsError() throws ServiceNotFoundException {
            RequestCredentials credentials = mockCredentials();

            var authSource = mock(AuthSource.class);
            var parsed = mock(AuthSource.Parsed.class);
            when(authSource.getRawSource()).thenReturn("raw".toCharArray());

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.parse(authSource)).thenReturn(parsed);

            ZosmfService zosmfService = mock(ZosmfService.class);
            when(zosmfService.exchangeAuthenticationForZosmfToken(any(), any()))
                .thenThrow(new RuntimeException("Simulated failure"));

            transformApi = new ZaasSchemeTransformApi(
                authSourceService,
                passTicketService,
                zosmfService,
                mock(TokenCreationService.class),
                messageService
            );

            var result = transformApi.zosmf(credentials).block();

            assertNotNull(result);
            assertNull(result.getBody());
        }

    }

    private RequestCredentials mockCredentialsWithAppId(String appId) {
        RequestCredentials credentials = mock(RequestCredentials.class);
        when(credentials.getApplId()).thenReturn(appId);
        when(credentials.getRequestURI()).thenReturn("/dummy");
        when(credentials.getCookies()).thenReturn(Map.of());
        when(credentials.getHeaders()).thenReturn(Map.of());
        return credentials;
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
