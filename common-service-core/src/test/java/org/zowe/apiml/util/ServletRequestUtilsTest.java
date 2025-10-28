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

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServletRequestUtilsTest {

    @Test
    void whenClientCertHeaderNotDefined_thenReturnFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean result = ServletRequestUtils.isClientCertificateIgnored(request);

        assertFalse(result, "Expected false when Client-Cert header is not defined");
    }

    @Test
    void whenClientCertHeaderEmpty_thenReturnTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Client-Cert", "");

        boolean result = ServletRequestUtils.isClientCertificateIgnored(request);

        assertTrue(result, "Expected true when Client-Cert header is empty");
    }

    @Test
    void whenClientCertHeaderHasValue_thenReturnFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Client-Cert", "some-cert-data");

        boolean result = ServletRequestUtils.isClientCertificateIgnored(request);

        assertFalse(result, "Expected false when Client-Cert header has a value");
    }
}
