/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.security.login;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AuthMethodNotSupportedExceptionTest {

    @Test
    public void constructorTest() {
        String message = "wrong HTTP method";

        AuthMethodNotSupportedException exception = new AuthMethodNotSupportedException(message);

        assertThat(exception.getMessage(), is(message));
    }
}
