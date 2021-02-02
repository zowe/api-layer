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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PH12143Test {
    private static final String SERVICE = "authentication";
    private static final String USERNAME = "USER";
    private static final String PASSWORD = "validPassword";

    private PH12143 underTest;
    private MockHttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        List<String> usernames = Collections.singletonList(USERNAME);
        List<String> passwords = Collections.singletonList(PASSWORD);

        underTest = new PH12143(usernames, passwords);
        mockResponse = new MockHttpServletResponse();
    }

    @Nested
    class whenCreating {
        @Test
        void givenNoAuthorization_thenReturnUnauthorized() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @Test
        void givenEmptyAuthorization_thenReturnUnauthorized() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", "");
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @Test
        void givenNoCredentials_thenReturnUnauthorized() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", getAuthorizationHeader(null, null));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @Test
        void givenInvalidUsername_thenReturnUnauthorized() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", getAuthorizationHeader("baduser", PASSWORD));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @Test
        void givenInvalidPassword_thenReturnUnauthorized() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", getAuthorizationHeader(USERNAME, "badpassword"));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @Test
        void givenValidUserAndPassword_thenReturnJwt() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", getAuthorizationHeader(USERNAME, PASSWORD));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }

        @Test
        void givenValidUserAndPassticket_thenReturnJwt() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", getAuthorizationHeader(USERNAME, "PASS_TICKET"));
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

            assertThat(result, is(expected));
        }
    }

    @Nested
    class whenVerifying {

    }

    @Nested
    class whenDeleting {
        @Test
        void thenReturnNoContent() {
            Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

            Map<String, String> headers = new HashMap<>();
            Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "delete", Optional.empty(), headers);

            assertThat(result, is(expected));
        }
    }

    @Test
    void givenServiceNotHandled_whenApplyApar_thenReturnOriginalResult(){
        Optional<ResponseEntity<?>> previousResult = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        Map<String, String> headers = new HashMap<>();
        Optional<ResponseEntity<?>> result = underTest.apply("badservice", "", previousResult, headers);

        assertThat(result, is(previousResult));
    }

    private String getAuthorizationHeader(String user, String password) {
        return "Basic " + Base64.encodeBase64String((user + ":" + password).getBytes());
    }
}
