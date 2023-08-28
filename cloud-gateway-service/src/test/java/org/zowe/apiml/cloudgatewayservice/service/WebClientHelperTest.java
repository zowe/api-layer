/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.HttpsConfigError;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebClientHelperTest {
    static final char[] PASSWORD = "password".toCharArray(); // NOSONAR
    private static final char[] WRONG_PASSWORD = "wrong_password".toCharArray(); // NOSONAR
    static final String KEYSTORE_PATH = "../keystore/localhost/localhost.keystore.p12";

    @Nested
    class WhenLoading {
        @Test
        void givenWrongPath_thenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> WebClientHelper.load("../wrong/path", PASSWORD));
        }

        @Test
        void givenCorrectPath_thenLoadSSLContext() {
            final SslContext sslContext = WebClientHelper.load(KEYSTORE_PATH, PASSWORD);
            assertNotNull(sslContext);
        }

        @Test
        void givenWrongPassword_thenExit() {
            assertThrows(HttpsConfigError.class, () -> WebClientHelper.load("../keystore/localhost/localhost.keystore.p12", WRONG_PASSWORD));
        }
    }
}
