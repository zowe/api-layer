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
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
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
    void setup() {
        requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        request = new MockHttpServletRequest();
        requestContext.setRequest(request);

        authSourceService = mock(AuthSourceService.class);
        configurationProperties = mock(AuthConfigurationProperties.class);
        when(configurationProperties.getCookieProperties()).thenReturn(new AuthConfigurationProperties.CookieProperties());
    }

    @Nested
    class GivenJWTAuthSourceTest {
        AuthenticationCommand command;

        @BeforeEach
        void setup() {
            Optional<AuthSource> authSource = Optional.of(new JwtAuthSource("jwtToken"));
            when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);
            when(authSourceService.getJWT(authSource.get())).thenReturn("jwtToken");
            ZoweJwtScheme scheme = new ZoweJwtScheme(authSourceService, configurationProperties);
            assertFalse(scheme.isDefault());
            assertEquals(AuthenticationScheme.ZOWE_JWT, scheme.getScheme());
            command = scheme.createCommand(null, null);
        }

        @Test
        void whenValidJWTAuthSource_thenUpdateZuulHeaderWithJWToken() {
            command.apply(null);
            verify(requestContext, times(1)).addZuulRequestHeader(any(), any());
        }

        @Test
        void whenValidJWTAuthSource_thenUpdateCookieWithJWToken() {
            HttpRequest httpRequest = new HttpGet("api/v1/files");
            httpRequest.setHeader(new BasicHeader("authorization", "basic=aha"));
            command.applyToRequest(httpRequest);
            assertEquals("apimlAuthenticationToken=jwtToken", httpRequest.getFirstHeader("cookie").getValue());
        }
    }

    @Nested
    class GivenX509AuthSourceTest {

        private AuthenticationCommand command;

        @BeforeEach
        void setup() {
            X509Certificate cert = mock(X509Certificate.class);
            Optional<AuthSource> authSource = Optional.of(new X509AuthSource(cert));
            when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);
            when(authSourceService.getJWT(authSource.get())).thenReturn("jwtToken");

            ZoweJwtScheme scheme = new ZoweJwtScheme(authSourceService, configurationProperties);
            command = scheme.createCommand(null, null);
        }

        @Test
        void whenValid_thenUpdateZuulHeaderWithJWToken() {

            command.apply(null);
            verify(requestContext, times(1)).addZuulRequestHeader(any(), any());
            assertEquals("apimlAuthenticationToken=jwtToken", requestContext.getZuulRequestHeaders().get("cookie"));
        }

        @Test
        void whenValid_thenUpdateCookiesWithJWToken() {
            HttpRequest httpRequest = new HttpGet("api/v1/files");
            httpRequest.setHeader(new BasicHeader("authorization", "basic=aha"));
            command.applyToRequest(httpRequest);
            assertEquals("apimlAuthenticationToken=jwtToken", httpRequest.getFirstHeader("cookie").getValue());
        }

        @Test
        void whenNoJWTReturned_thenUpdateZuulHeaderWithJWToken() {
            X509Certificate cert = mock(X509Certificate.class);
            Optional<AuthSource> authSource = Optional.of(new X509AuthSource(cert));
            when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);
            when(authSourceService.getJWT(authSource.get())).thenReturn(null);

            ZoweJwtScheme scheme = new ZoweJwtScheme(authSourceService, configurationProperties);
            AuthenticationCommand command = scheme.createCommand(null, null);
            assertThrows(AccessDeniedException.class, () -> command.apply(null));
        }

    }


}
