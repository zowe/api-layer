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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.*;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class ZoweJwtSchemeTest {

    public static final String EXPECTED_TOKEN_RESULT = "apimlAuthenticationToken=jwtToken";
    RequestContext requestContext;
    HttpServletRequest request;
    AuthSourceService authSourceService;
    AuthConfigurationProperties configurationProperties;
    ZoweJwtScheme scheme;
    static MessageService messageService;

    @BeforeAll
    static void setForAll() {
        messageService = new YamlMessageService();
        messageService.loadMessages("/gateway-messages.yml");
    }

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
        Optional<AuthSource> authSource = Optional.of(new JwtAuthSource("jwtToken"));

        @BeforeEach
        void setup() {

            when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);
            when(authSourceService.getJWT(authSource.get())).thenReturn("jwtToken");
            scheme = new ZoweJwtScheme(authSourceService, configurationProperties, messageService);
            assertFalse(scheme.isDefault());
            assertEquals(AuthenticationScheme.ZOWE_JWT, scheme.getScheme());

        }

        @Test
        void whenValidJWTAuthSource_thenUpdateZuulHeaderWithJWToken() {
            command = scheme.createCommand(null, authSource.get());
            command.apply(null);
            verify(requestContext, times(1)).addZuulRequestHeader(any(), any());
        }

        @Test
        void whenInvalidJwt_thenCreateErrorMessage() {
            AuthSource jwtSource = new JwtAuthSource("invalidToken");
            when(authSourceService.parse(jwtSource)).thenThrow(new TokenNotValidException(""));
            command = scheme.createCommand(null, jwtSource);
            assertTrue(command instanceof ZoweJwtScheme.ZoweJwtAuthCommand);
            assertEquals("ZWEAG102E Token is not valid", ((ZoweJwtScheme.ZoweJwtAuthCommand) command).getErrorHeader());
        }

        @Test
        void whenValidJWTAuthSource_thenUpdateCookieWithJWToken() {
            HttpRequest httpRequest = new HttpGet("api/v1/files");
            httpRequest.setHeader(new BasicHeader("authorization", "basic=aha"));
            command = scheme.createCommand(null, authSource.get());
            command.applyToRequest(httpRequest);
            assertEquals(EXPECTED_TOKEN_RESULT, httpRequest.getFirstHeader("cookie").getValue());
        }

    }

    @Nested
    class GivenX509AuthSourceTest {

        private AuthenticationCommand command;
        Optional<AuthSource> authSource;
        ZoweJwtScheme scheme;

        @BeforeEach
        void setup() {
            X509Certificate cert = mock(X509Certificate.class);
            authSource = Optional.of(new X509AuthSource(cert));
            when(authSourceService.getAuthSourceFromRequest()).thenReturn(authSource);
            when(authSourceService.getJWT(authSource.get())).thenReturn("jwtToken");

            scheme = new ZoweJwtScheme(authSourceService, configurationProperties, messageService);

        }

        @Test
        void whenValid_thenUpdateZuulHeaderWithJWToken() {
            command = scheme.createCommand(null, authSource.get());
            command.apply(null);
            verify(requestContext, times(1)).addZuulRequestHeader(any(), any());
            assertEquals(EXPECTED_TOKEN_RESULT, requestContext.getZuulRequestHeaders().get("cookie"));
        }

        @Test
        void whenValid_thenUpdateCookiesWithJWToken() {
            command = scheme.createCommand(null, authSource.get());
            HttpRequest httpRequest = new HttpGet("api/v1/files");
            httpRequest.setHeader(new BasicHeader("authorization", "basic=aha"));
            command.applyToRequest(httpRequest);
            assertEquals(EXPECTED_TOKEN_RESULT, httpRequest.getFirstHeader("cookie").getValue());
        }

        @Test
        void whenJwtCannotBeCreatedFromX509_thenCreateErrorMessage() {
            X509Certificate cert = mock(X509Certificate.class);
            AuthSource certSource = new X509AuthSource(cert);
            when(authSourceService.parse(certSource)).thenReturn(new X509AuthSource.Parsed("user", null, null, null, null, null));
            when(authSourceService.getJWT(certSource)).thenThrow(new UserNotMappedException("org.zowe.apiml.gateway.security.schema.x509.mappingFailed"));
            command = scheme.createCommand(null, certSource);
            assertEquals("ZWEAG161E No user was found", ((ZoweJwtScheme.ZoweJwtAuthCommand) command).getErrorHeader());
        }

        @Test
        void whenNoJWTReturned_thenUpdateZuulHeaderWithJWToken() {
            String errorHeaderValue = "ZWEAG160E No authentication provided in the request";
            when(authSourceService.getJWT(authSource.get())).thenReturn(null);
            AuthenticationCommand authenticationCommand = scheme.createCommand(null, null);

            assertTrue(authenticationCommand instanceof ZoweJwtScheme.ZoweJwtAuthCommand);
            assertNotNull(((ZoweJwtScheme.ZoweJwtAuthCommand) authenticationCommand).getErrorHeader());
            authenticationCommand.apply(null);
            assertEquals(errorHeaderValue, requestContext.getZuulRequestHeaders().get("x-zowe-auth-failure"));
        }

        @Test
        void whenNoJWTReturned_thenUpdateHeaderWithJWToken() {
            when(authSourceService.getJWT(authSource.get())).thenReturn(null);
            AuthenticationCommand authenticationCommand = scheme.createCommand(null, null);
            HttpRequest request = mock(HttpRequest.class);
            assertTrue(authenticationCommand instanceof ZoweJwtScheme.ZoweJwtAuthCommand);
            assertNotNull(((ZoweJwtScheme.ZoweJwtAuthCommand) authenticationCommand).getErrorHeader());
            authenticationCommand.applyToRequest(request);
            verify(request, times(1)).addHeader(any(), any());
        }
    }


}
