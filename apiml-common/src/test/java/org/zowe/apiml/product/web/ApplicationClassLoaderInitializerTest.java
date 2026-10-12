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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The initializer exists so that classpath resource resolution does not depend on the context class
 * loader of the thread performing it; see the class comment for the failure it prevents. The
 * contract the rest of the application relies on is that the context ends up with the application's
 * own class loader, so that is what is asserted here.
 */
class ApplicationClassLoaderInitializerTest {

    @Test
    void givenAnApplicationContext_whenInitialized_thenTheApplicationClassLoaderIsPinned() {
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        new ApplicationClassLoaderInitializer().initialize(applicationContext);

        ArgumentCaptor<ClassLoader> classLoader = ArgumentCaptor.forClass(ClassLoader.class);
        verify(applicationContext).setClassLoader(classLoader.capture());
        assertNotNull(classLoader.getValue());
        assertSame(ApplicationClassLoaderInitializer.class.getClassLoader(), classLoader.getValue());
    }

}
