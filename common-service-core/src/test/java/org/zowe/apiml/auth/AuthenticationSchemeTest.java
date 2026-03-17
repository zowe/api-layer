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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
class AuthenticationSchemeTest {

    @Test
    void givenValidValue_whenFromString_thenReturn() {
        for (AuthenticationScheme as : AuthenticationScheme.values()) {
            assertSame(as, AuthenticationScheme.fromString(as.getScheme()));
        }
    }

    @Test
    void givenNonExistingValue_whenFromString_thenReturnNull() {
        assertNull(AuthenticationScheme.fromString("absolute nonsense"));
    }

}
