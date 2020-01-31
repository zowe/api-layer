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

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class CompositeKeyGeneratorTest {

    @Test
    public void generate() throws NoSuchMethodException {
        CompositeKeyGenerator g = new CompositeKeyGenerator();

        Object target = new Object();
        Method method = this.getClass().getDeclaredMethod("generate");

        assertSame(CompositeKey.EMPTY, g.generate(target, method, (Object[]) null));
        assertSame(CompositeKey.EMPTY, g.generate(target, method));

        assertEquals("a", g.generate(target, method, "a"));

        Object param = new Object[]{"a"};
        Object response = g.generate(target, method, param);
        assertTrue(response instanceof CompositeKey);
        assertEquals(param, ((CompositeKey) response).get(0));

        assertEquals(new CompositeKey("a", "b"), g.generate(target, method, "a", "b"));
        assertEquals(new CompositeKey(new String[]{"a"}, "b"), g.generate(target, method, new String[]{"a"}, "b"));
    }

}
