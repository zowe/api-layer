/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.security;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;

//@Slf4j
public class SecurityTestUtils {
    private static final String STORE_PASSWORD = "password"; // NOSONAR

    public static HttpsConfig.HttpsConfigBuilder correctHttpsSettings() throws IOException {
        return SecurityTestUtils.correctHttpsKeyStoreSettings()
            .trustStore(pathFromRepository("keystore/localhost/localhost.truststore.p12"))
            .trustStorePassword(STORE_PASSWORD);
    }

    public static HttpsConfig.HttpsConfigBuilder correctHttpsKeyStoreSettings() throws IOException {
        return HttpsConfig.builder().protocol("TLSv1.2")
            .keyStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost.keystore.p12"))
            .keyStorePassword(STORE_PASSWORD).keyPassword(STORE_PASSWORD);
    }

    public static String pathFromRepository(String path) {
        String newPath = "../" + path;
        try {
            return new File(newPath).getCanonicalPath();
        } catch (IOException e) {
//            log.error("Error opening file {}", newPath, e);
            fail("Invalid repository path: " + newPath);
            return null;
        }
    }
}
