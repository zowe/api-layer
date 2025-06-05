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

import org.apache.tomcat.util.http.SameSiteCookies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties.CookieProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpUtilsTest {

    private HttpUtils httpUtils;

    @BeforeEach
    void setUp() {
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setCookieName("test-token");
        cookieProperties.setCookiePath("/");
        cookieProperties.setCookieSameSite(SameSiteCookies.LAX);
        cookieProperties.setCookieMaxAge(3600);
        cookieProperties.setCookieSecure(true);

        AuthConfigurationProperties authConfigProps = new AuthConfigurationProperties();
        authConfigProps.setCookieProperties(cookieProperties);

        httpUtils = new HttpUtils(authConfigProps);
    }

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
