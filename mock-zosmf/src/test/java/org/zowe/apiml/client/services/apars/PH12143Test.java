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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

class PH12143Test {
    private static final String SERVICE = "authentication";
    private static final String USERNAME = "USER";
    private static final String PASSWORD = "validPassword";

    private PH12143 underTest;
    private HttpServletResponse mockResponse;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        List<String> usernames = Collections.singletonList(USERNAME);
        List<String> passwords = Collections.singletonList(PASSWORD);

        underTest = new PH12143(usernames, passwords, "../keystore/localhost/localhost.keystore.p12");
        mockResponse = mock(HttpServletResponse.class);
        headers = new HashMap<>();
    }

    @Nested
    class whenAuthenticating {
        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenNoAuthorization_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenEmptyAuthorization_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", "");
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenNoCredentials_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", getAuthorizationHeader(null, null));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenInvalidUsername_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", getAuthorizationHeader("baduser", PASSWORD));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenInvalidPassword_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", getAuthorizationHeader(USERNAME, "badpassword"));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenValidUserAndPassword_thenReturnJwtAndLtpa(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>("{}", HttpStatus.OK));

            headers.put("authorization", getAuthorizationHeader(USERNAME, PASSWORD));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse, times(2)).addCookie(called.capture());
            List<Cookie> cookies = called.getAllValues();

            Cookie jwt = cookies.get(0);
            assertThat(jwt.getName(), is("jwtToken"));

            Cookie ltpa = cookies.get(1);
            assertThat(ltpa.getName(), is("LtpaToken2"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenValidUserAndPassticket_thenReturnJwtAndLtpa(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>("{}", HttpStatus.OK));

            headers.put("authorization", getAuthorizationHeader(USERNAME, "PASS_TICKET"));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse, times(2)).addCookie(called.capture());

            List<Cookie> cookies = called.getAllValues();
            Cookie jwt = cookies.get(0);
            assertThat(jwt.getName(), is("jwtToken"));
        }
    }

    @Nested
    class whenDeleting {
        @Test
        void thenReturnNoContent() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "delete", Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));
        }
    }

    @Test
    void givenAuthenticationMethodNotHandled_whenApplyApar_thenReturnOriginalResult() {
        Optional<ResponseEntity<?>> previousResult = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        Optional<ResponseEntity<?>> result = underTest.apply("authentication", "default", previousResult, mockResponse, headers);
        assertThat(result, is(previousResult));
    }

    @Test
    void givenServiceNotHandled_whenApplyApar_thenReturnOriginalResult() {
        Optional<ResponseEntity<?>> previousResult = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        Optional<ResponseEntity<?>> result = underTest.apply("badservice", "", previousResult, mockResponse, headers);
        assertThat(result, is(previousResult));
    }

    private String getAuthorizationHeader(String user, String password) {
        return "Basic " + Base64.encodeBase64String((user + ":" + password).getBytes());
    }
}
