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

import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.Assert.*;

public class InMemoryUserDetailsServiceTest {

    private static BCryptPasswordEncoder encoder;
    private static InMemoryUserDetailsService inMemoryUserDetailsService;

    @BeforeClass
    public static void setup() {
        MonitoringHelper.initMocks();
        encoder = Mockito.mock(BCryptPasswordEncoder.class);
        inMemoryUserDetailsService = new InMemoryUserDetailsService(encoder);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldEncodePasswordAndReturnFoundUsername() {
        Mockito.when(encoder.encode("user")).thenReturn("hashcodeUser");

        UserDetails userDetails = inMemoryUserDetailsService.loadUserByUsername("user");

        assertEquals("user", userDetails.getUsername());
        assertEquals("hashcodeUser", userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    public void shouldThrowExceptionIfUsernameNotFound() {
        exception.expect(UsernameNotFoundException.class);
        exception.expectMessage("Username: andrea not found.");

        inMemoryUserDetailsService.loadUserByUsername("andrea");
    }
}
