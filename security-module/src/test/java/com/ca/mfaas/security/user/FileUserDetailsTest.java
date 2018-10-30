/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.user;

import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileUserDetailsTest {
    @Test
    public void testUserDetailsCreation() {
        String username = "user";
        String password = "password";
        User user = new User(username, password);

        FileUserDetails userDetails = new FileUserDetails(user);

        assertThat(userDetails.getUsername(), is(username));
        assertThat(userDetails.getPassword(), is(password));
        assertThat(userDetails.isEnabled(), is(true));
        assertThat(userDetails.isAccountNonExpired(), is(true));
        assertThat(userDetails.isAccountNonLocked(), is(true));
        assertThat(userDetails.isCredentialsNonExpired(), is(true));
        assertThat(userDetails.getAuthorities(), is(Collections.emptyList()));
    }
}
