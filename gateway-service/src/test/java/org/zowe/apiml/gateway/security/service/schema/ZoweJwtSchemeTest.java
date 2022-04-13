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
import java.util.Date;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.*;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource.Parsed;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.schema.JwtCommand.COOKIE_HEADER;
import static org.zowe.apiml.gateway.security.service.schema.X509Scheme.AUTH_FAIL_HEADER;


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
        when(configurationProperties.getTokenProperties()).thenReturn(new AuthConfigurationProperties.TokenProperties());
    }

    @Nested
    class GivenJWTAuthSourceTest {
        AuthenticationCommand command;
        AuthSource authSource = new JwtAuthSource("jwtToken");

        @BeforeEach
        void setup() {

            when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(authSource));
            when(authSourceService.getJWT(authSource)).thenReturn("jwtToken");
            scheme = new ZoweJwtScheme(authSourceService, configurationProperties, messageService);
            assertFalse(scheme.isDefault());
            assertEquals(AuthenticationScheme.ZOWE_JWT, scheme.getScheme());

        }

        @Test
        void whenValidJWTAuthSource_thenUpdateZuulHeaderWithJWToken() {
            command = scheme.createCommand(null, authSource);
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
        void whenExpiredJwt_thenCreateErrorMessage() {
            ((MockHttpServletRequest)request).addHeader(COOKIE_HEADER, "apimlAuthenticationToken=expiredToken");
            AuthSource jwtSource = new JwtAuthSource("expiredToken");
            when(authSourceService.parse(jwtSource)).thenThrow(new TokenExpireException("expired token"));
            command = scheme.createCommand(null, jwtSource);
            assertEquals("ZWEAG103E The token has expired", ((ZoweJwtScheme.ZoweJwtAuthCommand) command).getErrorHeader());
            command.apply(null);
            assertEquals("", requestContext.getZuulRequestHeaders().get("cookie"));
        }

        @Test
        void whenValidJWTAuthSource_thenUpdateCookieWithJWToken() {
            HttpRequest httpRequest = new HttpGet("api/v1/files");
            httpRequest.setHeader(new BasicHeader("authorization", "basic=aha"));
            command = scheme.createCommand(null, authSource);
            command.applyToRequest(httpRequest);
            assertEquals(EXPECTED_TOKEN_RESULT, httpRequest.getFirstHeader("cookie").getValue());
        }

        @Test
        void whenValidJWTAuthSource_thenCommandIsNotExpired() {
            long expectedExpiration = System.currentTimeMillis() + (5 * 60 * 1000);
            when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(new Parsed("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

            command = scheme.createCommand(null, authSource);
            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");

            assertFalse(command.isExpired());
            assertNotNull(expiration);
            assertEquals(expectedExpiration, expiration);
        }

        @Test
        void whenExpiredJWTAuthSource_thenCommandIsExpired() {
            long expectedExpiration = System.currentTimeMillis() - (5 * 60 * 1000);
            when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(new Parsed("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

            command = scheme.createCommand(null, authSource);
            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");

            assertTrue(command.isExpired());
            assertNotNull(expiration);
            assertEquals(expectedExpiration, expiration);
        }
    }

    @Nested
    class GivenX509AuthSourceTest {

        private AuthenticationCommand command;
        AuthSource authSource;
        ZoweJwtScheme scheme;

        @BeforeEach
        void setup() {
            X509Certificate cert = mock(X509Certificate.class);
            authSource = new X509AuthSource(cert);
            when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(authSource));
            when(authSourceService.getJWT(authSource)).thenReturn("jwtToken");

            scheme = new ZoweJwtScheme(authSourceService, configurationProperties, messageService);

        }

        @Nested
        class WhenCertificateInRequest {
            @Test
            void whenValid_thenUpdateZuulHeaderWithJWToken() {
                command = scheme.createCommand(null, authSource);
                command.apply(null);
                verify(requestContext, times(1)).addZuulRequestHeader(any(), any());
                assertEquals(EXPECTED_TOKEN_RESULT, requestContext.getZuulRequestHeaders().get("cookie"));
            }

            @Test
            void whenValid_thenUpdateCookiesWithJWToken() {
                command = scheme.createCommand(null, authSource);
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
                when(authSourceService.getJWT(authSource)).thenReturn(null);
                AuthenticationCommand authenticationCommand = scheme.createCommand(null, null);

                assertTrue(authenticationCommand instanceof ZoweJwtScheme.ZoweJwtAuthCommand);
                assertNotNull(((ZoweJwtScheme.ZoweJwtAuthCommand) authenticationCommand).getErrorHeader());
                authenticationCommand.apply(null);
                assertEquals(errorHeaderValue, requestContext.getZuulRequestHeaders().get("x-zowe-auth-failure"));
            }

            @Test
            void whenNoJWTReturned_thenUpdateHeaderWithJWToken() {
                when(authSourceService.getJWT(authSource)).thenReturn(null);
                AuthenticationCommand authenticationCommand = scheme.createCommand(null, null);
                HttpRequest request = mock(HttpRequest.class);
                assertTrue(authenticationCommand instanceof ZoweJwtScheme.ZoweJwtAuthCommand);
                assertNotNull(((ZoweJwtScheme.ZoweJwtAuthCommand) authenticationCommand).getErrorHeader());
                authenticationCommand.applyToRequest(request);
                verify(request, times(1)).addHeader(any(), any());
            }

            @Test
            void whenValidX509AuthSource_thenCommandIsNotExpired() {
                long expectedExpiration = System.currentTimeMillis() + (5L * 60 * 1000);
                when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(new Parsed("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

                command = scheme.createCommand(null, authSource);
                Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");

                assertFalse(command.isExpired());
                assertNotNull(expiration);
                assertEquals(expectedExpiration, expiration);
            }
        }

        @Nested
        class NoCertificateInRequest {
            @Test
            void givenNoClientCertificate_thenCommandDoNotExpire() {
                String errorHeaderValue = "ZWEAG160E No authentication provided in the request";
                doReturn(errorHeaderValue).when(requestContext).get(AUTH_FAIL_HEADER);

                ZoweJwtScheme.ZoweJwtAuthCommand command = (ZoweJwtScheme.ZoweJwtAuthCommand) scheme.createCommand(null, null);

                assertNotNull(command);

                Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");
                assertNotNull(expiration);
                assertTrue(expiration <= System.currentTimeMillis());
            }
        }

        @Nested
        class IncorrectCertificateInRequest {
            @Test
            void givenNoClientCertificate_andX509SchemeRequired_thenNoHeaderIsSet() {
                String errorHeaderValue = "ZWEAG164E Error occurred while validating X509 certificate. Can't get extensions from certificate";
                doReturn(errorHeaderValue).when(requestContext).get(AUTH_FAIL_HEADER);

                ZoweJwtScheme.ZoweJwtAuthCommand command = (ZoweJwtScheme.ZoweJwtAuthCommand) scheme.createCommand(null, null);

                assertNotNull(command);

                command.apply(null);
                assertEquals(errorHeaderValue, requestContext.getZuulRequestHeaders().get("x-zowe-auth-failure"));
            }

            @Test
            void whenExpiredX509AuthSource_thenCommandIsExpired() {
                long expectedExpiration = System.currentTimeMillis() - (5 * 60 * 1000);
                when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(new Parsed("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

                command = scheme.createCommand(null, authSource);
                Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");

                assertTrue(command.isExpired());
                assertNotNull(expiration);
                assertEquals(expectedExpiration, expiration);
            }
        }
    }


}
