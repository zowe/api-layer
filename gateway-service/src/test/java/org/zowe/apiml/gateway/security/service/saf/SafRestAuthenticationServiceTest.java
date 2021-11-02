/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.saf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafRestAuthenticationServiceTest {
    private SafRestAuthenticationService underTest;

    private AuthenticationService authenticationService;
    private RestTemplate restTemplate;

    private PassTicketService passTicketService;
    private final String VALID_USERNAME = "am456723";

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        restTemplate = mock(RestTemplate.class);
        passTicketService = mock(PassTicketService.class);

        underTest = new SafRestAuthenticationService(restTemplate, authenticationService, passTicketService);
        underTest.authenticationUrl = "https://localhost:10013/zss/saf/generate";
        underTest.verifyUrl = "https://localhost:10013/zss/saf/verify";
    }

    @Nested
    class WhenGenerating {
        @Nested
        class GivenValidJwtToken {
            @BeforeEach
            void prepareValidToken() {
                String validToken = "validToken";
                TokenAuthentication unauthenticated = new TokenAuthentication(validToken);
                unauthenticated.setAuthenticated(true);

                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of(validToken));
                when(authenticationService.validateJwtToken(validToken)).thenReturn(unauthenticated);
            }

            @Nested
            class ReturnValidToken {
                @Test
                void givenValidResponse() {
                    String validSafToken = "validSafToken";

                    ResponseEntity<Object> response = mock(ResponseEntity.class);
                    when(restTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                    when(response.getStatusCode()).thenReturn(HttpStatus.CREATED);
                    SafRestAuthenticationService.Token responseBody = new SafRestAuthenticationService.Token();
                    responseBody.setJwt(validSafToken);
                    when(response.getBody()).thenReturn(responseBody);

                    Optional<String> token = underTest.generate(VALID_USERNAME);

                    assertThat(token.isPresent(), is(true));
                    assertThat(token.get(), is(validSafToken));
                }
            }

            @Nested
            class ReturnEmpty {
                @Test
                void givenUnauthorizedResponse() {
                    HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "statusText", new HttpHeaders(), new byte[]{}, null);
                    when(restTemplate.postForEntity(any(), any(), any())).thenThrow(exception);

                    Optional<String> token = underTest.generate(VALID_USERNAME);
                    assertThat(token.isPresent(), is(false));
                }

                @Test
                void givenBadResponse() {
                    ResponseEntity<Object> response = mock(ResponseEntity.class);
                    when(restTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

                    Optional<String> token = underTest.generate(VALID_USERNAME);
                    assertThat(token.isPresent(), is(false));
                }

                @Test
                void givenEmptyResponse() {
                    ResponseEntity<Object> response = mock(ResponseEntity.class);
                    when(restTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                    when(response.getStatusCode()).thenReturn(HttpStatus.CREATED);
                    when(response.getBody()).thenReturn(null);

                    Optional<String> token = underTest.generate(VALID_USERNAME);
                    assertThat(token.isPresent(), is(false));
                }
            }

            @Nested
            class ThrowException {
                @Test
                void givenInvalidPassticket() throws IRRPassTicketGenerationException {
                    String validSafToken = "validSafToken";

                    ResponseEntity<Object> response = mock(ResponseEntity.class);
                    when(restTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                    when(response.getStatusCode()).thenReturn(HttpStatus.CREATED);
                    SafRestAuthenticationService.Token responseBody = new SafRestAuthenticationService.Token();
                    responseBody.setJwt(validSafToken);
                    when(response.getBody()).thenReturn(responseBody);

                    when(passTicketService.generate(any(), any())).thenThrow(new IRRPassTicketGenerationException(1, 2, 3));
                    assertThrows(AuthenticationTokenException.class,
                        () -> underTest.generate(VALID_USERNAME), "Exception is not AuthenticationTokenException");
                }
            }
        }

        @Nested
        class ReturnEmpty {
            @Test
            void givenNoJwtToken() {
                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.empty());

                Optional<String> token = underTest.generate(VALID_USERNAME);
                assertThat(token.isPresent(), is(false));
            }

            @Test
            void givenInvalidJwtToken() {
                String invalidToken = "invalidToken";
                TokenAuthentication unauthenticated = new TokenAuthentication(invalidToken);
                unauthenticated.setAuthenticated(false);

                when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of(invalidToken));
                when(authenticationService.validateJwtToken(invalidToken)).thenReturn(unauthenticated);

                Optional<String> token = underTest.generate(VALID_USERNAME);
                assertThat(token.isPresent(), is(false));
            }

        }
    }

    @Nested
    class WhenVerifying {
        @Nested
        class ReturnFalse {
            @Test
            void givenNoSafToken() {
                assertThat(underTest.verify(null), is(false));
            }

            @Test
            void givenUnauthenticated() {
                HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "statusText", new HttpHeaders(), new byte[]{}, null);
                when(restTemplate.postForEntity(any(), any(), any())).thenThrow(exception);

                assertThat(underTest.verify("validSafToken"), is(false));
            }

            @Test
            void givenOtherWrongResponse() {
                ResponseEntity<Object> response = mock(ResponseEntity.class);
                when(restTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

                assertThat(underTest.verify("validSafToken"), is(false));
            }
        }

        @Nested
        class ReturnTrue {
            @Test
            void givenCorrectResponse() {
                ResponseEntity<Object> response = mock(ResponseEntity.class);
                when(restTemplate.postForEntity(any(), any(), any())).thenReturn(response);
                when(response.getStatusCode()).thenReturn(HttpStatus.OK);

                assertThat(underTest.verify("validSafToken"), is(true));
            }
        }
    }
}
