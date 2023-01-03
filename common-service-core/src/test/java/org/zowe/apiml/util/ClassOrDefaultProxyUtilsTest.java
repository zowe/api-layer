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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ClassOrDefaultProxyUtilsTest {

    private static final String VALUE = "theValue";
    private static final String MOCKED_VALUE = "mock";

    private static String voidResponse;

    @Test
    void testBasics() {
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
    void specificConstructorCanBeInvoked() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        TestInterface1 ti;

        ti = ClassOrDefaultProxyUtils.createProxyByConstructor(TestInterface1.class, ConstructorTestImplementation.class.getName(),
                TestImplementation1B::new, new Class[]{String.class}, new Object[]{"Spejbl"});
        assertEquals("Spejbl", ti.method1());

        ti = ClassOrDefaultProxyUtils.createProxyByConstructor(TestInterface1.class, ConstructorTestImplementation.class.getName(),
                TestImplementation1B::new, new Class[]{int.class}, new Object[]{5});
        assertEquals("Zeryk_5", ti.method2("dummy", 0));
    }

    @Test
    void testOverride() {
        TestInterface2 ti;

        // create foreign class (without interface implementation) by string
        ti = ClassOrDefaultProxyUtils.createProxy(TestInterface2.class, TestImplementation2.class.getName(), TestImplementation2::new);

        // method1 is not overrided
        assertEquals("response_2_1", ti.method1());

        // getImplementationClass is override with base implementation
        assertEquals(TestImplementation2.class, ((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) ti).getImplementationClass());
    }

    @Test
    void testExceptionMapping() {
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
    void testExceptionMappingUnkwnown() {
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

    @Test
    void testMissingMethod() {
        String className = Object.class.getName();
        Exception e = assertThrows(ExceptionMappingError.class, () -> {
            ClassOrDefaultProxyUtils.createProxy(TestInterface1.class, className, TestImplementation1B::new);
        });
        assertTrue(e.getMessage().contains(" was not found on class java.lang.Object"));
    }

    class InnerClass implements TestInterface1 {
        private String name;

        @Override
        public String method1() {
            return "Hello world!";
        }

        @Override
        public String method2(String x, int y) {
            return x + y;
        }

        @Override
        public void method3() {
        }
    }

    @Test
    void testSyntentic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        new InnerClass().name = "toCompilarMakeASyntheticMethod";

        TestInterface1Super ti = ClassOrDefaultProxyUtils.createProxy(TestInterface1Super.class, "unknownClassName", InnerClass::new);
        Optional<Method> jacocoMethod = Arrays.stream(InnerClass.class.getDeclaredMethods()).filter(x -> x.isSynthetic()).filter(x -> x.getParameterTypes().length == 0).findFirst();
        assertTrue(jacocoMethod.isPresent());

        Method method = jacocoMethod.get();
        method.setAccessible(true);
        method.invoke(ti);
    }

    @Test
    void testImplementationWithExtends() {
        TestInterface2 ti = ClassOrDefaultProxyUtils.createProxy(TestInterface2.class, "unknownClassName", Extends::new);
        assertEquals("method1Response", ti.method1());
    }

    @Test
    void testWrongMapping() {
        TestInterface1 ti = ClassOrDefaultProxyUtils.createProxy(TestInterface1.class, "unknownClassName", TestImplementation1B::new);
        ((Map<String, Object>) ReflectionTestUtils.getField(ReflectionTestUtils.getField(ti, "h"), "mapping")).clear();
        try {
            ti.method1();
            fail();
        } catch (ExceptionMappingError e) {
            assertTrue(e.getMessage().startsWith("Cannot found method "));
        }
    }

    @Test
    void testExceptionMappingSourceMethod() {
        Exception e = assertThrows(ExceptionMappingError.class, () -> {
            new ClassOrDefaultProxyUtils.ByMethodName<>("java.lang.NullPointerException", NullPointerException.class, "getOurStackTraceX");
        });
        assertEquals("Cannot find method getOurStackTraceX in java.lang.NullPointerException to map exceptions", e.getMessage());

        e = assertThrows(ExceptionMappingError.class, () -> {
            new ClassOrDefaultProxyUtils.ByMethodName<>("java.lang.NullPointerException", NullPointerException.class, "getCause");
        });
        assertEquals("Cannot find constructor on java.lang.NullPointerException with [class java.lang.Throwable]", e.getMessage());
    }

    @Test
    void testCannotConstructException() throws NullPointerExceptionPrivate, NullPointerExceptionException {
        Exception e = assertThrows(ExceptionMappingError.class, () -> {
            new ClassOrDefaultProxyUtils.ByMethodName<>("java.lang.NullPointerException", NullPointerExceptionPrivate.class, "getMessage");
        });
        assertEquals("Cannot find constructor on java.lang.NullPointerException with [class java.lang.String]", e.getMessage());

        Exception nullPtrExceptionPrivate = new NullPointerExceptionPrivate("x");
        final ClassOrDefaultProxyUtils.ByMethodName<?> proxyUtilsNullPrivate = 
            new ClassOrDefaultProxyUtils.ByMethodName<>("org.zowe.apiml.util.ClassOrDefaultProxyUtilsTest$NullPointerExceptionPrivate", NullPointerException.class, "getMsg");
        e = assertThrows(ExceptionMappingError.class, () -> proxyUtilsNullPrivate.apply(nullPtrExceptionPrivate));
        assertTrue(e.getMessage().contains("Cannot invoke method private"));

        Exception nullPtrException = new NullPointerException("x");
        final ClassOrDefaultProxyUtils.ByMethodName<?> proxyUtilsNull = 
            new ClassOrDefaultProxyUtils.ByMethodName<>("java.lang.NullPointerException", NullPointerExceptionException.class, "getMessage");
        e = assertThrows(ExceptionMappingError.class, () -> proxyUtilsNull.apply(nullPtrException));
        assertTrue(e.getMessage().startsWith("Cannot construct exception"));
    }

    @Nested
    class GivenStaticMethods {

        @Test
        void whenProxied_thenCallOriginalMethod() {
            StaticMethods staticMethods = ClassOrDefaultProxyUtils.createProxy(
                StaticMethods.class,
                "org.zowe.apiml.util.ClassOrDefaultProxyUtilsTest$StaticMethodsTestClass",
                    StaticMethodsMockClass::new
            );
            assertEquals(VALUE, staticMethods.getValue());
        }

    }

    @Nested
    class GivenInvalidMapping {

        @Test
        void whenClassIsMissing_thenCallMock() {
            StaticMethods staticMethods = ClassOrDefaultProxyUtils.createProxy(
                    StaticMethods.class,
                    "unknown.Class",
                    StaticMethodsMockClass::new
            );
            assertEquals(MOCKED_VALUE, staticMethods.getValue());
        }

        @Test
        void whenMethodDefinitionsAreNotMatching_thenUseMock() {
            StaticMethods staticMethods = ClassOrDefaultProxyUtils.createProxy(
                    StaticMethods.class,
                    "org.zowe.apiml.util.ClassOrDefaultProxyUtilsTest$InvalidStaticMethodsTestClass",
                    StaticMethodsMockClass::new
            );
            assertEquals(MOCKED_VALUE, staticMethods.getValue());
        }

    }

    interface StaticMethods {

        String getValue();

    }

    class StaticMethodsMockClass implements StaticMethods {

        @Override
        public String getValue() {
            return MOCKED_VALUE;
        }

    }

    public static class StaticMethodsTestClass {

        private StaticMethodsTestClass() {
        }

        public static String getValue() {
            return VALUE;
        }

    }

    public static class InvalidStaticMethodsTestClass {

        private InvalidStaticMethodsTestClass() {
        }

    }

    private static class NullPointerExceptionPrivate extends Exception {

        private NullPointerExceptionPrivate(String arg) {}

        private String getMsg() {
            return "x";
        }

    }

    public static class NullPointerExceptionException extends Exception {

        public NullPointerExceptionException(String arg) {
            throw new RuntimeException("an error");
        }

        private String getMsg() {
            return "x";
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

    public class ExtendsSuper {

        public String method1() {
            return "method1Response";
        }

    }

    public class Extends extends ExtendsSuper implements TestInterface2 {

        @Override
        public Class<?> getImplementationClass() {
            return this.getClass();
        }

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

    public static class ConstructorTestImplementation {

        private String name = "Zeryk";
        private int number = 5;

        public ConstructorTestImplementation(String string) {
            this.name = string;
        }

        public ConstructorTestImplementation(int nePetkuNe) {
            this.number = nePetkuNe;
        }

        public String method1() {
            return name;
        }

        public String method2(String x, int y) {
            return name + "_" + number;
        }

        public void method3() {

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
