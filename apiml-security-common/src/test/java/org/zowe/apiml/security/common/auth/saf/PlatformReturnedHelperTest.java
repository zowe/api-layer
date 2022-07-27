/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.Builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PlatformReturnedHelperTest {

    private static final Object OBJECT_RET = new Object();
    private static final String ERROR_MESSAGE = "error message";
    private static final String RETURN_VALUE = "return value";

    @Test
    void testConvert_whenObjectContainsAllFields_thenSuccessfulConversion() throws NoSuchFieldException {
        TestPlatformReturned testPlatformReturned = TestPlatformReturned.builder()
            .success(true)
            .rc(123)
            .errno(234)
            .errno2(345)
            .errnoMsg(ERROR_MESSAGE)
            .stringRet(RETURN_VALUE)
            .objectRet(OBJECT_RET)
            .build();
        PlatformReturned platformReturned = new PlatformReturnedHelper<>(TestPlatformReturned.class)
            .convert(testPlatformReturned);

        assertNotNull(platformReturned);
        assertTrue(platformReturned.isSuccess());
        assertEquals(123, platformReturned.getRc());
        assertEquals(234, platformReturned.getErrno());
        assertEquals(345, platformReturned.getErrno2());
        assertEquals(ERROR_MESSAGE, platformReturned.getErrnoMsg());
        assertEquals(RETURN_VALUE, platformReturned.getStringRet());
        assertSame(OBJECT_RET, platformReturned.getObjectRet());
    }

    @Test
    void testConvert_whenSuccessIsFalse_thenSuccessfulConversion() throws NoSuchFieldException {
        TestPlatformReturned testPlatformReturned = TestPlatformReturned.builder()
            .success(false)
            .build();
        PlatformReturned platformReturned = new PlatformReturnedHelper<>(TestPlatformReturned.class)
            .convert(testPlatformReturned);

        assertNotNull(platformReturned);
        assertFalse(platformReturned.isSuccess());
    }

    @Test
    void testInit_whenMissingFields_thenNoSuchFieldException() {
        assertThrows(NoSuchFieldException.class, () -> new PlatformReturnedHelper<>(Object.class));
    }

    @Test
    void testInit_whenNull_thenNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PlatformReturnedHelper<>(null));
    }

    @Builder
    static class TestPlatformReturned {

        boolean success;
        int rc;
        int errno;
        int errno2;
        String errnoMsg;
        String stringRet;
        Object objectRet;

    }

}
