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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Purpose of this library of methods is to support multiple implementation and serve them as under one interface.
 *
 * It could be helpful if implementation is available only on specific environment (ie. libraries in IBM JVM, which
 * cannot be distributed with project.). It is necessary to generate new interface, which cover both implementation
 * (dummy for local purpose and original, represented as fully class name). This util will create implementation by
 * class name or create a local (dummy) implementation. Outside this library it will be used and programmer don't
 * need test if class exists or not.
 *
 * Created proxy offer also interface @link #MethodInvocationHandler to check state of created proxy. This is the
 * reason exclude method names getImplementationClass and isUsingBaseImplementation from proxied object. Any object
 * cannot have methods with the names without attributes. In case of conflict methods for checking state of proxy
 * has higher priority.
 *
 * How to use that:
 * 1. create interface A for proxied object (a subset of methods is enough)
 * 2. create dummy implementation B (must implement the interface)
 * 3. create proxy object
 *
 * A i=ClassOrDefaultProxyUtils.createProxy(A.class, "<full name of class>", () -> new B());
 *
 * 4. you can test if implementation is dummy or not
 *
 * if (!((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) i).isUsingBaseImplementation()) {
 *     log.error("The searched class was not found, use " +
 *      ((ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) i).isUsingBaseImplementation() +
 *      "instanceof");
 * }
 *
 */
@Slf4j
public final class ClassOrDefaultProxyUtils {

    /**
     * Create a proxy, which implement interfaceClass and ClassOrDefaultProxyState. This proxy will call object created
     * for defaultImplementation class. If this object is not available it will call defaultImplementation instance of.
     * Both implementationClassName and defaultImplementation have to have default constructor to be created.
     *
     * @param interfaceClass Interface of created proxy
     * @param implementationClassName Full name of prefer implementation
     * @param defaultImplementation Supplier to fetch implementation to use, if the prefer one is missing
     * @param <T> Common interface for prefer and default implementation
     * @param exceptionMappings handlers to map exception to custom class
     * @return Proxy object implementing interfaceClass and ClassOrDefaultProxyState
     */
    public static <T> T createProxy(Class<T> interfaceClass, String implementationClassName, Supplier<? extends T> defaultImplementation, ExceptionMapping<? extends Exception> ... exceptionMappings) {
        return createProxyByConstructor(interfaceClass, implementationClassName, defaultImplementation, new Class[]{}, new Object[]{}, exceptionMappings);
    }

    /**
     * Same as createProxy but with option to specify constructor signature and supply parameters
     *
     * @param interfaceClass Interface of created proxy
     * @param implementationClassName Full name of prefer implementation
     * @param defaultImplementation Supplier to fetch implementation to use, if the prefer one is missing
     * @param <T> Common interface for prefer and default implementation
     * @param constructorSignature Signature of requested constructor
     * @param constructorParams Parameters for requested constructor
     * @param exceptionMappings handlers to map exception to custom class
     * @return Proxy object implementing interfaceClass and ClassOrDefaultProxyState
     */
    public static <T> T createProxyByConstructor(Class<T> interfaceClass, String implementationClassName, Supplier<? extends T> defaultImplementation, Class[] constructorSignature, Object[] constructorParams, ExceptionMapping<? extends Exception> ... exceptionMappings) {
        ObjectUtil.requireNotNull(interfaceClass, "interfaceClass can't be null");
        ObjectUtil.requireNotEmpty(implementationClassName, "implementationClassName can't be empty");
        ObjectUtil.requireNotNull(defaultImplementation, "defaultImplementation can't be null");
        ObjectUtil.requireNotNull(constructorSignature, "constructorSignature can't be null");
        ObjectUtil.requireNotNull(constructorParams, "constructorParams can't be null");

        try {
            final Class<?> implementationClazz = Class.forName(implementationClassName);
            final Object implementation = implementationClazz.getDeclaredConstructor(constructorSignature).newInstance(constructorParams);
            return makeProxy(interfaceClass, implementation, true, exceptionMappings);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.warn("Implementation {} is not available with constructor signature {}, it will continue with default one {} : " + e.getLocalizedMessage(),
                implementationClassName, constructorSignature,  defaultImplementation);
        }

        return makeProxy(interfaceClass, defaultImplementation.get(), false, exceptionMappings);
    }

    private static <T> T makeProxy(Class<T> interfaceClass, Object implementation, boolean usingBaseImplementation, ExceptionMapping<? extends Exception> ... exceptionMappings) {
        return (T) Proxy.newProxyInstance(
            ClassOrDefaultProxyUtils.class.getClassLoader(),
            new Class<?>[] {interfaceClass, ClassOrDefaultProxyUtils.ClassOrDefaultProxyState.class},
            new MethodInvocationHandler(implementation, interfaceClass, usingBaseImplementation, exceptionMappings));
    }

    /**
     * Interface to check state of created proxy object
     */
    public interface ClassOrDefaultProxyState {

        /**
         *
         * @return class which is now proxied. It could be one of implementationClassName or defaultImplementation
         */
        Class<?> getImplementationClass();

        /**
         *
         * @return true if proxy use the original class, false if is using default (dummy) class
         */
        boolean isUsingBaseImplementation();

    }

    /**
     * Handler of proxy. This class prepare mapping of call and then invoke target method.
     */
    private static class MethodInvocationHandler implements InvocationHandler, ClassOrDefaultProxyState {

        private final Map<Method, EndPoint> mapping = new HashMap<>();

        private final boolean usingBaseImplementation;
        private final Object implementation;
        private final Class<?> interfaceClass;
        private final ExceptionMapping<? extends Exception>[] exceptionMappings;

        public MethodInvocationHandler(Object implementation, Class<?> interfaceClass, boolean usingBaseImplementation, ExceptionMapping<? extends Exception> ... exceptionMappings) {
            this.usingBaseImplementation = usingBaseImplementation;
            this.implementation = implementation;
            this.interfaceClass = interfaceClass;
            this.exceptionMappings = exceptionMappings;

            if (this.usingBaseImplementation) {
                for (ExceptionMapping<? extends Exception> exceptionMapping : exceptionMappings) {
                    if (!exceptionMapping.isInitialized()) {
                        log.error("Mapping of exception {} is not initialized", exceptionMapping);
                    }
                }
            }

            this.initMapping();
        }

        private EndPoint addMapping(Object target, Method caller, Method callee) {
            final EndPoint endPoint = new EndPoint(target, callee);
            mapping.put(caller, endPoint);
            return endPoint;
        }

        private Method findMethod(Class<?> clazz, Method method) {
            try {
                return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException nsme) {
                if (clazz == Object.class) {
                    throw new ExceptionMappingError("Cannot construct proxy", nsme);
                }
                return findMethod(clazz.getSuperclass(), method);
            }
        }

        private void fetchAllInterfaces(Class<?> interfaceClass, List<Class<?>> list) {
            list.add(interfaceClass);

            for (final Class<?> superInterface : interfaceClass.getInterfaces()) {
                fetchAllInterfaces(superInterface, list);
            }
        }

        private List<Class<?>> fetchAllInterfaces(Class<?> interfaceClass) {
            final List<Class<?>> output = new LinkedList<>();
            fetchAllInterfaces(interfaceClass, output);
            return output;
        }

        private void initMapping() {
            final Class<?> implementationClass = implementation.getClass();
            final Map<String, EndPoint> byName = new HashMap<>();

            // first check the state interface. It has higher priority, could rewrite previous mapping
            for (final Method method : ClassOrDefaultProxyState.class.getDeclaredMethods()) {
                final EndPoint endPoint = addMapping(this, method, method);
                byName.put(ObjectUtil.getMethodIdentifier(method), endPoint);
            }

            // second map methods of target
            for (Class<?> partInterfaceClass : fetchAllInterfaces(interfaceClass)) {
                for (final Method caller : partInterfaceClass.getDeclaredMethods()) {
                    // ignore methods of frameworks created during execution
                    if (caller.isSynthetic()) continue;

                    // try to find by name - avoid to multiple implementation for same method name, ie. getImplementationClass (proxy vs. implementation)
                    final EndPoint oldEndPoint = byName.get(ObjectUtil.getMethodIdentifier(caller));
                    if (oldEndPoint != null) {
                        // use same mapping like previous matching
                        mapping.put(caller, oldEndPoint);
                    } else {
                        // find it in implementation and make a mapping
                        try {
                            final Method callee = findMethod(implementationClass, caller);
                            final EndPoint newEndPoint = addMapping(implementation, caller, callee);
                            byName.put(ObjectUtil.getMethodIdentifier(caller), newEndPoint);
                        } catch (Exception e) {
                            throw new ExceptionMappingError("Method " + ObjectUtil.getMethodIdentifier(caller) + " was not found on " + implementationClass);
                        }
                    }
                }
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final EndPoint endPoint = mapping.get(method);

            if (endPoint == null) {
                throw new ExceptionMappingError(String.format("Cannot found method %s", method));
            }

            try {
                return endPoint.invoke(args);
            } catch (InvocationTargetException ite) {
                // thrown exception in proxied object
                Throwable t = ite.getCause();
                // if there is a mapping of exceptions, apply it to use custom Exception
                for (ExceptionMapping em : exceptionMappings) {
                    em.apply(t);
                }
                throw t;
            }
        }

        @Override
        public Class<?> getImplementationClass() {
            return implementation.getClass();
        }

        @Override
        public boolean isUsingBaseImplementation() {
            return usingBaseImplementation;
        }

        /**
         * Object define instance of object where should be invoked the method and contains also method instance. It is
         * prepare before to make invoke in the fastest way.
         */
        @Data
        public static final class EndPoint {

            private final Object target;
            private final Method method;

            public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
                return method.invoke(target, args);
            }

        }

    }

    /**
     * Interface to control an exception mapping. It offer base method to make control on exception conversion
     * from Handler side.
     * @param <T> Target exception (which is thrown in case of mapping)
     */
    public interface ExceptionMapping<T extends Exception> {

        /**
         * Method indicate if mapping was created well (to control from proxy handler)
         * @return true if mapping is ready to use otherwise false
         */
        boolean isInitialized();

        /**
         * Method will check if t is able to map. If yes, make mapping and throw new (mapped) exception
         * @param t Original exception
         * @throws T Type of mapped exception to throw in case of mapping is right to type of t
         */
        void apply(Throwable t) throws T;

    }

    /**
     * This exception mapper is based on getter in source exception. It allows to get from zero to N getters without
     * argument in source exception and use them as arguments in target constructor.
     *
     * You have to define same count of getter names (with same result type) as a constructor in target exception. The
     * order has to be also same.
     *
     * @param <T> Type of target exception
     */
    @Data
    public static class ByMethodName<T extends Exception> implements ExceptionMapping<T> {

        private final String sourceExceptionClassName;
        private final Function<Throwable, T> mappingFunction;

        /**
         * Constructor define all required values to make a mapping
         * @param sourceExceptionClassName Name of source exception's class type  {@see Class#getName()}
         * @param targetExceptionClass Type of exception to be thrown in case of successful mapping
         * @param methodNames names of getter without arguments in sources exception, which results will be use as constructor parameters
         */
        public ByMethodName(String sourceExceptionClassName, Class<T> targetExceptionClass, String...methodNames) {
            this.sourceExceptionClassName = sourceExceptionClassName;
            this.mappingFunction = getMappingFunction(sourceExceptionClassName, targetExceptionClass, methodNames);
        }

        /**
         * Find method with name and no arguments in the class hierarchy
         * @param clazz - base method to lookup
         * @param methodName - name of method
         * @return found method with name methodName and no arguments or null
         */
        private Method findMethod(Class<?> clazz, String methodName) {
            if (clazz == Object.class) return null;

            try {
                return clazz.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                return findMethod(clazz.getSuperclass(), methodName);
            }
        }

        /**
         * Method find constructor in target exception, prepare lambdas to get values from source exception and return
         * function to convert source exception to new (target) one
         * @param sourceExceptionClassName name of exception to map
         * @param targetExceptionClass exception which could be construct after mapping
         * @param methodNames names of methods without argument on source exception to get values into constructor to create target exception
         * @return function to mapping of exception
         */
        private Function<Throwable, T> getMappingFunction(String sourceExceptionClassName, Class<T> targetExceptionClass, String...methodNames ) {
            // find source exception
            final Class<Throwable> eClass;
            try {
                eClass = (Class<Throwable>) Class.forName(sourceExceptionClassName);
            } catch (ClassNotFoundException e) {
                log.debug("Exception {} is not available, it will not be mapped into {} : " + e, sourceExceptionClassName, targetExceptionClass);
                return null;
            }

            // find arguments of constructor and methods by names, methods should be without any arguments
            final List<Class<?>> argClasses = new LinkedList<>();
            final List<Function<Throwable, Object>> mapFunctions = new LinkedList<>();
            for (String methodName : methodNames) {
                final Method method = findMethod(eClass, methodName);
                if (method == null) {
                    throw new ExceptionMappingError("Cannot find method " + methodName + " in " + sourceExceptionClassName + " to map exceptions");
                }
                argClasses.add(method.getReturnType());
                mapFunctions.add(x -> {
                    try {
                        return method.invoke(x);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new ExceptionMappingError("Cannot invoke method " + method, e);
                    }
                });
            }

            // find the constructor and store functions to invoke then
            try {
                return getMappingFunction(targetExceptionClass.getConstructor(argClasses.toArray(new Class[0])), mapFunctions);
            } catch (NoSuchMethodException e) {
                throw new ExceptionMappingError("Cannot find constructor on " + sourceExceptionClassName + " with " + argClasses);
            }
        }

        /**
         * Method will create lambda to fully conversion of source exception
         * @param constructor constructor to use (right count and type of arguments)
         * @param mapFunctions list of lambdas to get results from source exception
         * @return lambda function to convert exception
         */
        private Function<Throwable, T> getMappingFunction(Constructor<T> constructor, List<Function<Throwable, Object>> mapFunctions) {
            return x -> {
                try {
                    return constructor.newInstance(
                        mapFunctions.stream().map(y -> y.apply(x)).toArray()
                    );
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new ExceptionMappingError("Cannot construct exception " + constructor.getDeclaringClass(), e);
                }
            };
        }

        @Override
        public boolean isInitialized() {
            return mappingFunction != null;
        }

        /**
         * Check if this mapping is right for type of t
         * @param t source exception
         * @return true if mapping is for type of t, otherwise false
         */
        private boolean isMatching(Throwable t) {
            return StringUtils.equals(t.getClass().getName(), sourceExceptionClassName);
        }

        @Override
        public void apply(Throwable t) throws T {
            if (!isMatching(t)) return;

           throw mappingFunction.apply(t);
        }

        @Override
        public String toString() {
            return "{ExceptionMapping [sourceExceptionClassName = " + sourceExceptionClassName + "]}";
        }

    }

}
