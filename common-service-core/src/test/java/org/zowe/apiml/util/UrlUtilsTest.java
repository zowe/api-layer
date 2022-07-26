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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlUtilsTest {

    @Test
    void testTrimSlashes() {
        assertEquals("abc", UrlUtils.trimSlashes("abc"));
        assertEquals("abc", UrlUtils.trimSlashes("abc/"));
        assertEquals("abc", UrlUtils.trimSlashes("/abc"));
        assertEquals("abc", UrlUtils.trimSlashes("/abc/"));
        assertEquals("", UrlUtils.trimSlashes(""));
        assertEquals("/", UrlUtils.trimSlashes("///"));
        assertEquals("", UrlUtils.trimSlashes("//"));
    }

    @Test
    void getEncodedUrlTest() {
        assertEquals("api-v1", UrlUtils.getEncodedUrl("api/v1"));
        assertEquals("-api-v1", UrlUtils.getEncodedUrl("/api/v1"));
        assertEquals("-api-v1-", UrlUtils.getEncodedUrl("/api/v1/"));

        assertNotNull(UrlUtils.getEncodedUrl(null));
    }

    @Test
    void removeFirstAndLastSlash() {
        assertNull(UrlUtils.removeFirstAndLastSlash(null));

        String whiteSpace = "       ";
        assertEquals("", UrlUtils.removeFirstAndLastSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("blah", UrlUtils.removeFirstAndLastSlash(hasSlashes));
    }

    @Test
    void addFirstSlash() {
        assertNull(UrlUtils.addFirstSlash(null));

        String whiteSpace = "       ";
        assertEquals("/", UrlUtils.addFirstSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah/", UrlUtils.addFirstSlash(hasSlashes));
    }

    @Test
    void removeLastSlash() {
        assertNull(UrlUtils.removeLastSlash(null));

        String whiteSpace = "       ";
        assertEquals("", UrlUtils.removeLastSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah", UrlUtils.removeLastSlash(hasSlashes));
    }

    
    @ParameterizedTest
    @ValueSource(strings = {"Does-Not-Exist", "httpp://www.google.com", "http://www.google.co"})
    void testGetHostIPAddress_UnknownHost(String fqdn) throws UnknownHostException {
        assertThrows(UnknownHostException.class, () -> UrlUtils.getHostIPAddress(fqdn));
    }

    @Test
    void testGetHostIPAddress_Ok() throws UnknownHostException {
        String fqdn = "www.google.com";
        String ipAddress = UrlUtils.getHostIPAddress(fqdn);
        assertNotNull(ipAddress);
    }

    @Test
    void testGetIPAddressFromUrl_Ok() throws UnknownHostException, MalformedURLException {
        String fqdn = "https://www.google.com";
        String ipAddress = UrlUtils.getIpAddressFromUrl(fqdn);
        assertNotNull(ipAddress);
    }
}
