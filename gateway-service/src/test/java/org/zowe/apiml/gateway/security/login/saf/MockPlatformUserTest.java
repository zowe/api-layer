/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login.saf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;

import static org.junit.jupiter.api.Assertions.*;

class MockPlatformUserTest {

    private static MockPlatformUser mockPlatformUser;
    @BeforeAll
    static void setup() {
        mockPlatformUser = new MockPlatformUser();
    }

    @Test
    void givenValidCredentials_whenAuthenticate_thenReturnNull() {
        assertEquals(null, mockPlatformUser.authenticate("USER", "validPassword"));
    }

    @Test
    void givenInValidCredentials_whenAuthenticate_thenReturnPlatformReturnedSuccessFalse() {
        PlatformReturned platformReturned = PlatformReturned.builder().success(false).build();
        assertEquals(platformReturned, mockPlatformUser.authenticate("USER", "invalidPassword"));
    }

    @Test
    void givenValidCredentialsAndNewValidPassword_whenChangePassword_thenReturnNull() {
        assertEquals(null, mockPlatformUser.changePassword("USER", "validPassword", "newPassword"));
    }

    @Test
    void givenInvalidCredentialsAndNewValidPassword_whenChangePassword_thenReturnPlatformReturnedSuccessFalse() {
        PlatformReturned platformReturned = PlatformReturned.builder().success(false).build();
        assertEquals(platformReturned, mockPlatformUser.changePassword("InvalidUser", "validPassword", "newPassword"));
    }

    @Test
    void givenValidCredentialsAndInvalidNewPassword_whenChangePassword_thenReturnPlatformReturnedSuccessFalse() {
        PlatformReturned platformReturned = PlatformReturned.builder().success(false).build();
        assertEquals(platformReturned, mockPlatformUser.changePassword("USER", "validPassword", "validPassword"));
    }

}
