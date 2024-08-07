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
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.security.service.schema.source.*;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.schema.JwtCommand.COOKIE_HEADER;


class ZoweJwtSchemeTest {

    public static final String EXPECTED_TOKEN_RESULT = "apimlAuthenticationToken=jwtToken";
    private final AuthSource authSource = new JwtAuthSource("jwtToken");
    RequestContext requestContext;
    HttpServletRequest request;
    AuthSourceService authSourceService;
    AuthConfigurationProperties configurationProperties;
    ZoweJwtScheme scheme;
    AuthenticationCommand command;

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

        when(authSourceService.getJWT(authSource)).thenReturn("jwtToken");
        scheme = new ZoweJwtScheme(authSourceService, configurationProperties);
    }

    @Nested
    class AuthSourceIndependentTests {
        @Test
        void testGetAuthSource() {
            doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest(any());

            scheme.getAuthSource();
            verify(authSourceService, times(1)).getAuthSourceFromRequest(any());
        }

        @Test
        void givenNullParsingResult_thenThrows() {
            AuthSource authSource = new JwtAuthSource("jwt");
            doReturn(null).when(authSourceService).parse(any(AuthSource.class));
            assertThrows(IllegalStateException.class, () -> scheme.createCommand(null, authSource));
        }

        @Test
        void whenCannotGetExpiration_thenUseDefaultExpiration() {
            AuthSource.Parsed parsedSource = new X509AuthSource.Parsed("commonName", new Date(), null, Origin.X509, "", "distName");
            doReturn(parsedSource).when(authSourceService).parse(any(AuthSource.class));
            command = scheme.createCommand(null, new JwtAuthSource("jwtToken"));

            assertNotNull(command);
            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");
            assertNotNull(expiration);
        }
    }

    @Nested
    class GivenJWTAuthSourceTest {
        AuthenticationCommand command;

        @Test
        void whenValidJWTAuthSource_thenUpdateZuulHeaderWithJWToken() {
            when(authSourceService.parse(authSource)).thenReturn(new ParsedTokenAuthSource("user", new Date(), new Date(), Origin.ZOSMF));
            command = scheme.createCommand(null, authSource);
            command.apply(null);
            verify(requestContext).addZuulRequestHeader(eq("cookie"), any());
            verify(requestContext).addZuulRequestHeader(eq(HttpHeaders.AUTHORIZATION), any());
        }

        @Test
        void whenInvalidJwt_thenThrows() {
            AuthSource jwtSource = new JwtAuthSource("invalidToken");
            when(authSourceService.parse(jwtSource)).thenThrow(new TokenNotValidException(""));
            assertThrows(AuthSchemeException.class, () -> scheme.createCommand(null, jwtSource));
        }

        @Test
        void whenExpiredJwt_thenThrows() {
            ((MockHttpServletRequest) request).addHeader(COOKIE_HEADER, "apimlAuthenticationToken=expiredToken");
            AuthSource jwtSource = new JwtAuthSource("expiredToken");
            when(authSourceService.parse(jwtSource)).thenThrow(new TokenExpireException("expired token"));
            assertThrows(AuthSchemeException.class, () -> scheme.createCommand(null, jwtSource));
        }

        @Test
        void whenValidJWTAuthSource_thenCommandIsNotExpired() {
            long expectedExpiration = System.currentTimeMillis() + (5 * 60 * 1000);
            when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(new ParsedTokenAuthSource("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

            command = scheme.createCommand(null, authSource);
            Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");

            assertFalse(command.isExpired());
            assertNotNull(expiration);
            assertEquals(expectedExpiration, expiration);
        }

        @Test
        void whenExpiredJWTAuthSource_thenCommandIsExpired() {
            long expectedExpiration = System.currentTimeMillis() - (5 * 60 * 1000);
            when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(new ParsedTokenAuthSource("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

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
            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(authSource));
            when(authSourceService.getJWT(authSource)).thenReturn("jwtToken");

            scheme = new ZoweJwtScheme(authSourceService, configurationProperties);
        }

        @Nested
        class WhenCertificateInRequest {
            @Test
            void whenValid_thenUpdateZuulHeaderWithJWToken() {
                when(authSourceService.parse(authSource)).thenReturn(new X509AuthSource.Parsed("user", new Date(), new Date(), Origin.ZOSMF, "public key", "distinguishedName"));
                command = scheme.createCommand(null, authSource);
                command.apply(null);
                verify(requestContext).addZuulRequestHeader(eq("cookie"), any());
                verify(requestContext).addZuulRequestHeader(eq(HttpHeaders.AUTHORIZATION), any());
                assertEquals(EXPECTED_TOKEN_RESULT, requestContext.getZuulRequestHeaders().get("cookie"));
            }

            @Test
            void whenJwtCannotBeCreatedFromX509_thenThrows() {
                X509Certificate cert = mock(X509Certificate.class);
                AuthSource certSource = new X509AuthSource(cert);
                when(authSourceService.parse(certSource)).thenReturn(new X509AuthSource.Parsed("user", null, null, null, null, null));
                when(authSourceService.getJWT(certSource)).thenThrow(new AuthSchemeException("org.zowe.apiml.gateway.security.schema.x509.mappingFailed"));
                assertThrows(AuthSchemeException.class, () -> scheme.createCommand(null, certSource));
            }

            @Test
            void whenNoJWTReturned_thenThrows() {
                when(authSourceService.getJWT(authSource)).thenReturn(null);
                assertThrows(AuthSchemeException.class, () -> scheme.createCommand(null, null));
            }

            @Test
            void whenValidX509AuthSource_thenCommandIsNotExpired() {
                long expectedExpiration = System.currentTimeMillis() + (5L * 60 * 1000);
                when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(new ParsedTokenAuthSource("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

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
            void givenNoClientCertificate_thenCommandCreationFails() {
                assertThrows(AuthSchemeException.class, () -> scheme.createCommand(null, null));
            }
        }

        @Nested
        class IncorrectCertificateInRequest {
            @Test
            void whenExpiredX509AuthSource_thenCommandIsExpired() {
                long expectedExpiration = System.currentTimeMillis() - (5 * 60 * 1000);
                when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(new ParsedTokenAuthSource("userId", new Date(), new Date(expectedExpiration), Origin.ZOWE));

                command = scheme.createCommand(null, authSource);
                Long expiration = (Long) ReflectionTestUtils.getField(command, "expireAt");

                assertTrue(command.isExpired());
                assertNotNull(expiration);
                assertEquals(expectedExpiration, expiration);
            }
        }
    }

    @Nested
    class GivenCustomAuthHeader {
        @Test
        void thenAddRequestHeaderContainingToken() {
            ReflectionTestUtils.setField(scheme, "customHeader", "header");
            when(authSourceService.parse(authSource)).thenReturn(new ParsedTokenAuthSource("user", new Date(), new Date(), Origin.ZOSMF));
            command = scheme.createCommand(null, authSource);
            command.apply(null);
            verify(requestContext, times(1)).addZuulRequestHeader(eq("header"), any());
        }
    }
}
