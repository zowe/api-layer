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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PH30398Test {
    private static final String SERVICE = "authentication";
    private static final String USERNAME = "USER";
    private static final String PASSWORD = "validPassword";

    private PH30398 underTest;
    private MockHttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        List<String> usernames = Collections.singletonList(USERNAME);
        List<String> passwords = Collections.singletonList(PASSWORD);

        underTest = new PH30398(usernames, passwords);
        mockResponse = new MockHttpServletResponse();
    }

    @Test
    void givenNoAuthorization_thenReturnInternalServerError() {
        Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        Map<String, String> headers = new HashMap<>();
        Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

        assertThat(result, is(expected));
    }

    @Test
    void givenEmptyAuthorization_thenReturnInteralServerError() {
        Optional<ResponseEntity<?>> expected = Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "");
        Optional<ResponseEntity<?>> result = underTest.apply(SERVICE, "create", Optional.empty(), mockResponse, headers);

        assertThat(result, is(expected));
    }

    @Test
    void givenServiceNotHandled_whenApplyApar_thenReturnOriginalResult() {
        Optional<ResponseEntity<?>> previousResult = Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        Map<String, String> headers = new HashMap<>();
        Optional<ResponseEntity<?>> result = underTest.apply("badservice", "", previousResult, headers);

        assertThat(result, is(previousResult));
    }
}
