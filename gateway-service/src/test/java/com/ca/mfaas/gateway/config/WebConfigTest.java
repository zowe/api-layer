/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.config;


import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;

public class WebConfigTest {

    @Test
    public void shouldHandleEmptyEndointsWithDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wrongendpoint");
        assertEquals("MockServletContext", request.getServletContext().getServletContextName());
    }

}
