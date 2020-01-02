package com.ca.apiml.security.common.token;/*
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

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class QueryResponseTest {

    @Test
    public void testIsExpired() {
        final Calendar c = Calendar.getInstance();
        final Date now = c.getTime();
        c.add(Calendar.MINUTE, -1);
        final Date before = c.getTime();
        c.add(Calendar.MINUTE, 2);
        final Date after = c.getTime();

        assertTrue(new QueryResponse("domain", "user", now, before).isExpired());
        assertFalse(new QueryResponse("domain", "user", now, after).isExpired());
    }

}
