/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.security.login.dummy;

import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DummyAuthenticationProviderTest {

    private static final String PRINCIPAL = "user";
    private static final String USERNAME = "user";

    private static DummyAuthenticationProvider dummyAuthenticationProvider;
    private static AuthenticationService authenticationService;
    private  static BCryptPasswordEncoder encoder;
    private static UserDetailsService userDetailsService;


    @BeforeClass
    public static void setup() {
        MonitoringHelper.initMocks();
        authenticationService = mock(AuthenticationService.class);
        encoder = new BCryptPasswordEncoder(10);
        userDetailsService = new InMemoryUserDetailsService(encoder);
        dummyAuthenticationProvider = new DummyAuthenticationProvider(encoder, userDetailsService, authenticationService);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldReturnDummyToken() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, USERNAME);
        Authentication returnedTokenAuthentication = dummyAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertTrue(returnedTokenAuthentication.isAuthenticated());
        assertEquals(USERNAME, returnedTokenAuthentication.getName());
        assertEquals(PRINCIPAL, returnedTokenAuthentication.getPrincipal());

    }

    @Test
    public void shouldThrowExceptionIfTokenNotValid() {
        exception.expect(AuthenticationServiceException.class);
        exception.expectMessage("A failure occurred when authenticating.");

        dummyAuthenticationProvider.authenticate(null);
    }

    @Test
    public void shouldThrowExceptionIfCredentialsAreNull() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, "sdsd");

        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password are invalid.");

        dummyAuthenticationProvider.authenticate(usernamePasswordAuthentication);

    }
}
