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
        assertEquals("", StringUtils.removeFirstAndLastOccurrence(whiteSpace, "any-string"));

        String hasSlashes = "  /blah/   ";
        assertEquals("blah", StringUtils.removeFirstAndLastOccurrence(hasSlashes, "/"));
    }

    @Test
    public void removeLastOccurrenceTest() {
        assertNull(StringUtils.removeLastOccurrence(null, "any-string"));

        String whiteSpace = "       ";
        assertEquals("", StringUtils.removeLastOccurrence(whiteSpace, "any-string"));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah", StringUtils.removeLastOccurrence(hasSlashes, "/"));
    }

    @Test
    public void prependSubstringTest() {
        assertNull(StringUtils.prependSubstring(null, "any-string"));

        assertNull(StringUtils.prependSubstring("", null));

        String whiteSpace = "       ";
        assertEquals("any-string", StringUtils.prependSubstring(whiteSpace, "any-string"));
        assertEquals("any-string", StringUtils.prependSubstring("any-string", "any-string"));
        assertEquals("any-stringany-string", StringUtils.prependSubstring("any-string", "any-string", false));
        assertEquals("any-string" + whiteSpace, StringUtils.prependSubstring(whiteSpace, "any-string", true, false));
        assertEquals("any-string" + whiteSpace, StringUtils.prependSubstring(whiteSpace, "any-string", false, false));

        String hasSlashes = "  /blah/   ";
        assertEquals("/blah/", StringUtils.prependSubstring(hasSlashes, "/", true, true));
        assertEquals("//blah/", StringUtils.prependSubstring(hasSlashes, "/", false, true));
        assertEquals("/  /blah/   ", StringUtils.prependSubstring(hasSlashes, "/", false, false));
        assertEquals("/  /blah/   ", StringUtils.prependSubstring(hasSlashes, "/", true, false));
    }
}
