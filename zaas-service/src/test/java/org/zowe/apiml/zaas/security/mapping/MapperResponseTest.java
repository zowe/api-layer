/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.mapping;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaas.security.mapping.model.MapperResponse;

import static org.junit.jupiter.api.Assertions.*;

class MapperResponseTest {

    private static final String USER = "ZOSUSER";
    private static final int RC = 1;
    private static final int SAFRC = 2;
    private static final int RACFRC = 3;
    private static final int RACFREASON = 4;


    @Nested
    class GivenAnyMapperResponse {
        @Test
        void thenDisplayToString() {
            MapperResponse response = new MapperResponse(USER, RC, SAFRC, RACFRC, RACFREASON);
            String expected = "User: ZOSUSER, rc=1, safRc=2, racfRc=3, racfRs=4";
            assertEquals(expected, response.toString());
        }
    }

    @Nested
    class GivenValidatingResponse {

        @Test
        void whenExpectedValues_thenResponseIsValid() {
            MapperResponse response = new MapperResponse("user", 0, 0, 0, 0);
            assertTrue(response.isOIDCResultValid());
        }

        @Test
        void whenWrongMapperInput_thenResponseIsInvalid() {
            MapperResponse response = new MapperResponse("", 8, 8, 8, 44);
            assertFalse(response.isOIDCResultValid());
        }

        @Test
        void whenMappingNotExist_thenResponseIsInvalid() {
            MapperResponse response = new MapperResponse("", 8, 8, 8, 48);
            assertFalse(response.isOIDCResultValid());
        }

        @Test
        void whenUnexpectedResponse_thenResponseIsInvalid() {
            MapperResponse response = new MapperResponse(USER, RC, SAFRC, RACFRC, RACFREASON);
            assertFalse(response.isOIDCResultValid());
        }

        @Test
        void whenNotAuthorized_thenResponseIsInvalid() {
            MapperResponse response = new MapperResponse("", 8, 8, 8, 20);
            assertFalse(response.isOIDCResultValid());
        }
    }
}
