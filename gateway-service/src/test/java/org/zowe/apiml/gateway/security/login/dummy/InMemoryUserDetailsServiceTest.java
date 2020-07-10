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

import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;


public class InMemoryUserDetailsServiceTest {

    private static BCryptPasswordEncoder encoder;
    private static InMemoryUserDetailsService inMemoryUserDetailsService;

    @BeforeAll
    public static void setup() {
        MonitoringHelper.initMocks();
        encoder = Mockito.mock(BCryptPasswordEncoder.class);
        inMemoryUserDetailsService = new InMemoryUserDetailsService(encoder);
    }

    @Test
    void shouldEncodePasswordAndReturnFoundUsername() {
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
    void shouldThrowExceptionIfUsernameNotFound() {
        Exception exception = assertThrows(UsernameNotFoundException.class,
            () -> inMemoryUserDetailsService.loadUserByUsername("andrea"),
            "Expected exception is not UsernameNotFoundException");
        assertEquals("Username: andrea not found.", exception.getMessage());


    }
}
