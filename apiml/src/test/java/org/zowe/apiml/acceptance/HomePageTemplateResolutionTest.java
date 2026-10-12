/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The modulith home page is a Thymeleaf template rendered on Reactor's JVM-wide {@code boundedElastic}
 * pool, and template resolution used to go through the calling thread's context class loader. A worker
 * thread created while an earlier Tomcat web application was running keeps that application's (stopped)
 * class loader, which turned the home page into a 500 with
 * {@code Illegal access: this web application instance has been stopped already} - intermittently, and
 * only when another context happened to have primed the pool first.
 *
 * <p>These tests pin the failure down deterministically instead of relying on that ordering.
 */
@AcceptanceTest
class HomePageTemplateResolutionTest extends AcceptanceTestWithBasePath {

    @Autowired
    private ApplicationContext applicationContext;

    @Nested
    class GivenTheHomePageIsRequested {

        @Test
        void thenItIsRendered() {
            given()
            .when()
                .get(basePath)
            .then()
                .statusCode(200);
        }

    }

    @Nested
    class GivenAStoppedWebApplicationClassLoaderOnTheCallingThread {

        /**
         * Resolving a classpath resource must not depend on the context class loader of the thread doing
         * it, otherwise any thread that outlives a Tomcat web application breaks resource resolution for
         * the rest of the JVM's life.
         */
        @Test
        void whenTheHomeTemplateIsResolved_thenItIsStillFound() {
            var original = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(new StoppedWebApplicationClassLoader(original));
            try {
                assertTrue(
                    applicationContext.getResource("classpath:/templates/home.html").exists(),
                    "The home template has to resolve regardless of the calling thread's context class loader"
                );
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }

    }

    @Nested
    class GivenTheApplicationContext {

        @Test
        void thenItsClassLoaderIsPinnedToTheApplicationClassLoader() {
            assertEquals(
                HomePageTemplateResolutionTest.class.getClassLoader(),
                applicationContext.getClassLoader(),
                "The application context has to resolve its own resources with the application's class loader"
            );
        }

    }

    /**
     * Stands in for {@code org.apache.catalina.loader.WebappClassLoaderBase} of a stopped web
     * application, which rejects every resource lookup from another thread with an
     * {@link IllegalStateException}.
     */
    private static class StoppedWebApplicationClassLoader extends ClassLoader {

        StoppedWebApplicationClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public URL getResource(String name) {
            throw new IllegalStateException(
                "Illegal access: this web application instance has been stopped already. Could not load [" + name + "]."
            );
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            throw new IllegalStateException(
                "Illegal access: this web application instance has been stopped already. Could not load [" + name + "]."
            );
        }

        @Override
        public URL findResource(String name) {
            throw new IllegalStateException(
                "Illegal access: this web application instance has been stopped already. Could not load [" + name + "]."
            );
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

        @Override
        public String toString() {
            return "StoppedWebApplicationClassLoader";
        }

    }

}
