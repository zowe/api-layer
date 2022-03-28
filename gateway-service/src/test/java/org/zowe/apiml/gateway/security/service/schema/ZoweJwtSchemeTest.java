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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ZoweJwtSchemeTest {

    RequestContext requestContext;
    HttpServletRequest request;
    AuthSourceService authSourceService;
    AuthConfigurationProperties configurationProperties;

    @BeforeEach
    void setup(){
        requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        authSourceService = mock(AuthSourceService.class);
        configurationProperties = mock(AuthConfigurationProperties.class);
        when(configurationProperties.getCookieProperties()).thenReturn(new AuthConfigurationProperties.CookieProperties());
    }

    @Test
    void whenValidJWTAuthSource_thenUpdateZuulHeaderWithJWToken() {

        Optional<AuthSource> authSource = Optional.of(new JwtAuthSource("jwtToken"));
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);

        ZoweJwtScheme scheme = new ZoweJwtScheme(authSourceService, configurationProperties);
        assertFalse(scheme.isDefault());
        assertEquals(AuthenticationScheme.ZOWE_JWT, scheme.getScheme());
        AuthenticationCommand command = scheme.createCommand(null, null);
        command.apply(null);
        verify(requestContext,times(0)).addZuulRequestHeader(any(),any());
    }

    @Test
    void whenValidX509AuthSource_thenUpdateZuulHeaderWithJWToken() {

        X509Certificate cert = mock(X509Certificate.class);
        Optional<AuthSource> authSource = Optional.of(new X509AuthSource(cert));
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);
        when(authSourceService.getJWT(authSource.get())).thenReturn("jwtToken");
        ZoweJwtScheme scheme = new ZoweJwtScheme(authSourceService, configurationProperties);

        AuthenticationCommand command = scheme.createCommand(null, null);
        command.apply(null);
        verify(requestContext,times(1)).addZuulRequestHeader(any(),any());
        assertEquals("apimlAuthenticationToken=jwtToken",requestContext.getZuulRequestHeaders().get("cookie"));
    }

}
