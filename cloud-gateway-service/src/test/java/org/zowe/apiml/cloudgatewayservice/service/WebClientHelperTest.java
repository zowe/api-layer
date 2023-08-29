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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.zowe.apiml.security.HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED;

class WebClientHelperTest {
    static final char[] PASSWORD = "password".toCharArray(); // NOSONAR
    private static final char[] WRONG_PASSWORD = "wrong_password".toCharArray(); // NOSONAR
    static final String KEYSTORE_PATH = "../keystore/localhost/localhost.keystore.p12";

    @Nested
    class WhenLoading {
        @Test
        void givenWrongPath_thenThrowException() {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> WebClientHelper.load("../wrong/path", PASSWORD));
        }

        @Test
        void givenCorrectPath_thenLoadSSLContext() {
            final SslContext sslContext = WebClientHelper.load(KEYSTORE_PATH, PASSWORD);
            assertThat(sslContext).isNotNull();
        }

        @Test
        void givenWrongPassword_httpConfigErrorIsExpected() {
            HttpsConfigError error = catchThrowableOfType(() -> WebClientHelper.load("../keystore/localhost/localhost.keystore.p12", WRONG_PASSWORD), HttpsConfigError.class);
            assertThat(error.getCode()).isEqualTo(HTTP_CLIENT_INITIALIZATION_FAILED);
            assertThat(error.getConfig()).isNull();
        }
    }
}
