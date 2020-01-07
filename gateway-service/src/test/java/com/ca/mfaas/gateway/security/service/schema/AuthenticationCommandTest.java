package com.ca.mfaas.gateway.security.service.schema;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertFalse;
@RunWith(JUnit4.class)
public class AuthenticationCommandTest {

    @Test
    public void testEmptyCommand() throws Exception {
        assertFalse(AuthenticationCommand.EMPTY.isExpired());
        AuthenticationCommand.EMPTY.apply(null);
    }

}
