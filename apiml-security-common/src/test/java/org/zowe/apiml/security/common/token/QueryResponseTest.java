/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.token;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryResponseTest {

    @Test
    void testIsExpired() {
        final Calendar c = Calendar.getInstance();
        final Date now = c.getTime();
        c.add(Calendar.MINUTE, -1);
        final Date before = c.getTime();
        c.add(Calendar.MINUTE, 2);
        final Date after = c.getTime();

        assertTrue(new QueryResponse("domain", "user", now, before, Collections.emptyList(), QueryResponse.Source.ZOWE).isExpired());
        assertFalse(new QueryResponse("domain", "user", now, after, Collections.emptyList(), QueryResponse.Source.ZOWE).isExpired());
    }

    @Test
    void testSource() {
        assertEquals(QueryResponse.Source.ZOSMF, QueryResponse.Source.valueByIssuer("zosmf"));
        assertEquals(QueryResponse.Source.ZOSMF, QueryResponse.Source.valueByIssuer("zOSMF"));
        assertEquals(QueryResponse.Source.ZOWE, QueryResponse.Source.valueByIssuer("apiml"));
        assertEquals(QueryResponse.Source.ZOWE, QueryResponse.Source.valueByIssuer("APIML"));

        Exception tnve = assertThrows(TokenNotValidException.class, () -> QueryResponse.Source.valueByIssuer(null));
        assertEquals("Unknown token type : null", tnve.getMessage());

        tnve = assertThrows(TokenNotValidException.class, () -> QueryResponse.Source.valueByIssuer("unknown"));
        assertEquals("Unknown token type : unknown", tnve.getMessage());
    }

}
