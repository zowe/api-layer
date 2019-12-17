package com.ca.mfaas.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;

/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
@RunWith(JUnit4.class)
public class ClassOrDefaultProxyUtilsTest {

    private static String voidResponse;

    @Test
    public void testBasics() {
        TestInterface1 ti;

        // create foreign class (without interface implementation) by string
        ti = ClassOrDefaultProxyUtils.createProxy(TestInterface1.class, TestImplementation1A.class.getName(), TestImplementation1B::new);
        assertEquals("response_1a_1", ti.method1());
        assertEquals("response_1a_2_X_3", ti.method2("X", 3));
        assertTrue(((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) ti).isUsingBaseImplementation());
        assertEquals(TestImplementation1A.class, ((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) ti).getImplementationClass());

        voidResponse = null;
        ti.method3();
        assertEquals("response_1a_3", voidResponse);

        // foreign class is not known, use internal one
        ti = ClassOrDefaultProxyUtils.createProxy(TestInterface1.class, "uknown", TestImplementation1B::new);
        assertEquals("response_1b_1", ti.method1());
        assertEquals("response_1b_2_X_3", ti.method2("X", 3));
        assertFalse(((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) ti).isUsingBaseImplementation());
        assertEquals(TestImplementation1B.class, ((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) ti).getImplementationClass());

        voidResponse = null;
        ti.method3();
        assertEquals("response_1b_3", voidResponse);
    }

    @Test
    public void testOverride() {
        TestInterface2 ti;

        // create foreign class (without interface implementation) by string
        ti = ClassOrDefaultProxyUtils.createProxy(TestInterface2.class, TestImplementation2.class.getName(), TestImplementation2::new);

        // method1 is not overrided
        assertEquals("response_2_1", ti.method1());

        // getImplementationClass is override with base implementation
        assertEquals(TestImplementation2.class, ((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) ti).getImplementationClass());
    }

    public interface TestInterface1Super {

        public String method1();

    }

    public interface TestInterface1 extends TestInterface1Super {

        public String method2(String x, int y);

        public void method3();

    }

    public interface TestInterface2 {

        public String method1();

        public Class<?> getImplementationClass();

    }

    public static class TestImplementation1A {

        public String method1() {
            return "response_1a_1";
        }

        public String method2(String x, int y) {
            return "response_1a_2_" + x + "_" + y;
        }

        public void method3() {
            voidResponse = "response_1a_3";
        }

    }

    public static class TestImplementation1B implements TestInterface1 {

        @Override
        public String method1() {
            return "response_1b_1";
        }

        @Override
        public String method2(String x, int y) {
            return "response_1b_2_" + x + "_" + y;
        }

        @Override
        public void method3() {
            voidResponse = "response_1b_3";
        }

    }

    public static class TestImplementation2 implements TestInterface2 {

        @Override
        public String method1() {
            return "response_2_1";
        }

        @Override
        public Class<?> getImplementationClass() {
            return Object.class;
        }

    }

}
