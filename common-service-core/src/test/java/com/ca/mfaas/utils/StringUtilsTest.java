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

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void removeFirstAndLastOccurrenceTest() {
        assertNull(StringUtils.removeFirstAndLastOccurrence(null, "any-string"));

        String whiteSpace = "       ";
        assertTrue(StringUtils.removeFirstAndLastOccurrence(whiteSpace, "any-string").isEmpty());

        String hasSlashes = "  /blah/   ";
        assertEquals(StringUtils.removeFirstAndLastOccurrence(hasSlashes, "/"), "blah");
    }

    @Test
    public void removeLastOccurrenceTest() {
        assertNull(StringUtils.removeLastOccurrence(null, "any-string"));

        String whiteSpace = "       ";
        assertTrue(StringUtils.removeLastOccurrence(whiteSpace, "any-string").isEmpty());

        String hasSlashes = "  /blah/   ";
        assertEquals(StringUtils.removeLastOccurrence(hasSlashes, "/"), "/blah");
    }

    @Test
    public void prependSubstringTest() {
        assertNull(StringUtils.prependSubstring(null, "any-string"));

        assertNull(StringUtils.prependSubstring("", null));

        String whiteSpace = "       ";
        assertEquals(StringUtils.prependSubstring(whiteSpace, "any-string"), "any-string");
        assertEquals(StringUtils.prependSubstring("any-string", "any-string"), "any-string");
        assertEquals(StringUtils.prependSubstring("any-string", "any-string", false), "any-stringany-string");
        assertEquals(StringUtils.prependSubstring(whiteSpace, "any-string", true, false), "any-string" + whiteSpace);
        assertEquals(StringUtils.prependSubstring(whiteSpace, "any-string", false, false), "any-string" + whiteSpace);

        String hasSlashes = "  /blah/   ";
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", true, true), "/blah/");
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", false, true), "//blah/");
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", false, false), "/  /blah/   ");
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", true, false), "/  /blah/   ");
    }
}
