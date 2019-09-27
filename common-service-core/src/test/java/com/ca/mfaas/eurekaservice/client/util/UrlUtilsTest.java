/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

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
        //if (size > 0) {
        for (int n = 0; n < size; n++) {
            System.out.println("     url[" + n + "]=" + urls.get(n));
        }
    }
}
