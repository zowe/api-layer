package com.ca.mfaas.cache;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;

import static org.junit.Assert.*;

public class CompositeKeyTest {

    @Test
    public void testInit() {
        CompositeKey ck;

        ck = new CompositeKey();
        assertEquals(0, ck.size());

        ck = new CompositeKey((Object[]) null);
        assertEquals(0, ck.size());

        ck = new CompositeKey("a", "b", "c");
        assertEquals(new CompositeKey("a", "b", "c"), ck);
        assertNotEquals(new CompositeKey("a", "b", "d"), ck);
        assertEquals(3, ck.size());
        assertEquals("a", ck.get(0));
        assertEquals("b", ck.get(1));
        assertEquals("c", ck.get(2));
        try {
            ck.get(3);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
        try {
            ck.get(-1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        ck = new CompositeKey(new Object[] {"a", "b"}, "c");
        assertEquals(new CompositeKey(new Object[] {"a", "b"}, "c"), ck);
        assertNotEquals(new CompositeKey(new Object[] {"a", "b"}, "d"), ck);
        assertNotEquals(new CompositeKey(new Object[] {"a", "d"}, "c"), ck);
        assertNotEquals(new CompositeKey(new Object[] {"d", "b"}, "c"), ck);
    }

    @Test
    public void testToString() {
        assertEquals("CompositeKey []", new CompositeKey().toString());
        assertEquals("CompositeKey []", new CompositeKey((Object[]) null).toString());
        assertEquals("CompositeKey [a]", new CompositeKey("a").toString());
        assertEquals("CompositeKey [a,b]", new CompositeKey("a", "b").toString());
        assertEquals("CompositeKey [a,1,true]", new CompositeKey("a", 1, true).toString());
    }

}
