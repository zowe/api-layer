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

import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordRequestTest {
    public static final String NEW_PASS = "newPass";
    public static final String PASS = "pass";
    public static final String USERNAME = "user";

    @Nested
    class WhenPassingLoginRequestObject {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASS, NEW_PASS);
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(loginRequest);

        @Test
        void thenMapConstructor() {
            assertEquals(loginRequest.getUsername(), changePasswordRequest.getUsername());
            assertEquals(loginRequest.getPassword(), changePasswordRequest.getPassword());
            assertEquals(loginRequest.getNewPassword(), changePasswordRequest.getNewPassword());
        }
    }
}
