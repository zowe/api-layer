/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.exception;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PetIdMismatchExceptionTest {
    @Test
    void testConstructor() {
        String message = "id in body is different from in URL";
        long pathId = 1L;
        long bodyId = 2;
        PetIdMismatchException exception = new PetIdMismatchException(message, pathId, bodyId);

        assertThat(exception.getMessage(), is(message));
        assertThat(exception.getPathId(), is(pathId));
        assertThat(exception.getBodyId(), is(bodyId));
    }
}
