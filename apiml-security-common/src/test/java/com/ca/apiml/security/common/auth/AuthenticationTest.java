package com.ca.apiml.security.common.auth;/*
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
import static org.junit.Assert.assertTrue;
@RunWith(JUnit4.class)
public class AuthenticationTest {

    @Test
    public void testIsEmpty() {
        Authentication a;

        a = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        assertFalse(a.isEmpty());

        a = new Authentication(AuthenticationScheme.ZOSMF, null);
        assertFalse(a.isEmpty());

        a = new Authentication(null, "applid");
        assertFalse(a.isEmpty());

        a = new Authentication(null, "");
        assertFalse(a.isEmpty());


        a = new Authentication(null, null);
        assertTrue(a.isEmpty());
    }

}
