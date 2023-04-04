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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternalMapperExceptionTest {
    @Nested
    class GivenExceptionMessage {

        @Test
        void thenReturnMessage() {
            String message = "This is an error message";
            ExternalMapperException exception = new ExternalMapperException(message);
            assertEquals(message, exception.getMessage());
        }
    }
}
