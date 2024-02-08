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
import org.zowe.apiml.client.model.LoginBody;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

class PH34912Test {
    private static final String SERVICE = "authentication";
    private static final String USERNAME = "USER";
    private static final String PASSWORD = "validPassword";
    private static final String NEW_PASSWORD = "newPassword";

    private PH34912 underTest;
    private HttpServletResponse mockResponse;
    private Map<String, String> headers;

    @BeforeEach
    void setUp() {
        List<String> usernames = Collections.singletonList(USERNAME);
        List<String> passwords = new ArrayList<>();
        passwords.add(PASSWORD);
        underTest = new PH34912(usernames, passwords, "../keystore/localhost/localhost.keystore.p12", 60);
        mockResponse = mock(HttpServletResponse.class);
        headers = new HashMap<>();
    }

    @Nested
    class whenAuthenticating {
        @ParameterizedTest
        @ValueSource(strings = {"create", "verify", "delete"})
        void givenNoAuthorization_thenReturnInternalServerError(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify", "delete"})
        void givenEmptyAuthorization_thenReturnInternalServerError(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

            headers.put("authorization", "");
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify", "delete"})
        void givenNoCredentials_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", getBasicAuthorizationHeader(null, null));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify", "delete"})
        void givenInvalidUsername_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", getBasicAuthorizationHeader("baduser", PASSWORD));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify", "delete"})
        void givenInvalidPassword_thenReturnUnauthorized(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            headers.put("authorization", getBasicAuthorizationHeader(USERNAME, "badpassword"));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenValidUserAndPassword_thenReturnJwtAndLtpa(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>("{}", HttpStatus.OK));

            headers.put("authorization", getBasicAuthorizationHeader(USERNAME, PASSWORD));
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

            headers.put("authorization", getBasicAuthorizationHeader(USERNAME, "PASS_TICKET"));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse, times(2)).addCookie(called.capture());

            List<Cookie> cookies = called.getAllValues();
            Cookie jwt = cookies.get(0);
            assertThat(jwt.getName(), is("jwtToken"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"create", "verify"})
        void givenValidLtpaCookie_thenReturnJwtAndLtpa(String method) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>("{}", HttpStatus.OK));

            headers.put("cookie", getLtpaCookieHeader());
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, method, Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse, times(2)).addCookie(called.capture());

            List<Cookie> cookies = called.getAllValues();
            Cookie jwt = cookies.get(0);
            assertThat(jwt.getName(), is("jwtToken"));
        }

        @ParameterizedTest
        @ValueSource(strings = {PASSWORD, "PASS_TICKET"})
        void givenValidCredentials_whenDelete_thenReturnNoContent(String password) {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));
            headers.put("authorization", getBasicAuthorizationHeader(USERNAME, password));

            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "delete", Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));
        }

        @Test
        void givenValidLtpaCookie_whenDelete_thenReturnNoContent() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));
            headers.put("cookie", getLtpaCookieHeader());

            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "delete", Optional.empty(), mockResponse, headers);
            assertThat(result, is(expected));
        }
    }

    @Nested
    class whenApplyApar {
        @Test
        void givenAuthenticationMethodNotHandled_thenReturnOriginalResult() {
            Optional<ResponseEntity<?>> previousResult = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "default", previousResult, mockResponse, headers);
            assertThat(result, is(previousResult));
        }

        @Test
        void givenServiceNotHandled_thenReturnOriginalResult() {
            Optional<ResponseEntity<?>> previousResult = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

            Optional<ResponseEntity<?>> result = underTest.apply("badservice", "", previousResult, mockResponse, headers);
            assertThat(result, is(previousResult));
        }
    }

    @Nested
    class whenRetrieveJwtKeys {
        @Test
        void thenOkIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("jwtKeys", "get", null, null, null);

            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.OK));
        }
    }

    @Nested
    class WhenChangePassword {
        @Test
        void givenValidBody_thenChangePassword() {
            LoginBody loginBody = new LoginBody(USERNAME, PASSWORD, NEW_PASSWORD);
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "update", Optional.empty(), mockResponse, headers, loginBody);
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.OK));
        }

        @Test
        void givenEmptyBody_thenReturnInternalError() {
            LoginBody loginBody = new LoginBody(USERNAME, PASSWORD, PASSWORD);
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "update", Optional.empty(), mockResponse, headers, loginBody);
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        @Test
        void givenEmptyBody_thenReturnBadRequest() {
            LoginBody loginBody = new LoginBody(USERNAME, null, null);
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "update", Optional.empty(), mockResponse, headers, loginBody);
            assertThat(result.isPresent(), is(true));
            assertThat(result.get().getStatusCode(), is(HttpStatus.BAD_REQUEST));
        }
    }

    private String getBasicAuthorizationHeader(String user, String password) {
        return "Basic " + Base64.encodeBase64String((user + ":" + password).getBytes());
    }

    private String getLtpaCookieHeader() {
        return "LtpaToken2=token";
    }

}
