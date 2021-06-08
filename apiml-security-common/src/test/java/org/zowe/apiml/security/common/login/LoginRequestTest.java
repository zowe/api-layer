/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoginRequestTest {

    public static final String NEW_PASS = "newPass";
    public static final String PASS = "pass";
    public static final String USERNAME = "user";

    @Nested
    class WhenPasswordInCredentials {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(USERNAME, PASS);

        @Test
        void returnPassword() {
            assertEquals(PASS, LoginRequest.getPassword(auth));
        }

        @Test
        void returnNull() {
            assertNull(LoginRequest.getNewPassword(auth));
        }

    }

    @Nested
    class WhenLoginRequestInCredentials {

        LoginRequest loginRequest = new LoginRequest(USERNAME, PASS, NEW_PASS);

        @Test
        void returnNewPassword() {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest);
            assertEquals(NEW_PASS, LoginRequest.getNewPassword(auth));
        }

        @Test
        void returnPassword() {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest);
            assertEquals(PASS, LoginRequest.getPassword(auth));
        }
    }

}
