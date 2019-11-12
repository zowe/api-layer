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

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UrlUtilsTest {
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
    public void getEndPointsTest() {
        List<String> urls = UrlUtils.getHostBaseUrls();
        int size = (urls == null) ? 0 : urls.size();
        System.out.println("urls size: " + size);
        for (int n = 0; n < size; n++) {
            System.out.println("     url[" + n + "]=" + urls.get(n));
        }
    }


    @Test
    public void getEncodedUrlTest () {
        assertEquals(UrlUtils.getEncodedUrl("api/v1"), "api-v1");
        assertEquals(UrlUtils.getEncodedUrl("/api/v1"), "-api-v1");
        assertEquals(UrlUtils.getEncodedUrl("/api/v1/"), "-api-v1-");
        assertNotEquals(UrlUtils.getEncodedUrl(null), "api-v1");
        assertNotEquals(UrlUtils.getEncodedUrl(null), "null");
        assertNotEquals(UrlUtils.getEncodedUrl(null), null);

    }

    @Test
    public void validateUrl () {

    }

    @Test
    public void removeFirstAndLastSlash () {

    }

    @Test
    public void addFirstSlash () {

    }

    @Test
    public void removeLastSlash () {

    }
}
