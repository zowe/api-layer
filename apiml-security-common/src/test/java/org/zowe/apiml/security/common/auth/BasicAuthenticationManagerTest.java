/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BasicAuthenticationManagerTest {

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ROLE = "CACHING_SERVICE";
    private static final AbstractAuthenticationToken VALID_AUTHENTICATION = new UsernamePasswordAuthenticationToken(USER, PASSWORD.toCharArray(), Collections.singleton(new SimpleGrantedAuthority(ROLE)));

    @Nested
    class MissingCredentials {

        @ParameterizedTest
        @CsvSource({
            ",,",
            "user,,",
            ",password,"
        })
        void givenNoCompleteCredentials_whenAuthorize_thenThrowException(String user, String password) {
            var authenticationManager = new BasicAuthenticationManager(user, password == null ? null : password.toCharArray(), ROLE);
            StepVerifier.create(authenticationManager.authenticate(VALID_AUTHENTICATION))
                .expectError(BadCredentialsException.class)
                .verify();
        }

    }

    @Nested
    class ValidCredentials {

        private ReactiveAuthenticationManager basicAuthenticationManager = new BasicAuthenticationManager(USER, PASSWORD.toCharArray(), ROLE);

        @Test
        void givenValidCredentials_whenAuthenticate_thenSuccess() {
            StepVerifier.create(basicAuthenticationManager.authenticate(VALID_AUTHENTICATION))
                .assertNext(authentication -> {
                    assertNotSame(VALID_AUTHENTICATION, authentication);
                    assertTrue(authentication.isAuthenticated());
                    assertEquals(USER, authentication.getName());
                    String credentials = authentication.getCredentials() instanceof char[] chars ? new String(chars) : String.valueOf(authentication.getCredentials());
                    assertEquals(PASSWORD, credentials);
                })
                .verifyComplete();
        }

        @Test
        void givenValidCredentialsInAnotherForm_whenAuthenticate_thenSuccess() {
            var user = new Object() {
                @Override
                public String toString() {
                    return USER;
                }
            };
            Authentication validAuthentication = new UsernamePasswordAuthenticationToken(user, PASSWORD, Collections.singleton(new SimpleGrantedAuthority(ROLE)));
            StepVerifier.create(basicAuthenticationManager.authenticate(validAuthentication))
                .assertNext(authentication -> {
                    assertNotSame(validAuthentication, authentication);
                    assertTrue(authentication.isAuthenticated());
                    assertEquals(USER, authentication.getName());
                    assertTrue(authentication.getCredentials() instanceof char[]);
                    assertEquals(PASSWORD, new String((char[]) authentication.getCredentials()));
                })
                .verifyComplete();
        }

        @ParameterizedTest
        @CsvSource({
            ",,",
            "user,,",
            ",password,",
            "attacker,attempt"
        })
        void givenInvalidCredentials_whenAuthenticate_thenThrowException(String user, String password) {
            var authentication = new UsernamePasswordAuthenticationToken(user, password == null ? null : password.toCharArray(), Collections.singleton(new SimpleGrantedAuthority(ROLE)));
            StepVerifier.create(basicAuthenticationManager.authenticate(authentication))
                .expectError(BadCredentialsException.class)
                .verify();
        }

    }

}

