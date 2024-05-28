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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.zowe.apiml.zaas.security.login.saf.MockPlatformUser.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SafPlatformUserTests {

    private static SafPlatformUser safPlatformUser;

    @BeforeAll
    void setUp() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        safPlatformUser = new SafPlatformUser(new MockPlatformClassFactory());
    }

    @Test
    void returnsNullForValidAuthentication() {
        assertNull(safPlatformUser.authenticate(VALID_USERID, VALID_PASSWORD));
    }

    @Test
    void returnsDetailsForInvalidAuthentication() {
        PlatformReturned returned = safPlatformUser.authenticate(INVALID_USERID, INVALID_PASSWORD);
        assertFalse(returned.isSuccess());
    }

    @Test
    void returnsNullForChangePassword() {
        PlatformReturned returned = safPlatformUser.changePassword(VALID_USERID, VALID_PASSWORD, "newPassword" );
        assertNull(returned);
    }

    @Test
    void whenNewPasswordEqualsOld_thenReturnsNotSuccess() {
        PlatformReturned returned = safPlatformUser.changePassword(VALID_USERID, VALID_PASSWORD, VALID_PASSWORD );
        assertFalse(returned.isSuccess());
    }

}
