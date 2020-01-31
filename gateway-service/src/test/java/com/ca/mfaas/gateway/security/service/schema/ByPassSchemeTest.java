package com.ca.mfaas.gateway.security.service.schema;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.ca.apiml.security.common.auth.AuthenticationScheme;
import org.junit.Test;

import static org.junit.Assert.*;

public class ByPassSchemeTest {

    @Test
    public void testScheme() {
        ByPassScheme scheme = new ByPassScheme();
        assertTrue(scheme.isDefault());
        assertEquals(AuthenticationScheme.BYPASS, scheme.getScheme());
        assertSame(AuthenticationCommand.EMPTY, scheme.createCommand(null, null));
    }

}
