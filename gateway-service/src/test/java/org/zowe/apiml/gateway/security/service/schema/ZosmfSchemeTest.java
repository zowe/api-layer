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
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource.Parsed;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.gateway.utils.X509Utils;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.gateway.security.service.schema.ZosmfScheme.ZosmfCommand.COOKIE_HEADER;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZosmfSchemeTest extends CleanCurrentRequestContextTest {

    @Mock
    private AuthSourceService authSourceService;

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;

    private ZosmfScheme zosmfScheme;

    private Authentication authentication;
    private AuthSource.Parsed parsedSourceZowe;
    private AuthSource.Parsed parsedSourceZosmf;
    private AuthSource.Parsed parsedSourceX509;
    private RequestContext requestContext;
    private HttpServletRequest request;

    @BeforeEach
    void prepareContextForTests() {
        Calendar calendar = Calendar.getInstance();
        authentication = new Authentication(AuthenticationScheme.ZOSMF, null);
        parsedSourceZowe = new JwtAuthSource.Parsed("username", calendar.getTime(), calendar.getTime(), Origin.ZOWE);
        parsedSourceZosmf = new JwtAuthSource.Parsed("username", calendar.getTime(), calendar.getTime(), Origin.ZOSMF);
        parsedSourceX509 = new Parsed("username", calendar.getTime(), calendar.getTime(), Origin.X509, "encoded", "distName");
        requestContext = spy(new RequestContext());
        RequestContext.testSetCurrentContext(requestContext);

        request = new MockHttpServletRequest();
        requestContext.setRequest(request);
        zosmfScheme = new ZosmfScheme(authSourceService, authConfigurationProperties);
        ReflectionTestUtils.setField(zosmfScheme, "authProvider", "zosmf");
    }

    static Stream<Arguments> authSources() {
        return Stream.of(Arguments.of(new JwtAuthSource("jwtToken2")), Arguments.of(new X509AuthSource(mock(
            X509Certificate.class))));
    }

    @Test
    void returnCorrectScheme() {
        assertEquals(AuthenticationScheme.ZOSMF, zosmfScheme.getScheme());
    }

    @Test
    void givenNoAuthSource_thenValidAuthSourceIsRequired() {
        AuthenticationCommand command = zosmfScheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), null);

        assertTrue(command.isRequiredValidSource());
    }

    @Test
    void givenAuthSource_whenZosmfIsNotSetAsAuthProvider_thenThrowException() {
        ZosmfScheme zosmfScheme = new ZosmfScheme(authSourceService, null);
        JwtAuthSource authSource = new JwtAuthSource("jwt");
        assertThrows(AuthenticationSchemeNotSupportedException.class, () -> zosmfScheme.createCommand(null, authSource));
    }



    @Nested
    class ZuulRequestTest {

        @Test
        void givenNoAuthSource_thenDontAddZuulHeader() {
            when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.empty());

            zosmfScheme.createCommand(authentication, null).apply(null);

            verify(requestContext, never()).addZuulRequestHeader(anyString(), anyString());

        }

        @Nested
        class GivenZoweJwtAuthSourceTest {

            @BeforeEach
            void setup() {
                when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtToken1")));
                when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken1"))).thenReturn("ltpa1");
                when(authSourceService.parse(new JwtAuthSource("jwtToken1"))).thenReturn(parsedSourceZowe);
            }

            @Test
            void givenZoweJwtAuthSource_thenAddOnlyLtpaCookie() {
                requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, null);
                zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken1")).apply(null);
                assertEquals("LtpaToken2=ltpa1", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));
            }

            @Test
            void givenZoweJwtAuthSource_andExistingCookie_thenAppendCookieWithLtpa() {
                requestContext.getZuulRequestHeaders().put(COOKIE_HEADER, "cookie1=1");
                zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken1")).apply(null);
                assertEquals("cookie1=1;LtpaToken2=ltpa1", requestContext.getZuulRequestHeaders().get(COOKIE_HEADER));
            }

            @Test
            void givenInvalidZoweJwtAuthSource_thenThrowTokenNotValidException() {
                when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken1"))).thenThrow(new TokenNotValidException("Token is not valid"));

                AuthenticationCommand command = zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken1"));
                Exception exception = assertThrows(TokenNotValidException.class, () -> command.apply(null), " Token is not valid");
                assertEquals("Token is not valid", exception.getMessage());

            }

            @Test
            void givenExpiredZoweJwtAuthSource_thenThrowJwtTokenException() {
                when(authSourceService.getLtpaToken(new JwtAuthSource("jwtToken1"))).thenThrow(new JwtException("Token is expired"));

                AuthenticationCommand command = zosmfScheme.createCommand(authentication, new JwtAuthSource("jwtToken1"));
                Exception exception = assertThrows(JwtException.class, () -> command.apply(null), "Token is expired");
                assertEquals("Token is expired", exception.getMessage());

            }
        }

        @Nested
        class GivenZosmfAuthSourceTest {
            @Test
            void thenOnlyJwtTokenIsForwardedInCookie() {
                AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
                when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
                when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
                when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtTokenZosmf")));
                when(authSourceService.parse(new JwtAuthSource("jwtTokenZosmf"))).thenReturn(parsedSourceZosmf);

                AuthenticationCommand command = zosmfScheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), new JwtAuthSource("jwtTokenZosmf"));

                command.apply(null);

                verify(authSourceService, times(1)).getAuthSourceFromRequest();
                verify(authSourceService, times(2)).parse(new JwtAuthSource("jwtTokenZosmf"));
                verify(authSourceService, never()).getLtpaToken(new JwtAuthSource("jwtTokenZosmf"));
            }
        }

        @Nested
        class GivenX509AuthSourceTest {
            @Test
            void givenClientCertificate_thenAddZuulHeader() {
                when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new X509AuthSource(mock(
                    X509Certificate.class))));
                when(authSourceService.parse(any(X509AuthSource.class))).thenReturn(parsedSourceX509);
                when(authSourceService.getJWT(any())).thenReturn("jwt");
                when(authSourceService.parse(any(JwtAuthSource.class))).thenReturn(parsedSourceZosmf);
                AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
                when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
                when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
                X509Certificate certificate = X509Utils.getCertificate("zowe");
                X509AuthSource authSource = new X509AuthSource(certificate);
                zosmfScheme.createCommand(authentication, authSource).apply(null);

                verify(requestContext, times(1)).addZuulRequestHeader(anyString(), anyString());
            }
        }

    }

    @Nested
    class ExpirationTest {

        @Test
        void givenNoAuthSource_thenCommandIsNotExpired() {
            AuthenticationCommand command = zosmfScheme.createCommand(null, null);

            assertNull(ReflectionTestUtils.getField(command, "expireAt"));
            assertFalse(command.isExpired());
        }

        @Test
        void givenAuthSourceWithoutExpiration_thenCommandIsNotExpired() {
            when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(new JwtAuthSource.Parsed("user", null, null, Origin.ZOWE));

            AuthenticationCommand command = zosmfScheme.createCommand(null, new JwtAuthSource("jwtToken"));

            assertNull(ReflectionTestUtils.getField(command, "expireAt"));
            assertFalse(command.isExpired());
        }

        @Test
        void givenAuthWithExpirationSetToNow_thenCommandIsExpired() {
            when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(new JwtAuthSource.Parsed("user", new Date(123), new Date(123), Origin.ZOWE));

            AuthenticationCommand command = zosmfScheme.createCommand(null, new JwtAuthSource("jwtToken"));

            assertNotNull(ReflectionTestUtils.getField(command, "expireAt"));
            assertEquals(123L, ReflectionTestUtils.getField(command, "expireAt"));
            assertTrue(command.isExpired());
        }

        private AuthSource.Parsed prepareParsedAuthSourceForTime(int amountOfSeconds) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, amountOfSeconds);
            return new JwtAuthSource.Parsed("user", new Date(), c.getTime(), Origin.ZOWE);
        }

        @Test
        void givenTokenExpiredOneSecAgo_thenCommandIsExpired() {
            when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(prepareParsedAuthSourceForTime(-1));

            AuthenticationCommand command = zosmfScheme.createCommand(null, new JwtAuthSource("jwtToken"));

            assertTrue(command.isExpired());
        }

        @Test
        void givenTokenThatWillExpireSoon_thenCommandIsNotExpired() {
            when(authSourceService.parse(new JwtAuthSource("jwtToken"))).thenReturn(prepareParsedAuthSourceForTime(2));

            AuthenticationCommand command = zosmfScheme.createCommand(null, new JwtAuthSource("jwtToken"));

            assertFalse(command.isExpired());
        }

    @Test
    void givenZosmfToken_whenCreateCommand_thenTestJwtToken() {
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(cookieProperties.getCookieName()).thenReturn("apimlAuthenticationToken");
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);

        ZosmfScheme scheme = new ZosmfScheme(authSourceService, authConfigurationProperties);
        when(authSourceService.getAuthSourceFromRequest()).thenReturn(Optional.of(new JwtAuthSource("jwtTokenZosmf")));
        when(authSourceService.parse(new JwtAuthSource("jwtTokenZosmf"))).thenReturn(parsedSourceZosmf);

        AuthenticationCommand command = scheme.createCommand(new Authentication(AuthenticationScheme.ZOSMF, null), new JwtAuthSource("jwtTokenZosmf"));

        command.apply(null);

        verify(authSourceService, times(1)).getAuthSourceFromRequest();
        verify(authSourceService, times(2)).parse(new JwtAuthSource("jwtTokenZosmf"));
        verify(authSourceService, never()).getLtpaToken(new JwtAuthSource("jwtTokenZosmf"));
    }

    }

}
