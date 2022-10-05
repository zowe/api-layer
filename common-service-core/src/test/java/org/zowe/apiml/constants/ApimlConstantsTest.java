/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ApimlConstantsTest {

    @Test
    void whenCreatingObject_thenThrowException() throws NoSuchMethodException {
        Constructor<ApimlConstants> cnstrctor = ApimlConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(cnstrctor.getModifiers()));
        cnstrctor.setAccessible(true);
        Exception exception = assertThrows(InvocationTargetException.class, cnstrctor::newInstance);
        assertEquals(IllegalStateException.class, exception.getCause().getClass());
    }
}
