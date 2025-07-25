/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.apicatalog.exceptions.ApiDocTransformationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiDocTransformationExceptionTest {

    @Nested
    class GivenExceptionMessage {
        @Test
        void thenReturnMessage() {
            String message = "This is an error message";
            ApiDocTransformationException exception = new ApiDocTransformationException(message);
            assertEquals(message, exception.getMessage());
        }
    }
}
