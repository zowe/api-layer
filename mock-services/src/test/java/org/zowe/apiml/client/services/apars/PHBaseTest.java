/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.services.apars;

import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.client.services.JwtTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PHBaseTest {
    private PHBase underTest;
    private Map<String, String> headers;
    private HttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        List<String> usernames = Collections.singletonList("USER");
        List<String> passwords = Collections.singletonList("validPassword");

        underTest = new PHBase(usernames, passwords);
        headers = new HashMap<>();
        mockResponse = mock(HttpServletResponse.class);
    }

    @Nested
    class whenInfoCalled {
        @Test
        void givenNothing_Ltpa2TokenIsntReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("information", "", Optional.empty(), mockResponse, headers);

            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.OK));
            verify(mockResponse, never()).addCookie(any());
        }

        @Test
        void givenValidAuthenticationCredentials_Ltpa2TokenIsReturned() {
            headers.put("authorization", Base64.encodeBase64String("USER:validPassword".getBytes()));

            underTest.apply("information", "", Optional.empty(), mockResponse, headers);

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse).addCookie(called.capture());

            Cookie ltpa = called.getValue();
            assertThat(ltpa.getName(), is("LtpaToken2"));
        }
    }

    @Nested
    class whenAuthenticateIsCalled {
        @Test
        void givenVerifyMethodWithNoAuthorization_returnInternalServerError() {
            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "verify", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        @Test
        void givenVerifyMethodWithEmptyAuthorization_returnInternalServerError() {
            headers.put("authorization", "");

            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "verify", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        @Test
        void givenVerifyMethodWithInvalidUser_returnUnauthorized() {
            headers.put("authorization", Base64.encodeBase64String("baduser:badpassword".getBytes()));

            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "verify", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        }

        @Test
        void givenVerifyMethodWithInvalidCookie_returnUnauthorized() {
            headers.put("cookie", "bad cookie");

            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "verify", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        }

        @Test
        void givenVerifyMethodWithValidUser_returnOkLtpa2Token() {
            headers.put("authorization", Base64.encodeBase64String("USER:validPassword".getBytes()));

            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "verify", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.OK));

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse).addCookie(called.capture());

            Cookie ltpa = called.getValue();
            assertThat(ltpa.getName(), is("LtpaToken2"));
        }

        @Test
        void givenVerifyMethodWithValidCookie_returnOkLtpa2Token() {
            headers.put("cookie", "LtpaToken2=randomValidValue");

            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "verify", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.OK));

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse).addCookie(called.capture());

            Cookie ltpa = called.getValue();
            assertThat(ltpa.getName(), is("LtpaToken2"));
        }

        @Test
        void givenUnimplementedMethod_notFoundIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "fake method", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class whenFilesAreCalled {

        @Test
        void givenProperAuthorization_validFilesAreReturned() throws Exception {

            JwtTokenService service = new JwtTokenService(60);
            String token = service.generateJwt("USER");
            headers.put("cookie", "LtpaToken2=randomValidValue;jwtToken=" + token);
            Optional<ResponseEntity<?>> result = underTest.apply("files", "", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.OK));
        }

        @Test
        void givenInvalidAuthorization_unauthorizedIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("files", "", Optional.empty(), mockResponse, headers);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        }
    }

    @Nested
    class whenRetrieveJwtKeys {
        @Test
        void thenNotFoundIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("jwtKeys", "get", null, null, null);

            assertThat(result, is(nullValue()));
        }
    }
}
