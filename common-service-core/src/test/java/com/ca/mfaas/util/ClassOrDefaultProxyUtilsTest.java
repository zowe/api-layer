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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Test;

import java.lang.reflect.UndeclaredThrowableException;

import static org.junit.Assert.*;

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

    @Test
    public void testExceptionMapping() {
        TestInterfaceException tie;

        tie = ClassOrDefaultProxyUtils.createProxy(
            TestInterfaceException.class,
            TestImplementationException.class.getName(),
            () -> {
                fail("Test failed, missing implementation for test");
                return null;
            },
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                TestSourceException.class.getName(),
                TestTargetException.class,
                "getParam1", "getParam2", "getParam3"
            )
        );

        Object testObject = new Object();
        try {
            tie.doSomething(testObject, "testString", 123);
            fail();
        } catch (TestTargetException e) {
            assertSame(testObject, e.getObject());
            assertEquals("testString", e.getString());
            assertEquals(123, e.getNumber());
        }
    }

    @Test
    public void testExceptionMappingUnkwnown() {
        TestInterfaceException tie;

        tie = ClassOrDefaultProxyUtils.createProxy(
            TestInterfaceException.class,
            TestImplementationException.class.getName(),
            () -> {
                fail("Test failed, missing implementation for test");
                return null;
            },
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "Unkwnown",
                TestTargetException.class,
                "getParam1", "getParam2", "getParam3"
            )
        );

        Object testObject = new Object();
        try {
            tie.doSomething(testObject, "testString", 123);
            fail();
        } catch (TestTargetException e) {
            // there is missing mapping, it will throw source exception
            fail();
        } catch (UndeclaredThrowableException e) {
            assertTrue(e.getCause() instanceof TestSourceException);
        }
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

    @Getter
    @AllArgsConstructor
    public static class TestSourceException extends Exception {

        private static final long serialVersionUID = -5824895837948769100L;

        private final Object param1;
        private final String param2;
        private final int param3;

    }

    @Getter
    @AllArgsConstructor
    public static class TestTargetException extends Exception {

        private static final long serialVersionUID = -3566209353622503908L;

        private final Object object;
        private final String string;
        private final int number;

    }

    public interface TestInterfaceException {

        public void doSomething(Object param1, String param2, int param3) throws TestTargetException;

    }

    public static class TestImplementationException {

        public void doSomething(Object param1, String param2, int param3) throws TestSourceException {
            throw new TestSourceException(param1, param2, param3);
        }

    }

}
