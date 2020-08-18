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

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.zowe.apiml.gateway.security.login.saf.MockPlatformUser.*;

class SafPlatformUserTests {
    private static SafPlatformUser safPlatformUser = new SafPlatformUser(new MockPlatformClassFactory());

    @Test
    void returnsNullForValidAuthentication() {
        assertNull(safPlatformUser.authenticate(VALID_USERID, VALID_PASSWORD));
    }

    @Test
    void returnsDetailsForInvalidAuthentication() {
        PlatformReturned returned = safPlatformUser.authenticate(INVALID_USERID, INVALID_PASSWORD);
        assertFalse(returned.isSuccess());
    }

}
