/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

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
    public void getEncodedUrlTest () {
        assertEquals(UrlUtils.getEncodedUrl("api/v1"), "api-v1");
        assertEquals(UrlUtils.getEncodedUrl("/api/v1"), "-api-v1");
        assertEquals(UrlUtils.getEncodedUrl("/api/v1/"), "-api-v1-");
        assertNotEquals(UrlUtils.getEncodedUrl(null), "api-v1");
        assertNotEquals(UrlUtils.getEncodedUrl(null), "null");
        assertNotEquals(UrlUtils.getEncodedUrl(null), null);

    }

    @Test
    public void validateUrlTest_ValidUrl() throws MalformedURLException {
        UrlUtils.validateUrl("https://www.google.com");
    }

    @Test
    public void validateUrlTest_BadProtocol() throws MalformedURLException {
        thrown.expect(MalformedURLException.class);
        thrown.expectMessage(CoreMatchers.containsString("Invalid URL"));

        UrlUtils.validateUrl("httpD://blah.com/pa=pa=path");
    }

    @Test
    public void removeFirstAndLastSlash () {
        assertNull(UrlUtils.removeFirstAndLastSlash(null));

        String whiteSpace = "       ";
        assertTrue(UrlUtils.removeFirstAndLastSlash(whiteSpace).isEmpty());

        String hasSlashes = "  /blah/   ";
        assertEquals(UrlUtils.removeFirstAndLastSlash(hasSlashes), "blah");
    }

    @Test
    public void addFirstSlash () {
        assertNull(UrlUtils.addFirstSlash(null));

        String whiteSpace = "       ";
        assertEquals(UrlUtils.addFirstSlash(whiteSpace), "/");

        String hasSlashes = "  /blah/   ";
        assertEquals(UrlUtils.addFirstSlash(hasSlashes), "/blah/");
    }

    @Test
    public void removeLastSlash () {
        assertNull(UrlUtils.removeLastSlash(null));

        String whiteSpace = "       ";
        assertTrue(UrlUtils.removeLastSlash(whiteSpace).isEmpty());

        String hasSlashes = "  /blah/   ";
        assertEquals(UrlUtils.removeLastSlash(hasSlashes), "/blah");
    }
/*
    @Test
    public void testGetHostIPAddress(String fqdn) throws UnknownHostException {
        UrlUtils.getHostIPAddress(fqdn);
    }*/
}
