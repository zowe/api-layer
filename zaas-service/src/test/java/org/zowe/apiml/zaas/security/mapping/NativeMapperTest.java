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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.commons.usermap.CertificateResponse;
import org.zowe.commons.usermap.MapperResponse;
import org.zowe.commons.usermap.UserMapper;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeMapperTest {

    public static final String MF_USER = "mf_user";
    private static final NativeMapper nativeMapper = new NativeMapper();

    static UserMapper mockUserMapper = new MockUserMapper();

    @BeforeAll
    static void setup() throws NoSuchFieldException, IllegalAccessException {
        Field userMapper = NativeMapper.class.getDeclaredField("userMapper");
        userMapper.setAccessible(true);
        userMapper.set(nativeMapper, mockUserMapper);
    }

    @Nested
    class GivenRequestsToNativeMapper {

        @Test
        void thenMappedUserProvidedFromCertificate() {
            CertificateResponse response = nativeMapper.getUserIDForCertificate(new byte[2]);
            assertEquals(MF_USER, response.getUserId());

        }

        @Test
        void thenMappedUserProvidedFromDN() {
            MapperResponse response = nativeMapper.getUserIDForDN("dn", "registry");
            assertEquals(MF_USER, response.getUserId());
        }
    }

    /**
     * Extend the UserMapper class to overcome the issue of UnsatisfiedLinkError exception while mocking native methods in UserMapper class.
     */
    static class MockUserMapper extends UserMapper {
        public MockUserMapper() {
        }

        @Override
        public CertificateResponse getUserIDForCertificate(byte[] var1) {
            return new CertificateResponse(MF_USER, 0, 0, 0);
        }

        @Override
        public MapperResponse getUserIDForDN(String var1, String var2) {
            return new MapperResponse(MF_USER, 0, 0, 0, 0);
        }
    }
}
