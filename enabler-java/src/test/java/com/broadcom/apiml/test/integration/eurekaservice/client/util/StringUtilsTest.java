/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.eurekaservice.client.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {
    @Test
    public void emptyStrungTest() {
        String string = "";

        assertTrue(StringUtils.isNullOrEmpty(string));
    }

    @Test
    public void nullStringTest() {
        String string = null;

        assertTrue(StringUtils.isNullOrEmpty(string));
    }

    @Test
    public void notEmptyStringTest() {
        String string = "i am string";

        assertFalse(StringUtils.isNullOrEmpty(string));
    }

}
