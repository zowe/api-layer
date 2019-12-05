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
    public void removeLastSlash() {
        assertNull(UrlUtils.removeLastSlash(null));

        String whiteSpace = "       ";
        assertEquals("", UrlUtils.removeLastSlash(whiteSpace));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah", UrlUtils.removeLastSlash(hasSlashes));
    }

    @Test
    public void testGetHostIPAddress_DoesNotExist() throws UnknownHostException {
        thrown.expect(UnknownHostException.class);

        String fqdn = "Does-Not-Exist";
        String ipAddress = UrlUtils.getHostIPAddress(fqdn);
        assertNull(ipAddress);
    }

    @Test
    public void testGetHostIPAddress_Ok() throws UnknownHostException {
        String fqdn = "www.google.com";
        String ipAddress = UrlUtils.getHostIPAddress(fqdn);
        assertNotNull(ipAddress);
    }

    @Test
    public void testGetIPAddressFromUrl_Ok() throws UnknownHostException, MalformedURLException {
        String fqdn = "https://www.google.com";
        String ipAddress = UrlUtils.getIpAddressFromUrl(fqdn);
        assertNotNull(ipAddress);
    }

    @Test
    public void testGetIPAddressFromUrl_BAD_PROTOCOL() throws UnknownHostException {
        thrown.expect(UnknownHostException.class);

        String fqdn = "httpp://www.google.com";
        String ipAddress = UrlUtils.getHostIPAddress(fqdn);
        assertNull(ipAddress);
    }

    @Test
    public void testGetIPAddressFromUrl_NULL_Address() throws UnknownHostException {
        thrown.expect(UnknownHostException.class);

        String fqdn = "http://www.google.co";
        String ipAddress = UrlUtils.getHostIPAddress(fqdn);
        assertNull(ipAddress);
    }

    @Test
    public void testValidateUrl_OK() throws UnknownHostException {
        thrown.expect(UnknownHostException.class);

        String fqdn = "http://www.google.co";
        String ipAddress = UrlUtils.getHostIPAddress(fqdn);
        assertNull(ipAddress);
    }

    @Test
    public void testValidateUrl_InvalidProtocol() throws MalformedURLException {
        thrown.expect(MalformedURLException.class);

        UrlUtils.validateUrl("httpN://www.google.com");
    }

    @Test
    public void testValidateUrl_InvalidTLD() throws MalformedURLException {
        thrown.expect(MalformedURLException.class);

        UrlUtils.validateUrl("://www.google.com");
    }
}
