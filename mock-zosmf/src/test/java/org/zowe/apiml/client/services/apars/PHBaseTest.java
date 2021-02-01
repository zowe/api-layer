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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PHBaseTest {
    private PHBase underTest;

    @BeforeEach
    void setUp() {
        underTest = new PHBase();
    }

    @Nested
    class whenInfoCalled {
        @Test
        void givenNothing_Ltpa2TokenIsntReturned() {
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            underTest.apply("info", "", Optional.empty(), mockResponse, new HashMap<>());

            verify(mockResponse, never()).addCookie(any());
        }

        @Test
        void givenValidAuthenticationCredentials_Ltpa2TokenIsReturned() {
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", Base64.encodeBase64String("USER:validPassword".getBytes()));
            underTest.apply("info", "", Optional.empty(), mockResponse, headers);

            ArgumentCaptor<Cookie> called = ArgumentCaptor.forClass(Cookie.class);
            verify(mockResponse).addCookie(called.capture());

            Cookie ltpa = called.getValue();
            assertThat(ltpa.getName(), is("LtpaToken2"));
        }
    }

    @Nested
    class whenAuthenticateIsCalled {
        @Test
        void notExistIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("authentication", "", Optional.empty());
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class whenFilesAreCalled {
        @Test
        void givenProperAuthorization_validFilesAreReturned() {
            Map<String, String> authorization = new HashMap<>();
            authorization.put("cookie", "LtpaToken2=randomValidValue");

            Optional<ResponseEntity<?>> result = underTest.apply("files", "", Optional.empty(), authorization);
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.OK));
        }

        @Test
        void givenInvalidAuthorization_unauthorizedIsReturned() {
            Optional<ResponseEntity<?>> result = underTest.apply("files", "", Optional.empty(), new HashMap<>());
            assertThat(result.isPresent(), is(true));

            ResponseEntity<?> response = result.get();
            assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        }
    }
}
