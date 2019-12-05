/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

public class ObjectUtilTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testRequireNotNull() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Parameter can't be null");

        ObjectUtil.requireNotNull(null, "Parameter can't be null");
    }

    @Test
    public void testGetThisClas() {
        Class aClass = ObjectUtil.getThisClass();
        assertNotNull(aClass);
        assertEquals(this.getClass().getSimpleName(), aClass.getSimpleName());
    }
}
