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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SafResourceAccessDummyTest {

    private static final String ALL_LEVELS =
        "safAccess:\n" +
        " CLASS1:\n" +
        "  RESOURCE1:\n" +
        "   READ:\n" +
        "    - USER1\n" +
        "    - USER2\n" +
        " CLASS2:\n" +
        "  RESOURCE2:\n" +
        "   UPDATE:\n" +
        "    - USER3\n" +
        "   CONTROL:\n" +
        "    - USER4\n" +
        "  RESOURCE3:\n" +
        "   ALTER:\n" +
        "    - USER5\n";

    private static final String MULTIPLE_LEVELS =
        "safAccess:\n" +
            " CLASS:\n" +
            "  RESOURCE:\n" +
            "   READ:\n" +
            "    - USER\n" +
            "   UPDATE:\n" +
            "    - USER\n";

    InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testLoadSafFile_whenEmptyFile_thenNoCredentials() throws IOException {
        try (InputStream is = toInputStream("")) {
            SafResourceAccessVerifying verifying = new SafResourceAccessDummy(is);
            Map<?, ?> map = (Map<?, ?>) ReflectionTestUtils.getField(verifying, "resourceUserToAccessLevel");
            assertTrue(map.isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
        "CLASS1,RESOURCE1,READ,true,true,false,false,false",
        "CLASS2,RESOURCE2,UPDATE,false,false,true,true,false",
        "CLASS2,RESOURCE2,CONTROL,false,false,false,true,false",
        "CLASS2,RESOURCE2,ALTER,false,false,false,false,false",
        "CLASS2,RESOURCE3,ALTER,false,false,false,false,true",
        "CLASS1,RESOURCE3,READ,false,false,false,false,false"
    }, delimiter = ',')
    void testFullSafFile_whenTestCombination_thenEvaluateRight(
        String clazz, String resource, String level,
        boolean user1, boolean user2, boolean user3, boolean user4, boolean user5
    ) throws IOException {
        boolean[] allowedUsers = new boolean[] { user1, user2, user3, user4, user5 };
        try (InputStream is = toInputStream(ALL_LEVELS)) {
            SafResourceAccessVerifying verifying = new SafResourceAccessDummy(is);
            for (int i = 0; i < allowedUsers.length; i++) {
                assertEquals(
                    allowedUsers[i],
                    verifying.hasSafResourceAccess(
                        new UsernamePasswordAuthenticationToken("USER" + (i + 1),"token"),
                        clazz, resource, level
                    )
                );
            }
        }
    }

    @Test
    void testMultipleLevelsOnSameResourceAndUser_whenLoaded_thenUseHigherOne() throws IOException {
        try (InputStream is = toInputStream(MULTIPLE_LEVELS)) {
            SafResourceAccessVerifying verifying = new SafResourceAccessDummy(is);
            Authentication authentication = new UsernamePasswordAuthenticationToken("USER", "token");
            assertTrue(verifying.hasSafResourceAccess(authentication, "CLASS", "RESOURCE", "READ"));
            assertTrue(verifying.hasSafResourceAccess(authentication, "CLASS", "RESOURCE", "UPDATE"));
            assertFalse(verifying.hasSafResourceAccess(authentication, "CLASS", "RESOURCE", "CONTROL"));
        }
    }

    @Test
    void testLoading_whenExternalFileExist_thenLoadIt() throws IOException {
        File file = null;
        try {
            file = File.createTempFile("junit-saf", ".yml");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write("safAccess:\n CLASSTST:\n  RESOURCE:\n   READ:\n    - USERFILE\n".getBytes(StandardCharsets.UTF_8));
            }

            final File fileFinal = file;
            SafResourceAccessVerifying verifying = new SafResourceAccessDummy() {
                @Override
                protected File getFile() {
                    return fileFinal;
                }
            };
            Authentication authentication = new UsernamePasswordAuthenticationToken("USERFILE", "token");
            assertTrue(verifying.hasSafResourceAccess(authentication, "CLASSTST", "RESOURCE", "READ"));
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    void testLoading_whenExternalDoesntExists_thenLoadFromResources() throws IOException {
        File file = File.createTempFile("junit-saf2", ".yml");
        file.delete();
        assumeFalse(file.exists());

        SafResourceAccessVerifying verifying = new SafResourceAccessDummy();
        Authentication authentication = new UsernamePasswordAuthenticationToken("ZOWE", "token");
        assertTrue(verifying.hasSafResourceAccess(authentication, "ZOWE", "APIML.RES", "READ"));
    }

}
