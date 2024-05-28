/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.security.service.saf.SafIdtException;
import org.zowe.apiml.zaas.security.service.saf.SafIdtProvider;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.ParsedTokenAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.auth.AuthenticationScheme.SAF_IDT;
import static org.zowe.apiml.constants.ApimlConstants.AUTH_FAIL_HEADER;

class SafIdtSchemeTest {
    private SafIdtScheme underTest;
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final JwtAuthSource authSource = new JwtAuthSource("token");

    private AuthSourceService authSourceService;
    private PassTicketService passTicketService;
    private SafIdtProvider safIdtProvider;

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        passTicketService = mock(PassTicketService.class);
        safIdtProvider = mock(SafIdtProvider.class);

        underTest = new SafIdtScheme(authConfigurationProperties, authSourceService, passTicketService, safIdtProvider);
        underTest.defaultIdtExpiration = 10;
    }

    @AfterEach
    void tearDown() {
        RequestContext.getCurrentContext().clear();
    }

    @Test
    void testGetAuthSource() {
        doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest(any());

        underTest.getAuthSource();
        verify(authSourceService, times(1)).getAuthSourceFromRequest(any());
    }

    @Nested
    class WhenTokenIsRequested {
        private static final String USERNAME = "USERNAME";
        private static final String APPLID = "ANYAPPL";
        private static final String PASSTICKET = "PASSTICKET";

        private final Authentication auth = new Authentication(SAF_IDT, APPLID);
        private final ParsedTokenAuthSource parsedAuthSource = new ParsedTokenAuthSource(
            USERNAME,
            null,
            null,
            null
        );

        @Nested
        class ThenValidSafTokenIsProduced {
            @BeforeEach
            void setUp() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
            }

            @Nested
            class WhenApply {
                private RequestContext requestContext;
                private HttpServletRequest request;

                @BeforeEach
                void setup() {
                    requestContext = spy(new RequestContext());
                    RequestContext.testSetCurrentContext(requestContext);

                    request = new MockHttpServletRequest();
                    requestContext.setRequest(request);
                }

                @Test
                void givenAuthenticatedJwtToken() {
                    String safIdt = Jwts.builder()
                        .setExpiration(new Date(System.currentTimeMillis() + 1000000))
                        .compact();

                    when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                    AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                    assertNotNull(ac);
                    assertFalse(ac.isExpired());
                    assertTrue(ac.isRequiredValidSource());

                    ac.apply(null);
                    assertThat(getValueOfZuulHeader(ApimlConstants.SAF_TOKEN_HEADER), is(safIdt));
                    assertNull(getValueOfZuulHeader(AUTH_FAIL_HEADER));
                }

                @Test
                void givenIdtWithoutExpiration() {
                    String safIdt = Jwts.builder()
                        .setSubject(USERNAME)
                        .compact();

                    when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);

                    AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                    assertNotNull(ac);
                    assertFalse(ac.isExpired());
                    assertTrue(ac.isRequiredValidSource());

                    ac.apply(null);
                    assertThat(getValueOfZuulHeader(ApimlConstants.SAF_TOKEN_HEADER), is(safIdt));
                    assertNull(getValueOfZuulHeader(AUTH_FAIL_HEADER));
                }
            }
        }

        @Nested
        class ThenNoTokenIsProduced {
            @Test
            void givenNullAuthSource() {
                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, null));
                assertEquals("org.zowe.apiml.zaas.security.schema.missingAuthentication", exc.getMessage());
            }

            @Test
            void givenNoRawAuthSource() {
                AuthSource emptySource = new JwtAuthSource(null);
                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, emptySource));
                assertEquals("org.zowe.apiml.zaas.security.schema.missingAuthentication", exc.getMessage());
            }

            @Test
            void givenSafIdtException() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
                String errorMessage = "Error generating saf idt token";
                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenThrow(new SafIdtException(errorMessage));

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.security.idt.failed", exc.getMessage());
            }

            @Test
            void givenPassTicketException() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID))
                    .thenThrow(new IRRPassTicketGenerationException(8, 8, 0));

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.security.ticket.generateFailed", exc.getMessage());
            }

            @Test
            void givenAuthTokenNotValidException() {
                when(authSourceService.parse(authSource)).thenThrow(TokenNotValidException.class);

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.zaas.security.invalidToken", exc.getMessage());
            }

            @Test
            void givenAuthTokenExpiredException() {
                String safIdt = Jwts.builder()
                    .setExpiration(new Date(System.currentTimeMillis() - 1000L))
                    .compact();

                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);
                when(authSourceService.parse(authSource)).thenThrow(TokenExpireException.class);

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.zaas.security.expiredToken", exc.getMessage());
            }

            @Test
            void givenSafIdTokenNotValidException() throws IRRPassTicketGenerationException {
                String invalidSafIdt = "invalid_saf_id_token";
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(invalidSafIdt);


                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.zaas.security.invalidToken", exc.getMessage());
            }

            @Test
            void givenSafIdTokenExpired() throws IRRPassTicketGenerationException {
                String expiredSafIdt = Jwts.builder()
                    .setExpiration(new Date(System.currentTimeMillis() - 1000L))
                    .compact();
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(expiredSafIdt);

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.zaas.security.expiredToken", exc.getMessage());
            }

            @Test
            void givenNoUserIdFromAuthSource() {
                X509AuthSource.Parsed emptyAuthSource = new X509AuthSource.Parsed(null, null, null, null, null, null);
                when(authSourceService.parse(authSource)).thenReturn(emptyAuthSource);

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(auth, authSource));
                assertEquals("org.zowe.apiml.zaas.security.schema.x509.mappingFailed", exc.getMessage());
            }

            @Test
            void givenNoApplIdFromAuthentication() {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                Authentication authNoApplId = new Authentication(SAF_IDT, null);

                Exception exc = assertThrows(AuthSchemeException.class, () -> underTest.createCommand(authNoApplId, authSource));
                assertEquals("org.zowe.apiml.zaas.security.scheme.missingApplid", exc.getMessage());
            }
        }
    }

    private String getValueOfZuulHeader(String headerName) {
        final RequestContext context = RequestContext.getCurrentContext();
        String valueOfHeader = context.getZuulRequestHeaders().get(headerName.toLowerCase());
        if (valueOfHeader == null) {
            return null;
        }

        return valueOfHeader.replace(headerName + "=", "");
    }
}
