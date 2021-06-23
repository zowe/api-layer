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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CookieUtilTest {

    @Test
    void testSetCookie() {
        assertEquals("a=bc", CookieUtil.setCookie(null, "a", "bc"));
        assertEquals("a=bc", CookieUtil.setCookie("", "a", "bc"));
        assertEquals("a=1;b=2;c=4", CookieUtil.setCookie("a=1;c=3;b=2", "c", "4"));
        assertEquals("name=value", CookieUtil.setCookie(";", "name", "value"));
        assertEquals("name=value", CookieUtil.setCookie(";;;", "name", "value"));
        assertEquals("a=1;b=2;null=null", CookieUtil.setCookie("a=1;b=2", null, null));
    }

    @Test
    void removeCookie() {
        String c = "a=1;b=2";
        assertSame(c, CookieUtil.removeCookie(c, "c"));
        assertEquals("", CookieUtil.removeCookie("a=b", "a"));
        assertEquals("a=1;c=3", CookieUtil.removeCookie("a=1;b=2;c=3", "b"));
        assertEquals("", CookieUtil.removeCookie("a=1;a=2;a=3", "a"));
    }

    @Nested
    class WhenGetSetCookieHeader {
        private static final String NAME = "name";
        private static final String VALUE = "value";
        private static final String COMMENT = "comment";
        private static final String PATH = "/";
        private static final String SAME_SITE = "samesite";

        @Test
        void givenAllAttributesSet_thenReturnSetCookieWithAttributes() {
            String setCookieHeader = CookieUtil.setCookieHeader(
                NAME, VALUE, COMMENT, PATH, SAME_SITE, 1, true, true
            );

            assertEquals(setCookieHeader, String.format("%s=%s; Comment=%s; Path=%s; SameSite=%s; Max-Age=%d; HttpOnly; Secure;",
                NAME,
                VALUE,
                COMMENT,
                PATH,
                SAME_SITE,
                1
            ));
        }

        @Test
        void givenNullMaxAge_thenReturnSetCookieWithNoMaxAge() {
            String setCookieHeader = CookieUtil.setCookieHeader(
                NAME, VALUE, COMMENT, PATH, SAME_SITE, null, true, true
            );
            assertThat(setCookieHeader, not(containsString("Max-Age")));
        }

        @Test
        void givenNotHttpOnly_thenReturnSetCookieWithoutHttpOnly() {
            String setCookieHeader = CookieUtil.setCookieHeader(
                NAME, VALUE, COMMENT, PATH, SAME_SITE, 1, false, true
            );
            assertThat(setCookieHeader, not(containsString("HttpOnly")));
        }

        @Test
        void givenNotSecure_thenReturnSetCookieWithoutSecure() {
            String setCookieHeader = CookieUtil.setCookieHeader(
                NAME, VALUE, COMMENT, PATH, SAME_SITE, 1, true, false
            );
            assertThat(setCookieHeader, not(containsString("Secure")));
        }
    }
}
