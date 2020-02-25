/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.*;


public class ZoweJwtSchemeTest {

    @Test
    public void testScheme() {
        ZoweJwtScheme scheme = new ZoweJwtScheme();
        assertFalse(scheme.isDefault());
        assertEquals(AuthenticationScheme.ZOWE_JWT, scheme.getScheme());
        assertSame(AuthenticationCommand.EMPTY, scheme.createCommand(null, null));
    }

}
