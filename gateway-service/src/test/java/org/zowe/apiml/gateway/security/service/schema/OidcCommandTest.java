/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.constants.ApimlConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class OidcCommandTest {

    private static final String tokenValue = "tokenValue";

    @Test
    void givenTokenExists_thenSetAsHeaderAndRemoveAuthorization() {
        RequestContext mockContext = new RequestContext();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        RequestContext.testSetCurrentContext(mockContext);
        mockContext.setRequest(mockRequest);
        mockContext.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, "value");
        mockRequest.addHeader(HttpHeaders.COOKIE, "cookieName=cookieValue; apimlAuthenticationToken=xyz");
        new OidcCommand(tokenValue).apply(mock(InstanceInfo.class));

        assertEquals("cookieName=cookieValue", mockContext.getZuulRequestHeaders().get(HttpHeaders.COOKIE.toLowerCase()));
        assertNull(mockContext.getZuulRequestHeaders().get(HttpHeaders.AUTHORIZATION.toLowerCase()));
        assertEquals(tokenValue, mockContext.getZuulRequestHeaders().get(ApimlConstants.HEADER_OIDC_TOKEN.toLowerCase()));
    }

}
