/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class UrlUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testTrimSlashes() {
        assertEquals("abc", UrlUtils.trimSlashes("abc"));
        assertEquals("abc", UrlUtils.trimSlashes("abc/"));
        assertEquals("abc", UrlUtils.trimSlashes("/abc"));
        assertEquals("abc", UrlUtils.trimSlashes("/abc/"));
        assertEquals("", UrlUtils.trimSlashes(""));
        assertEquals("/", UrlUtils.trimSlashes("///"));
        assertEquals("", UrlUtils.trimSlashes("//"));
    }

    @Test
    public void getEncodedUrlTest() {
        assertEquals("api-v1", UrlUtils.getEncodedUrl("api/v1"));
        assertEquals("-api-v1", UrlUtils.getEncodedUrl("/api/v1"));
        assertEquals("-api-v1-", UrlUtils.getEncodedUrl("/api/v1/"));

        assertNotNull(UrlUtils.getEncodedUrl(null));
    }

    @Test
    public void removeFirstAndLastSlash() {
        assertNull(UrlUtils.removeFirstAndLastSlash(null));

        String whiteSpace = "       ";
        assertEquals("", UrlUtils.removeFirstAndLastSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("blah", UrlUtils.removeFirstAndLastSlash(hasSlashes));
    }

    @Test
    public void addFirstSlash() {
        assertNull(UrlUtils.addFirstSlash(null));

        String whiteSpace = "       ";
        assertEquals("/", UrlUtils.addFirstSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah/", UrlUtils.addFirstSlash(hasSlashes));
    }

    @Test
    public void removeLastSlash () {
        assertNull(UrlUtils.removeLastSlash(null));

        String whiteSpace = "       ";
        assertEquals("", UrlUtils.removeLastSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah", UrlUtils.removeLastSlash(hasSlashes));
    }
}
