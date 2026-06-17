/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;

class CachingServiceClientExceptionTest {

    @Nested
    class GivenExceptionConstructors {

        @Test
        void whenConstructorsAreCalled_thenInitializeCorrectly() {
            var exMessage = new CachingServiceClientException("Error");
            var exCause = new CachingServiceClientException("Error", new RuntimeException("Cause"));

            assertEquals("Error", exMessage.getMessage());
            assertNull(exMessage.getCause());

            assertEquals("Error", exCause.getMessage());
            assertNotNull(exCause.getCause());
        }
    }

    @Nested
    class GivenIsKeyCollisionCheck {

        @Test
        void whenCauseIsHttp409Conflict_thenReturnTrue() {
            var httpException = HttpClientErrorException.create(HttpStatus.CONFLICT, "", null, null, null);
            var exception = new CachingServiceClientException("Error", httpException);

            boolean result = exception.isKeyCollision();
            assertTrue(result);
        }

        @Test
        void whenCauseIsNotHttp409ConflictOrNull_thenReturnFalse() {
            var httpException = HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "", null, null, null);
            var exStatus = new CachingServiceClientException("Error", httpException);
            var exCause = new CachingServiceClientException("Error");

            assertFalse(exStatus.isKeyCollision());
            assertFalse(exCause.isKeyCollision());
        }
    }

}
