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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class SafResourceAccessSafTest {

    private static final String USER_ID = "userId";
    private static final String CLASS = "classTest";
    private static final String RESOURCE = "resourceTest";
    private static final AccessLevel LEVEL = AccessLevel.READ;
    private static final int LEVEL_INT = AccessLevel.READ.getValue();
    private static final Authentication authentication = new UsernamePasswordAuthenticationToken(USER_ID, "token");

    @Mock
    private static CheckPermissionMock checkPermissionMock;

    private SafResourceAccessVerifying safResourceAccessVerifying;

    @BeforeEach
    void setUp() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        safResourceAccessVerifying = spy(new SafResourceAccessSaf() {
            @Override
            protected Class<?> getPlatformClass() {
                return JzosMock.class;
            }
            @Override
            protected Class<?> getPlatformReturnedClass() {
                return TestPlatformReturned.class;
            }
        });
    }

    @ParameterizedTest
    @CsvSource(delimiter = ',', value = {
        "true,-1,-1,org.zowe.apiml.security.common.auth.saf.AccessControlError",
        "true,-1,0,org.zowe.apiml.security.common.auth.saf.AccessControlError",
        "true,111,-1,org.zowe.apiml.security.common.auth.saf.AccessControlError",
        "true,111,0,org.zowe.apiml.security.common.auth.saf.AccessControlError",
        "true,111,0x0d9,",
        "true,111,0x0cf,",
        "true,111,0x0f9,"
    })
    void testHasSafResourceAccess_whenErrorHappened_thenFalse(
        boolean success, int errno, int errno2, Class<Exception> exceptionClass
    ) {
        doReturn(
            TestPlatformReturned.builder()
                .success(success)
                .errno(errno)
                .errno2(errno2)
                .build()
        ).when(checkPermissionMock).checkPermission(
            USER_ID, CLASS, RESOURCE, LEVEL_INT
        );
        if (exceptionClass == null) {
            assertFalse(safResourceAccessVerifying.hasSafResourceAccess(authentication, CLASS, RESOURCE, LEVEL.name()));
        } else {
            assertThrows(exceptionClass, () -> safResourceAccessVerifying.hasSafResourceAccess(authentication, CLASS, RESOURCE, LEVEL.name()));
        }
    }

    @Test
    void testHasSafResourceAccess_whenNoResponse_thenTrue() {
        assertTrue(safResourceAccessVerifying.hasSafResourceAccess(authentication, CLASS, RESOURCE, LEVEL.name()));
    }

    @Builder
    public static class TestPlatformReturned {

        boolean success;
        int rc;
        int errno;
        int errno2;
        String errnoMsg;
        String stringRet;
        Object objectRet;

    }

    public interface CheckPermissionMock {

        TestPlatformReturned checkPermission(String userid, String resourceType, String resourceName, int accessLevel);

    }

    public static class JzosMock {

        public static TestPlatformReturned checkPermission(String userid, String resourceType, String resourceName, int accessLevel) {
            return checkPermissionMock.checkPermission(userid, resourceType, resourceName,accessLevel);
        }

    }

}
