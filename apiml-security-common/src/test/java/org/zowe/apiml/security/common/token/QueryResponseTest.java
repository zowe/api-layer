package org.zowe.apiml.security.common.token;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class QueryResponseTest {

    @Test
    public void testIsExpired() {
        final Calendar c = Calendar.getInstance();
        final Date now = c.getTime();
        c.add(Calendar.MINUTE, -1);
        final Date before = c.getTime();
        c.add(Calendar.MINUTE, 2);
        final Date after = c.getTime();

        assertTrue(new QueryResponse("domain", "user", now, before, QueryResponse.Source.ZOWE).isExpired());
        assertFalse(new QueryResponse("domain", "user", now, after, QueryResponse.Source.ZOWE).isExpired());
    }

    @Test
    public void testSource() {
        assertEquals(QueryResponse.Source.ZOSMF, QueryResponse.Source.valueByIssuer("zosmf"));
        assertEquals(QueryResponse.Source.ZOSMF, QueryResponse.Source.valueByIssuer("zOSMF"));
        assertEquals(QueryResponse.Source.ZOWE, QueryResponse.Source.valueByIssuer("apiml"));
        assertEquals(QueryResponse.Source.ZOWE, QueryResponse.Source.valueByIssuer("APIML"));
        try {
            assertEquals(QueryResponse.Source.ZOWE, QueryResponse.Source.valueByIssuer(null));
            fail();
        } catch (TokenNotValidException tnve) {
            assertEquals("Unknown token type : null", tnve.getMessage());
        }
        try {
            assertEquals(QueryResponse.Source.ZOWE, QueryResponse.Source.valueByIssuer("unknown"));
            fail();
        } catch (TokenNotValidException tnve) {
            assertEquals("Unknown token type : unknown", tnve.getMessage());
        }
    }

}
