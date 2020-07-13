package org.zowe.apiml.gateway.security.service.schema;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.netflix.zuul.context.RequestContext;
import org.apache.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ByPassSchemeTest {

    @Test
    void testScheme() {
        ByPassScheme scheme = new ByPassScheme();
        assertTrue(scheme.isDefault());
        assertEquals(AuthenticationScheme.BYPASS, scheme.getScheme());

        AuthenticationCommand cmd = scheme.createCommand(null, null);
        assertNull(RequestContext.getCurrentContext().get("AuthenticationSchemeByPass"));
        cmd.apply(null);
        assertEquals(Boolean.TRUE, RequestContext.getCurrentContext().get("AuthenticationSchemeByPass"));
        assertFalse(cmd.isExpired());
        assertFalse(cmd.isRequiredValidJwt());
    }

    @Test
    void testApplyToRequest() {
        HttpRequest request = mock(HttpRequest.class);
        ByPassScheme scheme = new ByPassScheme();
        AuthenticationCommand cmd = scheme.createCommand(null, null);
        cmd.applyToRequest(request);
        verify(request,never()).setHeaders(any());
    }

}
