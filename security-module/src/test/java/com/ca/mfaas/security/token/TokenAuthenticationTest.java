/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.token;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class TokenAuthenticationTest {

    @Test
    public void createWithTokenTest() {
        String token = "token";

        TokenAuthentication tokenAuthentication = new TokenAuthentication(token);

        assertThat(tokenAuthentication.getPrincipal(), is(nullValue()));
        assertThat(tokenAuthentication.getCredentials(), is(token));
    }

    @Test
    public void createWithUserNameAndTokenTest() {
        String username = "username";
        String token = "token";

        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token);

        assertThat(tokenAuthentication.getPrincipal(), is(username));
        assertThat(tokenAuthentication.getCredentials(), is(token));
    }
}
