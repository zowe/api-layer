/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.saf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;
import org.zowe.apiml.security.common.error.PlatformPwdErrno;

import static org.junit.jupiter.api.Assertions.*;

class MockPlatformUserTest {

    private static MockPlatformUser mockPlatformUser;
    @BeforeAll
    static void setup() {
        mockPlatformUser = new MockPlatformUser();
    }

    @Nested
    class WhenAuthenticate {
        @Nested
        class Success {
            @Test
            void givenValidCredentials_whenAuthenticate_thenReturnNull() {
                assertNull(mockPlatformUser.authenticate("USER", "validPassword"));
            }
        }

        @Nested
        class Fails {
            @Test
            void givenInValidCredentials_whenAuthenticate_thenReturnPlatformReturnedSuccessFalse() {
                PlatformReturned platformReturned = PlatformReturned.builder().success(false).errno(PlatformPwdErrno.EACCES.errno).build();
                assertEquals(platformReturned, mockPlatformUser.authenticate("USER", "invalidPassword"));
            }

            @Test
            void givenExpiredPassword_whenAuthenticate_thenReturnEmvsExpire() {
                PlatformReturned platformReturned = mockPlatformUser.authenticate("USER", "expiredPassword");
                assertFalse(platformReturned.success);
                assertEquals(PlatformPwdErrno.EMVSEXPIRE.errno, platformReturned.errno);
            }
        }

    }

    @Nested
    class WhenChangingPassword {
        @Nested
        class Success {
            @Test
            void givenValidCredentialsAndNewValidPassword_whenChangePassword_thenReturnNull() {
                assertNull( mockPlatformUser.changePassword("USER", "validPassword", "newPassword"));
            }
        }

        @Nested
        class Fails {
            @Test
            void givenInvalidCredentialsAndNewValidPassword_whenChangePassword_thenReturnPlatformReturnedSuccessFalse() {
                PlatformReturned platformReturned = PlatformReturned.builder().success(false).errno(PlatformPwdErrno.EMVSPASSWORD.errno).build();
                assertEquals(platformReturned, mockPlatformUser.changePassword("InvalidUser", "validPassword", "newPassword"));
            }

            @Test
            void givenValidCredentialsAndInvalidNewPassword_whenChangePassword_thenReturnPlatformReturnedSuccessFalse() {
                PlatformReturned platformReturned = PlatformReturned.builder().success(false).errno(PlatformPwdErrno.EMVSPASSWORD.errno).build();
                assertEquals(platformReturned, mockPlatformUser.changePassword("USER", "validPassword", "validPassword"));
            }
        }
    }







}
