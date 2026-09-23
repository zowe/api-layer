/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Pins the class loader the application context uses to resolve its own resources.
 *
 * <p>{@code ApplicationContext.getResource("classpath:...")} does not use the application's class
 * loader; {@code DefaultResourceLoader} falls back to the context class loader of whichever thread
 * happens to ask. Usually those are the same, but not always. Reactor's
 * {@code Schedulers.boundedElastic()} is a JVM-wide pool whose worker threads are created on first
 * use and inherit the context class loader of the thread that created them. Thymeleaf's reactive
 * template engine processes templates on that pool, so in a JVM that hosts more than one Spring
 * context - the unit-test suite, or any container that stops and restarts the web application - a
 * worker created while an earlier Tomcat web application was running keeps that web application's
 * (by then stopped) class loader, and template resolution fails with:
 *
 * <pre>
 * IllegalStateException: Illegal access: this web application instance has been stopped already.
 *     Could not load [templates/home.html].
 * </pre>
 *
 * <p>Setting the class loader explicitly removes the dependency on the calling thread, so template
 * and message resolution keep working no matter which thread performs them. The class loader used is
 * the one that loaded this class, which is the application's own class loader - the same one the
 * context class loader is set to under normal circumstances.
 */
public class ApplicationClassLoaderInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ClassLoader applicationClassLoader = ApplicationClassLoaderInitializer.class.getClassLoader();
        if (applicationClassLoader != null) {
            applicationContext.setClassLoader(applicationClassLoader);
        }
    }

}
