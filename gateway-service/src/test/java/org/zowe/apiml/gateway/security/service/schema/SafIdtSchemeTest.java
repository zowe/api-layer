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
import io.jsonwebtoken.Jwts;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.*;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.auth.AuthenticationScheme.SAF_IDT;
import static org.zowe.apiml.gateway.filters.pre.ServiceAuthenticationFilter.AUTH_FAIL_HEADER;

class SafIdtSchemeTest {
    private SafIdtScheme underTest;
    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final JwtAuthSource authSource = new JwtAuthSource("token");

    private AuthSourceService authSourceService;
    private PassTicketService passTicketService;
    private SafIdtProvider safIdtProvider;

    static MessageService messageService;

    @BeforeAll
    static void setForAll() {
        messageService = new YamlMessageService();
        messageService.loadMessages("/gateway-messages.yml");
    }

    @BeforeEach
    void setUp() {
        authSourceService = mock(AuthSourceService.class);
        passTicketService = mock(PassTicketService.class);
        safIdtProvider = mock(SafIdtProvider.class);

        underTest = new SafIdtScheme(authConfigurationProperties, authSourceService, passTicketService, safIdtProvider, messageService);
        underTest.defaultIdtExpiration = 10;
    }

    @AfterEach
    void tearDown() {
        RequestContext.getCurrentContext().clear();
    }

    @Test
    void testGetAuthSource() {
        doReturn(Optional.empty()).when(authSourceService).getAuthSourceFromRequest();

        underTest.getAuthSource();
        verify(authSourceService, times(1)).getAuthSourceFromRequest();
    }

    @Nested
    class WhenTokenIsRequested {
        private static final String USERNAME = "USERNAME";
        private static final String APPLID = "ANYAPPL";
        private static final String PASSTICKET = "PASSTICKET";

        private final Authentication auth = new Authentication(SAF_IDT, APPLID);
        private final JwtAuthSource.Parsed parsedAuthSource = new JwtAuthSource.Parsed(
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
                    assertThat(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER), is(safIdt));
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
                    assertThat(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER), is(safIdt));
                    assertNull(getValueOfZuulHeader(AUTH_FAIL_HEADER));
                }
            }

            @Nested
            class WhenApplyToRequest {
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

                    HttpRequest httpRequest = new HttpGet("/test/request");

                    ac.applyToRequest(httpRequest);
                    assertThat(httpRequest.getHeaders(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER),
                            hasItemInArray(hasToString(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER + ": " + safIdt)));
                    assertThat(httpRequest.getHeaders(AUTH_FAIL_HEADER), emptyArray());
                }
            }
        }

        @Nested
        class ThenNoTokenIsProduced {
            @Test
            void givenNullAuthSource() {
                AuthenticationCommand ac = underTest.createCommand(auth, null);
                assertNotNull(ac);
                assertEquals("ZWEAG160E No authentication provided in the request", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG160E No authentication provided in the request"));
            }

            @Test
            void givenNoRawAuthSource() {
                AuthSource emptySource = new JwtAuthSource(null);
                AuthenticationCommand ac = underTest.createCommand(auth, emptySource);
                assertNotNull(ac);
                assertEquals("ZWEAG160E No authentication provided in the request", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG160E No authentication provided in the request"));
            }

            @Test
            void givenErrorInRequestContext() {
                final RequestContext context = RequestContext.getCurrentContext();
                context.set(AUTH_FAIL_HEADER, "Some test error message.");

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("Some test error message.", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("Some test error message."));
            }

            @Test
            void givenSafIdtException() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
                String errorMessage = "Error generating saf idt token";
                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenThrow(new SafIdtException(errorMessage));

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG150E SAF IDT generation failed. Reason: " + errorMessage, ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG150E SAF IDT generation failed. Reason: " + errorMessage));
            }

            @Test
            void givenPassTicketException() throws IRRPassTicketGenerationException {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID))
                        .thenThrow(new IRRPassTicketGenerationException(8, 8, 0));

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG141E The generation of the PassTicket failed. Reason: Error on generation of PassTicket: Invalid function code.", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG141E The generation of the PassTicket failed. Reason: Error on generation of PassTicket: Invalid function code."));
            }

            @Test
            void givenAuthTokenNotValidException() {
                when(authSourceService.parse(authSource)).thenThrow(TokenNotValidException.class);

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG102E Token is not valid", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG102E Token is not valid"));
            }

            @Test
            void givenAuthTokenExpiredException() {
                String safIdt = Jwts.builder()
                    .setExpiration(new Date(System.currentTimeMillis() - 1000L))
                    .compact();

                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(safIdt);
                when(authSourceService.parse(authSource)).thenThrow(TokenExpireException.class);

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG103E The token has expired", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG103E The token has expired"));
            }

            @Test
            void givenSafIdTokenNotValidException() throws IRRPassTicketGenerationException {
                String invalidSafIdt = "invalid_saf_id_token";
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(invalidSafIdt);


                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG102E Token is not valid", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG102E Token is not valid"));
            }

            @Test
            void givenSafIdTokenExpired() throws IRRPassTicketGenerationException {
                String expiredSafIdt = Jwts.builder()
                    .setExpiration(new Date(System.currentTimeMillis() - 1000L))
                    .compact();
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                when(passTicketService.generate(USERNAME, APPLID)).thenReturn(PASSTICKET);
                when(safIdtProvider.generate(USERNAME, PASSTICKET.toCharArray(), APPLID)).thenReturn(expiredSafIdt);

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG103E The token has expired", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG103E The token has expired"));
            }

            @Test
            void givenNoUserIdFromAuthSource() {
                X509AuthSource.Parsed emptyAuthSource = new X509AuthSource.Parsed(null,null,null,null, null, null);
                when(authSourceService.parse(authSource)).thenReturn(emptyAuthSource);

                AuthenticationCommand ac = underTest.createCommand(auth, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG161E No user was found", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG161E No user was found"));
            }

            @Test
            void givenNoApplIdFromAuthentication() {
                when(authSourceService.parse(authSource)).thenReturn(parsedAuthSource);
                Authentication authNoApplId = new Authentication(SAF_IDT, null);

                AuthenticationCommand ac = underTest.createCommand(authNoApplId, authSource);
                assertNotNull(ac);
                assertEquals("ZWEAG165E The 'apiml.authentication.applid' parameter is not specified for a service.", ((SafIdtScheme.SafIdtCommand) ac).getErrorMessage());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getSafIdentityToken());
                assertNull(((SafIdtScheme.SafIdtCommand) ac).getExpireAt());

                ac.apply(null);
                assertNull(getValueOfZuulHeader(SafIdtScheme.SafIdtCommand.SAF_TOKEN_HEADER));
                assertThat(getValueOfZuulHeader(AUTH_FAIL_HEADER), is("ZWEAG165E The 'apiml.authentication.applid' parameter is not specified for a service."));
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
