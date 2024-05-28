/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.saf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafRestAuthenticationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SafRestAuthenticationService underTest;

    private final static String VALID_USERNAME = "am456723";

    @BeforeEach
    void setUp() {
        underTest = new SafRestAuthenticationService(restTemplate);
        underTest.authenticationUrl = "https://localhost:10013/zss/saf/generate";
        underTest.verifyUrl = "https://localhost:10013/zss/saf/verify";
    }

    @Nested
    @DisplayName("When greeting")
    class WhenGeneratingTest {

        @Nested
        @DisplayName("Given valid JWT")
        class GivenValidJwtTest {

            @Nested
            @DisplayName("Return valid token")
            class ReturnValidTokenTest {

                @Test
                void givenValidResponse() {
                    String validSafToken = "validSafToken";

                    SafRestAuthenticationService.Token responseBody =
                            new SafRestAuthenticationService.Token(validSafToken, "applid");
                    ResponseEntity<SafRestAuthenticationService.Token> response =
                            new ResponseEntity<>(responseBody, HttpStatus.CREATED);
                    when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                            .thenReturn(response);

                    String token = underTest.generate(VALID_USERNAME, "password".toCharArray(), "ANYAPPL");

                    assertThat(token, is(validSafToken));
                }
            }

            @Nested
            @DisplayName("Return Exception")
            class ReturnExceptionTest {

                @Test
                void givenUnauthorizedResponse() {
                    HttpClientErrorException exception =
                            HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "statusText", new HttpHeaders(), new byte[]{}, null);
                    when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                            .thenThrow(exception);

                    assertThrows(SafIdtAuthException.class,
                            () -> underTest.generate(VALID_USERNAME, new char[1], "ANYAPPL"));
                }

                @Test
                void givenBadResponse() {
                    ResponseEntity<SafRestAuthenticationService.Token> response =
                            new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
                    when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                            .thenReturn(response);

                    assertThrows(SafIdtException.class,
                            () -> underTest.generate(VALID_USERNAME, new char[1], "ANYAPPL"));
                }

                @Test
                void givenInternalErrorResponseWithEmptyBody() {
                    ResponseEntity<SafRestAuthenticationService.Token> response =
                        new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                    when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                        .thenReturn(response);

                    assertThrows(SafIdtException.class,
                        () -> underTest.generate(VALID_USERNAME, new char[1], "ANYAPPL"));
                }

                @Test
                void givenInternalErrorResponse() {
                    String validSafToken = "validSafToken";
                    SafRestAuthenticationService.Token responseBody =
                        new SafRestAuthenticationService.Token(validSafToken, "applid");
                    ResponseEntity<SafRestAuthenticationService.Token> response =
                        new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
                    when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(SafRestAuthenticationService.Token.class)))
                        .thenReturn(response);

                    assertThrows(SafIdtException.class,
                        () -> underTest.generate(VALID_USERNAME, new char[1], "ANYAPPL"));
                }
            }
        }
    }

    @Nested
    @DisplayName("When calling Verify")
    class WhenVerifyingTest {

        @Nested
        @DisplayName("Return false")
        class ReturnFalseTest {

            @Test
            void givenNoSafToken() {
                assertThat(underTest.verify(null, null), is(false));
            }

            @Test
            void givenUnauthenticated() {
                HttpClientErrorException exception =
                        HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "statusText", new HttpHeaders(), new byte[]{}, null);
                when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(Void.class))).thenThrow(exception);

                assertThat(underTest.verify("invalidSafToken", "applid"), is(false));
            }

            @Test
            void givenOtherWrongResponse() {
                ResponseEntity<Void> response = mock(ResponseEntity.class);
                when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(Void.class))).thenReturn(response);
                when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

                assertThat(underTest.verify("validSafToken", "applid"), is(false));
            }
        }

        @Nested
        @DisplayName("Return true")
        class ReturnTrueTest {

            @Test
            void givenCorrectResponse() {
                ResponseEntity<Void> response = mock(ResponseEntity.class);
                when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(Void.class))).thenReturn(response);
                when(response.getStatusCode()).thenReturn(HttpStatus.OK);

                assertThat(underTest.verify("validSafToken", "applid"), is(true));
            }
        }

        @Nested
        @DisplayName("Return Exception")
        class ReturnExceptionTest {
            @Test
            void givenInternalErrorResponse() {
                ResponseEntity response =
                    new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
                when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(Void.class)))
                    .thenReturn(response);

                assertThrows(SafIdtException.class,
                    () -> underTest.verify("validSafToken", "applid"));
            }
        }
    }

}
