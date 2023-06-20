/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class AuthenticationSchemesTest {

    @Test
    void testFromScheme() {
        AuthenticationSchemes underTest = new AuthenticationSchemes();
        for (AuthenticationScheme as : AuthenticationScheme.values()) {
            AuthenticationScheme as2 = underTest.map(as.getScheme());
            assertSame(as, as2);
        }
        assertNull(underTest.map("absolute nonsense"));
        assertEquals("bypass", underTest.map("bypass").toString());
    }

}
