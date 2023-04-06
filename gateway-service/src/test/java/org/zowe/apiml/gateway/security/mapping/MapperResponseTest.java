/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperResponseTest {

    private static final String USER = "ZOSUSER";
    private static final int RC = 1;
    private static final int SAFRC = 2;
    private static final int RACFRC = 3;
    private static final int RACFREASON = 4;

    @Test
    void testMapperResponseToString() {
        MapperResponse response = new MapperResponse(USER, RC, SAFRC, RACFRC, RACFREASON);
        String expected = "User: ZOSUSER, rc=1, safRc=2, racfRc=3, racfRs=4";
        assertEquals(expected, response.toString());
    }
}
