/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class HttpsWebSecurityConfigTest {

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private static final Authentication VALID_AUTHENTICATION = new UsernamePasswordAuthenticationToken(USER, PASSWORD.toCharArray(), Collections.singleton(new SimpleGrantedAuthority("EUREKA")));

    @Nested
    class Provider {

        @Nested
        class MissingCredentials {

            @ParameterizedTest
            @CsvSource({
                ",,",
                "user,,",
                ",password,"
            })
            void givenNoCompleteCredentials_whenAuthorize_thenThrowException(String user, String password) {
                AuthenticationProvider provider = new HttpsWebSecurityConfig.EurekaBasicAuthenticationProvider(user, password == null ? null : password.toCharArray());
                assertThrows(BadCredentialsException.class, () -> provider.authenticate(VALID_AUTHENTICATION));
            }

        }

        @Nested
        class ValidCredentials {

            private AuthenticationProvider provider = new HttpsWebSecurityConfig.EurekaBasicAuthenticationProvider(USER, PASSWORD.toCharArray());

            @Test
            void givenValidCredentials_whenAuthenticate_thenSuccess() {
                Authentication authentication = provider.authenticate(VALID_AUTHENTICATION);
                assertNotSame(VALID_AUTHENTICATION, authentication);
                assertTrue(authentication.isAuthenticated());
                assertEquals(USER, authentication.getPrincipal());
                String credentials = authentication.getCredentials() instanceof char[] ? new String((char[]) authentication.getCredentials()) : String.valueOf(authentication.getCredentials());
                assertEquals(PASSWORD, credentials);
            }

            @Test
            void givenValidCredentialsInAnotherForm_whenAuthenticate_thenSuccess() {
                Object user = new Object() {
                    @Override
                    public String toString() {
                        return USER;
                    }
                };
                Authentication validAuthentication = new UsernamePasswordAuthenticationToken(user, PASSWORD, Collections.singleton(new SimpleGrantedAuthority("EUREKA")));
                Authentication authentication = provider.authenticate(validAuthentication);
                assertNotSame(validAuthentication, authentication);
                assertTrue(authentication.isAuthenticated());
                assertEquals(USER, String.valueOf(authentication.getPrincipal()));
                assertEquals(PASSWORD, String.valueOf(authentication.getCredentials()));
            }

            @ParameterizedTest
            @CsvSource({
                ",,",
                "user,,",
                ",password,",
                "attacker,attempt"
            })
            void givenInvalidCredentials_whenAuthenticate_thenThrowException(String user, String password) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(user, password == null ? null : password.toCharArray(), Collections.singleton(new SimpleGrantedAuthority("EUREKA")));
                assertThrows(BadCredentialsException.class, () -> provider.authenticate(authentication));
            }

        }

        @Nested
        class Conversion {

            private static final String HELLO_WORLD = "helloWorld";

            @Test
            void givenNull_whenGetBytes_thenEmptyArray() {
                assertArrayEquals(new byte[0], HttpsWebSecurityConfig.EurekaBasicAuthenticationProvider.getBytes(null));
            }

            @Test
            void givenString_whenGetBytes_thenGetArray() {
                assertArrayEquals(HELLO_WORLD.getBytes(StandardCharsets.UTF_8), HttpsWebSecurityConfig.EurekaBasicAuthenticationProvider.getBytes(HELLO_WORLD));
            }

            @Test
            void givenCharArray_whenGetBytes_thenGetArray() {
                assertArrayEquals(HELLO_WORLD.getBytes(StandardCharsets.UTF_8), HttpsWebSecurityConfig.EurekaBasicAuthenticationProvider.getBytes(HELLO_WORLD.toCharArray()));
            }

            @Test
            void givenObject_whenGetBytes_thenGetArray() {
                Object object = new Object() {
                    @Override
                    public String toString() {
                        return HELLO_WORLD;
                    }
                };
                assertArrayEquals(HELLO_WORLD.getBytes(StandardCharsets.UTF_8), HttpsWebSecurityConfig.EurekaBasicAuthenticationProvider.getBytes(object));
            }

        }

    }

}

