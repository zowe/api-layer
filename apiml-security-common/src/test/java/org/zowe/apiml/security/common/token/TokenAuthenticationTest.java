package org.zowe.apiml.security.common.token;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
@RunWith(JUnit4.class)
public class TokenAuthenticationTest {

    @Test
    public void testCreateAuthenticated() {
        TokenAuthentication ta = TokenAuthentication.createAuthenticated("user", "token");
        assertEquals("user", ta.getPrincipal());
        assertEquals("token", ta.getCredentials());
        assertTrue(ta.isAuthenticated());
    }

}
