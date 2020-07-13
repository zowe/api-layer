/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login.dummy;

import org.zowe.apiml.gateway.security.service.AuthenticationService;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DummyAuthenticationProviderTest {

    private static final String PRINCIPAL = "user";
    private static final String USERNAME = "user";

    private static DummyAuthenticationProvider dummyAuthenticationProvider;

    @BeforeAll
    static void setup() {
        MonitoringHelper.initMocks();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        UserDetailsService userDetailsService = new InMemoryUserDetailsService(encoder);
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        dummyAuthenticationProvider = new DummyAuthenticationProvider(encoder, userDetailsService, authenticationService);
    }


    @Test
    void shouldReturnDummyToken() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, USERNAME);
        Authentication returnedTokenAuthentication = dummyAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(returnedTokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, returnedTokenAuthentication.getName());
        assertEquals(PRINCIPAL, returnedTokenAuthentication.getPrincipal());

    }

    @Test
    void shouldThrowExceptionIfTokenNotValid() {
        Exception exception = assertThrows(AuthenticationServiceException.class,
            () -> dummyAuthenticationProvider.authenticate(null),
            "Expected exception is not AuthenticationServiceException");
        assertEquals("A failure occurred when authenticating.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfCredentialsAreNull() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, "sdsd");

        Exception exception = assertThrows(BadCredentialsException.class,
            () -> dummyAuthenticationProvider.authenticate(usernamePasswordAuthentication),
        "Expected exception is not BadCredentialsException");
        assertEquals("Username or password are invalid.", exception.getMessage());
    }
}
