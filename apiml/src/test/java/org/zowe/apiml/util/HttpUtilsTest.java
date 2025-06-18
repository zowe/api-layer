/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
    "apiml.security.auth.cookie-properties.cookie-name=test-token",
    "apiml.security.auth.cookie-properties.cookie-path=/",
    "apiml.security.auth.cookie-properties.cookie-same-site=Lax",
    "apiml.security.auth.cookie-properties.cookie-max-age=3600",
    "apiml.security.auth.cookie-properties.cookie-secure=true"
})
@Import(HttpUtils.class)
@EnableConfigurationProperties(AuthConfigurationProperties.class)
class HttpUtilsTest {

    @Autowired
    private HttpUtils httpUtils;
    @Test
    void testCreateResponseCookie() {
        String jwt = "sample.jwt.token";
        ResponseCookie cookie = httpUtils.createResponseCookie(jwt);
        assertEquals("test-token", cookie.getName());
        assertEquals(jwt, cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals(3600, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }
}
