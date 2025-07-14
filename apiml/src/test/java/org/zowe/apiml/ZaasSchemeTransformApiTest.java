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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.filters.RequestCredentials;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import reactor.test.StepVerifier;

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
    TokenCreationService tokenCreationService;
    ZosmfService zosmfService;
    RequestCredentials requestCredentials;
    AuthSource authSource;
    private static MessageService messageService;

    @BeforeAll
    static void messageService() {
        messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/zaas-log-messages.yml");
    }

    private static final String INVALID_AUTH_MSG = "ZWEAO402E The request has not been applied because it lacks valid authentication credentials.";
    private static final String MISSING_AUTH_MSG = "ZWEAG160E No authentication provided in the request";

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        when(authSourceService.isValid(any())).thenReturn(true);
        tokenCreationService = mock(TokenCreationService.class);
        zosmfService = mock(ZosmfService.class);
        passTicketService = mock(PassTicketService.class);
        requestCredentials = mockCredentials();
        authSource = mock(AuthSource.class);
        transformApi = new ZaasSchemeTransformApi(
            authSourceService,
            passTicketService,
            zosmfService,
            tokenCreationService,
            messageService
        );
    }


    @Nested
    class GivenPassticketScheme {
        @Nested
        class GivenValidAuth {

            @BeforeEach
            void setup() {

                var parsed = mock(AuthSource.Parsed.class);
                when(parsed.getUserId()).thenReturn("USER1");
                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
                when(authSourceService.parse(authSource)).thenReturn(parsed);

            }

            @Test
            void thenReturnsExpectedTicket() throws PassTicketException {

                when(passTicketService.generate("USER1", "app1")).thenReturn("ticket123");

                StepVerifier.create(transformApi.passticket(requestCredentials)).assertNext(result -> {
                    assertNotNull(result);
                    TicketResponse response = result.getBody();
                    assertNotNull(response);
                    assertEquals("USER1", response.getUserId());
                    assertEquals("ticket123", response.getTicket());
                    assertEquals("app1", response.getApplicationName());
                }).verifyComplete();
            }

            @Test
            void whenTicketGenerationFails_writeErrorHeader() throws PassTicketException {

                when(passTicketService.generate("USER1", "app1")).thenThrow(new RuntimeException("boom"));

                StepVerifier.create(transformApi.passticket(requestCredentials)).assertNext(result -> {
                    assertEquals("boom", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                }).verifyComplete();
            }
        }

        @Nested
        class GivenInvalidRequest {

            @Test
            void whenAuthSourceMissing_returnsMissingAuthError() {
                RequestCredentials credentials = mockCredentials();
                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.empty());

                StepVerifier.create(transformApi.passticket(credentials)).assertNext(result -> {
                    assertNotNull(result);
                    assertNull(result.getBody());
                    assertTrue(result.getHeaders().header("x-zowe-error").isEmpty());
                }).verifyComplete();

            }


            @Test
            void whenAuthSourceInvalid_writeErrorHeader() throws PassTicketException {

                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
                when(authSourceService.isValid(authSource)).thenReturn(false);

                StepVerifier.create(transformApi.passticket(requestCredentials)).assertNext(result -> {
                    assertEquals(INVALID_AUTH_MSG, result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();
            }

            @Test
            void whenApplicationNameIsMissing_inPassticket_thenReturnsError() {
                when(requestCredentials.getApplId()).thenReturn(null);
                StepVerifier.create(transformApi.passticket(requestCredentials)).assertNext(result -> {
                    assertEquals("ApplicationName not provided.", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();
            }

        }

    }

    @Nested
    class GivenSafIdtScheme {


        @Test
        void whenMissingAppId_returnsError() {
            when(requestCredentials.getApplId()).thenReturn(null);

            StepVerifier.create(transformApi.safIdt(requestCredentials)).assertNext(result -> {
                assertEquals("ApplicationName not provided.", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                assertNull(result.getBody());
            }).verifyComplete();
        }

        @Nested
        class GivenValidAuth {

            @BeforeEach
            void setup() {

                var parsed = mock(AuthSource.Parsed.class);
                when(parsed.getUserId()).thenReturn("USER1");
                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
                when(authSourceService.parse(authSource)).thenReturn(parsed);

            }

            @Test
            void whenValidUser_returnsToken() throws PassTicketException {

                when(tokenCreationService.createSafIdTokenWithoutCredentials("USER1", "app1"))
                    .thenReturn("saf-idt");

                StepVerifier.create(transformApi.safIdt(requestCredentials)).assertNext(result -> {
                    assertNotNull(result);
                    assertEquals("saf-idt", result.getBody().getToken());
                }).verifyComplete();
            }

            @Test
            void whenSafIdTokenCreationFails_returnsError() {

                when(tokenCreationService.createSafIdTokenWithoutCredentials("USER1", "app1"))
                    .thenThrow(new RuntimeException("Simulated SAF IDT failure"));

                StepVerifier.create(transformApi.safIdt(requestCredentials)).assertNext(result -> {
                    assertEquals("Simulated SAF IDT failure", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();
            }
        }

        @Nested
        class GivenInvalidRequest {

            @Test
            void whenAuthSourceInvalid_returnsError() {

                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
                when(authSourceService.isValid(authSource)).thenReturn(false);

                StepVerifier.create(transformApi.safIdt(requestCredentials)).assertNext(result -> {
                    assertEquals(INVALID_AUTH_MSG, result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();
            }

            @Test
            void whenApplicationNameIsMissing_inSafIdt_thenReturnsError() {
                RequestCredentials credentials = mockCredentialsWithAppId(" "); // blank

                StepVerifier.create(transformApi.safIdt(credentials)).assertNext(result -> {
                    assertEquals("ApplicationName not provided.", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();

            }

        }
    }

    @Nested
    class GivenZoweJwtScheme {

        @Nested
        class GivenValidAuth {

            @BeforeEach
            void setup() {

                var parsed = mock(AuthSource.Parsed.class);
                when(parsed.getUserId()).thenReturn("USER1");
                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
                when(authSourceService.parse(authSource)).thenReturn(parsed);

            }


            @Test
            void thenReturnsJwt() {

                when(authSourceService.getJWT(authSource)).thenReturn("jwt-token");

                StepVerifier.create(transformApi.zoweJwt(requestCredentials)).assertNext(result -> {
                    assertNotNull(result);
                    ZaasTokenResponse response = result.getBody();
                    assertNotNull(response);
                    assertEquals("jwt-token", response.getToken());
                }).verifyComplete();
            }

            @Test
            void whenJwtRetrievalFails_returnsErrorResponse() {
                when(authSourceService.getJWT(authSource)).thenThrow(new RuntimeException("boom"));

                StepVerifier.create(transformApi.zoweJwt(requestCredentials)).assertNext(result -> {
                    assertEquals(INVALID_AUTH_MSG, result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();
            }
        }

        @Test
        void whenMissingAuthSource_returnsError() {

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.empty());

            StepVerifier.create(transformApi.zoweJwt(requestCredentials)).assertNext(result -> {
                assertEquals(MISSING_AUTH_MSG, result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                assertNull(result.getBody());
            }).verifyComplete();
        }


    }


    @Nested
    class GivenZosmfScheme {

        @Nested
        class GivenValidAuth {

            AuthSource.Parsed parsed;
            ZosmfService zosmfService;

            @BeforeEach
            void setup() {

                parsed = mock(AuthSource.Parsed.class);
                zosmfService = mock(ZosmfService.class);

                when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
                when(authSourceService.parse(authSource)).thenReturn(parsed);
                when(authSource.getRawSource()).thenReturn("raw".toCharArray());
                transformApi = new ZaasSchemeTransformApi(
                    authSourceService,
                    passTicketService,
                    zosmfService,
                    mock(TokenCreationService.class),
                    messageService
                );

            }

            @Test
            void whenValidAuthSource_returnsTokenResponse() throws ServiceNotFoundException {

                ZaasTokenResponse mockResponse = ZaasTokenResponse.builder().token("zosmf-token").build();

                when(zosmfService.exchangeAuthenticationForZosmfToken(anyString(), eq(parsed)))
                    .thenReturn(mockResponse);

                StepVerifier.create(transformApi.zosmf(requestCredentials)).assertNext(result -> {

                    assertNotNull(result);
                    assertEquals("zosmf-token", result.getBody().getToken());
                }).verifyComplete();
            }

            @Test
            void testZosmf_serviceThrowsException_returnsError() throws ServiceNotFoundException {

                when(zosmfService.exchangeAuthenticationForZosmfToken(any(), any()))
                    .thenThrow(new RuntimeException("Error returned from zosmf"));
                StepVerifier.create(transformApi.zosmf(requestCredentials)).assertNext(result -> {
                    assertEquals("Error returned from zosmf", result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                    assertNull(result.getBody());
                }).verifyComplete();
            }
        }

        @Test
        void whenAuthSourceMissing_returnsMissingAuthError() {

            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.empty());
            StepVerifier.create(transformApi.zosmf(requestCredentials)).assertNext(result -> {
                assertEquals(MISSING_AUTH_MSG, result.getHeaders().header(AUTH_FAIL_HEADER).get(0));
                assertNull(result.getBody());
            }).verifyComplete();

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
